package com.splunk.modinput.mq;

import java.util.Map;


import com.splunk.modinput.mq.MQModularInput.MQPoller;
import com.splunk.modinput.transport.Transport;

/**
 * Implement this class to provide your own custom event handler
 * 
 * @author ddallimore
 * 
 */
public abstract class AbstractEventHandler {

	private Transport transport;

    public void setTransport(Transport transport){
		
		this.transport = transport;
	}
	
	public void transportMessage(String message){
		
		if(transport != null)
		  this.transport.transport(message);
	}
	public abstract void handleMessage(Map<Object, Object> eventValues,
			MQPoller context) throws Exception;

	public abstract void setParams(Map<String, String> params);

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
