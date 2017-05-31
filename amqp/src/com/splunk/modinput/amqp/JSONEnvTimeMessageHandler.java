package com.splunk.modinput.amqp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import com.splunk.modinput.amqp.AMQPModularInput.MessageReceiver;

public class JSONEnvTimeMessageHandler extends AbstractMessageHandler {

	@Override
	public void handleMessage(byte[] messageContents, Envelope envelope,
			AMQP.BasicProperties messageProperties, MessageReceiver context)
			throws Exception {

		
		String jsonBody = getMessageBody(messageContents);
		
		String text = jsonBody.toString();
		
		Date when = messageProperties.getTimestamp();
		long when_ms = when == null ? System.currentTimeMillis() : when.getTime();
		transportMessage(text, String.valueOf(when_ms), "");
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
