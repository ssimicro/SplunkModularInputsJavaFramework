package com.splunk.modinput.coap;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;
import java.util.Map;

import java.util.StringTokenizer;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.network.config.NetworkConfig;

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
import com.splunk.modinput.coap.AbstractMessageHandler;
import com.splunk.modinput.transport.Transport;

public class COAPModularInput extends ModularInput {

	private static final String DEFAULT_MESSAGE_HANDLER = "com.splunk.modinput.coap.DefaultMessageHandler";

	public static void main(String[] args) {

		COAPModularInput instance = new COAPModularInput();
		instance.init(args);

	}

	boolean validateConnectionMode = false;

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("coap://")) {

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

		String messageHandlerImpl = DEFAULT_MESSAGE_HANDLER;
		String messageHandlerParams = "";
		String uri = "";
		long ackTimeout = 2000; // default
		long getFrequency = 30000; // default
		String pollingType = "GET"; // default
		String requestType = "CON"; // default
		int negotiationBlockSize = 0; // default

		Transport transport = getTransportInstance(params,stanzaName);
		
		for (Param param : params) {
			String value = param.getValue();
			if (value == null) {
				continue;
			}

			if (param.getName().equals("uri")) {
				uri = param.getValue();
			} else if (param.getName().equals("polling_type")) {
				pollingType = param.getValue();
			} else if (param.getName().equals("request_type")) {
				requestType = param.getValue();
			} else if (param.getName().equals("ack_timeout")) {
				try {
					ackTimeout = Long.parseLong(param.getValue());
				} catch (Exception e) {

				}
			} else if (param.getName().equals("negotiation_block_size")) {
				try {
					negotiationBlockSize = Integer.parseInt(param.getValue());
				} catch (Exception e) {

				}
			} else if (param.getName().equals("get_frequency")) {
				try {
					getFrequency = Integer.parseInt(param.getValue()) * 1000;
				} catch (Exception e) {

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
			MessageReceiver mr = new MessageReceiver(stanzaName, uri,
					ackTimeout, pollingType, requestType, negotiationBlockSize,
					getFrequency, messageHandlerImpl, messageHandlerParams,transport);
			if (validationConnectionMode)
				mr.testConnectOnly();
			else
				mr.start();
		}
	}

	public class MessageReceiver extends Thread {

		AbstractMessageHandler messageHandler;
		String stanzaName;
		String uri;
		long ackTimeout;
		long getFrequency;
		String pollingType;
		String requestType;
		int negotiationBlockSize;
		boolean connected = false;
		CoapClient client;
		CoapObserveRelation relation;

		public MessageReceiver(String stanzaName, String uri, long ackTimeout,
				String pollingType, String requestType,
				int negotiationBlockSize, long getFrequency,
				String messageHandlerImpl, String messageHandlerParams,Transport transport) {

			this.stanzaName = stanzaName;
			this.uri = uri;
			this.ackTimeout = ackTimeout;
			this.pollingType = pollingType;
			this.requestType = requestType;
			this.negotiationBlockSize = negotiationBlockSize;
			this.getFrequency = getFrequency;

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
				// do some stuff

			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Can't instantiate COAP Server : "
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

		public void streamMessageEvent(byte[] message) {
			try {
				messageHandler.handleMessage(message, this);
				
			} catch (Exception e) {
				logger.error("Stanza " + stanzaName + " : "
						+ "Error handling message : "
						+ ModularInput.getStackTrace(e));
			}
		}

		private void connect() throws Exception {

			// connect
			if (this.client == null) {
				NetworkConfig.setStandard(new NetworkConfig());
				this.client = new CoapClient(this.uri);
				this.client.setTimeout(this.ackTimeout);
				if (negotiationBlockSize > 0)
					this.client.useEarlyNegotiation(negotiationBlockSize);
				else
					this.client.useLateNegotiation();
				if (requestType.equalsIgnoreCase("NON"))
					this.client.useNONs();
				else
					this.client.useCONs();

			}
			connected = true;

		}

		private void disconnect() {
			try {
				if (this.client != null)
					this.client = null;
				if (this.relation != null)
					this.relation.proactiveCancel();
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
				if (this.client != null)
					if (!client.ping())
						logger.error("Stanza " + stanzaName + " : "
								+ "Could not ping COAP Server");
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

					if (this.pollingType.equalsIgnoreCase("GET")) {
						CoapResponse response = this.client.get();
						if (response != null)
							streamMessageEvent(response.getPayload());
						try {
							Thread.sleep(getFrequency);
						} catch (Exception e) {

						}

					} else if (this.pollingType.equalsIgnoreCase("OBSERVE")) {
						this.relation = this.client
								.observeAndWait(new CoapHandler() {

									public void onLoad(CoapResponse response) {
										if (response != null)
											streamMessageEvent(response
													.getPayload());
									}

									public void onError() {
										logger.error("Stanza "
												+ stanzaName
												+ " : "
												+ "Error in COAP observe handler");

									}

								});

					}

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
				stanza.setName("coap://" + item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			this.validateConnectionMode = true;
			doRun(input);

		} catch (Throwable t) {
			throw new Exception(
					"A COAP server can not be establised with the supplied propertys.Reason : "
							+ t.getMessage());
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("COAP");
		scheme.setDescription("Index messages from a COAP Server");
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
		arg.setName("uri");
		arg.setTitle("COAP Server URI");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("ack_timeout");
		arg.setTitle("ACK Timeout");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("polling_type");
		arg.setTitle("Polling Type");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("get_frequency");
		arg.setTitle("Get Frequency");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("request_type");
		arg.setTitle("Request Type");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("negotiation_block_size");
		arg.setTitle("Negotiation Block Size");
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
		arg.setDescription("An implementation of the com.splunk.modinput.coap.AbstractMessageHandler class.You would provide this if you required some custom handling/formatting of the messages you consume.Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/coap_ta/bin/lib directory");
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
