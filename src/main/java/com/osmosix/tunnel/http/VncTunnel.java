package com.osmosix.tunnel.http;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.InetGuacamoleSocket;
import org.glyptodon.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.servlet.GuacamoleSession;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Map;


public class VncTunnel extends HttpTunnel {

	private Logger _logger = Logger.getLogger(VncTunnel.class);

	@Override
	protected GuacamoleTunnel doConnect(HttpServletRequest request) throws GuacamoleException {

		HttpSession httpSession = request.getSession(true);
		String contextPath = request.getContextPath();
		GuacamoleClientInformation info = new GuacamoleClientInformation();
		Map<String, Object> serverSettings = null;
		Map<String, Object> connectionSettings = null;
		Map<String, Object> sshConnectionDetails = null;

		// PROCESS REQUEST PARAMS
		String width = request.getParameter("width");
		if (width != null)
			info.setOptimalScreenWidth(Integer.parseInt(width));
		String height = request.getParameter("height");
		if (height != null)
			info.setOptimalScreenHeight(Integer.parseInt(height));
		String[] audio_mimetypes = request.getParameterValues("audio");
		if (audio_mimetypes != null)
			info.getAudioMimetypes().addAll(Arrays.asList(audio_mimetypes));
		String[] video_mimetypes = request.getParameterValues("video");
		if (video_mimetypes != null)
			info.getVideoMimetypes().addAll(Arrays.asList(video_mimetypes));
		String protocol = request.getParameter("protocol");
		String mgmtSessionId = request.getParameter("session");

		// Create socket
		GuacamoleConfiguration config = new GuacamoleConfiguration();
		config.setProtocol(protocol);

		if ("vnc".equals(protocol)) {
			try{
				// initialize ssl for truststore
				initSslForTrustStore();
				serverSettings = getServerDetails(contextPath);
				String mgmtserver = (String)serverSettings.get("mgmtserver.dnsName");

				_logger.debug("MGMT SERVER - "+mgmtserver);
				if(StringUtils.isEmpty(mgmtserver)){
					_logger.error("MGMT Server not configured in mgmt.properties.");
					throw new GuacamoleException("MGMT Server not configured in mgmt.properties.");
				}
				sshConnectionDetails = getSshConnectionInfo(mgmtserver,mgmtSessionId);
				for(Map.Entry<String, Object> entry : sshConnectionDetails.entrySet()){
					String key = entry.getKey();
					String value = entry.getValue().toString();
					if (key.equals("publicIPAddr")) {
						config.setParameter("hostname",value);
					} else if (key.equals("userName")) {
						config.setParameter("username",value);
					} else if (key.equals("sshPort")) {
						config.setParameter("port",value);
					} else if (key.equals("sshPemKey")) {
						config.setParameter("private-key",value);
					}
				}
				config.setParameter("port", "5901");//always the vnc server on display :1
				config.setParameter("password", "osmosix");//"Osm0siX32" //has to match with one in agent bundle
				_logger.debug("SSH Connection Info set.");

				connectionSettings = getConnectionSettings(protocol,contextPath);
				_logger.debug("Connection Settings Set.");
				config = setConfigSettings(config,connectionSettings);
				_logger.debug("Config Settings Set");
			}catch(Exception ex){
				_logger.error("ERROR while getting details from mgmt server"+ex.getLocalizedMessage(), ex);
				ex.printStackTrace();
				_logger.debug(ex.getMessage());
			}

		}
		else {
			_logger.error("UNKNOWN PROTOCOL");
		}


		// Return connected socket
		String guacdHost = (String)serverSettings.get("guacd-hostname");
		if(StringUtils.isEmpty(guacdHost)){
			_logger.warn("guacd-host is not configured in mgmt.properties. Using localhost now");
			guacdHost = "localhost";
		}
		String guacdPort = serverSettings.get("guacd-port").toString();
		if(StringUtils.isEmpty(guacdPort)){
			_logger.warn("guacd-port is not configured in mgmt.properties. Using 4822 now");
			guacdPort = "4822";
		}

		_logger.debug("GUACD-HOST - "+guacdHost);
		_logger.debug("GUACD-PORT - "+guacdPort);



		GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
				new InetGuacamoleSocket(guacdHost, Integer.parseInt(guacdPort)),
				config, info
		);

		_logger.debug("STATUS - CREATED - Successfully created socket connection.");

		// Create tunnel from now-configured socket
		GuacamoleTunnel tunnel = new GuacamoleTunnel(socket);

		// Attach tunnel
		GuacamoleSession session = new GuacamoleSession(httpSession);
		session.attachTunnel(tunnel);
		_logger.debug("STATUS - DONE - Successfully created session and attached tunnel");
		return tunnel;
	}

}
