'''
Wrapper Script for the JMS Modular Input

Copyright (C) 2012 Splunk, Inc.
All Rights Reserved

Because Splunk can't directly invoke Java , we use this python wrapper script that
simply proxys through to the Java program
'''
import os, sys, signal
from subprocess import Popen

JAVA_MAIN_CLASS = 'com.splunk.modinput.jms.JMSModularInput'
MODINPUT_NAME = 'jms'


def checkForRunningProcess():

    canonPath = getPIDFilePath()
    if os.path.isfile(canonPath):
      pidfile = open(canonPath, "r")
      pidfile.seek(0)
      old_pid = pidfile.readline()
      try:
        os.kill(int(old_pid),signal.SIGKILL)
      except :
        pass
      os.remove(canonPath)
      
def writePidFile():
    canonPath = getPIDFilePath()
    pid = str(process.pid)
    f = open(canonPath, 'w')
    f.write(pid)
    f.close()
    
def getPIDFilePath():
    return MODINPUT_HOME+MODINPUT_NAME+"_ta.pid"
    
def usage():
    print "usage: %s [--scheme|--validate-arguments]"
    sys.exit(2)

def do_run():
    xml_str = sys.stdin.read()
    sys.argv.append(xml_str)
    run_java()

def do_scheme():
    run_java()

def do_validate():
    xml_str = sys.stdin.read()
    sys.argv.append(xml_str)
    run_java()

def run_java():
    global process,SPLUNK_HOME,MODINPUT_HOME
    if sys.platform.startswith('linux'):
      JAVA_EXECUTABLE = os.path.expandvars('$JAVA_HOME') + "/bin/java"
      SPLUNK_HOME = os.path.expandvars('$SPLUNK_HOME')
      MODINPUT_HOME = SPLUNK_HOME + "/etc/apps/"+MODINPUT_NAME+"_ta/"
      CLASSPATH = MODINPUT_HOME + "bin/lib/*"
    elif sys.platform == 'win32':
      JAVA_EXECUTABLE = os.path.expandvars('%JAVA_HOME%') + "\\bin\\java"
      SPLUNK_HOME = os.path.expandvars('%SPLUNK_HOME%')
      MODINPUT_HOME = SPLUNK_HOME  + "\\etc\\apps\\"+MODINPUT_NAME+"_ta\\"
      CLASSPATH = MODINPUT_HOME + "bin\\lib\\*"
    else:
      sys.stderr.writelines("ERROR Unsupported platform\n")
      sys.exit(0)

    checkForRunningProcess()

    java_args = [ JAVA_EXECUTABLE, "-classpath",CLASSPATH,"-Xms64m","-Xmx64m",JAVA_MAIN_CLASS]
    java_args.extend(sys.argv[1:])

    # Now we can run our command   
    process = Popen(java_args)
    writePidFile()
    # Wait for it to complete
    process.wait()
    sys.exit(process.returncode)

def signal_handler(signal, frame):
        #kill the java process
        process.kill()
        #exit this script
        sys.exit(0)

        
signal.signal(signal.SIGINT, signal_handler)

if __name__ == '__main__':
    
    if len(sys.argv) > 1:
        if sys.argv[1] == "--scheme":
            do_scheme()
        elif sys.argv[1] == "--validate-arguments":
            do_validate()
        else:
            usage()
    else:
        do_run()