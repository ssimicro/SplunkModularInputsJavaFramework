package com.splunk.modinput.alexa;

import java.util.Map;

import com.splunk.Service;

public class AlexaSessionManager {

	private static Service service;
	
	private static Map<String, IntentMapping> intentMappings;
	private static Map<String, TimeMapping> timeMappings;
	private static Map<String, DynamicActionMapping> dynamicActionMappings;

	
	public static Service getService() {
		return service;
	}

	public static void setService(Service service) {
		AlexaSessionManager.service = service;
	}

	public static Map<String, IntentMapping> getIntentMappings() {
		return AlexaSessionManager.intentMappings;
	}

	public static void setIntentMappings(Map<String, IntentMapping> intentMappings) {
		AlexaSessionManager.intentMappings = intentMappings;
	}

	public static Map<String, TimeMapping> getTimeMappings() {
		return AlexaSessionManager.timeMappings;
	}

	public static void setTimeMappings(Map<String, TimeMapping> timeMappings) {
		AlexaSessionManager.timeMappings = timeMappings;
	}
	
	public static Map<String, DynamicActionMapping> getDynamicActionMappings() {
		return AlexaSessionManager.dynamicActionMappings;
	}

	public static void setDynamicActionMappings(Map<String, DynamicActionMapping> dynamicActionMappings) {
		AlexaSessionManager.dynamicActionMappings = dynamicActionMappings;
	}
	

}
