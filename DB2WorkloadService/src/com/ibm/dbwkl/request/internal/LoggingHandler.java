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
package com.ibm.dbwkl.request.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.dbwkl.helper.StringUtility;
import com.ibm.dbwkl.helper.xml.XMLSerializer;
import com.ibm.dbwkl.logging.ILoggerService;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.Logger;
import com.ibm.dbwkl.logging.LoggerEntry;
import com.ibm.dbwkl.logging.output.DatabaseLogger;
import com.ibm.dbwkl.logging.output.HTMLLogger;
import com.ibm.dbwkl.logging.output.LiveLogger;
import com.ibm.dbwkl.logging.output.TextLogger;
import com.ibm.dbwkl.request.parser.Options;
import com.ibm.staf.STAFResult;

/**
 * This controller provides access to the log component for both, global settings
 * and local settings (within request)
 * 
 * Stephan Arenswald (arens@de.ibm.com)
 *
 */
public class LoggingHandler extends InternalRequest {

	@Override
	public STAFResult execute() {
		StringBuilder resultString = new StringBuilder();
			
		// user for db logger
		String logdbuser = getRequestOption(Options.DB_USER);
			
		// password for db logger
		String logdbpassword = getRequestOption(Options.DB_PASSWORD);
			
		// URL for db logger
		String logdburl = getRequestOption(Options.DB_URL);
		
		if(hasRequestOption(Options.LOG_LOGTEST)) {
			for(int i=0;i<=25;i++) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					//ignore
				}
				Logger.log("LOGTEST:" + i, LogLevel.Debug);
			}
			return new STAFResult(STAFResult.Ok, "Logtest at Loglevel Debug executed!");
		}
			
		/*
		 * everything went fine, thus do the changes and hope that all values are allowed
		 */
			
		// level
		if (hasRequestOption(Options.LOG_LEVEL)) {
			String level = getRequestOption(Options.LOG_LEVEL);
			try {
				Logger.logLevel = LogLevel.getLevel(level);
				resultString.append("Log Level set to " + level + "\n");
			} catch (Exception e) {
				return new STAFResult(STAFResult.InvalidValue, "The value '" + level + "' is not allowed as log level. Use one of the following instead: Debug Info Warning Error");
			}
		}
			
		// details
		if (hasRequestOption(Options.LOG_DETAILS)) {
			String details = getRequestOption(Options.LOG_DETAILS);
			try {
				Logger.details = new Boolean(details).booleanValue();
				resultString.append("Log Details now " + (Logger.details == true ? "ON" : "OFF") + "\n");
			} catch (Exception e) {
				return new STAFResult(STAFResult.InvalidValue, "The value '" + details + "' is not allowed as detailed logging settings. Use either true or false as value.");
			}
		}
			
		// Cleaning the logger
		if(hasRequestOption(Options.LOG_CLEAN)) {
			String clean = getRequestOption(Options.LOG_CLEAN).trim();
			
			try {
				ILoggerService logger = getLoggerByName(clean);
				if(logger == null) 
					return new STAFResult(STAFResult.DoesNotExist, "Logger could not be found!");
				
				logger.term();
				logger.clean();
				Logger.registeredLoggers.remove(logger);
				
				logger = logger.getClass().newInstance();
				Logger.addLogger(logger);
				
				resultString.append("Logger " + clean + " has been cleaned!");
				
			} catch (Exception e) {
				return new STAFResult(STAFResult.InvalidValue, "The logger '" + clean + "' is unknown. Use one of the following: htmllogger filelogger dblogger sysout livelog livelogreceiver");
			}
		}
		
		// remove logger
		else if(hasRequestOption(Options.LOG_REMOVE)) {
			String remove = getRequestOption(Options.LOG_REMOVE).trim();
			
			try {	
				if(remove.equalsIgnoreCase("LIVELOG")) {
					LiveLogger.getInstance().term();
					Logger.registeredLoggers.remove(LiveLogger.getInstance());
					resultString.append("Logger " + remove + " has been removed \n");
				}
				else {
					ILoggerService logger = getLoggerByName(remove);
					
					if(logger == null) 
						return new STAFResult(STAFResult.DoesNotExist, "Logger could not be found!");
					
					logger.term();
					Logger.registeredLoggers.remove(logger);
					
					resultString.append("Logger has been removed!");
				}
					
			} catch (Exception e) {
				return new STAFResult(STAFResult.InvalidValue, "The logger '" + remove + "' to be removed is unknown.");
			}
		}
				
			// add logger
		else if(hasRequestOption(Options.LOG_ADD)) {
			String add = getRequestOption(Options.LOG_ADD);
			
					
			if(add.equalsIgnoreCase("FILELOGGER"))
				try {
					Logger.addLogger(new TextLogger());
				} catch (IOException e1) {
					return new STAFResult(STAFResult.JavaError, "Exception occurs while adding a FileLogger.");
				}
			else if(add.equalsIgnoreCase("DBLOGGER")) {
				try {
					Logger.addLogger(new DatabaseLogger(logdbuser, logdbpassword, logdburl));
				} catch (IOException e) {
					return new STAFResult(STAFResult.JavaError, "Exception occurs while adding a DBLogger: " + e.getMessage());				
				} catch (InterruptedException e) {
					//
				}
			}
						
			else if(add.equalsIgnoreCase("HTMLLOGGER"))
				try {
					Logger.addLogger(new HTMLLogger());
				} catch (IOException e) {
					return new STAFResult(STAFResult.JavaError, "Exception occurs while adding a HTMLLogger.");
				}
			else if(add.equalsIgnoreCase("LIVELOG")) {
				if(hasRequestOption(Options.SOCKET_HOST)) {
					if(getRequestOption(Options.SOCKET_HOST).length()!=0) {
						LiveLogger liveLogger = LiveLogger.getInstance();
						if(!Logger.getCopyOfLoggers().contains(liveLogger)) Logger.addLogger(liveLogger);
						liveLogger.openServer(getRequestOption(Options.SOCKET_HOST).trim());
						return new STAFResult(STAFResult.Ok,"LiveLogger added, it will now accept a live logging connection!");
					}
				}
				Logger.log("Adding LiveLog not possible: Invalid Host", LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, "Adding LiveLog not possible: Invalid Host");
			}
					
			else {
				Logger.log("Adding Logger failed: no such logger to add found: " + add, LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, "Adding Logger failed: no such logger to add found: " + add);
			}
			resultString.append("Logger " + add + " has been added \n");
		}
			
		else if(hasRequestOption(Options.LOG_SCHEMA)) {
			String xsd = null;
			try {
				xsd = XMLSerializer.createXMLSchema(LoggerEntry.class);
			} catch (IOException e) {
				Logger.log(e.getMessage(), LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, e.getMessage());
			} catch (JAXBException e) {
				Logger.log(e.getMessage(), LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, e.getMessage());
			}
			
			if(xsd!=null) return new STAFResult(STAFResult.Ok, xsd);
			
			return new STAFResult(STAFResult.JavaError, "Error while creating the Schema");
		}
		
		// LIST shows logs on the console
		// schema:
		//		LIST <what> [FILTER <filter>] [COLUMNS <columns>]
		//			 <what> = { , n} (empty=all, n=last n)
		//			 <filter> = {LEVEL={DEBUG, INFO, WARNING, ERROR}*;REQID=<reqid>}
		//			 <columns> = {REQUEST, THREAD, LOGLEVEL, MESSAGE}
		else if (hasRequestOption(Options.LOG_LIST)) {
			//read <what>
			String listValue = getRequestOption(Options.LOG_LIST);
			//the number of latest entries to retrieve
			int n = 0;
			if (listValue.equalsIgnoreCase("")){
				//when n<=0, all entries are retrieved
				n = 0;
			} else {
				try {
					n = Integer.parseInt(listValue);
				} catch (NumberFormatException e) {
					return new STAFResult(STAFResult.InvalidValue, "List option needs to be a positive number or empty");
				}
				if(n <= 0){
					return new STAFResult(STAFResult.InvalidValue, "List option needs to be a positive number or empty");
				}
			}

			//read <filter>
			String levels[] = null;
			int reqid = 0;
			if (hasRequestOption(Options.LOG_LIST_FILTER)) {
				String filter = getRequestOption(Options.LOG_LIST_FILTER).toLowerCase();
				StringTokenizer tokenizer = new StringTokenizer(filter, ";");
				while (tokenizer.hasMoreElements()) {
					String option = (String) tokenizer.nextElement();
					// an option is of the form key=value
					if(option.contains("=")){
						String key = option.substring(0, option.indexOf("="));
						//read LEVEL={DEBUG,INFO,WARNING,ERROR}*
						if (key.equalsIgnoreCase("level")) {
							levels = option.substring(option.indexOf("=") + 1).split(",");
							//Check if the given levels are supported.
							if(!StringUtility.arrayContainsOnlyIgnoreCase(levels, 
																			LogLevel.Debug.toString().trim(),
																			LogLevel.Warning.toString().trim(),
																			LogLevel.Error.toString().trim(),
																			LogLevel.Info.toString().trim())){
								return new STAFResult(STAFResult.InvalidValue,
														"Supporting only Log Levels: " + 
														LogLevel.Debug.toString().trim() + "," + 
														LogLevel.Warning.toString().trim() + "," + 
														LogLevel.Error.toString().trim() + "," + 
														LogLevel.Info.toString().trim());
							}
						//read REQID=<reqid>		
						} else if (key.equals("reqid")) {
							try {
								reqid = Integer.parseInt(option.substring(option.indexOf("=") + 1));
							} catch (NumberFormatException e) {
								return new STAFResult(STAFResult.InvalidValue, "REQID needs to be a positive number.");
							}
							if(reqid <= 0){
								return new STAFResult(STAFResult.InvalidValue, "REQID needs to be a positive number.");
							}
						} else {
							//option is not level= or reqid=
							return new STAFResult(STAFResult.InvalidValue, "Invalid option in the filter: " + option);
						}
					} else {
						//option doesn't have '='
						return new STAFResult(STAFResult.InvalidValue, "Invalid requestOptions in the filter: " + option);
					}
				}
			}
			
			//read <columns>
			String[] cols = null;
			if (hasRequestOption(Options.LOG_LIST_COL)) {
				cols = getRequestOption(Options.LOG_LIST_COL).toLowerCase().split(",");
				if(!StringUtility.arrayContainsOnlyIgnoreCase(cols, "request", "thread", "level", "message"))
					return new STAFResult(STAFResult.InvalidValue, "Supporting only Columns: Request, Thread, Level, Message.");
			}
			

			// filter the entries accordingly
			List<LoggerEntry> logEntries = null;

			if (levels == null || levels.length == 0) {
				if (reqid == 0) {
					logEntries = Logger.getLogEntries(n);
				} else {
					logEntries = Logger.getLogEntries(n, reqid);
				}
			} else {
				if (reqid == 0) {
					logEntries = Logger.getLogEntries(n, levels);
				} else {
					logEntries = Logger.getLogEntries(n, levels, reqid);
				}
			}

			// print the lines, with selected columns.
			String ALLCOLUMNS[] = {"Request", "Thread", "Level", "Message" }; 
			String formats[] = { "%1$-8s", " %2$-12s", " %3$-7s", " %4$-100s" };
			
			//if no columns is set, display all columns
			if (cols == null || cols.length == 0) {
				cols = ALLCOLUMNS;	
			}
			
			
			//check is one of the four columns exists, and construct the format string.
			boolean[] colExists = new boolean[ALLCOLUMNS.length];
			String format = "";
			
			for(int i = 0; i < ALLCOLUMNS.length; i++){
				if(StringUtility.arrayContainsIgnoreCase(cols, ALLCOLUMNS[i])){
					colExists[i] = true;
					format += formats[i];
				}
			}
			format += "\n";
			
			//print the table head
			resultString.append(String.format(format, 
								colExists[0] ? "Request" : "", 
								colExists[1] ? "Thread" : "", 
								colExists[2] ? "Level" : "",
								colExists[3] ? "Message" : ""));
			
			resultString.append(String.format(format, 
								colExists[0] ? "-------" : "", 
								colExists[1] ? "------" : "", 
								colExists[2] ? "-----" : "",
								colExists[3] ? "-------" : ""));
			
			//print the log entries
			for (LoggerEntry logEntry : logEntries) {

				if(colExists[3]){
					//number of chars per row in the message column
					int charsPerRow = 100;
					//in case the message contains multiple lines.
					String msg = logEntry.getMessage();
					String[] lines = msg.split("\\n");
					for(int i = 0; i < lines.length; i++){
						//other columns should only appear once on the first row.
						String formatedString = String.format(format,
												(i == 0 && colExists[0]) ? logEntry.getRequestName() : "",
												(i == 0 && colExists[1]) ? logEntry.getThread() : "",
												(i == 0 && colExists[2]) ? logEntry.getLevel() : "",
												StringUtils.left(lines[i], charsPerRow));
						resultString.append(formatedString);
						
						//if the line is longer than 'charsPerRow', split it into multiple rows
						if (lines[i].length() > charsPerRow) {
							//cut every 'charsPerRow' chars out, and put it in a new row.
							String txt = "";
							int j = 1;
							while ((txt = StringUtils.mid(lines[i], charsPerRow * j, charsPerRow)).length() != 0) {
									resultString.append(String.format(format, "", "", "", txt));
									j++;
							}
						}
					}
				} else {
					String formatedString = String.format(format,
											colExists[0] ? logEntry.getRequestName() : "",
											colExists[1] ? logEntry.getThread() : "",
											colExists[2] ? logEntry.getLevel() : "",
											"");
					resultString.append(formatedString);
				}
			}
			
		}
		
		return new STAFResult(STAFResult.Ok, resultString.toString());
	}


	/**
	 * @return xml output
	 */
	@Override
	public Document getXML() {
		if(hasRequestOption(Options.LOG_LIST)) {
			try {
				List<LoggerEntry> logEntries = Logger.getLogEntries();
				
				int entriesToShow = Integer.parseInt(getRequestOption(Options.LOG_LIST));
				
				if(entriesToShow<logEntries.size()) 
					logEntries = logEntries.subList(logEntries.size()-entriesToShow, logEntries.size());
				// create the document
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.newDocument();
				
				// create the document root node
				Element root = document.createElement("db2wklresult");
				document.appendChild(root);
				
				// create the result information
				Element rc = document.createElement("rc");
				rc.setTextContent(Integer.toString(this.getResult().rc));
				root.appendChild(rc);
				
				Element object = document.createElement("resultObject");
				object.setTextContent(this.getResult().resultObj == null ? "" : this.getResult().resultObj.toString());
	
				Element entries = null;
				
				for(LoggerEntry logEntry : logEntries){
					entries = document.createElement("logEntry");
					entries.setAttribute("formattedTime", logEntry.getFormatedTime());
					entries.setAttribute("requestName", logEntry.getRequestName());
					entries.setAttribute("loglevel", logEntry.getLevel());
					entries.setAttribute("message", logEntry.getMessage().replaceAll(String.valueOf('"'), "'"));
					entries.setAttribute("class", logEntry.getClasse());
					entries.setAttribute("method", logEntry.getMethod());
					entries.setAttribute("line", String.valueOf(logEntry.getLineNumber()));
					entries.setAttribute("thread", logEntry.getThread());
					entries.setAttribute("threadGroup", logEntry.getThreadGroup());
					object.appendChild(entries);
				}
						
				root.appendChild(object);
				
				// return the resulting document
				return document;
			} catch (ParserConfigurationException e) {
				Logger.log(e.getMessage(), LogLevel.Error);
			}
		}
		
		return null;	
	}
	
	/**
	 * Searches the copy of registered Loggers for a logger with the given name
	 * @param loggerName as the type name of the logger instance like htmllogger, filelogger, livelogger...
	 * @return ILoggerService as the by name founded logger instance
	 */
	private ILoggerService getLoggerByName(String loggerName) {
		ILoggerService logger = null;
		ArrayList<ILoggerService> copy = Logger.getCopyOfLoggers();
		
		for(int i = 0; i < copy.size(); i++) {
			if(loggerName.equalsIgnoreCase(copy.get(i).getName())) {
				logger = copy.get(i);
				break;
			}
		}
		
		return logger;
	}


	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.request.internal.InternalRequest#getText()
	 */
	@Override
	protected String getText() {
		// TODO Auto-generated method stub
		return null;
	}
}
