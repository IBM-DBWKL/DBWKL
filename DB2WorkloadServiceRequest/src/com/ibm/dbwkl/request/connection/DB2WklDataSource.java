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
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.ibm.db2.jcc.DB2BaseDataSource;
import com.ibm.dbwkl.helper.URLCutter;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.request.LoggedRuntimeException;
import com.ibm.dbwkl.request.Logger;
import com.ibm.dbwkl.request.Request;
import com.ibm.dbwkl.request.parser.Options;
import com.ibm.dbwkl.workloadservice.ADataSourceConsumer;

/**
 * New connection manager that handles the connections for a request
 * 
 *
 */
public abstract class DB2WklDataSource {

	/**
	 * List of connection pools where each pool is responsible for 
	 * one URL connection. 
	 */
	protected HashMap<String, DB2BaseDataSource> dataSourceList;
	
	/**
	 * List of active connections. For correct maintenance use always this
	 * connection manager.
	 */
	protected ArrayList<Connection> activeConnections; 
	
	/**
	 * List of active statements. For correct maintenance use always this 
	 * connection manager to get and close statements.
	 */
	protected ArrayList<Statement> activeStatements;
	
	/**
	 * This default URL contains the first URL that is initialized in this 
	 * connection manager
	 */
	protected String defaultURL = null;
	
	/**
	 * This method must be implemented by all extending data sources
	 * 
	 * @param dataSource data source to get a connection to 
	 * @param user name needed to establish the connection
	 * @param password needed to establish the connection
	 * @return the created connection
	 * @throws SQLException could not build up the connection
	 */
	public abstract Connection getConnection(DB2BaseDataSource dataSource, String user, String password) throws SQLException;
	
	/**
	 * Initializes a data source. Each data source must extend DB2BaseDataSource
	 * 
	 * @return the initialized data source
	 */
	protected abstract DB2BaseDataSource initializeDataSourceInternal();
	
	/**
	 * This method is used to create a table space which is basically a logical or physical
	 * container (depending on the data source) for workload tables. Each data provider needs
	 * to implement this method in order to provide system specific support.
	 * 
	 * @param prefix the prefix for a table space that is created by this method. Note that differs
	 * 			from the tableSpace table space name in the way that additional characters are added
	 * 			at the end of this prefix
	 * @param tableSpace the name of the table space to create. If this is null, a table space
	 * 			of the following form having a random alphanumeric characters will be generated. 
	 * 			<p>
	 * 			<i>TWKLxxxx</i> where x is an alphanumeric character
	 * @param creationStatement a table space creation statement that can contain place holders
	 * 			for the table space name and the database to be created in for DB2 z/OS implementation
	 * @return the name of the generated table space
	 */
	public abstract String createTablespace(String prefix, String tableSpace, String creationStatement);
	
	/**
	 * Target database system depending method that has to be implemented by each data provider
	 * available. The table space is used as a container for all workload specific tables
	 * for a workload. Using this method, the workload writer has it easier to write workloads.
	 * 
	 * @param tableSpace the name of the table space to delete 
	 * @return true if the deletion was successful, false otherwise
	 */
	public abstract boolean dropTablespace(String tableSpace);
	
	/**
	 * Returns the target where the database objects are created. This is data provider
	 * dependent.
	 * <p>
	 * For DB2 zOS the target consists of a database name and a table space name which
	 * is used in different types of statements. E.g.
	 * <p>
	 * CREATE TABLE abc(a int) IN [dbname].[tsname]
	 * <p>
	 * In this case, getTarget() would return the IN ... depending on what is specified
	 * on the command line. Without dbname and tsname specified an empty string is returned.
	 * If dbname and tsname is specified dbname and tsname is returned. If only dbname
	 * is specified then IN DATABASE dbname is returned.
	 * <p>
	 * For other database systems this may be completely different. In such case, getTarget()
	 * may always return an empty string.
	 * 
	 * @param tableSpace The name of a table space if one exists. The implementor must also
	 * 			handle the case when it's null. 
	 * @return the target for DDL statements
	 */
	public abstract String getTarget(String tableSpace);
	
	/**
	 * Default constructor
	 */
	public DB2WklDataSource() {
		this.dataSourceList = new HashMap<String, DB2BaseDataSource>();
		this.activeConnections = new ArrayList<Connection>();
		this.activeStatements = new ArrayList<Statement>();
	}
	
