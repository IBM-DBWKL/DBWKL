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

import com.ibm.db2wkl.helper.xml.ToXML;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.Logger;
import com.ibm.db2wkl.request.Request;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.db2wkl.workloadservice.WorkloadStatus;
import com.ibm.staf.STAFResult;

/**
 * This class is the threaded framework for the Stored Procedures Module
 *
 */
public abstract class AStoredProceduresModule extends APhaseConsumer implements Runnable {

	/**
	 * <p>This field counts the calls of the execute()-method.</p>
	 */
	public int eCount = 0;
	
	/**
	 * The STAFResult that is set during the <code>work(...)</code> method.
	 */
	private STAFResult result = new STAFResult(STAFResult.Ok, "Stored Procedure successfull created");
	
	/**
	 * <p>This is the unique ID of the workload. The ID is unique for the whole session 
	 * (so long as the STAF process is running and the service is registered to it).</p>
	 */
	private Long id;
	
	/**
	 * <p>This object contains date and time information.</p>
	 */
	protected final WorkloadTimer timer = new WorkloadTimer();
	
	/*
	 * Methods
	 */
	
	/**
	 * This is the default constructor of a workload. It must have no parameter, because
	 * it must be instantiated in the loading process. 
	 * 
	 * The constructor will create all necessary objects and it will count up the global
	 * workload counter.
	 */
	public AStoredProceduresModule(){
		
		// set some attributes TODO
		this.setId(new Long(0));
		this.setCreationDate(new Date());
	}

	/**
	 * <p>This method will initialize the workload. This is necessary, because the workload class needs 
	 * to have a default and empty constructor for the loading mechanism. Also, it needs to be initialized, 
	 * so that the GUI components could be shown.</p>
	 * 
	 * @param parserResult with PASSWORD, USER, etc.
	 * @param url to the database
	 */
	public final void initialize(String url){
		
		this.workloadURL = url;
		
		ADataSourceConsumer.setUrl(url);
	}
	
	/**
	 * <p>This method will call the <code>init(...)<code> method, the <code>execute(...)</code> 
	 * method, the <code>clean(...)</code> method and the <code>terminate(...)</code> method of 
	 * the workload. Also, it will check  whether the methods were successful or not. It will also 
	 * log some additional informations and will increment some counters to control the execution 
	 * and the logging informations.</p>
	 */
	@Override
	public final void run() {

		//ADataSourceConsumer.setUrl(this.getWorkloadURL());
		Logger.log("Stored Procedure " + getName() + this.id + " started", LogLevel.Info);
		
		try {
			
			/* ********************
			 * Before
			 * *******************/
			Logger.log(getName() + this.id + " runs as thread " + Thread.currentThread().getName(), LogLevel.Debug);
			
			setStartDate(new Date());
	
			/* ********************
			 * INIT
			 * *******************/
			
			this.result = this.performInit();
			
				if (this.result.rc != STAFResult.Ok) { 
					StoredProcedureTaskFailed();
					return;
				}
			
			/* ********************
			 * EXECUTE
			 * *******************/

			this.result = performExecute();
					
			this.eCount++;
						
			if (this.result.rc != STAFResult.Ok) { 
				StoredProcedureTaskFailed();
				return;
			}
			
			/* ********************
			 * Clean & terminate
			 * *******************/
			
			if(!Request.hasOption(Options.WKL_NO_CLEAN)) {
				
				this.result = performClean();
		
				if (this.result.rc != STAFResult.Ok) { 
					StoredProcedureTaskFailed();
					return;
				}
			}
	
			/* ********************
			 * After
			 * *******************/
			this.setEndDate(new Date());
			
		}
		catch(Exception e){
			Logger.log("There is an exception in " + getName() + this.id, LogLevel.Debug);
			Logger.logException(e);
			
			this.result = new STAFResult(STAFResult.JavaError, e.getLocalizedMessage()); 
		}
	}
	
	/**
	 * Tasks if a SPM Task fails
	 * @throws Exception 
	 */
	private void StoredProcedureTaskFailed() throws Exception {
		clean();
	}

	/**
	 * @return the current Result
	 */
	public final STAFResult getResult() {
		
		return this.result;
	}
	
	/**
	 * @return the unique ID to identify the workload
	 */
	@Override
	public final Long getId() {
		
		return this.id;
	}
	
	/**
	 * @return the duration time of this actual sp instance
	 */
	protected int getDurationTime() {

		String duration = getDurtime();
		
		if(duration == null || duration.equals(""))
			duration = "0";

		return Integer.parseInt(duration);
	}
	
	/**
	 * @return the threads of this actual sp instance
	 */
	protected int getThreads() {

		if(Request.hasOption(Options.THREADED_INSTANCES)) {
			String threads = Request.getOption(Options.THREADED_INSTANCES);
			try {
				return Integer.parseInt(threads);
			} catch(NumberFormatException e) {
				return 0;
			}
		}

		return 0;
	}

