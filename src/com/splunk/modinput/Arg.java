package com.splunk.modinput;

import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "title", "description", "validation", "data_type",
		"required_on_edit", "required_on_create" }, name = "arg")
public class Arg {

	private String name;
	private String title;
	private String description;
	private String validation;
	private DataType data_type = DataType.STRING;
	private boolean required_on_edit = false;
	private boolean required_on_create = true;

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

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

	public String getValidation() {
		return validation;
	}

	public void setValidation(String validation) {
		this.validation = validation;
	}

	public DataType getData_type() {
		return data_type;
	}

	public void setData_type(DataType data_type) {
		this.data_type = data_type;
	}

	public boolean isRequired_on_edit() {
		return required_on_edit;
	}

	public void setRequired_on_edit(boolean required_on_edit) {
		this.required_on_edit = required_on_edit;
	}

	public boolean isRequired_on_create() {
		return required_on_create;
	}

	public void setRequired_on_create(boolean required_on_create) {
		this.required_on_create = required_on_create;
	}

	public enum DataType {
		@XmlEnumValue(value = "string")
		STRING, @XmlEnumValue(value = "number")
		NUMBER, @XmlEnumValue(value = "boolean")
		BOOLEAN
	}

}
