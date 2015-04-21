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
package com.ibm.db2wkl.request.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import com.ibm.db2wkl.DB2WorkloadServiceDirectory;
import com.ibm.db2wkl.STAFHandler;
import com.ibm.db2wkl.helper.CaseInsensitiveMap;
import com.ibm.db2wkl.helper.FileLoader;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.logging.Logger;
import com.ibm.staf.STAFResult;
import com.ibm.staf.STAFUtil;
import com.ibm.staf.service.STAFCommandParseResult;
import com.ibm.staf.service.STAFCommandParser;
import com.ibm.staf.service.STAFServiceInterfaceLevel30.RequestInfo;

/**
 */
public class Parser2 {

	/**
	 * Singleton instance for this parser
	 */
	private static Parser2 parser2;

	/**
	 * Map of command parsers identified by their action
	 */
	private HashMap<String, STAFCommandParser> commandParsers;

	/*
	 * Command parsers
	 */

	/**
	 * Command parser for the VERSION primary command
	 */
	private STAFCommandParser cpVersion;

	/**
	 * Command parser for the LOG primary command
	 */
	private STAFCommandParser cpLog;
	
	/**
	 * Command parser for the REPORT primary command
	 */
	private STAFCommandParser cpReport;

	/**
	 * Command parser for the Do SQL primary command
	 */
	private STAFCommandParser cpSQL;

	/**
	 * Command parser for the Help
	 */
	private STAFCommandParser cpHelp;

	/**
	 * Command parser for the request interface
	 */
	private STAFCommandParser cpRequests;

	/**
	 * Command parser for the request interface
	 */
	private STAFCommandParser cpWorkloads;

	/**
	 * Command parser for the stored procedure module
	 */
	private STAFCommandParser cpSP;

	/**
	 * Command parser for the MISC Options
	 */
	private STAFCommandParser cpMISC;
	
	/**
	 * Command parser for the list interface
	 */
	private STAFCommandParser cpLIST;

	/**
	 * Command parser for the connection test
	 */
	private STAFCommandParser cpCONTEST;
	
	/**
	 * Command parser for the setup
	 */
	private STAFCommandParser cpSetup;
	
	/**
	 * Command parser for the redirect handler
	 */
//	private STAFCommandParser cpRedirect;
	
	/**
	 * Command parser for the coordination manager
	 */
//	private STAFCommandParser cpCOORD;

	/*
	 * Methods
	 */

