package com.splunk.modinput.protocol;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
	private static Map<String, String> outputVerticles = new HashMap<String, String>();
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

		outputVerticles
				.put("stdout",
						"com.splunk.modinput.protocol.outputverticle.STDOUTOutputVerticle");
		outputVerticles
				.put("tcp",
						"com.splunk.modinput.protocol.outputverticle.TCPOutputVerticle");
	}

	public static void main(String[] args) {

		ProtocolModularInput instance = new ProtocolModularInput();
		instance.init(args);

	}

	@Override
	protected void doRun(Input input) throws Exception {

		setModsDirectory();
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

					String outputAddress = UUID.randomUUID().toString();

					if (!config.containsField("handler_config")) {
						config.putString("handler_config",
								"{\"generated\":\"true\"}");
					}

					if (!config.containsField("output_type")) {
						config.putString("output_type", "stdout");
					}

					String outputType = config.getString("output_type");

					JsonObject handlerConfig = new JsonObject(
							config.getString("handler_config"));

					handlerConfig.putString("stanza",
							config.getString("stanza"));
					handlerConfig.putString("output_type", outputType);
					handlerConfig.putString("output_address", outputAddress);

					if (outputType.equalsIgnoreCase("tcp")) {

						// defaults
						int outputPort = 9999;
						String index = "main";
						String source = "pdi";
						String sourcetype = "pdi";

						if (config.containsField("output_port"))
							outputPort = config.getNumber("output_port")
									.intValue();
						if (config.containsField("index"))
							index = config.getString("index");
						if (config.containsField("source"))
							source = config.getString("source");
						if (config.containsField("sourcetype"))
							sourcetype = config.getString("sourcetype");
						createTCPInput(input, outputPort, index, sourcetype,
								source);

						handlerConfig.putNumber("output_port", outputPort);

					}

					config.putString("handler_config", handlerConfig.toString());

					if (!config.containsField("bind_address")) {
						config.putString("bind_address", "0.0.0.0");
					}
					if (!config.containsField("server_verticle_instances")) {
						config.putNumber("server_verticle_instances", 1);
					}
					if (!config.containsField("handler_verticle_instances")) {
						config.putNumber("handler_verticle_instances", 1);
					}
					if (!config.containsField("output_verticle_instances")) {
						config.putNumber("output_verticle_instances", 1);
					}

					if (config.containsField("additional_jvm_propertys"))
						setJVMSystemProperties(config
								.getString("additional_jvm_propertys"));

					if (!config.containsField("handler_verticle"))
						config.putString("handler_verticle",
								DEFAULT_HANDLER_VERTICLE);

					pm.deployWorkerVerticle(false, outputVerticles
							.get(outputType), handlerConfig,
							getClassPathAsURLArray(),
							config.getNumber("output_verticle_instances")
									.intValue(), null,
							new AsyncResultHandler<String>() {
								public void handle(
										AsyncResult<String> asyncResult) {
									if (asyncResult.succeeded()) {
										// ok
									} else {
										logger.error("Can't instantiate output verticle : "
												+ ModularInput
														.getStackTrace(asyncResult
																.cause()));

									}
								}
							});

					pm.deployVerticle(protocolVerticles.get(protocol), config,
							getClassPathAsURLArray(),
							config.getNumber("server_verticle_instances")
									.intValue(), null,
							new AsyncResultHandler<String>() {
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

	private void setModsDirectory() {

		String seperator = System.getProperty("file.separator");

		System.setProperty("vertx.mods", System.getenv("SPLUNK_HOME")
				+ seperator + "etc" + seperator + "apps" + seperator
				+ "protocol_ta" + seperator + "bin" + seperator
				+ "vertx_modules");
	}

	private void deployModules(PlatformManager pm) {

		String seperator = System.getProperty("file.separator");

		try {
			File modulesDir = new File(System.getenv("SPLUNK_HOME") + seperator
					+ "etc" + seperator + "apps" + seperator + "protocol_ta"
					+ seperator + "bin" + seperator + "vertx_modules");

			File[] modules = modulesDir.listFiles();

			for (File module : modules) {

				pm.deployModuleFromZip(module.getCanonicalPath(), null, 1,
						new AsyncResultHandler<String>() {
							public void handle(AsyncResult<String> asyncResult) {
								if (asyncResult.succeeded()) {
									// ok
								} else {
									logger.error("Can't deploy module : "
											+ ModularInput
													.getStackTrace(asyncResult
															.cause()));

								}
							}
						});
			}
		} catch (Exception e) {
			logger.error("Can't deploy module : "
					+ ModularInput.getStackTrace(e));
		}

	}

	public static URL[] getClassPathAsURLArray() {

		String seperator = System.getProperty("file.separator");

		String classpathRootDir = System.getenv("SPLUNK_HOME") + seperator
				+ "etc" + seperator + "apps" + seperator + "protocol_ta"
				+ seperator + "bin" + seperator;

		String datahandlersCP = classpathRootDir + "datahandlers";
		String modinputCP = classpathRootDir + "lib" + seperator
				+ "protocolmodinput.jar";
		URL[] classPathAsURLArray = new URL[2];

		try {
			classPathAsURLArray[0] = new URL("file:///"
					+ datahandlersCP.replace('\\', '/'));
			classPathAsURLArray[1] = new URL("file:///"
					+ modinputCP.replace('\\', '/'));
		} catch (MalformedURLException e) {
			logger.error("Error getting verticle classpath"
					+ ModularInput.getStackTrace(e));
		}

		/**
		 * String classPath = System.getProperty("java.class.path");
		 * 
		 * String[] splitClassPath =
		 * classPath.split(System.getProperty("path.separator"));
		 * 
		 * URL[] classPathAsURLArray = new URL[splitClassPath.length];
		 * 
		 * for (int i=0; i<splitClassPath.length; i++) {
		 * 
		 * try { classPathAsURLArray[i] = new URL("file:///" +
		 * splitClassPath[i].replace('\\', '/')); } catch (MalformedURLException
		 * ex) { logger.error("Error getting verticle classpath");
		 * classPathAsURLArray = null; }
		 * 
		 * }
		 **/

		return classPathAsURLArray;
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
			if (value != null) {
				String trimmed = value.trim();
				if (trimmed.length() > 0) {
					try {
						obj.putNumber(param.getName(),
								Integer.parseInt(trimmed));
					} catch (NumberFormatException e) {
						obj.putString(param.getName(), trimmed);
					}

				}

			}

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
		arg.setName("output_type");
		arg.setTitle("Output Type");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("output_port");
		arg.setTitle("Output Port");
		arg.setDescription("");
		arg.setRequired_on_create(false);
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
		arg.setName("output_verticle_instances");
		arg.setTitle("Output Verticle Instances");
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