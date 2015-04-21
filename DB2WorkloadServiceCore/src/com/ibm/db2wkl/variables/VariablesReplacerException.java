package com.ibm.db2wkl.variables;

/**
 * An exception that occurs in the variable replacer
 * 
 *
 */
public class VariablesReplacerException extends Exception {

	/**
	 * generate SUID 
	 */
	private static final long serialVersionUID = -8618862717246565633L;
	
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
	@SuppressWarnings("unused")
	private final static String COPYRIGHT = "Licensed Materials - Property of IBM\n"
			+ "Copyright IBM Corp. 2012 All Rights Reserved.\n"
			+ "US Government Users Restricted Rights - Use, duplication or\n"
			+ "disclosure restricted by GSA ADP Schedule Contract with\n"
			+ "IBM Corp.";

	/**
	 * Default constructor
	 * @param message the exception message
	 */
	public VariablesReplacerException(String message) {
		super(message);
	}
	
}
