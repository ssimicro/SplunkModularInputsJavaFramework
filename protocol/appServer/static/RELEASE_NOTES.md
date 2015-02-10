0.7
----
Enabled TLS1.2 support by default.
Made Core Modular Framework compatible with Splunk Java SDK

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
