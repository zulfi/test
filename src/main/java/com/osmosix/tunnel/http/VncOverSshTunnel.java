package com.osmosix.tunnel.http;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;


public class VncOverSshTunnel extends HttpTunnel {

	private Logger _logger = Logger.getLogger(VncOverSshTunnel.class);
	private Session jschSession;
	private int localVNCPort;



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
				String publicIpAddr = "",
						userName = "",
						sshPort = "",
						sshPemKey ="";
				for(Map.Entry<String, Object> entry : sshConnectionDetails.entrySet()){
					String key = entry.getKey();
					String value = entry.getValue().toString();
					_logger.debug("K: "+key+" V: "+value);
					if (key.equals("publicIPAddr")) {
						publicIpAddr = value;
					} else if (key.equals("userName")) {
						userName = value;
					} else if (key.equals("sshPort")) {
						sshPort = value;
					} else if (key.equals("sshPemKey")) {
						sshPemKey = value;
					}
				}


				// got info from server return
				JSch jsch = new JSch();

				String knownHostFile = getKnownHostsFile();
				try {
					jsch.setKnownHosts(knownHostFile);
				} catch (JSchException e1) {
					_logger.warn("Failed to set known host file");
				}

				try {
					jsch.removeAllIdentity();
					jsch.addIdentity("sshkey", sshPemKey.getBytes(), null, null);
					jschSession = jsch.getSession(userName, publicIpAddr, Integer.parseInt(sshPort));
					UserInfo ui = new VNCUserInfo();
					jschSession.setUserInfo(ui);
					jschSession.connect();
					int port;
					for (port = 10000; port < 10500; port ++) {
						try {
							localVNCPort=jschSession.setPortForwardingL(port, "127.0.0.1", 5901);
							break;
						} catch (Exception e) {
						}
					}
					if (port == 10500) {
						_logger.warn("Failed to establish SSH port forwarding tunnel, exit now");
						return null;
					}

				} catch (Exception e) {
					_logger.warn("Failed to establish SSH session");
				}




				config.setParameter("hostname","localhost");
				config.setParameter("port", String.valueOf(localVNCPort));//always the vnc server on display :1
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

		// Create tunnel from now-configured socket
		GuacamoleTunnel tunnel = new GuacamoleTunnel(socket);

		// Attach tunnel
		GuacamoleSession session = new GuacamoleSession(httpSession);
		session.attachTunnel(tunnel);
		_logger.debug("CONNECTED -  VNC session "+config.getParameter("userName")+"@"+
				config.getParameter("hostname")+" using tunnel "+tunnel.getUUID());
		return tunnel;
	}

	private String getKnownHostsFile() {
		String hostsFile = "known_hosts";

		String dirPath = System.getProperty("user.home") + System.getProperty("file.separator") + ".ssh";

		String filePath = dirPath + System.getProperty("file.separator") + hostsFile;
		try {
			File file = new File(filePath);

			boolean fileexists = file.exists();
			if (!fileexists) {
				File directory = new File(dirPath);
				boolean directoryexists = directory.exists();
				if (!directoryexists) {
					directory.mkdirs();
					file.createNewFile();
				} else {
					file.createNewFile();
				}
			}
		} catch (IOException e) {
			return null;
		}

		return filePath;
	}

}
