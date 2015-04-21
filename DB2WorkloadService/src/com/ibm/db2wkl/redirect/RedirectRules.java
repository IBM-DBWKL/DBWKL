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
package com.ibm.db2wkl.redirect;

import com.ibm.db2wkl.request.ARequest;
import com.ibm.db2wkl.request.parser.Options;

/**
 *
 */
public class RedirectRules {

	/**
	 * @param request
	 * @return whether the request is redirectable
	 */
	public static boolean redirectable(ARequest request){
		boolean redirectable = false;
		
		if(request.getAction().equalsIgnoreCase(Options.WKL_WORKLOAD) || request.getAction().equalsIgnoreCase(Options.SP_SP) || request.getAction().equalsIgnoreCase(Options.SQL_SQL)){
			redirectable = true;
		} else {
			redirectable = false;
		}
		return redirectable;
	}

}
