package com.splunk.modinput.protocol.outputverticle;

import org.apache.log4j.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.splunk.modinput.ModularInput;

public class HECOutputVerticle extends Verticle {

	private static Logger logger = Logger.getLogger(HECOutputVerticle.class);

	HttpClient client;
	String token;
	String index;
	String source;
	String sourcetype;

	public void start() {

		// handler config JSON
		JsonObject config = container.config();

		int port = config.getNumber("hec_port").intValue();
		int poolsize = config.getNumber("hec_poolsize").intValue();
		token = config.getString("hec_token");
		index = config.getString("index");
		source = config.getString("source");
		sourcetype = config.getString("sourcetype");
		boolean useHTTPs = config.getBoolean("hec_https");
		// Event Bus (so we can receive the data)
		String eventBusAddress = config.getString("output_address");
		EventBus eb = vertx.eventBus();

		client = vertx.createHttpClient().setPort(port).setHost("localhost")
				.setMaxPoolSize(poolsize);
		if (useHTTPs) {
			client.setSSL(true);
			client.setTrustAll(true);
		}

		// data handler that will process our received data
		Handler<Message<String>> myHandler = new Handler<Message<String>>() {
			public void handle(Message<String> message) {

				try {

					String messageContent = message.body();

					if (!(messageContent.startsWith("{") && messageContent
							.endsWith("}"))
							&& !(messageContent.startsWith("\"") && messageContent
									.endsWith("\"")))
						messageContent = wrapMessageInQuotes(messageContent);

					JsonObject obj = new JsonObject();
					obj.putString("event", messageContent);
					obj.putString("index", index);
					obj.putString("source", source);
					obj.putString("sourcetype", sourcetype);

					Buffer buff = new Buffer();
					buff.appendString(obj.toString());

					HttpClientRequest request = client.post(
							"/services/collector",
							new Handler<HttpClientResponse>() {
								public void handle(HttpClientResponse resp) {
									if (resp.statusCode() != 200)
										logger.error("Got a response: "
												+ resp.statusCode());
								}
							});
					request.headers().set("Authorization", "Splunk " + token);

					request.write(buff);
					request.end();
				} catch (Exception e) {
					logger.error("Error writing received data: "
							+ ModularInput.getStackTrace(e));
				}

			}
		};

		// start listening for data
		eb.registerHandler(eventBusAddress, myHandler);

	}

	private String wrapMessageInQuotes(String message) {

		return "\"" + message + "\"";
	}

}
