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
package com.ibm.dbwkl.logging.output;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.ibm.dbwkl.DB2WorkloadServiceDirectory;
import com.ibm.dbwkl.DB2WorkloadServiceException;
import com.ibm.dbwkl.helper.FileLoader;
import com.ibm.dbwkl.logging.ILoggerService;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.LoggerEntry;
import com.ibm.dbwkl.logging.helper.HtmlLogFileFilter;

/**
 * This class can write a log to a HTML file. The file is very good 
 * to read. This class should be prefered instead of the common text
 * logger.
 * 
 */
public class HTMLLogger implements ILoggerService {

	/**
	 * FileWriter to write the information to the file
	 */
	private FileWriter writer = null;

	/**
	 * HTML log file
	 */
	private File file;

	/**
	 * Default constructor
	 * @throws IOException 
	 */
	public HTMLLogger() throws IOException {
		init();
	}

	/**
	 * This method will prepare the HTML file with a HTML-header.
	 * @throws IOException 
	 */
	private void prepareFile() throws IOException {
		
		StringBuilder firstLines = new StringBuilder("");
		String header = FileLoader.getResource("/com/ibm/db2wkl/files/htmlLoggerHeader.txt", this);
		
		try {

			FileInputStream input = new FileInputStream(this.file);
			DataInputStream in = new DataInputStream(input);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String currentLine = null;
			int lineCount = 0;

			while ((currentLine = br.readLine()) != null && lineCount < 50) {

				firstLines.append(System.getProperty("line.separator"));
				firstLines.append(currentLine);
				lineCount++;
			}

			input.close();
			in.close();
			br.close();
		} 
		catch (Exception e) {
			
			throw new DB2WorkloadServiceException(e);
		}
		
		// check if the header already exists, if not add it to the html file
		if(!firstLines.toString().contains(header)) {
			
			try {
				
				this.writer.write(header);
				this.writer.flush();
			} 
			catch (IOException e) {

				throw new DB2WorkloadServiceException(e);
			}
		}
		
		// independent of whether it exists or not, this is a new start of this DB2WKL instance,
		// thus add a special html marker to the html file (for better debugging)
		String start = 
			"\t\t\t<tr>\n" +
			"\t\t\t\t<td colspan=\"9\" bgcolor=\"pink\">DB2WKL START (" + new Date().toString() + ")</td>\n" +
			"\t\t\t</tr>";
		try {
			this.writer.write(start);
			this.writer.flush();
		} catch (IOException e) {
			throw new DB2WorkloadServiceException(e);
		}
	}

	/**
	 * @see com.ibm.dbwkl.logging.ILoggerService#log(java.lang.String, int, java.lang.String, java.lang.String, java.lang.Integer, java.lang.String)
	 */
	@Override
	public synchronized void log(LoggerEntry entry) {
	    
		String time = entry.getFormatedTime();
		String requestName = entry.getRequestName();
		String level = entry.getLevel();
		
		String message = entry.getMessage();
		message = message.replace(System.getProperty("line.separator"),"<br>\t\t\t\t");
		
		String classe = entry.getClasse();
		String method = entry.getMethod();
		int line = entry.getLineNumber();
		String thread = entry.getThread();
		String threadGroup = entry.getThreadGroup();
		
		try {
				
			String log = "\t\t\t<tr>\n" +
							"\t\t\t\t<td>" + time + "</td>\n" +
							"\t\t\t\t<td>" + requestName + "</td>\n" +
							"\t\t\t\t<td class=\"level\" BGCOLOR='" + LogLevel.getLevel(level).getHTMLColor() + "'>" + level + "</td>\n" +
							"\t\t\t\t<td>" + message + "</td>\n" +
							"\t\t\t\t<td>" + classe + "</td>\n" +
							"\t\t\t\t<td>" + method + "</td>\n" +
							"\t\t\t\t<td>" + line + "</td>\n" +
							"\t\t\t\t<td>" + thread + "</td>\n" +
							"\t\t\t\t<td>" + threadGroup + "</td>\n" +
					     "\t\t\t</tr>\n";

			if(this.writer != null) {
				
				this.writer.write(log);
				this.writer.flush();
			}
		} 
		catch (IOException e) {
			
			// don't log in a logger
		}
	}

	/**
	 * This method is called when the garbage collector destroys the object. 
	 * The file write will be closed in this moment.
	 */
	@Override
	public void finalize() {
		
		if(this.writer != null) {
			
			try {
				
				this.writer.close();
			} 
			catch (IOException e) {
				
				//
			}
		}
	}

	@Override
	public String getName() {
		
		return "HTMLLogger";
	}

	@Override
	public String toString() {
		
		return 
		"HTMLLogger v1\n" +
		"Logging to file " + this.file.getAbsoluteFile();
	}

	@Override
	public void term() {

		try {
			
			this.writer.close();
		}
		catch (Exception e) { /**/ }
	}
	
	/**
	 * cleans the HTML log
	 */
	@Override
	public void clean() {
		try {
			this.file.delete();
		}
		catch (Exception e) { /**/ }
	}
	
	@Override
	public void init() throws IOException {
		
		Date myDate = Calendar.getInstance().getTime();

		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy.MM.dd");
		
		// following format was used for debugging to have a new filename every minute and not only every day
		//SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
		
		// creates a list of files that match the criteria implemented in LogFileFilter()
		File [] htmlLogFileList = new File(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryLogging()).listFiles(new HtmlLogFileFilter());
		
		// in case there are 10 or more text log files delete the oldest one to conserve space.
		while (htmlLogFileList.length >= 10) {
			
			File myOldestFile = null;
			// find the oldest file
			for (File myFile : htmlLogFileList) {
				
				if (myOldestFile == null) {
					myOldestFile = myFile;
				} 
				// write myFile into myOldestFile in case myFile is older. 
				if (myOldestFile.lastModified() >= myFile.lastModified()) {
					myOldestFile = myFile;				
				}
			}
			
			if (myOldestFile != null) {
				myOldestFile.delete();
			}
			
			// read the files in the directory again. This is done to update the length of textLogFileList which is checked in 
			// the condition of the while loop. Without this check, the variable would not change and create an infinite loop since length
			// would never decrease.
			htmlLogFileList = new File(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryLogging()).listFiles(new HtmlLogFileFilter());
		}
		
		String loggingFile = DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryLogging() + File.separator + "log_" + myFormatter.format(myDate) + ".html";

//		String loggingFile = DB2WorkloadService.getDb2WorkloadServiceDirectoryLogging() + "/log.html";
		
		this.file = new File(loggingFile);
		
		this.writer = new FileWriter(this.file, true);
		
		prepareFile();
	}
}