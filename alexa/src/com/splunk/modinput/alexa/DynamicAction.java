package com.splunk.modinput.alexa;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.amazon.speech.slu.Slot;

/**
 * Implement this abstract class to create a custom Dynamic Action. Then wire it
 * up in dynamic_actions/dynamicactions.json
 * 
 * @author ddallimore
 *
 */
public abstract class DynamicAction {

	private Map<String, String> args = new HashMap<String, String>();
	private Map<String, Slot> slots = new HashMap<String, Slot>();
	private String responseTemplate = "";

	/**
	 * Implementation classes implement this method
	 * 
	 * @return A String representing the text output to be sent to Alexa
	 */
	public abstract String executeAction();

	public void setSlots(Map<String, Slot> slots) {

		this.slots = slots;
	}

	public void setArgs(Map<String, String> args) {

		this.args = args;
	}

	public void setResponseTemplate(String template) {

		this.responseTemplate = template;
	}

	protected String replaceResponse(String dynamicResponse) {
		
		String response = getResponseTemplate().replace("\\$dynamic_response\\$", dynamicResponse);

		Set<String> slotKeys = slots.keySet();
		// search replace slots into search and response strings
		for (String key : slotKeys) {

			String value = slots.get(key).getValue();

			response = response.replaceAll("\\$" + key + "\\$", value);

		}
		return response;
	}

	protected String getSlot(String key) {

		return this.slots.get(key).getValue();

	}

	protected String getArg(String key) {

		return this.args.get(key);

	}

	protected String getResponseTemplate() {

		return this.responseTemplate;
	}

}
