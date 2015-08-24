package com.splunk.modinput.transport;

public class HECTransportConfig {
	
	private String token;
	private String host = "localhost";
	private int port = 8088;
	private boolean https = false;
	private int poolsize = 1;
	
	private String index;
	private String source = "modular_input_hec";
	private String sourcetype;
	
	public HECTransportConfig(){}

	
	public HECTransportConfig(String token, String host, int port,boolean https,int poolsize,String index, String source,
			String sourcetype) {
		super();
		this.token = token;
		this.host = host;
		this.port = port;
		this.https=https;
		this.poolsize=poolsize;
		this.index = index;
		this.source = source;
		this.sourcetype = sourcetype;
	}


	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}


	public boolean isHttps() {
		return https;
	}


	public void setHttps(boolean https) {
		this.https = https;
	}


	public int getPoolsize() {
		return poolsize;
	}


	public void setPoolsize(int poolsize) {
		this.poolsize = poolsize;
	}
	
	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
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

	
	
	
	
	
	

}
