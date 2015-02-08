2.2
----
Replaced the core Splunk Java SDK jar with a patched version that allows you to override the default use of SSLv3 with TLSv1.2.
To do so you specify "splunk.securetransport.protocol=tls" in the Additional JVM System Properties parameter when you configure the stanza.

Also had to change the root directory name from "jmx_ta" back to "SPLUNK4JMX" because Splunkbase was preventing 
me from uploading a new release if the App ID (not editable)  did not exactly match the root directory name.

2.1
----
Config file dynamically reloaded if it changes
PID File contents read in on each poller execution
PID Command execution on each poller execution
PID Command can also return JVM Descriptions

2.0.4
-----
Minor change so that when using "dumpAllAttributes" , only READABLE attributes will be polled.