package com.splunk.modinput.kinesis;

import java.net.InetAddress;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;
import java.util.Map;

import java.util.StringTokenizer;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;

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
import com.splunk.modinput.transport.Transport;

public class KinesisModularInput extends ModularInput {

	private static final String DEFAULT_MESSAGE_HANDLER = "com.splunk.modinput.kinesis.DefaultMessageHandler";

	public static void main(String[] args) {

		KinesisModularInput instance = new KinesisModularInput();
		instance.init(args);

	}

	boolean validateConnectionMode = false;

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("kinesis://")) {

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

		String appName = "";
		String streamName = "";
		String endpoint = "https://kinesis.us-east-1.amazonaws.com";
		String streamPosition = "TRIM_HORIZON";
		String awsKey = "";
		String awsSecret = "";
		long backoffTime = 3000L;
		int numRetries = 10;
		long checkpointInterval = 60000L;

		String messageHandlerImpl = DEFAULT_MESSAGE_HANDLER;
		String messageHandlerParams = "";

		Transport transport = getTransportInstance(params,stanzaName);
		
		for (Param param : params) {
			String value = param.getValue();
			if (value == null) {
				continue;
			}

			if (param.getName().equals("app_name")) {
				appName = param.getValue();
			} else if (param.getName().equals("stream_name")) {
				streamName = param.getValue();
			} else if (param.getName().equals("kinesis_endpoint")) {
				endpoint = param.getValue();
			} else if (param.getName().equals("initial_stream_position")) {
				streamPosition = param.getValue();
			} else if (param.getName().equals("aws_access_key_id")) {
				awsKey = param.getValue();
			} else if (param.getName().equals("aws_secret_access_key")) {
				awsSecret = param.getValue();
			}

			else if (param.getName().equals("num_retries")) {
				try {
					numRetries = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine num retries value, will revert to default value.");
				}
			} else if (param.getName().equals("backoff_time_millis")) {
				try {
					backoffTime = Long.parseLong(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine backoff time value, will revert to default value.");
				}
			} else if (param.getName().equals("checkpoint_interval_millis")) {
				try {
					checkpointInterval = Long.parseLong(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine checkpoint interval value, will revert to default value.");
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
			MessageReceiver mr = new MessageReceiver(stanzaName, appName,
					streamName, endpoint, streamPosition, awsKey, awsSecret,
					backoffTime, numRetries, checkpointInterval,
					messageHandlerImpl, messageHandlerParams,transport);
			if (validationConnectionMode)
				mr.testConnectOnly();
			else
				mr.start();
		}
	}

	public class MessageReceiver extends Thread implements
			IRecordProcessorFactory {

		String stanzaName;
		AbstractMessageHandler messageHandler;

		KinesisClientLibConfiguration kinesisClientLibConfiguration;
		InitialPositionInStream position = InitialPositionInStream.TRIM_HORIZON;

		boolean connected = false;
		String workerId = "splunk";
		Worker worker;

		long backoffTime;
		int numRetries;
		long checkpointInterval;

		public MessageReceiver(String stanzaName, String appName,
				String streamName, String endpoint, String streamPosition,
				String awsKey, String awsSecret, long backoffTime,
				int numRetries, long checkpointInterval,
				String messageHandlerImpl, String messageHandlerParams,Transport transport) {

			this.stanzaName = stanzaName;
			this.backoffTime = backoffTime;
			this.numRetries = numRetries;
			this.checkpointInterval = checkpointInterval;

			if (streamPosition.equalsIgnoreCase("LATEST"))
				position = InitialPositionInStream.LATEST;
			else if (streamPosition.equalsIgnoreCase("TRIM_HORIZON"))
				position = InitialPositionInStream.TRIM_HORIZON;

			AWSCredentialsProvider credentialsProvider = null;
			try {
				credentialsProvider = new MyCredentialsProvider(awsKey,
						awsSecret);
				// Verify we can fetch credentials from the provider
				credentialsProvider.getCredentials();

			} catch (AmazonClientException e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Can't set AWS credentials , "
						+ ModularInput.getStackTrace(e));
				System.exit(2);
			}

			try {
				this.workerId = InetAddress.getLocalHost()
						.getCanonicalHostName() + ":" + UUID.randomUUID();
			} catch (Exception e1) {
			}

			kinesisClientLibConfiguration = new KinesisClientLibConfiguration(
					appName, streamName, credentialsProvider, workerId)
					.withInitialPositionInStream(position).withKinesisEndpoint(
							endpoint);

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

		@Override
		public IRecordProcessor createProcessor() {
			return new RecordProcessor(this, backoffTime, numRetries,
					checkpointInterval);
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

		public void streamMessageEvent(String record,String seqNumber,String partitionKey) {
			try {
				messageHandler.handleMessage(record,seqNumber,partitionKey, this);
				
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error handling message : "
						+ ModularInput.getStackTrace(e));
			}
		}

		private void connect() throws Exception {

			this.worker = new Worker(this, kinesisClientLibConfiguration);

			connected = true;

		}

		private void disconnect() {
			try {
				this.worker.shutdown();
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

					this.worker.run();

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
				stanza.setName("kinesis://" + item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			this.validateConnectionMode = true;
			doRun(input);

		} catch (Throwable t) {
			throw new Exception(
					"A Kinesis connection can not be establised with the supplied propertys.Reason : "
							+ t.getMessage());
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("Amazon Kinesis");
		scheme.setDescription("Index records from Kinesis");
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
		arg.setName("app_name");
		arg.setTitle("Kinesis App Name");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("stream_name");
		arg.setTitle("Kinesis Stream Name");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("kinesis_endpoint");
		arg.setTitle("Kinesis Endpoint");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("initial_stream_position");
		arg.setTitle("Initial Stream Position");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("aws_access_key_id");
		arg.setTitle("AWS Access Key ID");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("aws_secret_access_key");
		arg.setTitle("AWS Secret");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("backoff_time_millis");
		arg.setTitle("Backoff Time (Millis)");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("num_retries");
		arg.setTitle("Number of Retries");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("checkpoint_interval_millis");
		arg.setTitle("Checkpoint Interval (Millis)");
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
		

		scheme.setEndpoint(endpoint);

		return scheme;
	}

	class RecordProcessor implements IRecordProcessor {

		private String kinesisShardId;

		// Backoff and retry settings
		private long backoffTime;
		private int numRetries;

		// Checkpoint about once a minute
		private long checkpointInterval;
		private long nextCheckpointTimeInMillis;

		private final CharsetDecoder decoder = Charset.forName("UTF-8")
				.newDecoder();

		MessageReceiver context;

		/**
		 * Constructor.
		 */
		public RecordProcessor(MessageReceiver context, long backoffTime,
				int numRetries, long checkpointInterval) {

			this.context = context;

			this.backoffTime = backoffTime;
			this.numRetries = numRetries;
			this.checkpointInterval = checkpointInterval;

		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void initialize(String shardId) {
			this.kinesisShardId = shardId;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void processRecords(List<Record> records,
				IRecordProcessorCheckpointer checkpointer) {

			// Process records and perform all exception handling.
			processRecordsWithRetries(records);

			// Checkpoint once every checkpoint interval.
			if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
				checkpoint(checkpointer);
				nextCheckpointTimeInMillis = System.currentTimeMillis()
						+ checkpointInterval;
			}

		}

		/**
		 * Process records performing retries as needed. Skip "poison pill"
		 * records.
		 * 
		 * @param records
		 */
		private void processRecordsWithRetries(List<Record> records) {
			for (Record record : records) {
				boolean processedSuccessfully = false;
				String data = null;
				for (int i = 0; i < numRetries; i++) {
					try {
						
						data = decoder.decode(record.getData()).toString();
						context.streamMessageEvent(data,record.getSequenceNumber(),record.getPartitionKey());
						processedSuccessfully = true;
						break;
					} catch (CharacterCodingException e) {
						logger.error("Malformed data: " + data, e);
						break;
					} catch (Throwable t) {
						logger.error(
								"Caught throwable while processing record "
										+ record, t);
					}

					// backoff if we encounter an exception.
					try {
						Thread.sleep(backoffTime);
					} catch (InterruptedException e) {

					}
				}

				if (!processedSuccessfully) {
					logger.error("Couldn't process record " + record
							+ ". Skipping the record.");
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void shutdown(IRecordProcessorCheckpointer checkpointer,
				ShutdownReason reason) {

			// Important to checkpoint after reaching end of shard, so we can
			// start processing data from child shards.
			if (reason == ShutdownReason.TERMINATE) {
				checkpoint(checkpointer);
			}
		}

		/**
		 * Checkpoint with retries.
		 * 
		 * @param checkpointer
		 */
		private void checkpoint(IRecordProcessorCheckpointer checkpointer) {

			for (int i = 0; i < numRetries; i++) {
				try {
					checkpointer.checkpoint();
					break;
				} catch (ShutdownException se) {
					// Ignore checkpoint if the processor instance has been
					// shutdown (fail over).
					logger.error(
							"Caught shutdown exception, skipping checkpoint.",
							se);
					break;
				} catch (ThrottlingException e) {
					// Backoff and re-attempt checkpoint upon transient failures
					if (i >= (numRetries - 1)) {
						logger.error("Checkpoint failed after " + (i + 1)
								+ "attempts.", e);
						break;
					} else {
						logger.error(
								"Transient issue when checkpointing - attempt "
										+ (i + 1) + " of " + numRetries, e);
					}
				} catch (InvalidStateException e) {
					// This indicates an issue with the DynamoDB table (check
					// for table, provisioned IOPS).
					logger.error(
							"Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.",
							e);
					break;
				}
				try {
					Thread.sleep(backoffTime);
				} catch (InterruptedException e) {

				}
			}
		}

	}

	public class MyCredentialsProvider implements AWSCredentialsProvider {

		String awsKey;
		String awsSecret;

		public MyCredentialsProvider(String awsKey, String awsSecret) {

			this.awsKey = awsKey;
			this.awsSecret = awsSecret;

		}

		@Override
		public AWSCredentials getCredentials() {

			return new BasicAWSCredentials(awsKey, awsSecret);
		}

		@Override
		public void refresh() {
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}

}
