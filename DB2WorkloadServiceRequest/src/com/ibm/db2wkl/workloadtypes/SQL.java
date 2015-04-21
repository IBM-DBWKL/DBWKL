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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import com.ibm.db2wkl.DB2WorkloadServiceDirectory;
import com.ibm.db2wkl.helper.FileLoader;
import com.ibm.db2wkl.helper.StringUtility;
import com.ibm.db2wkl.helper.xml.ToXML;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.Logger;
import com.ibm.db2wkl.request.Request;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.variables.VariablesReplacer;
import com.ibm.db2wkl.variables.VariablesReplacerException;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.db2wkl.workloadservice.WorkloadStatus;
import com.ibm.staf.STAFResult;

/**
 * <p>This class implements a service to execute simple SQL statements from the
 * command line or directly from a file on your system.</p>
 * 
 * <p><b>Note:</b></p>
 * <ul>
 * 		<li>A SQL statement must end with a <code>;</code></li>
 * 		<li>A SQL statement can have multiple lines</li>
 * 		<li>After the last SQL statement, a COMMIT will take place automatically</li>
 * </ul>
 * 
 * <h4>What can you do with this service?</h4>
 *
 * <ul>
 * 		<li>You can perform a simple basic SQL command</li>
 * 		<li>You can make a SELECT and view the result</li>
 * 		<li>You can make a CREATE and view the update count</li>
 * 		<li>You can make a DROP and view the update count</li>
 * 		<li>You can perform the SQL commands on multiple URLs (if they have all the same USER/PASSWORD)</li>
 * 		<li>You can perform a SQL script (from a file on your system)</li>
 * 		<li>You can make...</li>
 * </ul>
 * 
 */
public class SQL extends APhaseConsumer implements Runnable {

    /**
     * Timer for performing the given sql task within an duration time
     */
    private final WorkloadTimer timer = new WorkloadTimer();
    
    /**
     * the right STAFResult in different situation
     */
    private STAFResult _result = new STAFResult(STAFResult.Ok);
	
    /**
     * settings for the formatting
     */
    private final static int columnWidth = 16;
    
    /**
     * The delimiter for the commands in the file
     */
    private String delimiter = ";";
    
	/**
	 * <p>This is the unique ID of the workload. The ID is unique for the whole session 
	 * (so long as the STAF process is running and the service is registered to it).</p>
	 */
	private Long id;
    
    /**
     * List of sql commands
     */
    private ArrayList<String> commands;
    
    /**
     * default constructor for SQL
     * @param srid subrequest id
     */
    public SQL(int srid) {
    	this.setId(new Long(srid));
    }
    /**
     * @param srid subrequest id
     * @param url
     */
    public SQL(int srid, String url) {
    	this(srid);
		ADataSourceConsumer.setUrl(url);
	}

