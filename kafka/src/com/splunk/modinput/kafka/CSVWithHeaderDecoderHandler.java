package com.splunk.modinput.kafka;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.Charset;

import java.util.Map;
import java.util.StringTokenizer;

import org.json.JSONObject;

import com.splunk.modinput.kafka.KafkaModularInput.MessageReceiver;

public class CSVWithHeaderDecoderHandler extends AbstractMessageHandler {

	String[] headers = {};
	String charset = Charset.defaultCharset().name();
	String outputFormat = KV;
	boolean hasHeaderRow = false;

	private final static String KV = "kv";
	private final static String JSON = "json";

	@Override
	public void handleMessage(byte[] messageContents, MessageReceiver context) throws Exception {

		String text = getMessageBody(messageContents, charset);

		StringBuffer kvOutput = new StringBuffer();
		JSONObject jsonOutput = new JSONObject();
		int currentLine = 0;
		BufferedReader bufReader = new BufferedReader(new StringReader(text));
		String line = null;
		while ((line = bufReader.readLine()) != null) {

			//skip any header rows
			if (hasHeaderRow && currentLine == 0) {
				currentLine++;
				continue;
			}
			StringTokenizer st = new StringTokenizer(line, ",");
			int index = 0;
			while (st.hasMoreTokens()) {

				if (outputFormat == JSON) {
					jsonOutput.put(headers[index], st.nextToken());
				}
				if (outputFormat == KV) {
					kvOutput.append(headers[index]).append("=").append("\"").append(st.nextToken()).append("\"")
							.append(" ");
				}
				index++;
			}
			currentLine++;

		}
		String output = "";
		if (outputFormat == JSON) {
			output = jsonOutput.toString();
		}
		if (outputFormat == KV) {
			output = kvOutput.toString();
		}

		transportMessage(output, "", "");

	}

	@Override
	public void setParams(Map<String, String> params) {

		if (params.containsKey("headers")) {

			StringTokenizer st = new StringTokenizer(params.get("headers"), ":");
			headers = new String[st.countTokens()];
			int i = 0;
			while (st.hasMoreTokens()) {
				headers[i] = st.nextToken();
				i++;
			}

		} else if (params.containsKey("charset"))
			this.charset = params.get("charset");
		else if (params.containsKey("outputFormat"))
			this.outputFormat = params.get("outputFormat");
		else if (params.containsKey("hasHeaderRow"))
			try {
				this.hasHeaderRow = Boolean.parseBoolean(params.get("hasHeaderRow"));
			} catch (Exception e) {
				
			}

	}

}
