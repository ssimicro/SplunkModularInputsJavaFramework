package com.splunk.modinput.jms.custom.handler;

import java.util.Map;

import javax.jms.Message;

import com.splunk.modinput.jms.AbstractMessageHandler;
import com.splunk.modinput.jms.JMSModularInput.MessageReceiver;

public class BodyOnlyMessageHandler extends AbstractMessageHandler {

	@Override
	public void handleMessage(Message message, MessageReceiver context) throws Exception {

		String bodyContent = getMessageBody(message);

		transportMessage(bodyContent, String.valueOf(System.currentTimeMillis()), "");
	}

	@Override
	public void setParams(Map<String, String> params) {
		// Do nothing , params not used

	}

}
