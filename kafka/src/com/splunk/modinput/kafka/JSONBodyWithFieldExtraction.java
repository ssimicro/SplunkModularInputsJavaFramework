package com.splunk.modinput.kafka;

import java.nio.charset.Charset;

import java.util.Map;

import org.json.JSONObject;

import com.splunk.modinput.kafka.KafkaModularInput.MessageReceiver;

public class JSONBodyWithFieldExtraction extends AbstractMessageHandler {

	String charset = Charset.defaultCharset().name();
	String timefield = "";
	String hostfield = "";

	@Override
	public void handleMessage(byte[] messageContents, MessageReceiver context)
			throws Exception {

		String text = getMessageBody(messageContents, charset);

		JSONObject json = new JSONObject(text);

		String time = "";
		String host = "";

		if (timefield.length() > 0) {
			time = json.getString(timefield);

		} else {
			time = String.valueOf(System.currentTimeMillis());
		}
		if (hostfield.length() > 0) {
			host = json.getString(hostfield);

		}
		transportMessage(text, time, host);

	}

	@Override
	public void setParams(Map<String, String> params) {

		if (params.containsKey("charset"))
			this.charset = params.get("charset");
		if (params.containsKey("timefield"))
			this.timefield = params.get("timefield");
		if (params.containsKey("hostfield"))
			this.hostfield = params.get("hostfield");

	}

}
