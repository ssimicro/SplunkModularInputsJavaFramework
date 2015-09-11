package com.splunk.modinput.mqtt;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttMessage;


import com.splunk.modinput.SplunkLogEvent;

import com.splunk.modinput.mqtt.MQTTModularInput.MessageReceiver;
import com.splunk.modinput.transport.Transport;

public abstract class AbstractMessageHandler {

	private Transport transport;

    public void setTransport(Transport transport){
		
		this.transport = transport;
	}
	
	public void transportMessage(String message){
		
		if(transport != null)
		  this.transport.transport(message);
	}
	
	public abstract void handleMessage(String topic,MqttMessage message,MessageReceiver context) throws Exception;

	public abstract void setParams(Map<String, String> params);

	protected SplunkLogEvent buildCommonEventMessagePart(MessageReceiver context)
			throws Exception {

		SplunkLogEvent event = new SplunkLogEvent("mqtt_msg_received",
				 "", true, false);

		
		return event;

	}

	protected String getMessageBody(MqttMessage message) {

		return new String(message.getPayload());

	}

	protected String stripNewlines(String input) {

		if (input == null) {
			return "";
		}
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (Character.isWhitespace(chars[i])) {
				chars[i] = ' ';
			}
		}

		return new String(chars);
	}

}
