package com.splunk.modinput;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "events" }, name = "stream")
@XmlRootElement
public class Stream {

	private List<StreamEvent> events = new ArrayList<StreamEvent>();

	@XmlElement(name = "event")
	public List<StreamEvent> getEvents() {
		return events;
	}

	public void setEvents(List<StreamEvent> events) {
		this.events = events;
	}

}
