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
 */
public interface RequestNotifierRemoteInterface extends Remote{
	
	/**
	 * name
	 */
	public static final String BIND_NAME = "com.ibm.db2wkl.request.RequestNotifier";
	
	/**
	 * @param stop
	 * @throws RemoteException 
	 */
	void setStop(boolean stop) throws RemoteException;
}
