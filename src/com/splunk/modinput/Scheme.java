package com.splunk.modinput;

import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "title", "description", "use_external_validation",
		"streaming_mode", "use_single_instance", "endpoint" }, name = "scheme")
@XmlRootElement
public class Scheme {

	private String title;
	private String description;
	private boolean use_external_validation = false;
	private StreamingMode streaming_mode = StreamingMode.SIMPLE;
	private boolean use_single_instance = false;
	private Endpoint endpoint;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isUse_external_validation() {
		return use_external_validation;
	}

	public void setUse_external_validation(boolean use_external_validation) {
		this.use_external_validation = use_external_validation;
	}

	public StreamingMode getStreaming_mode() {
		return streaming_mode;
	}

	public void setStreaming_mode(StreamingMode streaming_mode) {
		this.streaming_mode = streaming_mode;
	}

	public boolean isUse_single_instance() {
		return use_single_instance;
	}

	public void setUse_single_instance(boolean use_single_instance) {
		this.use_single_instance = use_single_instance;
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	public enum StreamingMode {
		@XmlEnumValue(value = "xml")
		XML, @XmlEnumValue(value = "simple")
		SIMPLE,

	}

}
