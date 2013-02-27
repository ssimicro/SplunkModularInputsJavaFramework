package com.splunk.modinput.jms;

import java.util.ArrayList;
import java.util.Map;


import javax.jms.Message;


import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;
import com.splunk.modinput.jms.JMSModularInput.MessageReceiver;

public class DefaultMessageHandler extends AbstractMessageHandler {

	
	@Override
	public Stream handleMessage(Message message,MessageReceiver context) throws Exception{
		
		SplunkLogEvent splunkEvent = buildCommonEventMessagePart(message,context);
		
		String body = getMessageBody(message);
		splunkEvent.addPair("msg_body", context.stripNewlines ? stripNewlines(body): body);
		
		
		String text = splunkEvent.toString();
		Stream stream = new Stream();
		StreamEvent event = new StreamEvent();
		event.setData(text);
		event.setStanza(context.stanzaName);
		ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();
		list.add(event);
		stream.setEvents(list);
		return stream;
		
	}
	

	@Override
	public void setParams(Map<String, String> params) {
		//Do nothing , params not used
		
	}
	
	

}
