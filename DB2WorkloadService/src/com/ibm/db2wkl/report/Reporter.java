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
package com.ibm.db2wkl.report;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.db2wkl.DB2WorkloadService;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.logging.Logger;
import com.ibm.db2wkl.request.RequestManager;
import com.ibm.db2wkl.rmi.ReporterRemoteInterface;
import com.ibm.db2wkl.rmi.RequestRemoteInterface;
import com.ibm.db2wkl.rmi.RequestReporterRemoteInterface;


/**
 * This implements the Reporter functionality. It schedules TimerTask to run at a specified interval to retrieve reports from all the running RequestReporter.
 * And it generates report message every interval. 
 * 
 *
 */
public class Reporter implements ReporterRemoteInterface {

	/**
	 * Singleton instance of the request manager
	 */
	private static Reporter reporter;
	
	/**
	 * The timer 
	 */
	private Timer timer = null;
	
	/**
	 * The TimerTask which is scheduled to timely collect reports.  
	 */
	private ReportCollectTask reportCollectTask = null;
	
	/**
	 * The running status the Reporter
	 */
	private boolean status = false;
	
	/**
	 * The interval between each ReportCollectTask, in second
	 */
	private int interval = 60;
	
	/**
	 * Activates a detailed report message 
	 */
	private boolean details = false;
	
	/**
	 * The output File path
	 */
	private String outFile = null;
	
	/**
	 * Constructor. The timer is initialized and running throughout the service's lifetime
	 */
	private Reporter(){
		this.timer = new Timer("Reporter", true);
	}
	
	/**
	 * Returns or creates the singleton reporter
	 * 
	 * @return singleton instance of this reporter
	 */
	public static Reporter getInstance() {
		if(reporter == null)
			reporter = new Reporter();
		return reporter;
	}
				
	/**
	 * Turn the reporter on with default values.
	 */
	public void turnOn() {
		turnOn(this.interval, this.details, this.outFile);
	}
	
	/**
	 * Turns the reporter on. It activates the timer and reportCollectTask to timely collect reports from requests.
	 * If the reporter is already on, it changes the parameters accordingly, and reschedule the reportCollectTask.
	 * It also starts all the RequestReporters. 
	 * @param _interval the interval between each report collect action.
	 * @param _details the details marker to set
	 * @param _outFile the output file path
	 */
	public void turnOn(int _interval, boolean _details, String _outFile) {
		this.details = _details;
		this.outFile = _outFile;
		
		if(!this.status){
			//if it's not On already.
			this.interval = _interval;
			this.reportCollectTask = new ReportCollectTask(); 
			this.timer.schedule(this.reportCollectTask, 0, this.interval * 1000);
			this.status = true;
		} else {
			//if it's already On
			if(this.interval != _interval){
				this.interval = _interval;
				this.reportCollectTask.cancel();
				this.reportCollectTask = new ReportCollectTask(); 
				this.timer.schedule(this.reportCollectTask, 0, this.interval * 1000);
			} else {
				// do nothing if the interval is the same.
			}
		}
		
		//start all the RequestReporters if exist. 
		if(RequestManager.getInstance().getOriginatedRequests() != null) {
			Iterator<Entry<Long, RequestRemoteInterface>> requests = RequestManager.getInstance().getOriginatedRequests().entrySet().iterator(); 
			while(requests.hasNext()){
				Entry<Long, RequestRemoteInterface> request = requests.next();
				RequestReporterRemoteInterface requestReporter = null;
				try {
					if((requestReporter = request.getValue().getRequestReporter()) != null) {
						requestReporter.start();
					}
				} catch (RemoteException e) {
					Logger.log("RemoteException occurs while trying to start request reporter for request " + request.getKey() + ". " + e.getMessage(), LogLevel.Error);
					RequestManager.getInstance().notifyRemoteException(request.getKey(), DB2WorkloadService.getLOCALHOST(), e);
					continue;
				}
			}
		}
		Logger.log("Reporter is turned on. Interval: " + _interval + "s; details: " + _details + "; OutFile: " + _outFile, LogLevel.Info);
	}
	
