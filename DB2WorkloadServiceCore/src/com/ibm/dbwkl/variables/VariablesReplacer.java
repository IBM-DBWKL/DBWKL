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
package com.ibm.dbwkl.variables;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.dbwkl.helper.RandomStringGenerator;
import com.ibm.staf.STAFException;
import com.ibm.staf.STAFHandle;
import com.ibm.staf.STAFResult;

/**
 * This class is used to replace system properties or
 * STAF properties in a given string
 * 
 *
 */
public class VariablesReplacer {

	/**
	 * Replaces variables in the given string with system properties
	 * and STAF properties
	 * 
	 * @param input
	 * @return the string with the replaced variables
	 * @throws VariablesReplacerException thrown when the replacement failed du to some reason
	 */
	public String replaceVars(String input) throws VariablesReplacerException {
		String output = input;
		
		Pattern variablePattern = Pattern.compile("\\{(sys|env|staf)\\:([^{}]*)\\}");
		Matcher matcher = variablePattern.matcher(output);
		
		while (matcher.find()) {
					
			// check for the "{<src>:<var>}" eyecatcher using regex
			// get the source for the variable
			// sys  - Java System Property (System.getProperty(...))
			// env  - OS System Environment (System.getenv())
			// staf - STAF variable (IBM/DB2WKL/MYVAR)
			String source = matcher.group(1);
			
			// get the variable name
			String variable = matcher.group(2);
			
			// depending on the source, get the value
			String value = "";
			if (source.equalsIgnoreCase("sys")) {
				if (System.getProperty(variable) != null) {
					value = System.getProperty(variable);
				} else {
					throw new VariablesReplacerException("The variable " + variable + " is not defined as system property");
				}
			} else if (source.equalsIgnoreCase("env")) {
				if (System.getenv(variable) != null) {
					value = System.getenv(variable);
				} else {
					throw new VariablesReplacerException("The variable " + variable + " is not defined in the system evnironment");
				}
			} else if (source.equalsIgnoreCase("staf")) {
				try {
					STAFHandle stafHandle = new STAFHandle("db2wkl_sql_" + RandomStringGenerator.get(3));
					STAFResult result = stafHandle.submit2("local", "VAR", "GET SYSTEM VAR " + variable);
					if (result.rc == STAFResult.Ok) {
						value = result.result;
					} else {
						throw new VariablesReplacerException("The variable " + variable + " is not defined in STAF. Only system variables are supported.");
					}
				} catch (STAFException e) {
					throw new VariablesReplacerException("Calling STAF LOCAL VAR GET VAR ... failed");
				}
			} else {
				throw new VariablesReplacerException("Unknown source " + (matcher == null ? "null" : matcher.group(0)) + " in " + input);
			}
		
			output = matcher.replaceFirst(value);
		}
		
		return output;
	}
	
}
