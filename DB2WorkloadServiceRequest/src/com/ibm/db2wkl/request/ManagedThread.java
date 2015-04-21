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
 *
 */
public class ManagedThread extends Thread {

	/**
	 * A child thread counter so that the name of the thread with the number can be determined easily
	 */
	private int childThreadCounter = 0;
	
	/**
	 * Default constructor accepting a runnable
	 * 
	 * @param runnable 
	 * 
	 */
	public ManagedThread(Runnable runnable) {
		super(Thread.currentThread().getThreadGroup(), runnable);
		
		String threadName = Thread.currentThread().getName() + ":" + getNextNumber();
		this.setName(threadName);
	}
	
	/**
	 * Default constructor
	 */
	public ManagedThread() {
		super(Thread.currentThread().getThreadGroup(), Thread.currentThread().getName() + ":" + getNextNumber());
	}
	
	/**
	 * Constructor accepting a runnable and a threadGroup
	 * 
	 * @param runnable the thread to mark as managed thread 
	 * @param threadGroup the corresponding thread group
	 */
	public ManagedThread(Runnable runnable, ThreadGroup threadGroup) {
		super(threadGroup, runnable);
		
		String threadName = Thread.currentThread().getName() + ":" + getNextNumber();
		this.setName(threadName);
	}

	/**
	 * Returns the next number to identify the child thread
	 * @return the next number for the thread
	 */
	private static int getNextNumber() {
		if (Thread.currentThread() instanceof ManagedThread) {
			ManagedThread mt = (ManagedThread) Thread.currentThread();
			return mt.returnAndIncreaseChildThreadCounter();
		} else {
			return Request.getThreadCounter().next();
		}
	}
	
	/**
	 * @return the childThreadCounter
	 */
	public int getChildThreadCounter() {
		return this.childThreadCounter;
	}

	/**
	 * Returns and afterwards increases the child thread counter
	 * 
	 * @return the current child thread counter
	 */
	private int returnAndIncreaseChildThreadCounter() {
		return this.childThreadCounter++;
	}
}
