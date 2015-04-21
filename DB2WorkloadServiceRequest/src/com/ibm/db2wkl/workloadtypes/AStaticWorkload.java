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
package com.ibm.db2wkl.workloadtypes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.ibm.db2wkl.DB2WorkloadServiceDirectory;
import com.ibm.db2wkl.helper.RandomStringGenerator;
import com.ibm.db2wkl.io.FilenameUtils;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.Logger;
import com.ibm.db2wkl.request.Request;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.pdq.tools.Configure;
import com.ibm.pdq.tools.GeneratePureQueryXml;
import com.ibm.pdq.tools.StaticBinder;
import com.ibm.staf.STAFResult;

/**
 * For static workloads, the SQL must be bound on DB2 z/OS side. This class
 * handles the binding using PDQ features.
 * 
 *
 */
public abstract class AStaticWorkload extends APhaseConsumer {

	/**
	 * Path to the temporary static directory
	 */
	private final static String STATIC_PATH = DB2WorkloadServiceDirectory.getDB2WorkloadServiceDirectory() + "/static/";
	
	/**
	 * SQL extension name
	 */
	private final static String EXT_SQL = ".sql";
	
	/**
	 * PDQXML extension name
	 */
	private final static String EXT_PDQXML = ".pdqxml";
	
	/**
	 * A static variable storing whether the bind is finished or not. This makes
	 * sure that in the request the bind is only done once.
	 */
	private static Boolean bindDone = Boolean.FALSE;
	
	/**
	 * A static variable storing whether the bound packages are freed or not. This makes
	 * sure that in the request the free is only done once.
	 */
	private static Boolean freeDone = Boolean.FALSE;
	
	/**
	 * Name of the collection that is used for the packages. 
	 */
//	private static String collection = "DW" + Request.getRequest().getRid() + RandomStringGenerator.get(5).toUpperCase();
	private static String collection = "DW" + RandomStringGenerator.get(5).toUpperCase();
	
	/**
	 * Initializes the static workload. Note that when overriding init()
	 * in the actual child workload class it needs to call super.init().
	 */
	@Override
	public STAFResult init() {
		// if this workloads request has the option STATIC defined then
		// the statements need to be bound before the actual workload execution
		if (Request.hasOption(Options.DB_STATIC)) {
			Logger.log("Option STATIC found. Binding statements first", LogLevel.Debug);

			synchronized (bindDone) {
				if (!bindDone.booleanValue()) {
					bindDone = Boolean.TRUE;
					bind();	
				}
			}
		}			

		return new STAFResult(STAFResult.Ok);

	}
	
	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadtypes.APhaseConsumer#clean()
	 */
	@Override
	public STAFResult clean() {
		// remove the packages again
		if (Request.hasOption(Options.DB_STATIC)) {
			Logger.log("Option STATIC found. Free packages now", LogLevel.Debug);
			
			synchronized (freeDone) {
				if (!freeDone.booleanValue()) {
					freeDone = Boolean.TRUE;
					free();
				}
			}
		}
		
		return new STAFResult(STAFResult.Ok);
	}
	
	/**
	 * Prepares the static workload
	 * 
	 * @param workloadFile the workload file that contains all SQLs to be bound
	 */
	public void bind() {
		
		// get the urls for all DB2s in this request
		ArrayList<String> urls = Request.getDataSource().getURLsList();
		
		for (String url : urls) {
			Logger.log("Binding static workload for " + url, LogLevel.Info);
				
			// 0 set the temporary path
			String requestStaticPath = STATIC_PATH + Request.getRequestName() + "/" + new Date().getTime() + "/";
			
			// 1 prepare the workload file
			File workloadFile = prepareWorkloadSQLFile(requestStaticPath);
			
			// 2 generate the pdqxml file
			File pdqxmlFile = runGeneratePureQueryXmlUtility(workloadFile, requestStaticPath, url);
			
			// 3 run the configure utility
			runConfigureUtility(pdqxmlFile);
			
			// 4 bind the package
			runStaticBinderUtility(pdqxmlFile, url);
			
			// 5 set the PDQ properties in all data sources
			Request.getDataSource().getDataSource(url).setPdqProperties(
					"executionMode(STATIC)," +
					"pureQueryXml(" + pdqxmlFile.getAbsolutePath() + ")"//," +
					//"allowDynamicSQL=FALSE"
			);
			
			Logger.log("Finished binding static workload for " + url  + " (" + Request.getDataSource().getDataSource(url).getPdqProperties() + ")", LogLevel.Debug);		
		}
	}
	
	/**
	 * Free the packages bound before the workload execution
	 */
	private void free() {
		
		// get all the urls where the packages where created
		ArrayList<String> urls = Request.getDataSource().getURLsList();
		
		for (String url : urls) {
			
			try {
				// free the packages
				freePackages(url);
				
			} catch (SQLException e) {
				
				Logger.log("Could not free the packages from collection " + collection + ": " + e.getMessage(), LogLevel.Error);
			}
			
		}
		
	}
	
