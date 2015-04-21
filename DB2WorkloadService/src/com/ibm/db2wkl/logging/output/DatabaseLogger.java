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
package com.ibm.db2wkl.logging.output;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.ibm.db2wkl.DB2WorkloadServiceException;
import com.ibm.db2wkl.helper.DB2CommandUtility;
import com.ibm.db2wkl.helper.SimpleJDBCConnector;
import com.ibm.db2wkl.helper.URLCutter;
import com.ibm.db2wkl.logging.ILoggerService;
import com.ibm.db2wkl.logging.Logger;
import com.ibm.db2wkl.logging.LoggerEntry;

/**
 * This class writes a log to a database.
 * 
 */
public class DatabaseLogger implements ILoggerService {
	
	/**
	 * Connection to the database
	 */
	private Connection connection = null;

	/**
	 * The name of the table for the storage
	 */
	private static final String tableName = "LOGGER";
	
	/**
	 * create a table, where the log info will be stored
	 */
	private static final  String createTableStatement = "CREATE TABLE " + tableName + 
			    		"(" +
			    			"TIME VARCHAR(40), " +
			    			"LEVEL VARCHAR(10), " +
			    			"MESSAGE VARCHAR(400), " +
			    			"CLASS VARCHAR(400), " +
			    			"METHOD VARCHAR (400), " +
			    			"LINE int, " +
			    			"THREAD VARCHAR(400), " +
			    			"THREADGROUP VARCHAR(400)" +
			    		")";
	
	/**
	 * @param user for the logging database
	 * @param password for the logging database
	 * @param url to the logging database
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public DatabaseLogger(String user, String password, String url) throws IOException, InterruptedException {

		// now, we check the availability of the database
		//boolean isValid = new ConnectionValidator(ARequest.getRequest().).validateConnection(url, user, password);

		// if the connection is available, we create the logger
//		if(!isValid)
//			throw new DB2WorkloadServiceException("Cannot create database logger");
		createLocalDB2LoggingDatabase(url);
		
//		// we test the connection once again:
//		isValid = new ConnectionValidator(ARequest.getDataSource()).validateConnection(url, user, password);
//
//		// if the connection is available, we create the logger now
//		if(!isValid)
//			throw new DB2WorkloadServiceException("Local database for logging is not available, not accessible or can't be created automatically");
//	
		try {
		
		this.connection = SimpleJDBCConnector.getConnection(url, user, password);

		Statement stmt = this.connection.createStatement();

			stmt.execute(createTableStatement);

			stmt.close();
		
			this.connection.commit();
		} 
		catch (SQLException e) {

			throw new DB2WorkloadServiceException("Can't create database logger", e);
		}
	}
	
	/**
	 * @see com.ibm.db2wkl.logging.ILoggerService#log(java.lang.String, java.lang.String, int, java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.lang.String)
	 */
	@Override
	public synchronized void log(LoggerEntry entry) {
	    
		String time = entry.getFormatedTime();
		String level = entry.getLevel();
		String message = entry.getMessage();
		String classe = entry.getClasse();
		String method = entry.getMethod();
		int line = entry.getLineNumber();
		String thread = entry.getThread();
		String threadGroup = entry.getThreadGroup();
		
		//message contains illegal characters not allowed by DB2
		message = message.replace("'", "");
		
		try {			
		    String insertStatement = "INSERT INTO " + tableName + " VALUES(" +
					 "'" + time 						+ "', " +
					 "'" + level					 	+ "', " +
					 "'" + message 						+ "', " +
					 "'" + classe 						+ "', " +
					 "'" + method 						+ "', " +
					 ""  + line 						+ ",  " +
		    		 "'" + thread  						+ "', " +
		    		 "'" + threadGroup					+ "')";
		    
	    	Statement statement = this.connection.createStatement();
		    
		    statement.executeUpdate(insertStatement);

			this.connection.commit();
			statement.close();
		} 
	    catch (SQLException e) {
	    	
	    	throw new DB2WorkloadServiceException("SQLException while logging message to database", e);
	    }
	}
	
	/**
	 * @param url to the database
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	private static void createLocalDB2LoggingDatabase(String url) throws IOException, MalformedURLException, InterruptedException {

		Logger.log("Local database for logging is not available.", Logger.Warning);
		
		String local = url;
		String name = "";

		if(URLCutter.isType4URL(local))
			name = local.substring(local.indexOf("/", 12)+1, local.length());
		else if(URLCutter.isType2URL(local))
			name = local.substring(local.indexOf(":", 6)+1, local.length());
		else 
			throw new MalformedURLException(name + " is not a valid url.");
		
		(new DB2CommandUtility()).createLocalDB2Database(name);
	}
	
	/**
	 * This method is called when the garbage collector destroys the object. The connection 
	 * will be closed in this moment.
	 */
	@Override
	public void finalize() {
		
		try {
			
			this.connection.close();
		} 
		catch (SQLException e) {
			
			Logger.logSQLException(e, "While closing connection");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.logging.ILoggerService#getName()
	 */
	@Override
	public String getName() {
		
		return "DatabaseLogger";
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		
		String state = "DatabaseLogger in table " + tableName + "\n";
		
		try {
			
			state += "to database " + this.connection.getMetaData().getURL() + "\n" +
			                          this.connection.getMetaData().toString() + "\n";
		} 
		catch (SQLException e) {
			//
		}
		
		return state;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.logging.ILoggerService#term()
	 */
	@Override
	public void term() {

		try {
			
			this.connection.close();
		}
		catch(Exception e) { /**/ }
		
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.logging.ILoggerService#clean()
	 */
	@Override
	public void clean() {
		try {
			Statement stmt = this.connection.createStatement();

			stmt.execute("DROP TABLE " + tableName);

			stmt.close();
	
			this.connection.commit();
		} catch (SQLException e) {

			throw new DB2WorkloadServiceException("Can't create database logger", e);
		}
	}
	
	@Override
	public void init() {
		//		
	}
}