	/**
	 * Initializes the connection pools for a given list of URLs
	 * 
	 * @param urls list of URLs
	 * @return number of newly created connection pools
	 * @throws MalformedURLException 
	 * @throws IllegalArgumentException 
	 */
	public int initializeDataSources(String urls) throws MalformedURLException, IllegalArgumentException {
		// counter for newly created connection pools
		int i = 0;
		
		// tokenize the url list by ; and handle each url separately
		StringTokenizer tokenizer = new StringTokenizer(urls, ";");
		while (tokenizer.hasMoreElements()) {
			String url = (String) tokenizer.nextElement();
			Logger.log(url, LogLevel.Debug);
			boolean result = initializeDataSource(url);
			if (result) 
				i++;
		}
		
		// log the number of newly created connection pools and return the result
		return i;
	}

	/**
	 * Initializes the connection pool for a specific URL
	 * 
	 * @param url the URL to initialize a connection pool for
	 * @return true in case a new connection pool was added for the URL
	 * @throws MalformedURLException 
	 * @throws IllegalArgumentException 
	 */
	public boolean initializeDataSource(String url) throws MalformedURLException, IllegalArgumentException {
		URLCutter cutter = new URLCutter(url);
		if(Request.hasOption(Options.DB_TYPE_2) || URLCutter.isType2URL(url)) {
			return initializeDataSource(url, cutter.getDatabaseName());
		} else {
			return initializeDataSource(url, cutter.getServer(), cutter.getPort(), cutter.getDatabaseName());
		}
	}
	
	/**
	 * Initializes the connection pool for a specific Type 2 URL
	 * 
	 * @param url as type 2 connection URL
	 * @param databaseAlias the Catalog Database Alias
	 * @return true in case a new connection pool was added for the URL
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	public boolean initializeDataSource(String url, String databaseAlias) throws IllegalArgumentException {
		// only add if not existing yet
		if (this.dataSourceList.containsKey(url)) {
			return false;
		}
		
		// if this is the first URL, set it as default
		if (this.defaultURL == null) {
			this.defaultURL = url;
		}
		
		// initialize the data source
		DB2BaseDataSource dataSource = initializeDataSourceInternal();
		
		dataSource.setDatabaseName(databaseAlias);
		dataSource.setDriverType(2);
		
		setAdditionalConnectionProperties(dataSource);
		
		// add the connection pool
		this.dataSourceList.put(url, dataSource);
		
		return true;		
	}

	/**
	 * Initializes the connection pool for a specific Type 4 URL
	 * 
	 * @param url the URL to initialize a connection pool for
	 * @param host for the connection
	 * @param port for the connection
	 * @param locationname as the database name for the connection
	 * @return true in case a new connection pool was added for the URL
	 * @throws IllegalArgumentException 
	 */
	public boolean initializeDataSource(String url, String host, int port, String locationname) throws IllegalArgumentException {
		
		// only add if not existing yet
		if (this.dataSourceList.containsKey(url)) {
			return false;
		}
		
		// if this is the first URL, set it as default
		if (this.defaultURL == null) {
			this.defaultURL = url;
		}
		
		// initialize the data source
		DB2BaseDataSource dataSource = initializeDataSourceInternal();
		
		dataSource.setServerName(host);
		dataSource.setPortNumber(port);
		dataSource.setDatabaseName(locationname);
		dataSource.setDriverType(4);
		
		setAdditionalConnectionProperties(dataSource);
		
		// add the connection pool
		this.dataSourceList.put(url, dataSource);
		
		return true;
	}
	