	/**
	 * Creates a parser instance. This is a singleton to parse requests.
	 */
	private Parser2() {

		// init the actions list
		this.commandParsers = new HashMap<String, STAFCommandParser>();

		// initialize all command parsers

		/*
		 * Help
		 */
		this.cpHelp = new STAFCommandParser();

		this.cpHelp.addOption(Options.HELP_HELP, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpHelp.addOption(Options.HELP_WKL, 1, STAFCommandParser.VALUEALLOWED);
		this.cpHelp.addOption(Options.HELP_SQL, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpHelp.addOption(Options.HELP_LOG, 1, STAFCommandParser.VALUENOTALLOWED);

		addOutOptions(this.cpHelp, Options.HELP_HELP);
		
		addOptionNeed(this.cpHelp, Options.HELP_HELP, Options.HELP_WKL, Options.HELP_SQL, Options.HELP_LOG);
		
		this.cpHelp.addOptionGroup(Options.HELP_WKL + " " + Options.HELP_SQL + " " + Options.HELP_LOG, 0, 1);

		this.commandParsers.put(Options.HELP_HELP, this.cpHelp);

		/*
		 * Version
		 */
		this.cpVersion = new STAFCommandParser();
		this.cpVersion.addOption(Options.VERSION_VERSION, 1, STAFCommandParser.VALUENOTALLOWED);

	//	addLogOptions(this.cpVersion, Options.VERSION_VERSION);
		addOutOptions(this.cpVersion, Options.VERSION_VERSION);

		this.commandParsers.put(Options.VERSION_VERSION, this.cpVersion);

		/*
		 * Logging
		 */
		this.cpLog = new STAFCommandParser();

		this.cpLog.addOption(Options.LOG_LOG, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpLog.addOption(Options.LOG_LOGTEST, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpLog.addOption(Options.LOG_DETAILS, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpLog.addOption(Options.LOG_ADD, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpLog.addOption(Options.LOG_REMOVE, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpLog.addOption(Options.LOG_CLEAN, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpLog.addOption(Options.LOG_LEVEL, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpLog.addOption(Options.LOG_SCHEMA, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpLog.addOption(Options.LOG_LIST, 1, STAFCommandParser.VALUEALLOWED);
		this.cpLog.addOption(Options.LOG_LIST_FILTER, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpLog.addOption(Options.LOG_LIST_COL, 1, STAFCommandParser.VALUEREQUIRED);
		
		addOptionNeed(this.cpLog, Options.LOG_LOG, Options.LOG_LOGTEST, Options.LOG_DETAILS,
				Options.LOG_ADD, Options.LOG_REMOVE, Options.LOG_CLEAN, Options.LOG_LEVEL, Options.LOG_LIST, Options.LOG_SCHEMA);

		addOptionNeed(this.cpLog, Options.LOG_LIST, Options.LOG_LIST_FILTER, Options.LOG_LIST_COL);

		this.cpLog.addOptionGroup(Options.LOG_ADD + " " + Options.LOG_REMOVE + " " + 
				Options.LOG_CLEAN + " " + Options.LOG_DETAILS + " " + Options.LOG_LEVEL + " " + 
				Options.LOG_LIST + " " + Options.LOG_LOGTEST + " " + Options.LOG_SCHEMA, 1, 1);

//		addLogOptions(this.cpLog, Options.LOG_LOG);
		addOutOptions(this.cpLog, Options.LOG_LOG);
		
		//Socket connection options as used by the LiveLogger
		addSocketConnectionOptions(this.cpLog);
		
		this.commandParsers.put(Options.LOG_LOG, this.cpLog);
		
		/*
		 * Report
		 */
		this.cpReport = new STAFCommandParser();
		
		this.cpReport.addOption(Options.REPORT_REPORT, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpReport.addOption(Options.REPORT_ON, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpReport.addOption(Options.REPORT_INTERVAL, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpReport.addOption(Options.REPORT_DETAILS, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpReport.addOption(Options.REPORT_OUTFILE, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpReport.addOption(Options.REPORT_OFF, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpReport.addOption(Options.REPORT_STATUS, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpReport.addOption(Options.REPORT_SYSTEM, 1, STAFCommandParser.VALUENOTALLOWED);
		
		addOptionNeed(this.cpReport, Options.REPORT_ON, Options.REPORT_INTERVAL, Options.REPORT_DETAILS,  Options.REPORT_OUTFILE);
		
		this.cpReport.addOptionGroup(Options.REPORT_ON + " " + Options.REPORT_OFF + " " + Options.REPORT_STATUS + " " + Options.REPORT_SYSTEM, 1, 1);
		
//		addLogOptions(this.cpReport, Options.REPORT_REPORT);
		addOutOptions(this.cpReport, Options.REPORT_REPORT);
		
		this.commandParsers.put(Options.REPORT_REPORT, this.cpReport);
		
		/*
		 * SQL
		 */
		this.cpSQL = new STAFCommandParser();

		this.cpSQL.addOption(Options.SQL_SQL, 1, STAFCommandParser.VALUEALLOWED);
		this.cpSQL.addOption(Options.SQL_FILE, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpSQL.addOption(Options.SQL_DELIMITER, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpSQL.addOption(Options.SQL_SLEEP, 1, STAFCommandParser.VALUEREQUIRED);
		
		addOptionNeed(this.cpSQL, Options.SQL_SQL, Options.SQL_FILE);
		addOptionNeed(this.cpSQL, Options.SQL_FILE, Options.SQL_DELIMITER, Options.SQL_SLEEP);

		addComplexDatabaseURLAndAuthorizationOptions(this.cpSQL, Options.SQL_SQL, true);
		addOutOptions(this.cpSQL, Options.SQL_SQL);
		addLogOptions(this.cpSQL, Options.SQL_SQL);
		addRedirectOptions(this.cpSQL);
		addLibOptions(this.cpSQL, Options.SQL_SQL);
		addThreadedRequestOptions(this.cpSQL, Options.SQL_SQL);

		this.commandParsers.put(Options.SQL_SQL, this.cpSQL);

		/*
		 * Request interface
		 */
		this.cpRequests = new STAFCommandParser();

		this.cpRequests.addOption(Options.REQ_REQUEST, 1, STAFCommandParser.VALUEALLOWED);
		this.cpRequests.addOption(Options.REQ_LIST, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_DETAIL, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_START, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_STOP, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_CLEAN, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_COUNT, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_STORED, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_INTERNAL, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_UNKNOWN, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_EXECUTING, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_FINISHED, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_BROKEDOWN, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpRequests.addOption(Options.REQ_STOPPED, 1, STAFCommandParser.VALUENOTALLOWED);

		addOptionNeed(this.cpRequests, Options.REQ_REQUEST);
		addOptionNeed(this.cpRequests, Options.REQ_REQUEST, Options.REQ_LIST, Options.REQ_START, Options.REQ_STOP, Options.REQ_CLEAN, Options.REQ_COUNT);

		this.cpRequests.addOptionGroup(Options.REQ_LIST + " " + Options.REQ_START + " " + Options.REQ_STOP + " " + Options.REQ_CLEAN + " " + Options.REQ_COUNT, 0, 1);
		
		addOptionNeed(this.cpRequests, Options.REQ_LIST, Options.REQ_DETAIL);
		addOptionNeed(this.cpRequests, Options.REQ_COUNT, Options.REQ_STORED, Options.REQ_INTERNAL, Options.REQ_UNKNOWN, Options.REQ_EXECUTING, Options.REQ_FINISHED, Options.REQ_BROKEDOWN, Options.REQ_STOPPED);
		this.cpRequests.addOptionGroup(Options.REQ_STORED + " " + Options.REQ_INTERNAL + " " +  Options.REQ_UNKNOWN + " " +  Options.REQ_EXECUTING + " " +  Options.REQ_FINISHED + " " +  Options.REQ_BROKEDOWN + " " +  Options.REQ_STOPPED, 0, 1);
		
		addOutOptions(this.cpRequests, Options.REQ_REQUEST);
//		addLogOptions(this.cpRequests, Options.REQ_REQUEST);

		this.commandParsers.put(Options.REQ_REQUEST, this.cpRequests);

		/*
		 * Workload Interface
		 */
		this.cpWorkloads = new STAFCommandParser();

		this.cpWorkloads.addOption(Options.WKL_WORKLOAD, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpWorkloads.addOption(Options.WKL_START, 1, STAFCommandParser.VALUEREQUIRED);

		this.cpWorkloads.addOption(Options.WKL_SQL, 1, STAFCommandParser.VALUEREQUIRED);

		this.cpWorkloads.addOption(Options.DB_REC, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpWorkloads.addOption(Options.DB_REC_PDQDB, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpWorkloads.addOption(Options.DB_STATIC, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpWorkloads.addOption(Options.DB_STATIC_BINDOPTIONS, 1, STAFCommandParser.VALUEREQUIRED);

		this.cpWorkloads.addOption(Options.WKL_NO_CLEAN, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpWorkloads.addOption(Options.WKL_NO_INIT, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpWorkloads.addOption(Options.WKL_NO_EXECUTE, 1, STAFCommandParser.VALUENOTALLOWED);

		this.cpWorkloads.addOption(Options.WKL_OPTIONS, 1, STAFCommandParser.VALUEREQUIRED);

		addOptionNeed(this.cpWorkloads, Options.WKL_WORKLOAD, Options.WKL_START, Options.DB_REC, Options.WKL_NO_CLEAN, Options.WKL_NO_INIT, Options.WKL_NO_EXECUTE);
		addOptionNeed(this.cpWorkloads, Options.DB_STATIC, Options.DB_STATIC_BINDOPTIONS);

		this.cpWorkloads.addOptionGroup(Options.WKL_START, 1, 1);

		addOutOptions(this.cpWorkloads, Options.WKL_WORKLOAD);
		addLogOptions(this.cpWorkloads, Options.WKL_WORKLOAD);
		addLibOptions(this.cpWorkloads, Options.WKL_WORKLOAD);
		addComplexDatabaseURLAndAuthorizationOptions(this.cpWorkloads, Options.WKL_WORKLOAD, true);
		addRedirectOptions(this.cpWorkloads);
		addThreadedRequestOptions(this.cpWorkloads, Options.WKL_WORKLOAD);
		
		this.commandParsers.put(Options.WKL_WORKLOAD, this.cpWorkloads);

		/*
		 * Stored Procedures Module Interface
		 */
		this.cpSP = new STAFCommandParser();

		this.cpSP.addOption(Options.SP_SP, 1, STAFCommandParser.VALUENOTALLOWED);

		this.cpSP.addOption(Options.SP_FILES, 1, STAFCommandParser.VALUEREQUIRED);
		
		this.cpSP.addOption(Options.SP_REPORT, 1, STAFCommandParser.VALUEALLOWED);
		this.cpSP.addOption(Options.SP_REPLAY, 1, STAFCommandParser.VALUEALLOWED);
		
		this.cpSP.addOption(Options.SP_PARAMS, 1, STAFCommandParser.VALUEREQUIRED);

		addOptionNeed(this.cpSP, Options.SP_SP, Options.SP_FILES);
		this.cpSP.addOptionGroup(Options.SP_FILES, 1, 1);

		addOutOptions(this.cpSP, Options.SP_SP);
		addLogOptions(this.cpSP, Options.SP_SP);
		addLibOptions(this.cpSP, Options.SP_SP);
		addComplexDatabaseURLAndAuthorizationOptions(this.cpSP, Options.SP_SP, true);
		addRedirectOptions(this.cpSP);
		addThreadedRequestOptions(this.cpSP, Options.SP_SP);
		
		this.commandParsers.put(Options.SP_SP, this.cpSP);

		/*
		 * MISC
		 */
		this.cpMISC = new STAFCommandParser();

		this.cpMISC.addOption(Options.MISC_MISC, 1, STAFCommandParser.VALUENOTALLOWED);

		this.cpMISC.addOption(Options.MISC_ENCRYPT, 1, STAFCommandParser.VALUEREQUIRED);
		this.cpMISC.addOption(Options.MISC_TOFILE, 1, STAFCommandParser.VALUEALLOWED);
		
		this.cpMISC.addOption(Options.MISC_UPDATEDEFINITIONFILE, 1, STAFCommandParser.VALUENOTALLOWED);

		addOptionNeed(this.cpMISC, Options.MISC_MISC, Options.MISC_ENCRYPT);
		this.cpMISC.addOptionGroup(Options.MISC_ENCRYPT,1,1);

		addLogOptions(this.cpMISC, Options.MISC_MISC);

		this.commandParsers.put("MISC", this.cpMISC);
		
		/*
		 * Connection test
		 */
		this.cpCONTEST = new STAFCommandParser();
		
		this.cpCONTEST.addOption(Options.CONTEST_CONTEST, 1, STAFCommandParser.VALUENOTALLOWED);
		
		addOutOptions(this.cpCONTEST, Options.CONTEST_CONTEST);
		addLogOptions(this.cpCONTEST, Options.CONTEST_CONTEST);
		
		addComplexDatabaseURLAndAuthorizationOptions(this.cpCONTEST, Options.CONTEST_CONTEST, true);
			
		this.commandParsers.put("CONTEST", this.cpCONTEST);
		
		/*
		 * List interface
		 */
		this.cpLIST = new STAFCommandParser();

		this.cpLIST.addOption(Options.LIST_LIST, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpLIST.addOption(Options.LIST_WORKLOADS, 1, STAFCommandParser.VALUENOTALLOWED);

		addOptionNeed(this.cpLIST, Options.LIST_LIST, Options.LIST_WORKLOADS);

		this.cpLIST.addOptionGroup(Options.LIST_WORKLOADS, 1, 1);

		addOutOptions(this.cpLIST, Options.LIST_LIST);
		addLogOptions(this.cpLIST, Options.LIST_LIST);

		this.commandParsers.put(Options.LIST_LIST, this.cpLIST);
		
		/*
		 * Setup
		 */
		this.cpSetup = new STAFCommandParser();
		
		this.cpSetup.addOption(Options.SETUP_SETUP, 1, STAFCommandParser.VALUENOTALLOWED);
		this.cpSetup.addOption(Options.SETUP_JCCLIBS, 1, STAFCommandParser.VALUEALLOWED);
		this.cpSetup.addOption(Options.SETUP_OVERRIDE, 1, STAFCommandParser.VALUENOTALLOWED);
		
		this.cpSetup.addOptionNeed(Options.SETUP_JCCLIBS, Options.SETUP_SETUP);
		this.cpSetup.addOptionNeed(Options.SETUP_OVERRIDE, Options.SETUP_SETUP);
		
		addOutOptions(this.cpSetup, Options.SETUP_SETUP);
		addLogOptions(this.cpSetup, Options.SETUP_SETUP);
		
		this.commandParsers.put(Options.SETUP_SETUP, this.cpSetup);
		
	}

	/**
	 * Adds the for socket connection required options to the given command parser
	 * @param commandParser commandParser instance to add the options to
	 */
	private void addSocketConnectionOptions(STAFCommandParser commandParser) {
		commandParser.addOption(Options.SOCKET_HOST, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.SOCKET_PORT, 1, STAFCommandParser.VALUEREQUIRED);
	}

	/**
	 * @param commandParser 
	 */
	public void addRedirectOptions(STAFCommandParser commandParser){
		commandParser.addOption(Options.ORIGIN_HOST, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.ORIGIN_PORT, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.RID, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.REDIRECT_TO, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOptionNeed(Options.ORIGIN_HOST, Options.ORIGIN_PORT);
		commandParser.addOptionNeed(Options.ORIGIN_PORT, Options.ORIGIN_HOST);
		commandParser.addOptionNeed(Options.ORIGIN_HOST, Options.RID);
		commandParser.addOptionNeed(Options.RID, Options.ORIGIN_HOST);
		commandParser.addOptionGroup(Options.ORIGIN_HOST + " " + Options.REDIRECT_TO, 0, 1);
	}

	/**
	 * Adds additional log options to the given command parser
	 * 
	 * @param commandParser command parser to add the options to
	 * @param action the action that causes this command parser to be executed
	 */
	private void addLogOptions(STAFCommandParser commandParser, String action) {

		// add log settings
		commandParser.addOption(Options.GEN_LOG_LOGLEVEL, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.GEN_LOG_LOGDETAILS, 1, STAFCommandParser.VALUENOTALLOWED);

		commandParser.addOptionNeed(Options.GEN_LOG_LOGDETAILS + " " + Options.GEN_LOG_LOGLEVEL, action);
	}

	/**
	 * Adds additional out options to the given command parser
	 * 
	 * @param commandParser
	 *            command parser to add the options to
	 * @param action
	 *            the action that causes this command parser to be executed
	 */
	private void addOutOptions(STAFCommandParser commandParser, String action) {

		// add out options
		commandParser.addOption(Options.GEN_OUTFORMAT, 1, STAFCommandParser.VALUEREQUIRED);

		commandParser.addOptionNeed(Options.GEN_OUTFORMAT, action);
	}

	/**
	 * Adds additional library options to the given command parser
	 * 
	 * @param commandParser
	 * @param action
	 */
	private void addLibOptions(STAFCommandParser commandParser, String action){
		
		//add lib options
		commandParser.addOption(Options.JCCLIBS, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.PDQLIBS, 1, STAFCommandParser.VALUEREQUIRED);
		
		commandParser.addOptionNeed(Options.JCCLIBS, action);
		commandParser.addOptionNeed(Options.PDQLIBS, action);
	}
	
	/**
	 * Adds additional options for database access which includes <lu> <li>User:
	 * user name for database access</li> <li>Password: password for database
	 * access</li> <li>URL: URL for database (should be type 4 but not
	 * necessary)</li> </lu>
	 * 
	 * @param commandParser
	 *            command parser to add the options to
	 * @param action
	 *            the action that causes this command parser to be executed
	 * @param upuRequired 
	 */
	@SuppressWarnings("unused")
	private void addDatabaseURLAndAuthorizationOptions(
			STAFCommandParser commandParser, String action, boolean upuRequired) {

		// add DB options
		commandParser.addOption(Options.DB_USER, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_PASSWORD, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_URL, 1, STAFCommandParser.VALUEREQUIRED);

		addOptionNeed(commandParser, action, Options.DB_USER, Options.DB_PASSWORD, Options.DB_URL);

		if (upuRequired) {
			commandParser.addOptionNeed(action, Options.DB_USER);
			commandParser.addOptionNeed(action, Options.DB_PASSWORD);
			commandParser.addOptionNeed(action, Options.DB_URL);
		}

		commandParser.addOptionGroup(Options.DB_URL, 1, 1);
	}

	/**
	 * Adds additional options for database access which includes <lu> <li>User:
	 * user name for database access</li> <li>Password: password for database
	 * access</li> <li>URL: URL for database (should be type 4 but not
	 * necessary)</li> </lu>
	 * 
	 * @param commandParser
	 *            command parser to add the options to
	 * @param action
	 *            the action that causes this command parser to be executed
	 * @param upuRequired
	 *            specifies whether user, password and urls are required options
	 */
	private void addComplexDatabaseURLAndAuthorizationOptions(
			STAFCommandParser commandParser, String action, boolean upuRequired) {

		// add complex db options
		commandParser.addOption(Options.DB_USER, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_PASSWORD, 1, STAFCommandParser.VALUEREQUIRED);
		
		/*
		 * type
		 */
		commandParser.addOption(Options.DB_TYPE_2, 1, STAFCommandParser.VALUENOTALLOWED);
		commandParser.addOption(Options.DB_TYPE_4, 1, STAFCommandParser.VALUENOTALLOWED);

		commandParser.addOptionGroup(Options.DB_TYPE_2 + " " + Options.DB_TYPE_4, 0, 1);
		
		//Not supporting the JCCLIB Options for type 2 compatibility because of shared native classes
		commandParser.addOptionGroup(Options.DB_TYPE_2 + " " + Options.JCCLIBS, 0, 1);
		
		//when Host or Alias is set, the type is implicitly specified.
		commandParser.addOptionGroup(Options.DB_TYPE_2 + " " + Options.DB_HOST, 0, 1);
		commandParser.addOptionGroup(Options.DB_TYPE_2 + " " + Options.DB_ALIAS, 0, 1);
		
		
		//when URL, URLs, Host or Alias is set, the type is implicitly specified.
		commandParser.addOptionGroup(Options.DB_TYPE_4 + " " + Options.DB_URL, 0, 1);
		commandParser.addOptionGroup(Options.DB_TYPE_4 + " " + Options.DB_URLS, 0, 1);
		commandParser.addOptionGroup(Options.DB_TYPE_4 + " " + Options.DB_HOST, 0, 1);
		commandParser.addOptionGroup(Options.DB_TYPE_4 + " " + Options.DB_ALIAS, 0, 1);
		
		commandParser.addOption(Options.DB_POOLING, 1, STAFCommandParser.VALUENOTALLOWED);

		commandParser.addOption(Options.DB_DB2OPTIONS, 1, STAFCommandParser.VALUEREQUIRED);

		/*
		 * url type 2/4
		 */
		commandParser.addOption(Options.DB_URL, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_URLS, 1, STAFCommandParser.VALUEREQUIRED);
		
		commandParser.addOption(Options.DB_HOST, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_PORT, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_LOCATIONNAME, 1, STAFCommandParser.VALUEREQUIRED);
		
	//	commandParser.addOption(Options.DB_DBTYPE, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_DBNODE, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_DBALIAS, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_NOCATALOG, 1, STAFCommandParser.VALUENOTALLOWED);

		commandParser.addOptionNeed(Options.DB_HOST, Options.DB_PORT);
		commandParser.addOptionNeed(Options.DB_HOST, Options.DB_LOCATIONNAME);
		commandParser.addOptionNeed(Options.DB_PORT + " " + Options.DB_LOCATIONNAME, Options.DB_HOST);
		
		commandParser.addOptionNeed(Options.DB_DBALIAS, Options.DB_HOST);
		commandParser.addOptionNeed(Options.DB_DBNODE + " " + Options.DB_NOCATALOG, Options.DB_DBALIAS);
		commandParser.addOptionNeed(Options.DB_DBALIAS, Options.DB_DBNODE);
		
		/*
		 * url type 2 only
		 */
		commandParser.addOption(Options.DB_ALIAS, 1, STAFCommandParser.VALUEREQUIRED);

		/*
		 * predefined config
		 */
		//CFG VALUENOTALLOWED?
		commandParser.addOption(Options.DB_CFG, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_CFG_ID, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_CFG_SSID, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_CFG_SHOST, 1, STAFCommandParser.VALUEREQUIRED);

		//addOptionNeed(commandParser, action, Options.DB_CFG_ID, Options.DB_CFG_SSID, Options.DB_CFG_SHOST);
		commandParser.addOptionNeed(Options.DB_CFG, Options.DB_CFG_ID + " " + Options.DB_CFG_SSID);

		commandParser.addOptionGroup(Options.DB_CFG_SSID + " " + Options.DB_CFG_ID, 0, 1);

		commandParser.addOptionNeed(Options.DB_CFG_SHOST, Options.DB_CFG_SSID);
		commandParser.addOptionNeed(Options.DB_CFG_SSID, Options.DB_CFG_SHOST);

		/*
		 * DB and TS to use for the statements
		 */
		commandParser.addOption(Options.DB_DBNAME, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_TSNAME, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.DB_TSSQL, 1, STAFCommandParser.VALUEREQUIRED);

		commandParser.addOptionGroup(Options.DB_URL + " " + Options.DB_URLS + " " + Options.DB_HOST + " " + Options.DB_ALIAS + " " + Options.DB_CFG, 1, 1);

		if (upuRequired) {
			commandParser.addOptionNeed(action, Options.DB_USER);
			commandParser.addOptionNeed(action, Options.DB_PASSWORD);
		}

		commandParser.addOptionNeed(Options.DB_USER + " " + Options.DB_PASSWORD + " " 
				+ Options.DB_URL + " " + Options.DB_URLS + " " 
				+ Options.DB_TYPE_2 + " "
				//+ Options.DB_TYPE_4 + " " 
				+ Options.DB_HOST + " " + Options.DB_PORT + " " + Options.DB_LOCATIONNAME + " "
				//+ Options.DB_DBTYPE + " " 
				+ Options.DB_DBNODE + " " + Options.DB_DBALIAS + " " + Options.DB_NOCATALOG + " "
				+ Options.DB_ALIAS + " " 
				+ Options.DB_CFG + " "	+ Options.DB_CFG_ID + " " + Options.DB_CFG_SSID + " "
				+ Options.DB_CFG_SHOST + " " 
				+ Options.DB_DBNAME + " " + Options.DB_TSNAME + " " + Options.DB_TSSQL + " ", action);
	}
	
	/**
	 * Adds options that are only valid for threaded requests
	 * 
	 * @param commandParser
	 *            command parser to add the options to
	 * @param action
	 *            the action that causes this command parser to be executed
	 */
	private void addThreadedRequestOptions(
			STAFCommandParser commandParser, String action) {
		
		commandParser.addOption(Options.THREADED_DURATION, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.THREADED_REPEAT, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.THREADED_INSTANCES, 1, STAFCommandParser.VALUEREQUIRED);
		commandParser.addOption(Options.JVM_OPTIONS, 1, STAFCommandParser.VALUEREQUIRED);
		
		commandParser.addOptionGroup(Options.THREADED_DURATION + " " + Options.THREADED_REPEAT, 0, 1);
	}

	/**
	 * Adds a dependency of several options to another option/action.
	 * 
	 * @param cp
	 *            the command parser to add the option needs to
	 * @param action
	 *            the action/option where the other options depend on
	 * @param options
	 */
	private void addOptionNeed(STAFCommandParser cp, String action,
			String... options) {

		StringBuilder optionsString = new StringBuilder();
		for (String option : options) {
			optionsString.append(option);
			optionsString.append(" ");
		}

		cp.addOptionNeed(optionsString.toString(), action);
	}

	/**
	 * Returns the singleton instance for this parser
	 * 
	 * @return singleton instance for this parser
	 */
	public static Parser2 getInstance() {
		if (parser2 == null) {
			parser2 = new Parser2();
		}
		return parser2;
	}



	/**
	 * Determines the STAF command parser for the request. Returns null if no
	 * command parser exists. In this case error handling must be done at the
	 * caller of this method. It is recommended to use the validateRequest()
	 * method before calling this one.
	 * 
	 * @param requestInfo
	 *            the request to get a command parser
	 * @return the command parser for the action in the request
	 */
/*	public STAFCommandParser getCommandParser(RequestInfo requestInfo) {

		StringBuilder action = new StringBuilder();
		return getCommandParser(requestInfo, action);
	}
*/
	/**
	 * Determines the STAF command parser for the request. Returns null if no
	 * command parser exists. In this case error handling must be done at the
	 * caller of this method. It is recommended to use the validateRequest()
	 * method before calling this one. <br>
	 * Additionally, a string buffer can be supplied optionally to get the
	 * action. This is only an out parameter.
	 * 
	 * @param requestInfo
	 *            the request to get a command parser for
	 * @param action
	 *            the action that was found
	 * @return the command parser for the action in the request
	 */
//	public STAFCommandParser getCommandParser(RequestInfo requestInfo, StringBuilder action) {
//
//		/*
//		 * get the action
//		 */
//		action.append(getAction(requestInfo.request));
//
//		/*
//		 * get the appropriate command parser for the action
//		 */
//		STAFCommandParser commandParser = this.commandParsers.get(action.toString().toUpperCase());
//
//		// return the found parser, null if no parser was found
//		return commandParser;
//	}

	/**
	 * Validates the given request
	 * 
	 * @param requestInfo
	 *            request to validate
	 * @return {@link STAFResult} with the validation result
	 */
//	public STAFResult validateRequest(RequestInfo requestInfo) {
//
//		
//		// check if help action was issued, in this case ignore the following check for SQL string that is required to handle
//		// SQL related commands. 
//		if (!(action.toString().equalsIgnoreCase(Options.HELP_HELP))){
//			
//			// an additional check for SQL, only necessary if there is no HELP action (otherwise it would block help 
//			// display for SQL commands)
//			String sqlCommand = result.optionValue(Options.SQL_SQL);
//
//			if (result.optionTimes(Options.SQL_SQL) != 0 && result.optionTimes(Options.SQL_FILE) != 0
//					&& sqlCommand != null && !sqlCommand.equals("")) {
//				return new STAFResult(STAFResult.InvalidRequestString, "It isn't allowed to have a SQL-statement AND a file.");
//			}
//			if (result.optionTimes(Options.SQL_SQL) != 0 && result.optionTimes(Options.SQL_FILE) == 0
//					&& (sqlCommand != null && sqlCommand.equals("")) || sqlCommand == null) {
//				return new STAFResult(STAFResult.InvalidRequestString, "Please enter a valid SQL-command.");
//			}
//		}
//		// everything went fine, thus return the result
//		return new STAFResult(STAFResult.Ok);
//	}

	/**
	 * <p>
	 * This method will resolve a configuration file. It will add the content of
	 * it to so specified command string. If no configuration file is specified
	 * in the command, it will return the command itself.
	 * </p>
	 * @param request 
	 * @return new request
	 * @throws Exception 
	 */
	public String updateRequestForConfigFile(String request) throws Exception {
	
		if (request.toUpperCase().contains(Options.GEN_CFG_FILE)) {
			StringBuilder newRequest = new StringBuilder();
			StringTokenizer tokenizer = new StringTokenizer(request);
			while (tokenizer.hasMoreTokens()) {

				String currentToken = tokenizer.nextToken();

				if (currentToken.equalsIgnoreCase(Options.GEN_CFG_FILE)) {

					// load the config file
					if (tokenizer.hasMoreTokens()) {
						String cfgFileUrl = DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryConfig() + File.separator + tokenizer.nextToken();
						newRequest.append(" " + loadCommandFromFile(cfgFileUrl, request));
					} else {
						throw new Exception("You must specify a file name to the " + Options.GEN_CFG_FILE + " option");
					}
				} else {
					newRequest.append(" " + currentToken);
				}
			}

			Logger.log("Created request: " + newRequest, LogLevel.Debug);

			// update the request
			return newRequest.toString().trim();
		} else {
			return request;
		}
	}

	/**
	 * <p>
	 * Loads a command from the specified file. If you have already a parsed
	 * command, which should be compared to the new options, you should give
	 * that in the arguments.
	 * </p>
	 * 
	 * @param cfgFileUrl
	 *            config file path
	 * @param command
	 *            the original command to make sure that options in the current
	 *            request do override options in the command file
	 * @return the commands from the file as command string
	 * @throws IOException 
	 */
	private String loadCommandFromFile(String cfgFileUrl, String command) throws IOException {

		StringBuilder stringBuilder = new StringBuilder();

		Properties properties = new Properties();
		File file = FileLoader.getDefaultFileLoader().loadFile(cfgFileUrl);
		FileInputStream stream = new FileInputStream(file);
		properties.load(stream);
		stream.close();
		
		for (Entry<Object, Object> propertieEntry : properties.entrySet()) {

			if (!command.toLowerCase().contains(" " + ((String) propertieEntry.getKey()).toLowerCase() + " ")) {

				stringBuilder.append(" ");
				stringBuilder.append(((String)propertieEntry.getKey()).trim());
				stringBuilder.append(" ");

				if (!((String) propertieEntry.getValue()).equalsIgnoreCase("true"))
					stringBuilder.append(((String)propertieEntry.getValue()).trim());
			}
		}

		return stringBuilder.toString();
	}

	/**
	 * Checks whether a string is of type numeric
	 * 
	 * @param number
	 *            text to check whether it is numeric
	 * @return true if the value of this option is a number
	 */
	public static boolean isNumeric(String number) {

		try {
			Integer.parseInt(number);
			return true;
		} catch (NumberFormatException e) {

			return false;
		}
	}
	
	/**
	 * @param action 
	 * @param requestInfo
	 * @return parseResult parse result of the request
	 * @throws Exception 
	 */
	public CaseInsensitiveMap parse(String action, RequestInfo requestInfo) throws Exception{

		requestInfo.request = updateRequestForConfigFile(requestInfo.request);
		
		STAFCommandParser commandParser = this.commandParsers.get(action.toUpperCase());
		if(commandParser == null){
			throw new Exception(action + " is not a valid action");
		}
		STAFCommandParseResult stafParseResult = commandParser.parse(requestInfo.request);
		if (stafParseResult.rc != STAFResult.Ok) {
			throw new Exception(stafParseResult.errorBuffer);
		}
		
		CaseInsensitiveMap parseResult = new CaseInsensitiveMap();
		for(int i = 1; i <= stafParseResult.numInstances(); i++){
			String option = stafParseResult.instanceName(i);
			String value = stafParseResult.instanceValue(i);
			value = resolveRequestVar(option, value, requestInfo.requestNumber);
			parseResult.put(option, value);
		}
		
		validateRequest(action, parseResult);
		
		return parseResult;
	}
	
	/**
	 * @param action
	 * @param request
	 * @return parse result
	 * @throws Exception
	 */
	public CaseInsensitiveMap parse(String action, String request) throws Exception{

		String newRequest = updateRequestForConfigFile(request);
		
		STAFCommandParser commandParser = this.commandParsers.get(action.toUpperCase());
		if(commandParser == null){
			throw new Exception(action + " is not a valid action");
		}
		STAFCommandParseResult stafParseResult = commandParser.parse(newRequest);
		if (stafParseResult.rc != STAFResult.Ok) {
			throw new Exception(stafParseResult.errorBuffer);
		}
		
		CaseInsensitiveMap parseResult = new CaseInsensitiveMap();
		for(int i = 1; i <= stafParseResult.numInstances(); i++){
			String option = stafParseResult.instanceName(i);
			String value = stafParseResult.instanceValue(i);
			parseResult.put(option, value);
		}
		
		validateRequest(action, parseResult);
		
		return parseResult;
	}
	
	/**
	 * @param action 
	 * @param options
	 * @throws Exception 
	 */
	public void validateRequest(String action, CaseInsensitiveMap options) throws Exception{
		if(action.equalsIgnoreCase(Options.SQL_SQL)){
			String sql = options.get(Options.SQL_SQL);
			if(!(sql == null) && !sql.equals("") && options.containsKey(Options.SQL_FILE)){
				throw new Exception("It isn't allowed to have a SQL-statement AND a file.");
			} else if((sql == null || sql.equals("")) && !options.containsKey(Options.SQL_FILE)){
				throw new Exception("Please enter a valid SQL-command.");
			}
		}
		//TODO: tbc
	}
	
	/**
	 * Determines the action for the given request
	 * @param request 
	 *            the request to search for the action
	 * @return the action, null if the request string is empty
	 */
	public String getAction(String request) {

		String action = "";
		int actionLength = request.indexOf(" ");

		if (actionLength != -1) {
			action = request.substring(0, actionLength);
		} else {
			action = request;
		}

		return action;
	}
	
	/**
	 * @param option
	 * @param value 
	 * @param requestNumber 
	 * @return value
	 * @throws Exception 
	 */
	public String resolveRequestVar(String option, String value, int requestNumber) throws Exception {
		STAFResult res;
		
		res = STAFUtil.resolveRequestVar(value, STAFHandler.instance.getSTAFHandle(), requestNumber);
		if (res.rc != STAFResult.Ok) {
			Logger.log("[" + res.rc + "] " + res.result, LogLevel.Error);
			throw new Exception(res.result);
		}
	
		return res.result;
	}
}
