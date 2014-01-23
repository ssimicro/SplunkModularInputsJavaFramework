package com.splunk.modinput.jms;

import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

public interface LocalJMSResourceFactory {

	public void setParams(Map<String, String> params) throws Exception;

	public Topic createTopic(String topicName) throws Exception;

	public Queue createQueue(String queueName) throws Exception;

	public ConnectionFactory createConnectionFactory() throws Exception;

}