	/**
	 * Retrieving and setting the additional requestOptions like PDQ and DB2options
	 * @param dataSource
	 * @throws IllegalArgumentException 
	 */
	private void setAdditionalConnectionProperties(DB2BaseDataSource dataSource) throws IllegalArgumentException{
		
		// Set the client program name and application to a default: db2wkl_req<n>. This can
		// be overridden when using ClientProgramName with DB2OPTIONS
		String db2wkl_reqn = "db2wkl_" + Request.getRequestName();
		dataSource.setClientProgramName(db2wkl_reqn);
		dataSource.setClientApplicationInformation(db2wkl_reqn);
		try {
			dataSource.setClientWorkstation(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e1) {
			// in this case ignore as it is not important or necessary to set this name
		}
		
		// add additional properties
		if (Request.hasOption(Options.DB_DB2OPTIONS)) {
			String props = Request.getOption(Options.DB_DB2OPTIONS);
			Method methods[] = DB2BaseDataSource.class.getMethods();
			ArrayList<Method> methodList = new ArrayList<Method>();
			Collections.addAll(methodList, methods);
			
			StringTokenizer tokenizer = new StringTokenizer(props, ";");
			while (tokenizer.hasMoreElements()) {
				String prop = (String) tokenizer.nextElement();
				String key = prop.substring(0, prop.indexOf("="));
				String value = prop.substring(prop.indexOf("=") + 1);
				for (Method method : methods) {
					if (method.getName().equalsIgnoreCase("set" + key)) {
						try {
							Method	m = dataSource.getClass().getMethod("set" + key, method.getParameterTypes());
							Class<?> types = method.getParameterTypes()[0];
							if (types == Integer.class) 
								m.invoke(dataSource, new Object[] { Integer.getInteger(value) });
							else if (types == Boolean.class) 
								m.invoke(dataSource, new Object[] { Boolean.valueOf(value) });
							else if (types == String.class) 
								m.invoke(dataSource, new Object[] { value });
							else if (types == Short.class) 
								m.invoke(dataSource, new Object[] { Short.valueOf(value) });
							else if (types == Long.class) 
								m.invoke(dataSource, new Object[] { Long.getLong(value) });
						} catch (Exception e) {
							throw new IllegalArgumentException("Exception occurs when setting additional db2 option " + prop, e);
						}
						break;						
					}
				}
			}
		}	
	}

	/**
	 * @return urlsList with all initialized URLs
	 */
	public ArrayList<String> getURLsList() {
		return new ArrayList<String>(this.dataSourceList.keySet());
	}

	/**
	 * Returns the data source for the given URL. Note that if the 
	 * data source does not exist yet, it is created
	 * 
	 * @param url URL to get the data source for 
	 * @return the data source for the given URL
	 */
	public DB2BaseDataSource getDataSource(String url) {
		return this.dataSourceList.get(url);
	}
	
	/**
	 * Returns the data source for the given URL. If it does not exist,
	 * create can be used to allow creation.
	 * 
	 * @param url the URL to get the data source for
	 * @param create if true the data source will be created if it does not exist yet
	 * @return the data source, note that it might be null in case it does not exist and create is set to false
	 * @throws MalformedURLException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	public synchronized DB2BaseDataSource getDataSource(String url, boolean create) throws MalformedURLException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		
		// if it exists, just return
		if (this.dataSourceList.containsKey(url)) {
			return getDataSource(url);
		}
		
		// it does not exist, if it should be created, do it
		if (create) {
			initializeDataSource(url);
		}
		
		// now return the result, independent of whether it was created or not
		return getDataSource(url);
	}
	
	/**
	 * Returns a new/reused connection (depending on whether connection pool is used or not used)
	 * 
	 * @return a new connection to the default data source
	 */
	public synchronized Connection getConnection() {

		String url = ADataSourceConsumer.getUrl();
		
		if(url == null){
			url = this.defaultURL;
			Logger.log("This is the case when the URL is not set for the workload.", 
					LogLevel.Warning);
		}
		
		// get the connection
		return getConnection(url);
	}
	
	/**
	 * Returns a new/reused connection (depending on whether connection pool
	 * is used or not) for the given URL
	 * 
	 * @param url URL to get the connection for
	 * @return the connection
	 */
	public Connection getConnection(String url) {
		return getConnection(url, Request.getOption(Options.DB_USER), Request.getOption(Options.DB_PASSWORD));
	}
	
	/**
	 * Returns a new/reused connection for user and password(depending on whether connection pool
	 * is used or not) with the default URL
	 * 
	 * @param url URL to get the connection for, if null then default URL
	 * @param user name to establish the connection
	 * @param password to establish the connection
	 * @return the connection
	 */
	public Connection getConnection(String url, String user, String password) {
		
		DB2BaseDataSource dataSource;
		
		// get the data source
		if(!(url == null || url.length() == 0)) {
			dataSource = this.dataSourceList.get(url);
		}
		else {
			dataSource = this.dataSourceList.get(this.defaultURL);
		}
		if (dataSource == null) {
			throw new LoggedRuntimeException("The specified data source does not exist. " +
					"This is the case when the URL is initialized in the " +
					"Connection Manager. Note that this should not happen! If" +
					"you encounter this error message, open a defect.");
		}
		
		Connection con = null;
		
		//this counts the number of iterations the loop will go through in case a certain bindException occurs, this
		//will usually only have a value of 1 since everything will be ok in most cases resulting in only one iteration.
		int iterationLoopCounter = 0;
		
		while (iterationLoopCounter < 5000) {
			
			iterationLoopCounter++;
			
			//only write a log message for every 500s iteration
			if (iterationLoopCounter % 500 == 0) {
				Logger.log("Iteration count for getConnection() is ongoing: " + iterationLoopCounter, LogLevel.Debug);
			}
			// get the connection
			con = null;
			try {
				con = this.getConnection(dataSource, user, password);
				con.setAutoCommit(false);
				
				// new connection, add it to the list of active connections if not null
				if (con != null && !this.activeConnections.contains(con)){
					this.activeConnections.add(con);
				}
				//will only show in case an SQLException has occurred beforehand 
				if (iterationLoopCounter > 1) {
					Logger.log("Iteration count is complete for this getConnection() attempt: " + iterationLoopCounter, LogLevel.Debug);
				}
				return con;
			
			} catch (SQLException e) {
			
				//Special handling for a rare case where a socket is still in use even though it should be closed.
				//This seems to be a problem with java handling and not DB2WKL itself. Retry if this happens!  
				if (e.getCause() != null && e.getCause().toString().startsWith("java.net.BindException") && e.getErrorCode() == -4499) {
					
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {
						//don't do anything if interrupted!
					}

				} else {

					throw new LoggedRuntimeException("Could not establish a connection due to the following reason: " + 
							e.getLocalizedMessage(), 
							e);
				}
			}
		}
		throw new LoggedRuntimeException("Could not get connection after 5000 tries. Abort!");
		
	}
	
	/**
	 * Creates a statement
	 * 
	 * @param con connection to create the statement for
	 * @return the created statement
	 * @throws SQLException creation unsuccessful
	 */
	public synchronized Statement createStatement(Connection con) throws SQLException {
		Statement stmt = con.createStatement();
		this.activeStatements.add(stmt);
		return stmt;
	}
	
	/**
	 * Creates a prepared statement
	 * 
	 * @param con connection to create the statement for
	 * @param sql SQL text
	 * @return the created prepared statement
	 * @throws SQLException creation unsuccessful
	 */
	public synchronized PreparedStatement createPreparedStatement(Connection con, String sql) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(sql);
		this.activeStatements.add(stmt);
		return stmt;
	}
	
