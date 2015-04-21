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
package com.ibm.db2wkl.request.internal;


/**
 * An Object with all required information for the outformat text and xml
 *
 */
public class ResultObject {

	/**
	 * Request Type
	 */
	private String type;
	/**
	 * object number
	 */
	private int number;
	/**
	 * request status
	 */
	private String status;
	/**
	 * request id
	 */
	private String requestID;
	/**
	 * workload id
	 */
	private String wklID;
	/**
	 * start time of the request
	 */
	private String start_time;
	/**
	 * end time of the request
	 */
	private String end_time;
	/**
	 * message in case it is set
	 */
	private String message;
	/**
	 * list of all workloads within the request
	 */
	private String list_of_workloads;
	
	/**
	 * ResultObject for stop
	 * @param requestID 
	 * @param id 
	 */
	public ResultObject(String requestID){
		this.requestID = requestID;
	}
	
	/**
	 * ResultObject for the request list
	 * @param type 
	 * @param number 
	 */
	public ResultObject(String type, int number){
		this.type = type;
		this.number = number;
	}
	
	/**
	 * ResultObject for the request requestID
	 * @param requestID 
	 * @param wklID 
	 * @param status 
	 * @param start_time 
	 * @param end_time 
	 * @param type 
	 * @param list_of_workloads 
	 */
	public ResultObject(String requestID, String wklID, String status, String start_time, String end_time, String type, String list_of_workloads){
		this.requestID = requestID;
		this.wklID = wklID;
		this.status = status;
		this.start_time = start_time;
		this.end_time = end_time;
		this.type = type;
		this.list_of_workloads = list_of_workloads;	
	}

	/**
	 * ResultObject for the request list executing/stopped/brokedown/finished/stored || request list detail
	 * @param requestID 
	 * @param number
	 * @param status 
	 * @param start_time 
	 * @param end_time 
	 * @param type 
	 */
	public ResultObject(String requestID, String status, String start_time, String end_time, String type){
		this.requestID = requestID;
		this.status = status;
		this.start_time = start_time;
		this.end_time = end_time;
		this.type = type;
	}
	
	/**
	 * @return Type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * @return number
	 */
	public int getNumber() {
		return this.number;
	}

	/**
	 * @return status
	 */
	public String getStatus() {
		return this.status;
	}
	
	/**
	 * @return requestID
	 */
	public String getRequestID() {
		return this.requestID;
	}
	
	/**
	 * @return workloadID
	 */
	public String getWklID() {
		return this.wklID;
	}

	/**
	 * @return start time
	 */
	public String getStart_time() {
		return this.start_time;
	}

	/**
	 * @return end time
	 */
	public String getEnd_time() {
		return this.end_time;
	}

	/**
	 * @return list of workloads
	 */
	public String getList_of_workloads() {
		return this.list_of_workloads;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}
		
}


