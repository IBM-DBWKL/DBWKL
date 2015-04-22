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

import java.io.File;
import java.io.IOException;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.Date;

import com.ibm.dbwkl.DB2WorkloadServiceDirectory;
import com.ibm.dbwkl.helper.CaseInsensitiveMap;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.Logger;
import com.ibm.dbwkl.report.Reporter;
import com.ibm.dbwkl.request.ARequest;
import com.ibm.dbwkl.request.RMIRegistry;
import com.ibm.dbwkl.request.RequestManager;
import com.ibm.dbwkl.request.parser.Parser2;
import com.ibm.dbwkl.rmi.LoggerRemoteInterface;
import com.ibm.dbwkl.rmi.ReporterRemoteInterface;
import com.ibm.dbwkl.rmi.RequestManagerRemoteInterface;
import com.ibm.staf.STAFResult;
import com.ibm.staf.service.STAFServiceInterfaceLevel30;

/**
 * <p>This is the main class of the DB2WorkloadService.</p>
 * 
 */ 
public class DB2WorkloadService implements STAFServiceInterfaceLevel30 {
	
	/**
	 * The creation date.
	 */
	@SuppressWarnings("unused")
	private final static Date sessionCreationDate = new Date();

	/**
	 * the Logger that is used as stub
	 */
	private Logger remoteLogger;
	
	/**
	 * security policy
	 */
	private static URL securiyPolicy = null;
	
	/**
	 * local host address
	 */
	private static String LOCALHOST = null;
	
	/**
	 * Header for each output on the command line
	 */
//	public final static String header = "\nIBM DB2WorkloadService [Version " + Version.getVersion() + "]\n";

	/**
	 * Default constructor
	 */
	public DB2WorkloadService() {
		// nothing to do
	}

	/* (non-Javadoc)
	 * @see com.ibm.staf.service.STAFServiceInterfaceLevel30#acceptRequest(com.ibm.staf.service.STAFServiceInterfaceLevel30.RequestInfo)
	 */
	@Override
	public STAFResult acceptRequest(RequestInfo arg0) {
		// save for statistics
		String requeststring = arg0.request;
		//cover the password part for logging
		try {
			if(requeststring.contains("password ")) {
				int a = requeststring.indexOf("password ");
				int b = requeststring.indexOf(" ", a + 9);
				if(b==-1) b = requeststring.length();
				requeststring = requeststring.replace(requeststring.substring(a + 9, b), "[*PASSWORD*]");
			} else if(requeststring.contains("encrypt ")) {
				int a = requeststring.indexOf("encrypt ");
				int b = requeststring.indexOf(" ", a + 8);
				if(b==-1) b = requeststring.length();
				requeststring = requeststring.replace(requeststring.substring(a + 8, b), "[*PASSWORD*]");
			}
		} catch (IndexOutOfBoundsException e) {
			return new STAFResult(STAFResult.InvalidRequestString,
					"Error while checking for the PASSWORD option. Make sure that the request is valid");
		}
		if(arg0.isLocalRequest){
			Logger.log("Accept Request: " + requeststring, LogLevel.Info);
		} else {
			Logger.log("Accept Request: '" + requeststring + "' from " + arg0.machine, LogLevel.Info);
		}
		CommandCounter.saveNewRequest(requeststring);
		
		String action = Parser2.getInstance().getAction(arg0.request);
		CaseInsensitiveMap parseResult;
		try {
			parseResult = Parser2.getInstance().parse(action, arg0);
		} catch (Exception e) {
			return new STAFResult(STAFResult.InvalidRequestString, e.getMessage());
		}

		ARequest request = new ARequest(arg0.request, action, parseResult, LOCALHOST, RMIRegistry.INSTANCE.getPort());
		
		return RequestManager.getInstance().startRequest(request);
	}

	/* (non-Javadoc)
	 * @see com.ibm.staf.service.STAFServiceInterfaceLevel30#init(com.ibm.staf.service.STAFServiceInterfaceLevel30.InitInfo)
	 */
	@Override
	public STAFResult init(InitInfo arg0) {
		try {
    		//Get the service´s installation directory
    		File sourcePath = new File(arg0.serviceJar.getName());
    		
			sourcePath = sourcePath.getAbsoluteFile().getParentFile();
    		String installationDirectory = sourcePath.getAbsolutePath();
    		
    		DB2WorkloadServiceDirectory.init(installationDirectory);
    	} 
    	catch (Exception e) {

			throw new RuntimeException(e);
		}
    	
    	//load and set security policy from Jar
		ClassLoader cl = getClass().getClassLoader(); 
		DB2WorkloadService.securiyPolicy = cl.getResource("com/ibm/db2wkl/files/rmi.policy");
		System.setProperty("java.security.policy", DB2WorkloadService.securiyPolicy.toString());
		
		//get the IP address of localhost
		try {
			DB2WorkloadService.LOCALHOST = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		
		//Set RMI security Manager
		if(System.getSecurityManager() == null){
			System.setSecurityManager(new RMISecurityManager());
		}
		
		// add a shutdown hook to make sure that we get informed when
		// STAF shuts down
		ShutDownHook.addShutDownHook();
		
		// set thread name
		Thread.currentThread().setName("DB2WKL");
		
		// set default uncaught exception handler
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
		
		// add logger
		try {
			Logger.init();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
				
		// create a new log session
		Logger.logNewSession();
		
		//setup RMI registry
    	this.remoteLogger = new Logger();
    	try {
    		RMIRegistry.INSTANCE.bind(RequestManagerRemoteInterface.BIND_NAME, RequestManager.getInstance());
    		RMIRegistry.INSTANCE.bind(ReporterRemoteInterface.BIND_NAME, Reporter.getInstance());
    		RMIRegistry.INSTANCE.bind(LoggerRemoteInterface.BIND_NAME, this.remoteLogger);
		} catch (AccessException e) {
			throw new RuntimeException(e);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		} catch (AlreadyBoundException e) {
			throw new RuntimeException(e);
		}
		
		Reporter.getInstance().turnOn();
		
		return new STAFResult(STAFResult.Ok);
	}

	/**
	 * <p>This method is called by the STAF process, when the service is terminated or
	 * by the main()-method if the program is running as a stand-alone version.</p>
	 * 
	 * @return a STAFResult with the return code "OK"
	 */
	@Override
	public STAFResult term() {
		
		ShutDownHook.removeShutDownHook();
		try {
			RMIRegistry.INSTANCE.terminate();
		} catch (AccessException e) {
			Logger.logException(e);
		} catch (RemoteException e) {
			Logger.logException(e);
		} catch (NotBoundException e) {
			Logger.logException(e);
		}
		Logger.term();
		STAFHandler.instance.unRegisterSTAFHandle();
		
		return new STAFResult(STAFResult.Ok);
	}
	
	/**
	 * @return security policy
	 */
	public static URL getSecuriyPolicy(){
		return DB2WorkloadService.securiyPolicy;
	}

	/**
	 * @return the lOCALHOST IP address
	 */
	public static String getLOCALHOST() {
		return LOCALHOST;
	}
}