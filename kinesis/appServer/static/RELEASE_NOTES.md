0.9
---
Tweaked the HEC transport.

Added a new custom handler that allows you to declare the fieldnames in the JSON that hold the time and host values of the event.

message_handler_impl = com.splunk.modinput.kinesis.JSONBodyWithFieldExtraction
message_handler_params = timefield=foo,hostfield=goo


0.8
---
Added support to optional output to Splunk via a HEC (HTTP Event Collector) endpoint

0.7
----
Enabled TLS1.2 support by default.
Made the  core Modular Input Framework compatible with latest Splunk Java SDK
Please use a Java Runtime version 7+
If you need to use SSLv3 , you can turn this on in bin/kinesis.py
SECURE_TRANSPORT = "tls"
#SECURE_TRANSPORT = "ssl"

0.6
-----
Added a custom message handler that just dumps the JSON body

0.5
-----
Initial beta release
