package com.splunk.modinput.kafka;

import java.nio.charset.Charset;

import java.util.Map;

import org.json.JSONObject;

import com.splunk.modinput.kafka.KafkaModularInput.MessageReceiver;

public class JSONBodyWithTimeExtraction extends AbstractMessageHandler {

	String charset = Charset.defaultCharset().name();
	String timefield = "";

	@Override
	public void handleMessage(byte[] messageContents, MessageReceiver context)
			throws Exception {

		String text = getMessageBody(messageContents, charset);

		JSONObject json = new JSONObject(text);

		if (timefield.length() > 0) {
			String time = json.getString(timefield);
			transportMessage(text, time);
		} else {
			transportMessage(text, "");
		}

	}

	@Override
	public void setParams(Map<String, String> params) {

		if (params.containsKey("charset"))
			this.charset = params.get("charset");
		if (params.containsKey("timefield"))
			this.timefield = params.get("timefield");

	}

}
