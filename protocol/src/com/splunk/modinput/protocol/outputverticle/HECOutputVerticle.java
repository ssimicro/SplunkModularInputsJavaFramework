package com.splunk.modinput.protocol.outputverticle;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
	boolean hecBatchMode;
	long hecMaxBatchSizeBytes;
	long hecMaxBatchSizeEvents;
	long hecMaxInactiveTimeBeforeBatchFlush;

	// batch buffer
	private List<String> batchBuffer;
	private long currentBatchSizeBytes = 0;
	private long lastEventReceivedTime;

	public void start() {

		// handler config JSON
		JsonObject config = container.config();

		int port = config.getNumber("hec_port").intValue();
		int poolsize = config.getNumber("hec_poolsize").intValue();
		this.token = config.getString("hec_token");
		this.index = config.getString("index");
		this.source = config.getString("source");
		this.sourcetype = config.getString("sourcetype");
		boolean useHTTPs = config.getBoolean("hec_https");

		this.hecBatchMode = config.getBoolean("hec_batch_mode");
		this.hecMaxBatchSizeBytes = config
				.getNumber("hec_max_batch_size_bytes").longValue();
		this.hecMaxBatchSizeEvents = config.getNumber(
				"hec_max_batch_size_events").longValue();
		this.hecMaxInactiveTimeBeforeBatchFlush = config.getNumber(
				"hec_max_inactive_time_before_batch_flush").longValue();

		this.batchBuffer = Collections
				.synchronizedList(new LinkedList<String>());
		this.lastEventReceivedTime = System.currentTimeMillis();

		// Event Bus (so we can receive the data)
		String eventBusAddress = config.getString("output_address");
		EventBus eb = vertx.eventBus();

		client = vertx.createHttpClient().setPort(port).setHost("localhost")
				.setMaxPoolSize(poolsize);
		if (useHTTPs) {
			client.setSSLContext(getSSLContext());
			client.setVerifyHost(false);
			client.setSSL(true);
			client.setTrustAll(true);
		}

		// data handler that will process our received data
		Handler<Message<String>> myHandler = new Handler<Message<String>>() {

			public void handle(Message<String> message) {

				try {

					String messageContent = escapeMessageIfNeeded(message
							.body());

					StringBuffer json = new StringBuffer();
					json.append("{\"").append("event\":")
							.append(messageContent).append(",\"");

					if (!index.equalsIgnoreCase("default"))
						json.append("index\":\"").append(index).append("\",\"");

					json.append("source\":\"").append(source).append("\",\"")
							.append("sourcetype\":\"").append(sourcetype)
							.append("\"").append("}");

					String bodyContent = json.toString();

					if (hecBatchMode) {
						lastEventReceivedTime = System.currentTimeMillis();
						currentBatchSizeBytes += bodyContent.length();
						batchBuffer.add(bodyContent);
						if (flushBuffer()) {
							bodyContent = rollOutBatchBuffer();
							batchBuffer.clear();
							currentBatchSizeBytes = 0;
							hecPost(bodyContent);
						}
					} else {
						hecPost(bodyContent);
					}

				} catch (Exception e) {
					logger.error("Error writing received data: "
							+ ModularInput.getStackTrace(e));
				}

			}

			/**
			 * from Tivo
			 * 
			 * @param message
			 * @return
			 */
			private String escapeMessageIfNeeded(String message) {
				String trimmedMessage = message.trim();
				if (trimmedMessage.startsWith("{")
						&& trimmedMessage.endsWith("}")) {
					// this is *probably* JSON.
					return trimmedMessage;
				} else if (trimmedMessage.startsWith("\"")
						&& trimmedMessage.endsWith("\"")
						&& !message.substring(1, message.length() - 1)
								.contains("\"")) {
					// this appears to be a quoted string with no internal
					// quotes
					return trimmedMessage;
				} else {
					// don't know what this thing is, so need to escape all
					// quotes, and
					// then wrap the result in quotes
					return "\"" + message.replace("\"", "\\\"") + "\"";
				}
			}

		};

		if (hecBatchMode) {
			new BatchBufferActivityCheckerThread().start();
		}
		// start listening for data
		eb.registerHandler(eventBusAddress, myHandler);

	}

	class BatchBufferActivityCheckerThread extends Thread {

		BatchBufferActivityCheckerThread() {

		}

		public void run() {

			while (true) {
				String currentMessage = "";
				try {
					long currentTime = System.currentTimeMillis();
					if ((currentTime - lastEventReceivedTime) >= hecMaxInactiveTimeBeforeBatchFlush) {
						if (batchBuffer.size() > 0) {
							currentMessage = rollOutBatchBuffer();
							batchBuffer.clear();
							currentBatchSizeBytes = 0;
							hecPost(currentMessage);
						}
					}

					Thread.sleep(1000);
				} catch (Exception e) {
				}

			}
		}
	}

	private void hecPost(String bodyContent) throws Exception {

		Buffer buff = new Buffer();
		buff.appendString(bodyContent);

		HttpClientRequest request = client.post("/services/collector",
				new Handler<HttpClientResponse>() {
					public void handle(HttpClientResponse resp) {
						if (resp.statusCode() != 200)
							logger.error("Got a response: " + resp.statusCode());
					}
				});
		request.headers().set("Authorization", "Splunk " + token);
		request.headers().set("Content-Length",
				String.valueOf(bodyContent.length()));
		request.write(buff);

		request.end();

	}

	private boolean flushBuffer() {

		return (currentBatchSizeBytes >= hecMaxBatchSizeBytes)
				|| (batchBuffer.size() >= hecMaxBatchSizeEvents);

	}

	private String rollOutBatchBuffer() {

		StringBuffer sb = new StringBuffer();

		for (String event : batchBuffer) {
			sb.append(event);
		}

		return sb.toString();
	}

	private SSLContext getSSLContext() {
		TrustManager[] trustAll = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
			}
		} };
		SSLContext context = null;
		try {
			context = SSLContext.getInstance("TLSv1.2");
			context.init(null, trustAll, new java.security.SecureRandom());
			return context;
		} catch (Exception e) {
			logger.error("Error setting up SSL context: " + e, e);
		}
		return context;

	}

	private String wrapMessageInQuotes(String message) {

		return "\"" + message + "\"";
	}

}
