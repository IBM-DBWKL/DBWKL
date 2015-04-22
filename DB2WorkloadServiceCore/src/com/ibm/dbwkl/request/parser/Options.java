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
package com.ibm.dbwkl.request.parser;

/**
 * <p>
 * Contains all available command line options. They follow the following 
 * naming conventions:
 * </p>
 * [type]_[name]<br/>
 * [type] = the type of that command which is in most cases the action<br/>
 * [name] = the name which is per default the same as the option specified on the command line 
 * 
 * Stephan Arenswald (arens@de.ibm.com)
 * Hong Nhung Pham (hnpham@de.ibm.com)
 * Silvio Walther (silviowa@de.ibm.com)
 * Marco Miseré (misere@de.ibm.com)
 *
 */
public class Options {

	/* *******************************************************************
	 * 
	 * General options
	 * 
	 * *******************************************************************/
	
	/**
	 * Config file
	 */
	public static final String GEN_CFG_FILE = "CFGFILE";
	
	/* *******************************************************************
	 * 
	 * General options for all requests that allow these additional options for requests
	 * 
	 * *******************************************************************/
	
	/**
	 * Log details for all requests if allowed
	 */
	public static final String GEN_LOG_LOGDETAILS = "LOGDETAILS";
	
	/**
	 * Log level for all requests if allowed
	 */
	public static final String GEN_LOG_LOGLEVEL = "LOGLEVEL";
	
	/**
	 * Output format for requests
	 */
	public static final String GEN_OUTFORMAT = "OUTFORMAT";

	/* *******************************************************************
	 * 
	 * Help options
	 * 
	 * *******************************************************************/
	
	/**
	 * Action
	 */
	public static final String HELP_HELP = "HELP";
	
	/**
	 * General keyword for workload related help
	 */
	public static final String HELP_WKL = Options.WKL_WORKLOAD;
	
	/**
	 * General keyword for SQL command related help
	 */
	public static final String HELP_SQL = Options.SQL_SQL;
	
	/**
	 * General keyword for LOG command related help
	 */
	public static final String HELP_LOG = Options.LOG_LOG;
	
	
	/* *******************************************************************
	 * 
	 * Version options
	 * 
	 * *******************************************************************/
	
	/**
	 * Action
	 */
	public static final String VERSION_VERSION = "VERSION";
	
	/* *******************************************************************
	 * 
	 * Log options
	 * 
	 * *******************************************************************/
	
	/**
	 * Action
	 */
	public static final String LOG_LOG = "LOG";
	
	/**
	 * Enable/Disable log details
	 */
	public static final String LOG_DETAILS = "DETAILS";
	
	/**
	 * Add a new log handler
	 */
	public static final String LOG_ADD = "ADD";
	
	/**
	 * Remove a new log handler
	 */
	public static final String LOG_REMOVE = "REMOVE";

	/**
	 * Cleans the log
	 */
	public static final String LOG_CLEAN = "CLEAN";
	
	/**
	 * Log level
	 */
	public static final String LOG_LEVEL = "LEVEL";
	
	/**
	 * List with last x log entries
	 */
	public static final String LOG_LIST = "LIST";
	
	/**
	 * List the log entries with a filter
	 */
	public static final String LOG_LIST_FILTER = "FILTER";
	
	/**
	 * List the log with specified columns
	 */
	public static final String LOG_LIST_COL = "COLUMNS";
	
	/**
	 * List with last x log entries
	 */
	public static final String LOG_LOGTEST = "LOGTEST";
	
	/**
	 * XSD Schema of a LogEntry
	 */
	public static final String LOG_SCHEMA = "SCHEMA";
	
	/* *******************************************************************
	 * 
	 * Report options
	 * 
	 * *******************************************************************/
	
	/**
	 * Action
	 */
	public static final String REPORT_REPORT = "REPORT";
	
	/**
	 * Turn on the Reporter
	 */
	public static final String REPORT_ON = "ON";
	
	/**
	 * Turn off the Reporter
	 */
	public static final String REPORT_OFF = "OFF";
	
	/**
	 * The report interval
	 */
	public static final String REPORT_INTERVAL = "INTERVAL";
	
	/**
	 * The report output file path
	 */
	public static final String REPORT_OUTFILE = "OUTFILE";
	
	/**
	 * Enable/Disable report details
	 */
	public static final String REPORT_DETAILS = "DETAILS";
	
