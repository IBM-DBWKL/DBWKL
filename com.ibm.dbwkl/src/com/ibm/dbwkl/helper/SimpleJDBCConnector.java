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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.dbwkl.DB2WorkloadServiceException;

/**
 * <p>This utility encapsulate the creation of a simple JDBC connection. It
 * will simply create it, and log some messages.</p>
 * 
 */
public class SimpleJDBCConnector {
	
	/**
	 * This method returns a new JDBC connection. This method should only be called, when a 
	 * connection is possible (use the ConnectionValidator to check this).
	 * 
	 * @param user for the connection
	 * @param password for the user
	 * @param url to the database
	 * @return a new connection
	 */
	public static Connection getConnection(String url, String user, String password)
	{
		Connection connection = null;
		
		try {
			
			if(user == null || password == null)
				connection = DriverManager.getConnection(url);
			else	
				connection = DriverManager.getConnection(url, user, password);
			
			connection.setAutoCommit(false); 			// no auto commit (best practice)
		} 
		catch (SQLException e) {
			
			throw new DB2WorkloadServiceException(e);
		} 

		return connection;
	}

	/**
	 * This method returns a list of new JDBC connection. This method should only be called, when a 
	 * connection is possible (use the ConnectionValidator to check this).
	 * @param user for the connection
	 * @param password for the user
	 * @param urls - to get every url for the database
	 * @return a list of new connections
	 */
	public static List<Connection> getConnections(ArrayList<String> urls, String user, String password) {

		ArrayList<Connection> connections = new ArrayList<Connection>();
		
		for(String url: urls)
			connections.add(getConnection(url, user, password));
		
		return connections;
	}
}