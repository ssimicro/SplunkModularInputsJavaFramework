package com.splunk.modinput.alexa;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.amazon.speech.slu.Slot;

public abstract class DynamicAction {

	private Map<String, String> args = new HashMap<String, String>();
	private Map<String, Slot> slots = new HashMap<String, Slot>();
	private String responseTemplate = "";

	/**
	 * Implementation classes implement this method
	 * @return A String representing the text output to be sent to Alexa
	 */
	public abstract String executeAction();

	public void setSlots(Map<String, Slot> slots) {

		this.slots = slots;
	}

	public void setArgs(String keyValString) {

		this.args = getParamMap(keyValString);
	}
	
	public void setResponseTemplate(String template){
		
		this.responseTemplate = template;
	}

	private Map<String, String> getParamMap(String keyValString) {

		Map<String, String> map = new HashMap<String, String>();

		try {
			StringTokenizer st = new StringTokenizer(keyValString, ",");
			while (st.hasMoreTokens()) {
				StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
				while (st2.hasMoreTokens()) {
					map.put(st2.nextToken(), st2.nextToken());
				}
			}
		} catch (Exception e) {

		}

		return map;

	}

	protected String getSlot(String key) {

		return this.slots.get(key).getValue();

	}

	protected String getArg(String key) {

		return this.args.get(key);

	}
	
    protected String getResponseTemplate(){
		
		return this.responseTemplate;
	}

}