	/**
	 * Frees packages
	 * 
	 * @param url the url for the DB2 where to free the packages 
	 * @throws SQLException 
	 */
	private void freePackages(String url) throws SQLException {
		
		// get a connection
		Connection con = Request.getDataSource().getConnection(url);
		
		CallableStatement cstmt = con.prepareCall("call sysproc.admin_command_dsn(?,?)");
		cstmt.registerOutParameter(2, Types.VARCHAR);
		
		cstmt.setString(1, "FREE PACKAGE (" + collection + ".*.(*))");
		cstmt.execute();

		con.commit();
		
		Request.getDataSource().closeConnection(con);
	}
	
	/**
	 * Prepares the workload specific SQL file by copying it to a temporary
	 * directory. This is required because the utilities later used to bind
	 * the statements require a file path and don't accept input streams from
	 * within the workload jar.
	 * @param requestStaticPath Path of the temporary folder that holds the bind information
	 * 	
	 * @param workload the workload instance for which to prepare the statements
	 * @return the SQL that contains all the statements to be bound into DB2
	 */
	public File prepareWorkloadSQLFile(String requestStaticPath) {
		
		File workloadFile = null;
		try {
			new File(requestStaticPath).mkdirs();

			workloadFile = new File(requestStaticPath + Thread.currentThread().getName().replace(":", "_") + EXT_SQL);
			
			FileWriter writer = new FileWriter(workloadFile);
			
			for (String statementText : getStatementTexts()) {
				writer.write(statementText + ";\n");
			}
			
			writer.close();
			
		} catch (FileNotFoundException e) {
			Logger.log("The SQL workload input file was not found: " + e.getLocalizedMessage(), Logger.Error);
		} catch (IOException e) {
			Logger.log("An IO exception occurred while preparing workload SQL file for STATIC workload execution: " + e.getLocalizedMessage(), Logger.Error);
		} catch (Exception e) {
			Logger.log("Preparing workload SQL file for STATIC workload execution failed: " + e.getLocalizedMessage(), Logger.Error);
		}
		
		return workloadFile;
	}
	
	/**
	 * Runs the utility to generate the PureQuery XML from the SQL
	 * 
	 * @param workloadFile the workload file that contains all the workloads SQL
	 * @param requestStaticPath the path to the PDQXML file
	 * @param url the URL of the database to bind the statements for
	 * @return the pdqxml file
	 */
	private File runGeneratePureQueryXmlUtility(File workloadFile, String requestStaticPath, String url) {
		
		File pdqxmlFile = new File(requestStaticPath + FilenameUtils.getBaseName(workloadFile.getAbsolutePath()) + EXT_PDQXML);
		
		new GeneratePureQueryXml().generatePureQueryXml(new String[] {
				"-username", Request.getOption(Options.DB_USER),
				"-password", Request.getOption(Options.DB_PASSWORD),
				"-url", url,
				"-pureQueryXml", pdqxmlFile.getAbsolutePath(),
				"-inputSql", workloadFile.getAbsolutePath()
		}, new PrintWriter(System.out));
		
		return pdqxmlFile;
	}
	
	/**
	 * Runs the configure utility on the pdqxml to add the root package and optional collection
	 * 
	 * @param pdqxmlFile
	 */
	public void runConfigureUtility(File pdqxmlFile) {
		
		List<String[]> whatever = new ArrayList<String[]>();
		whatever.add(new String[]{
				"defaultOptions",  
				" -collection " + collection, 
				" -sqlLimit 400"
		});
		whatever.add(new String[]{
				pdqxmlFile.getAbsolutePath(),  
				" -rootPkgName DWPKG",
				" -collection " + collection, 
				" -sqlLimit 400"
		});
		for (String[] strings : whatever) {
			Logger.log(Arrays.toString(strings), LogLevel.Debug);
		}
		
		new Configure().config(
				pdqxmlFile.getAbsolutePath(),
				whatever, 
				new PrintWriter(System.out));
	}

	/**
	 * Runs the actual bind utility
	 * 
	 * @param pdqxmlFile the PDQXML that holds all bind info for the statements
	 * @param url the URL for the DB2 where to bind the statements
	 */
	public void runStaticBinderUtility(File pdqxmlFile, String url) {
		
		Logger.log("Run binds for static workload", LogLevel.Info);
		Logger.log(Request.getOption(Options.DB_STATIC_BINDOPTIONS), LogLevel.Info);
		
		new StaticBinder().bind(new String[]{
				"-pureQueryXml", pdqxmlFile.getAbsolutePath(),
				"-url", url,
				"-username", Request.getOption(Options.DB_USER),
				"-password", Request.getOption(Options.DB_PASSWORD),
				"-bindOptions", Request.getOption(Options.DB_STATIC_BINDOPTIONS)
		}, new PrintWriter(System.out));
	}
	
}
