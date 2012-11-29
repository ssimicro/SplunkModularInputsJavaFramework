## Splunk JMS Modular Input v1.0

## Overview

This is a Splunk modular input add-on for polling message queues and topics via the JMS interface.

It implements the  <a href="http://docs.splunk.com/Documentation/Splunk/latest/AdvancedDev/ModInputsIntro">Splunk Modular Inputs Framework</a>

The <a href="https://github.com/damiendallimore/SplunkModularInputsJavaFramework">Splunk Modular Inputs Java Framework</a> is utilized.

## Dependencies

* Splunk 5.0+
* Java Runtime 1.5+

## Setup

* Set your JAVA_HOME environment variable
* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk

## Configuration

As this is a modular input , you can then configure your JMS inputs via Manager->DataInputs

## Logging

Any log entries will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

# Third party jars

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
