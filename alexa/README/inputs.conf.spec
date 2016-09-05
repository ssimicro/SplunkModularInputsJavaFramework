[alexa://name]

#defaults to 443
https_port = <value>

#defaults to  https
https_scheme = <value>

#ie : www.mysplunkhost.com/alexa
endpoint = <value>

#defaults to SPLUNK_HOME/etc/apps/alexa/crypto/java-keystore.jks
keystore = <value>

#your java keystore password
keystore_pass = <value>

#defaults to false
disable_request_signature_check = <value>

#If the applicationId provided with the request does not match an ID provided in this property, 
#the SpeechletServlet does not call any methods, but instead returns an HTTP error code (400 Bad Request). 
#Leaving this property blank turns off application ID verification. 
#defaults to blank
supported_application_ids = <value>

#defaults to 150
timestamp_tolerance = <value>