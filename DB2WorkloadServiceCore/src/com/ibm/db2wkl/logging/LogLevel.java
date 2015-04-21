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
package com.ibm.db2wkl.logging;

import java.io.Serializable;

/**
 * This class represents a logging level.
 */
public class LogLevel implements Serializable{

	/**
	 * Auto generated
	 */
	private static final long serialVersionUID = -6649772155255531602L;

	/**
	 * This level logs all informations
	 */
	public static final LogLevel Debug = new LogLevel(0, "Debug  ", "This level logs all informations.");
	
	/**
	 * This level only information that are good to know
	 */
	public static final LogLevel Info = new LogLevel(1, "Info   ", "This level only information that are good to know.");
	
	/**
	 * This level logs only warning
	 */
	public static final LogLevel Warning = new LogLevel(2, "Warning", "This level logs only warning.");
	
	/**
	 * This level logs only errors. In many cases, the application will terminate after such an error
	 */
	public static final LogLevel Error = new LogLevel(3, "Error  ", "This level logs only errors. In many cases, the application will terminate after such an error.");

	/**
	 * Information, Message, Description
	 */
	private final String message;
	
	/**
	 * Name of the level
	 */
	private final String name;
	
	/**
	 * A integer that represents the level
	 */
	private final int level;
	
	/**
	 * @param level
	 * @param name
	 * @param message
	 */
	private LogLevel(int level, String name, String message) {
		
		this.level = level;
		this.name = name;
		this.message = message;
	}
	
	@Override
	public String toString() {
		
		return this.name;
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		
		return this.level;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		
		return this.message;
	}

	/**
	 * @param logLevel
	 * @return the log level with the specified integer order
	 */
	public static LogLevel getLevel(int logLevel) {

		if(logLevel == 1)
			return Info;
		if(logLevel == 2)
			return Warning;
		if(logLevel == 3)
			return Error;
		
		return Debug;
	}
	
	/**
	 * @param value
	 * @return the log level with the specified name
	 */
	public static LogLevel getLevel(String value) {

		if(value.trim().equalsIgnoreCase(Info.toString().trim()))
			return Info;
		if(value.trim().equalsIgnoreCase(Warning.toString().trim()))
			return Warning;
		if(value.trim().equalsIgnoreCase(Error.toString().trim()))
			return Error;
		
		return Debug;
	}

	/**
	 * @return a associated HTML color (e.g. for 'ERROR' the color 'RED')
	 */
	public String getHTMLColor() {
		
		if(this.level == 1)
			return "2EF01D";		//lighter green than normal "GREEN"
		if(this.level == 2)
			return "YELLOW";
		if(this.level == 3)
			return "RED";
		
		return "GRAY";
	}
}