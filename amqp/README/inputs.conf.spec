[amqp://name]

# name of the queue

queue_name = <value>

#fields for the AMQP URI

hostname = <value>
port = <value>
username = <value>
password = <value>
virtual_host = <value>
use_ssl = <value>

#common settings for polling queues and topics

routing_key_pattern = <value>
exchange_name = <value>
ack_messages = <value>
basic_qos_limit = <value>

#message handler

message_handler_impl = <value>
message_handler_params = <value>

#optional parts of the message to index

index_message_envelope = <value>
index_message_propertys = <value>

#additional startup settings

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
# 1 | 0
hec_batch_mode = <value>
# numeric value
hec_max_batch_size_bytes = <value>
# numeric value
hec_max_batch_size_events = <value>
#in milliseconds
hec_max_inactive_time_before_batch_flush = <value>
		
