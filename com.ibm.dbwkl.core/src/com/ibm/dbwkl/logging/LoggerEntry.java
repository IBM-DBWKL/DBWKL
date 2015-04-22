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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * This class represents a log entry as a POJO.
 * 
 */
@XmlRootElement
public class LoggerEntry implements Serializable{

	/**
	 * auto generated
	 */
	private static final long serialVersionUID = 402979185403559949L;

	/**
	 * The global id count for logger entries
	 */
	private static int count = 0;
	
	/**
	 * The name of the request if the log message is part of a request
	 */
	private String requestName = null;
	
	/**
	 * The Time stamp of the log entry
	 */
	private Date time = null;
	
	/**
	 * The message to log
	 */
	private String message = "";
	
	/**
	 * The log level
	 */
	private String level = "";
	
	/**
	 * The class that calls the Logger
	 */
	private String classe = "";
	
	/**
	 * The method that calls the logger
	 */
	private String method = "";
	
	/**
	 * The thread that calls the logger
	 */
	private String thread = "";
	
	/**
	 * The thread group of the thread
	 */
	private String threadGroup = "";
	
	/**
	 * The line number of the call
	 */
	private int lineNumber = 0;
	
	/**
	 * The Id/order of the log entry
	 */
	private int id = 0;
	
	/**
	 * A default date format
	 */
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	/**
	 * default constructor for marshaling the object with JAXB
	 */
	public LoggerEntry() {
		//ignore, used for marshaling
	}

	/**
	 * @param time to create the new entry with
	 * @param requestName name of the request
	 * @param message to create the new entry with
	 * @param level to create the new entry with
	 * @param classe to create the new entry with
	 * @param method to create the new entry with
	 * @param lineNumber to create the new entry with
	 * @param thread to create the new entry with
	 * @param threadGroup thread group
	 */
	public LoggerEntry(Date time, String requestName, String message, LogLevel level, String classe, String method, int lineNumber, String thread, String threadGroup) {

		this.id = ++count;
		
		this.requestName = requestName;
		
		this.time = time;
		this.message = message;
		
		this.classe = classe;
		this.method = method;
		this.lineNumber = lineNumber;
		this.thread = thread;
		this.threadGroup = threadGroup;
		
		if(level != null)
			this.level = level.toString();
	}

	/**
	 * The time will be created automatically with <code>new Date()</code>.
	 * 
	 * @param message
	 * @param requestName name of the request
	 * @param message to create the new entry with
	 * @param level to create the new entry with
	 * @param classe to create the new entry with
	 * @param method to create the new entry with
	 * @param lineNumber to create the new entry with
	 * @param thread to create the new entry with
	 * @param threadGroup thread group
	 */
	public LoggerEntry(String requestName, String message, LogLevel level, String classe, String method, int lineNumber, String thread, String threadGroup) {

		this(new Date(), requestName, message, level, classe, method, lineNumber, thread, threadGroup);
	}

	/**
	 * @return the time stamp of the entry
	 */
	public Date getTime() {
	
		if(this.time == null)
			return new Date();
		
		return this.time;
	}
	
	/**
	 * @return a formated time string
	 */
	public String getFormatedTime() {
		
		if(this.time == null)
			return this.dateFormat.format(new Date());
		
		 return this.dateFormat.format(this.time);
	}
	
	/**
	 * @param time to set
	 */
	public void setTime(Date time) {
	
		this.time = time;
	}
	
	/**
	 * @return the logged message
	 */
	public String getMessage() {
	
		if(this.message == null)
			return "";
		
		return this.message;
	}
	
	/**
	 * @param message to set
	 */
	public void setMessage(String message) {
	
		this.message = message;
	}
	
	/**
	 * @return the log level of the entry
	 */
	public String getLevel() {
	
		if(this.level == null)
			return "";
		
		return this.level;
	}
	
	/**
	 * @param level to set
	 */
	public void setLevel(String level) {
	
		this.level = level;
	}
	
	/**
	 * @return the class of the logger call
	 */
	public String getClasse() {
	
		if(this.classe == null)
			return "";
		
		return this.classe;
	}
	
	/**
	 * @param classe to set
	 */
	public void setClasse(String classe) {
	
		this.classe = classe;
	}
	
	/**
	 * @return the method of the logger call
	 */
	public String getMethod() {
		
		if(this.method == null)
			return "";
		
		return this.method;
	}
	
	/**
	 * @param method to set
	 */
	public void setMethod(String method) {
	
		this.method = method;
	}
	
	/**
	 * @return the thread of the logger call
	 */
	public String getThread() {
	
		if(this.thread == null)
			return "";
		
		return this.thread;
	}
	
	/**
	 * 
	 * @param thread to set
	 */
	public void setThread(String thread) {
	
		this.thread = thread;
	}
	
	/**
	 * @return the thread group of the logger call
	 */
	public String getThreadGroup() {
	
		if(this.threadGroup == null)
			return "";
		
		return this.threadGroup;
	}
	
	/**
	 * @param threadGroup to set
	 */
	public void setThreadGroup(String threadGroup) {
	
		this.threadGroup = threadGroup;
	}
	
	/**
	 * @return the line number of the logger call
	 */
	public int getLineNumber() {
	
		return this.lineNumber;
	}
	
	/**
	 * @param lineNumber to set
	 */
	public void setLineNumber(int lineNumber) {
	
		this.lineNumber = lineNumber;
	}

	/**
	 * @param id to set
	 */
	public void setId(int id) {

		this.id = id;
	}

	/**
	 * @return the id/order of the log entry
	 */
	public int getId() {

		return this.id;
	}

	/**
	 * @param requestName the requestName to set
	 */
	public void setRequestName(String requestName) {
		this.requestName = requestName;
	}

	/**
	 * @return the requestName
	 */
	public String getRequestName() {
		return this.requestName;
	}
}