package com.splunk.modinput.mqtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttMessage;


import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;
import com.splunk.modinput.mqtt.MQTTModularInput.MessageReceiver; 

public class BodyOnlyMessageHandler extends AbstractMessageHandler {

	@Override
	public Stream handleMessage(String topic, MqttMessage message,MessageReceiver context)
			throws Exception {

		
		long timestamp = System.currentTimeMillis();
		String text = timestamp +" "+getMessageBody(message);
		Stream stream = new Stream();
		ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();
		List<String> chunks = chunkData(text, 1024);

		for (int i = 0; i < chunks.size(); i++) {
			StreamEvent event = new StreamEvent();
			event.setUnbroken("1");
			event.setData(chunks.get(i));
			event.setStanza(context.stanzaName);
			// if we are seeing the last chunk, set the "done" element
			if (i == chunks.size() - 1)
				event.setDone(" ");
			list.add(event);
		}
		stream.setEvents(list);
		return stream;

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
