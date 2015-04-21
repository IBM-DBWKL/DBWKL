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
package com.ibm.db2wkl.workloadtypes.sp;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.ibm.db2wkl.DB2WorkloadServiceDirectory;
import com.ibm.db2wkl.helper.FileLoader;
import com.ibm.db2wkl.helper.RandomStringGenerator;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.LoggedRuntimeException;
import com.ibm.db2wkl.request.Logger;
import com.ibm.db2wkl.request.Request;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.db2wkl.workloadtypes.AStoredProceduresModule;
import com.ibm.db2wkl.workloadtypes.sp.datatypes.ObjectFactory;
import com.ibm.db2wkl.workloadtypes.sp.datatypes.Parameter;
import com.ibm.db2wkl.workloadtypes.sp.datatypes.Procedure;
import com.ibm.db2wkl.workloadtypes.sp.datatypes.Task;
import com.ibm.staf.STAFResult;

/**
 * The Stored Procedures Module
 * 
 *         version 2 and integrating this project into the db2wkl service core
 * 
 */
public class StoredProceduresModule extends AStoredProceduresModule {

	/**
	 * The Task object that represents the contents of the read XML File.
	 */
	private Task task;

	/**
	 * The path to the report file where execution details will be
	 */
	private String reportPath;

	/**
	 * Taskfile path
	 */
	private String taskfile;
	
	/**
	 * Replay file writer
	 */
	private ReplayWriter replay;
	
	/**
	 * Counter for stored procedure execution
	 */
	private HashMap<String, Integer> proceduresCount;

	/**
	 * @param taskfile
	 */
	public StoredProceduresModule(String taskfile) {
		this.taskfile = taskfile;
	}

	/*
	 * @see
	 * com.ibm.db2wkl.workloadservice.AWorkloadService#init(
	 * java.util.Properties, java.lang.String)
	 */
	@Override
	public STAFResult init(){
		// TASK = The path to the xml file that contains the task to execute
		// REPORT = The path to the report file where execution details will be
		// written
		// REPLAY = The desired path for the ouput replay xml file
			
		if (this.taskfile != null) {
			this.taskfile = new FileLoader(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryTasks()).loadFile(this.taskfile).getAbsolutePath();
		} else {
			return new STAFResult(STAFResult.JavaError);
		}
		
		String timestamp = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date());
		
		String replayFileName = File.separator + Thread.currentThread().getName().replace(':', '_') + "_" + timestamp + ".xml";
		if (Request.hasOption(Options.SP_REPLAY)) {
			this.replay = new ReplayWriter(Request.getOption(Options.SP_REPLAY) + replayFileName);
		} else {
			this.replay = new ReplayWriter(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryTasks() + replayFileName);
			Logger.log("REPLAY path missing: writing file to " + this.replay.getFilePath(), LogLevel.Info);
		}
		
		if (Request.hasOption(Options.SP_REPORT)) {
			this.reportPath = Request.getOption(Options.SP_REPORT);
		} else {
			this.reportPath = DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryTasksOutput() + File.separator + "Report" + this.getId() + "-" + timestamp + ".txt";
			Logger.log("REPORT path missing: writing file to " + this.reportPath,LogLevel.Info);
		}

		// UnMarshall the task
		String schemaPath = DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectorySchema() + File.separator +"StoredProceduresSchema.xsd";
		String contextPath = ObjectFactory.class.getPackage().getName();
		String taskFilePath = this.taskfile;

		try {
			Logger.log("Unmarshalling XML File: " + taskFilePath + " Schema: " + schemaPath + " Context: " + contextPath, LogLevel.Info);
			
			JAXBContext jb = JAXBContext.newInstance(contextPath, this.getClass().getClassLoader());
			Unmarshaller u = jb.createUnmarshaller();
			this.task = (Task) u.unmarshal(new File(taskFilePath));
			
		} catch (JAXBException e) {
			
			String text = "Unmarshalling failed due to the following reason: " + e.getMessage();
			text += "; Reason: ";
			if (e.getLinkedException() != null)
				text += e.getLinkedException().getMessage();
			else
				text += "unknown";
			
			Logger.log(text, LogLevel.Error);
			return new STAFResult(STAFResult.JavaError, e.getMessage());
		}
		
		
		List<Procedure> procedures;
		List<String> distinctProcedureNames = new ArrayList<String>();
		List<Procedure> distinctProcedures = new ArrayList<Procedure>();
		try {
			procedures = this.task.getProcedure();
			for (Procedure procedure : procedures) {
				if (distinctProcedureNames.add(procedure.getName())) {
					distinctProcedures.add(procedure);
				}
			}
		} catch (Exception e) {
			return new STAFResult(STAFResult.JavaError, e.getMessage());
		}
		
