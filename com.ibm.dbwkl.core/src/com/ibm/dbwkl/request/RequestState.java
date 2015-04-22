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

import java.io.Serializable;

/**
 * Stephan Arenswald (arens@de.ibm.com)
 * Hong Nhung Pham (hnpham@de.ibm.com)
 *
 */
public enum RequestState implements Serializable{

	/**
	 * The request is new
	 */
	NEW,
	
	/**
	 * The request is initializing
	 */
	INITIALIZING,
	
	/**
	 * The request was initialized
	 */
	INITIALIZED,
	
	/**
	 * The request is executing
	 */
	EXECUTING,
	
	/**
	 * The request was executed
	 */
	EXECUTED,
	
	/**
	 * The request is cleaning
	 */
	CLEANING,
	
	/**
	 * The request was finished
	 */
	FINISHED,
	
	/**
	 * An error occurred during one of the thread state transitions
	 */
	BROKEDOWN,
	
	/**
	 * The request is stopping
	 */
	STOPPING,
	
	/**
	 * The request was stopped
	 */
	STOPPED
}
