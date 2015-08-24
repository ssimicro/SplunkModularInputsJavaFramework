package com.splunk.modinput.transport;

import java.net.URI;

import org.apache.http.HttpHost;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.log4j.Logger;

import com.splunk.modinput.ModularInput;

public class HECTransport implements Transport {

	protected static Logger logger = Logger.getLogger(HECTransport.class);

	private HECTransportConfig config;
	private CloseableHttpAsyncClient httpClient;
	private URI uri;

	@Override
	public void init(Object obj) {
		config = (HECTransportConfig) obj;

		try {

			ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
		    PoolingNHttpClientConnectionManager cm = 
		      new PoolingNHttpClientConnectionManager(ioReactor);
			cm.setMaxTotal(config.getPoolsize());

			
			HttpHost splunk = new HttpHost(config.getHost(), config.getPort());
			cm.setMaxPerRoute(new HttpRoute(splunk), config.getPoolsize());


			httpClient = HttpAsyncClients.custom().setConnectionManager(cm).build();

			uri = new URIBuilder()
					.setScheme(config.isHttps() ? "https" : "http")
					.setHost(config.getHost()).setPort(config.getPort())
					.setPath("/services/collector").build();
			
			httpClient.start();

		} catch (Exception e) {
			logger.error("Error initialising HEC Transport: "
					+ ModularInput.getStackTrace(e));
		}

	}

	@Override
	public void setStanzaName(String name) {
		// not required

	}

	@Override
	public void transport(String message) {

		try {
			
			if(!(message.startsWith("{") && message.endsWith("}")) && !(message.startsWith("\"") && message.endsWith("\"")))
				message = wrapMessageInQuotes(message);

			
			// could use a JSON Object , but the JSON is so trivial , just
			// building it with a StringBuffer
			StringBuffer json = new StringBuffer();
			json.append("{\"").append("event\":").append(message)
					.append(",\"").append("index\":\"")
					.append(config.getIndex()).append("\",\"")
					.append("source\":\"").append(config.getSource())
					.append("\",\"").append("sourcetype\":\"")
					.append(config.getSourcetype()).append("\"").append("}");

			HttpPost post = new HttpPost(uri);
			post.addHeader("Authorization", "Splunk " + config.getToken());

			StringEntity requestEntity = new StringEntity(json.toString(),
					ContentType.create("application/json", "UTF-8"));

			post.setEntity(requestEntity);
			httpClient.execute(post,null);

		} catch (Exception e) {
			logger.error("Error writing received data via HEC: "
					+ ModularInput.getStackTrace(e));
		}
	}

	private String wrapMessageInQuotes(String message) {
		
		return "\""+message+"\"";
	}

}
