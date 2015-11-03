package com.splunk.modinput.kafka;

import java.nio.charset.Charset;

import java.util.Map;


import com.splunk.modinput.SplunkLogEvent;

import com.splunk.modinput.kafka.KafkaModularInput.MessageReceiver;

public class DefaultMessageHandler extends AbstractMessageHandler {

	String charset = Charset.defaultCharset().name();
	
	@Override
	public void handleMessage(byte[] messageContents,MessageReceiver context)
			throws Exception {

		SplunkLogEvent splunkEvent = buildCommonEventMessagePart(context);

		String body = getMessageBody(messageContents,charset);
		splunkEvent.addPair("msg_body", stripNewlines(body));

		String text = splunkEvent.toString();
		
		transportMessage(text,String.valueOf(System.currentTimeMillis()),"");
		
	}

	

	@Override
	public void setParams(Map<String, String> params) {
		
		if(params.containsKey("charset"))
		  this.charset = params.get("charset");
		

	}

}
