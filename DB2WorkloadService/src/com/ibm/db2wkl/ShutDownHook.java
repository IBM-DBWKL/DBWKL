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
package com.ibm.db2wkl;

import com.ibm.db2wkl.logging.Logger;

/**
 * <p>This thread will be added to the runtime environment to handle an immediate program shutdown.</p>
 */
public class ShutDownHook extends Thread {

	/**
	 * A thread that acts as a shut-down-hook
	 */
	private static final Thread shutDownHook = new ShutDownHook();
	
	/** Singleton pattern **/
	private ShutDownHook() { /***/ }
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		
        Logger.log("Immediate programm shutdown", Logger.Warning);
        
	}
	
	/**
	 * Adds the shutdown hook to the system runtime
	 */
	public static void addShutDownHook() {
		
		Runtime.getRuntime().addShutdownHook(shutDownHook);
	}
	
	/**
	 * Removes the shutdown hook from the system runtime
	 */
	public static void removeShutDownHook() {
		
		Runtime.getRuntime().removeShutdownHook(shutDownHook);
	}
}