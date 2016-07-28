package com.splunk.modinput.protocol.handlerverticle;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.splunk.modinput.ModularInput;

public class GZipHandler  extends Verticle {

	private static Logger logger = Logger.getLogger(GZipHandler.class);

	public void start() {

		JsonObject config = container.config();
		
		
		String eventBusAddress = config.getString("address");
		final String outputAddress = config.getString("output_address");
		
		// Event Bus (so we can receive the data)
		final EventBus eb = vertx.eventBus();

		Handler<Message<byte[]>> myHandler = new Handler<Message<byte[]>>() {
			public void handle(Message<byte[]> message) {

				try {
				    
				    //actual received data
					byte[] data = message.body();

                    //decompress gzip content
					String output = decompress(data);

					//pass along to output handler
					eb.send(outputAddress, output);

				} catch (Exception e) {
					logger.error("Error writing received data: "
							+ ModularInput.getStackTrace(e));
				}

			}
		};

		eb.registerHandler(eventBusAddress, myHandler);

	}
	
	public String decompress(byte[] data) throws Exception {
		GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data));
		BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
		StringBuffer outStr =new StringBuffer();
		String line;
		while ((line = bf.readLine()) != null) {
			outStr.append(line);
		}
		bf.close();
		return outStr.toString();
	}

}
