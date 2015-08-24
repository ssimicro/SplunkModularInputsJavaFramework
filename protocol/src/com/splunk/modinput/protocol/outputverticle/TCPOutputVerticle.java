package com.splunk.modinput.protocol.outputverticle;

import org.apache.log4j.Logger;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import com.splunk.modinput.ModularInput;

public class TCPOutputVerticle extends Verticle {

	private static Logger logger = Logger.getLogger(TCPOutputVerticle.class);

	NetSocket socket;

	public void start() {

		// handler config JSON
		JsonObject config = container.config();

		int port = config.getNumber("output_port").intValue();

		// Event Bus (so we can receive the data)
		String eventBusAddress = config.getString("output_address");
		EventBus eb = vertx.eventBus();

		NetClient client = vertx.createNetClient();
		client.setReconnectAttempts(1000);
		client.setReconnectInterval(500);

		client.connect(port, "localhost", new AsyncResultHandler<NetSocket>() {
			public void handle(AsyncResult<NetSocket> asyncResult) {
				if (asyncResult.succeeded()) {
					// succcess
					socket = asyncResult.result();
					socket.exceptionHandler(new Handler<Throwable>() {
						public void handle(Throwable t) {
							logger.error("Error in the TCP Output Socket: "
									+ ModularInput.getStackTrace(t));
						}
					});
				} else {
					logger.error("Can't instantiate tcp output connection : "
							+ ModularInput.getStackTrace(asyncResult.cause()));
				}
			}
		});

		// data handler that will process our received data
		Handler<Message<String>> myHandler = new Handler<Message<String>>() {
			public void handle(Message<String> message) {

				try {
					Buffer buff = new Buffer();
					buff.appendString(message.body());
					socket.write(buff);
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
