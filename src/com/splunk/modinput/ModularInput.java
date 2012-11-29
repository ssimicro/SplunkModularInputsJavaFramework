package com.splunk.modinput;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

public abstract class ModularInput {

	protected static Logger logger = Logger.getLogger(ModularInput.class);

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
			logger.error(e.getMessage());
			System.exit(2);
		}

	}

	// extending classes must implement these
	protected abstract void doRun(Input input);

	protected abstract void doValidate(Validation val);

	protected abstract Scheme getScheme();

}
