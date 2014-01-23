package com.splunk.modinput.jms.custom.factory;

import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import com.ibm.mq.jms.MQTopic;
import com.ibm.msg.client.wmq.WMQConstants;
import com.splunk.modinput.jms.LocalJMSResourceFactory;

/**
 * Custom MQConnectionFactory that use the MQ specific JMS classes. Also has
 * properties that allow for SSL connections.
 * 
 * @author ddallimore
 * 
 */
public class LocalMQConnectionFactory implements LocalJMSResourceFactory {

	// user configurable fields
	private boolean sslEnabled;
	private String sslCipherSuite;
	private int transportType;
	private int port;
	private String sslTrustStore;
	private String sslKeyStore;
	private String sslKeyStorePassword;
	private String queueManager;
	private String channel;
	private String hostName;
	private String clientID;

	
	public LocalMQConnectionFactory() {
		
	}

	@Override
	public void setParams(Map<String, String> params) throws Exception {

		try {
			String sslEnabled = params.get("sslEnabled");
			this.sslEnabled = sslEnabled != null ? Boolean
					.parseBoolean(sslEnabled) : false;

			String sslCipherSuite = params.get("sslCipherSuite");
			this.sslCipherSuite = sslCipherSuite != null ? sslCipherSuite
					: "SSL_RSA_WITH_NULL_MD5";

			String transportType = params.get("transportType");
			this.transportType = transportType != null ? Integer
					.parseInt(transportType) : WMQConstants.WMQ_CM_CLIENT;

			String port = params.get("port");
			this.port = port != null ? Integer.parseInt(port) : 1414;

			String sslTrustStore = params.get("sslTrustStore");
			this.sslTrustStore = sslTrustStore != null ? sslTrustStore : null;

			String sslKeyStore = params.get("sslKeyStore");
			this.sslKeyStore = sslKeyStore != null ? sslKeyStore : null;

			String sslKeyStorePassword = params.get("sslKeyStorePassword");
			this.sslKeyStorePassword = sslKeyStorePassword != null ? sslKeyStorePassword
					: null;

			String queueManager = params.get("queueManager");
			this.queueManager = queueManager != null ? queueManager : null;

			String channel = params.get("channel");
			this.channel = channel != null ? channel : null;

			String hostName = params.get("hostName");
			this.hostName = hostName != null ? hostName : null;

			String clientID = params.get("clientID");
			this.clientID = clientID != null ? clientID : null;
		} catch (Exception e) {
			throw new Exception(
					"Error setting parameters for LocalMQConnectionFactory : "
							+ e.getMessage());
		}

	}

	@Override
	public Topic createTopic(String topicName) throws Exception {

		try {
			return new MQTopic(topicName);
		} catch (Exception e) {
			throw new Exception("Error creating MQ Topic " + topicName + " : "
					+ e.getMessage());
		}
	}

	@Override
	public Queue createQueue(String queueName) throws Exception {

		try {
			return new MQQueue(queueName);
		} catch (Exception e) {
			throw new Exception("Error creating MQ Queue " + queueName + " : "
					+ e.getMessage());
		}
	}

	@Override
	public ConnectionFactory createConnectionFactory() throws Exception {

		MQConnectionFactory factory = new MQConnectionFactory();
		try {

			factory.setTransportType(transportType);
			factory.setPort(port);

			if (queueManager != null)
				factory.setQueueManager(queueManager);
			if (channel != null)
				factory.setChannel(channel);
			if (hostName != null)
				factory.setHostName(hostName);
			if (clientID != null)
				factory.setClientID(clientID);

			if (sslEnabled) {

				if (sslTrustStore != null)
					System.setProperty("javax.net.ssl.trustStore",
							sslTrustStore);
				if (sslKeyStore != null)
					System.setProperty("javax.net.ssl.keyStore", sslKeyStore);
				if (sslKeyStorePassword != null)
					System.setProperty("javax.net.ssl.keyStorePassword",
							sslKeyStorePassword);

				factory.setSSLCipherSuite(sslCipherSuite);

			}

		} catch (Exception e) {
			throw new Exception("Error creating MQ Connection factory : "
					+ e.getMessage());
		}
		return factory;
	}

}
