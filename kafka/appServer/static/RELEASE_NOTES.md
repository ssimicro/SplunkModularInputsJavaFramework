0.9.1
-----
Better logging around HEC success/failure
Can now add custom timestamp into HEC payload
New custom handler (JSONBodyWithTimeExtraction) for pulling out timestamp from JSON messages from Kafka and adding this into HEC payload

0.9
---
Added support to optional output to Splunk via a HEC (HTTP Event Collector) endpoint


0.8.1
---
Added support for raw connection string format so that multiple zookeeper hosts
can be provided in a comma delimited manner
ie: hostname1:port1,hostname2:port2,hostname3:port3/chroot/path 

0.8
---
Added chroot support

0.7
----
Enabled TLS1.2 support by default.
Made the  core Modular Input Framework compatible with latest Splunk Java SDK
Please use a Java Runtime version 7+
If you need to use SSLv3 , you can turn this on in bin/kafka.py
SECURE_TRANSPORT = "tls"
#SECURE_TRANSPORT = "ssl"

0.6
-----
You can now pass a charset name to the DefaultHandler

0.5
-----
Initial beta release
