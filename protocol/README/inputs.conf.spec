[protocol://<name>]

*------------
*General settings
*------------

*protocol to use  , one of  [tcp , udp, http, websocket , sockjs]
protocol= <value>

*network port to open.For ports < 1024 , you'll need to be running with root permissions.
port= <value>

*network interface address to bind to , IP or hostname , defaults to 0.0.0.0 (listen on all interfaces)
bind_address= <value>

*whether or not (0,1) to use SSL for TCP or HTTP
use_ssl= <value>

*------------
*TCP settings
*------------

*whether or not (0,1) to enable TCP No Delay
tcp_nodelay= <value>

*buffer size (number)
receive_buffer_size= <value>

*whether or not (0,1) to enable TCP Keep Alive
tcp_keepalive= <value>

*SO Linger time in seconds.Using a negative value will disable it.
so_linger= <value>

*-------------------------------------------------------------------------------
*SSL settings (uses your own Java Keystore , NOT Splunk's internal Certificates)
*Refer to http://vertx.io/core_manual_java.html#ssl-servers
*-------------------------------------------------------------------------------

*Java Keystore password
keystore_pass= <value>

*Java Keystore path
keystore_path= <value>

*Java Truststore password
truststore_pass= <value>

*Java Truststore path
truststore_path= <value>

*whether or not (0,1) client authentication is required
client_auth_required= <value>

*------------
*UDP settings
*------------

*v4 or v6
ip_version= <value>

*whether or not (0,1) this UDP socket is also multicast
is_multicast= <value>

*buffer size (number)
udp_receive_buffer_size= <value>

*whether or not (0,1) to set broadcast mode
set_broadcast= <value>

*IP address pattern of the network interface
multicast_group= <value>

*time to live (number)
multicast_ttl= <value>

*whether or not (0,1) to set multicast loopback mode
set_multicast_loopback_mode= <value>

*---------------
*SockJS Settings
*---------------

*session timeout (number)
session_timeout= <value>

*heartbeat period (number)
heartbeat_period= <value>

*application name. Defaults to "splunk" , so the URI would be http://somehost/splunk
app_name= <value>

*---------------
*Custom Data Handler
*---------------

*custom data handler name (a vertx polyglot verticle that you've placed in the protocol_ta/bin/datahandlers directory)
handler_verticle = <value>

*A JSON Config String to pass to the handler, example :  {"foo":"1","zoo":"goo"}
handler_config = <value>

*------------
*Data Output
*------------

* One of [stdout | tcp ]. Defaults to stdout.
output_type = <value>

* For tcp output.
output_port = <value>


*---------------------
*JVM System Properties
*---------------------

*additional JVM properties , these will get applied JVM wide , so be judicious in use
additional_jvm_propertys = <value>

*-------------------------------
*Performance Tuning and Scaling
*-------------------------------

*You can increase the number of instances to utilise more cores on your server

*defaults to 1 , refer to http://vertx.io/core_manual_java.html#specifying-number-of-instances
server_verticle_instances = <value>

*defaults to 1 , refer to http://vertx.io/core_manual_java.html#specifying-number-of-instances
handler_verticle_instances = <value>

*defaults to 1 , refer to http://vertx.io/core_manual_java.html#specifying-number-of-instances
output_verticle_instances = <value>

* Refer to http://vertx.io/manual.html#improving-connection-time
accept_backlog = <value>
