[kafka://name]

# name of the topic

topic_name = <value>

# consumer connection properties

zookeeper_connect_host = <value>
zookeeper_connect_port = <value>
zookeeper_connect_chroot = <value>
zookeeper_connect_rawstring = <value>
group_id = <value>
zookeeper_session_timeout_ms = <value>
zookeeper_sync_time_ms = <value>
auto_commit_interval_ms = <value>
additional_consumer_properties = <value>

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
