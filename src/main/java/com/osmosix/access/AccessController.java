package com.osmosix.access;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/")
public class AccessController {
	/**
	 *  APIs for knowing active/inactive/complated connection statistics
	 *
	 *
	 *
	 */

	private static final Logger _logger = Logger.getLogger(AccessController.class);

	@Autowired
	private AccessService accessService;

	// TODO
	//@Autowired
	private RepositoryService repositoryService;

	//Just a method to return weather the service is ready or not.
	@RequestMapping(value = "sendOk", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> heartbeat(HttpServletRequest request) {
		_logger.debug("Heartbeat is requested from " + request.getRemoteHost());
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		result.put("ALIVE", Boolean.TRUE);
		return result;
	}


	@RequestMapping(value = "load/settings", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> loadSettings(HttpServletRequest request){
		_logger.debug("Settings reload has been requested from "+ request.getRemoteAddr());
		Map<String, Object> result = new HashMap<String, Object>();
		try{
			return accessService.reloadSettings(request);
		} catch(Exception ex){
			result.put("Fail","Unable to reload connection settings - "+ex.getLocalizedMessage());
		}
		return result;
	}

	@RequestMapping(value = "load/servers", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> loadServers(HttpServletRequest request){
		_logger.debug("Server setting reload has been requested from "+ request.getRemoteAddr());
		Map<String, Object> result = new HashMap<String, Object>();
		try{
			return accessService.reloadServers();
		} catch(Exception ex){
			result.put("Fail","Failed to load server settings - "+ex.getLocalizedMessage());
		}
		return result;
	}

	@RequestMapping(value = "settings/{connectionType}", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getSettings(@PathVariable String connectionType, HttpServletRequest request) {
		return accessService.getSettings(connectionType, request);
	}

	@RequestMapping(value = "set/{property}/{value}", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> setServerProperties(@PathVariable String property,
	                                                                       @PathVariable String value,
	                                                                       HttpServletRequest request) {
		_logger.debug("Request to change connected mgmt server has come from "+ request.getRemoteAddr()+". Proceeding...");
		return accessService.modifyServerSettings(property, value, request);
	}

	@RequestMapping(value = "set/{connectionType}/{property}/{value}", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> setConnectionProperties(@PathVariable String connectionType,
																		@PathVariable String property,
	                                                                    @PathVariable String value,
	                                                                    HttpServletRequest request) {
		_logger.debug("Request to change connection settings has come from "+ request.getRemoteAddr()+". Proceeding...");
		return accessService.modifyConnectionSettings(connectionType, property, value, request);
	}

	@RequestMapping(value = "get/{property}", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getServerProperties(@PathVariable String property) {
		return Collections.singletonMap(property, accessService.getServerProperties().get(property));
	}

	@RequestMapping(value = "get/{connectionType}/{property}", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getConnectionSetting(@PathVariable String connectionType,
																			@PathVariable String property,
																			HttpServletRequest request) {
		return Collections.singletonMap(property, accessService.getSettings(connectionType, request).get(property));
	}
	//TODO - remove later
	@RequestMapping(value = "getallserversettings", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getAllServerProperties(HttpServletRequest request) {
		return accessService.getServerProperties();
	}

	@RequestMapping(value = "get/connections", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getAllConnections(){
		Map<String, Object> result = new HashMap<String, Object>();
		try{
			result.put("data", repositoryService.getConnections());
		}catch(Exception ex){
			result.put("error", "Can not fetch connections :"+ex.getMessage());
		}
		return result;
	}

	@RequestMapping(value = "get/connections/active", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getAllActiveConnections(){
		Map<String, Object> result = new HashMap<String, Object>();
		try{
			result.put("data", repositoryService.getActiveConnections());
		}catch(Exception ex){
			result.put("error", "Can not fetch connections :"+ex.getMessage());
		}
		return result;
	}

	@RequestMapping(value = "setupdb", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> setupDb(){
		Map<String, Object> result = new HashMap<String, Object>();
		try{
			repositoryService.buildSchemaAgain();
			result.put("schema", "Schema has been setup successfully!");
			repositoryService.createDefaultUsers();
			result.put("data","Dummy data has been added successfully!");
		}catch(Exception ex){
			result.put("error", "Unable to re-setup DB :"+ex.getMessage());
		}
		return result;
	}


}