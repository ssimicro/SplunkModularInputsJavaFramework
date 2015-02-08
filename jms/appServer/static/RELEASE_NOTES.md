1.3.8
-----
Replaced the core Splunk Java SDK jar with a patched version that allows you to override the default use of SSLv3 with TLSv1.2.
To do so you specify "splunk.securetransport.protocol=tls" in the Additional JVM System Properties parameter when you configure the stanza.

1.3.7
-----
Changed the point in the code where client ID is set for durable topic subscriptions

1.3.6
-----
Added a LocalConnectionFactory for ActiveMQ

1.3.5
-----
Added the ability to declare custom JVM System Properties in your stanzas
