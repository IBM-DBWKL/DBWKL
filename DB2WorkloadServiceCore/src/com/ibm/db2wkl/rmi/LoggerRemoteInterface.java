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
public interface LoggerRemoteInterface extends Remote{
	
	/**
	 * 
	 */
	public static String BIND_NAME = "com.ibm.db2wkl.logging.Logger";
	/**
	 * @param message 
	 * @param requestName 
	 * @param level 
	 * @param classe 
	 * @param method 
	 * @param lineNumber 
	 * @param thread 
	 * @param threadGroup 
	 * @throws RemoteException
	 */
	void Log(String message, String requestName, String level, String classe, String method, int lineNumber, String thread, String threadGroup) throws RemoteException;

}
