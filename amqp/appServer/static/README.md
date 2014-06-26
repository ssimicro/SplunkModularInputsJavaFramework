## Splunk AMQP Messaging Modular Input v0.5b

## Overview

This is a Splunk Modular Input Add-On for indexing messages from an AMQP Broker.It utilizes the RabbitMQ Java client library(v3.3.3) , but can be used against any AMQP v1.0 compliant broker. Testing was performed against RabbitMQ Server v3.3.3


## What is AMQP ?

From Wikipedia : 


## Examples of AMQP v1.0  Brokers

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

As this is a Modular Input , you can then configure your AMQP inputs via Manager->Data Inputs->AMQP. The field entry should be straightforward and intuitive for anyone with basic experience with AMQP.

## Logging

Any log entries/errors will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

## JVM Heap Size

The default heap maximum is 64MB.
If you require a larger heap, then you can alter this in $SPLUNK_HOME/etc/apps/amqp_ta/bin/amqp.py on line 95

## JVM System Properties

You can declare custom JVM System Properties when setting up new input stanzas.
Note : these JVM System Properties will apply to the entire JVM context and all stanzas you have setup

## Customized Message Handling

The way in which the Modular Input processes the received AMQP messages is enitrely pluggable with custom implementations should you wish.

To do this you code an implementation of the com.splunk.modinput.amqp.AbstractMessageHandler class and jar it up.

Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/amqp_ta/bin/lib directory.

If you don't need a custom handler then the default handler com.splunk.modinput.amqp.DefaultMessageHandler will be used.

This handler simply trys to convert the received byte array into a textual string for indexing in Splunk.

Code examples are on GitHub : 

## Troubleshooting

* JAVA_HOME environment variable is set or "java" is on the PATH for the user's environment you are running Splunk as
* You are using Splunk 5+
* You are using a 1.6+ Java Runtime
* You are running on a supported operating system
* Look for any errors in $SPLUNK_HOME/var/log/splunk/splunkd.log
* Run this command as the same user that you are running Splunk as and observe console output : "$SPLUNK_HOME/bin/splunk cmd python ../etc/apps/amqp_ta/bin/amqp.py --scheme" 

## Contact

This project was initiated by Damien Dallimore 

