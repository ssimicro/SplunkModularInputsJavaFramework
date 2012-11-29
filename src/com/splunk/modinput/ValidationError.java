package com.splunk.modinput;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "message" }, name = "error")
@XmlRootElement(name = "error")
public class ValidationError {

	private String message;

	public ValidationError(String message) {

		this.message = message;
	}

	public ValidationError() {

	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
