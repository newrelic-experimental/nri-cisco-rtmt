package com.newrelic.event.cisco.rtmt;

public class Authentication {

	private String username = null;
	private String password = null;
	
	public Authentication(String u, String p) {
		username = u;
		password = p;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	
	
}
