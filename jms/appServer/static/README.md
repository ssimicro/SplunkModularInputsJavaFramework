## Splunk JMS Modular Input v1.0

## Overview

This is a Splunk modular input add-on for polling message queues and topics via the JMS interface.

It implements the  <a href="http://docs.splunk.com/Documentation/Splunk/latest/AdvancedDev/ModInputsIntro">Splunk Modular Inputs Framework</a>

The <a href="https://github.com/damiendallimore/SplunkModularInputsJavaFramework">Splunk Modular Inputs Java Framework</a> is utilized.

## Why JMS ?

JMS is simply a messaging API and is a convenient means by which to write 1 modular input that can talk to several different underlying messaging
providers :  MQSeries(Websphere MQ), ActiveMQ, HornetQ, RabbitMQ,Native JMS, Weblogic JMS, Sonic MQ etc..
The modular input code is generic because it is programmed to the JMS interface.
You can then supply messaging provider specific jar files at runtime.
<a href="http://en.wikipedia.org/wiki/Java_Message_Service">More details on JMS at Wikipedia</a>

## Dependencies

* Splunk 5.0+
* Java Runtime 1.6+

## Setup

* Set your JAVA_HOME environment variable
* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk

## Configuration

As this is a modular input , you can then configure your JMS inputs via Manager->DataInputs

## Logging

Any log entries will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

## Third party jars

If you require specific JMS provider or JNDI Context implementation jars, then you can simply copy these to $SPLUNK_HOME/etc/apps/JMSModularInput/bin/lib

They will be automatically picked up upon restart 

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
