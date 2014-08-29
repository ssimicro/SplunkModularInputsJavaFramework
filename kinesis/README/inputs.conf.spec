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
