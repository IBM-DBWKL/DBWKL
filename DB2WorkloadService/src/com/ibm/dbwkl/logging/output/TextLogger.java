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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.ibm.dbwkl.DB2WorkloadServiceDirectory;
import com.ibm.dbwkl.helper.StringUtility;
import com.ibm.dbwkl.logging.ILoggerService;
import com.ibm.dbwkl.logging.LoggerEntry;
import com.ibm.dbwkl.logging.helper.TextLogFileFilter;

/**
 * This class stores a log messages, a log level (INFO, DEBUG, etc.), the
 * class name, the method name and the message line into a file on our
 * system.
 * 
 * 
 */
public class TextLogger implements ILoggerService {
	
	/**
	 * file writer to write into the logging file
	 */
	private FileWriter writer = null;
	
	/**
	 * The name of the logger
	 */
	private String name = "FileLogger";
	
	// settings that controls the maximal length of the logged strings
	// in each column of the file. The values must be a multiple of 4,
	// because one tap are 4 blanks!
	
	/**
	 * 
	 */
	private final static int maxMessageLength = 10000;
	
	/**
	 * 
	 */
	private final static int maxClasseLength = 32;
	
	/**
	 * 
	 */
	private final static int maxThreadLength = 12;
	
	/**
	 * 
	 */
	private final static int maxLineLength = 8;
	
	/**
	 * 
	 */
	private final static int maxMethodLength = 20;
	
	/**
	 * 
	 */
	private final static int maxThreadGroupLength = 12;

	/**
	 * the logging string
	 */
	private String log = "";

	/**
	 * The log file
	 */
	private File file;

	/**
	 * @param BIND_NAME 
	 * @param logFile to write the informations to it
	 * @throws IOException 
	 */
	public TextLogger() throws IOException {
		
		init();	
	}

	/**
	 * @see com.ibm.dbwkl.logging.ILoggerService#log(java.lang.String, int, java.lang.String, java.lang.String, java.lang.Integer, java.lang.String)
	 */
	@Override
	public synchronized void log(LoggerEntry entry) {
	    
		String time = entry.getFormatedTime();
		String level = entry.getLevel();
		String message = entry.getMessage();
		String classe = entry.getClasse();
		String method = entry.getMethod();
		int line = entry.getLineNumber();
		String thread = entry.getThread();
		String threadGroup = entry.getThreadGroup();
		
		String m = StringUtility.cutString(message, TextLogger.maxMessageLength, false);
		String className = StringUtility.getSimpleClassName(classe);
		className = StringUtility.cutString(className, TextLogger.maxClasseLength);
		String threadName = StringUtility.cutString(thread, TextLogger.maxThreadLength);
		String methodName = StringUtility.cutString(method, TextLogger.maxMethodLength);
		String lineNumber = StringUtility.cutString(line + "", TextLogger.maxLineLength);
		String threadGroupName = StringUtility.cutString(threadGroup, TextLogger.maxThreadGroupLength);
		
		try {
				
			//this.log = ARequest.getLogger().isDetails() + "[" + time + "]" + "\t[ " + level + "]\t" + threadName + "\t" + threadGroupName + "\t" + (ARequest.getLogger().isDetails() ? className + "\t" + methodName + "\t" + lineNumber : " ") + "\t" + m + "\n";
			//this.log = "[" + time + "]" + " [" + level + "] [" + threadGroupName + "] [" + threadName + "] " + ((ARequest.getLogger() != null) ? (ARequest.getLogger().isDetails() ? "\t[" + className + "]\t[" + methodName + "]\t[" + lineNumber +"]\t" : ""): "") + "\t" + m + "\n";
 			this.log = "[" + time + "]" + " [" + level + "] [" + threadGroupName + "] [" + threadName + "] " + 	(!className.trim().equals("") ? "\t[" + className + "]\t[" + methodName + "]\t[" + lineNumber +"]\t" : "") + "\t" + m + "\n";

			if(this.writer != null) {
				
				this.writer.write(this.log);
				this.writer.flush();
			}
		} 
		catch (IOException e) {
			
			// don't log in a logger
		}
	}

	/**
	 * This method is called when the garbage collector
	 * destroys the object. The file write will be closed 
	 * in this moment.
	 */
	@Override
	public void finalize() {
		
		// close writer
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
		
		return this.name;
	}

	@Override
	public String toString() {
		
		return "FileLogger to file " + this.file.getAbsoluteFile();
	}

	@Override
	public void term() {

		try {
			this.writer.close();
		}
		catch (Exception e) { /**/ }
	}

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
		
		// following SimpleDateFormat was used for debugging to have a new filename every minute and not only every day
//		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
		
		// creates a list of files that match the criteria implemented in LogFileFilter()
		File [] textLogFileList = new File(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryLogging()).listFiles(new TextLogFileFilter());
		
		// in case there are 10 or more text log files delete the oldest one to conserve space.
		while (textLogFileList.length >= 10) {
			
			File oldestFile = null;
			// find the oldest file
			for (File textLogFile : textLogFileList) {
				
				if (oldestFile == null) {
					oldestFile = textLogFile;
				} 
				// write myFile into myOldestFile in case myFile is older. 
				if (oldestFile.lastModified() >= textLogFile.lastModified()) {
					oldestFile = textLogFile;				
				}
			}
			
			if (oldestFile != null) {
				oldestFile.delete();
			}
			
			// read the files in the directory again. This is done to update the length of textLogFileList which is checked in 
			// the condition of the while loop. Without this check, the variable would not change and create an infinite loop since length
			// would never decrease.
			textLogFileList = new File(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryLogging()).listFiles(new TextLogFileFilter());
		}
		
		String loggingFile = DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryLogging() + File.separator +"log_" + myFormatter.format(myDate) + ".txt";
		
//		String loggingFile = DB2WorkloadService.getDb2WorkloadServiceDirectoryLogging() + "/log.txt";
		
		this.file = new File(loggingFile);
			
		this.writer = new FileWriter(this.file , true);
		
//		Logger.log("No of Files: " + test, LogLevel.Info);
		
	}
}