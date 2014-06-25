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

#message handler

message_handler_impl = <value>
message_handler_params = <value>

#optional parts of the message to index

index_message_envelope = <value>
index_message_propertys = <value>

#additional startup settings

additional_jvm_propertys = <value>
