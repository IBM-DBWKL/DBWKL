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
package com.ibm.dbwkl.helper;

import java.util.Comparator;

import com.ibm.dbwkl.request.ARequest;

/**
 * 
 * The RequestComparator will compare requests by their IDs
 * this is used for sorting the requests in ascending order for the requests list overview
 * and can be used by Collections.sort(List<ARequest>, new RequestComparator());
 */
public class RequestComparator implements Comparator<ARequest>{

	@Override
	public int compare(ARequest req1, ARequest req2) {
		
		return req1.getRid().compareTo(req2.getRid());
	}

}
