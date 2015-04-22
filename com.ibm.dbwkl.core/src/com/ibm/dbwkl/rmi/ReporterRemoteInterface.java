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
 */
public interface ReporterRemoteInterface extends Remote {
	
	/**
	 * name
	 */
	public static final String BIND_NAME = "com.ibm.dbwkl.report.Reporter";
	
	/**
	 * @return staus of repoter
	 * @throws RemoteException
	 */
	boolean getStatus() throws RemoteException;

	/**
	 * @param id
	 * @param reports
	 * @throws RemoteException 
	 */
	void lastReport(Long id, Vector<Report> reports) throws RemoteException;

	/**
	 * @return out file
	 * @throws RemoteException 
	 */
	String getOutFile() throws RemoteException;
}
