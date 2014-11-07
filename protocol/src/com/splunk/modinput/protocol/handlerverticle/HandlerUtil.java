package com.splunk.modinput.protocol.handlerverticle;

import java.util.ArrayList;
import java.util.List;

import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;

/**
 * Utility class to take a String and wrap it up in a Stream object for
 * marshalling out to Splunk
 * 
 * @author ddallimore
 * 
 */
public abstract class HandlerUtil {

	public static Stream getStream(String output, String stanza)
			throws Exception {

		Stream stream = new Stream();
		ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();
		List<String> chunks = chunkData(output, 1024);

		for (int i = 0; i < chunks.size(); i++) {
			StreamEvent event = new StreamEvent();
			event.setUnbroken("1");
			event.setData(chunks.get(i));
			event.setStanza(stanza);
			// if we are seeing the last chunk, set the "done" element
			if (i == chunks.size() - 1)
				event.setDone(" ");
			list.add(event);
		}
		stream.setEvents(list);
		return stream;

	}

	public static List<String> chunkData(String text, int size) {

		List<String> ret = new ArrayList<String>((text.length() + size - 1)
				/ size);

		for (int start = 0; start < text.length(); start += size) {
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}

}
