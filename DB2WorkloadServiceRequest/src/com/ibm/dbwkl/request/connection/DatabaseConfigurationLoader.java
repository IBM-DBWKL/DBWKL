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
package com.ibm.dbwkl.request.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.ibm.dbwkl.DB2WorkloadServiceDirectory;
import com.ibm.dbwkl.helper.FileLoader;
import com.ibm.dbwkl.request.Logger;

/**
 * Database configuration loader
 * 
 * 
 *
 */
public class DatabaseConfigurationLoader {
	
    /**
     * Singleton databaseConfigurationLoader instance
     */
    private static DatabaseConfigurationLoader databaseConfigurationLoader = null;

    /**
     * List of database configuration files
     */
    private ArrayList<String> filesList;
    
    /**
     * private default constructor for singleton DatabaseConfigurationLoader
     */
    private DatabaseConfigurationLoader() {
    	this.filesList = new ArrayList<String>();
    }
    
    /**
     * @param files separated by ';'
     */
    public void loadFiles(String files) {
		
    	StringTokenizer tokenizer = new StringTokenizer(files, ";");
    	String file = "";
    	
    	while(tokenizer.hasMoreTokens()){
    		file = tokenizer.nextToken().trim();
    		if(!this.filesList.contains(file)){ 
    			this.filesList.add(file);
    		}
    	}
	}

	/**
     * This method will load the configuration for the specified CFGID out of the configuration
     * files (specified in the STAF.cfg file). It will create a type 2 or type 4 URL. It will 
     * return null, if the CFGID was not found.
     * 
     * @param cfgid to load the URL from
     * @param type 2 or 4 URL or null if the ID was not found
     * @return a URL or null if the ID was not found
     */
	public String loadUrl(String cfgid, String type)
	{
		Logger.log("Loading URL from database configuration for ID " + cfgid, Logger.Info);
		
		return createURL(getDatabaseConfiguration(loadDatabaseConfigurations(), cfgid), type);
	}
	
	/**
     * This method will load the configuration for the specified CFGIDs out of the configuration
     * files (specified in the STAF.cfg file). It will create type 2 or type 4 URLs. 
     * It will return null, if one of the CFGID was not found.
     * 
     * @param cfgids to load the URL from
     * @param type 2 or 4 URL or null if the ID was not found
     * @return a URL or null if the ID was not found
     */
	public String loadUrls(String cfgids, String type)
	{	
		String urls = "";
		String url = null;
		String[] cfgIds = cfgids.split(";");
		ArrayList<DatabaseConfiguration> databaseConfigurationList = loadDatabaseConfigurations();
		
		for(String cfgId:cfgIds) {
			Logger.log("Loading URL from database configuration for ID " + cfgId, Logger.Info);
			url = createURL(getDatabaseConfiguration(databaseConfigurationList, cfgId), type);
			if(url==null) return null;
			urls += url + ";";
		}
		
		return urls;
	}


    /**
     * This method will load the configuration for the specified SSID and HOST out of the configuration
     * files (specified in the STAF.cfg file). It will create a type 2 or type 4 URL. It will 
     * return null, if the CFGID was not found.
     * 
     * @param ssid to load the URL from
     * @param host to load the URL from
     * @param type 2 or 4 URL or null if the ID was not found
     * @return a URL or null if the ID was not found
     */
	public String loadUrl(String ssid, String host, String type)
	{
		return createURL(getDatabaseConfiguration(loadDatabaseConfigurations(), ssid, host), type);
	}
	
	/**
	 * This method will create a valid URL string with a specified DBConfig object
	 * and the specified type (2 or 4).
	 * 
	 * @param config object to create the URL with
	 * @param type 2 or 4 (could be null for default type 4)
	 * @return a valid URL or null if the DBConfig object or a needed field was null
	 */
	private String createURL(DatabaseConfiguration config, String type)
	{
		String _type = type;
		
		if(_type == null)
			_type = "4";
			
		if(config != null)
		{
			if(_type.equalsIgnoreCase("2") && config.location != null)
				return "jdbc:db2:" + config.location;
			
			else if(config.hostname != null && config.port != null && config.location != null)
				return "jdbc:db2://" + config.hostname + ":" + config.port + "/" + config.location;	
		}
		
		return null;
	}

	/**
	 * This method will return the corresponding DBConfig object to the ssid and host
	 * or null if it was not found in the DBCONFIG file.
	 * 
	 * @param configs database configurations 
	 * @param ssid to search a corresponding entry in the DBCONFIG
	 * @param host to search a corresponding entry in the DBCONFIG
	 * @return the DatabaseConfiguration object or null if it was not found
	 */
	private DatabaseConfiguration getDatabaseConfiguration(ArrayList<DatabaseConfiguration> configs, String ssid, String host)
	{
		DatabaseConfiguration databaseConfiguration = null;
		
		for(DatabaseConfiguration c: configs) {
			if(c.ssid.toLowerCase().equalsIgnoreCase(ssid.toLowerCase()) && c.hostname.toLowerCase().equalsIgnoreCase(host.toLowerCase())) {
				Logger.log("Found configuration for HOST " + host + " and SSID " + ssid, Logger.Debug);
				
				databaseConfiguration = c;
				break;
			}
		}
		
		return databaseConfiguration;
	}

