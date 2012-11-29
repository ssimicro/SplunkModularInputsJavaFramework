package com.splunk.modinput;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "values" }, name = "param_list")
public class ParamList {

	private String name;
	private List<Value> values = new ArrayList<Value>();

	@XmlElement(name = "value")
	public List<Value> getValues() {
		return values;
	}

	public void setValues(List<Value> values) {
		this.values = values;
	}

	public void addValue(Value value) {
		this.values.add(value);
	}

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
