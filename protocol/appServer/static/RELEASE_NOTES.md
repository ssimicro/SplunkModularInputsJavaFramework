1.0
---
Added support for output to be sent via the HTTP Event Collector

0.7
----
Enabled TLS1.2 support by default.
Made the  core Modular Input Framework compatible with latest Splunk Java SDK
Please use a Java Runtime version 7+
If you need to use SSLv3 , you can turn this on in bin/protocol.py
SECURE_TRANSPORT = "tls"
#SECURE_TRANSPORT = "ssl"

0.6
-----
Abstracted the output transport logic out into verticles.
So you can choose from STDOUT (default for Modular Inputs) or bypass this and output
data to Splunk over other transports ie: TCP.
This also makes it easy to add other output transports  in the future.
Futhermore , this makes the implementation of custom data handlers much cleaner as you don't have
worry out output transport logic or formatting Modular Input Stream XML for STDOUT transports. 

0.5
-----
Initial beta release
