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
package com.ibm.dbwkl.request;

import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.request.Logger;
import com.ibm.dbwkl.request.RequestState;

/**
 *
 */
public class UncaughtExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {

	/* (non-Javadoc)
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		try {
			Logger.log("UncaughtException from thread " + thread.getName() + ": " + throwable.getMessage(), LogLevel.Error);
			for(StackTraceElement element: throwable.getStackTrace()) {
				Logger.log("UncaughtException : stack element: " + element.getClassName() + "." + element.getMethodName() + " (" + element.getLineNumber() + ")", Logger.Debug);
			}
			Request.getRequest().setRequestState(RequestState.BROKEDOWN);
		} finally {
			throwable.printStackTrace(System.err);
//			System.exit(1);
		}
	}
}