	/**
     * <p>This method can execute a specified SQL command or a file with SQL commands.
     * The SQL commands must end with <code>;</code> and if you use a file, the path
     * must end with the file extension, like <code>.txt</code> or <code>.sql</code>. A
     * SQL command can have multiple lines. You can specifies more then one SQL command, 
     * separated by <code>;</code>.</p>
     * 
     * @param parsing of the request
     */
	@Override
	public void run() {
		
		// setting the creation date for isDurationTimeGoingOn()
		this.timer.setCreationDate(new Date());
		
		// only allow statement or file
		if (Request.hasOption(Options.SQL_FILE)
				&& (Request.getOption(Options.SQL_SQL) != null && Request.getOption(Options.SQL_SQL) != "")) {
			Logger.log(Request.hasOption(Options.SQL_FILE) + " " + (Request.getOption(Options.SQL_SQL) == null) + " " + (Request.getOption(Options.SQL_SQL) == ""), LogLevel.Error);
			Logger.log("You can only specify either a file or an SQL statement but not both", LogLevel.Error);
			this._result = new STAFResult(STAFResult.InvalidRequestString, "You can only specify either a file or an SQL statement but not both");
			return;
		}
		
		if (Request.hasOption(Options.SQL_DELIMITER)) {
			this.delimiter = Request.getOption(Options.SQL_DELIMITER);
		}
		
    	if (Request.hasOption(Options.SQL_FILE)) {
    		
    		// read the file
    		File file = new File(Request.getOption(Options.SQL_FILE));
    		if (!file.exists()) {
    			String tempPath;
				try {
					tempPath = file.getCanonicalPath();
				} catch (IOException e) {
					Logger.log("could not get the canonical path for " + file.toString(), LogLevel.Error);
					return;
				}
    			file = new File(DB2WorkloadServiceDirectory.getDB2WorkloadServiceDirectory() + "/" + file.getName());
    			if (!file.exists()) {
    				Logger.log("the SQL file with the following path does not exist: " + tempPath, LogLevel.Error);
					return;
				}
    		}
    		String content;
			try {
				content = FileLoader.readFile(file);
			} catch (IOException e) {
				this._result = new STAFResult(STAFResult.JavaError, "Can't read from file " + file.getAbsolutePath() + ". " + e.getMessage());
				return;
			}
    		
    		// parse the command from the content of the file
    		this.commands = getSingleSQLStatements(content);
		} else {
			this.commands = getSingleSQLStatements(Request.getOption(Options.SQL_SQL));
		}
    	
    	// execute the statements
		performExecute();
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadtypes.APhaseConsumer#init()
	 */
	@Override
	public STAFResult init() {
		return new STAFResult(STAFResult.Ok);
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadtypes.APhaseConsumer#clean()
	 */
	@Override
	public STAFResult clean() {
		return new STAFResult(STAFResult.Ok);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadtypes.APhaseConsumer#execute()
	 */
	@Override
	public STAFResult execute()
    {
	
		
    	// we create our result string
    	StringBuilder result = new StringBuilder("");
    	
	    // starting the timer
	    this.timer.setStartDate(new Date());
	    
		Connection connection = null;
	    Statement statement = null;

		try {
			connection = Request.getDataSource().getConnection();
			statement = Request.getDataSource().createStatement(connection);

			// we execute the commands on this connection
			result.append(executeCommands(statement, this.commands));
			
			connection.commit();
			
		} catch (VariablesReplacerException e) {
			Logger.log("SQL file execution not successful because: " + e.getMessage(), LogLevel.Error);
			return new STAFResult(STAFResult.InvalidParm, e.getMessage());
    	} catch (SQLException e) {
			return new STAFResult(STAFResult.JavaError, e.getMessage());
		} finally {
			Request.getDataSource().closeConnection(connection);
			Request.getDataSource().closeStatement(statement);
		}
	    
	    this.timer.setEndDate(new Date());
    	
	    Logger.log(result.toString(),LogLevel.Info);
	    
    	// we return our result string
		return this._result;
    }

	/**
	 * Executes the given list of SQL commands on the given connection.
	 * 
	 * @param statement
	 * @param list
	 * @return a pretty formatted result String
	 * @throws VariablesReplacerException 
	 */
	public String executeCommands(Statement statement, ArrayList<String> list) throws VariablesReplacerException {

    	// we create our result string
    	StringBuilder result = new StringBuilder("");
    	
    	// get the sleep time between the statements in seconds
    	int sleep = 0;
    	if (Request.hasOption(Options.SQL_SLEEP)) {
			try {
				sleep = Integer.parseInt(Request.getOption(Options.SQL_SLEEP));
			} catch (NumberFormatException e) {
				Logger.log("Option SLEEP does not contain a valid number. Specify the number of seconds to wait. Using default of 0 now.", LogLevel.Error);
				Logger.logException(e);
			}
		}
    	
    	int successfullStatementExecutions = 0;
    	
		// now, we execute each SQL command in our list
    	for (String command : list)
    	{
    		// replace optional variables
    		Logger.log("before : " + command, LogLevel.Info);
    		String sql = new VariablesReplacer().replaceVars(command);
    		Logger.log("after  : " + sql, LogLevel.Info);
    		
    		// check for comments and ignore them or process them in case they contain control logic
    		if (sql.startsWith("--")) {
				// check if it contains a control statement
    			if (sql.contains("<db2wkl")) {
					try {
						// search for the only supported attribute yet, sleep
						String[] sleepSplit = sql.split("sleep=\"");
						int sleepTime = Integer.parseInt(sleepSplit[1].substring(0, sleepSplit[1].indexOf("\"")));
						
						// sleep
						Logger.log("Sleep control statement found in SQL file. Waiting for " + sleepTime + " seconds.", LogLevel.Info);
						Thread.sleep(sleepTime * 1000);
						Logger.log("Sleep control statement found in SQL file. Waited for  " + sleepTime + " seconds.", LogLevel.Info);
						
					} catch (Exception e) {
						Logger.log("Sleep control statement in SQL file invalid. Use the following format: <db2wkl sleep=\"5\"/>", LogLevel.Error);
						Logger.logException(e);
					}
				}
			} else {
				
				try
	    		{
	    			if (this._result.rc == 0){
		    			// we log the command
		    			Logger.log("The SQL-Statement : " + sql + " will execute.", LogLevel.Debug);
		    			
		    			// we execute the command and get the result
		    			boolean statementHasResultSet = statement.execute(sql);
		
		    			// if the statement returns a result set
		    			if(statementHasResultSet)
		    			{
		    				ResultSet rs = statement.getResultSet();
			    			
		    				Logger.log(resultSetToTable(rs), LogLevel.Debug);
		    		
			    			try {
			    				
			    				rs.close();
			    				this._result =  new STAFResult(STAFResult.Ok);
			    			} 
			    			catch (SQLException e1) {
			    				
			    				Logger.logSQLException(e1, "While closing result set.");
			    			}
		    			}
		    			
		    			// if we only have a update count
		    			else
		    			{
		    				Logger.log("UpdateCount is " + statement.getUpdateCount(), LogLevel.Debug);
		    				this._result =  new STAFResult(STAFResult.Ok);
		    			}
	    			}
	    			
	    			successfullStatementExecutions++;
	    		}
	    		catch(SQLException e)
	        	{
	        		Logger.logSQLException(e, "");
	        		Logger.log("Couldn't execute: " + sql, LogLevel.Error);
	        		this._result = new STAFResult(STAFResult.InvalidValue);
	        	}	
			}
    		
    		if (sleep > 0) {
				try {
					Thread.sleep(sleep * 1000);
				} catch (InterruptedException e) {
					// nop
				}
			}
    		
    	}
    	
    	Logger.log("Number of successfull executed statements is " + successfullStatementExecutions, LogLevel.Info);
    	
    	return result.toString();
		
	}

	/**
	 * Gives some pretty formatting to the result set
	 * @param rs the result set
	 * @return the formatted string
	 * @throws SQLException if retrieving information from the resultSet causes an error
	 */
	public String resultSetToTable(ResultSet rs) throws SQLException {

		StringBuilder result = new StringBuilder("");
		
		// we get the meta data (for the column names)
		ResultSetMetaData rsmd = rs.getMetaData(); 

		// we make a array for our columns
		ArrayList<String> columns = new ArrayList<String>();
		
		// we go trough each column and get the name
		for (int i = 1; i < rsmd.getColumnCount() + 1; i++) 
		{
			// get the name
			String name = rsmd.getColumnName(i).trim();
			
			// save it in the column array
			columns.add(name);

			// we format the name and append it to the result to print
			result.append(StringUtility.cutString(name, columnWidth));
		}

		// line break
		result.append("\n");
		
		// we append a "-----" for each column
		for(@SuppressWarnings("unused") String column: columns)
			result.append(StringUtility.cutString("------------------------------------------------------------", columnWidth));
		
		// line break
		result.append("\n");
		
		// now, we go trough our whole result row by row
		while(rs.next())
		{
			// in each row, we go trough all columns
			for(String column: columns)
			{
				// we get the column content...
				String tmp = rs.getString(column);
				
				// ...and append it pretty formatted to our result string
				result.append(StringUtility.cutString(tmp, columnWidth));
			}
			
			// after each row, a line break
			result.append("\n");
		}
		
		return result.toString();
	}

	/**
	 * Decides whether the given value is a SQL command or a file path. It will return a SQL string, out of the file or
	 * out of the command. 
	 * 
	 * @param sqlCommandOrSqlFile
	 * @return a SQL command string
	 */
	public String resolveSqlCommandOrFile(String sqlCommandOrSqlFile) {

    	// if we have a URI and not a SQL statement
    	if(!isSQL(sqlCommandOrSqlFile)) {
    		
    		Logger.log("Reading SQL from file", Logger.Info);
    	
    		FileLoader fl;
    		File file;
    		
    		//Check out what the sqlCommandOrSqlFile is. Either a path or the name of a file
    		if(sqlCommandOrSqlFile.contains(":")){
    			int lastBackSlash = sqlCommandOrSqlFile.lastIndexOf("/");
    			if(lastBackSlash < 0){
    				lastBackSlash = sqlCommandOrSqlFile.lastIndexOf("\\");
    			}
    			String path = sqlCommandOrSqlFile.substring(0, lastBackSlash);
    			String fileData = sqlCommandOrSqlFile.substring(lastBackSlash+1, sqlCommandOrSqlFile.length());
    			
    			fl = new FileLoader(path);
    			file = fl.loadFile(fileData);
    		}else{
    			fl = FileLoader.getDefaultFileLoader();
        		file = fl.loadFile(sqlCommandOrSqlFile);
    		}
    		
    		if(file.exists()){
    			try {
					return fl.readFile(sqlCommandOrSqlFile);
				} catch (IOException e) {
					this._result = new STAFResult(STAFResult.FileReadError, e.getMessage());
					return e.getMessage();
				}
    		}
			Logger.log("It couldn't load the File : " + sqlCommandOrSqlFile, Logger.Error);
			this._result = new STAFResult(STAFResult.FileOpenError);
			return "noFile";
    		
    	}
    	
		Logger.log("Found SQL commands", Logger.Info);
		return sqlCommandOrSqlFile.trim();
	}

	/**
	 * Converts a ';' separated string into a list of SQL commands.
	 * 
	 * @param rawSQLCommands to convert
	 * @return a list of SQL statements
	 */
	public ArrayList<String> getSingleSQLStatements(String rawSQLCommands) {

		ArrayList<String> listTmp = new ArrayList<String>();
		
    	String[] commands1 = rawSQLCommands.split(this.delimiter);
    	for (String command : commands1) {
    		// check if there is a sleep command before the current command and extract it in this case
			if (command.trim().startsWith("--")) {
				StringBuilder command1 = new StringBuilder();
				StringReader reader = new StringReader(command);
				BufferedReader lineReader = new BufferedReader(reader);
				String line;
				try {
					while ((line = lineReader.readLine()) != null) {
						if (line.startsWith("--")) {
							listTmp.add(line);
						} else {
							command1.append(line.trim());
						}
					}
				} catch (IOException e) {
					Logger.logException(e);
				}
				
				if (command1.toString().trim().length() > 0) {
					listTmp.add(command1.toString().trim());	
				}
			} else if (command.trim().length() != 0) {
    			listTmp.add(command);	
			}
		}
    	
		return listTmp;
	}
	
	/**
	 * <p>This method tests a specified string, whether it is a SQL statement
	 * or not. It will return true, if it is a SQL statement and false if not.</p>
	 * 
	 * <p><b>Note:</b> This method will test whether the strings ends with a <code>;</code>
	 * or not. So, make sure that your SQL command ends with a <code>;</code></p>
	 *  
	 * @param sql statement to test
	 * @return true if it is a SQL statement
	 */
	public static boolean isSQL(String sql)
	{
		if(sql == null)
			return false;

		Logger.log("Last char: " + sql.substring(sql.length()-1, sql.length()), Logger.Debug);
		
		if(sql.substring(sql.length()-1, sql.length()).equals(";"))
			return true;
		return false;
	}

	/**
	 * @return true if the duration time is still going on
	 */
	public boolean isDurationTimeGoingOn() {

		int durationTime = getDurationTime();
		long currentTimeInMilli = this.timer.getStartDate().getTime();
		
		boolean isDurationTimeGoingOn = true;
		
		if(durationTime != -1) // -1 means that we have turned the duration time to infinite
			isDurationTimeGoingOn = new Date().getTime() <= currentTimeInMilli + (durationTime * 1000);
		
		Logger.log("durationTime = " + durationTime, Logger.Debug);
		Logger.log("currentTimeInMilli = " + currentTimeInMilli, Logger.Debug);
		Logger.log("isDurationTimeGoingOn = " + isDurationTimeGoingOn, Logger.Debug);
		
		return isDurationTimeGoingOn;
	}
	
	/**
	 * @return the duration time of this actual workload instance
	 */
	protected int getDurationTime() {

		String duration = getDurtime();
		
		if(duration == null || duration.equals(""))
			duration = "0";

		return Integer.parseInt(duration);
	}

	/**
	 * Need to provide a search on the objects!
	 * 
	 * @return the duration time of this actual workload instance
	 */
	@ToXML
	public final String getDurtime() {
		
		if(Request.hasOption(Options.THREADED_DURATION))
			return Request.getOption(Options.THREADED_DURATION);
		return "";
	}
	
	/**
	 * <p>This method will set the id in the workload itself and it will update it 
	 * in the visual representation of it.</p>
	 * 
	 * @param id to set
	 */
	private final void setId(Long id) {
		
		this.id = id;
	}

	/**
	 * @return STAFResult output
	 */
	public final STAFResult getResult() {
		return this._result;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadservice.ADataSourceConsumer#getName()
	 */
	@Override
	public String getName() {
		return Options.SQL_SQL;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadservice.ADataSourceConsumer#getStatus()
	 */
	@Override
	public WorkloadStatus getStatus() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadservice.ADataSourceConsumer#getId()
	 */
	@Override
	public Long getId() {
		return this.id;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadservice.ADataSourceConsumer#getEndDate()
	 */
	@Override
	public Date getEndDate() {
		return this.timer.getEndDate();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadservice.ADataSourceConsumer#getStartDate()
	 */
	@Override
	public Date getStartDate() {
		return this.timer.getStartDate();
	}
	
}
