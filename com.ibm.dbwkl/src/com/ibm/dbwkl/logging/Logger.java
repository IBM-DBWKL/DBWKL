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
package com.ibm.dbwkl.logging;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.ibm.dbwkl.helper.DateUtility;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.LoggerEntry;
import com.ibm.dbwkl.logging.helper.LogHelper;
import com.ibm.dbwkl.logging.output.HTMLLogger;
import com.ibm.dbwkl.logging.output.TextLogger;
import com.ibm.dbwkl.rmi.LoggerRemoteInterface;

/**
 * <p>This class provides a logging mechanism for the application. You can register a various loggers 
 * that implements the ILoggerService interface. each one would be called by this class.</p>
 * 
 * <p>Also, this class provides functions that could log exceptions or informations in a standard 
 * "<i>layout</i>".</p>
 * 
 * <p><b>Naming convention:</b> All methods names must start with <code>log</code>. This makes it 
 * easily possible to find the correct stack element, namely the first one which method doesn't 
 * start with "log...".</p> 
 * 
 */
public class Logger implements LoggerRemoteInterface {
	
	/**
	 * The list of dynamic registered loggers
	 */
	public static final List<ILoggerService> registeredLoggers = Collections.synchronizedList(new ArrayList<ILoggerService>());

	/**
	 * This is a back-door-logger, for example for JUnit tests. It is not in the regular list,
	 * and it is called in any case.
	 */
	private static ILoggerService backDoorLogger = null;
	
	/**
	 * This log-level logs everything. It is the lowest level of all.
	 */
	public static final LogLevel Debug = LogLevel.Debug;
	
	/**
	 * This is the default logging level. It logs useful information for the user.
	 */
	public static final LogLevel Info = LogLevel.Info;
	
	/**
	 * This level logs warnings, like exceptions.
	 */
	public static final LogLevel Warning = LogLevel.Warning;
	
	/**
	 * An error is a fatal failure. After an error, a workload or the whole service could stop.
	 */
	public static final LogLevel Error = LogLevel.Error;
	/**
	 * Activates a detailed logging with stack trace information.
	 */
	public static boolean details = false;
	
	/**
	 * The current log level.
	 */
	public static LogLevel logLevel = Logger.Debug;

	/**
	 * A store, to hold the log entries
	 */
	private static LoggerStore store = new LoggerStore();
	
	/**
	 * Store the old date to compare against to detect a day switch
	 */
	private static Date oldDate = null;
	
	/**
	 * blocks logging in case variable is true. This prevents infinite loops in case someone tries to call the Logger.log()
	 * method during the init phase.
	 */
	private static Boolean blockLog = Boolean.FALSE;
	
	/**
	 * Default constructor 
	 */
	public Logger() {
		//
	}
	
	/**
	 * <p>This method will add a new ILoggerService object to the list of loggers. If the logger 
	 * is already registered, this method will <i>not</i> add the logger again. To control this, 
	 * it will compare the name of the new logger to the loggers in the list.</p>
	 * 
	 * @param logger to add
	 */
	public static void addLogger(ILoggerService logger) {
		
		if(!isLoggerInTheList(logger.getName()))
			Logger.registeredLoggers.add(logger);
	}

	/**
	 * Checks whether the specified logger is in the list of registered loggers
	 * 
	 * @param logger to look up in the list of registered loggers
	 * @return true if there is already a logger with the same name
	 */
	protected static boolean isLoggerInTheList(String logger) {

		List<ILoggerService> tmp = getCopyOfLoggers();
		
		for(ILoggerService logger1: tmp){
			
			if(logger1.getName().equalsIgnoreCase(logger))
				return true;
		}
		
		return false;
	}
	
	/**
	 * <p>This method will log the specified message in each registered logger. If the detailed
	 * logging is activated, it will also create a StackTrace.</p>
	 * 
	 * @param message message to log
	 * @param level level of the message
	 */
//	private void log2(String message, LogLevel level) {
//
//		// log only if the log level is at least the log level in the settings
//		if (this._logLevel.getLevel() <= level.getLevel()) {
//			Logger.logInternal(message, /*ARequest.getRequestName()*/"", level, this._details);
//		}
//
//	}

	/**
	 * <p>This method will log the specified message in each registered logger. If the detailed
	 * logging is activated, it will also create a StackTrace.</p>
	 * 
	 * @param message to log
	 * @param level of the message (e.g. STAFLog.Error)
	 */
	public static void log(String message, LogLevel level) {
		if (logLevel.getLevel() <= level.getLevel()) {
			Logger.logInternal(message, "", level, details);
		}
	}
	
