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
package com.ibm.db2wkl.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import com.ibm.db2wkl.request.RequestState;

/**
 *
 */
public interface RequestRemoteInterface extends Remote{

	/**
	 * name
	 */
	public static final String BIND_NAME = "com.ibm.db2wkl.request.ThreadedRequest";
	
	/**
	 * @return request reporter's stub
	 * @throws RemoteException 
	 */
	RequestReporterRemoteInterface getRequestReporter() throws RemoteException;
	
	/**
	 * @return request notifier's stub
	 * @throws RemoteException
	 */
	RequestNotifierRemoteInterface getRequestNotifier() throws RemoteException;
	
	/**
	 * @return request state
	 * @throws RemoteException
	 */
	RequestState getState() throws RemoteException;
	
	/**
	 * @return get a list of informations about the workloads 
	 * @throws RemoteException 
	 */
	ArrayList<HashMap<String, String>> getWorkloadInfo() throws RemoteException;
}
