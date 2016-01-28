package com.splunk.modinput.alexa;

import java.util.HashMap;
import java.util.Map;

import com.splunk.Service;

public class AlexaSessionManager {

	private static Service service;
	private static Map<String, IntentMapping> intentMappings;

	private static Map<String, String> timeMappings = new HashMap<String, String>();

	static {
		timeMappings.put("last business week", "earliest=-5d@w1 latest=@w6");
		timeMappings.put("this week", "earliest=@w0");
		timeMappings.put("today", "earliest=@d latest=now");
		timeMappings.put("yesterday", "earliest=-1d@d latest=@d");
		timeMappings.put("last week", "earliest=-7d@d latest=now");
	}

	public static Service getService() {
		return service;
	}

	public static void setService(Service service) {
		AlexaSessionManager.service = service;
	}

	public static Map<String, IntentMapping> getIntentMappings() {
		return intentMappings;
	}

	public static void setIntentMappings(Map<String, IntentMapping> intentMappings) {
		AlexaSessionManager.intentMappings = intentMappings;
	}

	public static Map<String, String> getTimeMappings() {
		return timeMappings;
	}

}
