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

import java.io.IOException;


/**
 * This interface describes a logger object.
 *
 */
public interface ILoggerService {
	
	/**
	 * This method will be called by the Logger class for each registered logger instance.
	 * @param entry to log
	 */
	public abstract void log(LoggerEntry entry);
	
	/**
	 * @return the name of the logger
	 */
	public abstract String getName();
	
	/**
	 * This method is called when the application is terminating. You should
	 * close all resources, streams and connections here.
	 */
	public abstract void term();

	/**
	 * cleans if wished the generated log file
	 */
	public abstract void clean();
	
	/**
	 * initializes the logger
	 * @throws IOException 
	 */
	public abstract void init() throws IOException;
	
}