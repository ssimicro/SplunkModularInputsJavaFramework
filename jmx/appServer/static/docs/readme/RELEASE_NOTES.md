2.2.1
-----
Fixed minor typos

2.2
----
Enabled TLS1.2 support by default.
Made the  core Modular Input Framework compatible with latest Splunk Java SDK
Please use a Java Runtime version 7+
If you need to use SSLv3 , you can turn this on in bin/jmx.py
SECURE_TRANSPORT = "tls"
#SECURE_TRANSPORT = "ssl"

2.1
----
Config file dynamically reloaded if it changes
PID File contents read in on each poller execution
PID Command execution on each poller execution
PID Command can also return JVM Descriptions

2.0.4
-----
Minor change so that when using "dumpAllAttributes" , only READABLE attributes will be polled.