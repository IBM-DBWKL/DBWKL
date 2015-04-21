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
package com.ibm.db2wkl.helper;

/**
 *
 */
public class NumberCounter {

	/**
	 * the counter
	 */
	private int counter = 0;
	
	/**
	 * Default constructor 
	 */
	public NumberCounter() {
		// 
	}
	
	/**
	 * Returns the current value of the counter and then increases it
	 * 
	 * @return the current counter value
	 */
	public synchronized int next() {
		return this.counter++;
	}
	
}
