package com.splunk.modinput.mqtt;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;
import java.util.Map;

import java.util.StringTokenizer;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import com.splunk.modinput.Arg;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.Item;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;

import com.splunk.modinput.Stanza;

import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;
import com.splunk.modinput.Scheme.StreamingMode;
import com.splunk.modinput.mqtt.AbstractMessageHandler;
import com.splunk.modinput.transport.Transport;

public class MQTTModularInput extends ModularInput {

	private static final String DEFAULT_MESSAGE_HANDLER = "com.splunk.modinput.mqtt.DefaultMessageHandler";

	public static void main(String[] args) {

		MQTTModularInput instance = new MQTTModularInput();
		instance.init(args);

	}

	boolean validateConnectionMode = false;

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("mqtt://")) {

					startMessageReceiverThread(name, stanza.getParams(),
							validateConnectionMode);

				}

				else {
					logger.error("Invalid stanza name : " + name);
					System.exit(2);
				}

			}
		} else {
			logger.error("Input is null");
			System.exit(2);
		}

	}

	private void startMessageReceiverThread(String stanzaName,
			List<Param> params, boolean validationConnectionMode)
			throws Exception {

		final int DEFAULT_TCP_PORT = 1883;
		final int DEFAULT_SSL_PORT = 8883;

		final String TCP_PROTOCOL = "tcp://";
		final String SSL_PROTOCOL = "ssl://";

		String topicName = "";
		String brokerHost = "";
		int brokerPort = DEFAULT_TCP_PORT;
		String brokerProtocol = TCP_PROTOCOL;

		String seperator = System.getProperty("file.separator");
		String reliableDeliveryDirectory = System.getenv("SPLUNK_HOME")+seperator+"etc"+seperator+"apps"+seperator+"mqtt_ta";
		
		boolean useSSL = false;

		String username = "";
		String password = "";
		String clientID = "splunk";

		int qos = 0;
		boolean cleanSession = false;
		int connectionTimeout = 30;
		int keepAliveInterval = 60;

		String messageHandlerImpl = DEFAULT_MESSAGE_HANDLER;
		String messageHandlerParams = "";

		Transport transport = getTransportInstance(params,stanzaName);
		
		for (Param param : params) {
			String value = param.getValue();
			if (value == null) {
				continue;
			}

			if (param.getName().equals("topic_name")) {
				topicName = param.getValue();
			} else if (param.getName().equals("broker_host")) {
				brokerHost = param.getValue();
			} else if (param.getName().equals("use_ssl")) {
				try {
					useSSL = Boolean
							.parseBoolean(param.getValue().equals("1") ? "true"
									: "false");
					if (useSSL) {
						brokerPort = DEFAULT_SSL_PORT;
						brokerProtocol = SSL_PROTOCOL;
					}
				} catch (Exception e) {
					logger.error("Can't determine use ssl setting");
				}
			} else if (param.getName().equals("broker_port")) {
				try {
					brokerPort = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine broker port, will revert to default value.");
				}
			} else if (param.getName().equals("username")) {
				username = param.getValue();
			} else if (param.getName().equals("password")) {
				password = param.getValue();
			} else if (param.getName().equals("client_id")) {
				clientID = param.getValue();
			} else if (param.getName().equals("qos")) {
				try {
					qos = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine qos, will revert to default value.");
				}
			} else if (param.getName().equals("reliable_delivery_dir")) {
				String val = param.getValue();
				if(val != null && val.length() > 0)
				  reliableDeliveryDirectory = val;
				
			} else if (param.getName().equals("clean_session")) {
				try {
					cleanSession = Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine clean session setting");
				}
			} else if (param.getName().equals("connection_timeout")) {
				try {
					connectionTimeout = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine connection timeout, will revert to default value.");
				}
			} else if (param.getName().equals("keepalive_interval")) {
				try {
					keepAliveInterval = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine keep alive interval, will revert to default value.");
				}
			}

			else if (param.getName().equals("message_handler_impl")) {
				messageHandlerImpl = param.getValue();
			} else if (param.getName().equals("message_handler_params")) {
				messageHandlerParams = param.getValue();
			} else if (param.getName().equals("additional_jvm_propertys")) {
				setJVMSystemProperties(param.getValue());
			}

		}

		String brokerURL = brokerProtocol + brokerHost + ":" + brokerPort;

		if (!isDisabled(stanzaName)) {
			MessageReceiver mr = new MessageReceiver(stanzaName, topicName,
					brokerURL, username, password, clientID, qos, cleanSession,
					connectionTimeout, keepAliveInterval, messageHandlerImpl,
					messageHandlerParams, reliableDeliveryDirectory,transport);
			if (validationConnectionMode)
				mr.testConnectOnly();
			else
				mr.start();
		}
	}

	public class MessageReceiver extends Thread implements MqttCallback {

		String stanzaName;
		String topicName;
		int qos;

		MqttClient client;
		MqttConnectOptions conOpt;
		MqttDefaultFilePersistence dataStore;

		boolean connected = false;
		AbstractMessageHandler messageHandler;

		public MessageReceiver(String stanzaName, String topicName,
				String brokerURL, String userName, String password,
				String clientID, int qos, boolean cleanSession,
				int connectionTimeout, int keepAliveInterval,
				String messageHandlerImpl, String messageHandlerParams,
				String reliableDeliveryDirectory,Transport transport) {

			this.stanzaName = stanzaName;

			this.topicName = topicName;
			this.qos = qos;

			try {
				messageHandler = (AbstractMessageHandler) Class.forName(
						messageHandlerImpl).newInstance();
				messageHandler.setParams(getParamMap(messageHandlerParams));
				messageHandler.setTransport(transport);
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Can't instantiate message handler : "
						+ messageHandlerImpl + " , "
						+ ModularInput.getStackTrace(e));
				System.exit(2);
			}

			try {
				// Construct the connection options object that contains
				// connection parameters
				// such as cleanSession and LWT
				conOpt = new MqttConnectOptions();
				conOpt.setCleanSession(cleanSession);
				conOpt.setConnectionTimeout(connectionTimeout);
				conOpt.setKeepAliveInterval(keepAliveInterval);

				if (password != null && password.length() > 0) {
					conOpt.setPassword(password.toCharArray());
				}
				if (userName != null && userName.length() > 0) {
					conOpt.setUserName(userName);
				}
				
				this.dataStore = new MqttDefaultFilePersistence(
						reliableDeliveryDirectory);
				// Construct an MQTT blocking mode client
				client = new MqttClient(brokerURL, clientID, dataStore);

				// Set this wrapper as the callback handler
				client.setCallback(this);

			} catch (MqttException e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Can't instantiate MQTT client : "
						+ ModularInput.getStackTrace(e));
				System.exit(2);
			}

		}

		private Map<String, String> getParamMap(
				String localResourceFactoryParams) {

			Map<String, String> map = new HashMap<String, String>();

			try {
				StringTokenizer st = new StringTokenizer(
						localResourceFactoryParams, ",");
				while (st.hasMoreTokens()) {
					StringTokenizer st2 = new StringTokenizer(st.nextToken(),
							"=");
					while (st2.hasMoreTokens()) {
						map.put(st2.nextToken(), st2.nextToken());
					}
				}
			} catch (Exception e) {

			}

			return map;

		}

		public void streamMessageEvent(String topic, MqttMessage message) {
			try {
				messageHandler.handleMessage(topic, message,
						this);
			
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error handling message : "
						+ ModularInput.getStackTrace(e));
			}
		}

		private void connect() throws Exception {

			client.connect(conOpt);

			connected = true;

		}

		private void disconnect() {
			try {
				client.disconnect();
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error disconnecting : "
						+ ModularInput.getStackTrace(e));
			}
			connected = false;

		}

		public void testConnectOnly() throws Exception {
			try {

				connect();
			} catch (Throwable t) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error connecting : " + ModularInput.getStackTrace(t));
			} finally {
				disconnect();
			}
		}

		/**
		 * @see MqttCallback#messageArrived(String, MqttMessage)
		 */
		public void messageArrived(String topic, MqttMessage message)
				throws MqttException {
			// Called when a message arrives from the server that matches any
			// subscription made by the client
			
			streamMessageEvent(topic, message);

		}

		/**
		 * @see MqttCallback#connectionLost(Throwable)
		 */
		public void connectionLost(Throwable cause) {
			logger.error("Stanza " + stanzaName + " : "
					+ "Connection lost : " + ModularInput.getStackTrace(cause));
		}

		/**
		 * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
		 */
		public void deliveryComplete(IMqttDeliveryToken token) {
		}

		public void run() {

			while (!isDisabled(stanzaName)) {
				while (!connected) {
					try {
						connect();

					} catch (Throwable t) {
						logger.error("Stanza " + stanzaName + " : "
								+ "Error connecting : "
								+ ModularInput.getStackTrace(t));
						try {
							// sleep 10 secs then try to reconnect
							Thread.sleep(10000);
						} catch (Exception exception) {

						}
					}
				}

				try {

					client.subscribe(topicName, qos);

				} catch (Throwable e) {
					logger.error("Stanza " + stanzaName + " : "
							+ "Error running message receiver : "
							+ ModularInput.getStackTrace(e));
					disconnect();

				} finally {

				}
			}
		}

	}

	@Override
	protected void doValidate(Validation val) {

		try {

			if (val != null) {
				validateConnection(val);
				/**
				 * List<Item> items = val.getItems(); for (Item item : items) {
				 * List<Param> params = item.getParams();
				 * 
				 * 
				 * for (Param param : params) { if
				 * (param.getName().equals("some_param")) {
				 * validateSomeParam(param.getValue()); } }
				 * 
				 * }
				 **/
			}
			System.exit(0);
		} catch (Exception e) {
			logger.error(e.getMessage());
			ValidationError error = new ValidationError("Validation Failed : "
					+ e.getMessage());
			sendValidationError(error);
			System.exit(2);
		}

	}

	private void validateConnection(Validation val) throws Exception {

		try {
			Input input = new Input();

			input.setCheckpoint_dir(val.getCheckpoint_dir());
			input.setServer_host(val.getServer_host());
			input.setServer_uri(val.getServer_uri());
			input.setSession_key(val.getSession_key());

			List<Item> items = val.getItems();
			List<Stanza> stanzas = new ArrayList<Stanza>();
			for (Item item : items) {
				Stanza stanza = new Stanza();
				stanza.setName("mqtt://" + item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			this.validateConnectionMode = true;
			doRun(input);

		} catch (Throwable t) {
			throw new Exception(
					"A MQTT connection can not be establised with the supplied propertys.Reason : "
							+ t.getMessage());
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("MQTT");
		scheme.setDescription("Index messages from a MQTT Broker");
		scheme.setUse_external_validation(true);
		scheme.setUse_single_instance(true);
		scheme.setStreaming_mode(StreamingMode.XML);

		Endpoint endpoint = new Endpoint();

		Arg arg = new Arg();
		arg.setName("name");
		arg.setTitle("Stanza Name");
		arg.setDescription("");

		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("topic_name");
		arg.setTitle("Topic Name");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("broker_host");
		arg.setTitle("Broker Host");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("broker_port");
		arg.setTitle("Broker Port");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("use_ssl");
		arg.setTitle("Use SSL");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("username");
		arg.setTitle("Username");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("password");
		arg.setTitle("Password");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("client_id");
		arg.setTitle("Client ID");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("qos");
		arg.setTitle("QOS");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("reliable_delivery_dir");
		arg.setTitle("Reliable Delivery Directory");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("clean_session");
		arg.setTitle("Clean Session");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("connection_timeout");
		arg.setTitle("Connection Timeout");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("keepalive_interval");
		arg.setTitle("Keep Alive Interval");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("additional_jvm_propertys");
		arg.setTitle("Additional JVM Propertys");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("message_handler_impl");
		arg.setTitle("Implementation class for a custom message handler");
		arg.setDescription("An implementation of the com.splunk.modinput.mqtt.AbstractMessageHandler class.You would provide this if you required some custom handling/formatting of the messages you consume.Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/mqtt_ta/bin/lib directory");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("message_handler_params");
		arg.setTitle("Implementation parameter string for the custom message handler");
		arg.setDescription("Parameter string in format 'key1=value1,key2=value2,key3=value3'. This gets passed to the implementation class to process.");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("output_type");
		arg.setTitle("Output Type");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("hec_port");
		arg.setTitle("HEC Port");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("hec_token");
		arg.setTitle("HEC Token");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("hec_poolsize");
		arg.setTitle("HEC Pool Size");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("hec_https");
		arg.setTitle("Use HTTPs");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("hec_batch_mode");
		arg.setTitle("Use batch mode");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("hec_max_batch_size_bytes");
		arg.setTitle("Max batch size in bytes");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("hec_max_batch_size_events");
		arg.setTitle("Max batch size in events");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("hec_max_inactive_time_before_batch_flush");
		arg.setTitle("Max inactive time before batch flush");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		scheme.setEndpoint(endpoint);

		return scheme;
	}

}
