## Protocol Data Inputs v0.5b

## Overview

This is a Splunk Add-On for receiving data via a number of different data protocols.

## Protocols

* TCP
* TCP w/ TLS (optional client certificate authentication)
* UDP (unicast and multicast)
* HTTP (PUT and POST methods only , data in body)
* HTTPS (PUT and POST methods only , data in body) (optional client certificate authentication)
* Websockets
* SockJS

## But we already have TCP/UDP natively in Splunk

Yes we do. And by all means use those. But if you want to perform some custom data handling and pre-processing 
of the received data before it gets indexed (above and beyond what you can accomplish using Splunk conf files) , 
then this Modular Input presents another option for you.
Furthermore , this Modular Input also implements several other protocols for sending data to Splunk.


## Implementation

This Modular Input utilizes VERTX.IO version 2.1.4 under the hood.http://vertx.io/manual.html#what-is-vertx.

This framework provides for an implementation that is :

* asynchronous
* event driven
* polyglot (code custom data handlers in java , javascript , groovy , scala , clojure , ruby , python , any JVM lang with a vertx module)
* non blocking IO
* scales over all your available cores
* can serve high volumes of concurrent client connections

## Polyglot Custom Data Handling / Pre Processing 

The way in which the Modular Input processes the received raw data is entirely pluggable with custom implementations should you wish. 

This allows you to :

* pre process the raw data before indexing 
* transform the data into a more optimum state for Splunk
* perform custom computations on the data that the Splunk Search language is not the best fit for
* decode binary data (encrypted , compressed , images , proprietary protocols , EBCDIC etc....)
* enforce CIM compliance on the data you feed into the Splunk indexing pipeline
* basically do anything programmatic to the raw byte data you want

To do this you code a Vertx "Verticle" to handle the received data. http://vertx.io/manual.html#verticle

These data handlers can be written in numerous JVM languages. http://vertx.io/manual.html#polyglot

You then place the handler in the $SPLUNK_HOME/etc/apps/protocol_ta/bin/datahandlers directory.

On the Splunk config screen for the Modular Input there is a field where you can then specify the name of this handler to be applied.

If you don't need a custom handler then the default handler com.splunk.modinput.protocolverticle.DefaultHandlerVerticle will be used.

To get started , you can refer to the  default handler examples in the datahandlers directory.

## SSL / TLS

This is provisioned using your own Java Keystore that you can create using the keytool utility that is part of the JDK.

Refer to http://vertx.io/core_manual_java.html#ssl-servers

## Authentication

Client certificate based authentication can be enabled for the TLS/SSL channels you setup.


## Performance tuning tips

Due to the nature of the async/event driven/non blocking architecture , the out of the box default settings may just well suffice for you.

But there are some other parameters that you can tune to take more advantage of your underlying computing resource(cpu core setc..) available to you.

These are the "server_verticle_instances" and "handler_verticle_instances" params.

Refer to http://vertx.io/core_manual_java.html#specifying-number-of-instances for an explanation of how increasing the number of instances may help you.

You can also tune the TCP accept queue settings (also requires OS tweaks) , particularly if you are receiving lots of connections within a short time span.

Refer to http://vertx.io/manual.html#improving-connection-time


## Dependencies

* Splunk 5.0+
* Java Runtime 1.5+
* Supported on Windows, Linux, MacOS, Solaris, FreeBSD, HP-UX, AIX

## Setup

* Optionally set your JAVA_HOME environment variable to the root directory of your JRE installation.If you don't set this , the input will look for a default installed java executable on the path.
* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk

## Configuration

As this is a Modular Input , you can then configure your Protocol inputs via Manager->Data Inputs->Protocol Data Inputs 



## Logging

Any log entries/errors will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

## JVM Heap Size

The default heap maximum is 256MB.
If you require a larger heap, then you can alter this in $SPLUNK_HOME/etc/apps/protocol_ta/bin/protocol.py on line 95

## JVM System Properties

You can declare custom JVM System Properties when setting up new input stanzas.
Note : these JVM System Properties will apply to the entire JVM context and all stanzas you have setup


## Troubleshooting

* JAVA_HOME environment variable is set or "java" is on the PATH for the user's environment you are running Splunk as
* You are using Splunk 5+
* You are using a 1.5+ Java Runtime
* You are running on a supported operating system
* Look for any errors in $SPLUNK_HOME/var/log/splunk/splunkd.log
* Run this command as the same user that you are running Splunk as and observe console output : "$SPLUNK_HOME/bin/splunk cmd python ../etc/apps/protocol_ta/bin/protocol.py --scheme" 

## Contact

This project was initiated by Damien Dallimore , ddallimore@splunk.com

