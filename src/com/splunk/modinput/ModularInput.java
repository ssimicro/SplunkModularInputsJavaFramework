package com.splunk.modinput;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.splunk.Args;
import com.splunk.InputCollection;
import com.splunk.InputKind;
import com.splunk.Service;
import com.splunk.modinput.transport.HECTransportConfig;
import com.splunk.modinput.transport.Transport;

public abstract class ModularInput {

	protected static Logger logger = Logger.getLogger(ModularInput.class);

	protected static Map<String, Boolean> inputStates = new HashMap<String, Boolean>();

	protected boolean connectedToSplunk = false;

	private static Map<String, String> transports = new HashMap<String, String>();
	static {

		transports.put("stdout",
				"com.splunk.modinput.transport.STDOUTTransport");
		transports.put("hec", "com.splunk.modinput.transport.HECTransport");

	}

	protected Transport getTransportInstance(List<Param> params,
			String stanzaName) {
		Transport instance = null;
		String key = "stdout"; // default
		HECTransportConfig hec = new HECTransportConfig();

		for (Param param : params) {
			String value = param.getValue();
			if (value == null) {
				continue;
			}

			if (param.getName().equals("output_type")) {
				key = param.getValue();
			} else if (param.getName().equals("hec_port")) {
				try {
					hec.setPort(Integer.parseInt(param.getValue()));
				} catch (NumberFormatException e) {
					logger.error("Can't determine hec port value, will revert to default value.");
				}
			}

			else if (param.getName().equals("hec_poolsize")) {
				try {
					hec.setPoolsize(Integer.parseInt(param.getValue()));
				} catch (NumberFormatException e) {
					logger.error("Can't determine hec poolsize value, will revert to default value.");
				}

			} else if (param.getName().equals("hec_token")) {
				hec.setToken(param.getValue());

			} else if (param.getName().equals("hec_https")) {
				try {
					hec.setHttps(Boolean.parseBoolean(param.getValue().equals(
							"1") ? "true" : "false"));
				} catch (Exception e) {
					logger.error("Can't determine hec https value, will revert to default value.");
				}

			} else if (param.getName().equals("source")) {
				hec.setSource(param.getValue());

			} else if (param.getName().equals("sourcetype")) {
				hec.setSourcetype(param.getValue());

			} else if (param.getName().equals("index")) {
				hec.setIndex(param.getValue());

			} else if (param.getName().equals("hec_batch_mode")) {
				try {
					hec.setBatchMode(Boolean.parseBoolean(param.getValue()
							.equals("1") ? "true" : "false"));
				} catch (Exception e) {
					logger.error("Can't determine batch_mode value, will revert to default value.");
				}

			} else if (param.getName().equals("hec_max_batch_size_bytes")) {
				try {
					hec.setMaxBatchSizeBytes(Long.parseLong(param.getValue()));
				} catch (NumberFormatException e) {
					logger.error("Can't determine max_batch_size_bytes value, will revert to default value.");
				}

			} else if (param.getName().equals("hec_max_batch_size_events")) {
				try {
					hec.setMaxBatchSizeEvents(Long.parseLong(param.getValue()));
				} catch (NumberFormatException e) {
					logger.error("Can't determine max_batch_size_events value, will revert to default value.");
				}

			} else if (param.getName().equals(
					"hec_max_inactive_time_before_batch_flush")) {
				try {
					hec.setMaxInactiveTimeBeforeBatchFlush(Long.parseLong(param
							.getValue()));
				} catch (NumberFormatException e) {
					logger.error("Can't determine max_inactive_time_before_batch_flush value, will revert to default value.");
				}
			}
		}
		try {
			instance = (Transport) Class.forName(transports.get(key))
					.newInstance();
			instance.setStanzaName(stanzaName);
			if (key.equalsIgnoreCase("hec"))
				instance.init(hec);

		} catch (Exception e) {
			logger.error("Error instantiating transport : " + e.getMessage());
		}
		return instance;

	}

	public static void marshallObjectToXML(Object obj) {
		try {
			JAXBContext context = JAXBContext.newInstance(obj.getClass());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			StringWriter sw = new StringWriter();
			marshaller.marshal(obj, sw);
			String xml = sw.toString();
			logger.info("Data sent to Splunk:" + xml);
			logger.info("Size of data sent to Splunk:" + xml.length());
			System.out.println(xml.trim());

		} catch (Exception e) {
			logger.error("Error writing XML : " + e.getMessage());
		}
	}

