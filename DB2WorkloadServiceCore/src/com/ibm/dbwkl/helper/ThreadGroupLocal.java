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
package com.ibm.dbwkl.helper;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @param <T> Type to store in this localized thread group
 */
public class ThreadGroupLocal <T> {

	/**
	 * Map that stores the object for the thread group
	 */
	private ConcurrentHashMap<ThreadGroup, T> map = new ConcurrentHashMap<ThreadGroup, T>();
	
	/**
	 * Returns the object for this thread group
	 * 
	 * @return object for the thread group
	 */
	public T get() {
		synchronized (this.map) {
			return this.map.get(Thread.currentThread().getThreadGroup());
		}
	}
	
	/**
	 * Returns the current object for the current thread group if it
	 * exists. If not, it is added to the hash map. In both cases,
	 * it is also returned.
	 * 
	 * @param object object to set in case it is not in the map already
	 * @return the object for the thread group
	 */
	public T getAndSetIfNull(T object) {
		synchronized (this.map) {
			if (this.map.containsKey(Thread.currentThread().getThreadGroup())) {
				return this.map.get(Thread.currentThread().getThreadGroup());
			}
			this.map.put(Thread.currentThread().getThreadGroup(), object);
			return object;
		}
	}
	
	/**
	 * Sets the given object to the current thread group
	 * 
	 * @param object object to set
	 */
	public void set(T object) {
		synchronized (this.map) {
			this.map.put(Thread.currentThread().getThreadGroup(), object);
		}
	}
	
	/**
	 * Sets the given object to the current thread group
	 * 
	 * @param object object to set
	 */
	public void setIfNull(T object) {
		synchronized (this.map) {
			this.map.putIfAbsent(Thread.currentThread().getThreadGroup(), object);
		}
	}
	
	/**
	 * Removes the object for the current thread group and the thread group itself from the map
	 */
	public void remove() {
		synchronized (this.map) {
			this.map.remove(Thread.currentThread().getThreadGroup());
		}
	}
}
