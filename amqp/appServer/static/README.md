## Splunk AMQP Modular Input v0.5b

## Overview

This is a Splunk Modular Input Add-On for indexing messages from an AMQP Broker.It utilizes the RabbitMQ Java client library , but can be used against any AMQP v1.0 compliant broker.


## What is AMQP ?

From Wikipedia : http://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol


## AMQP v1.0  Brokers

* RabbitMQ
* Apache ActiveMQ
* Apache Qpid
* SwiftMQ
* Apache Apollo
* Windows Azure Service Bus

## Dependencies

* Splunk 5.0+
* Java Runtime 1.6+
* Supported on Windows, Linux, MacOS, Solaris, FreeBSD, HP-UX, AIX

## Setup

* Optionally set your JAVA_HOME environment variable to the root directory of your JRE installation.If you don't set this , the input will look for a default installed java executable on the path.
* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk

## Configuration

As this is a modular input , you can then configure your AMQP inputs via Manager->DataInputs


## Logging

Any log entries/errors will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

##JVM Heap Size

The default heap maximum is 64MB.
If you require a larger heap, then you can alter this in $SPLUNK_HOME/etc/apps/amqp_ta/bin/amqp.py on line 95

##JVM System Properties

You can declare custom JVM System Properties when setting up new input stanzas.
Note : these JVM System Properties will apply to the entire JVM context and all stanzas you have setup

## Troubleshooting

* JAVA_HOME environment variable is set or "java" is on the PATH for the user's environment you are running Splunk as
* You are using Splunk 5+
* You are using a 1.6+ Java Runtime
* You are running on a supported operating system
* Look for any errors in $SPLUNK_HOME/var/log/splunk/splunkd.log
* Run this command as the same user that you are running Splunk as and observe console output : "$SPLUNK_HOME/bin/splunk cmd python ../etc/apps/amqp_ta/bin/amqp.py --scheme" 

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
