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
package com.ibm.dbwkl.workloadtypes.model;


/**
 * This class represents a binary workload class in a JAR file
 * 
 *
 */
public class WorkloadClass {

	/**
	 * package + class name of the class
	 */
	private String name;
	
	/**
	 * Name of the jar file that this class is in
	 */
	private String jarFileName;
	
	/**
	 * The long name of the workload 
	 */
	private String longName;
	
	/**
	 * The short name of the workload
	 */
	private String shortName;
	
	/**
	 * The description of the workload
	 */
	private String description;

	/**
	 * @return the longName
	 */
	public String getLongName() {
		return this.longName;
	}

	/**
	 * @param longName the longName to set
	 */
	public void setLongName(String longName) {
		this.longName = longName;
	}

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return this.shortName;
	}

	/**
	 * @param shortName the shortName to set
	 */
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the jarFileName
	 */
	public String getJarFileName() {
		return this.jarFileName;
	}

	/**
	 * @param jarFileName the jarFileName to set
	 */
	public void setJarFileName(String jarFileName) {
		this.jarFileName = jarFileName;
	}
	
}
