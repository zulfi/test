package com.osmosix.tunnel.http;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.servlet.GuacamoleHTTPTunnelServlet;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import javax.net.ssl.*;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public abstract class HttpTunnel extends GuacamoleHTTPTunnelServlet {
	private Logger _logger = Logger.getLogger(HttpTunnel.class);


	protected void initSslForTrustStore()
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
		InputStream trustStoreFile = SshTunnel.class.getResourceAsStream("/.truststore");
		String trustStorePassword = "osmosix";
		// create keystore object, load it with truststorefile data
		KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(trustStoreFile, trustStorePassword == null ? null : trustStorePassword.toCharArray());

		// create trustmanager factory and load the keystore object in it
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory
				.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);
		trustStoreFile.close();
		TrustManager[] trustMgrs =  trustManagerFactory.getTrustManagers();
		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, trustMgrs, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier(){
			@Override
			public boolean verify(String string, SSLSession ssls) {
				return true;
			}
		});
	}

	protected Map<String, Object> getSshConnectionInfo(String mgmtServer, String mgmtSessionId) throws IOException, ParseException {
		URL url = new URL("https://" + mgmtServer + "/ssh/service/getinfo/" + mgmtSessionId);
		return makeRestCall(url);
	}

	protected  Map<String, Object> getServerDetails(String contextPath) throws IOException, ParseException {
		URL url = new URL("https://localhost"+contextPath+"/api/getallserversettings");
		return makeRestCall(url);
	}

	protected  Map<String,Object> getConnectionSettings(String connectionType, String contextPath) throws IOException, ParseException {
		URL url = new URL("https://localhost"+contextPath+"/api/settings/"+connectionType);
		return makeRestCall(url);
	}

	private Map<String,Object> makeRestCall(URL url) throws IOException, ParseException {
		StringBuilder jsonText = new StringBuilder();
		_logger.info("CALLING "+url.toString());

		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			jsonText.append(inputLine);
			jsonText.append("\n");
		}
		in.close();
		JSONParser parser = new JSONParser();
		ContainerFactory containerFactory = new ContainerFactory(){
			@Override
			public List creatArrayContainer() {
				return new LinkedList();
			}

			@Override
			public Map createObjectContainer() {
				return new LinkedHashMap();
			}
		};
		Map<String, Object> result = (Map<String, Object>)parser.parse(jsonText.toString(), containerFactory);
		return result;
	}

	protected GuacamoleConfiguration setConfigSettings(GuacamoleConfiguration config, Map<String, Object> settings){
		for (Map.Entry<String, Object> entry : settings.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(!StringUtils.isEmpty(key) && !StringUtils.isEmpty(value)){
//				_logger.info(" GUA CONFIG - "+key+":"+value);
				config.setParameter(key,value.toString());
			}else{
				_logger.info(" GUA CONFIG - "+key+" has empty value");
			}
		}
		return config;
	}


}
