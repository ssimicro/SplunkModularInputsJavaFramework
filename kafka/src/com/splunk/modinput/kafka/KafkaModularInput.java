package com.splunk.modinput.kafka;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

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
import com.splunk.modinput.transport.Transport;

public class KafkaModularInput extends ModularInput {

	private static final String DEFAULT_MESSAGE_HANDLER = "com.splunk.modinput.kafka.DefaultMessageHandler";
	
	public static void main(String[] args) {

		KafkaModularInput instance = new KafkaModularInput();
		instance.init(args);

	}

	boolean validateConnectionMode = false;

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();
			

				if (name != null && name.startsWith("kafka://")) {

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

		String topicName = "";
		String zkhost = "";
		int zkport = 2181; // default Zookeeper port
		String zkchroot = "";
		String zkconnectRawString = "";
		String groupID = "";
		int zkSessionTimeout = 400;
		int zkSyncTime = 200;
		int autoCommitInterval = 1000;
		String additionalConnectionProps = "";
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
			} else if (param.getName().equals("zookeeper_connect_host")) {
				zkhost = param.getValue();
			} else if (param.getName().equals("zookeeper_connect_chroot")) {
				zkchroot = param.getValue();
			} else if (param.getName().equals("zookeeper_connect_rawstring")) {
				zkconnectRawString = param.getValue();
			} else if (param.getName().equals("zookeeper_connect_port")) {
				try {
					zkport = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine port value, will revert to default value.");
				}
			} else if (param.getName().equals("zookeeper_session_timeout_ms")) {
				try {
					zkSessionTimeout = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine session timeout value, will revert to default value.");
				}
			} else if (param.getName().equals("zookeeper_sync_time_ms")) {
				try {
					zkSyncTime = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine sync time value, will revert to default value.");
				}
			} else if (param.getName().equals("auto_commit_interval_ms")) {
				try {
					autoCommitInterval = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine auto commit value, will revert to default value.");
				}
			} else if (param.getName().equals("group_id")) {
				groupID = param.getValue();

			} else if (param.getName().equals("additional_consumer_properties")) {
				additionalConnectionProps = param.getValue();
			}

			else if (param.getName().equals("message_handler_impl")) {
				messageHandlerImpl = param.getValue();
			} else if (param.getName().equals("message_handler_params")) {
				messageHandlerParams = param.getValue();
			} else if (param.getName().equals("additional_jvm_propertys")) {
				setJVMSystemProperties(param.getValue());
			}

		}

		if (!isDisabled(stanzaName)) {
			MessageReceiver mr = new MessageReceiver(stanzaName, topicName,
					zkhost, zkport, zkchroot, zkconnectRawString, groupID,
					zkSessionTimeout, zkSyncTime, autoCommitInterval,
					additionalConnectionProps, messageHandlerImpl,
					messageHandlerParams,transport);
			if (validationConnectionMode)
				mr.testConnectOnly();
			else
				mr.start();
		}
	}

	public class MessageReceiver extends Thread {

		String topicName;
		String stanzaName;
		AbstractMessageHandler messageHandler;

		boolean connected = false;

		ConsumerConfig consumerConfig;
		ConsumerConnector consumer;

		public MessageReceiver(String stanzaName, String topicName,
				String zkhost, int zkport, String zkchroot,
				String zkconnectRawString, String groupID,
				int zkSessionTimeout, int zkSyncTime, int autoCommitInterval,
				String additionalConnectionProps, String messageHandlerImpl,
				String messageHandlerParams,Transport transport) {

			this.stanzaName = stanzaName;

			this.topicName = topicName;

			Properties connectionProperties = new Properties();

			String connectionString = "";
			if (zkconnectRawString.length() > 0)
				connectionString = zkconnectRawString;
			else {
				connectionString = zkhost + ":" + zkport;
				if (zkchroot.length() > 0)
					connectionString += "/" + zkchroot;
			}

			connectionProperties.put("zookeeper.connect", connectionString);
			connectionProperties.put("group.id", String.valueOf(groupID));
			connectionProperties.put("zookeeper.session.timeout.ms",
					String.valueOf(zkSessionTimeout));
			connectionProperties.put("zookeeper.sync.time.ms",
					String.valueOf(zkSyncTime));
			connectionProperties.put("auto.commit.interval.ms",
					String.valueOf(autoCommitInterval));

			Map<String, String> additionalProps = getParamMap(additionalConnectionProps);

			for (String key : additionalProps.keySet()) {
				connectionProperties.put(key, additionalProps.get(key));
			}

			this.consumerConfig = new ConsumerConfig(connectionProperties);

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

			this.consumer = kafka.consumer.Consumer
					.createJavaConsumerConnector(this.consumerConfig);

			connected = true;

		}

		private void disconnect() {
			try {
				this.consumer.shutdown();
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

					Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
					topicCountMap.put(this.topicName, new Integer(1));
					Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = this.consumer
							.createMessageStreams(topicCountMap);
					KafkaStream<byte[], byte[]> stream = consumerMap.get(
							this.topicName).get(0);
					ConsumerIterator<byte[], byte[]> it = stream.iterator();
					while (it.hasNext())
						streamMessageEvent(it.next().message());

				} catch (Throwable e) {
					logger.error("Stanza " + stanzaName + " : "
							+ "Error running message receiver : "
							+ ModularInput.getStackTrace(e));
					disconnect();

				} finally {

				}
			}
		}

		private void streamMessageEvent(byte[] message) {
			try {
				messageHandler.handleMessage(message, this);
			
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
				stanza.setName("kafka://" + item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			this.validateConnectionMode = true;
			doRun(input);

		} catch (Throwable t) {
			throw new Exception(
					"A Zookeeper connection can not be establised with the supplied propertys.Reason : "
							+ t.getMessage());
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("Kafka Messaging");
		scheme.setDescription("Index messages from a Kafka broker or cluster of brokers");
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
		arg.setName("zookeeper_connect_host");
		arg.setTitle("Zookeeper Host");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("zookeeper_connect_port");
		arg.setTitle("Zookeeper Port");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("zookeeper_connect_chroot");
		arg.setTitle("Zookeeper CHROOT");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("zookeeper_connect_rawstring");
		arg.setTitle("Zookeeper Raw Connection String");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("group_id");
		arg.setTitle("Group ID");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("zookeeper_session_timeout_ms");
		arg.setTitle("Zookeeper Session Timeout (ms)");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("zookeeper_sync_time_ms");
		arg.setTitle("Zookeeper Sync Time (ms)");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("auto_commit_interval_ms");
		arg.setTitle("Autocommit Interval (ms)");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("additional_consumer_properties");
		arg.setTitle("Additional Consumer Properties");
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
		arg.setDescription("An implementation of the com.splunk.modinput.kafka.AbstractMessageHandler class.You would provide this if you required some custom handling/formatting of the messages you consume.Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/kafka_ta/bin/lib directory");
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