	/**
	 * Creates a callable statement
	 * 
	 * @param con connection to create the statement for
	 * @param sql SQL text
	 * @return the created callable statement
	 * @throws SQLException creation unsuccessful
	 */
	public synchronized CallableStatement createPreparedCallableStatement(Connection con, String sql) throws SQLException {
		CallableStatement stmt = con.prepareCall(sql);
		this.activeStatements.add(stmt);
		return stmt;
	}
	
	/**
	 * Closes a given connection. Note that this method automatically
	 * makes a rollback. Thus, if you want to preserve your changes
	 * and you not have autocommit enabled use a commit before calling
	 * this method.
	 * 
	 * @param url URL to close the connection for
	 * @param connection connection to close
	 */
	public synchronized void closeConnection(Connection connection) {
		
		try {
			if (connection != null && connection.isClosed() != true) {
				try {
					connection.rollback();
				} catch (SQLException e) {
					Logger.log("There is a problem with in rollback. " + e.getLocalizedMessage(), 
							LogLevel.Debug);
				}finally{
					connection.close();
					this.activeConnections.remove(connection);
				}
			}
		} catch (SQLException e) {
			Logger.log("Could not close the connection: " + connection + " " + e.getLocalizedMessage(), LogLevel.Error);
		}
	}
	
	/**
	 * Closes the given statement
	 * 
	 * @param statement
	 */
	public synchronized void closeStatement(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
				this.activeStatements.remove(statement);
			} catch (SQLException e) {
				Logger.log("Could not close the statement: " + e.getLocalizedMessage(), 
						LogLevel.Warning);
			}
		}
	}

	/**
	 * Closes all active connections
	 */
	public synchronized void terminate() {
		for (Statement stmt : this.activeStatements) {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				Logger.log("Could not close the statement: " + e.getLocalizedMessage(), 
						LogLevel.Warning);
			}
		}
		for (Connection con : this.activeConnections) {
			try {
				if (con != null && con.isClosed() != true) {
					try {
						con.rollback();
						con.close();
					} catch (SQLException e) {
						Logger.log("There is a problem in the rollback. " + e.getLocalizedMessage(), 
								LogLevel.Debug);
					}
				}
			} catch (SQLException e) {
				Logger.log("Could not close the connection: " + e.getLocalizedMessage(), LogLevel.Warning);
			}			
		}
		this.activeStatements.clear();
		this.activeConnections.clear();
	}
	
	/**
	 * Returns the number of currently active connections
	 * 
	 * @return number of currently active connections
	 */
	public int getNumberOfActiveConnections() {
		return this.activeConnections.size();
	}
	
	/**
	 * Returns the number of currently active statements
	 * 
	 * @return number of currently active statements
	 */
	public int getNumberOfActiveStatements(){
		return this.activeStatements.size();
	}
}
