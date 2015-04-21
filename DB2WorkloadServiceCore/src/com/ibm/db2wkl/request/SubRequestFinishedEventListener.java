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

import java.util.EventListener;

/**
 *
 */
public interface SubRequestFinishedEventListener extends EventListener {

	/**
	 * This event is generated when a sub request has finished the execution
	 * 
	 * @param subRequest the sub request that has finished
	 */
	public void subRequestFinished(ASubRequest subRequest);
	
}
