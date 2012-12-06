package com.splunk.modinput.jms;

import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

public interface LocalJMSResourceFactory {

	public void setParams(Map<String, String> params);

	public Topic createTopic();

	public Queue createQueue();

	public ConnectionFactory createConnectionFactory();

}
