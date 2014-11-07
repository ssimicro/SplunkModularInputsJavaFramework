import org.apache.log4j.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import org.vertx.java.platform.Verticle;

import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Stream;

public class ExampleHandler extends Verticle {

	private static Logger logger = Logger.getLogger(ExampleHandler.class);

	public void start() {

		JsonObject config = container.config();
		final String stanza = config.getString("stanza");
		String eventBusAddress = config.getString("address");
		EventBus eb = vertx.eventBus();

		Handler<Message<byte[]>> myHandler = new Handler<Message<byte[]>>() {
			public void handle(Message<byte[]> message) {

				try {
				    
				    //actual received data
					byte[] data = message.body();

                    //pre-processing / transforming the received data
					String output = "Ignoring received data and replacing with JAVA GOATS!!!!!!";

					//bundle up String output and send to Splunk
					Stream stream = HandlerUtil.getStream(output, stanza);
					ModularInput.marshallObjectToXML(stream);

				} catch (Exception e) {
					logger.error("Error writing received data: "
							+ ModularInput.getStackTrace(e));
				}

			}
		};

		eb.registerHandler(eventBusAddress, myHandler);

	}

}
