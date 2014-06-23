[amqp://name]

# topic or queue

destination_type = <value>

# name of the queue or topic

destination_name = <value>

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
channel_durable = <value>
channel_exclusive = <value>
channel_auto_delete = <value>
ack_messages = <value>

#message handler

message_handler_impl = <value>
message_handler_params = <value>

#optional parts of the message to index

index_message_envelope = <value>
index_message_propertys = <value>

#additional startup settings

additional_jvm_propertys = <value>
