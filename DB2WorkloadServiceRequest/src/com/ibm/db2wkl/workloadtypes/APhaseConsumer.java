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
package com.ibm.db2wkl.workloadtypes;

import java.util.Date;

import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.Logger;
import com.ibm.db2wkl.request.Request;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.staf.STAFResult;

/**
 * Provides phase to those classes that require them
 * 
 *
 */
public abstract class APhaseConsumer extends ADataSourceConsumer {

	/**
	 * Initialize the workload. After this method all tables, functions and database object have to be created.
	 * 
	 * @return a Result object with an error code and a message
	 */
	public abstract STAFResult init();
	
	/**
	 * <p>Executes the workload.</p>
	 *
	 * @return a Result object with an error code and a message
	 */
	public abstract STAFResult execute();

	/**
	 * <p>This method <i>cleans</i> the database after the execution of the workload. It will be called, even after 
	 * the workload has failed or an error has occurred. So, make sure that this method will work correctly in any case.</p>
	 * 
	 * <p><b>Note:</b> This method will not be called, if the NOCLEAN option was specified!</p>
	 * 
	 * <p>This method removes all tables, functions and database object, that have been created in the <code>init()</code> 
	 * method of the workload. After that method, all created objects should be removed.</p>
	 * 
	 * <p>What's the difference between clean and terminate? - The clean method should delete all create database objects, 
	 * such as tables, views or table spaces. This method could be turned off with the NOCLEAN option. The terminate method 
	 * will be called in any case. It should close statements, connections and result sets.</p>
	 * 
	 * @return a Result object with an error code and a message
	 */
	public abstract STAFResult clean();

	/**
	 * This variable is set outside of the phase consumer when the request
	 * asks to stop the workload processing.
	 */
	private boolean stop = false;
	
	/**
	 * Tell the phase consumer to stop processing and go directly from the execute
	 * phase to the clean phase 
	 */
	public void stop() {
		this.stop = true;
	}
	
	/**
	 * <p><b>This method will...</b></p>
	 * <ul>
	 * 		<li>set the status to INIT</li>
	 * 		<li>call the INIT-method</li>
	 * 		<li>then call the connectionManager to close all objects</li>
	 * 		<li>then call the TERMINATE-method</li>
	 * </ul>
	 * @return result of initialization
	 */
	protected final STAFResult performInit() {

		STAFResult res;
		res = init();
		return res;
	}
	
	/**
	 * <p><b>This method will...</b></p>
	 * <ul>
	 * 		<li>set the status to EXECUTE</li>
	 * 		<li>call the EXECUTE-method</li>
	 * 		<li>then call the connectionManager to close all objects</li>
	 * 		<li>then call the TERMINATE-method</li>
	 * </ul>
	 * @return result of execution
	 */
	protected final STAFResult performExecute() {

		STAFResult res = null;
		
		// handle REPEAT and DURATION option here
		if (Request.hasOption(Options.THREADED_DURATION)) {
			
			try {
				// get the times
				int durationInMinutes = Integer.parseInt(Request.getOption(Options.THREADED_DURATION));
				int duration = durationInMinutes * 60 * 1000;
				long startTime = System.currentTimeMillis();
				long endTime = startTime + duration;
				
				Logger.log("End time: " + new Date(endTime).toString(), LogLevel.Debug);
				
				// use duration time
				int cnt = 0;
				do {
					res = execute();
					if (res.rc != STAFResult.Ok) {
						Logger.log("An error occurred during execution after " + 
								(System.currentTimeMillis() - startTime) +
								"ms of " + duration + "ms. Worked for " + cnt + " times", LogLevel.Error);
						break;
					}
					cnt++;
				} while (System.currentTimeMillis() < endTime && this.stop == false);
				
				Logger.log("Executed for " + cnt + " times", LogLevel.Info);
				
			} catch (NumberFormatException e) {
				String msg = "Invalid number as duration time: " + Request.getOption(Options.THREADED_DURATION);
				Logger.log(msg, LogLevel.Error);
				res = new STAFResult(STAFResult.JavaError, msg);
			}
			
		} else if (Request.hasOption(Options.THREADED_REPEAT)) {
			
			// use number of repeats
			int repeat;
			try {
				repeat = Integer.parseInt(Request.getOption(Options.THREADED_REPEAT));
			
				int cnt = 0;
				for (int i = 0; i < repeat; i++) {
					res = execute();
					if (res.rc != STAFResult.Ok) {
						Logger.log("An error occurred during execution after " + i + " repeats", LogLevel.Error);
						break;
					}
					cnt++;
					
					if (this.stop)
						break;
				}
				
				Logger.log("Executed for " + cnt + " times", LogLevel.Info);
			
			} catch (NumberFormatException e) {
				String msg = "Invalid number as number of repeats: " + Request.getOption(Options.THREADED_REPEAT);
				Logger.log(msg, LogLevel.Error);
				res = new STAFResult(STAFResult.JavaError, msg);
			}
			
		} else {
			
			// just execute it once
			res = execute();
			
		}
		
		return res;
	}	

	/**
	 * <p><b>This method will...</b></p>
	 * <ul>
	 * 		<li>set the status to CLEAN</li>
	 * 		<li>call the CLEAN-method</li>
	 * 		<li>then call the connectionManager to close all objects</li>
	 * 		<li>then call the TERMINATE-method</li>
	 * </ul>
	 * @return result of cleaning
	 */
	public final STAFResult performClean() {

		STAFResult res;
		res = clean();
		return res;
	}
	
}