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

import java.io.File;
import java.io.IOException;
import org.w3c.dom.Document;

import com.ibm.dbwkl.report.Report;
import com.ibm.dbwkl.report.Reporter;
import com.ibm.dbwkl.request.RequestManager;
import com.ibm.dbwkl.request.parser.Options;
import com.ibm.staf.STAFResult;

/**
 *
 */
public class ReportHandler extends InternalRequest {

	@Override
	public STAFResult execute() {
		//Query the status of the Reporter
		if(hasRequestOption(Options.REPORT_STATUS)){
			if(Reporter.getInstance().getStatus()){
				return new STAFResult(STAFResult.Ok, "Current Report Status: ON.\t" +
													 "Interval = " + Reporter.getInstance().getInterval() + "s; " + 
													 "Details = " + Reporter.getInstance().isDetails() + "; " +
													 "OutFile = " + Reporter.getInstance().getOutFile());
			} else {
				return new STAFResult(STAFResult.Ok, "Current Report Status: OFF.");
			}
		} 
		// query a report on the local system. 
		else if(hasRequestOption(Options.REPORT_SYSTEM)){
			Report report = RequestManager.getInstance().getCurrentSystemStatus();
			return new STAFResult(STAFResult.Ok, report.toString());
		}
		
		else if(hasRequestOption(Options.REPORT_OFF)){
			Reporter.getInstance().turnOff();
			return new STAFResult(STAFResult.Ok, "Reporter is turned OFF.");
		}
		//Turn the Reporter ON with specified parameters
		else if(hasRequestOption(Options.REPORT_ON)){
			//read the default values.
			boolean details = Reporter.getInstance().isDetails();
			int interval = Reporter.getInstance().getInterval();
			String outFile = Reporter.getInstance().getOutFile();
			//set details marker 
			if(hasRequestOption(Options.REPORT_DETAILS)){
				String optionValue = getRequestOption(Options.REPORT_DETAILS);
				if(optionValue.equalsIgnoreCase("true")){
					details = true;
				} else if(optionValue.equalsIgnoreCase("false")) {
					details = false;
				} else {
					return new STAFResult(STAFResult.InvalidValue, "Option " + Options.REPORT_DETAILS + " can only be TRUE or FALSE.");
				}
			}
			//set interval 
			if(hasRequestOption(Options.REPORT_INTERVAL)){
				try {	
					interval = Integer.parseInt(getRequestOption(Options.REPORT_INTERVAL));
				} catch(NumberFormatException e) {
					return new STAFResult(STAFResult.InvalidValue, "Option " + Options.REPORT_INTERVAL + " can only be an integer.");
				}
			}
			//set the output file path
			if(hasRequestOption(Options.REPORT_OUTFILE)){
				outFile = getRequestOption(Options.REPORT_OUTFILE);
				//turn the outFile off
				if(outFile.equalsIgnoreCase("off")){
					outFile = null;
				} else {
					File file = new File(outFile);
					if(!file.isAbsolute() || file.isDirectory())
						return new STAFResult(STAFResult.InvalidValue, "Option " + Options.REPORT_OUTFILE + " can only be an absolute file path.");
					try {
						if(!file.createNewFile()){
							//if the file already exist
							return new STAFResult(STAFResult.JavaError, "The file already exists.");
						}
					} catch (IOException e) {
						return new STAFResult(STAFResult.JavaError, "Can not create the specified file.");
					}
				}
			}
			Reporter.getInstance().turnOn(interval, details, outFile);
			return new STAFResult(STAFResult.Ok, "Reporter is turned ON.\t" +
												 "Interval = " + interval + "s; " + 
												 "Details = " + details + "; " +
												 "OutFile = " + outFile);
		}
		
		return new STAFResult(STAFResult.InvalidRequestString, "The request is invalide. Check syntax for correct request format");
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
