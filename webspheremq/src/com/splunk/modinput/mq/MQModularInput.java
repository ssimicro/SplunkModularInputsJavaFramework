package com.splunk.modinput.mq;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;
import java.util.Map;

import java.util.StringTokenizer;

import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.ibm.mq.headers.pcf.PCFMessage;

import com.splunk.modinput.Arg;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.Item;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;

import com.splunk.modinput.Stanza;
import com.splunk.modinput.Stream;
import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;
import com.splunk.modinput.Scheme.StreamingMode;

/**
 * Splunk Modular Input for monitoring Websphere MQ
 * 
 * @author ddallimore
 * 
 */
public class MQModularInput extends ModularInput {

	private static final String DEFAULT_EVENT_HANDLER = "com.splunk.modinput.mq.DefaultEventHandler";

	public static void main(String[] args) {

		MQModularInput instance = new MQModularInput();
		instance.init(args);

	}

	boolean validateConnectionMode = false;

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("mq://")) {

					startMQInquiryThread(name, stanza.getParams(),
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

	private void startMQInquiryThread(String stanzaName, List<Param> params,
			boolean validationConnectionMode) throws Exception {

		String mqHost = "localhost";
		int mqPort = 1414;
		String mqUsername = "";
		String mqPassword = "";
		String mqManagerChannel = "SYSTEM.DEF.SVRCONN";
		String mqManagerName = "";

		String eventHandlerImpl = DEFAULT_EVENT_HANDLER;
		String eventHandlerParams = "";

		int pollingFrequency = 60;

		boolean inquireQueues = false;
		boolean inquireTopics = false;
		boolean inquireChannels = false;
		boolean inquireListeners = false;
		boolean inquireSubscriptions = false;
		boolean inquireServices = false;
		boolean inquireCurrentQueueManager = false;

		String sslCipherSuite = "";
		boolean sslFipsRequired = false;
		boolean useSSL = false;

		for (Param param : params) {
			String value = param.getValue();
			if (value == null) {
				continue;
			}

			if (param.getName().equals("host")) {
				mqHost = param.getValue();
			} else if (param.getName().equals("port")) {
				try {
					mqPort = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine MQ Manager port, will revert to default value.");
				}
			} else if (param.getName().equals("username")) {
				mqUsername = param.getValue();
			} else if (param.getName().equals("password")) {
				mqPassword = param.getValue();
			} else if (param.getName().equals("manager_name")) {
				mqManagerName = param.getValue();
			} else if (param.getName().equals("channel_name")) {
				mqManagerChannel = param.getValue();
			} else if (param.getName().equals("event_handler_impl")) {
				eventHandlerImpl = param.getValue();
			} else if (param.getName().equals("event_handler_params")) {
				eventHandlerParams = param.getValue();
			} else if (param.getName().equals("additional_jvm_properties")) {
				setJVMSystemProperties(param.getValue());
			} else if (param.getName().equals("polling_frequency")) {
				try {
					pollingFrequency = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine polling frequency, will revert to default value.");
				}

			} else if (param.getName().equals("inquire_queues")) {
				try {
					inquireQueues = Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for inquire_queues");
				}
			} else if (param.getName().equals("inquire_topics")) {
				try {
					inquireTopics = Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for inquire_topics");
				}
			} else if (param.getName().equals("inquire_channels")) {
				try {
					inquireChannels = Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for inquire_channels");
				}
			} else if (param.getName().equals("inquire_listeners")) {
				try {
					inquireListeners = Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for inquire_listeners");
				}
			} else if (param.getName().equals("inquire_subscriptions")) {
				try {
					inquireSubscriptions = Boolean.parseBoolean(param
							.getValue().equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for inquire_subscriptions");
				}
			} else if (param.getName().equals("inquire_services")) {
				try {
					inquireServices = Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for inquire_services");
				}
			} else if (param.getName().equals("inquire_current_queuemanager")) {
				try {
					inquireCurrentQueueManager = Boolean.parseBoolean(param
							.getValue().equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for inquire_current_queuemanager");
				}
			} else if (param.getName().equals("use_ssl")) {
				try {
					useSSL = Boolean
							.parseBoolean(param.getValue().equals("1") ? "true"
									: "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for use_ssl");
				}
			} else if (param.getName().equals("ssl_fips_required")) {
				try {
					sslFipsRequired = Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false");
				} catch (Exception e) {
					logger.error("Can't determine setting for ssl_fips_required");
				}
			} else if (param.getName().equals("ssl_cipher_suite")) {
				sslCipherSuite = param.getValue();
			}

		}
		if (!isDisabled(stanzaName)) {
			MQPoller mr = new MQPoller(stanzaName, mqHost, mqPort, mqUsername,
					mqPassword, mqManagerChannel, mqManagerName,
					eventHandlerImpl, eventHandlerParams, pollingFrequency,
					inquireQueues, inquireTopics, inquireChannels,
					inquireListeners, inquireSubscriptions, inquireServices,
					inquireCurrentQueueManager, useSSL, sslCipherSuite,
					sslFipsRequired);
			if (validationConnectionMode)
				mr.testConnectOnly();
			else
				mr.start();
		}
	}

	public class MQPoller extends Thread {

		String stanzaName;
		String mqHost;
		int mqPort;
		String mqUsername;
		String mqPassword;
		String mqManagerChannel;
		String mqManagerName;

		int pollingFrequency;

		boolean inquireQueues = false;
		boolean inquireTopics = false;
		boolean inquireChannels = false;
		boolean inquireListeners = false;
		boolean inquireSubscriptions = false;
		boolean inquireServices = false;
		boolean inquireCurrentQueueManager = false;

		boolean connected = false;
		AbstractEventHandler eventHandler;

		MQQueueManager qMgr;
		PCFMessageAgent agent;

		String sslCipherSuite = "";
		boolean sslFipsRequired = false;
		boolean useSSL = false;

		public MQPoller(String stanzaName, String mqHost, int mqPort,
				String mqUsername, String mqPassword, String mqManagerChannel,
				String mqManagerName, String eventHandlerImpl,
				String eventHandlerParams, int pollingFrequency,
				boolean inquireQueues, boolean inquireTopics,
				boolean inquireChannels, boolean inquireListeners,
				boolean inquireSubscriptions, boolean inquireServices,
				boolean inquireCurrentQueueManager, boolean useSSL,
				String sslCipherSuite, boolean sslFipsRequired) {

			this.stanzaName = stanzaName;
			this.mqHost = mqHost;
			this.mqPort = mqPort;
			this.mqUsername = mqUsername;
			this.mqPassword = mqPassword;
			this.mqManagerChannel = mqManagerChannel;
			this.mqManagerName = mqManagerName;

			this.pollingFrequency = pollingFrequency;
			this.inquireQueues = inquireQueues;
			this.inquireTopics = inquireTopics;
			this.inquireChannels = inquireChannels;
			this.inquireListeners = inquireListeners;
			this.inquireSubscriptions = inquireSubscriptions;
			this.inquireServices = inquireServices;
			this.inquireCurrentQueueManager = inquireCurrentQueueManager;

			this.sslCipherSuite = sslCipherSuite;
			this.sslFipsRequired = sslFipsRequired;
			this.useSSL = useSSL;

			try {
				eventHandler = (AbstractEventHandler) Class.forName(
						eventHandlerImpl).newInstance();
				eventHandler.setParams(getParamMap(eventHandlerParams));
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Can't instantiate event handler : "
						+ eventHandlerImpl + " , "
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

		public void streamEvent(Map<Object, Object> eventValues) {
			try {
				Stream stream = eventHandler.handleMessage(eventValues, this);
				marshallObjectToXML(stream);
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error handling MQ event : "
						+ ModularInput.getStackTrace(e));
			}
		}

		private void connect() throws Exception {

			MQEnvironment.hostname = this.mqHost;
			MQEnvironment.port = this.mqPort;
			MQEnvironment.channel = this.mqManagerChannel;

			if (this.mqUsername.equals("") && mqPassword.equals("")) {
				MQEnvironment.userID = this.mqUsername;
				MQEnvironment.password = this.mqPassword;

			}

			if (useSSL) {
				MQEnvironment.sslCipherSuite = sslCipherSuite;
				MQEnvironment.sslFipsRequired = sslFipsRequired;
			}

			this.qMgr = new MQQueueManager(this.mqManagerName);

			this.agent = new PCFMessageAgent();
			agent.connect(this.qMgr);

			connected = true;

		}

		private void disconnect() {
			try {
				this.agent.disconnect();
				this.qMgr.disconnect();
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

					if (inquireChannels)
						inquireChannels();
					if (inquireQueues)
						inquireQueues();
					if (inquireListeners)
						inquireListeners();
					if (inquireTopics)
						inquireTopics();
					if (inquireSubscriptions)
						inquireSubscriptions();
					if (inquireServices)
						inquireServices();
					if (inquireCurrentQueueManager)
						inquireCurrentQueueManager();

					try {
						Thread.sleep(pollingFrequency * 1000);
					} catch (Exception e) {

					}

				} catch (Throwable e) {
					logger.error("Stanza " + stanzaName + " : "
							+ "Error running MQ Poller: "
							+ ModularInput.getStackTrace(e));
					disconnect();

				} finally {

				}
			}
		}

		private void inquireCurrentQueueManager() {
			PCFMessage request;
			PCFMessage[] response;
			int[] attrs = { MQConstants.MQIACF_Q_MGR_STATUS,
					MQConstants.MQIACF_CONNECTION_COUNT,
					MQConstants.MQCACF_Q_MGR_START_DATE,
					MQConstants.MQCACF_Q_MGR_START_TIME,
					MQConstants.MQCA_Q_MGR_NAME };
			request = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q_MGR_STATUS);
			request.addParameter(MQConstants.MQIACF_Q_MGR_STATUS_ATTRS, attrs);
			try {
				response = agent.send(request);
				for (int i = 0; i < response.length; i++) {

					Map<Object, Object> event = new HashMap<Object, Object>();

					event.put("inquiry_type", "queuemanager");
					event.put("timestamp", System.currentTimeMillis());

					event.put("status", response[i]
							.getParameterValue(MQConstants.MQIACF_Q_MGR_STATUS));
					event.put(
							"connection_count",
							response[i]
									.getParameterValue(MQConstants.MQIACF_CONNECTION_COUNT));
					event.put(
							"start_date",
							response[i]
									.getParameterValue(MQConstants.MQCACF_Q_MGR_START_DATE));
					event.put(
							"start_time",
							response[i]
									.getParameterValue(MQConstants.MQCACF_Q_MGR_START_TIME));
					event.put("manager_name", response[i]
							.getParameterValue(MQConstants.MQCA_Q_MGR_NAME));

					streamEvent(event);
				}
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error inquiring queue manager: "
						+ ModularInput.getStackTrace(e));
			}

		}

		private void inquireServices() {
			PCFMessage request;
			PCFMessage[] response;
			int[] attrs = { MQConstants.MQCA_SERVICE_NAME,
					MQConstants.MQIACF_SERVICE_STATUS,
					MQConstants.MQCACF_SERVICE_START_DATE,
					MQConstants.MQCACF_SERVICE_START_TIME };
			request = new PCFMessage(MQConstants.MQCMD_INQUIRE_SERVICE_STATUS);
			request.addParameter(MQConstants.MQCA_SERVICE_NAME, "*");
			request.addParameter(MQConstants.MQIACF_SERVICE_STATUS_ATTRS, attrs);
			try {
				response = agent.send(request);
				for (int i = 0; i < response.length; i++) {

					Map<Object, Object> event = new HashMap<Object, Object>();

					event.put("inquiry_type", "service");
					event.put("timestamp", System.currentTimeMillis());

					event.put("service_name", response[i]
							.getParameterValue(MQConstants.MQCA_SERVICE_NAME));
					event.put(
							"status",
							response[i]
									.getParameterValue(MQConstants.MQIACF_SERVICE_STATUS));
					event.put(
							"start_date",
							response[i]
									.getParameterValue(MQConstants.MQCACF_SERVICE_START_DATE));
					event.put(
							"start_time",
							response[i]
									.getParameterValue(MQConstants.MQCACF_SERVICE_START_TIME));

					streamEvent(event);
				}
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error inquiring services: "
						+ ModularInput.getStackTrace(e));
			}

		}

		private void inquireSubscriptions() {
			PCFMessage request;
			PCFMessage[] response;
			int[] attrs = { MQConstants.MQCA_TOPIC_STRING,
					MQConstants.MQCACF_SUB_USER_ID,
					MQConstants.MQCACF_LAST_MSG_DATE,
					MQConstants.MQCACF_LAST_MSG_TIME,
					MQConstants.MQIACF_MESSAGE_COUNT,
					MQConstants.MQIACF_SUB_TYPE };

			request = new PCFMessage(MQConstants.MQCMD_INQUIRE_SUB_STATUS);
			request.addParameter(MQConstants.MQCACF_SUB_NAME, "*");
			request.addParameter(MQConstants.MQIACF_SUB_TYPE,
					MQConstants.MQSUBTYPE_ALL);
			request.addParameter(MQConstants.MQIACF_SUB_STATUS_ATTRS, attrs);

			try {
				response = agent.send(request);
				for (int i = 0; i < response.length; i++) {

					Map<Object, Object> event = new HashMap<Object, Object>();

					event.put("inquiry_type", "subscriptions");
					event.put("timestamp", System.currentTimeMillis());
					event.put("topic_string", response[i]
							.getParameterValue(MQConstants.MQCA_TOPIC_STRING));
					event.put("sub_user_id", response[i]
							.getParameterValue(MQConstants.MQCACF_SUB_USER_ID));
					event.put(
							"last_msg_date",
							response[i]
									.getParameterValue(MQConstants.MQCACF_LAST_MSG_DATE));
					event.put(
							"last_msg_time",
							response[i]
									.getParameterValue(MQConstants.MQCACF_LAST_MSG_TIME));
					event.put(
							"msg_count",
							response[i]
									.getParameterValue(MQConstants.MQIACF_MESSAGE_COUNT));
					event.put("sub_type", response[i]
							.getParameterValue(MQConstants.MQIACF_SUB_TYPE));
					streamEvent(event);
				}
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error inquiring subscriptions: "
						+ ModularInput.getStackTrace(e));
			}

		}

		private void inquireTopics() {
			PCFMessage request;
			PCFMessage[] response;
			request = new PCFMessage(MQConstants.MQCMD_INQUIRE_TOPIC_STATUS);
			request.addParameter(MQConstants.MQCA_TOPIC_STRING, "#");
			request.addParameter(MQConstants.MQIACF_TOPIC_STATUS_TYPE,
					MQConstants.MQIACF_TOPIC_STATUS);

			try {
				response = agent.send(request);
				for (int i = 0; i < response.length; i++) {

					Map<Object, Object> event = new HashMap<Object, Object>();

					event.put("inquiry_type", "topic");
					event.put("timestamp", System.currentTimeMillis());

					event.put("topic_string", response[i]
							.getParameterValue(MQConstants.MQCA_TOPIC_STRING));
					event.put("pub_count", response[i]
							.getParameterValue(MQConstants.MQIA_PUB_COUNT));
					event.put("sub_count", response[i]
							.getParameterValue(MQConstants.MQIA_SUB_COUNT));
					streamEvent(event);
				}
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error inquiring topics: "
						+ ModularInput.getStackTrace(e));
			}

		}

		private void inquireListeners() {
			PCFMessage request;
			PCFMessage[] response;
			int[] attrs = { MQConstants.MQCACH_LISTENER_NAME,
					MQConstants.MQCACH_LISTENER_START_DATE,
					MQConstants.MQCACH_LISTENER_START_TIME,
					MQConstants.MQIACH_LISTENER_STATUS };
			request = new PCFMessage(MQConstants.MQCMD_INQUIRE_LISTENER_STATUS);
			request.addParameter(MQConstants.MQCACH_LISTENER_NAME, "*");
			request.addParameter(MQConstants.MQIACF_LISTENER_STATUS_ATTRS,
					attrs);
			try {
				response = agent.send(request);
				for (int i = 0; i < response.length; i++) {

					Map<Object, Object> event = new HashMap<Object, Object>();

					event.put("inquiry_type", "listener");
					event.put("timestamp", System.currentTimeMillis());
					event.put(
							"listener_name",
							response[i]
									.getParameterValue(MQConstants.MQCACH_LISTENER_NAME));
					event.put(
							"start_date",
							response[i]
									.getParameterValue(MQConstants.MQCACH_LISTENER_START_DATE));
					event.put(
							"start_time",
							response[i]
									.getParameterValue(MQConstants.MQCACH_LISTENER_START_TIME));
					event.put(
							"status",
							response[i]
									.getParameterValue(MQConstants.MQIACH_LISTENER_STATUS));

					streamEvent(event);
				}
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error inquiring listeners: "
						+ ModularInput.getStackTrace(e));
			}

		}

		private void inquireQueues() {
			PCFMessage request;
			PCFMessage[] response;
			int[] attrs = { MQConstants.MQCA_Q_NAME,
					MQConstants.MQIA_CURRENT_Q_DEPTH,
					MQConstants.MQCACF_LAST_GET_DATE,
					MQConstants.MQCACF_LAST_GET_TIME,
					MQConstants.MQCACF_LAST_PUT_DATE,
					MQConstants.MQCACF_LAST_PUT_TIME,
					MQConstants.MQIACF_OLDEST_MSG_AGE,
					MQConstants.MQIA_OPEN_INPUT_COUNT,
					MQConstants.MQIA_OPEN_OUTPUT_COUNT,
					MQConstants.MQIACF_UNCOMMITTED_MSGS };
			request = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q_STATUS);
			request.addParameter(MQConstants.MQCA_Q_NAME, "*");
			request.addParameter(MQConstants.MQIACF_Q_STATUS_TYPE,
					MQConstants.MQIACF_Q_STATUS);
			request.addParameter(MQConstants.MQIACF_Q_STATUS_ATTRS, attrs);
			try {
				response = agent.send(request);
				for (int i = 0; i < response.length; i++) {

					Map<Object, Object> event = new HashMap<Object, Object>();

					event.put("inquiry_type", "queue");
					event.put("timestamp", System.currentTimeMillis());
					event.put("queue_name", response[i]
							.getParameterValue(MQConstants.MQCA_Q_NAME));
					event.put(
							"queue_depth",
							response[i]
									.getParameterValue(MQConstants.MQIA_CURRENT_Q_DEPTH));
					event.put(
							"last_get_date",
							response[i]
									.getParameterValue(MQConstants.MQCACF_LAST_GET_DATE));
					event.put(
							"last_get_time",
							response[i]
									.getParameterValue(MQConstants.MQCACF_LAST_GET_TIME));
					event.put(
							"last_put_date",
							response[i]
									.getParameterValue(MQConstants.MQCACF_LAST_PUT_DATE));
					event.put(
							"last_put_time",
							response[i]
									.getParameterValue(MQConstants.MQCACF_LAST_PUT_TIME));
					event.put(
							"oldest_msg_age",
							response[i]
									.getParameterValue(MQConstants.MQIACF_OLDEST_MSG_AGE));
					event.put(
							"open_input_count",
							response[i]
									.getParameterValue(MQConstants.MQIA_OPEN_INPUT_COUNT));
					event.put(
							"open_output_count",
							response[i]
									.getParameterValue(MQConstants.MQIA_OPEN_OUTPUT_COUNT));
					event.put(
							"uncommitted_msgs",
							response[i]
									.getParameterValue(MQConstants.MQIACF_UNCOMMITTED_MSGS));

					streamEvent(event);
				}
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error inquiring queues: "
						+ ModularInput.getStackTrace(e));
			}

		}

		private void inquireChannels() {

			PCFMessage request;
			PCFMessage[] response;
			int[] attrs = { MQConstants.MQCACH_CHANNEL_NAME,
					MQConstants.MQCACH_CONNECTION_NAME,
					MQConstants.MQIACH_CHANNEL_STATUS, MQConstants.MQIACH_MSGS,
					MQConstants.MQIACH_BYTES_SENT,
					MQConstants.MQIACH_BYTES_RECEIVED,
					MQConstants.MQIACH_BUFFERS_SENT,
					MQConstants.MQIACH_BUFFERS_RECEIVED };
			request = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);
			request.addParameter(MQConstants.MQCACH_CHANNEL_NAME, "*");
			request.addParameter(MQConstants.MQIACH_CHANNEL_INSTANCE_TYPE,
					MQConstants.MQOT_CURRENT_CHANNEL);
			request.addParameter(MQConstants.MQIACH_CHANNEL_INSTANCE_ATTRS,
					attrs);
			try {
				response = agent.send(request);
				for (int i = 0; i < response.length; i++) {

					Map<Object, Object> event = new HashMap<Object, Object>();

					event.put("inquiry_type", "channel");
					event.put("timestamp", System.currentTimeMillis());
					event.put("channel_name", response[i]
							.getParameterValue(MQConstants.MQCACH_CHANNEL_NAME));
					event.put(
							"status",
							response[i]
									.getParameterValue(MQConstants.MQIACH_CHANNEL_STATUS));
					event.put("msgs", response[i]
							.getParameterValue(MQConstants.MQIACH_MSGS));
					event.put("bytes_sent", response[i]
							.getParameterValue(MQConstants.MQIACH_BYTES_SENT));
					event.put(
							"bytes_received",
							response[i]
									.getParameterValue(MQConstants.MQIACH_BYTES_RECEIVED));
					event.put("buffers_sent", response[i]
							.getParameterValue(MQConstants.MQIACH_BUFFERS_SENT));
					event.put(
							"buffers_received",
							response[i]
									.getParameterValue(MQConstants.MQIACH_BUFFERS_RECEIVED));

					streamEvent(event);
				}
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error inquiring channels: "
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
				stanza.setName("mq://" + item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			this.validateConnectionMode = true;
			doRun(input);

		} catch (Throwable t) {
			throw new Exception(
					"A MQ Manager connection can not be establised with the supplied propertys.Reason : "
							+ t.getMessage());
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("Websphere MQ");
		scheme.setDescription("Inquire for various metrics and statistics from an MQ Manager");
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
		arg.setName("inquire_queues");
		arg.setTitle("Inquire Queues");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("inquire_topics");
		arg.setTitle("Inquire Topics");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("inquire_channels");
		arg.setTitle("Inquire Channels");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("inquire_listeners");
		arg.setTitle("Inquire Listeners");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("inquire_subscriptions");
		arg.setTitle("Inquire Subscriptions");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("inquire_services");
		arg.setTitle("Inquire Services");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("inquire_current_queuemanager");
		arg.setTitle("Inquire Current Queue Manager");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("host");
		arg.setTitle("MQ Host");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("port");
		arg.setTitle("MQ Port");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("manager_name");
		arg.setTitle("MQ Manager Name");
		arg.setDescription("");
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("channel_name");
		arg.setTitle("MQ Channel Name");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("username");
		arg.setTitle("MQ Username");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("password");
		arg.setTitle("MQ Password");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("use_ssl");
		arg.setTitle("Use SSL ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("ssl_cipher_suite");
		arg.setTitle("SSL Cipher Suite");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("ssl_fips_required");
		arg.setTitle("SSL FIPS Required ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("polling_frequency");
		arg.setTitle("Polling Frequency");
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
		arg.setName("event_handler_impl");
		arg.setTitle("Implementation class for a custom event handler");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("event_handler_params");
		arg.setTitle("Implementation parameter string for the custom event handler");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		scheme.setEndpoint(endpoint);

		return scheme;
	}

}
