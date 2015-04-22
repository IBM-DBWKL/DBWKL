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

import com.ibm.dbwkl.request.Logger;

/**
 *
 */
public class LoggedRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6487069121738705472L;

	/**
	 * @param message to put into the exception
	 */
	public LoggedRuntimeException(String message) {

		super(message);
		
		Logger.logException(this);
	}
	
	/**
	 * @param message to put into the exception
	 * @param exception that caused this exception
	 */
	public LoggedRuntimeException(String message, Exception exception) {

		super(message, exception);
		
		Logger.logException(this);
	}

	/**
	 * @param exception that caused this exception
	 */
	public LoggedRuntimeException(Exception exception) {

		super(exception);
		
		Logger.logException(this);
	}

}
