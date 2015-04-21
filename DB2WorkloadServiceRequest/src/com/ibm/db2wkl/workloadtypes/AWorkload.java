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

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import com.ibm.db2wkl.helper.xml.ToXML;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.Logger;
import com.ibm.db2wkl.request.Request;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.db2wkl.workloadservice.WorkloadOS;
import com.ibm.db2wkl.workloadservice.WorkloadStatus;
import com.ibm.staf.STAFResult;

/**
 * <p>This is the abstract skeleton of a workload class.<p>
 * 
 * <p><b>Make sure...</b></p>
 * <ul>
 * 		<li>... that the method <code>getRequiredOptions()</code> works correctly</li>
 * 		<li>... that the method <code>getAllowedOptions()</code> works correctly</li>
 * 		<li>... that all created threads are added to the list <code>threads</code></li>
 * 		<li>... that your threads don't depends on each other, so that no deadlock can occur</li>
 * </ul>
 * 
 * <p>The abstract class AWorkloadService provides a lot of functions,to create a new workload as 
 * easy as possible. Many logging messages and result checking were done automatically.</p>
 * 
 * <p>Please take a close look to the code of the AWorkload class, before starting to write 
 * your own workload!</p>
 * 
 */
public abstract class AWorkload extends AStaticWorkload implements Runnable {

	/**
	 * <p>This field counts the calls of the execute()-method.</p>
	 */
	public int eCount = 0;
	
	/**
	 * The STAFResult that is set during the <code>work(...)</code> method.
	 */
	private STAFResult result = new STAFResult(STAFResult.Ok, "Workload successfull created");

	/**
	 * <p>This field contains the current status of the workload.</p>
	 * 
	 * <p><b>Note:</b> Please use the <code>setStatus(...)</code> method for this field (even
	 * when you use it internal)!</p>
	 */
	private WorkloadStatus status = WorkloadStatus.NEW;
	
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
	public AWorkload(){
		this.setCreationDate(new Date());
		this.setStatus(WorkloadStatus.NEW);
	}

	/**
	 * <p>This method will initialize the workload. This is necessary, because the workload class needs 
	 * to have a default and empty constructor for the loading mechanism. Also, it needs to be initialized, 
	 * so that the GUI components could be shown.</p>
	 * @param url 
	 * @param rsid sub request id
	 * 
	 */
	public final void initialize(String url, Long rsid){
		this.setId(rsid);
		this.setWorkloadURL(url);
		this.setStatus(WorkloadStatus.INITIALIZED);
	}
	
	/**
	 * @return the current Result
	 */
	public final STAFResult getResult() {
		
		return this.result;
	}
	
	/**
	 * @return the current status of the workload
	 */
	@Override
	@ToXML
	public final WorkloadStatus getStatus() {
		
		return this.status;
	}
	
	/**
	 * @return the unique ID to identify the workload
	 */
	@Override
	public final Long getId() {
		
		return this.id;
	}
	
	/**
	 * @return a unique status name (NAME + ID, like 'ManyThreads67')
	 */
	public final String getStatusName() {
		
		return getName() + getId();
	}

	/**
	 * Need to provide a search on the objects!
	 * 
	 * @return the user or the empty string if it was found
	 */
	@ToXML 
	public final String getUser() {
		
		if(Request.hasOption(Options.DB_USER))
			return Request.getOption(Options.DB_USER);
		return "";
	}
	
	/**
	 * Need to provide a search on the objects!
	 * 
	 * @return the SSID of the connection as a String or the empty string if it was found
	 */
	@ToXML
	public final String getSSID() {
		
		if(Request.hasOption(Options.DB_URLS))
			return Request.getOption(Options.DB_URLS);
		return "";
	}
	
	/**
	 * Need to provide a search on the objects!
	 * 
	 * @return the host of the connection as a String or the empty string if it was found
	 */
	@ToXML
	public final String getHost() {
		
		if(Request.hasOption(Options.DB_HOST))
			return Request.getOption(Options.DB_HOST);		
		return "";
	}
	
