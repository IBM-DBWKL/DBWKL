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
package com.ibm.dbwkl;

import java.io.File;

/**
 *
 */
public final class DB2WorkloadServiceDirectory {

	/**
	 * The local directory of the service
	 */
	private static String db2WorkloadServiceDirectory;
	
	/**
	 * The local directory of the logging files
	 */
	private static String db2WorkloadServiceDirectoryLogging;
	
	/**
	 * The local directory of the pure-query files
	 */
	private static String db2WorkloadServiceDirectoryPdq;
	
	/**
	 * The local directory of the workloads
	 */
	private static String db2WorkloadServiceDirectoryWorkloads;
	
	/**
	 * The local directory of the JAR-archives
	 */
	private static String db2WorkloadServiceDirectoryLibs;
	
	/**
	 * The local directory of the configuration files
	 */
	private static String db2WorkloadServiceDirectoryConfig;

	/**
	 * Location of the schema for the StoredProceduresModule
	 */
	private static String db2WorkloadServiceDirectorySchema;

	/**
	 * Location of the tasks for the StoredProceduresModule
	 */
	private static String db2WorkloadServiceDirectoryTasks;

	/**
	 * Location of the report and replay files for the StoredProceduresModule
	 */
	private static String db2WorkloadServiceDirectoryTasksOutput;
	
	/**
	 * db2wkl.jar
	 */
	private static String db2wklServiceJar;
	
	/**
	 * db2wkl.core.jar
	 */
	private static String db2wklCoreJar;
	
	/**
	 * db2wkl.request.jar
	 */
	private static String db2wklRequestJar;
	
	/**
	 * Initialize all the directory names from the installation directroy of DB2WorkloadService
	 * 
	 * @param installationDirectory (the local directory of the DB2WorkloadService, like c:/STAF/services/db2wkl)
	 */
	public static final void init(String installationDirectory) {
		String directory = installationDirectory;
		
		if (directory.startsWith("\"") && directory.endsWith("\""))
			directory = directory.substring(1, installationDirectory.length() - 1);
		
		if (directory.endsWith("/") || directory.endsWith("\\"))
			directory = directory.substring(0, directory.length() - 1);
		
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectory = directory;
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectoryLogging = directory + File.separator + "log";
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectoryPdq = directory + File.separator + "pdq";
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectoryWorkloads = directory + File.separator + "workloads";
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectoryLibs = directory + File.separator + "libs";
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectoryConfig = directory + File.separator + "config";
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectorySchema = directory + File.separator + "schema";
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectoryTasks = directory + File.separator + "tasks";
		DB2WorkloadServiceDirectory.db2WorkloadServiceDirectoryTasksOutput = DB2WorkloadServiceDirectory.db2WorkloadServiceDirectoryTasks + File.separator + "output";
		DB2WorkloadServiceDirectory.db2wklCoreJar = directory + File.separator + "db2wkl.core.jar";
		DB2WorkloadServiceDirectory.db2wklServiceJar = directory + File.separator + "db2wkl.jar";
		DB2WorkloadServiceDirectory.db2wklRequestJar = directory + File.separator + "db2wkl.request.jar";
	}
	/**
	 * @return the local directory of the service
	 */
	public static String getDB2WorkloadServiceDirectory() {
		return db2WorkloadServiceDirectory;
	}
	
	/**
	 * @return the local directory of the logging files
	 */
	public static String getDb2WorkloadServiceDirectoryLogging() {
		return db2WorkloadServiceDirectoryLogging;
	}

	/**
	 * @return the local directory of the pure-query files
	 */
	public static String getDb2WorkloadServiceDirectoryPdq() {
		return db2WorkloadServiceDirectoryPdq;
	}

	/**
	 * @return the local directory of the workloads
	 */
	public static String getDb2WorkloadServiceDirectoryWorkloads() {
		return db2WorkloadServiceDirectoryWorkloads;
	}
	
	/**
	 * @return the local directory of the workloads
	 */
	public static String getDb2WorkloadServiceDirectoryLibs() {
		return db2WorkloadServiceDirectoryLibs;
	}
	
	/**
	 * @return the local directory of the configuration files
	 */
	public static String getDb2WorkloadServiceDirectoryConfig() {
		return db2WorkloadServiceDirectoryConfig;
	}
	
	/**
	 * @return the local directory of the task files for the StoredProceduresModule
	 */
	public static String getDb2WorkloadServiceDirectoryTasks() {
		return db2WorkloadServiceDirectoryTasks;
	}

	/**
	 * @return the local directory of the report and replay files for the StoredProceduresModule
	 */
	public static String getDb2WorkloadServiceDirectoryTasksOutput() {
		return db2WorkloadServiceDirectoryTasksOutput;
	}

	/**
	 * @return the db2WorkloadServiceDirectorySchema
	 */
	public static String getDb2WorkloadServiceDirectorySchema() {
		return db2WorkloadServiceDirectorySchema;
	}
	
	/**
	 * @return db2wkl.jar
	 */
	public static String getDB2WorkloadServiceJar(){
		return db2wklServiceJar;
	}
	
	/**
	 * @return the db2wkl.core.jar 
	 */
	public static String getDB2WorkloadServiceCoreJar(){
		return db2wklCoreJar;
	}
	
	/**
	 * @return the db2wkl.request.jar
	 */
	public static String getDB2WorkloadServiceRequestJar(){
		return db2wklRequestJar;
	}
	
}
