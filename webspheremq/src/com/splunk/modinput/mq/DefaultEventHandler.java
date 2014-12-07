package com.splunk.modinput.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.google.gson.Gson;
import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;
import com.splunk.modinput.mq.MQModularInput.MQPoller; 

/**
 * Default handler that just outputs events in JSON.
 * 
 * @author ddallimore
 *
 */
public class DefaultEventHandler extends AbstractEventHandler {

	@Override
	public Stream handleMessage(Map <Object,Object> eventValues,MQPoller context)
			throws Exception {

				
		String text = jsonify(eventValues);
		
		Stream stream = new Stream();
		ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();
		List<String> chunks = chunkData(text, 1024);

		for (int i = 0; i < chunks.size(); i++) {
			StreamEvent event = new StreamEvent();
			event.setUnbroken("1");
			event.setData(chunks.get(i));
			event.setStanza(context.stanzaName);
			// if we are seeing the last chunk, set the "done" element
			if (i == chunks.size() - 1)
				event.setDone(" ");
			list.add(event);
		}
		stream.setEvents(list);
		return stream;

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
