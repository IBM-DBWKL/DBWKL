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

import com.ibm.staf.STAFResult;

/**
 * Stephan Arenswald (arens@de.ibm.com)
 *
 */
public interface RequestStateChangedListener {

	/**
	 * The method that is executed when the request changes its state
	 * @param request the request that changed its states
	 * @param result the result of that caused the state change
	 */
	public void notify(ARequest request, STAFResult result);
}
