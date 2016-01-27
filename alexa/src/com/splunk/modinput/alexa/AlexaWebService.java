package com.splunk.modinput.alexa;

import java.util.ArrayList;

import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.amazon.speech.Sdk;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.servlet.SpeechletServlet;

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

public class AlexaWebService extends ModularInput {

	public static void main(String[] args) {

		AlexaWebService instance = new AlexaWebService();
		instance.init(args);

	}

	boolean validateMode = false;

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null && name.startsWith("alexa://")) {

					startWebServer(name, stanza.getParams(), validateMode);

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

	private void startWebServer(String stanzaName, List<Param> params, boolean validationMode) throws Exception {

		int httpsPort = 443;
		String httpsScheme = "https";
		String endpoint = "";
		String keystore = "../crypto/java-keystore.jks";
		String keystorePass = "";
		String disableRequestSignatureCheck = "false";
		String supportedApplicationIds = "";
		int timestampTolerance = 150;

		Transport transport = getTransportInstance(params, stanzaName);

		for (Param param : params) {
			String value = param.getValue();
			if (value == null) {
				continue;
			}

			if (param.getName().equals("https_scheme")) {
				httpsScheme = param.getValue();

			} else if (param.getName().equals("https_port")) {
				try {
					httpsPort = Integer.parseInt(param.getValue());
				} catch (Exception e) {
					logger.error("Can't determine https port value, will revert to default value.");
				}
			} else if (param.getName().equals("endpoint")) {
				endpoint = param.getValue();

			} else if (param.getName().equals("keystore")) {
				keystore = param.getValue();

			} else if (param.getName().equals("keystorePass")) {
				keystorePass = param.getValue();

			} else if (param.getName().equals("supported_application_ids")) {
				supportedApplicationIds = param.getValue();
				System.setProperty("com.amazon.speech.speechlet.servlet.supportedApplicationIds",
						supportedApplicationIds);

			} else if (param.getName().equals("disable_request_signature_check")) {
				disableRequestSignatureCheck = param.getValue().equals("1") ? "true" : "false";
				System.setProperty("com.amazon.speech.speechlet.servlet.disableRequestSignatureCheck",
						disableRequestSignatureCheck);

			} else if (param.getName().equals("timestamp_tolerance")) {
				try {
					timestampTolerance = Integer.parseInt(param.getValue());
					System.setProperty("com.amazon.speech.speechlet.servlet.timestampTolerance",
							String.valueOf(timestampTolerance));
				} catch (Exception e) {
					logger.error("Can't determine timestampTolerance value, will revert to default value.");
				}
			}
		}

		if (!isDisabled(stanzaName)) {

			if (validationMode) {
				// TODO , just a test start then stop of the web server
			} else {

				// Configure server and its associated servlets
				Server server = new Server();
				SslConnectionFactory sslConnectionFactory = new SslConnectionFactory();
				SslContextFactory sslContextFactory = sslConnectionFactory.getSslContextFactory();
				sslContextFactory.setKeyStorePath(keystore);
				sslContextFactory.setKeyStorePassword(keystorePass);
				sslContextFactory.setIncludeCipherSuites(Sdk.SUPPORTED_CIPHER_SUITES);

				HttpConfiguration httpConf = new HttpConfiguration();
				httpConf.setSecurePort(httpsPort);
				httpConf.setSecureScheme(httpsScheme);
				httpConf.addCustomizer(new SecureRequestCustomizer());
				HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConf);

				ServerConnector serverConnector = new ServerConnector(server, sslConnectionFactory,
						httpConnectionFactory);
				serverConnector.setPort(httpsPort);

				Connector[] connectors = new Connector[1];
				connectors[0] = serverConnector;
				server.setConnectors(connectors);

				ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
				context.setContextPath("/");
				server.setHandler(context);
				context.addServlet(new ServletHolder(createServlet(new SplunkSpeechlet())),
						endpoint.substring(endpoint.indexOf("/")));
				server.start();
				server.join();
			}
		}
	}

	private static SpeechletServlet createServlet(final Speechlet speechlet) {
		SpeechletServlet servlet = new SpeechletServlet();
		servlet.setSpeechlet(speechlet);
		return servlet;
	}

	@Override
	protected void doValidate(Validation val) {

		try {

			if (val != null) {
				validateWebServer(val);
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
			ValidationError error = new ValidationError("Validation Failed : " + e.getMessage());
			sendValidationError(error);
			System.exit(2);
		}

	}

	private void validateWebServer(Validation val) throws Exception {

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
				stanza.setName("alexa://" + item.getName());
				stanza.setParams(item.getParams());
				stanzas.add(stanza);
			}

			input.setStanzas(stanzas);
			this.validateMode = true;
			doRun(input);

		} catch (Throwable t) {
			throw new Exception(
					"An Alexa web server can not be run with the supplied propertys.Reason : " + t.getMessage());
		}

	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("Alexa");
		scheme.setDescription("Alexa");
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
		arg.setName("https_port");
		arg.setTitle("HTTPS Port");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("https_scheme");
		arg.setTitle("HTTPS Scheme");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("endpoint");
		arg.setTitle("Endpoint");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("keystore");
		arg.setTitle("Keystore");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("keystore_pass");
		arg.setTitle("Keystore Pass");
		arg.setDescription("");
		arg.setRequired_on_create(true);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("disable_request_signature_check");
		arg.setTitle("Disable Request Signature Check");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("supported_application_ids");
		arg.setTitle("Supported Application IDs");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("timestamp_tolerance");
		arg.setTitle("Timestamp Tolerance");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		scheme.setEndpoint(endpoint);

		return scheme;
	}

}
