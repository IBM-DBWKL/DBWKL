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
package com.ibm.db2wkl.request.handler;

import java.util.ArrayList;
import java.util.List;

import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.Logger;
import com.ibm.db2wkl.request.ASubRequest;
import com.ibm.db2wkl.request.DataSourceRequired;
import com.ibm.db2wkl.request.ManagedThread;
import com.ibm.db2wkl.request.Request;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.db2wkl.workloadtypes.sp.StoredProceduresModule;
import com.ibm.staf.STAFResult;

/**
 * RequestReciever for SP related requests
 * 
 * 
 */
@DataSourceRequired
public class SPHandler extends ASubRequest {

	/**
	 * ArrayList with all given tasks
	 */
	private ArrayList<String> taskset;
	
	/**
	 * ArrayList with the stored procedure modules for all tasks
	 */
	private ArrayList<StoredProceduresModule> sptasks;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.db2wkl.receiver.ASubRequest#acceptRequest
	 * (com.ibm.staf.STAFHandle,
	 * com.ibm.staf.service.STAFServiceInterfaceLevel30.RequestInfo,
	 * com.ibm.staf.service.STAFCommandParseResult)
	 */
	@Override
	public STAFResult acceptRequest() {
		STAFResult result = null;
		
		Logger.log("Accepted SP Request", LogLevel.Info);
		
		String[] tasks = Request.getOption(Options.SP_FILES).split(";");
		
		result = startTasks(tasks);
		
		return result;
	}

	/**
	 * @param tasks
	 *            as String Array with the file paths to the Stored Procedures
	 *            XML task files
	 * @return STAFResult if the SPM Task was successful or if a error occured
	 */
	private STAFResult startTasks(String[] tasks) {
		
		this.sptasks = new ArrayList<StoredProceduresModule>();
		this.taskset = new ArrayList<String>();
		
		List<Thread> spruns = new ArrayList<Thread>();
		
		if (tasks.length >= 1) {
			
			// execute the tasks
			for (String task : tasks) {
				this.taskset.add(task);

				StoredProceduresModule sptask = new StoredProceduresModule(task);
				sptask.initialize(this.url);
				this.sptasks.add(sptask);
				
				ManagedThread sprun = new ManagedThread(sptask);
				sprun.start();
				
				spruns.add(sprun);
				
			}
			
			// wait for the tasks to end
			for (Thread thread : spruns) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					//
				}
			}
			
		}
		
		return new STAFResult(STAFResult.Ok);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.db2wkl.receiver.ASubRequest#getAction()
	 */
	@Override
	public String getAction() {
		return Options.SP_SP;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.receiver.ASubRequest#stopForce()
	 */
	@Override
	public void stopForce() {
		if(this.sptasks!=null) {
			for(StoredProceduresModule sptask:this.sptasks) {
				sptask.stop();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.receiver.ASubRequest#getDataSourceConsumer()
	 */
	@Override
	public ADataSourceConsumer getDataSourceConsumer() {
		return null;
	}

}
