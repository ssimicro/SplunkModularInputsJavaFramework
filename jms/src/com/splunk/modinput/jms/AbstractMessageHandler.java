package com.splunk.modinput.jms;


import java.util.Enumeration;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.jms.JMSModularInput.MessageReceiver;


public abstract class AbstractMessageHandler {
	
	public abstract Stream handleMessage(Message message,MessageReceiver context) throws Exception;
	public abstract void setParams(Map<String, String> params);

	protected SplunkLogEvent buildCommonEventMessagePart(Message message,MessageReceiver context) throws Exception{
	
		SplunkLogEvent event = new SplunkLogEvent(context.type + "_msg_received",
				message.getJMSMessageID(), true, true);

		event.addPair("msg_dest", context.destination);

		if (context.indexHeader) {
			// JMS Message Header fields
			event.addPair("msg_header_timestamp", message.getJMSTimestamp());
			event.addPair("msg_header_correlation_id",
					message.getJMSCorrelationID());
			event.addPair("msg_header_delivery_mode",
					message.getJMSDeliveryMode());
			event.addPair("msg_header_expiration",
					message.getJMSExpiration());
			event.addPair("msg_header_priority", message.getJMSPriority());
			event.addPair("msg_header_redelivered",
					message.getJMSRedelivered());
			event.addPair("msg_header_type", message.getJMSType());
		}

		if (context.indexProperties) {
			// JMS Message Properties
			Enumeration propertyNames = message.getPropertyNames();
			while (propertyNames.hasMoreElements()) {
				String name = (String) propertyNames.nextElement();
				Object property = message.getObjectProperty(name);
				event.addPair("msg_property_" + name, property);
			}
		}
		
		return event;
		
		
	}
	
	
	protected String getMessageBody(Message message){
		
		String body="";
		try {
			if (message instanceof TextMessage) {
				body = ((TextMessage) message).getText();
			} else if (message instanceof BytesMessage) {
				try {

					int bufSize = 1024;
					byte[] buffer = null;
					int readBytes = 0;
					byte[] messageBodyBytes = null;

					while (true) {

						buffer = new byte[bufSize];
						readBytes = ((BytesMessage) message).readBytes(
								buffer, bufSize);
						if (readBytes == -1)
							break;
						if (messageBodyBytes == null) {
							messageBodyBytes = new byte[readBytes];
							System.arraycopy(buffer, 0, messageBodyBytes,
									0, readBytes);
						} else {
							byte[] extended = new byte[messageBodyBytes.length
									+ readBytes];
							System.arraycopy(messageBodyBytes, 0, extended,
									0, messageBodyBytes.length);
							System.arraycopy(buffer, 0, extended,
									messageBodyBytes.length, readBytes);
							messageBodyBytes = extended;
						}

					}

					body = new String(messageBodyBytes);
				} catch (Exception e) {
					body = "binary message body can't be read";
				}

			} else if (message instanceof StreamMessage) {
				body = "binary stream message";
			} else if (message instanceof ObjectMessage) {
				body = ((ObjectMessage) message).getObject().toString();
			} else if (message instanceof MapMessage) {
				Enumeration names = ((MapMessage) message).getMapNames();
				while (names.hasMoreElements()) {
					String name = (String) names.nextElement();
					Object value = ((MapMessage) message).getObject(name);
					body += name + "=" + value;
					if (names.hasMoreElements())
						body += ",";
				}
				body = ((MapMessage) message).toString();
			} else {
				body = message.toString();
			}
		} catch (Exception e) {

		}
		return body;

	}
	
	protected String stripNewlines(String input) {

		if (input == null) {
			return "";
		}
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (Character.isWhitespace(chars[i])) {
				chars[i] = ' ';
			}
		}

		return new String(chars);
	}
	
}
