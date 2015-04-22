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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.ibm.dbwkl.helper.RandomStringGenerator;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.request.LoggedRuntimeException;
import com.ibm.dbwkl.request.Logger;
import com.ibm.dbwkl.request.Request;
import com.ibm.dbwkl.request.parser.Options;


/**
 *
 */
public abstract class DB2zDataSource extends DB2WklDataSource {

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.service.request.connection.DB2WklDataSource#getTarget()
	 */
	@Override
	public String getTarget(String tableSpace) {
		
		boolean hasDBName = Request.hasOption(Options.DB_DBNAME);
		boolean hasTSName = Request.hasOption(Options.DB_TSNAME);
		boolean givenTSName = tableSpace != null && tableSpace.length() > 0;
		String dbName = "";
		String tsName = "";
		if(hasDBName)
			dbName = Request.getOption(Options.DB_DBNAME).toUpperCase();
		if(hasTSName)
			tsName = Request.getOption(Options.DB_TSNAME).toUpperCase();
		
		if (hasDBName && hasTSName && !givenTSName) {
			return " IN " + dbName + "." + tsName; 
		} else if (!hasDBName && hasTSName && !givenTSName) {
			return " IN " + tsName; 
		} else if (hasDBName && !hasTSName && !givenTSName) {
			return " IN DATABASE " + dbName;
		} else if (!hasDBName && !hasTSName && !givenTSName) {
			return "";
		} else if (hasDBName && hasTSName && givenTSName) {
			return " IN " + dbName + "." + tableSpace; 
		} else if (!hasDBName && hasTSName && givenTSName) {
			return " IN DSNDB04." + tableSpace; 
		} else if (hasDBName && !hasTSName && givenTSName) {
			return " IN " + dbName + "." + tableSpace;
		} else if (!hasDBName && !hasTSName && givenTSName) {
			return " IN DSNDB04." + tableSpace;
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.service.request.connection.DB2WklDataSource#createTablespace(java.lang.String)
	 */
	@Override
	public String createTablespace(String prefix, String tableSpace, String creationStatement) {
		
		String tsName = tableSpace;
		String creationStmt = creationStatement;
		
		// check if table space name is provided via command line, use it in this case
		if (tsName == null && Request.hasOption(Options.DB_TSNAME)) {
			tsName = Request.getOption(Options.DB_TSNAME);
		}
		
		// check if a table space name is given, otherwise return a randomized
		if (tsName == null) {
			if (prefix == null || prefix.length() == 0)
				tsName = "TWKL";
			else
				tsName = prefix;
			tsName += RandomStringGenerator.get(8 - tsName.length()).toUpperCase();
		}
		
		// get a connection and create the table space
		Connection con = getConnection();
		Statement stmt = null;
		try {
			
			stmt = createStatement(con);
			
			// check if a customized statement is given by the user
			if (Request.hasOption(Options.DB_TSSQL) || 
					creationStmt != null) {
				
				// replace place holders
				String statement = creationStmt != null ? creationStmt : Request.getOption(Options.DB_TSSQL);
				if (statement.contains("?TS?")) {
					statement = statement.replace("?TS?", tsName);
				}
				if (statement.contains("?DB?")) {
					if (Request.hasOption(Options.DB_DBNAME)) {
						statement = statement.replace("?DB?", Request.getOption(Options.DB_DBNAME).toUpperCase());
					} else {
						statement = statement.replace("?DB?", "DSNDB04");
					}
				}
				
				Logger.log("Trying to create the table space using " + statement, LogLevel.Debug);
				
				// execute the statement
				stmt.execute(statement);
				con.commit();
				
				Logger.log("Tablespace " + tsName + " successfully " +
						"created using the following statement " + statement, 
						LogLevel.Debug);
				
			} else if (Request.hasOption(Options.DB_DBNAME)) {
				
				String statement = "CREATE TABLESPACE " + tsName + " IN " + Request.getOption(Options.DB_DBNAME).toUpperCase();
				
				Logger.log("Trying to create the table space using " + statement, LogLevel.Debug);
				
				// use the specified database
				stmt.execute(statement);
				con.commit();
				
				Logger.log("Tablespace " + tsName + " successfully " +
						"created in database " + Request.getOption(Options.DB_DBNAME).toUpperCase(), 
						LogLevel.Debug);
			} else {
				
				String statement = "CREATE TABLESPACE " + tsName;
				
				Logger.log("Trying to create the table space using " + statement, LogLevel.Debug);
				
				// use the default database
				stmt.execute(statement);
				con.commit();
				
				Logger.log("Tablespace " + tsName + " successfully created the default database", 
						LogLevel.Debug);
			}
			
			return tsName;
			
		} catch (SQLException e) {
			
			Logger.log("Creating the table space " + tsName + " was not possible " +
					"due to the following exception: " + e.getLocalizedMessage(), 
					LogLevel.Error);
			
			throw new LoggedRuntimeException("Creating the table space for the workload failed", e);
		} finally {
			
			if (stmt != null) {
				closeStatement(stmt);
			}
			if (con != null) {
				closeConnection(con);
			}
		}

	}
	
	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.service.request.connection.DB2WklDataSource#dropTablespace(java.lang.String)
	 */
	@Override
	public boolean dropTablespace(String tableSpace) {
		
		Connection con = getConnection();
		Statement stmt = null;
		try {
			
			stmt = createStatement(con);			
			
			if (Request.hasOption(Options.DB_DBNAME)) {
				// use the specified database
				stmt.execute("DROP TABLESPACE " + Request.getOption(Options.DB_DBNAME).toUpperCase() + "." + tableSpace);
			} else {
				// use the default database
				stmt.execute("DROP TABLESPACE " + tableSpace);
			}
			
			con.commit();
			
			Logger.log("Removing the table space " + tableSpace + " successful", LogLevel.Debug);
			
			return true;
			
		} catch (SQLException e) {
			
			Logger.log("Removing the table space " + tableSpace + " was not possible " +
					"due to the following exception: " + e.getLocalizedMessage(), 
					LogLevel.Error);
			
			return false;
		} finally {
			
			if (stmt != null) {
				closeStatement(stmt);
			}
			if (con != null) {
				closeConnection(con);
			}
		}
	}

}
