package com.splunk.modinput.alexa;

/**
 * Class for mapping.json
 * @author ddallimore
 *
 */
public class IntentMapping {

	private String intent;
	private String search;
	private String response;
	private String timeSlot = "timeperiod"; //default
	private String savedSearchName;
	private String savedSearchArgs;
	private String dynamicAction;
	private String dynamicActionArgs;
	private String earliest ="";
	private String latest = "";
	private String appNamespace = "";
	

	
	public IntentMapping() {
	}

	
	


	public String getAppNamespace() {
		return appNamespace;
	}




	public void setAppNamespace(String appNamespace) {
		this.appNamespace = appNamespace;
	}




	public String getEarliest() {
		return earliest;
	}


	public void setEarliest(String earliest) {
		this.earliest = earliest;
	}


	public String getLatest() {
		return latest;
	}


	public void setLatest(String latest) {
		this.latest = latest;
	}


	public String getIntent() {
		return intent;
	}

	public void setIntent(String intent) {
		this.intent = intent;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getTimeSlot() {
		return timeSlot;
	}

	public void setTimeSlot(String timeSlot) {
		this.timeSlot = timeSlot;
	}

	public String getSavedSearchName() {
		return savedSearchName;
	}

	public void setSavedSearchName(String savedSearchName) {
		this.savedSearchName = savedSearchName;
	}

	public String getSavedSearchArgs() {
		return savedSearchArgs;
	}

	public void setSavedSearchArgs(String savedSearchArgs) {
		this.savedSearchArgs = savedSearchArgs;
	}

	public String getDynamicAction() {
		return dynamicAction;
	}

	public void setDynamicAction(String dynamicAction) {
		this.dynamicAction = dynamicAction;
	}

	public String getDynamicActionArgs() {
		return dynamicActionArgs;
	}

	public void setDynamicActionArgs(String dynamicActionArgs) {
		this.dynamicActionArgs = dynamicActionArgs;
	}
	
	


	
}
