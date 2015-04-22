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

import java.util.ArrayList;

import com.ibm.dbwkl.logging.Logger;
import com.ibm.dbwkl.request.RequestIdGenerator;

/**
 * This class could store commands. This could be used for a history,
 * a report or a auto-completion.
 * 
 */
public class CommandCounter {

	/**
	 * <p>This field counts unique workload IDs.</p> 
	 * Note: this no longer works since workloads will be created in a separate JVM, and this value is not synchronized between JVMs.
	 */
	@Deprecated
	private static int currentWorkloadId = 0;
	
	/**
	 * <p>This field counts unique request IDs.</p>
	 */
	@Deprecated
	private static int currentRequestId = 0;

	/**
	 * A list of stored commands
	 */
	private static ArrayList<String> commands = new ArrayList<String>();
	
	/**
	 * @param request to store
	 */
	public static void saveNewRequest(String request) {

		currentRequestId++;
		
		commands.add(request);
		
		Logger.log("Request number " + commands.size(), Logger.Debug);
		Logger.log("Request: '" + request + "'", Logger.Debug);
	}
	
	/**
	 * @return a new and unique workload ID
	 */
	@Deprecated
	public static int getNextWorkloadId() {
		
			return ++currentWorkloadId;
	}

	/**
	 * @return a new and unique workload ID
	 * Note: {@link RequestIdGenerator} is used instead.
	 */
	@Deprecated
	public static int getNextRequestId(){
		
		return ++currentRequestId;
	}
}