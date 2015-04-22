package com.splunk.modinput.coap;

import java.util.Map;



import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.coap.COAPModularInput.MessageReceiver;

public abstract class AbstractMessageHandler {

	public abstract Stream handleMessage(byte [] message,MessageReceiver context) throws Exception;

	public abstract void setParams(Map<String, String> params);

	protected SplunkLogEvent buildCommonEventMessagePart(MessageReceiver context)
			throws Exception {

		SplunkLogEvent event = new SplunkLogEvent("coap_msg_received",
				 "", true, true);

		
		return event;

	}

	protected String getMessageBody(byte []  message) {

		return new String(message);

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
