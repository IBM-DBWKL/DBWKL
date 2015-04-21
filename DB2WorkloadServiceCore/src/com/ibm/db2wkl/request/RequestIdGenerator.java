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
package com.ibm.db2wkl.request;

/**
 * Request ID generator
 * 
 *
 */
public class RequestIdGenerator {

	/**
	 * Static id that holds the last generated ID
	 */
	private static Long id = new Long(0);
	
	/**
	 * Generates a new id
	 * @return the newly generated ids
	 */
	public synchronized static Long getNextId() {
		id = new Long(id.longValue() + 1);
		return id;
	}
	
}
