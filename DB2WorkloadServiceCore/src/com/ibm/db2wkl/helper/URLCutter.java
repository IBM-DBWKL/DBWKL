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
package com.ibm.db2wkl.helper;

import java.net.MalformedURLException;
import java.util.regex.Pattern;

/**
 * <p>This class takes a given URL string like <code>jdbc:db2://localhost:50000/Test</code>
 * and provides functions to get each part of this URL by its own (like the port number or
 * the server name).</p>
 * 
 * <p>This class could handle connection type 2 and 4. It will return null if a requested value
 * is not specified in the URL (like the port number in the type 2 connection string <code>
 * jdbc:db2:Test</code>).</p>
 * 
 */
public class URLCutter {
	
	/** The structure of a type 4 connection:                                        */
	/** jdbc:db2://localhost:50000/Test:retrieveMessagesFromServerOnGetMessage=true; */
	/** HEADER__://SERVER___:PORT_/DATABASE_________________________________________ */
	/** START   ://11       :     /                                              END */
	
	/** The structure of a type 2 connection:                      */
	/** jdbc:db2:Test:retrieveMessagesFromServerOnGetMessage=true; */
	/** HEADER__:DATABASE_________________________________________ */
	/** START   :8                                             END */

	private final String url;
	
	/**
	 * @param url string (type 2 or 4)
	 * @throws MalformedURLException 
	 */
	public URLCutter(String url) throws MalformedURLException {
		if(!isType2URL(url) && !isType4URL(url))
			throw new MalformedURLException("The specified URL " + url +  " is invalid.");
		this.url = url;
	}
	
	/**
	 * @return the port number or null if it is not specified in the URL string
	 */
	public int getPort() {
		
		String port = "0";
		
		if(isType4URL(this.url))
			port = this.url.substring(this.url.indexOf(":", 12) + 1, this.url.indexOf("/", 12));

		return Integer.parseInt(port);
	}
	
	/**
	 * @return the server name or null if it is not specified in the URL string
	 */
	public String getServer() {
		
		String server = "";
		
		if(isType4URL(this.url))
			server = this.url.substring(11, this.url.indexOf(":", 12));
		
		return server;
	}
	
	/**
	 * @return the database name or null if it is not specified in the URL string
	 */
	public String getDatabaseName() {
		
		String databaseName = "";
		
		if(isType4URL(this.url))
			databaseName = this.url.substring(this.url.indexOf("/", 12) + 1, this.url.length());
		
		if(isType2URL(this.url))
			databaseName = this.url.substring(this.url.indexOf(":", 8) + 1, this.url.length());
		
		return databaseName;
	}

	/**
	 * @return the node name
	 */
	public String getNode() {

		String nodeName = "";
		
		if(isType4URL(this.url))
			nodeName = this.url.substring(this.url.indexOf("://") + 3, this.url.indexOf(".", this.url.indexOf("://") + 4));

		return nodeName;
	}
	
	/**
	 * This method validates a URL. It will return true,
	 * if the URL is a type 2 connection.
	 * 
	 * @param url to test
	 * @return true if it is a type 2 URL, false in any other case
	 */
	public static boolean isType2URL(String url)
	{
		// create two regular expressions
		Pattern pattern = Pattern.compile("jdbc:[a-z_0-9]*:[a-z_0-9_.]*", Pattern.CASE_INSENSITIVE);

		if(url == null)
			return false;

		return pattern.matcher(url).matches();
	}
	
	/**
	 * This method validates a URL. It will return true,
	 * if the URL is a type 4 connection.
	 * 
	 * @param url to test
	 * @return true if it is a type 4 URL, false in any other case
	 */
	public static boolean isType4URL(String url)
	{
		// create two regular expressions
		Pattern pattern = Pattern.compile("jdbc:[a-z_0-9]*://[a-z_0-9_.]*:[0-9]*/[a-z_0-9]*", Pattern.CASE_INSENSITIVE);

		if(url == null)
			return false;

		return pattern.matcher(url).matches();
	}
}