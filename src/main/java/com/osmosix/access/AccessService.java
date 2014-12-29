package com.osmosix.access;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface AccessService {

	Map<String, String> reloadSettings(HttpServletRequest request) throws Exception;
	Map<String, String> reloadServers() throws Exception;
	Map<String,String> getSettings(String connectionType, HttpServletRequest request);
	Map<String,String> getServerProperties();
	Map<String,String> modifyServerSettings(String propertyName, String propertyValue, HttpServletRequest request);
	Map<String,String> modifyConnectionSettings(String connectionType, String propertyName, String propertyValue, HttpServletRequest request);
}
