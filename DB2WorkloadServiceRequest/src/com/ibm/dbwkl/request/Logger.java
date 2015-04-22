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
package com.ibm.dbwkl.request;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.SQLException;

import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.helper.SQLErrorCodeToMessageConverter;
import com.ibm.dbwkl.rmi.LoggerRemoteInterface;

/**
 * Within a request running in a separate JVM other than DB2WKL, 
 * 
 * this Logger is used to send the logging entry through RMI to the DB2WKL JVM, where the entry will be stored and output.
 * 
 *
 */
public class Logger {

	/**
	 * debug log level
	 */
	public static final LogLevel Debug = LogLevel.Debug;


	/**
	 * warning log level
	 */
	public static final LogLevel Warning = LogLevel.Warning;


	/**
	 * info log level
	 */
	public static final LogLevel Info = LogLevel.Info;


	/**
	 * error log level
	 */
	public static final LogLevel Error = LogLevel.Error;
	
	
	/**
	 * logger stub
	 */
	private static LoggerRemoteInterface logStub;
	
	/**
	 * @param host
	 * @param port
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws AccessException 
	 */
	public static void init(String host, int port) throws AccessException, RemoteException, NotBoundException {
		Logger.logStub = (LoggerRemoteInterface) RequestPerformer.getStub(host, port, LoggerRemoteInterface.BIND_NAME);
	}
	
	/**
	 * @param message
	 * @param level
	 */
	public static void log(String message, LogLevel level){
		String classe = "";
		String method = "";
		int lineNumber = 0;
		if(level.getLevel() >= Request.getLogLevel().getLevel()){
		// if a detailed logging is activated, we get the class name, method name...
			if(Request.isLogDetails()){
				StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
				// we go through the whole stack
				// we start by element [1] (not [0]) because [0] is 'getStackTrace()'
				for(int i = 1; i < currentStack.length; i++){
		
					String methodNameTmp = currentStack[i].getMethodName();
					// we take the first element, that doesn't start with "log" (NAMING CONVENTION!)
					if(!methodNameTmp.substring(0, 3).equals("log") && !methodNameTmp.equals("getStackTrace")) {
						classe = currentStack[i].getClassName();
						method = currentStack[i].getMethodName();
						lineNumber = currentStack[i].getLineNumber();
						break; // we have it, so we break
					}
				}
			}
			
			if(logStub != null){
				try {
					logStub.Log(message, Request.getRequestName(), level.toString(), classe, method, lineNumber, Thread.currentThread().getName(), Thread.currentThread().getThreadGroup().getName());
				} catch (RemoteException e) {
					e.printStackTrace(System.err);
					System.exit(1);
				}
			} else {
				throw new LoggedRuntimeException("Logger's stub is not initilized.");
			}
		}
	}

	/**
	 * @param exception 
	 */
	public static void logException(Exception exception) {
		if(exception == null)
			return;
			
		String className = exception.getClass().getSimpleName();
		String message = exception.getMessage();
		String localizedMessage = exception.getLocalizedMessage();

		log(className + ": message: " + message, LogLevel.Error);
		log(className + ": localized message: " + localizedMessage, LogLevel.Debug);
			
		for(StackTraceElement element: exception.getStackTrace())
			log(className + ": stack element: " + element.getMethodName() + " (" + element.getLineNumber() + ")", LogLevel.Debug);
		}

	/**
	 * @param exception 
	 * @param message 
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
}
