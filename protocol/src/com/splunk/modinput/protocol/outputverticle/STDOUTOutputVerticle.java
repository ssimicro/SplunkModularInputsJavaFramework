package com.splunk.modinput.protocol.outputverticle;

import org.apache.log4j.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Stream;
import com.splunk.modinput.protocol.handlerverticle.HandlerUtil;

public class STDOUTOutputVerticle extends Verticle {

	private static Logger logger = Logger.getLogger(STDOUTOutputVerticle.class);

	public void start() {

		// handler config JSON
		JsonObject config = container.config();

		// Event Bus (so we can receive the data)
		String eventBusAddress = config.getString("output_address");
		final String stanza = config.getString("stanza");
		EventBus eb = vertx.eventBus();

		// data handler that will process our received data
		Handler<Message<String>> myHandler = new Handler<Message<String>>() {
			public void handle(Message<String> message) {

				try {

					// wrap in a Stream object
					Stream stream = HandlerUtil.getStream(message.body(),
							stanza);
					// marshall out to Splunk
					ModularInput.marshallObjectToXML(stream);

				} catch (Exception e) {
					logger.error("Error writing received data: "
							+ ModularInput.getStackTrace(e));
				}

			}
		};

		// start listening for data
		eb.registerHandler(eventBusAddress, myHandler);

	}

}
