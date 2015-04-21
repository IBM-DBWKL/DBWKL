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
package com.ibm.db2wkl.logging.helper;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import com.ibm.db2wkl.logging.Logger;

/**
 * This class provides helper methods for the logging. For example,
 * it can log exceptions in a default format.
 * 
 */
public class LogHelper {

	/**
	 * <p>This method logs a general Exception in a standard "<i>layout</i>":</p>
	 * 
	 * <p><code>
	 * Exception: message: ...<br/>
	 * Exception: localized message: ...<br/>
	 * Exception: stack element: ...<br/>
	 * ...
	 * </code></p>
	 * 
	 * @param exception to log
	 */
	public static void logException(Exception exception) {
		
		if(exception == null)
			return;
		
		String className = exception.getClass().getSimpleName();
		String message = exception.getMessage();
		String localizedMessage = exception.getLocalizedMessage();

		Logger.log(className + ": message: " + message, Logger.Error);
		Logger.log(className + ": localized message: " + localizedMessage, Logger.Debug);
		
		for(StackTraceElement element: exception.getStackTrace())
			Logger.log(className + ": stack element: " + element.getMethodName() + " (" + element.getLineNumber() + ")", Logger.Debug);
	}
	
	/**
	 * <p>This method logs a SQLException in a standard "<i>layout</i>":</p>
	 * 
	 * <p><code>
	 * SQLException: user information: ...<br/>
	 * SQLException: message: ...<br/>
	 * SQLException: error code: ...<br/>
	 * SQLException: SQL state: ...<br/>
	 * SQLException: Reason: ...<br/>
	 * </code></p>
	 * 
	 * @param exception to log
	 * @param message for the "<i>user information</i>" field
	 */
	public static void logSQLException(SQLException exception, String message) {
		
		if(exception == null)
			return;
		
		logException(exception);
			
		Logger.log("SQLException: user information: " + message, Logger.Warning);
		Logger.log("SQLException: message: " + exception.getMessage(), Logger.Warning);
		Logger.log("SQLException: error code: " + exception.getErrorCode(), Logger.Warning);
		Logger.log("SQLException: SQL state: " + exception.getSQLState(), Logger.Warning);
		Logger.log("SQLException: Reason: " + SQLErrorCodeToMessageConverter.getMessage(exception.getErrorCode()), Logger.Warning);
		Logger.log("SQLException: Localized Message: " + exception.getLocalizedMessage(), Logger.Warning);
	}
	
	/**
	 * <p>This method logs meta data informations about the
	 * connection (e.g. driver version or database version)
	 * in a standard "<i>layout</i>":</p>
	 * 
	 * <p><code>
	 * Connection information: URL: ...<br/>
	 * Connection information: User name: ...<br/>
	 * Connection information: Driver Name: ...<br/>
	 * Connection information: Driver Version: ...<br/>
	 * Connection information: DB2 product name: ...<br/>
	 * Connection information: DB2 version: ...<br/>
	 * </code></p>
	 * 
	 * @param connection to log
	 */
	public static void logConnectionInformation(Connection connection) {
		
		try {
			
			DatabaseMetaData dmd = connection.getMetaData();

			Logger.log("New connection to " + dmd.getURL() + " have been created", Logger.Info);
			
			Logger.log("Database is available.", Logger.Debug);
			Logger.log("Connection information: URL: " + dmd.getURL(),Logger.Debug);
			Logger.log("Connection information: User name: " + dmd.getUserName(), Logger.Debug);
			Logger.log("Connection information: Driver Name: " + dmd.getDriverName(), Logger.Debug);
			Logger.log("Connection information: Driver Version: " + dmd.getDriverVersion(), Logger.Debug);
			Logger.log("Connection information: DB2 product name: " + dmd.getDatabaseProductName(), Logger.Debug);
			Logger.log("Connection information: DB2 version: " + dmd.getDatabaseMajorVersion() + "." + dmd.getDatabaseMinorVersion(), Logger.Debug);
		} 
		catch (SQLException e) {
			
			logSQLException(e, "SQLException while logging connection meta data");
		}
	}
	
	/**
	 * This method logs a new session header
	 */
	public static void logNewSession() {
		
		Logger.log("NEW SESSION: new session started", Logger.Info);
		Logger.log("NEW SESSION: at " + new Date().toString(), Logger.Info);
		logJavaInformation();
	}
	
	/**
	 * This method logs information about the current Java environment.
	 */
	private static void logJavaInformation() {
		
		Properties system = System.getProperties();
		
		Logger.log("JAVA: JRE version: " + system.getProperty("java.version"), Logger.Debug);
		Logger.log("JAVA: Java vendor: " + system.getProperty("java.vendor"), Logger.Debug);
		Logger.log("JAVA: Installation directory: " + system.getProperty("java.home"), Logger.Debug);
		Logger.log("JAVA: VM name: " + system.getProperty("java.vm.name"), Logger.Debug);
		Logger.log("JAVA: VM version: " + system.getProperty("java.vm.version"), Logger.Debug);
		Logger.log("JAVA: Class path: " + system.getProperty("java.class.path"), Logger.Debug);
	}
}