	/**
	 * Stops the reporter. It cancels the reportCollectTask
	 * And stops all the RequestReporter. 
	 */
	public void turnOff() {
		
		if(this.reportCollectTask != null){
			this.reportCollectTask.cancel();
			this.reportCollectTask = null;
		}
		
		this.status = false;
		
		//set the parameters back to default values 
		this.interval = 60;
		this.details = false;
		this.outFile = null;
		
		//stop all the RequestReporters if exist
		if(RequestManager.getInstance().getOriginatedRequests() != null) {
			Iterator<Entry<Long, RequestRemoteInterface>> requests = RequestManager.getInstance().getOriginatedRequests().entrySet().iterator(); 
			while(requests.hasNext()){
				Entry<Long, RequestRemoteInterface> request = requests.next();
				RequestReporterRemoteInterface requestReporter = null;
				try {
					if((requestReporter = request.getValue().getRequestReporter()) != null) {
						requestReporter.stop();
					}
				} catch (RemoteException e) {
					Logger.log("RemoteException occurs while trying to stop request reporter for request " + request.getKey() + ". " + e.getMessage(), LogLevel.Error);
					RequestManager.getInstance().notifyRemoteException(request.getKey(), DB2WorkloadService.getLOCALHOST(), e);
					continue;
				}
			}
		}
		Logger.log("Reporter is turned off.", LogLevel.Info);
	}
			
	/**
	 * Retrieves the reports from all the running RequestReporters and outputs a report message accordingly.
	 */
	public void pullReports() {
		
		//the sum of all reports from all requests
		Report fullSum = new Report();
		
		//number of reports received from requests. normally equals to the number of running requests.
		int numOfReports = 0;
		
		//the message to write to the out file
	//	StringBuilder outFileMessage = new StringBuilder();
		
		//All running requests
		ConcurrentHashMap<Long, RequestRemoteInterface> requests = RequestManager.getInstance().getOriginatedRequests();
		
		for(Iterator<Entry<Long, RequestRemoteInterface>> it = requests.entrySet().iterator(); it.hasNext(); ){		
			Entry<Long, RequestRemoteInterface> request = it.next();
			
			RequestReporterRemoteInterface requestReporter = null;
			Vector<Report> reports = null;
			
			try {
				if((requestReporter = request.getValue().getRequestReporter()) != null &&
				   (reports = requestReporter.getAndClearReport()) != null){
					
					numOfReports++;
					
					//get the average value of the reports
					Report average = Report.average(reports);
					
					if(this.details){
						//one report message per request
						StringBuilder reportMessage = new StringBuilder();
						reportMessage.append("reqid = " + request.getKey() + "; ");
						reportMessage.append(average.toString());
						Logger.log(reportMessage.toString(), LogLevel.Info);
						
					//	outFileMessage.append(this.dateFormat.format(new Date()) + "\t " );
					//	outFileMessage.append(reportMessage.toString() + "\n");
					//	if(this.outFile != null){
					//		FileLoader.getDefaultFileLoader().appendToFile(outFileMessage.toString(), this.outFile);
					//	}
						
					} else {
						//add the average value to the full summation for summary report 
						fullSum.add(average);
					}
				}
			} catch (RemoteException e) {
				Logger.logException(e);
				RequestManager.getInstance().notifyRemoteException(request.getKey(), DB2WorkloadService.getLOCALHOST(), e);
				continue;
			}		
		}
		if(numOfReports != 0){
			if(!this.details){
				// Summary report with an overall average of all reports
				StringBuilder reportMessage = new StringBuilder();
				reportMessage.append("Total: Number of Requests = " + requests.size() + "; " );
				reportMessage.append(fullSum.toString());
				Logger.log(reportMessage.toString(), LogLevel.Info);
				
			//	outFileMessage.append(this.dateFormat.format(new Date()) + "\t" );
			//	outFileMessage.append(reportMessage.toString() + "\n");
				//if(this.outFile != null){
				//	FileLoader.getDefaultFileLoader().appendToFile(outFileMessage.toString(), this.outFile);
				//}
			}
		}
	}

	/**
	 * The RequestReport reports back the last reports before it terminates
	 * @param id the request id
	 * @param reports the last reports
	 */
	@Override
	public void lastReport(Long id, Vector<Report> reports) {
		if(reports != null && reports.size() > 0){

			StringBuilder reportMessage = new StringBuilder();
			reportMessage.append("Last Report: reqid = " + id + "; ");
			Report average = Report.average(reports);
			reportMessage.append(average.toString());
			//reportMessage.append("\n");
			Logger.log(reportMessage.toString(), LogLevel.Info);
			//if(this.outFile != null){
			//	FileLoader.getDefaultFileLoader().appendToFile(this.dateFormat.format(new Date()) + "\t " + reportMessage.toString(), this.outFile);
			//}	
		}
	}
	
	/**
	 * 
	 * @return the status of the Reporter
	 */
	@Override
	public boolean getStatus() {
		return this.status;
	}

	/**
	 * @return the interval
	 */
	public int getInterval() {
		return this.interval;
	}

	/**
	 * @return the details marker
	 */
	public boolean isDetails() {
		return this.details;
	}

	/**
	 * @return the outFile
	 */
	@Override
	public String getOutFile() {
		return this.outFile;
	}
}
