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
package com.ibm.db2wkl.request;

import java.io.PrintStream;
import java.net.Socket;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.ibm.db2wkl.DB2WorkloadServiceDirectory;
import com.ibm.db2wkl.helper.CryptionModule;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.rmi.ReporterRemoteInterface;
import com.ibm.db2wkl.rmi.RequestManagerRemoteInterface;

/**
 *
 */
public class RequestPerformer {

	/**
	 * the stub of request manager
	 */
	private static RequestManagerRemoteInterface requestManagerStub = null;
	
	
	/**
	 * the stub of Reporter
	 */
	private static ReporterRemoteInterface reporterStub = null;
	
	/**
	 * @param args host, port, directory, request info
	 */
	public static void main(String[] args) {
		
		try {
			
			// init the security manager
			if(System.getSecurityManager() == null){
				System.setSecurityManager(new RMISecurityManager());
			}
			//monitor the creation of socket within this JVM
			Socket.setSocketImplFactory(MonitoredSocketImplFactory.getInstance());
			
			//setup RMI Connection
			//host/port of request's originator
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			//host/port of local machine
			String hostLocal = args[2];
			int portLocal = Integer.parseInt(args[3]);
			
			//in case of incoming request(request's originator is not local machine), 
			//the request should notify the local machine about the creation of this request
			RequestManagerRemoteInterface localRequestManagerStub = null;
			if(!host.equalsIgnoreCase(hostLocal)){
				localRequestManagerStub = (RequestManagerRemoteInterface) getStub(hostLocal, portLocal, RequestManagerRemoteInterface.BIND_NAME);
			}
			requestManagerStub = (RequestManagerRemoteInterface) getStub(host, port, RequestManagerRemoteInterface.BIND_NAME);
			reporterStub = (ReporterRemoteInterface) getStub(host, port, ReporterRemoteInterface.BIND_NAME);
			Logger.init(host, port);
			
			//initialize all directory parameters in the new JVM
			DB2WorkloadServiceDirectory.init(args[4]);
			
			//Reconstruct the request from arguments
			ARequest aRequest = new ARequest(Arrays.copyOfRange(args, 5, args.length));
			Request request = new Request();
			request.init(aRequest);
			
			Thread.currentThread().setName(request.getName());
			
			// reassign the System.out to a file
			String dateTime = new SimpleDateFormat("-yyyymmddhhmmss").format(new Date());
			PrintStream ps = new PrintStream(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryLogging()
					+ "/" + request.getName() + dateTime + ".log");
			System.setOut(ps);
			System.setErr(ps);
			
			//decrypt the password when necessary
			if(request.hasRequestOption(Options.DB_PASSWORD)){
				String password = request.getRequestOption(Options.DB_PASSWORD);
				if(password.startsWith(CryptionModule.SECURITY_PREFIX) || password.startsWith(CryptionModule.PASSFILE_PREFIX)){
					String decrypted = CryptionModule.getInstance().getDecryptedPassword(password);
					request.getParseResult().put(Options.DB_PASSWORD, decrypted);
				}
			}
			
			//Setup request reporter
			RequestReporter requestReporter = new RequestReporter(request);
			requestReporter.init();
			request.setRequestReporter(requestReporter);
	
			//setup request notifier
			RequestNotifier requestNotifier = new RequestNotifier(request);
			request.setRequestNotifier(requestNotifier);
		
			requestManagerStub.addRequest(request.getRid(), host, request.getRemoteObject());
			if(localRequestManagerStub != null){
				//in case this request is an incoming request, it is also added to the local map of request, so that the local machine can 
				//detect whether this JVM is successfully started.
				localRequestManagerStub.addRequest(request.getRid(), host, request.getRemoteObject());	
			}
			
			Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
			//start request notifier as a thread.
			Thread requestNotifierThread = new Thread(requestNotifier, requestNotifier.getName());
			requestNotifierThread.setDaemon(true);
			requestNotifierThread.start();
			
			//request will run within the current thread.
			request.run();
			
			try {
				requestNotifierThread.join();
			} catch (InterruptedException e) {
				//
			}
			
			//if it's incoming request, notify the local requestManager about the finish of the request so that it will be removed from the map
			//it doesn't matter if it's finished, brokendown of stopped.
			if(localRequestManagerStub != null){
				localRequestManagerStub.notify(request.getRid(), request.getOrigin_host(), RequestState.FINISHED.name(), 0, null);
			}
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			System.exit(1);
		} 
	}
	
	/**
	 * @param host 
	 * @param port 
	 * @param name 
	 * @return stub 
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws AccessException 
	 */
	public static Remote getStub(String host, int port, String name) throws AccessException, RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry(host, port);
		Remote remote = registry.lookup(name);
		return remote;
	}
	
	/**
	 * @return reporter stub
	 */
	public static ReporterRemoteInterface getReporterStub(){
		return reporterStub;
	}
	
	/**
	 * @return request manager stub
	 */
	public static RequestManagerRemoteInterface getRequestManagerStub(){
		return requestManagerStub;
	}

}
