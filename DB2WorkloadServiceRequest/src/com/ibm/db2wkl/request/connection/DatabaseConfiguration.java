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
package com.ibm.db2wkl.request.connection;

/**
 * This class represents an entry in a DBCONFIG file. An entry looks like:
 * 
 *  <p><code>[OMP1DA11]</code></p>
 *	<p><code>ssid = DA11</code></p>
 *	<p><code>hostname = boeomp1</code></p>
 *	<p><code>port = 10511</code></p>
 *	<p><code>location = OMPDA11</code></p>
 *	<p><code>groupname =</code></p>
 *	<p><code>version = V100</code></p>
 *	<p><code>applid = ipo1d2c</code></p>
 *	<p><code>wlm = DSNFPERVA</code></p>
 *	<p><code>vcat=DSNA11</code></p>
 *	<p><code>racf=no</code></p>
 */
public class DatabaseConfiguration {

	/**
	 * ID of the configuration
	 */
	String id; 
	
	/**
	 * SSID
	 */
	String ssid;
	
	/**
	 * Host name
	 */
	String hostname;
	
	/**
	 * Port
	 */
	String port;
	
	/**
	 * Location name
	 */
	String location;
	
	/**
	 * Group name
	 */
	String groupname;
	
	/**
	 * Version
	 */
	String version;
	
	/**
	 * applid
	 */
	String applid;
	
	/**
	 * WLM environment
	 */
	String wlm;
	
	/**
	 * VCAT
	 */
	String vcat;
	
	/**
	 * RACF
	 */
	String racf;

	/**
	 * Default constructor
	 * 
	 * @param id
	 */
	public DatabaseConfiguration(String id) {
		
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		
		return "Configuration for ID " + this.id + ":\n\n" +
		       "\tSSID: " + this.ssid + "\n" + 
		       "\tHOST: " + this.hostname + "\n" + 
		       "\tPROT: " + this.port + "\n" + 
		       "\tLOCATION: " + this.location + "\n" + 
		       "\tGROUPNAME: " + this.groupname + "\n" + 
		       "\tVERSION: " + this.version + "\n" + 
		       "\tAPPLID: " + this.applid + "\n" + 
		       "\tWLM: " + this.wlm + "\n" + 
		       "\tVCAT: " + this.vcat + "\n" + 
		       "\tRACF: " + this.racf + "\n";
	}
}