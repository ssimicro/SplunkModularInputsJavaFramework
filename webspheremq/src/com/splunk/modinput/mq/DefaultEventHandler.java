package com.splunk.modinput.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import com.splunk.modinput.mq.MQModularInput.MQPoller;

/**
 * Default handler that just outputs events in JSON.
 * 
 * @author ddallimore
 * 
 */
public class DefaultEventHandler extends AbstractEventHandler {

	@Override
	public void handleMessage(Map<Object, Object> eventValues,
			MQPoller context) throws Exception {

		String text = jsonify(eventValues);

		transportMessage(text);

	}

	private String jsonify(Map<Object, Object> eventValues) {

		Gson gson = new Gson();
		return gson.toJson(eventValues);
	}

	public static List<String> chunkData(String text, int size) {

		List<String> ret = new ArrayList<String>((text.length() + size - 1)
				/ size);

		for (int start = 0; start < text.length(); start += size) {
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}

	@Override
	public void setParams(Map<String, String> params) {
		// Do nothing , params not used

	}

}
