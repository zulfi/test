package com.osmosix.access;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by irraju on 07/09/14.
 */
public class RepositoryServiceImpl implements RepositoryService  {

	private File dbFile;

	public void init() throws SqlJetException{
		dbFile = new File(DB_NAME);
		SqlJetDb db = SqlJetDb.open(dbFile, true);
		// set DB option that have to be set before running any transactions:
		db.getOptions().setAutovacuum(true);
		db.beginTransaction(SqlJetTransactionMode.WRITE);
		try {
			db.getOptions().setUserVersion(1);
		} finally {
			db.commit();
		}
		db.close();
	}

	@Override
	public void buildSchemaAgain() throws SqlJetException {
		System.out.println("Destroying the older DB files.");
		dbFile.delete();
		System.out.println("Creating new DB from Scratch");
		init();

		SqlJetDb db = SqlJetDb.open(dbFile, true);
		db.beginTransaction(SqlJetTransactionMode.WRITE);
		try {

			String createConnectionsTableQuery = "CREATE TABLE " + CONNECTIONS_TABLE_NAME + " (" + CONNECTION_ID + " TEXT NOT NULL PRIMARY KEY , "
					+ HOST_NAME + " TEXT NOT NULL, " + CONNECTION_TYPE + " TEXT NOT NULL,"
					+ OPENED_ON +" TEXT NOT NULL, "+ CLOSED_ON +" TEXT, "+ STATUS + " TEXT)";
			String statusIndexQuery = "CREATE INDEX STATUS_INDEX ON "+CONNECTIONS_TABLE_NAME+ " ("+STATUS+")";

					System.out.println();
			System.out.println(">DB schema queries:");
			System.out.println();
			System.out.println(createConnectionsTableQuery);
			db.createTable(createConnectionsTableQuery);
			db.createIndex(statusIndexQuery);
		} finally {
			db.commit();
			db.close();
		}
	}

//	@Override
//	public void createUser(String userName, String password) throws SqlJetException {
//		System.out.println(">Creating  User:"+userName);
//		SqlJetDb db = SqlJetDb.open(dbFile, true);
//		db.beginTransaction(SqlJetTransactionMode.WRITE);
//		try {
//			ISqlJetTable table = db.getTable(USER_TABLE_NAME);
//			table.insert(userName, password);
//		} finally {
//			db.commit();
//			db.close();
//		}
//
//	}

	@Override
	public void addConnection(ConnectionModel connection) throws SqlJetException {
		System.out.println(">Creating  Connection:"+connection.getConnectionId());
		SqlJetDb db = SqlJetDb.open(dbFile, true);
		db.beginTransaction(SqlJetTransactionMode.WRITE);
		try {
			ISqlJetTable table = db.getTable(CONNECTIONS_TABLE_NAME);
			table.insert(connection.getConnectionId(),connection.getHostname(),connection.getConnectionType(),
					connection.getOpenedOn(), connection.getClosedOn(), connection.getStatus());
		} finally {
			db.commit();
			db.close();
		}
	}

	@Override
	public void updateConnectionStatus(ConnectionModel connection) throws SqlJetException{

		ISqlJetCursor rowCursor = null;
		System.out.println(">Updating  Connection:");
		SqlJetDb db = SqlJetDb.open(dbFile, true);
		db.beginTransaction(SqlJetTransactionMode.WRITE);
		try {
			ISqlJetTable table = db.getTable(CONNECTIONS_TABLE_NAME);
			rowCursor = table.lookup(CONNECTION_ID, connection.getConnectionId());
			rowCursor.update(connection);

		} finally {
			if(rowCursor != null)
				rowCursor.close();
			db.commit();
			db.close();
		}
	}

	@Override
	public List<ConnectionModel> getConnections() throws SqlJetException {
		ISqlJetCursor cursor = null;
		List<ConnectionModel> result = new ArrayList<ConnectionModel>();

		System.out.println(">Getting list of  connections:");
		SqlJetDb db = SqlJetDb.open(dbFile, false);
		db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
		try {
			ISqlJetTable table = db.getTable(CONNECTIONS_TABLE_NAME);
			cursor = table.open().reverse();
			System.out.println(">Reading table list:");

			if (!cursor.eof()) {
				do {
					ConnectionModel connection = new ConnectionModel();
					connection.setConnectionId(cursor.getString(CONNECTION_ID));
					connection.setHostname(cursor.getString(HOST_NAME));
					connection.setConnectionType(cursor.getString(CONNECTION_TYPE));
					connection.setOpenedOn(cursor.getString(OPENED_ON));
					connection.setClosedOn(cursor.getString(CLOSED_ON));
					connection.setStatus(cursor.getString(STATUS));
					System.out.println(">Connection: "+connection.toString());
					result.add(connection);
				} while(cursor.next());
			}
		} finally {
			if(cursor != null)
				cursor.close();
			db.close();
		}
		return result;
	}

	@Override
	public List<ConnectionModel> getActiveConnections() throws SqlJetException {
		ISqlJetCursor cursor = null;
		List<ConnectionModel> result = new ArrayList<ConnectionModel>();

		System.out.println(">Getting list of  connections:");
		SqlJetDb db = SqlJetDb.open(dbFile, false);
		db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
		try {
			ISqlJetTable table = db.getTable(CONNECTIONS_TABLE_NAME);
			cursor = table.lookup("STATUS_INDEX","ACTIVE").reverse();
			if (!cursor.eof()) {
				do {
					ConnectionModel connection = new ConnectionModel();
					connection.setConnectionId(cursor.getString(CONNECTION_ID));
					connection.setHostname(cursor.getString(HOST_NAME));
					connection.setConnectionType(cursor.getString(CONNECTION_TYPE));
					connection.setOpenedOn(cursor.getString(OPENED_ON));
					connection.setClosedOn(cursor.getString(CLOSED_ON));
					connection.setStatus(cursor.getString(STATUS));
					System.out.println(">Connection: "+connection.toString());
					result.add(connection);
				} while(cursor.next());
			}
		} finally {
			if(cursor != null)
				cursor.close();
			db.commit();
			db.close();
		}
		return result;
	}


	@Override
	public void createDefaultUsers() throws SqlJetException {

		ConnectionModel connection = new ConnectionModel("FIRST","ABC.COM","SSH");
		connection.setStatus("ACTIVE");
		connection.setOpenedOn("TODAY");
		System.out.println(">Adding connection: FIRST");
		addConnection(connection);
		connection = new ConnectionModel("SECOND","DEF.COM","VNC");
		connection.setStatus("ACTIVE");
		connection.setOpenedOn("TODAY");
		System.out.println(">Adding connection: SECOND");
		addConnection(connection);
		connection = new ConnectionModel("THIRD","GHI.COM","RDP");
		connection.setStatus("CLOSED");
		connection.setOpenedOn("TODAY");
		System.out.println(">Adding connection: THIRD");
		addConnection(connection);
	}
}
