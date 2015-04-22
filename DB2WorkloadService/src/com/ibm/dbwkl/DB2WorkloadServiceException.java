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
package com.ibm.dbwkl;

import com.ibm.dbwkl.logging.Logger;

/**
 * <p>This class represents a default exception of the DB2WorkloadService. This
 * exception is 'unchecked' and must not be caught. It should be used by the internal 
 * classes and methods of the service.</p>
 * 
 * <p><b>Note:</b> Each created instance will be logged automatically!</p>
 * 
 */
public class DB2WorkloadServiceException extends RuntimeException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * @param message to put into the exception
	 */
	public DB2WorkloadServiceException(String message) {

		super(message);
		
		Logger.logException(this);
	}
	
	/**
	 * @param message to put into the exception
	 * @param exception that caused this exception
	 */
	public DB2WorkloadServiceException(String message, Exception exception) {

		super(message, exception);
		
		Logger.logException(this);
	}

	/**
	 * @param exception that caused this exception
	 */
	public DB2WorkloadServiceException(Exception exception) {

		super(exception);
		
		Logger.logException(this);
	}
}
