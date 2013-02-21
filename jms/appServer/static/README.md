## Splunk JMS Modular Input v1.1.4

## Overview

This is a Splunk modular input add-on for polling message queues and topics via the JMS interface.

It implements the  <a href="http://docs.splunk.com/Documentation/Splunk/latest/AdvancedDev/ModInputsIntro">Splunk Modular Inputs Framework</a>

The <a href="https://github.com/damiendallimore/SplunkModularInputsJavaFramework">Splunk Modular Inputs Java Framework</a> is utilized.

## Why JMS ?

JMS is simply a messaging API and is a convenient means by which to write 1 modular input that can talk to several different underlying messaging
providers :  MQSeries(Websphere MQ), ActiveMQ, TibcoEMS, HornetQ, RabbitMQ,Native JMS, Weblogic JMS, Sonic MQ etc..
The modular input code is generic because it is programmed to the JMS interface.
You can then supply messaging provider specific jar files at runtime.
<a href="http://en.wikipedia.org/wiki/Java_Message_Service">More details on JMS at Wikipedia</a>

## Dependencies

* Splunk 5.0+
* Java Runtime 1.6+
* Supported on Windows, Linux, MacOS, Solaris, FreeBSD, HP-UX, AIX

## Setup

* Optionally set your JAVA_HOME environment variable to the root directory of you JRE installation.If you don't set this , the input will look for a default installed java executable on the path.
* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk

## Configuration

As this is a modular input , you can then configure your JMS inputs via Manager->DataInputs

##JNDI vs Local mode

For the most part you will setup your JMS connectivity using JNDI to obtain the remote JMS objects.
However, you can bypass JNDI if you wish and use local instantiation.
To this you must code an implementation of the com.splunk.modinput.jms.LocalJMSResourceFactory interface.
You can then bundle the classes in a jar file and place them in $SPLUNK_HOME/etc/apps/jms_ta/bin/lib
The configuration screen in Splunk Manager for creating a new JMS input allows you to choose local or jndi as the instantiation mode.
So choose local , and then you can specify the name of implementation class, as well as any declarative paramaters you want to pass in.

## Logging

Any log entries/errors will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

## Third party jars

If you require specific JMS provider or JNDI Context implementation jars, then you can simply copy these to $SPLUNK_HOME/etc/apps/jms_ta/bin/lib

They will be automatically picked up upon restart 


## Troubleshooting

* JAVA_HOME environment variable is set or "java" is on the PATH for the user's environment you are running Splunk as
* You are using Splunk 5+
* You are using a 1.6+ Java Runtime
* You are running on a supported operating system
* Any 3rd party jar dependencies are present in $SPLUNK_HOME/etc/apps/jms_ta/bin/lib
* Look for any errors in $SPLUNK_HOME/var/log/splunk/splunkd.log
* Run this command as the same user that you are running Splunk as and observe console output : "$SPLUNK_HOME/bin/splunk cmd python ../etc/apps/jms_ta/bin/jms.py --scheme" 
* Your configuration parameters are correct for your JMX connection (check for typos, correct credentials, correct JNDI names etc...)
* You DNS resolution for hostnames is correctly configured

## Contact

This project was initiated by Damien Dallimore
<table>

<tr>
<td><em>Email</em></td>
<td>ddallimore@splunk.com</td>
</tr>

<tr>
<td><em>Twitter</em>
<td>@damiendallimore</td>
</tr>


</table>
