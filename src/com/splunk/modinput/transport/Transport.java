package com.splunk.modinput.transport;

public interface Transport {
	
	public void init(Object obj);
	
	public void setStanzaName(String name);
	
	public void transport(String message);
	
	public void transport(String message,String time,String host);

}
