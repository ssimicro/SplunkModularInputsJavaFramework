package com.splunk.modinput.kinesis;

import java.nio.ByteBuffer;
import java.util.Map;

import com.splunk.modinput.SplunkLogEvent;

import com.splunk.modinput.kinesis.KinesisModularInput.MessageReceiver;
import com.splunk.modinput.transport.Transport;

public abstract class AbstractMessageHandler {

	private Transport transport;

	public void setTransport(Transport transport) {

		this.transport = transport;
	}

	public void transportMessage(String message, String time, String host) {

		if (transport != null)
			this.transport.transport(message, time, host);
	}

	public abstract void handleMessage(ByteBuffer  rawBytes, String seqNumber,
			String partitionKey, MessageReceiver context) throws Exception;

	public abstract void setParams(Map<String, String> params);

	protected SplunkLogEvent buildCommonEventMessagePart(MessageReceiver context)
			throws Exception {

		SplunkLogEvent event = new SplunkLogEvent("kinesis_record_received",
				"", true, false);

		return event;

	}

	protected String getMessageBody(byte[] messageContents) {

		return new String(messageContents);

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
