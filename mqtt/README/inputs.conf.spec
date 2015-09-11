[mqtt://name]

#connection settings

topic_name = <value>
broker_host = <value>
broker_port = <value>
use_ssl = <value>


username = <value>
password = <value>
client_id = <value>

qos = <value>
reliable_delivery_dir = <value>
clean_session = <value>
connection_timeout = <value>
keepalive_interval = <value>

# message handler

message_handler_impl = <value>
message_handler_params = <value>

# additional startup settings

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