	/**
	 * Query the reporter's running status
	 */
	public static final String REPORT_STATUS = "STATUS";
	
	/**
	 * report the system status
	 */
	public static final String REPORT_SYSTEM = "SYSTEM";
	
	/* *******************************************************************
	 * 
	 * SOCKET options for socket Connections as used by the LiveLogger
	 * 
	 * *******************************************************************/
	
	/**
	 * Host for the Socket Connection to communicate to as used by the LiveLogger
	 */
	public static final String SOCKET_HOST = "HOST";

	/**
	 * Port for the Socket Connection to communicate to as used by the LiveLogger
	 */
	public static final String SOCKET_PORT = "PORT";
	
	
	/* *******************************************************************
	 * 
	 * REQUESTS options
	 * 
	 * *******************************************************************/
	
	/**
	 * Request action
	 */
	public static final String REQ_REQUEST = "REQUEST";
	
	/**
	 * List requests
	 */
	public static final String REQ_LIST = "LIST";
	
	/**
	 * List detailed requests
	 */
	public static final String REQ_DETAIL = "DETAIL";
	
	/**
	 * Stop a request
	 */
	public static final String REQ_STOP = "STOP";
	
	/**
	 * Start an stored request that was put on hold
	 */
	public static final String REQ_START = "START";
	
	/**
	 * Clean the request list
	 */
	public static final String REQ_CLEAN = "CLEAN";
	
	/**
	 * Count of the request type
	 */
	public static final String REQ_COUNT = "COUNT";
	
	/**
	 * all stored requests
	 */
	public static final String REQ_STORED = "STORED";
	
	/**
	 * all executing requests
	 */
	public static final String REQ_EXECUTING = "EXECUTING";
	
	/**
	 * all unknown requests
	 */
	public static final String REQ_UNKNOWN = "UNKNOWN";
	
	/**
	 * all finished requests
	 */
	public static final String REQ_FINISHED = "FINISHED";
	
	/**
	 * all brokedown requests
	 */
	public static final String REQ_BROKEDOWN = "BROKEDOWN";
	
	/**
	 * all internal requests
	 */
	public static final String REQ_INTERNAL = "INTERNAL";
	
	/**
	 * all stopped requests
	 */
	public static final String REQ_STOPPED = "STOPPED";
	
	/* *******************************************************************
	 * 
	 * Additional options for all threaded requests like workloads, stored procedures and SQL
	 * 
	 * *******************************************************************/
	
	/**
	 * Option for setting the duration time for executing a threaded request
	 */
	public static final String THREADED_DURATION = "DURATION";
	
	/**
	 * Option for setting the number of repeats for executing a threaded request
	 */
	public static final String THREADED_REPEAT = "REPEAT";
	
	/**
	 * Specifies number of threads
	 */
	public static final String THREADED_INSTANCES = "INSTANCES";
	
	/* *******************************************************************
	 * 
	 * Workload options
	 * 
	 * *******************************************************************/

	/**
	 * Workload (WKL) action
	 */
	public static final String WKL_WORKLOAD = "WKL";
	
	/**
	 * Stops a workload
	 */
	public static final String WKL_STOP = "STOP";
	
	/**
	 * Starts a workload
	 */
	public static final String WKL_START = "START";
	
	/**
	 * SQL code for own SQL Statements
	 */
	public static final String WKL_SQL = "SQL";
	
	/**
	 * No initialization phase
	 */
	public static final String WKL_NO_INIT = "NOINIT";
	
	/**
	 * No execute phase
	 */
	public static final String WKL_NO_EXECUTE = "NOEXECUTE";
	
	/**
	 * No clean phase
	 */
	public static final String WKL_NO_CLEAN = "NOCLEAN";
	
	/**
	 * Additional workload specific options
	 */
	public static final String WKL_OPTIONS = "WKLOPTIONS";
	
	/**
	 * Additional jvm options for request running in separate JVM
	 */
	public static final String JVM_OPTIONS = "JVMOPTIONS";
	
	/* *******************************************************************
	 * 
	 * Stored Procedures Module (SP) options
	 * 
	 * *******************************************************************/
	
	/**
	 * Stored Procedures Module (SPM) action
	 */
	public static final String SP_SP = "SP";
	
	/**
	 * Initiates a task or task set by name or path
	 */
	public static final String SP_FILES = "FILES";
	
