package com.splunk.modinput.jms;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

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
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;
import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stanza;
import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;

public class JMSModularInput extends ModularInput {

	public enum DestinationType {

		QUEUE, TOPIC;
	}

	public static void main(String[] args) {

		JMSModularInput instance = new JMSModularInput();
		instance.init(args);

	}

	@Override
	protected void doRun(Input input) {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("jms://queue/")) {
					String destination = name.substring(12);
					startMessageReceiverThread(destination, stanza.getParams(),
							DestinationType.QUEUE);
				}

				else if (name != null && name.startsWith("jms://topic/")) {
					String destination = name.substring(12);
					startMessageReceiverThread(destination, stanza.getParams(),
							DestinationType.TOPIC);
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

	private void startMessageReceiverThread(String destination,
			List<Param> params, DestinationType type) {

		String jndiURL = "";
		String jndiContextFactory = "";
		String jndiUser = "";
		String jndiPass = "";
		String jmsConnectionFactory = "";
		boolean durable = false;
		boolean indexHeader = false;
		boolean indexProperties = false;
		String selector = "";

		for (Param param : params) {
			if (param.getName().equals("jndi_provider_url")) {
				jndiURL = param.getValue();
			} else if (param.getName().equals("jndi_initialcontext_factory")) {
				jndiContextFactory = param.getValue();
			} else if (param.getName().equals("jndi_user")) {
				jndiUser = param.getValue();
			} else if (param.getName().equals("jndi_pass")) {
				jndiPass = param.getValue();
			} else if (param.getName().equals("jms_connection_factory_name")) {
				jmsConnectionFactory = param.getValue();
			} else if (param.getName().equals("message_selector")) {
				selector = param.getValue();
			} else if (param.getName().equals("durable")) {
				try {
					durable = Boolean.parseBoolean(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine durability mode");
				}
			} else if (param.getName().equals("index_message_properties")) {
				try {
					indexProperties = Boolean.parseBoolean(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine index message properties mode");
				}
			} else if (param.getName().equals("index_message_header")) {
				try {
					indexHeader = Boolean.parseBoolean(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine index message header mode");
				}
			}
		}
		MessageReceiver mr = new MessageReceiver(destination, jndiURL,
				jndiContextFactory, jndiUser, jndiPass, jmsConnectionFactory,
				durable, type, indexProperties, indexHeader, selector);
		mr.start();

	}

	class MessageReceiver extends Thread {

		String jndiURL;
		String jndiContextFactory;
		String jndiUser;
		String jndiPass;
		String jmsConnectionFactory;
		String destination;
		boolean durable = false;
		DestinationType type;
		boolean indexHeader = false;
		boolean indexProperties = false;
		String selector;

		Connection connection = null;
		Session session = null;
		ConnectionFactory connFactory;
		Context ctx;
		Destination dest;
		MessageConsumer messageConsumer;

		boolean connected = false;

		public MessageReceiver(String destination, String jndiURL,
				String jndiContextFactory, String jndiUser, String jndiPass,
				String jmsConnectionFactory, boolean durable,
				DestinationType type, boolean indexProperties,
				boolean indexHeader, String selector) {

			this.destination = destination;
			this.jndiURL = jndiURL;
			this.jndiContextFactory = jndiContextFactory;
			this.jndiUser = jndiUser;
			this.jndiPass = jndiPass;
			this.jmsConnectionFactory = jmsConnectionFactory;
			this.durable = durable;
			this.type = type;
			this.indexHeader = indexHeader;
			this.indexProperties = indexProperties;
			this.selector = selector;
		}

		private void connect() throws Exception {

			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, this.jndiContextFactory);
			env.put(Context.PROVIDER_URL, this.jndiURL);
			if (jndiUser.length() > 0) {
				env.put(Context.SECURITY_PRINCIPAL, this.jndiUser);
			}
			if (jndiPass.length() > 0) {
				env.put(Context.SECURITY_CREDENTIALS, this.jndiPass);
			}

			ctx = new InitialContext(env);

			connFactory = (ConnectionFactory) ctx
					.lookup(this.jmsConnectionFactory);
			connection = connFactory.createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			dest = (Destination) ctx.lookup(destination);

			if (durable && type.equals(DestinationType.TOPIC)) {
				messageConsumer = session.createDurableSubscriber((Topic) dest,
						"splunk_jms_mod_input", selector, true);

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
					if (text != null && text.length() > 0)
						System.out.println(text);
				} catch (Exception e) {
					logger.error("Error running message receiver : "
							+ e.getMessage());

				} finally {
					disconnect();
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

			// TODO
			String body = "";
			if (message instanceof TextMessage) {
				body = ((TextMessage) message).getText();
			} else if (message instanceof BytesMessage) {
				body = ((BytesMessage) message).toString();
			} else if (message instanceof MapMessage) {
				body = ((MapMessage) message).toString();
			} else if (message instanceof ObjectMessage) {
				body = ((ObjectMessage) message).toString();
			} else if (message instanceof StreamMessage) {
				body = ((StreamMessage) message).toString();
			} else {
				body = message.toString();
			}

			event.addPair("msg_body", body);

			return event.toString();

		}
	}

	@Override
	protected void doValidate(Validation val) {

		try {
			// TODO actually do some proper validation
			System.exit(0);
		} catch (Exception e) {
			logger.error(e.getMessage());
			ValidationError error = new ValidationError("Validation Failed : "
					+ e.getMessage());
			sendValidationError(error);
			System.exit(2);
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("JMS Messaging");
		scheme.setDescription("Poll messages from queues and topics");
		scheme.setUse_external_validation(true);
		scheme.setUse_single_instance(true);

		Endpoint endpoint = new Endpoint();

		Arg arg = new Arg();
		arg.setName("name");
		arg.setTitle("JMS queue or topic");
		arg.setDescription("Enter the name precisely in this format : topic/${mytopic} or queue/${myqueue}");

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
		arg.setTitle("JMS Connection Factory Name");
		arg.setDescription("Name of the JMS Connection Factory.If you are using a specific message provider implmentation, ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/jms_ta/bin/lib directory");
		endpoint.addArg(arg);

		scheme.setEndpoint(endpoint);

		return scheme;
	}

}
