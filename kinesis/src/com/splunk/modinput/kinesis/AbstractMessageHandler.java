package com.splunk.modinput.kinesis;

import java.util.Map;


import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.kinesis.KinesisModularInput.MessageReceiver;

public abstract class AbstractMessageHandler {

	public abstract Stream handleMessage(String record,String seqNumber,String partitionKey,MessageReceiver context) throws Exception;

	public abstract void setParams(Map<String, String> params);

	protected SplunkLogEvent buildCommonEventMessagePart(MessageReceiver context)
			throws Exception {

		SplunkLogEvent event = new SplunkLogEvent("kinesis_record_received",
				 "", true, true);

		
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
