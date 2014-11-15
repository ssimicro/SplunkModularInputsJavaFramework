package com.splunk.modinput.protocol.handlerverticle;

import org.apache.log4j.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import org.vertx.java.platform.Verticle;

import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Stream;

/**
 * Default Handler that just parses a raw received byte array into a String
 * 
 * @author ddallimore
 * 
 */
public class DefaultHandlerVerticle extends Verticle {

	private static Logger logger = Logger
			.getLogger(DefaultHandlerVerticle.class);

	public void start() {

		// handler config JSON
		JsonObject config = container.config();

	
		// Event Bus (so we can receive the data)
		String eventBusAddress = config.getString("address");
		final String outputAddress = config.getString("output_address");
		final EventBus eb = vertx.eventBus();

		// data handler that will process our received data
		Handler<Message<byte[]>> myHandler = new Handler<Message<byte[]>>() {
			public void handle(Message<byte[]> message) {

				try {
					// raw received bytes
					byte[] data = message.body();
					// parse into String
					String output = new String(data);
					eb.send(outputAddress, output);
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
