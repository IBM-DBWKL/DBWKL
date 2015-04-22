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
 * This is to distinguish the Framework threads(SubRequest...) to the other Workload threads. 
 * This kind of thread won't be forcedly stopped by command line.
 * 
 * 
 */
public class ManagedFrameworkThread extends ManagedThread {

	/**
	 * Default constructor
	 */
	public ManagedFrameworkThread(){
		super();
	}
	
	/**
	 * Default constructor accepting a runnable
	 * @param runnable
	 */
	public ManagedFrameworkThread(Runnable runnable){
		super(runnable);
	}
	/**
	 * Constructor accepting a runnable and ThreadGroup
	 * @param runnable
	 * @param threadGroup
	 */
	public ManagedFrameworkThread(Runnable runnable, ThreadGroup threadGroup) {
		super(runnable, threadGroup);
	}

}
