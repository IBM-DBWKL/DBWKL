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
package com.ibm.dbwkl.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

import com.ibm.dbwkl.report.Report;

/**
 *
 */
public interface RequestReporterRemoteInterface extends Remote{
	
	/**
	 * name
	 */
	public static final String BIND_NAME = "com.ibm.dbwkl.request.RequestReporter";
	
	/**
	 * start the RequestReporterTask
	 * @throws RemoteException 
	 */
	public void start() throws RemoteException;
	
	/**
	 * stop the RequestReporterTask
	 * @throws RemoteException 
	 */
	public void stop() throws RemoteException;
	
	/**
	 * @return the reports
	 * @throws RemoteException 
	 */
	public Vector<Report> getAndClearReport() throws RemoteException;
	
	/**
	 * @return the latest report
	 * @throws RemoteException
	 */
	public Report getLatestReport() throws RemoteException;
	
}
