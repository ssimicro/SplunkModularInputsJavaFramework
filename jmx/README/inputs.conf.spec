[jmx://<name>]

*name of the config file.Defaults to config.xml
config_file = <value>

*optional alternate location for config files
config_file_dir = <value>

*how frequently to execute the polling in seconds.Defaults to 60
polling_frequency = <value>


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

