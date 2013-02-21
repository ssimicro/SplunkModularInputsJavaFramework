package com.splunk.modinput;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.splunk.Service;

public abstract class ModularInput {

	protected static Logger logger = Logger.getLogger(ModularInput.class);

	protected static Map<String, Boolean> inputStates = new HashMap<String, Boolean>();

	protected boolean connectedToSplunk = false;

	protected static void marshallObjectToXML(Object obj) {
		try {
			JAXBContext context = JAXBContext.newInstance(obj.getClass());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			StringWriter sw = new StringWriter();
			marshaller.marshal(obj, sw);
			String xml = sw.toString();
			System.out.println(xml.trim());
		} catch (Exception e) {
			logger.error("Error writing XML : " + e.getMessage());
		}
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
			Scheme scheme = getScheme();
			sendScheme(scheme);
			System.exit(0);
		} catch (Exception e) {
			logger.error("Error getting scheme : " + e.getMessage());
			System.exit(2);
		}

	}

	protected void init(String[] args) {

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

					Validation val = getValidation(args[1]);
					doValidate(val);
				}
			} else {
				logger.error("Incorrect Program Usage");
				System.exit(2);
			}
		} catch (Exception e) {
			logger.error("Error executing modular input : " + e.getMessage());
			System.exit(2);
		}

	}

	// polls splunkd via REST API to check whether input is enabled/disabled
	private void runStateCheckerThread(Input input) {

		// init the state map based on the passed in values from Splunk
		List<Stanza> stanzas = input.getStanzas();
		for (Stanza stanza : stanzas) {
			List<Param> params = stanza.getParams();
			setDisabled(stanza.getName(),false);//default setting
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
		Service service = new Service(host, port);
		service.setToken("Splunk " + token);

		StateCheckerThread checker = new StateCheckerThread(service);
		checker.start();

	}

	class StateCheckerThread extends Thread {

		Service service;

		StateCheckerThread(Service service) {
			this.service = service;
		}

		public void run() {

			// an initial standoff period of 30 seconds to prevent potential
			// race conditions
			// with the SplunkD state
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e1) {
			}

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
						logger.error("Can't connect to Splunk REST API, either SplunkD has exited ,or if not,check that your DNS configuration is resolving your system's hostname ("
								+ service.getHost()
								+ ") correctly : "
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

}
