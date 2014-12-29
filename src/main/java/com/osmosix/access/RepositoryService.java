package com.osmosix.access;

import org.tmatesoft.sqljet.core.SqlJetException;

import java.util.List;

/**
 * Created by irraju on 07/09/14.
 */
public interface RepositoryService {


	static final String DB_NAME = "/usr/local/tomcat/logs/gua.db";
	static final String CONNECTIONS_TABLE_NAME = "CONNECTIONS";
	static final String USER_TABLE_NAME = "USERS";

	static final String CONNECTION_ID = "connection_id";
	static final String HOST_NAME = "hostname";
	static final String CONNECTION_TYPE = "connection_type";
	static final String OPENED_ON   = "opened_on";
	static final String CLOSED_ON = "closed_on";
	static final String STATUS = "status";

	static final String USERNAME = "username";
	static final String PASSWORD = "password";

	void buildSchemaAgain() throws SqlJetException;

	//void createUser(String userName, String password) throws SqlJetException;

	void addConnection(ConnectionModel connection) throws SqlJetException;

	void updateConnectionStatus(ConnectionModel connection) throws SqlJetException;

	List<ConnectionModel> getConnections() throws SqlJetException;

	List<ConnectionModel> getActiveConnections() throws SqlJetException;

	void createDefaultUsers() throws SqlJetException;
}