	/**
	 * Own Path to a report directory
	 */
	public static final String SP_REPORT = "REPORT";

	/**
	 * Own Path to a replay directory
	 */
	public static final String SP_REPLAY = "REPLAY";
	
	/**
	 * Option for overwriting SP params that are written within an task file
	 */
	public static final String SP_PARAMS = "PARAMS";
	
	/* *******************************************************************
	 * 
	 * MISC options
	 * 
	 * *******************************************************************/

	/**
	 * MISC option for MISC options
	 */
	public static final String MISC_MISC = "MISC";
	
	/**
	 * MISC Cryptographic encrypt option
	 */
	public static final String MISC_ENCRYPT = "ENCRYPT";
	
	/**
	 * Cryptographic encrypt toFile option for generating file with encrypted password
	 */
	public static final String MISC_TOFILE = "TOFILE";
	
	/**
	 * MISC Testing option for tests
	 */
	public static final String CONTEST_CONTEST = "CONTEST";
	
	/**
	 * MISC Option for updating the service definition file with the coordination settings
	 * and rules
	 */
	public static final String MISC_UPDATEDEFINITIONFILE = "UPDATEDEFINITIONFILE";
	

	/* *******************************************************************
	 * 
	 * SQL options
	 * 
	 * *******************************************************************/
	
	/**
	 * The SQL action command
	 */
	public static final String SQL_SQL = "SQL";
	
	/**
	 * File that contains SQLs as input
	 */
	public static final String SQL_FILE = "FILE";
	
	/**
	 * The delimiter used to separate the commands. Default is ;
	 */
	public static final String SQL_DELIMITER = "DELIMITER";
	
	/**
	 * The amount of time in seconds to wait between the execution of 2 consecutive statements
	 */
	public static final String SQL_SLEEP = "SLEEP";
	
	/* *******************************************************************
	 * 
	 * Database URL options
	 * 
	 * *******************************************************************/
	
	/**
	 * User name
	 */
	public static final String DB_USER = "USER";
	
	/**
	 * User password
	 */
	public static final String DB_PASSWORD = "PASSWORD";
	
	/**
	 * Type 2 connection
	 */
	public static final String DB_TYPE_2 = "TYPE2";
	
	/**
	 * Type 4 connection
	 */
	public static final String DB_TYPE_4 = "TYPE4";
	
	/**
	 * Use a pooled connection 
	 */
	public static final String DB_POOLING = "POOLING";
	
	/**
	 * URLs for at least one connection
	 */
	public static final String DB_URLS = "URLS";
	
	/**
	 * URL for exactly one connection
	 */
	public static final String DB_URL = "URL";
	
	/**
	 * Host
	 */
	public static final String DB_HOST = "HOST";
	
	/**
	 * Port
	 */
	public static final String DB_PORT = "PORT";
	
	/**
	 * Location name for DB2 z/OS subsystem/DSG
	 */
	public static final String DB_LOCATIONNAME = "LOCATIONNAME";
	
	/**
	 * Database type required for catalog action
	 */
	//public static final String DB_DBTYPE = "DBTYPE";
	
	/**
	 * Node where DB2 z/OS subsystem resides for catalog action
	 */
	public static final String DB_DBNODE = "DBNODE";
	
	/**
	 * Alias to catalog the database with
	 */
	public static final String DB_DBALIAS = "DBALIAS";
	
	/**
	 * Prevents auto cataloging a DB if it is not cataloged yet
	 */
	public static final String DB_NOCATALOG = "NOCATALOG";
	
	/**
	 * Alias to use for type 2 connection
	 */
	public static final String DB_ALIAS = "ALIAS";
	
	/**
	 * Usage of cfg file in config folder
	 */
	public static final String DB_CFG = "DBCFG";
	
	/**
	 * ID of datbase config in config file
	 */
	public static final String DB_CFG_ID = "DBCFGID";
	
	/**
	 * SSID of database in config file (together with DB_SHOST unique)
	 */
	public static final String DB_CFG_SSID = "SSID";
	
	/**
	 * Host of database in config file (together with DB_SSID unique)
	 */
	public static final String DB_CFG_SHOST = "SHOST"; 
	
	/**
	 * Specifies whether to record the workload using Pure Query
	 */
	public static final String DB_REC = "REC";