	/**
	 * Need to provide a search on the objects!
	 *
	 * @return the location of the connection as a String or the empty string if it was found
	 */
	@ToXML
	public final String getLocation() {
		
		if(Request.hasOption(Options.DB_LOCATIONNAME))
			return Request.getOption(Options.DB_LOCATIONNAME);
		return "";
	}
	
	/**
	 * Need to provide a search on the objects!
	 * 
	 * @return the port of the connection as a String or the empty string if it was found
	 */
	@ToXML
	public final String getPort() {
		
		if(Request.hasOption(Options.DB_PORT))
			return Request.getOption(Options.DB_PORT);	
		return "";
	}
	
	/**
	 * Need to provide a search on the objects!
	 * 
	 * @return the connection type (4 or 2) as a String or the empty string if it was found
	 */
	@ToXML
	public final String getType() {
	/*	TODO
		if(this._parsing != null) {
			if(this._parsing.optionTimes(Options.DB_TYPE_2) < 0){
				return "2";
			}
			return "4";
		}*/
		return "";
	}
	

	/**
	 * @return the current password
	 */
	public String getPassword() {

		return Request.getOption(Options.DB_PASSWORD);
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
	 * @return true, if the option REC was specified for this actual workload instance
	 */
	public boolean hasRecOption() {

		return Request.hasOption(Options.DB_REC);
	}
	
	/**
	 * <p>This method will set the current status and will call the visualization to update
	 * the new status there. Also, it will log the change.</p>
	 * 
	 * @param newStatus to set
	 */
	public final void setStatus(WorkloadStatus newStatus) 
	{
		if(newStatus != this.status) {
			
			Logger.log("Status of workload " + getStatusName() + " has changed from " + this.status + " to " + newStatus, LogLevel.Info);
			
			this.status = newStatus;
			
		}
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

		return getId() + "\t" + getName() + "\t" + getStatus() + "\t" + " started at " + this.timer.getStartDate() + "\t" + "Result: " + getResult();
	}

	/**
	 * @return the name of the workload
	 */
	@Override
	public abstract String getName();
	
	/**
	 * @return the command, which starts the workload
	 */
	public abstract String getCommand();
	
	/**
	 * @return the minimal required version of the DB2 system (e.g. 9.5)
	 */
	public double getMinimalRequiredDB2Version() {
		
		return 0;
	}
	
	/**
	 * @return the minimal required mode of the DB2 system (0 to 5)
	 */
	public int getMinimalRequiredDB2Mode() {
		
		return 0;
	}
	
	/**
	 * @return the supported operating systems
	 */
	public ArrayList<WorkloadOS> getSupportedOS() {

		return WorkloadOS.LUW_AND_ZOS;
	}

	/**
	 * @return a string representation of the current status and the workload name
	 */
	public String printStatus() {

		return this.getName() + " >> " + this.status;
	}
	
	/**
	 * Returns the value for the workload option identified by the key
	 * 
	 * @param key the name of the option to return the value for
	 * @return the value for the workload option; null if WKLOPTIONS is not specified and
	 * 			an empty string ("") when the key was not found
	 */
	protected String getWorkloadOption(String key) {
		
		// check for the existence of the workload option
		if (!Request.hasOption(Options.WKL_OPTIONS)) {
			return null;
		}
		
		String options = Request.getOption(Options.WKL_OPTIONS);
		StringTokenizer tokenizer = new StringTokenizer(options, ";");
		while (tokenizer.hasMoreElements()) {
			String option = (String) tokenizer.nextElement();
			// an option is of the following form key=value
			String _key = option.substring(0, option.indexOf("="));
			if (_key.equalsIgnoreCase(key)) {
				String value = option.substring(option.indexOf("=") + 1);
				return value;
			}
		}
		
		return "";
	}
	
	/**
	 * Loads the workload requestOptions into a {@link Properties} object
	 * 
	 * @return the workload requestOptions
	 */
	protected Properties getWorkloadOptions() {
		
		Properties properties = new Properties();
		
		String wklOptions = Request.getOption(Options.WKL_OPTIONS);
		if(wklOptions == null){
			return properties;
		}
		
		// a semicolon is used as delimiter for WKLOPTIONS
		StringTokenizer tokenizer = new StringTokenizer(wklOptions, ";");
		while (tokenizer.hasMoreElements()) {
			String option = (String) tokenizer.nextElement();
			// an option is given in the following form key=value (i.e. PAGES=100) or as a single String. (i.e. BP1;BP2;BP3)
			// In this case the String is used as key and as value.
			if (option.contains("=")) {
				String key = option.substring(0, option.indexOf("="));
				String value = option.substring(option.indexOf("=") + 1);
				properties.put(key.toUpperCase(), value);
			} else {
				//in case no "=" is provided, the key is also used as the value
				properties.put(option.toUpperCase(), option.toUpperCase());
			}
			
			
			
		}
		
		return properties;		
	}
	
	/**
	 * <p>This methods performs action, that have been to be done, after the init or execute method went wrong.</p>
	 */
	public final void workloadRunFailed() {
		
		try {
			
			/*
			 * >> CLEAN
			 */
			if(!Request.hasOption(Options.WKL_NO_CLEAN)) {
				this.setStatus(WorkloadStatus.CLEAN);
				performClean();
			}
			
			/*
			 * >> FINISH
			 */
			this.setStatus(WorkloadStatus.ABEND);
		} catch (Exception e) {
			Logger.log("The workload run failed. This exception is " +
					"thrown while trying to clean up the failed workload. " +
					"It does not correspond to the issue that caused the " +
					"workload to fail: " + e.getLocalizedMessage(), LogLevel.Error);
		} finally {
			this.setEndDate(new Date());
		}
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
		
		//set the URL for the workload
		ADataSourceConsumer.setUrl(this.getWorkloadURL());
		this.setStatus(WorkloadStatus.STARTED);
		
		try {
			
			/* ********************
			 * Before
			 * *******************/
			Logger.log("Workload " + getName() + " runs as thread " + Thread.currentThread().getName(), LogLevel.Debug);
			
			setStartDate(new Date());
	
			/* ********************
			 * INIT
			 * *******************/
			if(!Request.hasOption(Options.WKL_NO_INIT)) {
				this.setStatus(WorkloadStatus.INIT);
				STAFResult res = performInit();
				
				this.setResult(res);
			
				if (res.rc != STAFResult.Ok) { 
					workloadRunFailed();
					return;
				}
			}

			/* ********************
			 * EXECUTE
			 * *******************/
			if(!Request.hasOption(Options.WKL_NO_EXECUTE)) {
				
				this.setStatus(WorkloadStatus.EXECUTE);
				
				STAFResult res = performExecute();
				this.setResult(res);
				
				this.eCount++;
					
				if (res.rc != STAFResult.Ok) { 
					workloadRunFailed();
					return;
				}
			}
		} catch (Exception e){
			
			this.setStatus(WorkloadStatus.ABEND);
			Logger.log("There is an exception in the workloadStatus. " + this.getStatus(), LogLevel.Debug);
			Logger.logException(e);
			
			workloadRunFailed();
			
			this.setResult(new STAFResult(STAFResult.JavaError, e.getLocalizedMessage()));
			
		} catch (ThreadDeath e) {
			
			// this exception is thrown when the thread is stopped using the Thread.stop() method
			this.setStatus(WorkloadStatus.STOPPED);			
			Logger.log("The workload is stopping because the request received the STOP command", LogLevel.Info);
			
			this.setResult(new STAFResult(STAFResult.RequestCancelled));
		}
	}
}