package com.splunk.modinput.protocol.handlerverticle;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import org.vertx.java.platform.Verticle;

import com.splunk.modinput.ModularInput;

public class TokenAuthHandler extends Verticle {

	private static Logger logger = Logger.getLogger(TokenAuthHandler.class);

	private String TOKEN = "";

	public void start() {

		// handler config JSON
		JsonObject config = container.config();

		TOKEN = config.getString("token");
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
					Map<String, String> bodyParams = getParamMap(output);

					String token = bodyParams.get("token");
					String body = bodyParams.get("body");

					if (token.equals(TOKEN)) {
						// pass along to output handler
						eb.send(outputAddress, body);
					} else {
						throw new Exception("Auth token is not valid");
					}
				} catch (Exception e) {
					logger.error("Error writing received data: " + ModularInput.getStackTrace(e));
				}

			}
		};

		// start listening for data
		eb.registerHandler(eventBusAddress, myHandler);

	}

	private Map<String, String> getParamMap(String bodyParams) {

		Map<String, String> map = new HashMap<String, String>();

		try {
			StringTokenizer st = new StringTokenizer(bodyParams, ",");
			while (st.hasMoreTokens()) {
				StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
				while (st2.hasMoreTokens()) {
					map.put(st2.nextToken(), st2.nextToken());
				}
			}
		} catch (Exception e) {

		}

		return map;

	}

}