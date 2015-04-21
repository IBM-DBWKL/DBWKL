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
package com.ibm.db2wkl.request.handler;


import com.ibm.db2wkl.request.ASubRequest;
import com.ibm.db2wkl.request.DataSourceRequired;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.db2wkl.workloadtypes.SQL;
import com.ibm.staf.STAFResult;

/**
 */
@DataSourceRequired
public class SQLHandler extends ASubRequest {
    
    /*
     * (non-Javadoc)
     * @see com.ibm.db2wkl.receiver.ASubRequest#acceptRequest(com.ibm.db2wkl.parser.ParserResult)
     */
	@Override
	public STAFResult acceptRequest() {
		
		SQL sql = new SQL(this.getId(), this.url);
		
		sql.run();
		
		return sql.getResult();
	
	}

	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.receiver.ASubRequest#getAction()
	 */
	@Override
	public String getAction() {
		return Options.SQL_SQL;
	}

	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.receiver.ASubRequest#isConnectionManagerRequired()
	 */
	@Override
	public void stopForce() {
//		if(this.sqlList!=null) {
//			for(SQL sql:this.sqlList) {
//				sql.stop();
//			}
//			Logger.log("SQL Execution stopped, the current statements will end now before the duration time is reached.", LogLevel.Info);
//		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.db2wkl.receiver.ASubRequest#getDataSourceConsumer()
	 */
	@Override
	public ADataSourceConsumer getDataSourceConsumer() {
		return null;
	}

}