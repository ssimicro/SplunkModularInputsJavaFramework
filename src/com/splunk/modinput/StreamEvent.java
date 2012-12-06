package com.splunk.modinput;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "data", "source", "sourcetype", "index", "host", "done" }, name = "event")
public class StreamEvent {

	private String stanza;
	private String data;
	private String source;
	private String sourcetype;
	private String index;
	private String host;

	private String unbroken;
	private String done;

	@XmlAttribute(name = "stanza")
	public String getStanza() {
		return stanza;
	}

	public void setStanza(String stanza) {
		this.stanza = stanza;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourcetype() {
		return sourcetype;
	}

	public void setSourcetype(String sourcetype) {
		this.sourcetype = sourcetype;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@XmlAttribute(name = "unbroken")
	public String getUnbroken() {
		return unbroken;
	}

	public void setUnbroken(String unbroken) {
		this.unbroken = unbroken;
	}

	public String getDone() {
		return done;
	}

	public void setDone(String done) {
		this.done = done;
	}

}