	/**
	 * this logs an by the LiveLogger received LoggerEntry to all local loggers
	 * @param entry as the unmarshalled LoggerEntry instance given by the LiveLogger
	 */
	public static void log(LoggerEntry entry) {
		
		//it is necessary to lock this code passage in case the init() method is called. Otherwise we risk an
		//infinite loop. In case the blockLog variable is true, the log message will not be written.
		if (blockLog == Boolean.FALSE) {
			DateUtility dateCompare = new DateUtility();
		
			//fetch current date	
			Date currentDate = new Date();
			
			//in case of first code iteration and in case a date has switched, init() methods will be called
			if (oldDate == null || dateCompare.compare(currentDate, oldDate) > 0) { 
				oldDate = new Date();
				
				for (int i = 0; i < registeredLoggers.size(); i++){
					ILoggerService loggerService = registeredLoggers.get(i);
					
					//this block should be synced. If not, there is a chance that a infinite loop occurs
					//since the log() method is called during the init phase.
					synchronized (blockLog) {
						blockLog = Boolean.TRUE;
						try {
							loggerService.init();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						blockLog = Boolean.FALSE;
					}
				}
				
			}
			
			/* add LoggerEntry to the LoggerEntry store because this is a newly received
			 unregistered instance that was created by an external LoggerService */
			store.addEntry(entry);
			// we copy the list to prevent a ConcurrentModifikationException
			ArrayList<ILoggerService> copy = getCopyOfLoggers();
			
			for(ILoggerService logger: copy)
				logger.log(entry);
			
			copy = null;
			
			if(backDoorLogger != null)
				backDoorLogger.log(entry);
		}
	}
	
	/**
	 * Logs a message, independent of whether it is a global or a local (request based)
	 * message.
	 * 
	 * @param message message to log
	 * @param requestName request name
	 * @param level log level
	 * @param detailsInternal details on or off
	 */
	private static void logInternal(String message, String requestName, LogLevel level, boolean detailsInternal) {
		
		LoggerEntry entry = new LoggerEntry(requestName, message, level, "", "", 0, "", "");
		
		// if a detailed logging is activated, we get the class name, method name...
		if(detailsInternal){
			
			StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();

			// we go through the whole stack
			// we start by element [1] (not [0]) because [0] is 'getStackTrace()'
			for(int i = 1; i < currentStack.length; i++){

				String methodNameTmp = currentStack[i].getMethodName();

				// we take the first element, that doesn't start with "log" (NAMING CONVENTION!)
				if(!methodNameTmp.substring(0, 3).equals("log") && !methodNameTmp.equals("getStackTrace")) {
					
					entry.setClasse(currentStack[i].getClassName());
					entry.setMethod(currentStack[i].getMethodName());
					entry.setLineNumber(currentStack[i].getLineNumber());
					break; // we have it, so we break
				}
			}
		}
		
		// set thread information because the workload id is in there
		entry.setThread(Thread.currentThread().getName());
		entry.setThreadGroup(Thread.currentThread().getThreadGroup().getName());

		log(entry);
	}
	
	/**
	 * <p>This method will remove the logger with the specified name. If no logger with this 
	 * name is found, the method will do nothing.</p>
	 * 
	 * @param loggerName of the logger to remove
	 */
	protected static void removeLogger(String loggerName)
	{
		// we copy the list to prevent a ConcurrentModifikationException
		ArrayList<ILoggerService> copy = getCopyOfLoggers();
		
		for(int i = 0; i < copy.size(); i++) {
			
			if(copy.get(i).getName().equalsIgnoreCase(loggerName)) {
				
				registeredLoggers.remove(i);
				break;
			}
		}
	}

	/**
	 * <p>This method will configure the Logger with default settings.</p>
	 * @throws IOException 
	 */
	public static void init() throws IOException {
		
		details = false;
		logLevel = Logger.Info;
		
		addLogger(new TextLogger());
		addLogger(new HTMLLogger());
	}

	/**
	 * @return a copy of the current loggers
	 */
	public static ArrayList<ILoggerService> getCopyOfLoggers() {
		return new ArrayList<ILoggerService>(registeredLoggers);
	}
	
	/**
	 * @return a copy of the current loggers
	 */
/*	public static ArrayList<ILoggerService> getCopyOfRequestLoggers() {
		return new ArrayList<ILoggerService>(ARequest.getLogger().requestLoggers);
	}
	*/
	/**
	 * @return the details boolean
	 */
	public static boolean isDetailed() {
		
		return details;
	}

	/**
	 * @param level as integer
	 * @return the matching log level object to the integer level
	 */
	public static LogLevel getLogLevelFor(int level) {

		return LogLevel.getLevel(level);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		
		String state = "DB2WorkloadService log \n\n";
		
		StringBuilder builder = new StringBuilder(state);
		
		ArrayList<ILoggerService> copy = getCopyOfLoggers();
		
		for(ILoggerService logger: copy)
			builder.append("\t" + "Registered logger: " + logger.getName() + "\n");
		
		builder.append("\n");
		builder.append("You can see the detailed status of each logger object, by typing 'STATUS name'\n");
		builder.append("You can remove each logger object, by typing 'REMOVELOG name'\n");
		builder.append("You can add a new logger object, by typing 'ADDLOG name'\n");
		builder.append("The currently available loggers are 'DatabaseLogger, LiveLogger, LiveLogger, STAFLogger'\n");
		
		return builder.toString();
	}

	/**
	 * @see com.ibm.dbwkl.logging.helper.LogHelper#logNewSession()
	 */
	public static void logNewSession() {
		
		LogHelper.logNewSession();
	}
	
	/**
	 * @param connection to log
	 * @see com.ibm.dbwkl.logging.helper.LogHelper#logConnectionInformation(Connection)
	 */
	public static void logConnectionInformation(Connection connection) {

		LogHelper.logConnectionInformation(connection);
	}

	/**
	 * @param exception to log
	 * @param message of the user to log also
	 * @see com.ibm.dbwkl.logging.helper.LogHelper#logSQLException(SQLException, String)
	 */
	public static void logSQLException(SQLException exception, String message) {
		
		LogHelper.logSQLException(exception, message);
	}
	
	/**
	 * @param exception to log
	 * @see com.ibm.dbwkl.logging.helper.LogHelper#logException(Exception)
	 */
	public static void logException(Exception exception) {
		
		LogHelper.logException(exception);
	}

	/**
	 * This method will call the term()-method of all registered loggers. So, this
	 * logger could close resources and so on.
	 */
	public static void term() {

		ArrayList<ILoggerService> copy = getCopyOfLoggers();
		for(ILoggerService logger: copy)
			logger.term();	
	}

	/**
	 * A back door logger is a separate logger, that is not registered in the regular
	 * list. It could be used for JUnit test, because it is not in any tested list.
	 * 
	 * @param backDoorLogger to set
	 */
	public static void setBackDoorLogger(ILoggerService backDoorLogger) {

		Logger.backDoorLogger = backDoorLogger;
	}

//	/**
//	 * @param _logLevel the _logLevel to set
//	 */
//	public void setLogLevel(LogLevel _logLevel) {
//		this._logLevel = _logLevel;
//	}
//
//	/**
//	 * @return the _logLevel
//	 */
//	public LogLevel getLogLevel() {
//		return this._logLevel;
//	}
//
//	/**
//	 * @param _details the _details to set
//	 */
//	public void setDetails(boolean _details) {
//		this._details = _details;
//	}
//
//	/**
//	 * @return the _details
//	 */
//	public boolean isDetails() {
//		return this._details;
//	}
	
	/**
	 * @return the stored log entries
	 */
	public static ArrayList<LoggerEntry> getLogEntries() {

		return store.getEntries();
	}
	
	/**
	 * @param n if n > 0 : number of last log entries if n <= 0 : all the log entries
	 * @return the stored log entries
	 */
	public static List<LoggerEntry> getLogEntries(int n) {
		
		if (n > 0)
			return store.getEntries(n);
		else
			return store.getEntries();
	}
	
	/**
	 * @param n if n > 0 : number of last log entries if n <= 0 : all the log entries
	 * @param levels the levels to filter on
	 * @return the last n log entries filtered with levels
	 */
	public static List<LoggerEntry> getLogEntries(int n, String[] levels) {

		return store.getEntries(n, levels);
	}

	/**
	 * 
	 * @param n if n > 0 : number of last log entries if n <= 0 : all the log entries
	 * @param levels the levels to filter on
	 * @param reqid the request ID to filter on
	 * @return the last n log entries filtered with levels and reqid
	 */
	public static List<LoggerEntry> getLogEntries(int n, String[] levels,
			int reqid) {
		return store.getEntries(n, levels, reqid);
	}

	/**
	 * 
	 * @param n if n > 0 : number of last log entries if n <= 0 : all the log entries
	 * @param reqid the request ID to filter on
	 * @return the last n log entries filtered with reqid
	 */
	public static List<LoggerEntry> getLogEntries(int n, int reqid) {
		return store.getEntries(n, reqid);
	}

	/**
	 * @return the last log entry
	 */
	public static LoggerEntry getLastLogEntry() {
		return store.getLastLogEntry();
	}
	
//	/**
//	 * @param requestSpecificLogger as the logger within a request
//	 * @return boolean true if logger was successfully added
//	 */
//	public boolean addRequestSpecificLogger(ILoggerService requestSpecificLogger) {
//		return this.requestLoggers.add(requestSpecificLogger);
//	}
//	
//	/**
//	 * Terminates the request specific loggers
//	 */
//	public void termRequestSpecificLoggers() {
//		for(ILoggerService requestSpecificLogger:this.requestLoggers) {
//			requestSpecificLogger.term();
//		}
//		this.requestLoggers.clear();
//	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.rmi.LoggerRemoteInterface#Log(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, java.lang.String, java.lang.String)
	 */
	@Override
	public void Log(String message, String requestName, String level, String classe, 
			        String method, int lineNumber, String thread, String threadGroup) throws RemoteException {
		LoggerEntry entry = new LoggerEntry(requestName, message, LogLevel.getLevel(level), classe, 
				                            method, lineNumber, thread, threadGroup);
		log(entry);
	}

	/**
	 * @param message
	 * @param requestName
	 * @param level
	 * @param classe
	 * @param method
	 * @param lineNumber
	 * @param thread
	 * @param threadGroup
	 */
	public static void log(String message, String requestName, String level, String classe, 
			               String method, int lineNumber, String thread, String threadGroup) {
		LoggerEntry entry = new LoggerEntry(requestName, message, LogLevel.getLevel(level), classe, 
                                            method, lineNumber, thread, threadGroup);
		log(entry);
	}
}