package com.splunk.modinput.alexa;

/**
 * Class for timemappings.json
 * @author ddallimore
 *
 */
public class TimeMapping {
	
	String utterance;
	String earliest;
	String latest;
	
	public TimeMapping(){}

	public String getUtterance() {
		return utterance;
	}

	public void setUtterance(String utterance) {
		this.utterance = utterance;
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
	
	

}
