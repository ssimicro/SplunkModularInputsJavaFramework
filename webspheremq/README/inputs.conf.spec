[mq://name]

#boolean values,  1 or 0
inquire_queues = <value>
inquire_topics = <value>
inquire_pubsub = <value>
inquire_channels = <value>
inquire_listeners = <value>
inquire_processes = <value>
inquire_connections = <value>
inquire_subscriptions = <value>
inquire_services = <value>
inquire_current_queuemanager = <value>

#queue manager connection settings

#defaults to localhost
host = <value>

#defaults to 1414
port = <value>

manager_name = <value>

#defaults to SYSTEM.DEF.SVRCONN
channel_name = <value>

username = <value>
password = <value>

#the frequency in seconds which PCF request inquiry messages are sent.Defaults to 60 seconds
polling_frequency = <value>

# custom event handler
event_handler_impl = <value>
event_handler_params = <value>

#any additional JVM system propertys you might need to add in
additional_jvm_propertys = <value>
