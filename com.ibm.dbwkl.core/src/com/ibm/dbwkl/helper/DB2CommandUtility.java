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

import java.io.IOException;

/**
 * This class provides functions to create DB2 commands
 * to the local DB2 system. YOu can use it to catalog a
 * new remote database or to create a new local database
 * for logging.
 * 
 */
public class DB2CommandUtility {
    
	/**
	 * <p>This method catalogs a new (remote) database to the local db2 database.</p>
	 *
	 * @param host e.g. boeomp2.boeblingen.de.ibm.com
	 * @param port e.g. 10521
	 * @param locationname e.g. OMPDA21
	 * @param nodename e.g. boeomp2
	 * @param alias1 could be null
	 * @return the exit value of the started DB2 CMD process
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public int catalogRemoteDB(final String host, final String port, final String locationname, final String nodename, final String alias1) throws IOException, InterruptedException
	{
		Process proccess = null;
		
		// if no alias if given, we use the location name as a default value
		String alias = alias1;
		if(alias == null || alias.equals(""))
			alias = locationname;

		// catalogs the new node to the system
		String exec1 = "DB2CMD /c /w DB2 CATALOG TCPIP NODE " + nodename + " REMOTE " + host + " SERVER " + port + " REMOTE_INSTANCE " + locationname;

		proccess = Runtime.getRuntime().exec(exec1);
		proccess.waitFor();
		proccess.destroy();
		
		// catalogs the new database to the system
		String exec2 = "DB2CMD /c /w DB2 CATALOG DATABASE " + locationname + " as "  + alias + " AT node " + nodename + "";

		proccess = Runtime.getRuntime().exec(exec2);
		proccess.waitFor();
		proccess.destroy();
		
		// refresh the CLP's directory cache
		String exec3 = "DB2CMD /c /w DB2 TERMINATE";

		proccess = Runtime.getRuntime().exec(exec3);
		proccess.waitFor();
		proccess.destroy();
		
		Thread.sleep(5000);
		
		return proccess.exitValue();
	}
	
	/**
	 * <p>This method uncatalogs a (remote) database to the local db2 database.</p>
	 * 
	 * @param database name to uncatalog
	 * @param node to uncatalog
	 * @return the exit value of the started DB2 CMD process
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	protected int uncatalogRemoteDB(final String database, final String node) throws IOException, InterruptedException
	{
		Process proccess = null;

		// catalogs the new node to the system
		proccess = Runtime.getRuntime().exec("DB2CMD /c /w DB2 UNCATALOG DATABASE " + database);
		proccess.waitFor();
		proccess.destroy();
		
		// catalogs the new database to the system
		proccess = Runtime.getRuntime().exec("DB2CMD /c /w DB2 UNCATALOG NODE " + node);	
		proccess.waitFor();
		proccess.destroy();
			
		Thread.sleep(5000);
		
		return proccess.exitValue();
		
	}

	/**
	 * <p>This method creates a local database.</p>
	 * 
	 * @param name of the database
	 * @return the return code (exit value of the process) of the OS
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public int createLocalDB2Database(String name) throws IOException, InterruptedException
	{
		Process proccess = Runtime.getRuntime().exec("DB2CMD /c /w DB2 CREATE DATABASE " + name);
		proccess.waitFor();
		proccess.destroy();
			
		Thread.sleep(5000);
		
		return proccess.exitValue();
	}
	
	/**
	 * <p>This method drops a local database.</p>
	 * 
	 * @param name of the database
	 * @return the return code (exit value of the process) of the OS
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	protected int dropLocalDB2Database(String name) throws IOException, InterruptedException
	{
		Process proccess = Runtime.getRuntime().exec("DB2CMD /c /w DB2 DROP DATABASE " + name);
		proccess.waitFor();
		proccess.destroy();
			
		Thread.sleep(5000);
			
		return proccess.exitValue();
	}
}