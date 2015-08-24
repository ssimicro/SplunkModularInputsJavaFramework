[kinesis://name]

#connection settings

app_name = <value>
stream_name = <value>
kinesis_endpoint = <value>

#LATEST or TRIM_HORIZON
initial_stream_position = <value>

aws_access_key_id = <value>
aws_secret_access_key = <value>

#message reader settings

backoff_time_millis = <value>
num_retries = <value>
checkpoint_interval_millis = <value>

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