	/**
	 * This method will return the corresponding DBConfig object to the cfgid
	 * or null if it was not found in the DBCONFIG file.
	 * 
	 * @param configs database configurations 
	 * @param cfgid to search a corresponding entry in the DBCONFIG
	 * @return the DBConfig object or null if it was not found
	 */
	private DatabaseConfiguration getDatabaseConfiguration(ArrayList<DatabaseConfiguration> configs, String cfgid) {
		Logger.log("Getting database configuration for ID " + cfgid + " in " + configs.size() + " objects", Logger.Debug);
		
		DatabaseConfiguration config = null;
		
		for(DatabaseConfiguration c: configs) {
			if(c.id.equalsIgnoreCase(cfgid)) {
				Logger.log("Found configuration for ID " + cfgid, Logger.Debug);
				
				config = c;
				break;
			}
		}
		
		return config;
	}
	
	/**
	 * This method will load and return all DBConfig objects in the DBCONFIG files, specified in the
	 * STAF.cfg (like <code>SET SYSTEM VAR DBWKL/DBConfig="N:/petst/staf/cfg/ompdb.cfg;"</code>).
	 * An entry looks like:
	 * <p><code>
	 *  [{id}]<br>
	 *	ssid = {subsystem id}<br>
	 *	hostname = {hostname}<br>
	 *	port = {DB2 port}<br>
	 *	location = {location name or database name}<br>
	 *	groupname = {group name (i.e. secondary user id)}<br>
	 *	version = {DB2 version}<br>
	 *	applid = {applid}<br>
	 *	wlm = {wlm AS}<br>
	 *	vcat = {?}<br>
	 *	racf = {whether this is RACF enabled system}<br>
	 * </code></p>
	 *
	 * @return a list of DBConfig objects (could be empty if no file or entry was found)
	 */
	private ArrayList<DatabaseConfiguration> loadDatabaseConfigurations() {
		
		ArrayList<File> loadedFiles = new FileLoader(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryConfig()).loadFiles(this.filesList);
		ArrayList<DatabaseConfiguration> configs = new ArrayList<DatabaseConfiguration>();
		BufferedReader reader = null;
		
		for(File file: loadedFiles)
		{
			try
			{
				reader = new BufferedReader(new FileReader(file));
				
				String line = null;
				DatabaseConfiguration tmp = null;
				
				while((line = reader.readLine()) != null)
				{
					try
					{
						if(line.length() >= 2 && line.substring(0, 1).equalsIgnoreCase("["))
						{
							tmp = new DatabaseConfiguration(line.substring(1, line.length() - 1).trim());
							Logger.log("Found database configuration for ID " + line.substring(1, line.length() - 1).trim(), Logger.Debug);
						}
						
						else if(tmp != null && line.length() > 4 && line.substring(0, 4).equalsIgnoreCase("ssid"))
							tmp.ssid = line.substring(line.indexOf("=") + 1, line.length()).trim();
							
						else if(tmp != null && line.length() > 8 && line.substring(0, 8).equalsIgnoreCase("hostname"))	
							tmp.hostname = line.substring(line.indexOf("=") + 1, line.length()).trim();
							
						else if(tmp != null && line.length() > 4 && line.substring(0, 4).equalsIgnoreCase("port"))
							tmp.port = line.substring(line.indexOf("=") + 1, line.length()).trim();
						
						else if(tmp != null && line.length() > 8 && line.substring(0, 8).equalsIgnoreCase("location"))	
							tmp.location = line.substring(line.indexOf("=") + 1, line.length()).trim();
						
						else if(tmp != null && line.length() > 9 && line.substring(0, 9).equalsIgnoreCase("groupname"))
							tmp.groupname = line.substring(line.indexOf("=") + 1, line.length()).trim();
						
						else if(tmp != null && line.length() > 7 && line.substring(0, 7).equalsIgnoreCase("version"))	
							tmp.version = line.substring(line.indexOf("=") + 1, line.length()).trim();
						
						else if(tmp != null && line.length() > 6 && line.substring(0, 6).equalsIgnoreCase("applid"))	
							tmp.applid = line.substring(line.indexOf("=") + 1, line.length()).trim();
						
						else if(tmp != null && line.length() > 3 && line.substring(0, 3).equalsIgnoreCase("wlm"))	
							tmp.wlm = line.substring(line.indexOf("=") + 1, line.length()).trim();
						
						else if(tmp != null && line.length() > 4 && line.substring(0, 4).equalsIgnoreCase("vcat"))	
							tmp.vcat = line.substring(line.indexOf("=") + 1, line.length()).trim();
						
						else if(tmp != null && line.length() > 4 && line.substring(0, 4).equalsIgnoreCase("racf"))
							tmp.racf = line.substring(line.indexOf("=") + 1, line.length()).trim();
						
						else
						{
							if(tmp != null)
							{
								configs.add(tmp);
								tmp = null;
							}
						}
					}
					catch(IndexOutOfBoundsException e)
					{
						// shit happens, especially when a key got no value: host = 
					}
				}
				
				// we must add the last object extra
				if(tmp != null)
				{
					configs.add(tmp);
					tmp = null;
				}
			} 
			catch (FileNotFoundException e)
			{
				this.filesList.remove(file.getAbsolutePath());
				Logger.logException(e);
			} 
			catch (IOException e)
			{
				Logger.logException(e);
			}
			finally
			{
				if(reader != null)
				{
					try
					{
						reader.close();
					} 
					catch (IOException e)
					{
						//
					}
				}
			}
		}
		
		return configs;
	}

	/**
	 * @return singleton instance of DatabaseConfigurationLoader
	 */
	public static DatabaseConfigurationLoader getInstance() {
		if (databaseConfigurationLoader == null) {
			databaseConfigurationLoader = new DatabaseConfigurationLoader();
		}
		return databaseConfigurationLoader;
	}
}