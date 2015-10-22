package com.splunk.modinput.kafka;

import java.util.Map;


import com.splunk.modinput.SplunkLogEvent;

import com.splunk.modinput.kafka.KafkaModularInput.MessageReceiver;
import com.splunk.modinput.transport.Transport;

public abstract class AbstractMessageHandler {
	
	private Transport transport;

    public void setTransport(Transport transport){
		
		this.transport = transport;
	}
	
	public void transportMessage(String message,String time,String host){
		
		if(transport != null)
		  this.transport.transport(message,time,host);
	}

	public abstract void handleMessage(byte[] messageContents,MessageReceiver context) throws Exception;

	public abstract void setParams(Map<String, String> params);

	protected SplunkLogEvent buildCommonEventMessagePart(MessageReceiver context)
			throws Exception {

		SplunkLogEvent event = new SplunkLogEvent("kafka_msg_received",
				 "", true, false);

		
		return event;

	}

	protected String getMessageBody(byte[] messageContents,String charset) throws Exception {

		return new String(messageContents,charset);

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
