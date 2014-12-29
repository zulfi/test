package com.osmosix.access;

/**
 * Created by irraju on 04/09/14.
 */
public class LoginModel {

	/* This is the attribute name (path) of username field in login.jsp page */
	private String userName;

	/* This is the attribute name (path) of Password field in login.jsp page */
	private String password;

	/* Getters and setters for the private fields. */
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}

	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
}
