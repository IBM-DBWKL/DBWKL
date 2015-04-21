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
package com.ibm.db2wkl.workloadservice;


/**
 * <p>This class represents the status of a workload.</p>
 * 
 * <p><b>Note:</b><code> NEW < INITIALIZED < STARTED < INIT < EXECUTE < CLEAN < 
 * TERMINATE < STOPPED < ENDED < ABEND</code></p>
 * 
 */
public enum WorkloadStatus {

	/**
	 * Workload is new
	 */
	NEW,
	
	/**
	 * Workload is initialized
	 */
	INITIALIZED,
	
	/**
	 * Workload is started
	 */
	STARTED,
	
	/**
	 * Workload is in the initialize phase
	 */
	INIT,
	
	/**
	 * Workload is in the execute phase
	 */
	EXECUTE,
	
	/**
	 * Workload is in the clean phase
	 */
	CLEAN,
	
	/**
	 * Workload is terminating
	 */
	TERMINATE,
	
	/**
	 * Workload is stopped
	 */
	STOPPED,
	
	/**
	 * Workload has ended 
	 */
	ENDED,
	
	/**
	 * Workload is abended (broken down)
	 */
	ABEND
	
}