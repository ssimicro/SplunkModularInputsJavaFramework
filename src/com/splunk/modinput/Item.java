package com.splunk.modinput;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "params", "param_list" }, name = "item")
public class Item {

	private String name;
	private List<Param> params = new ArrayList<Param>();
	private List<ParamList> param_list = new ArrayList<ParamList>();

	public void addParamList(ParamList list) {
		this.param_list.add(list);
	}

	@XmlElement(name = "param_list")
	public List<ParamList> getParam_list() {
		return param_list;
	}

	public void setParam_list(List<ParamList> param_list) {
		this.param_list = param_list;
	}

	@XmlElement(name = "param")
	public List<Param> getParams() {
		return params;
	}

	public void setParams(List<Param> params) {
		this.params = params;
	}

	public void addParam(Param param) {
		this.params.add(param);
	}

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
