package com.splunk.modinput;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

public abstract class ModularInput {

	protected static Logger logger = Logger.getLogger(ModularInput.class);

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
			logger.error(e.getMessage());
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
			logger.error(e.getMessage());
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
			logger.error(e.getMessage());
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
					doRun(input,false);
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
			logger.error(e.getMessage());
			System.exit(2);
		}

	}

	class SplunkConnectionPoller extends Thread {

		String splunkHost;
		int port = 8089;

		SplunkConnectionPoller(Input input) {

			this.splunkHost = input.getServer_host();
			String uri = input.getServer_uri();
			try {
				int portOffset = uri.indexOf(":", 8);
				port = Integer.parseInt(uri.substring(portOffset + 1));
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
	protected abstract void doRun(Input input,boolean validationConnectionMode) throws Exception;

	protected abstract void doValidate(Validation val);

	protected abstract Scheme getScheme();

}
