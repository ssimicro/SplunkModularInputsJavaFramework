0.7
-----
Replaced the core Splunk Java SDK jar with a patched version that allows you to override the default use of SSLv3 with TLSv1.2.
To do so you specify "splunk.securetransport.protocol=tls" in the Additional JVM System Properties parameter when you configure the stanza.

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
