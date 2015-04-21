/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.db2wkl.request.connection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import com.ibm.db2wkl.helper.DB2CommandUtility;
import com.ibm.db2wkl.helper.ThreadGroupLocal;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.Logger;

/**
 * This class provides a connection test to see whether a database is
 * available or not.
 * 
 */
public class ConnectionValidator {
    
    /**
     * sqlException for the connection test to handle issues like missing catalog
     */
    private SQLException sqlException;
    
	/**
	 * DataSource Manager Instance
	 */
	private DB2WklDataSource dataSourceManager;
	
	/**
	 * HashMap with all failed URLs and sqlException to handle issues like missing catalog
	 */
	private final static ThreadGroupLocal<HashMap<String, SQLException>> failedURLs = new ThreadGroupLocal<HashMap<String, SQLException>>();
    
	/**
	 * private default constructor because the dataSourceManager have to be set
	 */
	@SuppressWarnings("unused")
	private ConnectionValidator() {
		//default constructor
	}
	
    /**
     * ConnectionValidator constructor where the dataSourceManager have to be set
     * @param dataSourceManager as DB2WklDataSource Instance
     */
    public ConnectionValidator(DB2WklDataSource dataSourceManager) {
    	this.dataSourceManager = dataSourceManager;
    	failedURLs.set(new HashMap<String, SQLException>());
    }
	/**
	 * This method will test a single URL. It will return a ConnectionInformation with the 
	 * results.
	 * If the database is available it will return the version of the database. Otherwise
	 * it will return null if the URL is null or an negative SQL error code.
	 * @param dataSourceManager 
	 * @param user for the connection test
	 * @param password for the connection test
	 * @param url to the database
	 * @return boolean isConnectionValid, true if the Connection could be established
	 */
	public boolean validateConnection(final String url, final String user, final String password)
	{
		boolean isConnectionValid = false;
		
		try {
			// you have to ask the driver manager for the connection,
			// not a connection utility, because you need the SQLException
			// that will be thrown
			Logger.log("Checking connection for " + url, LogLevel.Debug);
			
			Connection connection = this.dataSourceManager.getConnection(this.dataSourceManager.getDataSource(url), user, password);
			
			if(connection != null ) {
				isConnectionValid = true;
				this.dataSourceManager.closeConnection(connection);
			}
			Logger.log("Connection test for " + url + " successful", LogLevel.Info);
		} catch (SQLException e) {
			addFailedURL(url,e);
			Logger.log("Connection test for " + url + " failed:" + e.getLocalizedMessage(), LogLevel.Error);
			setSqlException(e);
		} 
		
		return isConnectionValid;
	}
	
	/**
	 * This method will test a single URL. It will return a ConnectionInformation with the 
	 * results.
	 * If the database is available it will return the version of the database. Otherwise
	 * it will return null if the URL is null or an negative SQL error code.
	 * @param dataSourceManager 
	 * @param dataSource 
	 * 
	 * @param user for the connection test
	 * @param password for the connection test
	 * @param url to the database
	 * @param host name
	 * @param port number
	 * @param locationname database name
	 * @param node alias
	 * @param dbAlias as type 2 alias for the database
	 * @param noCatalog if no catalog is on, the database wont be cataloged
	 * @return boolean isConnectionValid, true if the Connection could be established
	 */
	public boolean validateConnection(final String url, final String user, final String password, final String host, final String port, final String locationname, final String node, final String dbAlias, final boolean noCatalog)
	{
		boolean isConnectionValid = false;
		
		isConnectionValid = validateConnection(url, user, password);
		
		if(isConnectionValid) return true;

		if(hasSQLException() && hasCatalogedCausedErrorCode() && !noCatalog) {
			Logger.log("Found not cataloged database", LogLevel.Debug);
			
			int result = 0;
			try {
				result = (new DB2CommandUtility()).catalogRemoteDB(host, port, locationname, node, dbAlias);
			} catch (IOException e) {
				Logger.logException(e);
				result = -1;
			} catch (InterruptedException e) {
				Logger.logException(e);
				result = -1;
			}
	
			// if we have been successful
			if(result == 0) {
				Logger.log("Try re-connection to database to validate the connection.", LogLevel.Warning);
					
				// we test again, whether the database is available NOW or not
				isConnectionValid = validateConnection(url, user, password);
			} else { 
				Logger.log("Cannot catalog DB. Error or no requestOptions available.", LogLevel.Warning);
			}
		}
		
		return isConnectionValid;
	}
	
	/**
	 * This method will test a set of URLs with the same user and password. It will 
	 * return a ConnectionInformation with the results. 
	 * If the database is available it will return the version of the database. Other-
	 * wise it will return null if the URL is null or an negative SQL error code.
	 * @param dataSourceManager 
	 * @param dburls 
	 * 
	 * @param user for the connection tests
	 * @param password for the connection tests
	 * @param urls to the databases
	 * @return ArrayList with all checked URLS
	 */
	public HashMap<String, SQLException> validateConnections(final String dburls, final String user, final String password) {
		String[] urls = dburls.split(";");
		
		failedURLs.set(new HashMap<String, SQLException>());
		
		for (final String url : urls) {
			validateConnection(url, user, password);
		}
		
		return failedURLs.get();
	}

	/**
	 * @return failedURLs as ErrorMessage with the failed URL and the exception
	 */
	public static String getErrorMessage() {
		StringBuilder errorMessage = new StringBuilder();
		int i = 0;
		for(Map.Entry<String, SQLException> entry:failedURLs.get().entrySet()) {
			i++;
			errorMessage.append(i + ":" + entry.getKey() + " failed: " + entry.getValue().getLocalizedMessage());
		}
		
		return errorMessage.toString();
	}
	/**
	 * @param url of a failed connection
	 * @param sqlException for the failed url connection
	 */
	private static void addFailedURL(String url, SQLException sqlException) {
		failedURLs.get().put(url, sqlException);
	}

	/**
	 * Returns whether the connection test resulted in an exception
	 * 
	 * @return true if an exception occurred, false otherwise
	 */
	public boolean hasSQLException() {
		return getSqlException() != null;
	}

	/**
	 * Checks whether the catalog has caused the exception if one occurred (error codes -1031, -1013 or -1097) 
	 * 
	 * @return true in case the catalog caused the exception
	 */
	public boolean hasCatalogedCausedErrorCode() {

		if(getSqlException() == null)
			return false;
		
		return (getSqlException().getErrorCode() == -1031 || getSqlException().getErrorCode() == -1013 || getSqlException().getErrorCode() == -1097);
	}
	
	/**
	 * @param sqlException
	 */
	private void setSqlException(SQLException sqlException) {
		this.sqlException = sqlException;
	}
	
	/**
	 * @return sqlException
	 */
	private SQLException getSqlException() {
		return this.sqlException;
	}
}