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

import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.request.RequestState;
import com.ibm.dbwkl.rmi.RequestNotifierRemoteInterface;


/**
 * The Requestnotifier informed the Request that the user want to stop a request.
 * It means that the Request and the active workloads must stopped.
 * 
 * 
 **/
public class RequestNotifier implements RequestNotifierRemoteInterface, Runnable {
	
	/**
	 * request
	 */
	private Request request;
	
	/**
	 * The name of the Request notifier which started with reqn 
	 */
	private String name;
	
	/**
	 * is a signal that the request should stop now
	 */
	private boolean stop = false;
	
	/**
	 * Number of current active connections to remember in order to print
	 * a change to this to the log
	 */
	private int currentNoOfActiveConnections = 0;
	
	/**
	 * @param BIND_NAME
	 * @param request
	 */
	public RequestNotifier(Request request){
		this.request = request;
		this.name = "reqn" + request.getRid();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run(){	
		while(true){
			if(this.request.getRequestState() == RequestState.BROKEDOWN || 
					this.request.getRequestState() == RequestState.FINISHED || 
					this.request.getRequestState() == RequestState.STOPPED ){
				Logger.log("The Requestnotifier will close.", LogLevel.Debug);
				break;
			}
			
			if(this.stop == true && this.request.getRequestState() != RequestState.STOPPING){
				this.request.setRequestState(RequestState.STOPPING);

				//stop the request
				this.request.stopRequest();
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Logger.log(e.getMessage(), LogLevel.Error);
			}
			
			if (Request.getDataSource() != null) {
				if (this.currentNoOfActiveConnections != Request.getDataSource().getNumberOfActiveConnections()) {
					this.currentNoOfActiveConnections = Request.getDataSource().getNumberOfActiveConnections();
					Logger.log("number of active connections changed to: " + this.currentNoOfActiveConnections, LogLevel.Debug);
					
				}
			}
		}
	}

	/**
	 * @return request 
	 */
	public Request getRequest() {
		return this.request;
	}

	/**
	 * @return stop
	 */
	public boolean getStop() {
		return this.stop;
	}

	/**
	 * @param stop
	 */
	@Override
	public void setStop(boolean stop) {
		this.stop = stop;
	}

	/**
	 * @return name
	 */
	public String getName(){
		return this.name;
	}

}
