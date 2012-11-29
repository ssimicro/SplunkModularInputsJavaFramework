package com.splunk.modinput;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType(name = "value")
public class Value {

	private String value;

	@XmlValue
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
