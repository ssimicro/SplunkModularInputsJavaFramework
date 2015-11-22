package com.splunk.modinput.mqtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttMessage;


import com.splunk.modinput.mqtt.MQTTModularInput.MessageReceiver; 

public class BodyOnlyMessageHandler extends AbstractMessageHandler {

	@Override
	public void handleMessage(String topic, MqttMessage message,MessageReceiver context)
			throws Exception {

		
		long timestamp = System.currentTimeMillis();
		String text = timestamp +" "+getMessageBody(message);
		transportMessage(text,String.valueOf(System.currentTimeMillis()),"");

	}

	public static List<String> chunkData(String text, int size) {

		List<String> ret = new ArrayList<String>((text.length() + size - 1)
				/ size);

		for (int start = 0; start < text.length(); start += size) {
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}

	@Override
	public void setParams(Map<String, String> params) {
		// Do nothing , params not used

	}

}
