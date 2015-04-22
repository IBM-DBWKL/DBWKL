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


/**
 * Simin Xia (siminxia@de.ibm.com)
 *
 */
public enum RequestType {

	/**
	 * The request type is internal. It runs within the db2wkl service JVM, which often involves tightly the service
	 */
	INTERNAL,

	 //the following three types are of the opposite of INTERNAL requests. They run in the separate JVM from db2wkl 
	
	/**
	 * The request is initiated and running on local machine
	 */
	LOCAL,
	
	/**
	 * The request is redirected to a remote machine, but monitored and controlled by local machine. 
	 */
	REMOTE, 
	
	/**
	 * The request is redirected from another machine.
	 */
	INCOMING,
	
}