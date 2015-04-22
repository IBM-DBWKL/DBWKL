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

import com.ibm.dbwkl.helper.StringUtility;
import com.ibm.dbwkl.logging.ILoggerService;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.LoggerEntry;
import com.ibm.staf.STAFHandle;
import com.ibm.staf.wrapper.STAFLog;

/**
 * <p>This class provides a STAF logger to write logging informations
 * to standard STAF log.</p>
 * 
 * <p>You can see the log by typing: <code><b>STAF local LOG QUERY GLOBAL 
 * LOGNAME DB2WorkloadServiceLog</b></code></p>
 * 
 * <p>You can delete the log by typing: <code><b>STAF local LOG DELETE 
 * LOGNAME DB2WorkloadServiceLog</b></code></p>
 * 
 * <p>For more informations, see the STAF command reference
 * on http://staf.sourceforge.net/current/STAFCMDS.htm.</p>
 * 
 */
public class STAFLogger {
 
	/**
	 * @return a new STAFLogger object
	 */
	public static Object getSTAFLogger() {

		// TODO Auto-generated method stub
		ILoggerService stafLogger = new ILoggerService() {
			
			// STAF handle
//			private STAFHandle handle = STAFHandler.instance.getSTAFHandle();
			private STAFHandle handle = null;

			private final String name = "STAFLogger";
			
			// settings that controls the maximal length of the columns
			private final static int maxMessageLength = 300;
			private final static int maxClasseLength = 16;
			private final static int maxThreadLength = 16;
			private final static int maxLineLength = 8;
			private final static int maxMethodLength = 16;
			private final static int maxThreadGroupLength = 16;
		
			@Override
			public synchronized void log(LoggerEntry entry) {
			    
				String level = entry.getLevel();
				String message = entry.getMessage();
				String classe = entry.getClasse();
				String method = entry.getMethod();
				int line = entry.getLineNumber();
				String thread = entry.getThread();
				String threadGroup = entry.getThreadGroup();
				
				if(this.handle != null) {

					String c = classe.substring(classe.lastIndexOf(".") + 1, classe.length());
					String m = StringUtility.cutString(message, maxMessageLength);
						   c = StringUtility.cutString(c, maxClasseLength);
					String t = StringUtility.cutString(thread, maxThreadLength);
					String f = StringUtility.cutString(method, maxMethodLength);
					String l = StringUtility.cutString(line + "", maxLineLength);
					String g = StringUtility.cutString(threadGroup, maxThreadGroupLength);
						
					STAFLog.log(this.handle, STAFLog.GLOBAL, "DB2WorkloadServiceLog", LogLevel.getLevel(level).getLevel(), m + "\t" + c + "\t" + f + "\t" + l + "\t" + t + "\t" + g);
				}
			}
	
			@Override
			public String getName() {
				
				return this.name;
			}

			@Override
			public void term() {

				try {
					this.handle.unRegister();
				}
				catch (Exception e) { /**/ }
			}

			@Override
			public void clean() {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void init() {
				//				
			}
		};
		
		return stafLogger;
	}	
}