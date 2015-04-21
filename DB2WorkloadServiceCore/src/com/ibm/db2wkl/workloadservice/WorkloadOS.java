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
package com.ibm.db2wkl.workloadservice;

import java.util.ArrayList;

/**
 *
 */
public class WorkloadOS {
	
	/**
	 * z/OS
	 */
	public static final WorkloadOS ZOS = new WorkloadOS("ZOS", "IBM z/OS");
	
	/**
	 * LUW
	 */
	public static final WorkloadOS LUW = new WorkloadOS("LUW", "Linux, Unix and Windows");
	
	/**
	 * Only z/OS
	 */
	public static final ArrayList<WorkloadOS> ONLY_ZOS;
	
	/**
	 * only LUW
	 */
	public static final ArrayList<WorkloadOS> ONLY_LUW;
	
	/**
	 * LUW and z/OS
	 */
	public static final ArrayList<WorkloadOS> LUW_AND_ZOS;
	
	static {
		
		ONLY_ZOS = new ArrayList<WorkloadOS>(1);
		ONLY_LUW = new ArrayList<WorkloadOS>(1);
		LUW_AND_ZOS = new ArrayList<WorkloadOS>(2);
		
		ONLY_ZOS.add(ZOS);
		ONLY_LUW.add(LUW);
		LUW_AND_ZOS.add(LUW);
		LUW_AND_ZOS.add(ZOS);
	}
	
	/**
	 * Name
	 */
	private final String name;
	
	/**
	 * Comment
	 */
	private final String comment;
	
	/**
	 * @param name
	 * @param comment
	 */
	private WorkloadOS(String name, String comment) {
		
		this.name = name;
		this.comment = comment;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		
		return this.name;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		
		return this.comment;
	}
}