	/**
	 * Enablement PDQ database
	 */
	public static final String DB_REC_PDQDB = "PDQDB";
	
	/**
	 * Static execution of workload
	 */
	public static final String DB_STATIC = "STATIC";
	
	/**
	 * Bind options for static execution 
	 */
	public static final String DB_STATIC_BINDOPTIONS = "BINDOPTIONS";
	
	/**
	 * Additional dynamic options
	 */
	public static final String DB_DB2OPTIONS = "DB2OPTIONS";

	/**
	 * Default database to be used for the request
	 */
	public static final String DB_DBNAME = "DBNAME";
	
	/**
	 * Default tablespace to be used for the request
	 */
	public static final String DB_TSNAME = "TSNAME";

	/**
	 * The customized tablespace creation statement
	 */
	public static final String DB_TSSQL = "TSSQL";
	
	/* *******************************************************************
	 * 
	 * LIST options
	 * 
	 * *******************************************************************/
	
	/**
	 * The List action command
	 */
	public static final String LIST_LIST = "LIST";
	
	/**
	 * List all workloads
	 */
	public static final String LIST_WORKLOADS = "WORKLOADS";

//	/* *******************************************************************
//	 * 
//	 * Coordination options
//	 * 
//	 * *******************************************************************/
	
	/**
	 * the host name of the origin of the request
	 */
	public static final String ORIGIN_HOST = "ORIGINHOST";
	
	/**
	 * the port of the origin of the request
	 */
	public static final String ORIGIN_PORT = "ORIGINPORT";

	/**
	 * the rid of the request at its origin
	 */
	public static final String RID = "RID";
	
	
	/**
	 * redirect a request to another host.
	 */
	public static final String REDIRECT_TO = "REDIRECTTO";
	
//	
//	/**
//	 * Coordination Manager action
//	 */
//	public static final String COORD_COORD = "COORD";
//	
//	/**
//	 * Coordination Manager command to setup this service to receiver external live log entries
//	 */
//	public static final String COORD_SETUPLOGRECEIVER = "SETUPLOGRECEIVER";
//	
//	/**
//	 * Coordination Manager command to disconnect the live log receiver
//	 */
//	public static final String COORD_DISCONNECT = "DISCONNECT";
//	
//	/**
//	 * Coordination Manager command to disconnect all live log receiver
//	 */
//	public static final String COORD_DISCONNECTALL = "DISCONNECTALL";
//	
//	/**
//	 * Option to register this service with the coordination service
//	 */
//	public static final String COORD_REGISTER = "REGISTER";	
//	
//	/**
//	 * Needed Option for the coordination service to update the service data within the
//	 * coordination service like the version of this service, running requests and active
//	 * connections
//	 */
//	public static final String COORD_SERVICEDATA = "SERVICEDATA";	
//	
//	/**
//	 * Marker to identify external requests and set up the external handling
//	 */
//	public static final String COORD_EXTERNAL = "EXTERNAL";	
//	
//	/**
//	 * Origin as the requests origin host name
//	 */
//	public static final String COORD_ORIGIN = "ORIGIN";	
//	
//	/**
//	 * Marker to identify external requests and set up the external handling
//	 */
//	public static final String COORD_EXTERNALID = "EXTERNALID";
//	
//	/**
//	 * Marker to identify external requests and set up the external handling
//	 */
//	public static final String COORD_REQUESTOPTIONS = "REQUESTOPTIONS";	
//	
//	/**
//	 * Marker to identify external requests and set up the external handling
//	 */
//	public static final String COORD_NOCOORD = "NOCOORD";
	
	/* *******************************************************************
	 * 
	 * Library options
	 * 
	 * *******************************************************************/
	
	/**
	 * additional libraries action command for jdbc libraries
	 */
	public static final String JCCLIBS = "JCCLIBS";
	
	/**
	 * additional libraries action command for pdq libraries
	 */
	public static final String PDQLIBS = "PDQLIBS";
	
	/* *******************************************************************
	 * 
	 * SETUP options
	 * 
	 * *******************************************************************/
	
	/**
	 * The SETUP action command
	 */
	public static final String SETUP_SETUP = "SETUP";
	
	/**
	 * Provides path to JCC libs
	 */
	public static final String SETUP_JCCLIBS = "JCCLIBS";
	
	/**
	 * Override the current config
	 */
	public static final String SETUP_OVERRIDE = "OVERRIDE";

}
