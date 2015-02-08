0.7
-----
Replaced the core Splunk Java SDK jar with a patched version that allows you to override the default use of SSLv3 with TLSv1.2.
To do so you specify "splunk.securetransport.protocol=tls" in the Additional JVM System Properties parameter when you configure the stanza.


0.6
-----
Added a custom message handler that just dumps the JSON body

0.5
-----
Initial beta release
