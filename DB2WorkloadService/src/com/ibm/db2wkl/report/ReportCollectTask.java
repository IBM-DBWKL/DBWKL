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
package com.ibm.db2wkl.report;

import java.util.TimerTask;

/**
 * This is a TimerTask that will be scheduled by the Reporter. It pulls the reports from all RequestReporters every interval.
 * 
 *
 */
public class ReportCollectTask extends TimerTask {

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		Reporter.getInstance().pullReports(); 
		
	}

}
