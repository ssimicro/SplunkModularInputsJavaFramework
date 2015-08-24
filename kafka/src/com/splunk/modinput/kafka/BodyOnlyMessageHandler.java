package com.splunk.modinput.kafka;

import java.nio.charset.Charset;

import java.util.Map;


import com.splunk.modinput.kafka.KafkaModularInput.MessageReceiver;

public class BodyOnlyMessageHandler extends AbstractMessageHandler {

	String charset = Charset.defaultCharset().name();
	
	@Override
	public void handleMessage(byte[] messageContents,MessageReceiver context)
			throws Exception {

		String text = getMessageBody(messageContents,charset);
		transportMessage(text);

	}


	@Override
	public void setParams(Map<String, String> params) {
		
		if(params.containsKey("charset"))
		  this.charset = params.get("charset");
		

	}

}
