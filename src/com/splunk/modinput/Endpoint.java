package com.splunk.modinput;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "args" }, name = "endpoint")
public class Endpoint {

	private List<Arg> args = new ArrayList<Arg>();

	@XmlElementWrapper(name = "args")
	@XmlElement(name = "arg")
	public List<Arg> getArgs() {
		return args;
	}

	public void setArgs(List<Arg> args) {
		this.args = args;
	}

	public void addArg(Arg arg) {
		this.args.add(arg);
	}

}
