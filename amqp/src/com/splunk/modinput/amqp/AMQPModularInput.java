package com.splunk.modinput.amqp;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import com.splunk.modinput.Arg;
import com.splunk.modinput.Arg.DataType;
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
import com.splunk.modinput.transport.Transport;

public class AMQPModularInput extends ModularInput {

	private static final String DEFAULT_MESSAGE_HANDLER = "com.splunk.modinput.amqp.DefaultMessageHandler";

	public static void main(String[] args) {

		AMQPModularInput instance = new AMQPModularInput();
		instance.init(args);

	}

	boolean validateConnectionMode = false;

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("amqp://")) {

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

		String queueName = "";
		String host = "";
		int port = 5672; // default AMQP port
		String username = "";
		String password = "";
		String virtualHost = "";
		boolean useSsl = false;
		String routingKeyPattern = "";
		String exchangeName = "";
		int basicQos = 10;
		boolean ackMessages = false;
		boolean indexMessageEnvelope = false;
		boolean indexMessagePropertys = false;
		String messageHandlerImpl = DEFAULT_MESSAGE_HANDLER;
		String messageHandlerParams = "";

		Transport transport = getTransportInstance(params,stanzaName);
		
		for (Param param : params) {
			String value = param.getValue();
			if (value == null) {
				continue;
			}

			if (param.getName().equals("queue_name")) {
				queueName = param.getValue();
			} else if (param.getName().equals("hostname")) {
				host = param.getValue();
			} else if (param.getName().equals("port")) {
				try {
					port = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine port value, will revert to default value of 5672.");
				}
			} else if (param.getName().equals("username")) {
				username = param.getValue();
			} else if (param.getName().equals("password")) {
				password = param.getValue();
			} else if (param.getName().equals("virtual_host")) {
				virtualHost = param.getValue();
			} else if (param.getName().equals("use_ssl")) {
				try {
					useSsl = Boolean
							.parseBoolean(param.getValue().equals("1") ? "true"
									: "false");
				} catch (Exception e) {
					logger.error("Can't determine ssl mode");
				}
			} else if (param.getName().equals("routing_key_pattern")) {
				routingKeyPattern = param.getValue();
			} else if (param.getName().equals("exchange_name")) {
				exchangeName = param.getValue();
			} else if (param.getName().equals("basic_qos_limit")) {
				try {
					basicQos = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine basic qos value");
				}
			} else if (param.getName().equals("ack_messages")) {
				try {
					ackMessages = Boolean.parseBoolean(param.getValue().equals(
							"1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine ack messages mode");
				}
			} else if (param.getName().equals("index_message_envelope")) {
				try {
					indexMessageEnvelope = Boolean.parseBoolean(param
							.getValue().equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine index message envelope setting");
				}
			} else if (param.getName().equals("index_message_propertys")) {
				try {
					indexMessagePropertys = Boolean.parseBoolean(param
							.getValue().equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine index message propertys setting");
				}
			} else if (param.getName().equals("message_handler_impl")) {
				messageHandlerImpl = param.getValue();
			} else if (param.getName().equals("message_handler_params")) {
				messageHandlerParams = param.getValue();
			} else if (param.getName().equals("additional_jvm_propertys")) {
				setJVMSystemProperties(param.getValue());
			}

		}

		if (!isDisabled(stanzaName)) {
			MessageReceiver mr = new MessageReceiver(stanzaName, queueName,
					host, port, username, password, virtualHost, useSsl,
					routingKeyPattern, exchangeName, basicQos, ackMessages,
					indexMessageEnvelope, indexMessagePropertys,
					messageHandlerImpl, messageHandlerParams,transport);
			if (validationConnectionMode)
				mr.testConnectOnly();
			else
				mr.start();
		}
	}

	public class MessageReceiver extends Thread {

		String queueName;
		String host;
		int port;
		String username;
		String password;
		String virtualHost;
		boolean useSsl;
		String routingKeyPattern;
		String exchangeName;
		int basicQos;
		boolean ackMessages;
		boolean indexMessageEnvelope;
		boolean indexMessagePropertys;
		String stanzaName;
		AbstractMessageHandler messageHandler;

		boolean connected = false;

		Connection conn;
		Channel channel;

		public MessageReceiver(String stanzaName, String queueName,
				String host, int port, String username, String password,
				String virtualHost, boolean useSsl, String routingKeyPattern,
				String exchangeName, int basicQos, boolean ackMessages,
				boolean indexMessageEnvelope, boolean indexMessagePropertys,
				String messageHandlerImpl, String messageHandlerParams,Transport transport) {

			this.stanzaName = stanzaName;

			this.queueName = queueName;
			this.host = host;
			this.port = port;
			this.username = username;
			this.password = password;
			this.virtualHost = virtualHost;
			this.useSsl = useSsl;
			this.routingKeyPattern = routingKeyPattern;
			this.exchangeName = exchangeName;
			this.basicQos = basicQos;
			this.ackMessages = ackMessages;
			this.indexMessageEnvelope = indexMessageEnvelope;
			this.indexMessagePropertys = indexMessagePropertys;

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

		private void connect() throws Exception {

			ConnectionFactory connFactory = new ConnectionFactory();

			// currently just use the default settings
			if (this.useSsl)
				connFactory.useSslProtocol();

			connFactory.setHost(host);
			connFactory.setPort(port);

			if (username.length() > 0)
				connFactory.setUsername(username);
			if (password.length() > 0)
				connFactory.setPassword(password);
			if (virtualHost.length() > 0)
				connFactory.setVirtualHost(virtualHost);

			this.conn = connFactory.newConnection();
			this.channel = conn.createChannel();

			connected = true;

		}

		private void disconnect() {
			try {
				this.conn.close();
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

					channel.queueBind(queueName, exchangeName,
							routingKeyPattern);

					QueueingConsumer consumer = new QueueingConsumer(channel);
					channel.basicConsume(queueName, consumer);
					channel.basicQos(basicQos);

					while (true) {
						QueueingConsumer.Delivery delivery = consumer
								.nextDelivery();
						streamMessageEvent(delivery);
						if (ackMessages)
							channel.basicAck(delivery.getEnvelope()
									.getDeliveryTag(), false);
					}

				} catch (Exception e) {
					logger.error("Stanza " + stanzaName + " : "
							+ "Error running message receiver : "
							+ ModularInput.getStackTrace(e));
					disconnect();

				} finally {

				}
			}
		}

		private void streamMessageEvent(QueueingConsumer.Delivery delivery) {
			try {
				messageHandler.handleMessage(
						delivery.getBody(), delivery.getEnvelope(),
						delivery.getProperties(), this);
				
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error handling message : "
						+ ModularInput.getStackTrace(e));
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
				stanza.setName("amqp://" + item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			this.validateConnectionMode = true;
			doRun(input);

		} catch (Throwable t) {
			throw new Exception(
					"An AMQP connection can not be establised with the supplied propertys.Reason : "
							+ t.getMessage());
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("AMQP Messaging");
		scheme.setDescription("Index messages from an AMQP Broker");
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
		arg.setName("queue_name");
		arg.setTitle("Queue Name");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("hostname");
		arg.setTitle("Broker Host");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("port");
		arg.setTitle("Broker Port");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("username");
		arg.setTitle("Broker Username");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("password");
		arg.setTitle("Broker Password");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("virtual_host");
		arg.setTitle("Virtual Host");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("use_ssl");
		arg.setTitle("Use SSL");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		arg.setData_type(DataType.BOOLEAN);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("routing_key_pattern");
		arg.setTitle("Routing Key Pattern");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("exchange_name");
		arg.setTitle("Exchange Name");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("basic_qos_limit");
		arg.setTitle("Basic QOS Limit");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("ack_messages");
		arg.setTitle("ACk Messages");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		arg.setData_type(DataType.BOOLEAN);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("index_message_envelope");
		arg.setTitle("Index Message Envelope");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		arg.setData_type(DataType.BOOLEAN);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("index_message_propertys");
		arg.setTitle("Index Message Propertys");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		arg.setData_type(DataType.BOOLEAN);
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
		arg.setDescription("An implementation of the com.splunk.modinput.amqp.AbstractMessageHandler class.You would provide this if you required some custom handling/formatting of the messages you consume.Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/amqp_ta/bin/lib directory");
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
