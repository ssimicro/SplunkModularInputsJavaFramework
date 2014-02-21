package com.splunk.modinput.jms.custom.factory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;

import com.splunk.modinput.jms.LocalJMSResourceFactory;

public class LocalActiveMQConnectionFactory implements LocalJMSResourceFactory {

	// user configurable fields
	private String brokerURL;
	private String username;
	private String password;

	public LocalActiveMQConnectionFactory() {

	}

	@Override
	public void setParams(Map<String, String> params) throws Exception {

		try {
			this.brokerURL = params.get("brokerURL");
			this.username = params.get("username");
			this.password = params.get("password");

		} catch (Throwable e) {
			throw new Exception(
					"Error setting parameters for LocalActiveMQConnectionFactory : "
							+ getStackTrace(e));
		}

	}

	@Override
	public Topic createTopic(String topicName) throws Exception {

		try {
			return new ActiveMQTopic(topicName);
		} catch (Throwable e) {
			throw new Exception("Error creating ActiveMQ Topic " + topicName
					+ " : " + getStackTrace(e));
		}
	}

	@Override
	public Queue createQueue(String queueName) throws Exception {

		try {
			return new ActiveMQQueue(queueName);
		} catch (Throwable e) {
			throw new Exception("Error creating ActiveMQ Queue " + queueName
					+ " : " + getStackTrace(e));
		}
	}

	@Override
	public ConnectionFactory createConnectionFactory() throws Exception {

		ActiveMQConnectionFactory factory = null;
		if (brokerURL == null){
			throw new Exception("Broker URL must be supplied");
		}
		try {
			if (username == null || password == null)
				factory = new ActiveMQConnectionFactory(brokerURL);
			else
				factory = new ActiveMQConnectionFactory(brokerURL, username,
						password);
		}

		catch (Throwable e) {
			throw new Exception("Error creating ActiveMQ Connection factory : "
					+ getStackTrace(e));
		}
		return factory;
	}

	public static String getStackTrace(Throwable aThrowable) {
		Writer result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

}
