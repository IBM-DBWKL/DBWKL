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
package com.ibm.dbwkl.request;

import java.util.TimerTask;

import com.ibm.dbwkl.report.Report;
import com.ibm.dbwkl.request.connection.DB2WklDataSource;

/**
 *
 */
public class RequestReporterTask extends TimerTask {

	/**
	 * The owner of this task.
	 */
	private RequestReporter owner;
	
	/**
	 * Date Format
	 */
//	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * The last report
	 */
//	private Report lastReport = new Report();
	
	/**
	 * @param _owner
	 */
	public RequestReporterTask(RequestReporter _owner){
		this.owner = _owner;
	}
	
	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		// gather a report
		Report report = new Report();
		
		report.setNumber_of_threads(Thread.currentThread().getThreadGroup().activeCount());
		DB2WklDataSource dataSource = Request.getDataSource();
		if(dataSource != null){
			report.setNumber_of_db2Connection(dataSource.getNumberOfActiveConnections());
			report.setNumber_of_statements(dataSource.getNumberOfActiveStatements());
		}
		
		report.setNumber_of_sockets(MonitoredSocketImplFactory.getInstance().getNumberOfOpenedSockets());	
		
		report.setConsumed_memory(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
		//if the report is different from the last one, write it to the out File
		//TODO  output report to file
/*		try {
			if(!report.equals(this.lastReport) && RequestPerformer.getReporterStub().getOutFile() != null){
				StringBuilder content = new StringBuilder();
				
				content.append(this.dateFormat.format(new Date()) + "\t reqid = " + this.owner.getRequest().getRid() + "; ");
				content.append(report.toString());
				content.append("\n");
				//FileLoader.getDefaultFileLoader().appendToFile(content.toString(), Reporter.getInstance().getOutFile());
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/		
		this.owner.add(report);
		this.owner.output(report);
		
//		this.lastReport = report;	
	}
}
