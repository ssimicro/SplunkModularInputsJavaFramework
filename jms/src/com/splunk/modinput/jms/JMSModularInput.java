package com.splunk.modinput.jms;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;

import com.splunk.modinput.Arg;
import com.splunk.modinput.Arg.DataType;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.Item;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;
import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stanza;
import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;
import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;
import com.splunk.modinput.Scheme.StreamingMode;

public class JMSModularInput extends ModularInput {

	public enum DestinationType {

		QUEUE, TOPIC;
	}

	public enum InitMode {

		JNDI, LOCAL;
	}

	public static void main(String[] args) {

		JMSModularInput instance = new JMSModularInput();
		instance.init(args);

	}

	@Override
	protected void doRun(Input input, boolean validationConnectionMode)
			throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("jms://queue/")) {

					startMessageReceiverThread(name, name.substring(12),
							stanza.getParams(), DestinationType.QUEUE,
							validationConnectionMode);

				}

				else if (name != null && name.startsWith("jms://topic/")) {

					startMessageReceiverThread(name, name.substring(12),
							stanza.getParams(), DestinationType.TOPIC,
							validationConnectionMode);
				} else {
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
			String destination, List<Param> params, DestinationType type,
			boolean validationConnectionMode) throws Exception {

		String jndiURL = "";
		String jndiContextFactory = "";
		String jndiUser = "";
		String jndiPass = "";
		String jmsConnectionFactory = "";
		boolean durable = true;
		boolean indexHeader = false;
		boolean indexProperties = false;
		String selector = "";
		InitMode initMode = InitMode.JNDI;
		String localResourceFactoryImpl = "";
		String localResourceFactoryParams = "";
		String clientID = "splunkjms";
		String userJNDIProperties = "";

		
		for (Param param : params) {
			String value = param.getValue();
			if(value == null){
				continue;
			}
			
			if (param.getName().equals("jndi_provider_url")) {
				jndiURL = param.getValue() ;
			} else if (param.getName().equals("jndi_initialcontext_factory")) {
				jndiContextFactory = param.getValue();
			} else if (param.getName().equals("jndi_user")) {
				jndiUser = param.getValue();
			} else if (param.getName().equals("jndi_pass")) {
				jndiPass = param.getValue();
			} else if (param.getName().equals("user_jndi_properties")) {
				userJNDIProperties = param.getValue();
			} else if (param.getName().equals("jms_connection_factory_name")) {
				jmsConnectionFactory = param.getValue();
			} else if (param.getName().equals("message_selector")) {
				selector = param.getValue();
			} else if (param.getName().equals(
					"local_init_mode_resource_factory_impl")) {
				localResourceFactoryImpl = param.getValue();
			} else if (param.getName().equals(
					"local_init_mode_resource_factory_params")) {
				localResourceFactoryParams = param.getValue();
			} else if (param.getName().equals("client_id")) {
				clientID = param.getValue();
			} else if (param.getName().equals("init_mode")) {
				String val = param.getValue();
				if (val.equalsIgnoreCase("jndi")) {
					initMode = InitMode.JNDI;
				} else if (val.equalsIgnoreCase("local")) {
					initMode = InitMode.LOCAL;
				}
			} else if (param.getName().equals("durable")) {
				try {
					durable = Boolean
							.parseBoolean(param.getValue().equals("1") ? "true"
									: "false");
				} catch (Exception e) {
					logger.error("Can't determine durability mode");
				}
			} else if (param.getName().equals("index_message_properties")) {
				try {
					indexProperties = Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine index message properties mode");
				}
			} else if (param.getName().equals("index_message_header")) {
				try {
					indexHeader = Boolean.parseBoolean(param.getValue().equals(
							"1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine index message header mode");
				}
			}
		}
		
		MessageReceiver mr = new MessageReceiver(stanzaName, destination,
				jndiURL, jndiContextFactory, jndiUser, jndiPass,
				jmsConnectionFactory, durable, type, indexProperties,
				indexHeader, selector, initMode, localResourceFactoryImpl,
				localResourceFactoryParams, userJNDIProperties, clientID);
		if (validationConnectionMode)
			mr.testConnectOnly();
		else
			mr.start();

	}

	class MessageReceiver extends Thread {

		String jndiURL;
		String jndiContextFactory;
		String jndiUser;
		String jndiPass;
		String jmsConnectionFactory;
		String destination;
		boolean durable;;
		DestinationType type;
		InitMode initMode;
		boolean indexHeader;
		boolean indexProperties;
		String selector;
		String clientID;
		String stanzaName;

		Connection connection = null;
		Session session = null;
		ConnectionFactory connFactory;
		Context ctx;
		Destination dest;
		MessageConsumer messageConsumer;
		LocalJMSResourceFactory localFactory;

		Map<String, String> userJNDIProperties = null;

		boolean connected = false;
		

		public MessageReceiver(String stanzaName, String destination,
				String jndiURL, String jndiContextFactory, String jndiUser,
				String jndiPass, String jmsConnectionFactory, boolean durable,
				DestinationType type, boolean indexProperties,
				boolean indexHeader, String selector, InitMode initMode,
				String localResourceFactoryImpl,
				String localResourceFactoryParams,
				String userJNDIPropertiesString, String clientID) {

			this.destination = destination;
			this.jndiURL = jndiURL;
			this.jndiContextFactory = jndiContextFactory;
			this.jndiUser = jndiUser;
			this.jndiPass = jndiPass;
			this.jmsConnectionFactory = jmsConnectionFactory;
			this.durable = durable;
			this.clientID = clientID;
			this.type = type;
			this.indexHeader = indexHeader;
			this.indexProperties = indexProperties;
			this.selector = selector;
			this.initMode = initMode;
			this.stanzaName = stanzaName;
			

			if (userJNDIPropertiesString != null
					&& userJNDIPropertiesString.length() > 0) {
				userJNDIProperties = getParamMap(userJNDIPropertiesString);
			}
			if (initMode.equals(InitMode.LOCAL)) {
				try {
					localFactory = (LocalJMSResourceFactory) Class.forName(
							localResourceFactoryImpl).newInstance();
					localFactory
							.setParams(getParamMap(localResourceFactoryParams));
				} catch (Exception e) {

				}
			}
		}

		private Map<String, String> getParamMap(
				String localResourceFactoryParams) {

			Map<String, String> map = new HashMap<String, String>();

			try {
				StringTokenizer st = new StringTokenizer(
						localResourceFactoryParams, ",");
				while (st.hasMoreTokens()) {
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
					while (st2.hasMoreTokens()) {
						map.put(st2.nextToken(), st2.nextToken());
					}
				}
			} catch (Exception e) {
				
			}

			return map;

		}

		private void connect() throws Exception {

			if (initMode.equals(InitMode.JNDI)) {

				Hashtable<String, String> env = new Hashtable<String, String>();
				env.put(Context.INITIAL_CONTEXT_FACTORY,
						this.jndiContextFactory);
				env.put(Context.PROVIDER_URL, this.jndiURL);
				if (jndiUser.length() > 0) {
					env.put(Context.SECURITY_PRINCIPAL, this.jndiUser);
				}
				if (jndiPass.length() > 0) {
					env.put(Context.SECURITY_CREDENTIALS, this.jndiPass);
				}
				if (userJNDIProperties != null) {
					env.putAll(userJNDIProperties);
				}

				ctx = new InitialContext(env);

				connFactory = (ConnectionFactory) ctx
						.lookup(this.jmsConnectionFactory);
				dest = (Destination) ctx.lookup(destination);
			}

			else if (initMode.equals(InitMode.LOCAL)) {
				if (localFactory == null) {
					throw new Exception("LocalFactory Object is null");
				}
				connFactory = localFactory.createConnectionFactory();
				if (type.equals(DestinationType.QUEUE))
					dest = localFactory.createQueue();
				else if (type.equals(DestinationType.TOPIC))
					dest = localFactory.createTopic();
			}

			connection = connFactory.createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			
				if (durable && type.equals(DestinationType.TOPIC)) {

					try {
						messageConsumer = session.createDurableSubscriber(
								(Topic) dest, clientID, selector, true);
					} catch (Exception e) {
						messageConsumer = session
								.createConsumer(dest, selector);
					}

				} else {

					messageConsumer = session.createConsumer(dest, selector);

				}

				connection.start();
				connected = true;
			
		}

		private void disconnect() {
			try {
				if (session != null)
					session.close();
				if (connection != null)
					connection.close();
			} catch (Exception e) {

			}
			connected = false;

		}

		public void testConnectOnly() throws Exception {
			try {
				
				connect();
			} 
			finally {
				disconnect();
			}
		}

		public void run() {

			while (true) {
				while (!connected) {
					try {
						connect();

					} catch (Exception e) {

						try {
							// sleep 10 secs then try to reconnect
							Thread.sleep(10000);
						} catch (Exception exception) {
						}
					}
				}

				try {
					// block and wait for message
					Message message = messageConsumer.receive();
					String text = getSplunkFormattedMessage(message);
					if (text != null && text.length() > 0) {
						Stream stream = new Stream();

						StreamEvent event = new StreamEvent();
						event.setData(text);
						event.setStanza(stanzaName);
						ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();
						list.add(event);
						stream.setEvents(list);
						marshallObjectToXML(stream);
					}
				} catch (Exception e) {
					logger.error("Error running message receiver : "
							+ e.getMessage());
					disconnect();

				} finally {

				}
			}
		}

		private String getSplunkFormattedMessage(Message message)
				throws Exception {

			SplunkLogEvent event = new SplunkLogEvent(type + "_msg_received",
					message.getJMSMessageID(), true, true);

			event.addPair("msg_dest", destination);

			if (indexHeader) {
				// JMS Message Header fields
				event.addPair("msg_header_timestamp", message.getJMSTimestamp());
				event.addPair("msg_header_correlation_id",
						message.getJMSCorrelationID());
				event.addPair("msg_header_delivery_mode",
						message.getJMSDeliveryMode());
				event.addPair("msg_header_expiration",
						message.getJMSExpiration());
				event.addPair("msg_header_priority", message.getJMSPriority());
				event.addPair("msg_header_redelivered",
						message.getJMSRedelivered());
				event.addPair("msg_header_type", message.getJMSType());
			}

			if (indexProperties) {
				// JMS Message Properties
				Enumeration propertyNames = message.getPropertyNames();
				while (propertyNames.hasMoreElements()) {
					String name = (String) propertyNames.nextElement();
					Object property = message.getObjectProperty(name);
					event.addPair("msg_property_" + name, property);
				}
			}

			// JMS Message Body

			String body = "";

			try {
				if (message instanceof TextMessage) {
					body = ((TextMessage) message).getText();
				} else if (message instanceof BytesMessage) {
					try {
						body = ((BytesMessage) message).readUTF();
					} catch (Exception e) {
						body = "binary message";
					}

				} else if (message instanceof StreamMessage) {
					body = "binary stream message";
				} else if (message instanceof ObjectMessage) {
					body = ((ObjectMessage) message).getObject().toString();
				} else if (message instanceof MapMessage) {
					Enumeration names = ((MapMessage) message).getMapNames();
					while (names.hasMoreElements()) {
						String name = (String) names.nextElement();
						Object value = ((MapMessage) message).getObject(name);
						body += name + "=" + value;
						if (names.hasMoreElements())
							body += ",";
					}
					body = ((MapMessage) message).toString();
				} else {
					body = message.toString();
				}
			} catch (Exception e) {

			}

			event.addPair("msg_body", body);

			return event.toString();

		}
	}

	@Override
	protected void doValidate(Validation val) {

		try {

			if (val != null) {
				validateConnection(val);
				List<Item> items = val.getItems();
				for (Item item : items) {
					List<Param> params = item.getParams();

					for (Param param : params) {
						if (param.getName().equals("init_mode")) {
							validateInitMode(param.getValue());
						}
					}
				}
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
				validateName(item.getName());
				Stanza stanza = new Stanza();
				stanza.setName("jms://"+item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			doRun(input, true);

		} catch (Throwable t) {
			throw new Exception(
					"A JMS connection can not be establised with the supplied propertys.Reason : "+t.getMessage());
		}

	}

	private void validateInitMode(String value) throws Exception {
		if (!(value.equalsIgnoreCase("jndi") || value.equalsIgnoreCase("local"))) {
			throw new Exception(
					"Initialisation mode must be 'local' or 'jndi'." + value
							+ " is not valid");
		}

	}

	private void validateName(String value) throws Exception {
		if (!(value.startsWith("topic/") || value.startsWith("queue/"))) {
			throw new Exception(
					"Queue/Topic name must start with the prefix 'topic/' or 'queue/'."
							+ value + " is not valid");
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("JMS Messaging");
		scheme.setDescription("Poll messages from queues and topics");
		scheme.setUse_external_validation(true);
		scheme.setUse_single_instance(true);
		scheme.setStreaming_mode(StreamingMode.XML);

		Endpoint endpoint = new Endpoint();

		Arg arg = new Arg();
		arg.setName("name");
		arg.setTitle("JMS queue or topic");
		arg.setDescription("Enter the name precisely in this format : topic/mytopic or queue/myqueue");

		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("jndi_initialcontext_factory");
		arg.setTitle("JNDI Initial Context Factory Name");
		arg.setDescription("Name of the initial context factory.If you are using a specific context factory implmentation, ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/jms_ta/bin/lib directory");
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("jndi_provider_url");
		arg.setTitle("JNDI Provider URL");
		arg.setDescription("URL to the JNDI Server");
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("jndi_user");
		arg.setTitle("JNDI username");
		arg.setDescription("JNDI Username to authenticate with");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("jndi_pass");
		arg.setTitle("JNDI password");
		arg.setDescription("JNDI Password  to authenticate with");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("message_selector");
		arg.setTitle("Message Selector Pattern");
		arg.setDescription("Only messages whose header and property values match the selector are delivered.For syntax details , refer to http://docs.oracle.com/javaee/5/api/javax/jms/Message.html");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("durable");
		arg.setTitle("Topic Durability");
		arg.setDescription("If this is a topic you can specify the message consumer to be durable");
		arg.setRequired_on_create(false);
		arg.setData_type(DataType.BOOLEAN);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("client_id");
		arg.setTitle("Client ID");
		arg.setDescription("Specify a unique client id.This is used for durable topic subscription");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("init_mode");
		arg.setTitle("Initialisation Mode");
		arg.setDescription("Initialise connection objects via JNDI or Local instantiation");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("local_init_mode_resource_factory_impl");
		arg.setTitle("Implementation class for local JMS resource instantiation");
		arg.setDescription("An implementation of the com.splunk.modinput.jms.LocalJMSResourceFactory interface.Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/jms_ta/bin/lib directory");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("local_init_mode_resource_factory_params");
		arg.setTitle("Implementation parameter string for local JMS resource instantiation");
		arg.setDescription("Parameter string in format 'key1=value1,key2=value2,key3=value3'. This gets passed to the implementation class to process.");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("index_message_header");
		arg.setTitle("Index Message Header Fields");
		arg.setDescription("Whether or not to index the message header fields");
		arg.setRequired_on_create(false);
		arg.setData_type(DataType.BOOLEAN);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("index_message_properties");
		arg.setTitle("Index Message Properties");
		arg.setDescription("Whether or not to index the message property values");
		arg.setRequired_on_create(false);
		arg.setData_type(DataType.BOOLEAN);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("jms_connection_factory_name");
		arg.setTitle("JMS Connection Factory JNDI Name");
		arg.setDescription("JNDI name of the JMS Connection Factory.If you are using a specific message provider implmentation, ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/jms_ta/bin/lib directory");
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("user_jndi_properties");
		arg.setTitle("User defined JNDI properties");
		arg.setRequired_on_create(false);
		arg.setDescription("User specific JNDI properties string in format 'key1=value1,key2=value2,key3=value3'");
		endpoint.addArg(arg);

		scheme.setEndpoint(endpoint);

		return scheme;
	}

}
