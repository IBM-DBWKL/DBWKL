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

/**
 *
 */
public interface RequestManagerRemoteInterface extends Remote {
	
	/**
	 * name
	 */
	public static final String BIND_NAME = "com.ibm.db2wkl.request.RequestManager";
	
	/**
	 * @param rid request id
	 * @param host the originator's host name
	 * @param stub request's stub
	 * @throws RemoteException
	 */
	void addRequest(Long rid, String host, RequestRemoteInterface stub) throws RemoteException;
	
	/**
	 * @param rid request id
	 * @param host the originator's host name
	 * @param requestState request's state
	 * @param resultCode the result code
	 * @param changeResult the result that causes the change of state
	 * @throws RemoteException 
	 */
	void notify(Long rid, String host, String requestState, int resultCode, String changeResult) throws RemoteException;
}
