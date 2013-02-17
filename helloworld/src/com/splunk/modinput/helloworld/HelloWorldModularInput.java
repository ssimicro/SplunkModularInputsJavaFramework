package com.splunk.modinput.helloworld;

import java.util.ArrayList;
import java.util.Date;

import java.util.List;

import com.splunk.modinput.Arg;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.Item;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;
import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;

import com.splunk.modinput.Stanza;

import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;
import com.splunk.modinput.Scheme.StreamingMode;

public class HelloWorldModularInput extends ModularInput {

	public static void main(String[] args) {

		HelloWorldModularInput instance = new HelloWorldModularInput();
		instance.init(args);

	}

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();

				if (name != null) {

					List<Param> params = stanza.getParams();
					for (Param param : params) {
						String value = param.getValue();
						if (value == null) {
							continue;
						}

						if (param.getName().equals("some_property")) {

							new EchoThread(value, name).start();

						}
					}

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

	class EchoThread extends Thread {

		String value;
		String stanzaName;

		EchoThread(String value, String stanzaName) {

			this.value = value;
			this.stanzaName = stanzaName;
		}

		public void run() {

			while (!isDisabled(stanzaName)) {

				String eventString = new Date() + " some_property=" + value;
				Stream stream = new Stream();

				StreamEvent event = new StreamEvent();
				event.setData(eventString);
				event.setStanza(stanzaName);
				ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();
				list.add(event);
				stream.setEvents(list);
				marshallObjectToXML(stream);
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {

				}
			}

		}
	}

	@Override
	protected void doValidate(Validation val) {

		try {

			if (val != null) {

				List<Item> items = val.getItems();
				for (Item item : items) {
					List<Param> params = item.getParams();

					for (Param param : params) {
						if (param.getName().equals("some_property")) {
							String value = param.getValue();
							if (value.length() < 2) {
								throw new Exception(
										"Some Property must be at least 2 characters long");
							}
						}
					}
				}
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
		scheme.setTitle("Hello World");
		scheme.setDescription("Hello World Modular Input");
		scheme.setUse_external_validation(true);
		scheme.setUse_single_instance(true);
		scheme.setStreaming_mode(StreamingMode.XML);

		Endpoint endpoint = new Endpoint();

		Arg arg = new Arg();
		arg.setName("name");
		arg.setTitle("Input Name");
		arg.setDescription("Name of the input");

		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("some_property");
		arg.setTitle("Some Property");
		arg.setDescription("Some property that you want to set");
		endpoint.addArg(arg);

		scheme.setEndpoint(endpoint);

		return scheme;
	}

}
