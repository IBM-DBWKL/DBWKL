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

import java.lang.Thread.State;
import java.util.Date;

import com.ibm.db2wkl.helper.StringUtility;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.ASubRequest;
import com.ibm.db2wkl.request.DataSourceRequired;
import com.ibm.db2wkl.request.Logger;
import com.ibm.db2wkl.request.ManagedFrameworkThread;
import com.ibm.db2wkl.request.ManagedThread;
import com.ibm.db2wkl.request.Request;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.db2wkl.workloadservice.WorkloadStatus;
import com.ibm.db2wkl.workloadtypes.AWorkload;
import com.ibm.db2wkl.workloadtypes.model.WorkloadClass;
import com.ibm.db2wkl.workloadtypes.viewmodel.WorkloadClassViewModel;
import com.ibm.staf.STAFResult;

/**
 *
 */
@DataSourceRequired
public class WorkloadHandler extends ASubRequest {

	/**
	 * The workload that is executed by this handler
	 */
	private AWorkload workload;
	
	/**
	 * The workload thread
	 */
	private ManagedFrameworkThread workloadThread;
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.request.ASubRequest#acceptRequest()
	 */
	@Override
	public STAFResult acceptRequest() {
		
		if (Request.hasOption(Options.WKL_START)) {
			try {
				// get all available workloads
				WorkloadClassViewModel workloadClassViewModel = new WorkloadClassViewModel();
				
				// search the list of available workloads
				for (WorkloadClass workloadClass : workloadClassViewModel.getWorkloadClasses()) {
					if (StringUtility.equalsIgnoreCase(StringUtility.getSimpleClassName(workloadClass.getName()), Request.getOption(Options.WKL_START)) ||
							StringUtility.equalsIgnoreCase(workloadClass.getName(), Request.getOption(Options.WKL_START)) ||
							StringUtility.equalsIgnoreCase(workloadClass.getLongName(), Request.getOption(Options.WKL_START)) ||
							StringUtility.equalsIgnoreCase(workloadClass.getShortName(), Request.getOption(Options.WKL_START))) {
						
						Class<?> wcl = Class.forName(workloadClass.getName());
						Class<? extends AWorkload> wklClass = wcl.asSubclass(AWorkload.class);
						
						this.workload = wklClass.newInstance();
					}
				}
				
				// if there is now workload assigned now return with an error
				if (this.workload == null) {
					// reuse the workload list from above to display all registered workloads
					StringBuilder workloadsBuilder = new StringBuilder();
					for(WorkloadClass workloadClass : workloadClassViewModel.getWorkloadClasses()){
						workloadsBuilder.append(
								workloadClass.getLongName() + 
								(workloadClass.getShortName() == null ? "" : (" (" + workloadClass.getShortName()) + ")") + 
								"\n");
					}
					Logger.log("Workload not found", LogLevel.Warning);
					return new STAFResult (STAFResult.InvalidValue, "Workload not found. please use one of the following workloads.:\n\n" + workloadsBuilder.toString());
				}
				
			} catch (Exception e) {
				Logger.log(e.getLocalizedMessage(), LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, e.getLocalizedMessage());
			} 
			
			// set it up
			this.workload.initialize(this.url, new Long(this.getId()));
		
			ThreadGroup workloadThreadGroup = new ThreadGroup(Thread.currentThread().getName());
			this.workloadThread = new ManagedFrameworkThread(this.workload, workloadThreadGroup);
			this.workloadThread.start();
			
			try {
				// wait for the workload to finish
				this.workloadThread.join();
				
			} catch (Exception e) {
				// The workload thread was interrupted. This can happen when the request
				// is stopped via command line.
				Logger.log("Thread " + this.workloadThread.getName() + " was stopped", LogLevel.Info);
			}
			
			/* ********************
			 * Clean & terminate
			 * *******************/
			if(!Request.hasOption(Options.WKL_NO_CLEAN)) {
				this.workload.setStatus(WorkloadStatus.CLEAN);

				STAFResult res = this.workload.performClean();
				
				this.setResult(res);
		
				if (res.rc != STAFResult.Ok) { 
					this.workload.workloadRunFailed();
					this.setResult(res);
				}
			}
			
			/* ********************
			 * After
			 * *******************/
			
			this.workload.setEndDate(new Date());
			this.workload.setStatus(WorkloadStatus.ENDED);
			
			return this.workload.getResult();
				
		} else {
		
			return new STAFResult(STAFResult.InvalidRequestString, "No START specified");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.receiver.ASubRequest#getAction()
	 */
	@Override
	public String getAction() {
		return Options.WKL_WORKLOAD;
	}

	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.receiver.ASubRequest#isConnectionManagerRequired()
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void stopForce() {
		
		// stop the threads
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
		Thread[] threadList = new Thread[threadGroup.activeCount()];
		
		// Finish all threads in the list which are in the current threadgroup and opened
		// by the workload as managed threads. This does not include the workload thread
		// itself as this is a managed framework thread
		threadGroup.enumerate(threadList);
		for(Thread thread : threadList){
			if(!(thread instanceof ManagedFrameworkThread) && thread instanceof ManagedThread && !thread.getName().startsWith("AWT")){
				Logger.log("Thread "+ thread.getName() + " will stop.", LogLevel.Info);
				try {
					if (thread.getState() != State.TERMINATED) {
						thread.stop();
					}
				} catch (ThreadDeath e) {
					Logger.log("Expected ThreadDeath occurred for thread " + thread.getName(), LogLevel.Info);
				}
			}
		}
			
		
		try {
			
			// now that all child threads are stopped, stop the workload thread itself
			this.workloadThread.stop();
			
		} catch (ThreadDeath e) {
			Logger.log("Expected ThreadDeath occurred", LogLevel.Info);
		}
			
			
		
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.receiver.ASubRequest#getDataSourceConsumer()
	 */
	@Override
	public ADataSourceConsumer getDataSourceConsumer() {
		return this.workload;
	}
}
