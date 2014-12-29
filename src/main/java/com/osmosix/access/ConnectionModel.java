package com.osmosix.access;

import java.sql.Connection;

/**
 * Created by irraju on 07/09/14.
 */
public class ConnectionModel {
	private String connectionId;
	private String hostname;
	private String connectionType;
	private String openedOn;
	private String closedOn;
	private String status;


	ConnectionModel(){
	}

	ConnectionModel(String connectionId, String hostname, String connectionType){
		this.connectionId = connectionId;
		this.hostname = hostname;
		this.connectionType = connectionType;
	}
	public String getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getOpenedOn() {
		return openedOn;
	}

	public void setOpenedOn(String openedOn) {
		this.openedOn = openedOn;
	}

	public String getClosedOn() {
		return closedOn;
	}

	public void setClosedOn(String closedOn) {
		this.closedOn = closedOn;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
