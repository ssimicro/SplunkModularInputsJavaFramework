package com.splunk.modinput.protocol.protocolverticle;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.datagram.DatagramPacket;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.datagram.InternetProtocolFamily;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import org.vertx.java.platform.Verticle;

import com.splunk.modinput.ModularInput;

/**
 * A UDP Server
 * 
 * @author ddallimore
 * 
 */
public class UDPVerticle extends Verticle {

	String address = UUID.randomUUID().toString();
	protected static Logger logger = Logger.getLogger(UDPVerticle.class);

	public void start() {

		JsonObject config = container.config();

		JsonObject handlerConfig = new JsonObject(
				config.getString("handler_config"));

		handlerConfig.putString("address", address);

		container.deployWorkerVerticle(config.getString("handler_verticle"),
				handlerConfig, config.getNumber("handler_verticle_instances")
						.intValue(), false, new AsyncResultHandler<String>() {
					public void handle(AsyncResult<String> asyncResult) {
						if (asyncResult.succeeded()) {
							// ok
						} else {
							logger.error("Can't instantiate handler verticle : "
									+ ModularInput.getStackTrace(asyncResult
											.cause()));

						}
					}
				});

		InternetProtocolFamily ip = InternetProtocolFamily.IPv4;
		if (config.containsField("ip_version")) {
			String version = config.getString("ip_version");
			if (version.equalsIgnoreCase("v6")) {
				ip = InternetProtocolFamily.IPv6;
			}
		}
		final DatagramSocket socket = vertx.createDatagramSocket(ip);

		final boolean isMulticast = Boolean.parseBoolean(config.getNumber(
				"is_multicast").intValue() == 1 ? "true" : "false");
		final String multicastGroup = config.getString("multicast_group");

		if (config.containsField("udp_receive_buffer_size"))
			socket.setReceiveBufferSize(config.getNumber(
					"udp_receive_buffer_size").intValue());
		if (config.containsField("set_broadcast"))
			socket.setBroadcast(Boolean.parseBoolean(config.getNumber(
					"set_broadcast").intValue() == 1 ? "true" : "false"));

		if (isMulticast) {
			if (config.containsField("set_multicast_loopback_mode"))
				socket.setMulticastLoopbackMode(Boolean
						.parseBoolean(config.getNumber(
								"set_multicast_loopback_mode").intValue() == 1 ? "true"
								: "false"));
			if (config.containsField("multicast_ttl"))
				socket.setMulticastTimeToLive(config.getNumber("multicast_ttl")
						.intValue());

		}

		int port = config.getNumber("port").intValue();

		String bindAddress = "localhost";

		if (config.containsField("bind_address"))
			bindAddress = config.getString("bind_address");

		socket.listen(bindAddress, port,
				new AsyncResultHandler<DatagramSocket>() {
					public void handle(AsyncResult<DatagramSocket> asyncResult) {
						if (asyncResult.succeeded()) {

							socket.dataHandler(new Handler<DatagramPacket>() {
								public void handle(DatagramPacket packet) {
									EventBus eb = vertx.eventBus();
									eb.send(address, packet.data().getBytes());
								}
							});
							// join the multicast group
							if (isMulticast) {
								socket.listenMulticastGroup(
										multicastGroup,
										new AsyncResultHandler<DatagramSocket>() {
											public void handle(
													AsyncResult<DatagramSocket> asyncResult) {
												if (asyncResult.succeeded()) {
													socket.dataHandler(new Handler<DatagramPacket>() {
														public void handle(
																DatagramPacket packet) {
															EventBus eb = vertx
																	.eventBus();
															eb.send(address,
																	packet.data()
																			.getBytes());
														}
													});
												} else {
													logger.error(
															"Multicast Listen failed",
															asyncResult.cause());
												}
											}
										});
							}
						} else {
							logger.error("Listen failed", asyncResult.cause());
						}
					}
				});
		socket.exceptionHandler(new Handler<Throwable>() {
			public void handle(Throwable t) {
				logger.error("Error in the UDP Server: "
						+ ModularInput.getStackTrace(t));
			}
		});

	}
}
