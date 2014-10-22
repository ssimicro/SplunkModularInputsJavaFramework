package com.splunk.modinput.mqtt;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttMessage;


import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.mqtt.MQTTModularInput.MessageReceiver;

public abstract class AbstractMessageHandler {

	public abstract Stream handleMessage(String topic,MqttMessage message,MessageReceiver context) throws Exception;

	public abstract void setParams(Map<String, String> params);

	protected SplunkLogEvent buildCommonEventMessagePart(MessageReceiver context)
			throws Exception {

		SplunkLogEvent event = new SplunkLogEvent("mqtt_msg_received",
				 "", true, true);

		
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
