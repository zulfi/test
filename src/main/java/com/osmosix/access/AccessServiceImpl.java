package com.osmosix.access;

import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.net.ssl.*;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.*;

public class AccessServiceImpl implements AccessService {
	private static final Logger _logger = Logger.getLogger(AccessServiceImpl.class);
	private String serverConfigFile = "WEB-INF/mgmt.properties";
	private String gatewayConfigFile = "/usr/local/cliqr/etc/gateway_config.properties";
	private String sshPropsFile = "WEB-INF/ssh.properties";
	private String vncPropFile = "WEB-INF/vnc.properties";
	private String rdpPropFile = "WEB-INF/rdp.properties";


	private Properties serverProps;
	@Autowired
	private Properties sshProps;
	@Autowired
	private Properties vncProps;
	@Autowired
	private Properties rdpProps;

	@Override
	public Map<String, String> reloadSettings(HttpServletRequest request) throws Exception {
		Map<String,String> result = new HashMap<String,String>();
		result.putAll(reloadSettings("ssh", request));
		result.putAll(reloadSettings("vnc", request));
		result.putAll(reloadSettings("rdp", request));
		return result;
	}

	public void init(){
		try{
			reloadServers();
		}catch(Exception ex){
			_logger.error("Unable to load server settings from gateway_config.properties");
		}
	}

	private Map<String, String> reloadSettings(String connectionType, HttpServletRequest request) throws Exception {
		Map<String,String> result = new HashMap<String,String>();

		InputStream is = null;
		Properties props = 	getContainingProperties(connectionType);

		try{
			String appHome = getApplicationHomeDir(request);
			_logger.debug("RELOADING SETTINGS FOR -"+connectionType+" -"+appHome+"WEB-INF/"+connectionType+".properties");
			is = new FileInputStream(appHome+"/WEB-INF/"+connectionType+".properties");
			props.load(is);
			result.put(connectionType,"Reloaded!");
		}catch (IOException ioex){
			_logger.error("Unable to reload settings for "+connectionType);
			result.put(connectionType,"failed!"+ioex.getMessage());
		}finally{
			if(is != null){
				try{is.close();}catch (IOException iex){}
			}
		}
		return result;
	}

	@Override
	public Map<String, String> reloadServers() throws Exception {
		Map<String,String> result = new HashMap<String,String>();
		serverProps = new Properties();//clear all previous settings
		InputStream serverPropsIs = null;
		try{
			_logger.debug("RELOADING SETTINGS FOR SERVER -"+gatewayConfigFile);
			serverPropsIs = new FileInputStream(gatewayConfigFile);
			serverProps.load(serverPropsIs);
			if(StringUtils.isEmpty(serverProps.getProperty("guacd-hostname") )){
				serverProps.put("guacd-hostname","localhost");
			}
			if(StringUtils.isEmpty(serverProps.getProperty("guacd-port") )){
				serverProps.put("guacd-port","4822");
			}
			result.put("success", "Server Settings reloaded successfully!");
		}catch (Exception ex){
			ex.printStackTrace();
			result.put("fail", "Failed to reload server settings! -"+ex.getMessage());
			throw ex;
		}finally {
			if (serverPropsIs != null) {
				try { serverPropsIs.close();} catch (Exception e) {}
			}
		}
		return result;
	}

	private static String getApplicationHomeDir(HttpServletRequest request) {
		return request.getSession().getServletContext().getRealPath(File.separator);
	}

	@Override
	public Map<String,String> getSettings(String connectionType, HttpServletRequest request){
		Map<String,String> result = new HashMap<String,String>();
		Properties props = new Properties();
		InputStream is = null;
		try{
			String appHome = getApplicationHomeDir(request);
			_logger.debug("GETTING SETTINGS FOR -"+connectionType+" -"+appHome+"WEB-INF/"+connectionType+".properties");
			is = new FileInputStream(appHome+"WEB-INF/"+connectionType+".properties");
			props.load(is);
			if(props !=  null){
				Enumeration e = props.propertyNames();
				while(e.hasMoreElements()){
					String name = (String) e.nextElement();
					String value = props.getProperty(name);
					result.put(name, value);
				}
			}
		}catch(IOException ioex){
			_logger.error("Unable to retrieve "+connectionType+" settings -"+ioex.getMessage());
			ioex.printStackTrace();
			result.put("fail","Unable to retrieve "+connectionType+" settings.");
		}finally {
			if(is != null){
				try{is.close();}catch(IOException ioe){}
			}
		}
		return result;
	}

	@Override
	public Map<String,String> getServerProperties(){
		Map<String,String> result = new HashMap<String,String>();
		if(serverProps !=  null){
			Enumeration e = serverProps.propertyNames();
			while(e.hasMoreElements()){
				String name = (String) e.nextElement();
				String value = serverProps.getProperty(name);
				result.put(name, value);
			}
		}
		return result;
	}

	@Override
	public Map<String,String> modifyServerSettings(String propertyName, String propertyValue, HttpServletRequest request){
		Map<String,String> result = new HashMap<String,String>();
		if(!StringUtils.isEmpty(propertyName) && !StringUtils.isEmpty(propertyValue)){
			OutputStream ofs = null;
			try{
				_logger.debug("<--------CHANGING SERVER PROPERTY----------------> ");
				_logger.debug("OLD value "+propertyName+"="+ serverProps.get(propertyName));
				_logger.debug("NEW value " + propertyName + "=" + propertyValue);
				serverProps.setProperty(propertyName, propertyValue);
				_logger.debug("MODIFYING SETTINGS FOR SERVER -"+gatewayConfigFile);
				ofs = new FileOutputStream(gatewayConfigFile);
				serverProps.store(ofs, null);
				result.put("result", "Success");
				_logger.debug("<--------CHANGED " + propertyName + "----------------> ");
			}catch(Exception ioex){
				_logger.error("Unable to persist server properties to file -"+ioex.getMessage());
				ioex.printStackTrace();
				result.put("fail","Unable to save the server settings.");
			}finally {
				if(ofs != null){
					try{ofs.close();}catch(IOException ioe){}
				}
			}

		}else{
			result.put("fail","Property name/value is empty.");
		}
		return result;
	}



	@Override
	public Map<String,String> modifyConnectionSettings(String connectionType, String propertyName, String propertyValue, HttpServletRequest request){
		Map<String,String> result = new HashMap<String,String>();
		if(!StringUtils.isEmpty(propertyName) && !StringUtils.isEmpty(propertyValue)){
			OutputStream ofs = null;
			String appHome = getApplicationHomeDir(request);
			String filename = appHome+"WEB-INF/"+connectionType+".properties";
			_logger.debug("MODIFYING SETTINGS FOR -"+connectionType+" in "+filename);
			Properties props = getContainingProperties(connectionType);
			try{
				props.put(propertyName,propertyValue);
				ofs = new FileOutputStream(filename);
				props.store(ofs, null);
				result.put("result", "Success");
			}catch(Exception ioex){
				_logger.error("Unable to persist "+connectionType+" connection settings to file -"+ioex.getMessage());
				ioex.printStackTrace();
				result.put("fail","Unable to save the "+connectionType+" settings.");
			}finally {
				if(ofs != null){
					try{ofs.close();}catch(IOException ioe){}
				}
			}

		}else{
			result.put("fail","Property name/value is empty.");
		}
		return result;
	}

	private Properties getContainingProperties(String connectionType){
		if("ssh".equals(connectionType)){
			return sshProps;
		}else if("vnc".equals(connectionType)){
			return vncProps;
		}else if ("rdp".equals(connectionType)){
			return rdpProps;
		}
		return null;
	}

}