		//this.connection = Request.getDataSource().getConnection();
		
		// check whether the schemas exist
		try {
			checkSchemas(distinctProcedures);
			checkProcedures(distinctProcedures);
		} catch (Exception e) {
			return new STAFResult(STAFResult.JavaError, e.getMessage());
		}
		
		// initialize counters
		this.proceduresCount = new HashMap<String, Integer>();
		for (Procedure procedure : procedures) {
			this.proceduresCount.put(getProcedureSpecificName(procedure), new Integer(0));
		}

		return new STAFResult(STAFResult.Ok);
	}
	
	/**
	 * Converts the row type P,O,B to the corresponding
	 * name IN,OUT,IN_OUT 
	 * 
	 * @param rowType P,O,B
	 * @return IN,OUT,IN_OUT
	 */
	private String rowTypeToRowTypeName(String rowType) {
		if (rowType.equalsIgnoreCase("P"))
			return "IN";
		if (rowType.equalsIgnoreCase("O")) 
			return "OUT";
		return "IN_OUT";
	}
	
	/**
	 * Checks parameters for a given procedure and its corresponding schema
	 *
	 * @param procedure the procedure to check the parameters for
	 * @throws Exception something went wrong here
	 */
	private void checkProcedureParameters(Procedure procedure) throws Exception {
		
		Connection con = Request.getDataSource().getConnection();
		Statement stmt = Request.getDataSource().createStatement(con);
		
		ResultSet rs = stmt.executeQuery(
				"SELECT NAME, PARMNAME, ROWTYPE, TYPENAME, LENGTH " +
				"FROM SYSIBM.SYSPARMS " +
				"WHERE (NAME='" + procedure.getName().toUpperCase() + "' " +
						"AND SCHEMA='" + procedure.getSchema().toUpperCase() + "')"); 

		int countParametersInXML = procedure.getParameter().size();
		int countParametersInCatalog = 0;
		
		while (rs.next()) {
			// go through the list of all parameters specified in the XML and compare them with the result
			for (Parameter parameter : procedure.getParameter()) {
				
				// check if the current parameter was found
				if (parameter.getName().equalsIgnoreCase(rs.getString("PARMNAME"))) {
					countParametersInCatalog++;
					
					// check row type
					if (!rs.getString("TYPENAME").equalsIgnoreCase(parameter.getDataType().name())) {
						throw new Exception("Parameter " + parameter.getName() + " has wrong data type in XML. Has to be " + rs.getString("TYPENAME"));
					}
					
					// check typename
					if (!rowTypeToRowTypeName(rs.getString("ROWTYPE").trim()).equalsIgnoreCase(parameter.getParmType().name())) {
						throw new Exception("Parameter " + parameter.getName() + " has the wrong row type in XML. Has to be " + rowTypeToRowTypeName(rs.getString("ROWTYPE")));
					}
				}
			}
		}
		
		// check if all parameters are covered, that is the number of parameters in the XML
		// specification is the same number as results retrieved by the query above
		if (countParametersInCatalog != countParametersInXML) {
			throw new Exception("Number of parameters in catalog vs XML is different for the procedure " + procedure.getName() + " in schema " + procedure.getSchema());
		}
	}

	/**
	 * Checks if the procedures listed in the XML file exist, and if the
	 * parameters signatures are consistent
	 * 
	 * @param procedures the procedures
	 * @throws Exception 
	 */
	public void checkProcedures(List<Procedure> procedures) throws Exception {

		// get a connection
		Connection con = Request.getDataSource().getConnection();
		Statement stmt = Request.getDataSource().createStatement(con);
				
		try {
					
			for (Procedure procedure : procedures) {
				
				// check whether the procedure exists			
				int version = con.getMetaData().getDatabaseMajorVersion();
				Logger.log("Detected database version: " + version, LogLevel.Debug);
								
				String checkStoredProcedureExistsQuery;
				
				if (version >= 9) {
					checkStoredProcedureExistsQuery = "SELECT SCHEMA, NAME, PARM_COUNT, RESULT_SETS, VERSION, ACTIVE FROM SYSIBM.SYSROUTINES WHERE ";
				}
				else {
					checkStoredProcedureExistsQuery = "SELECT SCHEMA, NAME, PARM_COUNT, RESULT_SETS FROM SYSIBM.SYSROUTINES WHERE ";
				}			
				
				checkStoredProcedureExistsQuery += "(NAME='" + procedure.getName().toUpperCase() + "' AND SCHEMA='" + procedure.getSchema().toUpperCase() + "')";
				
				ResultSet rs = stmt.executeQuery(checkStoredProcedureExistsQuery);
				
				boolean spExists = rs.next();
				
				if (spExists) {
					if (rs.getInt("PARM_COUNT") > 0) {
						checkProcedureParameters(procedure);	
					}				
				} else {
					throw new Exception(
							"The stored procedure " + 
							procedure.getName().toUpperCase() +
							" in schema " + 
							procedure.getSchema().toUpperCase() +
							" not found at " + 
							ADataSourceConsumer.getUrl());
				}
			}
		
		} catch (SQLException e) {
			Logger.log("SQLException occurred while checking the stored procedure: " + e.getLocalizedMessage(), LogLevel.Error);
			throw e;
		} catch (Exception e) {
			Logger.log("Exception occurred while checking the stored procedure: " + e.getLocalizedMessage(), LogLevel.Error);
			throw e;
		} finally {
			Request.getDataSource().closeConnection(con);
			Request.getDataSource().closeStatement(stmt);
		}		
	}

	/**
	 * Checks if the schemas listed in the XML file exist
	 * 
	 * @param procedures
	 *            the procedures
	 * @throws SQLException   
	 * @throws LoggedRuntimeException
	 * 			if an inconsistency is found 
	 */
	public void checkSchemas(List<Procedure> procedures) throws SQLException, LoggedRuntimeException {
		ArrayList<String> schemas = new ArrayList<String>();
		for (Procedure procedure : procedures) {
			schemas.add(procedure.getSchema());
		}
		Connection connection = null;
		Statement statement = null;
		try {
			connection = Request.getDataSource().getConnection();
			statement = Request.getDataSource().createStatement(connection);

			String query = "";
			for (String string : schemas) {
				if (string.contains(".")) {
					string = string.substring(string.indexOf("."), string.length());
					query = query.concat("SCHEMA='" + string + "' OR ");
				}
			}
			if (!query.equals("")) {
				// remove the last "OR"
				query = query.substring(0, query.length() - 4);
				query = "SELECT DISTINCT SCHEMA FROM SYSIBM.SYSROUTINES WHERE "
						+ query;
				Logger.log("Checking schemas: " + query, LogLevel.Info);
				statement.executeQuery(query);
				ResultSet rs = statement.getResultSet();
				while (rs.next()) {
					String currentSchema = rs.getString(1);
					schemas.remove(currentSchema);
				}
				if (schemas.size() != 0) {
					String schemasNotFound = "";
					for (String string : schemas) {
						schemasNotFound = schemasNotFound.concat(string + ",");
					}
					schemasNotFound.substring(0, schemasNotFound.length() - 1);
					Logger.log("Schemas Not Found: " + schemasNotFound, LogLevel.Error);
					throw new LoggedRuntimeException("Schemas Not Found: " + schemasNotFound);
				}
			}
		} catch (SQLException e) {
			
			throw e;
		} finally {
			Request.getDataSource().closeStatement(statement);
			Request.getDataSource().closeConnection(connection);
		}
	}

	/*
	 * @see
	 * com.ibm.db2wkl.workloadservice.AWorkloadService#execute
	 * (java.util.Properties, java.lang.String)
	 */
	@Override
	public STAFResult execute() {

		// get the connection and the execution statement
		Connection con = Request.getDataSource().getConnection();

		STAFResult result;
		if (this.task.isReplay()) {
			result = executeReplay(con);
		}
		result = executeRandomized(con);

		Request.getDataSource().closeConnection(con);

		return result;
	}

	/**
	 * Executes the normal way
	 * @param con 
	 * 
	 * @return result code
	 */
	private STAFResult executeRandomized(Connection con) {
		CallableStatement cstmt;
		
		List<Procedure> procedures = this.task.getProcedure();
		
		// print the list of procedures for debugging purposes
		for (Procedure procedure : procedures) {
			printProcedure(procedure);
		}

		// get the random execution info
		int rndMax = 0;
		int[] rndBnd = new int[procedures.size()];
		int n = 0;
		for (Procedure procedure : procedures) {
			rndMax += procedure.getWeight();
			rndBnd[n] = rndMax;
			n++;
		}
		
		// go through the list of procedures and execute them for 10 times
		Random rnd = new Random(System.currentTimeMillis());
		for (int i = 0; i < 10; i++) {
			
			Procedure procedure = null;
			
			// get the next procedure
			int rnr = rnd.nextInt(rndMax);
			for (int j = 0; j < procedures.size(); j++) {
				if (rnr < rndBnd[j]) {
					procedure = procedures.get(j);
					break;
				} 
			}
			
			// check if we have a procedure
			if (procedure == null) {
				String info =
					"Processing error while retrieving random stored procedure " + 
					"[size:" + procedures.size() + "]" +
					"[max:" + rndMax + "]" +
					"[bnd:" + Arrays.toString(rndBnd) + "]"; 
				Logger.log(info, LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, info);
			}
			
			// increase the counter
			this.proceduresCount.put(getProcedureSpecificName(procedure), new Integer(this.proceduresCount.get(getProcedureSpecificName(procedure)).intValue() + 1));
			
			StringBuilder call = buildCallText(procedure);
			
			try {
				
				// create the statement
				Logger.log("Executing SP: " + call.toString(), LogLevel.Debug);
				cstmt = Request.getDataSource().createPreparedCallableStatement(con, call.toString());
				
				// set the parameter values for this call
				setParameters(procedure, cstmt);
				
				// execute the call statement
				cstmt.execute();
				
				// write to replay file
				this.replay.AddProcedure(procedure);
				
			} catch (SQLException e) {
				
				Logger.log("Couldn't execute the call statement: " + call, LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, "Couldn't execute the call statement: " + call);
			}
		}
		
		return new STAFResult(STAFResult.Ok);
	}
	
	/**
	 * Prints a procedure to the log with all the information
	 * 
	 * @param procedure
	 */
	private void printProcedure(Procedure procedure) {
		StringBuffer procedureString = new StringBuffer();
		
		procedureString.append(procedure.getSchema());
		procedureString.append(".");
		procedureString.append(procedure.getName());
		procedureString.append("(");
		for (Parameter parameter : procedure.getParameter()) {
			procedureString.append(parameter.getName());
			procedureString.append(" ");
			procedureString.append(parameter.getDataType());
			procedureString.append(", ");
		}
		procedureString.append(")");
		
		Logger.log(procedureString.toString(), LogLevel.Debug);
	}

	/**
	 * @param procedure 
	 * @param cstmt
	 * @throws SQLException setting the parameters failed
	 */
	private void setParameters(Procedure procedure, CallableStatement cstmt) throws SQLException {
		
		for (Parameter parameter : procedure.getParameter()) {
			
			switch (parameter.getParmType()) {
			case IN:
				

				if (parameter.getValue() != null && !"".equals(parameter.getValue())) {
					// preset value
					setParameterValue(cstmt, parameter, parameter.getValue());
				} else if (parameter.getList() != null && !"".equals(parameter.getValue())) {
					// list of values where to take one from randomly
					setParameterList(cstmt, parameter, parameter.getList());
				} else if (parameter.getMin() != null && !"".equals(parameter.getMin()) &&
						(parameter.getMax() == null || "".equals(parameter.getMax()))) {
					// minimum value only handling
					setParameterWithMinimum(cstmt, parameter, Integer.parseInt(parameter.getMin()));
				} else if (parameter.getMax() != null && !"".equals(parameter.getMax()) &&
						(parameter.getMin() == null || "".equals(parameter.getMin()))) {
					// maximum value only handling
					setParameterWithMaximum(cstmt, parameter, Integer.parseInt(parameter.getMax()));
				} else if (parameter.getMin() != null && !"".equals(parameter.getMin()) &&
						parameter.getMax() != null && !"".equals(parameter.getMax())) {
					// minimum and maximum value
					setParameterWithMinimumAndMaximum(cstmt, parameter, 
							Integer.parseInt(parameter.getMin()), 
							Integer.parseInt(parameter.getMax()));
				} else {
					throw new LoggedRuntimeException("You need to provide either a value, a list of values, a minimum or a maximum value");
				}
				
				break;
			case OUT:
				
				int type = 0;
				switch (parameter.getDataType()) {
				case CHAR:
					type = Types.CHAR;
					break;
				case VARCHAR:
					type = Types.VARCHAR;
					break;
				case SMALLINT:
					type = Types.SMALLINT;
					break;
				case INTEGER:
					type = Types.INTEGER;
					break;
				}
				
				cstmt.registerOutParameter(parameter.getName(), type);
				
				break;
			case IN_OUT:
				break;
			default:
				break;
			}
		}
		
	}
	
	/**
	 * @param cstmt
	 * @param parameter
	 * @param maximum 
	 * @throws SQLException setting the parameter value failed
	 */
	private void setParameterWithMaximum(CallableStatement cstmt,
			Parameter parameter, int maximum) throws SQLException {

		String rnds = null;
		
		switch (parameter.getDataType()) {
		
		case CHAR:
		case VARCHAR:
			rnds = RandomStringGenerator.get(maximum);
			cstmt.setString(parameter.getName(), rnds);
			break;
		case INTEGER:
			int rndi = new Random(System.currentTimeMillis()).nextInt(maximum);
			rnds = String.valueOf(rndi);
			cstmt.setInt(parameter.getName(), rndi);
			break;
		case SMALLINT:
			short rndsh = (short) new Random(System.currentTimeMillis()).nextInt(maximum);
			rnds = String.valueOf(rndsh);
			cstmt.setShort(parameter.getName(), rndsh);
			break;

		default:
			break;
		}
		
		parameter.setExecutionValue(rnds);
	}
	
	/**
	 * @param cstmt
	 * @param parameter
	 * @param minimum 
	 * @param maximum 
	 * @throws SQLException setting the parameter value failed
	 */
	private void setParameterWithMinimumAndMaximum(CallableStatement cstmt,
			Parameter parameter, int minimum, int maximum) throws SQLException {

		String rnds = null;
		
		switch (parameter.getDataType()) {
		
		case CHAR:
		case VARCHAR:
			rnds = RandomStringGenerator.get(maximum).substring(0, maximum - minimum);
			cstmt.setString(parameter.getName(), rnds);
			break;
		case INTEGER:
			int rndi = new Random(System.currentTimeMillis()).nextInt(maximum);
			rnds = String.valueOf(rndi);
			cstmt.setInt(parameter.getName(), rndi);
			break;
		case SMALLINT:
			short rndsh = (short) new Random(System.currentTimeMillis()).nextInt(maximum); 
			rnds = String.valueOf(rndsh);
			cstmt.setShort(parameter.getName(), rndsh);
			break;

		default:
			break;
		}
		
		parameter.setExecutionValue(rnds);
	}

	/**
	 * @param cstmt
	 * @param parameter
	 * @param minimum
	 * @throws SQLException setting the parameter value failed
	 */
	private void setParameterWithMinimum(CallableStatement cstmt,
			Parameter parameter, int minimum) throws SQLException {

		String rnds = null;
		
		switch (parameter.getDataType()) {
		
		case CHAR:
		case VARCHAR:
			throw new LoggedRuntimeException("For parameters of type CHAR or VARCHAR a maximum is required");
		case INTEGER:
			int rndi = new Random(System.currentTimeMillis()).nextInt(Integer.MAX_VALUE - minimum) + minimum;
			rnds = String.valueOf(rndi);
			cstmt.setInt(parameter.getName(), rndi);
			break;
		case SMALLINT:
			short rndsh = (short) (new Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE - minimum) + minimum);
			rnds = String.valueOf(rndsh);
			cstmt.setShort(parameter.getName(), rndsh);
			break;

		default:
			break;
		}
		
		parameter.setExecutionValue(rnds);
	}

	/**
	 * @param cstmt
	 * @param parameter
	 * @param list
	 * @throws SQLException setting the random parameter failed
	 */
	private void setParameterList(CallableStatement cstmt, Parameter parameter,
			String list) throws SQLException {

		String[] items = list.split(";");
		int rnd = new Random(System.currentTimeMillis()).nextInt(items.length);
		setParameterValue(cstmt, parameter, items[rnd]);
		
		parameter.setExecutionValue(items[rnd]);
	}

	/**
	 * Sets the value for a specific type
	 * 
	 * @param cstmt the callable statement where to set the parameter
	 * @param parameter the parameter itself
	 * @param value the value to set
	 * @throws SQLException setting the parameter to the callable statement failed
	 */
	private void setParameterValue(CallableStatement cstmt, Parameter parameter, String value) throws SQLException {

		switch (parameter.getDataType()) {
		case CHAR:
		case VARCHAR:
			cstmt.setString(parameter.getName(), value);
			break;
		case INTEGER:
			cstmt.setInt(parameter.getName(), Integer.parseInt(value));
			break;
		case SMALLINT:
			cstmt.setShort(parameter.getName(), Short.parseShort(value));
			break;
		default:
			break;
		}

		parameter.setExecutionValue(value);
	}

	/**
	 * Executes a replay file
	 * @param con 
	 * 
	 * @return result code
	 */
	private STAFResult executeReplay(Connection con) {
		CallableStatement cstmt;
		
		List<Procedure> procedures = this.task.getProcedure();
		
		// go through the list of procedures and execute them
		for (Procedure procedure : procedures) {
			
			// increase the counter
			this.proceduresCount.put(getProcedureSpecificName(procedure), new Integer(this.proceduresCount.get(getProcedureSpecificName(procedure)).intValue() + 1));
			
			StringBuilder call = buildCallText(procedure);
			
			try {
				
				// create the statement
				Logger.log("Executing SP: " + call.toString(), LogLevel.Debug);
				cstmt = Request.getDataSource().createPreparedCallableStatement(con, call.toString());
				
				// set the parameter values for this call
				setParameters(procedure, cstmt);
				
				// execute the call statement
				cstmt.execute();
				
				// write to replay file
				this.replay.AddProcedure(procedure);
				
			} catch (SQLException e) {
				
				Logger.log("Couldn't execute the call statement: " + call + " :: " + e.getMessage(), LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, "Couldn't execute the call statement: " + call);
			}
		}
		
		return new STAFResult(STAFResult.Ok);
	}

	/**
	 * @param procedure
	 * @return the specific name for a given stored procedure 
	 */
	private String getProcedureSpecificName(Procedure procedure) {
		return (procedure.getSchema() == null ? "" : procedure.getSchema() + ".") + procedure.getName();
	}

	/**
	 * @param procedure
	 * @return the call text for the stored procedure
	 */
	private StringBuilder buildCallText(Procedure procedure) {

		// call the procedure
		StringBuilder call = new StringBuilder();
		call.append("CALL ");
		if (procedure.getSchema() != null && !procedure.getSchema().equals("")) {
			call.append(procedure.getSchema());
			call.append(".");
		}
		
		call.append(procedure.getName());
		call.append("(");
		
		for (int i = 0; i < procedure.getParameter().size(); i++) {
			call.append("?");
			if (i != procedure.getParameter().size()) {
				call.append(",");
			}
		}
		call.append(")");
		
		return call;
	}

	/*
	 * @see
	 * com.ibm.db2wkl.workloadservice.AWorkloadService#clean
	 * (java.util.Properties, java.lang.String)
	 */
	@Override
	public STAFResult clean() {
		
		// print result
		if (this.proceduresCount != null) {
			for (String procedureName : this.proceduresCount.keySet()) {
				Logger.log("Executed SP " + procedureName + " for " + this.proceduresCount.get(procedureName) + " times", LogLevel.Info);
			}
		}
		
		// finalize the replay xml file
		this.replay.Close();
		
		return new STAFResult(STAFResult.Ok);
	}

	/*
	 * @see
	 * com.ibm.db2wkl.workloadservice.AWorkloadService#getCommand
	 * ()
	 */
	@Override
	public String getCommand() {
		return Options.SP_SP;
	}

	/*
	 * @see com.ibm.db2wkl.workloadservice.AWorkloadService#
	 * getDescription()
	 */
	@Override
	public String getDescription() {
		// TODO a nice description!
		return "<< " + getName() + " >>\n\nDESCRIPTION\n"
				+ "Here the description" + "\n\nEXAMPLE\n"
				+ "STAF local DB2WKL SP TASKS tasks.xml "
				+ " USER xyz PASSWORD 123 URLS something QCOUNT 1000";

	}

	/*
	 * @see
	 * com.ibm.db2wkl.workloadservice.AWorkloadService#getName()
	 */
	@Override
	public String getName() {
		return Options.SP_SP;
	}

}
