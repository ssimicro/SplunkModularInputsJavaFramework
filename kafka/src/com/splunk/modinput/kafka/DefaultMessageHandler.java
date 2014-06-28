package com.splunk.modinput.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;
import com.splunk.modinput.kafka.KafkaModularInput.MessageReceiver;

public class DefaultMessageHandler extends AbstractMessageHandler {

	@Override
	public Stream handleMessage(byte[] messageContents,MessageReceiver context)
			throws Exception {

		SplunkLogEvent splunkEvent = buildCommonEventMessagePart(context);

		String body = getMessageBody(messageContents);
		splunkEvent.addPair("msg_body", stripNewlines(body));

		String text = splunkEvent.toString();
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
