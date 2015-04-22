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

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.Vector;

import com.ibm.dbwkl.report.Report;
import com.ibm.dbwkl.rmi.RequestReporterRemoteInterface;


/**
 *
 */
public class RequestReporter implements RequestReporterRemoteInterface {

	/**
	 * the period of the RequestReporterTask. Default is 1 second.
	 */
	public static final long PERIOD = 1000;

	/**
	 * request
	 */
	private Request request;
	
	/**
	 * The name of the Request notifier which started with reqr 
	 */
	private String name;
		
	/**
	 * the report
	 */
	private Vector<Report> reportList;
	
	/**
	 * the timer
	 */
	private Timer timer;
	
	/**
	 * the RequestReporterTask to be scheduled on the timer
	 */
	private RequestReporterTask requestReporterTask;
	
	/**
	 * the running status
	 */
	private boolean status = false;
	
	/**
	 * @param request 
	 */
	public RequestReporter(Request request) {
		this.request = request;
		this.name = "reqr" + this.request.getRid();
	}

	/**
	 * @return request 
	 */
	public Request getRequest() {
		return this.request;
	}
	
	/**
	 * initialize the timer and start the requestReportTask.
	 * It has to be initialized within the request thread to ensure the timer thread is in the same thread group as the request.   
	 */
	public void init() {
		this.timer = new Timer(this.name, true);
		this.reportList = new Vector<Report>();
	}

	
	/**
	 * Start the timer and schedules the requestReporterTask. Initialize the report.
	 */
	@Override
	public void start() {
		if(this.status){
			//if it's already started, do nothing.
		} else {
			
			this.requestReporterTask = new RequestReporterTask(this);
			this.timer.schedule(this.requestReporterTask, 0, PERIOD);
			this.status = true;
		}
	}

	/**
	 * Stop the timer and the requestReporterTask. Clear the report list.
	 */
	@Override
	public void stop() {
		
		if(this.reportList != null){
			this.reportList.clear();
		}
		
		if(this.status) {
			this.requestReporterTask.cancel();
			this.requestReporterTask = null;
			this.status = false;
			
		} else {
			//if it's already stopped, do nothing.
		}
		
	}
	

	/**
	 * add the report to the list
	 * @param report the report to add
	 */
	public void add(Report report) {
		this.reportList.add(report);
	}

	/**
	 * output the report
	 * @param report the report to output
	 */
	public void output(Report report) {
		// TODO for future use
		//Logger.log("RR" + this.request.getRid() + " - #" + report[0] + "#" + report[1] + "#" + report[2] + "#" + report[3], LogLevel.Debug);
	}

	/**
	 * return the report to the Reporter and clear the report
	 * @return copyOfReport a copy of the report.
	 */
	@Override
	public Vector<Report> getAndClearReport() {
		if(this.reportList == null || this.reportList.size() == 0){
			return null;
		} else {
			//return a copy of the report, and clear the report.
			Vector<Report> copyOfReport = new Vector<Report>(this.reportList);
			this.reportList.clear();
			return copyOfReport;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.rmi.RequestReporterRemoteInterface#getLatestReport()
	 */
	@Override
	public Report getLatestReport() throws RemoteException {
		if (this.reportList.size() > 0)
			return this.reportList.lastElement();
		return null;
	}

	/**
	 * Terminate the RequestReporter, and report to the Reporter for the last time.
	 * @throws RemoteException 
	 */
	public void terminate() throws RemoteException {
		if(this.requestReporterTask != null)
			this.requestReporterTask.cancel();
		
		if(this.timer != null)
			this.timer.cancel();
		if(RequestPerformer.getReporterStub().getStatus()){
			RequestPerformer.getReporterStub().lastReport(this.request.getRid(), getAndClearReport());
		}
	}
}