	/**
	 * Need to provide a search on the objects!
	 * 
	 * @return the duration time of this actual workload instance
	 */
	@ToXML
	public final String getDurtime() {
		
		if(Request.hasOption(Options.THREADED_DURATION))
			return Request.getOption(Options.THREADED_DURATION);
		return "";
	}

	/**
	 * @return the creation date
	 */
	public Date getCreationDate() {

		return this.timer.getCreationDate();
	}

	/**
	 * @return the end date
	 */
	@Override
	public Date getEndDate() {

		return this.timer.getEndDate();
	}

	/**
	 * @return the start date
	 */
	@Override
	public Date getStartDate() {

		return this.timer.getStartDate();
	}

	/**
	 * <p>This is the setter for the result object. It should be used, even in the workload
	 * class itself. It will set the result and update the visualizer that needs this value.</p>
	 * 
	 * @param result to set
	 */
	public final void setResult(STAFResult result) {
		
		this.result = result;
	}
	
	/**
	 * <p>This method will set the id in the workload itself and it will update it 
	 * in the visual representation of it.</p>
	 * 
	 * @param id to set
	 */
	private final void setId(Long id) {
		
		this.id = id;
	}

	/**
	 * <p>This method will set the start date in the workload itself and it will update it 
	 * in the visual representation of it.</p>
	 * 
	 * @param date to set
	 */
	public final void setStartDate(Date date) {
		
		this.timer.setStartDate(date);
	}

	/**
	 * <p>This method will set the end date in the workload itself and it will update it 
	 * in the visual representation of it.</p>
	 * 
	 * @param date to set
	 */
	public final void setEndDate(Date date) {
		
		this.timer.setEndDate(date);
	}
	
	/**
	 * <p>This method will set the end date in the workload itself and it will update it 
	 * in the visual representation of it.</p>
	 * 
	 * @param date to set
	 */
	public final void setCreationDate(Date date) {
		
		this.timer.setCreationDate(date);
	}

	/**
	 * @return a string representation of the workload
	 */
	@Override
	public final String toString() {

		return getId() + "\t" + getName() + "\t" + this.id + "\t" + " started at " + this.timer.getStartDate() + "\t" + "Result: " + getResult();
	}
	
	/* ********************************************************************************************************** */
	/* ********************************************************************************************************** */
	/* ********************************************************************************************************** */
	/* *                                     ABSTRACT and open to overwrite                                     * */
	/* ********************************************************************************************************** */
	/* ********************************************************************************************************** */
	/* ********************************************************************************************************** */
	 
	/**
	 * Initialize the workload. After this method all tables, functions and database object have to be created.
	 * 
	 * @return a Result object with an error code and a message
	 * @throws Exception 
	 */
//	@Override
//	public abstract STAFResult init() throws Exception;
	
	/**
	 * <p>Executes the workload.</p>
	 *
	 * @return a Result object with an error code and a message
	 */
//	@Override
	//public abstract STAFResult execute() throws Exception;

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
//	@Override
	//public abstract STAFResult clean() throws Exception;

	/**
	 * <p>This method will be called as the last method. It will be called in <b>any case</b>, even if the execution of 
	 * the workload failed or was canceled.</p>
	 * 
	 * <p>This method close all statements, connection, results or anything like that, that the workload has created during 
	 * the execution. So make sure, that this method really close each open resource and isn't throwing an error in any case.</p>
	 * 
	 * <p>This method will also be called, after the workload has failed or an error has occurred. So, make sure that this 
	 * method will work correctly in any case.</p>
	 * 
	 * <p>What's the difference between clean and terminate? - The clean method should delete all create database objects, 
	 * such as tables, views or table spaces. This method could be turned off with the NOCLEAN option. The terminate method 
	 * will be called in any case. It should close statements, connections and result sets.</p>
	 * 
	 * @param url1 to the database where the workload should runs
	 * @param parsing with USER, PASSWORD, etc.
	 * @return a Result object with an error code and a message
	 */
	public STAFResult terminate() {
		
		Logger.log("terminate-Methode from AWorkloadService with the DataSource : " + Request.getDataSource().toString() , LogLevel.Info);
		Request.getDataSource().terminate();
		
		return new STAFResult(STAFResult.Ok, "Termination successfull");
	}

	/**
	 * @return the command, which starts the workload
	 */
	public abstract String getCommand();
	
	/**
	 * @return a description of the workload for the user
	 */
	public abstract String getDescription();
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadservice.ADataSourceConsumer#getName()
	 */
	@Override
	public String getName(){
		return Options.SP_SP;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadservice.ADataSourceConsumer#getStatus()
	 */
	@Override
	public WorkloadStatus getStatus(){
		return null;
	}
	
}
