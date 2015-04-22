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
package com.ibm.dbwkl.request.internal;

import java.io.IOException;

import org.w3c.dom.Document;

import com.ibm.dbwkl.helper.FileLoader;
import com.ibm.dbwkl.helper.StringUtility;
import com.ibm.dbwkl.request.parser.Options;
import com.ibm.dbwkl.workloadtypes.model.WorkloadClass;
import com.ibm.dbwkl.workloadtypes.viewmodel.WorkloadClassViewModel;
import com.ibm.staf.STAFResult;

/**
 * <p>This class provides methods, to show the user some help messages.
 * 
 *
 */
public class HelpHandler extends InternalRequest {

	/**
	 * <p>This string contains the whole syntax diagram of the service.</p>
	 * 
	 */
	private static String syntax;
	
	/**
	 * 
	 */
	public HelpHandler(){
		try {
			syntax = FileLoader.getResource("/com/ibm/db2wkl/files/syntax.txt", new VersionHandler());
		} catch (IOException e) {
			syntax = e.getMessage();
		}
	}
	
	/**
	 * @return a formatted string with help information
	 */
	public String getHelpText()
	{
		StringBuilder builder = new StringBuilder("Help:\n\n");
		
		builder.append(">> You can see the status of the application by typing 'STATUS'\n");
		builder.append(">> You can see the GUI by typing 'GUI'\n");
		builder.append(">> You can see and refresh the available workloads by typing 'REFRESHWKLS'\n");
		builder.append(">> You can see the current options by typing 'STATUS OPTIONS'\n");
		builder.append("\n");
		builder.append(syntax);
		
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.receiver.ASubRequest#acceptRequest(com.ibm.staf.service.STAFCommandParseResult)
	 */
	@Override
	public STAFResult execute() {
		
		// check if additional parameters are appended to the help keyword
		if (hasRequestOption(Options.HELP_WKL)) {
			
			WorkloadClassViewModel workloadClassViewModel;
			try {
				workloadClassViewModel = new WorkloadClassViewModel();
			} catch (IOException e1) {
				return new STAFResult(STAFResult.JavaError, e1.getLocalizedMessage());
			}
			
			for (WorkloadClass workloadClass : workloadClassViewModel.getWorkloadClasses()) {
				if (StringUtility.equalsIgnoreCase(workloadClass.getLongName(), getRequestOption(Options.HELP_WKL)) ||
						StringUtility.equalsIgnoreCase(workloadClass.getShortName(), getRequestOption(Options.HELP_WKL))) {
					
					return new STAFResult(STAFResult.Ok, 
							workloadClass.getDescription() == null ? "" : workloadClass.getDescription());
					
				}
			}
			
			// reuse the workload list from above to display all registered workloads
			StringBuilder workloadsBuilder = new StringBuilder();
			for(WorkloadClass workloadClass : workloadClassViewModel.getWorkloadClasses()){
				workloadsBuilder.append(
						workloadClass.getLongName() + 
						(workloadClass.getShortName() == null ? "" : (" (" + workloadClass.getShortName()) + ")") + 
						"\n");
			}
			return new STAFResult (STAFResult.InvalidValue, "Please use one of the following workloads.:\n\n" + workloadsBuilder.toString());
			
		// if help for SQL was selected, display appropriate help
		} else if (hasRequestOption(Options.HELP_SQL)) {
			
			this.result = new STAFResult(STAFResult.Ok, 
					"With the SQL command you can execute SQL statments directly from a file or the command line. You also need to specify user " +
					"credentials and the URL of the DB2 you want to connect to. Also various additional standard commands for " + 
					"LOG, THREAD and OUTPUT are available. \n\n" +
					"Example:\n\n" +
					"SQL from file:\n\n" +
					"\tSTAF LOCAL DB2WKL " + Options.SQL_SQL + " FILE c:\\yourfile.txt USER xxx PASSWORD yyy URLS jdbc:db2://lpar:port/db2_location_name\n\n" +
					"SQL from command line (include the quotes and DO NOT FORGET the semicolon!!!):\n\n" +
					"\tSTAF LOCAL DB2WKL " + Options.SQL_SQL + " \"select * from ...;\" USER xxx PASSWORD yyy URLS jdbc:db2://lpar:port/db2_location_name");
			
			return this.result;
			
		} else if (hasRequestOption(Options.HELP_LOG)) {
			
			this.result = new STAFResult(STAFResult.Ok, 
					"There are two types of log related commands:\n\n" +
					"1.\tLog as action command to set general log related settings:\n\n" +
					"\t" + Options.LOG_LOGTEST + ":\t???\n" +
					"\t" + Options.LOG_DETAILS + ":\tEnable/Disable log details\n" +
					"\t" + Options.LOG_ADD + ":\t\tAdd a new log handler\n" +
					"\t" + Options.LOG_REMOVE + ":\t\tRemove a log handler\n" +
					"\t" + Options.LOG_CLEAN + ":\t\tCleans the log\n" +
					"\t" + Options.LOG_LEVEL + ":\t\tSets the log level from x to y\n" +
					"\t" + Options.LOG_LIST + ":\t\tList with last x log entries\n" +
					"\t" + Options.LOG_SCHEMA + ":\t\tXSD Schema of a LogEntry\n\n" +
			
					"Example:\n\n" +
					"\tSTAF LOCAL DB2WKL " + Options.LOG_LOG + " ADD sysout DETAILS true\n" +
					"or\n" +
					"\tSTAF LOCAL DB2WKL " + Options.LOG_LOG + " LIST global" +
					
					"\n\n" +
					"2.\tLog related commands as general options for all requests:\n\n" +
					"\t" + Options.GEN_LOG_LOGDETAILS + "\tEnable log details for current request if allowed\n" +
					"\t" + Options.GEN_LOG_LOGLEVEL + "\tSet log level for current request if allowed: INFO WARNING ERROR DEBUG\n" +
					"\t" + Options.GEN_OUTFORMAT + "\tSet output format for current request: STAF XML TEXT(standard)\n" +

					"\nExample:\n\n" +
					"\tSTAF LOCAL DB2WKL " + Options.WKL_WORKLOAD + " START workload USER xxx PASSWORD yyy URLS jdbc:db2://host:port/db2_name " +
					Options.GEN_LOG_LOGLEVEL + " debug\n" +
					"or\n" +
					"\tSTAF LOCAL DB2WKL " + Options.WKL_WORKLOAD + " START workload USER xxx PASSWORD yyy URLS jdbc:db2://host:port/db2_name " +
					Options.GEN_LOG_LOGDETAILS +					
					"");
			
			return this.result;
			
		}
		
		// if no additional parameter is appended, the syntax table is print
		this.result = new STAFResult(STAFResult.Ok, syntax);
		return this.result;
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.receiver.ASubRequest#getAction()
	 */
	@Override
	public String getAction() {
		return "Help";
	}

	/**
	 * @return the syntax
	 */
	public static String getSyntax() {
		return syntax;
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.request.internal.InternalRequest#getXML()
	 */
	@Override
	protected Document getXML() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.request.internal.InternalRequest#getText()
	 */
	@Override
	protected String getText() {
		// TODO Auto-generated method stub
		return null;
	}
}