[jms://<name>]
jndi_initialcontext_factory = <value>
jndi_provider_url = <value>
jndi_user = <value>
jndi_pass = <value>
destination_user = <value>
destination_pass = <value>
jms_connection_factory_name = <value>
durable = <value>
index_message_properties = <value>
index_message_header = <value>
message_selector = <value>
strip_newlines = <value>
init_mode = <value>
local_init_mode_resource_factory_impl = <value>
local_init_mode_resource_factory_params = <value>
message_handler_impl = <value>
message_handler_params = <value>
client_id = <value>
user_jndi_properties = <value>
browse_queue_only = <value>
browse_frequency = <value>
browse_mode = <value>
jvm_system_properties = <value>
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