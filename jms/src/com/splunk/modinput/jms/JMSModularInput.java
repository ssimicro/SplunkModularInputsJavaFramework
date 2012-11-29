package com.splunk.modinput.jms;

import java.util.Hashtable;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import com.splunk.modinput.Arg;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;
import com.splunk.modinput.Stanza;
import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;

public class JMSModularInput extends ModularInput {

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
					startMessageReceiverThread(destination, stanza.getParams());
				}

				else if (name != null && name.startsWith("jms://topic/")) {
					String destination = name.substring(12);
					startMessageReceiverThread(destination, stanza.getParams());
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
			List<Param> params) {

		String jndiURL = "";
		String jndiContextFactory = "";
		String jndiUser = "";
		String jndiPass = "";
		String jmsConnectionFactory = "";

		for (Param param : params) {
			if (param.getName().equals("jndi_initialcontext_factory")) {
				jndiURL = param.getValue();
			} else if (param.getName().equals("jndi_provider_url")) {
				jndiContextFactory = param.getValue();
			} else if (param.getName().equals("jndi_user")) {
				jndiUser = param.getValue();
			} else if (param.getName().equals("jndi_pass")) {
				jndiPass = param.getValue();
			} else if (param.getName().equals("jms_connection_factory_name")) {
				jmsConnectionFactory = param.getValue();
			}
		}
		MessageReceiver mr = new MessageReceiver(destination, jndiURL,
				jndiContextFactory, jndiUser, jndiPass, jmsConnectionFactory);
		mr.start();

	}

	class MessageReceiver extends Thread {

		String jndiURL;
		String jndiContextFactory;
		String jndiUser;
		String jndiPass;
		String jmsConnectionFactory;
		String destination;

		public MessageReceiver(String destination, String jndiURL,
				String jndiContextFactory, String jndiUser, String jndiPass,
				String jmsConnectionFactory) {

			this.destination = destination;
			this.jndiURL = jndiURL;
			this.jndiContextFactory = jndiContextFactory;
			this.jndiUser = jndiUser;
			this.jndiPass = jndiPass;
			this.jmsConnectionFactory = jmsConnectionFactory;
		}

		public void run() {

			Connection connection = null;
			Session session = null;
			ConnectionFactory connFactory;
			Context ctx;
			Destination queue;
			MessageConsumer messageConsumer;

			try {
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

				ctx = new InitialContext(env);

				connFactory = (ConnectionFactory) ctx
						.lookup(this.jmsConnectionFactory);
				connection = connFactory.createConnection();
				session = connection.createSession(false,
						Session.AUTO_ACKNOWLEDGE);

				queue = (Destination) ctx.lookup(destination);

				messageConsumer = session.createConsumer(queue);
				connection.start();

				while (true) {
					Message message = messageConsumer.receive();
					String text;
					if (message instanceof TextMessage) {
						text = ((TextMessage) message).getText();
					} else {
						text = message.toString();
					}
					if (text != null)
						System.out.println(text);
				}
			} catch (Exception e) {
				logger.error("Error running message receiver : "
						+ e.getMessage());
				System.exit(2);

			} finally {
				try {
					if (session != null)
						session.close();
					if (connection != null)
						connection.close();
				} catch (Exception e) {

				}
			}
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
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("jndi_pass");
		arg.setTitle("JNDI password");
		arg.setDescription("JNDI Password  to authenticate with");
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
