package com.splunk.modinput.protocol;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import com.splunk.modinput.Arg;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;

import com.splunk.modinput.Stanza;
import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;
import com.splunk.modinput.Scheme.StreamingMode;

/**
 * Modular input for running many different types of protocol servers to receive
 * data , process it and output to Splunk.
 * 
 * Uses vertx.io , so refer to that to understand most of the programming
 * semantic.
 * 
 * @author ddallimore
 * 
 */
public class ProtocolModularInput extends ModularInput {

	private static final String DEFAULT_HANDLER_VERTICLE = "com.splunk.modinput.protocol.handlerverticle.DefaultHandlerVerticle";

	private static Map<String, String> protocolVerticles = new HashMap<String, String>();

	static {

		protocolVerticles.put("tcp",
				"com.splunk.modinput.protocol.protocolverticle.TCPVerticle");
		protocolVerticles.put("udp",
				"com.splunk.modinput.protocol.protocolverticle.UDPVerticle");
		protocolVerticles.put("http",
				"com.splunk.modinput.protocol.protocolverticle.HTTPVerticle");
		protocolVerticles
				.put("websocket",
						"com.splunk.modinput.protocol.protocolverticle.WebSocketVerticle");
		protocolVerticles.put("sockjs",
				"com.splunk.modinput.protocol.protocolverticle.SockJSVerticle");
	}

	public static void main(String[] args) {

		ProtocolModularInput instance = new ProtocolModularInput();
		instance.init(args);

	}

	@Override
	protected void doRun(Input input) throws Exception {

		// run vertx container in embedded mode
		PlatformManager pm = PlatformLocator.factory.createPlatformManager();

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("protocol://")) {

					JsonObject config = stanzaToJson(stanza);

					if (!config.containsField("protocol")) {
						logger.error("No Protocol defined");
						System.exit(2);
					}
					if (!config.containsField("port")) {
						logger.error("No Port defined");
						System.exit(2);
					}
					
					String protocol = config.getString("protocol");

					if (!config.containsField("bind_address")) {
						config.putString("bind_address", "0.0.0.0");
					}
					if (!config.containsField("server_verticle_instances")) {
						config.putNumber("server_verticle_instances", 1);
					}
					if (!config.containsField("handler_verticle_instances")) {
						config.putNumber("handler_verticle_instances", 1);
					}
					
					if (config.containsField("additional_jvm_properties"))
						setJVMSystemProperties(config
								.getString("additional_jvm_properties"));

					if (!config.containsField("handler_verticle"))
						config.putString("handler_verticle",
								DEFAULT_HANDLER_VERTICLE);

					if (!config.containsField("handler_config")) {
						config.putString("handler_config",
								"{\"generated\":\"true\"}");
					}
					pm.deployVerticle(protocolVerticles.get(protocol), config,
							null, config.getNumber("server_verticle_instances").intValue(), null, new AsyncResultHandler<String>() {
								public void handle(
										AsyncResult<String> asyncResult) {
									if (asyncResult.succeeded()) {
										// ok
									} else {
										logger.error("Can't instantiate protocol verticle : "
												+ ModularInput
														.getStackTrace(asyncResult
																.cause()));

									}
								}
							});

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

	/**
	 * Convert stanza fields into JSON
	 * 
	 * @param stanza
	 * @return
	 */
	private JsonObject stanzaToJson(Stanza stanza) {

		JsonObject obj = new JsonObject();
		obj.putString("stanza", stanza.getName());
		for (Param param : stanza.getParams()) {

			String value = param.getValue();
			if (value != null && value.trim().length() > 0)
				obj.putString(param.getName(), param.getValue());
		}

		return obj;
	}

	@Override
	protected void doValidate(Validation val) {

		try {

			if (val != null) {
				// TODO , add some validation rules
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

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("Protocol Data Inputs");
		scheme.setDescription("Receive data via various protocols");
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
		arg.setName("protocol");
		arg.setTitle("Protocol");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("port");
		arg.setTitle("Listen Port");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("bind_address");
		arg.setTitle("Bind Address");
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
		arg.setName("tcp_nodelay");
		arg.setTitle("TCP No Delay ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("receive_buffer_size");
		arg.setTitle("Receive Buffer Size");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("tcp_keepalive");
		arg.setTitle("TCP Keep Alive ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("so_linger");
		arg.setTitle("Socket Linger ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("keystore_pass");
		arg.setTitle("Keystore Password");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("keystore_path");
		arg.setTitle("Keystore Path");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("truststore_pass");
		arg.setTitle("Truststore Password");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("truststore_path");
		arg.setTitle("Truststore Path");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("client_auth_required");
		arg.setTitle("Client Auth Required ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("ip_version");
		arg.setTitle("IP Version");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("is_multicast");
		arg.setTitle(" Multicast or Unicast ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("multicast_group");
		arg.setTitle("Multicast Group");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("udp_receive_buffer_size");
		arg.setTitle("Receive Buffer Size");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("multicast_ttl");
		arg.setTitle("Multicast TTL");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("set_broadcast");
		arg.setTitle("Set Broadcast ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("set_multicast_loopback_mode");
		arg.setTitle("Set Multicast Loopback Mode ?");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("session_timeout");
		arg.setTitle("Session Timeout");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("heartbeat_period");
		arg.setTitle("Heartbeat Period");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("app_name");
		arg.setTitle("App Name");
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
		arg.setName("handler_verticle");
		arg.setTitle("Custom Handler Implementation");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("handler_config");
		arg.setTitle("Handler Configuration (JSON String)");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("server_verticle_instances");
		arg.setTitle("Server Verticle Instances");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("handler_verticle_instances");
		arg.setTitle("Handler Verticle Instances");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("accept_backlog");
		arg.setTitle("Accept Backlog");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		scheme.setEndpoint(endpoint);

		return scheme;
	}

}
