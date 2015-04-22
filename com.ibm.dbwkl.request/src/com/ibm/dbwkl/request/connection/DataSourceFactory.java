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
package com.ibm.dbwkl.request.connection;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashMap;

import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.request.Logger;
import com.ibm.dbwkl.request.Request;
import com.ibm.dbwkl.request.parser.Options;

/**
 *
 */
public class DataSourceFactory {

	/**
	 * Creates a connection manager according to the configuration and command line input
	 * 
	 * @return the connection manager as dataSource
	 */
	public static DB2WklDataSource createDataSource() {
		DB2WklDataSource dataSourceManager = null;
		if (Request.hasOption(Options.DB_POOLING)) {
			dataSourceManager = new DB2zPooledDataSource();
		}
		else {
			dataSourceManager = new DB2zSimpleDataSource();
		}
		
		return dataSourceManager;
	}

	/**
	 * initializes the dataSources based on the Request Options
	 * @param dataSourceManager
	 * @return boolean true if the connections are valid
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 * @throws MalformedURLException 
	 */
	public static boolean initializeDataSources(DB2WklDataSource dataSourceManager) throws MalformedURLException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		String urls = null;
		boolean isConnectionValid = false;
		
		//splitting URLs and creating new DataSource instance per URL
		if (Request.hasOption(Options.DB_URLS)) {
			urls = Request.getOption(Options.DB_URLS);
			dataSourceManager.initializeDataSources(urls);
			HashMap<String, SQLException> failedURLs = new ConnectionValidator(Request.getDataSource()).validateConnections(urls, Request.getOption(Options.DB_USER), Request.getOption(Options.DB_PASSWORD));
			isConnectionValid = (failedURLs.size()==0);
		}
		
		//creating new DataSource instance with given URL
		else if (Request.hasOption(Options.DB_URL)) {
			urls = Request.getOption(Options.DB_URL);
			dataSourceManager.initializeDataSource(urls);
			isConnectionValid = new ConnectionValidator(Request.getDataSource()).validateConnection(urls, Request.getOption(Options.DB_USER), Request.getOption(Options.DB_PASSWORD));
		}
		
		else if (Request.hasOption(Options.DB_HOST)){
			//TYPE2 Connection (with possibility of automatic cataloging)
			if(Request.hasOption(Options.DB_DBALIAS)){
				urls = createURL(Request.getOption(Options.DB_DBALIAS));
				dataSourceManager.initializeDataSource(urls, Request.getOption(Options.DB_DBALIAS));
				isConnectionValid = new ConnectionValidator(Request.getDataSource()).validateConnection(urls, Request.getOption(Options.DB_USER), Request.getOption(Options.DB_PASSWORD), Request.getOption(Options.DB_HOST), Request.getOption(Options.DB_PORT), Request.getOption(Options.DB_LOCATIONNAME), Request.getOption(Options.DB_DBNODE), Request.getOption(Options.DB_DBALIAS), Request.hasOption(Options.DB_NOCATALOG));			
			}
			//TYPE4 Connection
			else {
				urls = createURL(Request.getOption(Options.DB_HOST), Request.getOption(Options.DB_PORT), Request.getOption(Options.DB_LOCATIONNAME));
				int port = 0;
				try{
					port = Integer.parseInt(Request.getOption(Options.DB_PORT));
				} catch(NumberFormatException e){
					Logger.log(e.getMessage(), LogLevel.Error);
					throw new RuntimeException(e);
				}
				dataSourceManager.initializeDataSource(urls, Request.getOption(Options.DB_HOST), port, Request.getOption(Options.DB_LOCATIONNAME));
				isConnectionValid = new ConnectionValidator(Request.getDataSource()).validateConnection(urls, Request.getOption(Options.DB_USER), Request.getOption(Options.DB_PASSWORD));			
			}
		}
		
		//TYPE2 Connection (without possibility of automatic cataloging)
		else if (Request.hasOption(Options.DB_ALIAS)) {
			urls = createURL(Request.getOption(Options.DB_ALIAS));
			dataSourceManager.initializeDataSource(urls, Request.getOption(Options.DB_ALIAS));
			isConnectionValid = new ConnectionValidator(Request.getDataSource()).validateConnection(urls, Request.getOption(Options.DB_USER), Request.getOption(Options.DB_PASSWORD));
		}
		
		else if(Request.hasOption(Options.DB_CFG)){
			DatabaseConfigurationLoader databaseConfigurationLoader = DatabaseConfigurationLoader.getInstance();
			databaseConfigurationLoader.loadFiles(Request.getOption(Options.DB_CFG));
	
			String type = "4";
			
			if (Request.hasOption(Options.DB_TYPE_2)) 
				type = "2";
			else if (Request.hasOption(Options.DB_TYPE_4))
				type = "4";
			
			if (Request.hasOption(Options.DB_CFG_ID)) {
				urls = databaseConfigurationLoader.loadUrls(Request.getOption(Options.DB_CFG_ID), type);
				if(urls == null) {
					urls = "INVALID CFGIDs";
				}
				dataSourceManager.initializeDataSources(urls);
				HashMap<String, SQLException> failedURLs = new ConnectionValidator(Request.getDataSource()).validateConnections(urls, Request.getOption(Options.DB_USER), Request.getOption(Options.DB_PASSWORD));
				isConnectionValid = (failedURLs.size() == 0);
			} else if(Request.hasOption(Options.DB_CFG_SSID)) {
				urls = databaseConfigurationLoader.loadUrl(Request.getOption(Options.DB_CFG_SSID), Request.getOption(Options.DB_CFG_SHOST), type);
				dataSourceManager.initializeDataSource(urls);
				isConnectionValid = new ConnectionValidator(Request.getDataSource()).validateConnection(urls, Request.getOption(Options.DB_USER), Request.getOption(Options.DB_PASSWORD));
			}
		}
		return isConnectionValid;
		
	}
	
	/**
	 * @param dbAlias
	 * @return a type 2 URL created with the cataloged database alias name
	 */
	private static String createURL(String dbAlias) {
		return "jdbc:db2:" + dbAlias;
	}

	/**
	 * @param host as server name for the connection 
	 * @param port as connection port
	 * @param locationname as database name
	 * @return a type 4 URL created with the host, port and location name
	 */
	private static String createURL(String host, String port, String locationname) {
		return "jdbc:db2://" + host + ":" + port + "/" + locationname;
	}

	/**
	 * @return connectionValidator ErrorMessage with the failed URLs and error messages
	 */
	public static String getErrorMessage() {
		return ConnectionValidator.getErrorMessage();
	}
}
