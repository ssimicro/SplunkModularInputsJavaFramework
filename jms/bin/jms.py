'''
Wrapper Script for the JMS Modular Input

Copyright (C) 2012 Splunk, Inc.
All Rights Reserved

Because Splunk can't directly invoke Java , we use this python wrapper script that
simply proxys through to the Java program
'''
import os, sys
from subprocess import Popen

JAVA_MAIN_CLASS = 'com.splunk.modinput.jms.JMSModularInput'

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

    if sys.platform.startswith('linux'):
      JAVA_EXECUTABLE = '$JAVA_HOME/bin/java'
    elif sys.platform == 'win32':
      JAVA_EXECUTABLE = '%JAVA_HOME%\bin\java'
    else:
      sys.stderr.writelines("ERROR Unsupported platform\n")
      sys.exit(0)
    
    java_args = [ JAVA_EXECUTABLE, "-classpath lib/*","-Xms64m -Xmx64m",JAVA_MAIN_CLASS] 
    java_args.extend(sys.argv[1:])

    # Now we can run our command
    process = Popen(java_args)
    # Wait for it to complete
    process.wait()              
    sys.exit(process.returncode)
    

        
