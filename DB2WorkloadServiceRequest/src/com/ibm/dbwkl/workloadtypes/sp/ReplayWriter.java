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
package com.ibm.dbwkl.workloadtypes.sp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ibm.db2wkl.workloadtypes.sp.datatypes.Parameter;
import com.ibm.db2wkl.workloadtypes.sp.datatypes.Procedure;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.request.Logger;

/**
 * Writes the replay file for the stored procedure module
 * 
 *
 */
public class ReplayWriter {

	/**
	 * Header of the replay file
	 */
	private final String header = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
		"<spm:Task xmlns:spm=\"http://www.example.org/StoredProceduresSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.example.org/StoredProceduresSchema StoredProceduresSchema.xsd \"> \n" +
		"\t<spm:replay>true</spm:replay> \n";
	
	/**
	 * Footer of the replay file
	 */
	private final String footer = 
		"</spm:Task>";
	
	/**
	 * replay file path
	 */
	private final File file;
	
	/**
	 * Default constructor
	 * 
	 * @param filePath path to the replay file
	 */
	public ReplayWriter(String filePath) {
		this.file = new File(filePath);
		
		try {
			// create the file and write the header
			FileWriter fw = new FileWriter(this.file);
			BufferedWriter bw = new BufferedWriter(fw); 
			
			bw.write(this.header);
			
			bw.close();
			fw.close();
		} catch (IOException e) {
			Logger.log("Could not create the replay file on path filePath: " + e.getMessage(), LogLevel.Error);
		}
	}
	
	/**
	 * Returns the file path
	 * 
	 * @return path to replay file 
	 */
	public String getFilePath() {
		return this.file.getAbsolutePath();
	}
	
	/**
	 * Adds a procedure to the report
	 * 
	 * @param procedure procedure to add to the report
	 */
	public void AddProcedure(Procedure procedure) {
		
		if (this.file == null) {
			return;
		}
		
		try {
			// create the file and write the header
			FileWriter fw = new FileWriter(this.file, true);
			BufferedWriter bw = new BufferedWriter(fw); 
			
			bw.write("\t<spm:procedure> \n");
			bw.write("\t\t<spm:name>" + procedure.getName() + "</spm:name> \n");
			bw.write("\t\t<spm:schema>" + procedure.getSchema()	+ "</spm:schema> \n");
			for (Parameter parameter : procedure.getParameter()) {
				bw.write("\t\t\t<spm:parameter> \n");
				bw.write("\t\t\t\t<spm:name>" + parameter.getName() + "</spm:name> \n");
				bw.write("\t\t\t\t<spm:dataType>" + parameter.getDataType().toString() + "</spm:dataType> \n");
				bw.write("\t\t\t\t<spm:parmType>" + parameter.getParmType().toString() + "</spm:parmType> \n");
				bw.write("\t\t\t\t<spm:value>" + parameter.getExecutionValue() + "</spm:value> \n");
				bw.write("\t\t\t</spm:parameter> \n");
			}
			bw.write("\t</spm:procedure> \n");
			
			bw.close();
			fw.close();
		} catch (IOException e) {
			Logger.log("Could not create the replay file on path filePath: " + e.getMessage(), LogLevel.Error);
		}
	}
	
	/**
	 * Closes the file
	 */
	public void Close() {
		try {
			// create the file and write the header
			FileWriter fw = new FileWriter(this.file, true);
			BufferedWriter bw = new BufferedWriter(fw); 
			
			bw.write(this.footer);
			
			bw.close();
			fw.close();
		} catch (IOException e) {
			Logger.log("Could not close the replay file on path filePath: " + e.getMessage(), LogLevel.Error);
		}
	}
}
