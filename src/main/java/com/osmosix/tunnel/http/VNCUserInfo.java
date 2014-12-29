package com.osmosix.tunnel.http;

import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.UIKeyboardInteractive;

public class VNCUserInfo implements UserInfo, UIKeyboardInteractive {


	public VNCUserInfo( ) {
	}

	@Override
	public boolean promptYesNo(String str) {
		return true;
	}

	String passwd = null;
	String passphrase = null;

	@Override
	public String getPassword() {
		return passwd;
	}

	@Override
	public String getPassphrase() {
		return passphrase;
	}

	@Override
	public boolean promptPassword(String message) {
		return true;
	}

	@Override
	public boolean promptPassphrase(String message) {
		return true;
	}

	@Override
	public void showMessage(String message) {
	}


	@Override
	public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
	                                          boolean[] echo) {
		return null;
	}
}