	public static String marshallObjectToXMLString(Object obj) {
		String xml = "";
		try {
			JAXBContext context = JAXBContext.newInstance(obj.getClass());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			StringWriter sw = new StringWriter();
			marshaller.marshal(obj, sw);
			xml = sw.toString();

		} catch (Exception e) {
			logger.error("Error getting XML String : " + e.getMessage());
		}
		return xml.trim();
	}

	protected static Object unmarshallXMLToObject(Class clazz, String xml) {
		try {
			JAXBContext context = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			StringReader sr = new StringReader(xml);
			Object obj = unmarshaller.unmarshal(sr);

			return obj;

		} catch (Exception e) {
			logger.error("Error parsing XML : " + e.getMessage());
		}
		return null;
	}

	protected static void sendScheme(Scheme scheme) {
		marshallObjectToXML(scheme);

	}

	protected static void sendValidationError(ValidationError error) {
		marshallObjectToXML(error);

	}

	protected static Input getInput(String xml) {
		return (Input) unmarshallXMLToObject(Input.class, xml);
	}

	protected static Validation getValidation(String xml) {

		return (Validation) unmarshallXMLToObject(Validation.class, xml);
	}

	protected void doScheme() {

		try {
			logger.info("Getting scheme");
			Scheme scheme = getScheme();
			sendScheme(scheme);
			System.exit(0);
			logger.info("Scheme sent");
		} catch (Exception e) {
			logger.error("Error getting scheme : " + e.getMessage());
			System.exit(2);
		}

	}

	protected void init(String[] args) {

		String loggingLevel = System.getProperty("splunk.logging.level",
				"ERROR");
		logger.setLevel(Level.toLevel(loggingLevel));
		logger.info("Initialising Modular Input");
		try {
			if (args.length == 1) {
				if (args[0].equals("--scheme")) {
					doScheme();
				} else {
					Input input = getInput(args[0]);

					new SplunkConnectionPoller(input).start();
					runStateCheckerThread(input);
					doRun(input);

				}
			} else if (args.length == 2) {
				if (args[0].equals("--validate-arguments")) {

					logger.info("Validating arguments");
					Validation val = getValidation(args[1]);
					doValidate(val);
				}
			} else {
				logger.error("Incorrect Program Usage");
				System.exit(2);
			}
		} catch (Exception e) {
			logger.error("Error executing modular input : " + e.getMessage()
					+ " : " + ModularInput.getStackTrace(e));

			System.exit(2);
		}

	}

	protected void createTCPInput(Input input, int tcpPort, String index,
			String sourcetype, String source) {

		String host = input.getServer_host();
		String uri = input.getServer_uri();
		int port = 8089;
		String token = input.getSession_key();
		try {
			int portOffset = uri.indexOf(":", 8);
			port = Integer.parseInt(uri.substring(portOffset + 1));
		} catch (Exception e) {

		}
		Service service = new Service("localhost", port);
		service.setToken("Splunk " + token);
		service.version = service.getInfo().getVersion();

		InputCollection inputs = service.getInputs();

		if (!inputs.containsKey(String.valueOf(tcpPort))) {
			Args args = new Args();
			args.add("index", index);
			args.add("sourcetype", sourcetype);
			args.add("source", sourcetype);

			service.getInputs().create(String.valueOf(tcpPort), InputKind.Tcp,
					args);
		}
	}

	protected String createHECInput(Input input, int hecPort, String index,
			String sourcetype, String source, boolean https) {

		String host = input.getServer_host();
		String uri = input.getServer_uri();
		int port = 8089;
		String token = input.getSession_key();
		try {
			int portOffset = uri.indexOf(":", 8);
			port = Integer.parseInt(uri.substring(portOffset + 1));
		} catch (Exception e) {

		}
		Service service = new Service("localhost", port);
		service.setToken("Splunk " + token);
		service.version = service.getInfo().getVersion();

		// TODO
		// if hec input does not exist , create it.
		// if hec input does exist , get the token
		String hecToken = "";
		return hecToken;
	}

