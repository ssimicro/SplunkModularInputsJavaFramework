package com.splunk.modinput.alexa;

import java.util.Map;

import com.splunk.Service;

/**
 * Just a place to hold static shared objects
 * 
 * @author ddallimore
 *
 */
public class AlexaSessionManager {

	//user splunk-system-user
	private static Service defaultService;
	//a user defined auth to override the default
	private static  Service currentService;
	
	private static Map<String, IntentMapping> intentMappings;
	private static Map<String, TimeMapping> timeMappings;
	private static Map<String, DynamicActionMapping> dynamicActionMappings;

	public static Service getService() {
		return AlexaSessionManager.currentService;
	}

	public static void setCurrentService(Service service) {
		AlexaSessionManager.currentService = service;
	}
	
	public static Service getDefaultService() {
		return AlexaSessionManager.defaultService;
	}

	public static void setDefaultService(Service service) {
		AlexaSessionManager.defaultService = service;
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
