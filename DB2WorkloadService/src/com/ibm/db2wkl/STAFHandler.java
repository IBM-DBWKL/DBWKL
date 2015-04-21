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
package com.ibm.db2wkl;

import com.ibm.db2wkl.logging.Logger;
import com.ibm.staf.STAFException;
import com.ibm.staf.STAFHandle;
import com.ibm.staf.STAFResult;

/**
 * Handles the single STAF handle
 * 
 *
 */
public class STAFHandler {
	
	/**
	 * STAF handle for this DB2WKL instance
	 */
	private STAFHandle handle = null;
	
	/**
	 * STAFHandle object that is registered with the STAF process.
	 */
	public final static STAFHandler instance = new STAFHandler();
	
	/**
	 * Default constructor
	 */
	private STAFHandler(){ /**/ }
	
	/**
	 * <p>A unique identifier for the service.</p>
	 */
	private final static String STAF_HANDLE_ID = "STAF/service/db2wkl";
	
	/**
	 * @return the unique STAF handle for the application
	 */
	public STAFHandle getSTAFHandle()
	{
		if (this.handle == null) {
			
			try {
				
				this.handle = new STAFHandle(STAF_HANDLE_ID);
			} 
			catch (STAFException e) {
				
				throw new DB2WorkloadServiceException("Couldn't get STAF handle: " + e.getMessage(), e);
			}
		}
		
		return this.handle;
	}
	
	/**
	 * submit the requestString for the service on host
	 * @param host
	 * @param service
	 * @param requestString
	 * @return STAF result
	 */
	public STAFResult performRequest(String host, String service, String requestString){
		return getSTAFHandle().submit2(host, service, requestString);
	}

	/**
	 * Unregisters the STAF handle when shutting down
	 */
	public void unRegisterSTAFHandle() {

		if(this.handle != null) {
			
			try {
				
				this.handle.unRegister();
				this.handle = null;			// important! or you get a STAFException because
											// the handle is not null but unregistered!
			} 
			catch (STAFException e) {
				
				Logger.logException(e);
			}
		}
	}
}