	// polls splunkd via REST API to check whether input is enabled/disabled
	private void runStateCheckerThread(Input input) {

		// init the state map based on the passed in values from Splunk
		List<Stanza> stanzas = input.getStanzas();
		for (Stanza stanza : stanzas) {
			List<Param> params = stanza.getParams();
			setDisabled(stanza.getName(), false);// default setting
			for (Param param : params) {
				if (param.getName().equals("disabled")) {
					String val = param.getValue();
					setDisabled(
							stanza.getName(),
							val.equals("0") || val.equalsIgnoreCase("false") ? false
									: true);

				}
			}
		}
		// get params required for REST call to splunkd
		String host = input.getServer_host();
		String uri = input.getServer_uri();
		int port = 8089;
		String token = input.getSession_key();
		try {
			int portOffset = uri.indexOf(":", 8);
			port = Integer.parseInt(uri.substring(portOffset + 1));
		} catch (Exception e) {

		}
		Service service = new Service("localhost", port);
		service.setToken("Splunk " + token);

		service.version = service.getInfo().getVersion();

		StateCheckerThread checker = new StateCheckerThread(service);
		checker.start();

	}

	class StateCheckerThread extends Thread {

		Service service;

		StateCheckerThread(Service service) {
			this.service = service;
		}

		public void run() {

			logger.info("Running state checker");
			int enabledCount = 1;
			while (enabledCount > 0) {
				enabledCount = 0;
				Set<String> stanzas = inputStates.keySet();
				for (String stanza : stanzas) {
					try {
						int index = stanza.indexOf("://");
						// REST call to get state of input
						com.splunk.Input input = service.getInputs().get(
								stanza.substring(index + 3));
						boolean isDisabled = input.isDisabled();
						// update state map
						setDisabled(stanza, isDisabled);
						enabledCount += isDisabled ? 0 : 1;
					} catch (Exception e) {
						logger.error("Can't connect to Splunk REST API with the token ["
								+ service.getToken()
								+ "], either the token is invalid or SplunkD has exited : "
								+ e.getMessage());
					}
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}

			}
			logger.error("It has been determined via the REST API that all inputs have been disabled");
			System.exit(2);
		}

	}

	protected synchronized static boolean isDisabled(String stanza) {

		if (!inputStates.containsKey(stanza))
			return true;
		return inputStates.get(stanza);

	}

	protected synchronized static void setDisabled(String stanza,
			boolean isDisabled) {

		inputStates.put(stanza, isDisabled);

	}

	class SplunkConnectionPoller extends Thread {

		String splunkHost;
		int port = 8089;

		SplunkConnectionPoller(Input input) {

			this.splunkHost = input.getServer_host();
			String uri = input.getServer_uri();
			try {
				int portOffset = uri.indexOf(":", 8);
				this.port = Integer.parseInt(uri.substring(portOffset + 1));
			} catch (Exception e) {
			}

		}

		public void run() {
			try {
				logger.info("Running connection poller");
				int failCount = 0;
				while (true) {
					Socket socket = null;
					connectedToSplunk = false;
					try {
						socket = new Socket(this.splunkHost, this.port);
						connectedToSplunk = true;
						failCount = 0;
					} catch (Exception e) {
						logger.error("Probing socket connection to SplunkD failed.Either SplunkD has exited ,or if not,  check that your DNS configuration is resolving your system's hostname ("
								+ this.splunkHost
								+ ") correctly : "
								+ e.getMessage());
						failCount++;
						connectedToSplunk = false;
					} finally {
						if (socket != null)
							try {
								socket.close();
							} catch (Exception e) {
							}
					}
					if (failCount >= 3) {
						logger.error("Determined that Splunk has probably exited, HARI KARI.");
						System.exit(2);
					} else {
						Thread.sleep(10000);
					}

				}

			} catch (Exception e) {
			}

		}

	}

	// extending classes must implement these
	protected abstract void doRun(Input input) throws Exception;

	protected abstract void doValidate(Validation val);

	protected abstract Scheme getScheme();

	public static String getStackTrace(Throwable aThrowable) {
		Writer result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

	protected void setJVMSystemProperties(String propsString) {

		try {
			StringTokenizer st = new StringTokenizer(propsString, ",");
			while (st.hasMoreTokens()) {
				StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
				while (st2.hasMoreTokens()) {
					System.setProperty(st2.nextToken(), st2.nextToken());

				}
			}
		} catch (Throwable e) {
			logger.error("Error setting JVM system propertys from string : "
					+ propsString + " : " + getStackTrace(e));
		}

	}

}
