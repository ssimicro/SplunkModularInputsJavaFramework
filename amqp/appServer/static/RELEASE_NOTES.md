0.9
---

Adjusted pre fetch logic

0.8
---
Added support to optional output to Splunk via a HEC (HTTP Event Collector) endpoint

0.7
---
Enabled TLS1.2 support by default.
Made the  core Modular Input Framework compatible with latest Splunk Java SDK
Please use a Java Runtime version 7+
If you need to use SSLv3 , you can turn this on in bin/mq.py
SECURE_TRANSPORT = "tls"
#SECURE_TRANSPORT = "ssl"

0.6
----
Added configurable basic qos parameter

0.5
-----
Initial beta release


