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

import java.util.HashMap;

import com.ibm.dbwkl.logging.Logger;

/**
 * <p>This class provides a universal Result-object for the service.</p>
 * 
 * <p><b>Note:</b> This class is depreciated. Why? - In most cases it is not
 * useful to return a universal result. A method should return a individual result,
 * like an Integer as a result of a mathematical operation. If a method fails, it 
 * should throw an exception, immediately. If you return result codes, you have to
 * check them, you have to document them and you have to define them far away 
 * from the point where they are used.</p>
 * 
 */
@SuppressWarnings("boxing")
@Deprecated
public class Result {
	/**
	 * Indicates that an operation was successful
	 */
	public static final int OK = 0;
	
	/**
	 * Indicates that an operation was not successful
	 */
	public static final int ERROR = 1;

	/**
	 * Indicates that a status id was not found in the status controller
	 */
	public static final int STATUS_ID_NOT_FOUND = 4004;

	/**
	 * Indicates that the init phase of a workload was successful
	 */
	public static final int INIT_SUCCESSFUL = 4400;

	/**
	 * Indicates that the init phase of a workload was not successful
	 */
	public static final int INIT_NOT_SUCCESSFUL = 4401;
	
	/**
	 * Indicates that the execute phase of a workload was successful
	 */
	public static final int EXECUTE_SUCCESSFUL = 4410;
	
	/**
	 * Indicates that the execute phase of a workload was not successful
	 */
	public static final int EXECUTE_NOT_SUCCESSFUL = 4411;
	
	/**
	 * Indicates that the clean phase of a workload was successful
	 */
	public static final int CLEAN_SUCCESSFUL = 4420;
	
	/**
	 * Indicates that the clean phase of a workload was not successful
	 */
	public static final int CLEAN_NOT_SUCCESSFUL = 4421;
	
	/**
	 * Indicates that the termination phase of a workload was successful
	 */
	public static final int TERMINATE_SUCCESSFUL = 4422;
	
	/**
	 * Indicates that the termination phase of a workload was not successful
	 */
	public static final int TERMINATE_NOT_SUCCESSFUL = 4423;

	/**
	 * Indicates that a workload was resumed
	 */
	public static final int WKL_RESUMED = 4434;
	
	/**
	 * Indicates that a workload was paused
	 */
	public static final int WKL_PAUSED = 4435;
	
	/**
	 * Indicates that a workload was stopped
	 */
	public static final int WKL_STOPPED = 4436;
	
	/**
	 * Indicates that a workload was started
	 */
	public static final int WKL_STARTED = 4437;
	
	/**
	 * Indicates that a workload was created
	 */
	public static final int NEW_WORKLOAD = 4499;
	
	/**
	 * A HashMap to map the error codes to messages
	 */
	public static final HashMap<Integer, String> messages = new HashMap<Integer, String>();

	/**
	 * This static constructor will initialize a HashMap to map error codes 
	 * to a list of standard messages.
	 */
	static
	{
		messages.put(OK, "Please see the individual results to make sure that the workload execution was successful.");
		messages.put(ERROR, "Unknown error.");

		messages.put(STATUS_ID_NOT_FOUND, "A object with this name was not found");
		
		messages.put(INIT_SUCCESSFUL, "Init()-method was successful.");
		messages.put(INIT_NOT_SUCCESSFUL, "Init()-method was not successful.");
		messages.put(EXECUTE_SUCCESSFUL, "Execute()-method was successful.");
		messages.put(EXECUTE_NOT_SUCCESSFUL, "Execute()-method was not successful.");
		messages.put(CLEAN_SUCCESSFUL, "Clean()-method was successful.");
		messages.put(CLEAN_NOT_SUCCESSFUL, "Clean()-method was not successful.");
		messages.put(TERMINATE_SUCCESSFUL, "Terminate()-method was successful.");
		messages.put(TERMINATE_NOT_SUCCESSFUL, "Terminate()-method was not successful.");
		
		messages.put(WKL_RESUMED, "Workload resumed.");
		messages.put(WKL_PAUSED, "Workload paused.");
		messages.put(WKL_STOPPED, "Workload stopped.");
		messages.put(WKL_STARTED, "Workload started.");
		
		messages.put(NEW_WORKLOAD, "New workload instance");
	}


	/**
	 * The message of the result.
	 */
	private String message;
	
	/**
	 * The error code of the result.
	 */
	private int ec;
	
	/**
	 * This constructor will create a new Result object with
	 * the specified (error) code. This object will contain a
	 * standard information message, if a corresponding message
	 * is found in the list. 
	 * 
	 * Note: If  the parameter "autologging" is true, the creation
	 * of the object will be logged automatically.
	 * 
	 * @param ec is the (error) code for the Result object
	 * @param message to put in the object
	 * @param autologging to activate a automatically logging
	 */
	public Result(int ec, String message, boolean autologging)
	{
		this.setEc(ec);
		this.message = message;
		
		if(autologging && message != null && !message.equals(""))
			Logger.log(message, Logger.Info);
	}

	/**
	 * This constructor will create a new Result object with
	 * the specified (error) code. This object will contain a
	 * standard information message, if a corresponding message
	 * is found in the list. 
	 * 
	 * Note: Nothing will be logged with this constructor!
	 * 
	 * @param ec is the (error) code for the Result object
	 * @param message to put in the object
	 */
	public Result(int ec, String message)
	{
		this(ec, message, false);
	}
	
	/**
	 * This constructor will create a new Result object with
	 * the specified (error) code. This object will contain a
	 * standard information message, if a corresponding message
	 * is found in the list. 
	 * 
	 * Note: Nothing will be logged with this constructor!
	 * 
	 * @param ec is the (error) code for the Result object
	 */
	public Result(int ec)
	{
		this(ec, get(ec), false);
	}

	/**
	 * This constructor will create a new Result object with
	 * the specified (error) code. This object will contain a
	 * standard information message, if a corresponding message
	 * is found in the list. 
	 * 
	 * Note: If  the parameter "autologging" is true, the creation
	 * of the object will be logged automatically.
	 * 
	 * @param ec is the (error) code for the Result object
	 * @param autologging to activate a automatically logging
	 */
	public Result(int ec, boolean autologging)
	{
		this(ec, get(ec), autologging);
	}
	
	/**
	 * <p>This method returns the specific message to the
	 * specified error code. If no corresponding message
	 * was found, it will return the string "<i>unknown</i>"
	 * and <i>not</i> null.</p>
	 * 
	 * @param ec
	 * @return the corresponding message to the error code or the string "unknown"
	 */
	public static String get(int ec) 
	{
		String value = messages.get(ec);
		
		if(value == null)
			value = "unknown";
		
		return value;
	}
	
	/**
	 * @returns the message of the result
	 */
	@Override
	public String toString() {
		
		return this.message;
	}

	/**
	 * @param ec the error code to set
	 */
	public void setEc(int ec) {
		this.ec = ec;
	}

	/**
	 * @return the error code
	 */
	public int getEc() {
		return this.ec;
	}
}