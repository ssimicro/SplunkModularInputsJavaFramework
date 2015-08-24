[mq://name]

#boolean values,  1 or 0
inquire_queues = <value>
inquire_topics = <value>
inquire_channels = <value>
inquire_listeners = <value>
inquire_subscriptions = <value>
inquire_services = <value>
inquire_current_queuemanager = <value>

#queue manager connection settings

#defaults to localhost
hostname = <value>

#defaults to 1414
port = <value>

#required field
manager_name = <value>

#defaults to SYSTEM.DEF.SVRCONN
channel_name = <value>

#optional fields
username = <value>
password = <value>

#defaults to false
use_ssl = <value>
ssl_cipher_suite = <value>
#defaults to false
ssl_fips_required = <value>

#the frequency in seconds which PCF request inquiry messages are sent.Defaults to 60 seconds
polling_frequency = <value>

# custom event handler
event_handler_impl = <value>
event_handler_params = <value>

#any additional JVM system propertys you might need to add in
additional_jvm_propertys = <value>

# data output

# One of [stdout | hec ]. Defaults to stdout.
output_type = <value>

# For hec(HTTP Event Collector) output
hec_port = <value>
# Defaults to 1
hec_poolsize = <value>
hec_token = <value>
# 1 | 0
hec_https = <value>
