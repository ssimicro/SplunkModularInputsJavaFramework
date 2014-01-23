package com.splunk.modinput.jms.custom.handler;

import java.util.ArrayList;
import java.util.Map;

import javax.jms.Message;

import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;
import com.splunk.modinput.jms.AbstractMessageHandler;
import com.splunk.modinput.jms.JMSModularInput.MessageReceiver;

public class SplitBodyMessageHandler extends AbstractMessageHandler {

	@Override
	public Stream handleMessage(Message message, MessageReceiver context)
			throws Exception {

		ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();

		Stream stream = new Stream();

		SplunkLogEvent headers = buildCommonEventMessagePart(message, context);

		StreamEvent event = new StreamEvent();
		event.setData(headers.toString());
		event.setStanza(context.stanzaName);
		list.add(event);

		String bodyContent = getMessageBody(message);

		StreamEvent event2 = new StreamEvent();
		event2.setData(bodyContent);
		event2.setStanza(context.stanzaName);
		list.add(event2);

		stream.setEvents(list);
		return stream;
	}

	@Override
	public void setParams(Map<String, String> params) {
		// Do nothing , params not used

	}

}
