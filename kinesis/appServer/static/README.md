## Splunk Amazon Kinesis Modular Input v0.9b

## Overview

This is a Splunk Modular Input Add-On for indexing records from Amazon Kinesis.

## What is Kinesis ?

http://aws.amazon.com/kinesis/

## Dependencies

* Splunk 5.0+
* Java Runtime 1.7+
* Supported on Windows, Linux, MacOS, Solaris, FreeBSD, HP-UX, AIX

## Setup

* Optionally set your JAVA_HOME environment variable to the root directory of your JRE installation.If you don't set this , the input will look for a default installed java executable on the path.
* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk

## Configuration

As this is a Modular Input , you can then configure your Kinesis inputs via Manager->Data Inputs->Kinesis. 

## Logging

Any log entries/errors will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

## JVM Heap Size

The default heap maximum is 64MB.
If you require a larger heap, then you can alter this in $SPLUNK_HOME/etc/apps/kinesis_ta/bin/kinesis.py on line 95

## JVM System Properties

You can declare custom JVM System Properties when setting up new input stanzas.
Note : these JVM System Properties will apply to the entire JVM context and all stanzas you have setup

## Customized Message Handling

The way in which the Modular Input processes the received Kinesis records is enitrely pluggable with custom implementations should you wish.

To do this you code an implementation of the com.splunk.modinput.kinesis.AbstractMessageHandler class and jar it up.

Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/kinesis_ta/bin/lib directory.

If you don't need a custom handler then the default handler com.splunk.modinput.kinesis.DefaultMessageHandler will be used.

Code examples are on GitHub : https://github.com/damiendallimore/SplunkModularInputsJavaFramework/tree/master/kinesis/src/com/splunk/modinput/kinesis

## Troubleshooting

* JAVA_HOME environment variable is set or "java" is on the PATH for the user's environment you are running Splunk as
* You are using Splunk 5+
* You are using a 1.7+ Java Runtime
* You are running on a supported operating system
* Look for any errors in $SPLUNK_HOME/var/log/splunk/splunkd.log
* Run this command as the same user that you are running Splunk as and observe console output : "$SPLUNK_HOME/bin/splunk cmd python ../etc/apps/kinesis_ta/bin/kinesis.py --scheme" 

## Contact

This project was initiated by Damien Dallimore , ddallimore@splunk.com

