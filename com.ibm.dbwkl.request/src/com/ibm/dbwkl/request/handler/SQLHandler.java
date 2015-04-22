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
package com.ibm.dbwkl.request.handler;


import com.ibm.dbwkl.request.ASubRequest;
import com.ibm.dbwkl.request.DataSourceRequired;
import com.ibm.dbwkl.request.parser.Options;
import com.ibm.dbwkl.workloadservice.ADataSourceConsumer;
import com.ibm.dbwkl.workloadtypes.SQL;
import com.ibm.staf.STAFResult;

/**
 */
@DataSourceRequired
public class SQLHandler extends ASubRequest {
    
    /*
     * (non-Javadoc)
     * @see com.ibm.dbwkl.receiver.ASubRequest#acceptRequest(com.ibm.dbwkl.parser.ParserResult)
     */
	@Override
	public STAFResult acceptRequest() {
		
		SQL sql = new SQL(this.getId(), this.url);
		
		sql.run();
		
		return sql.getResult();
	
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.receiver.ASubRequest#getAction()
	 */
	@Override
	public String getAction() {
		return Options.SQL_SQL;
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.receiver.ASubRequest#isConnectionManagerRequired()
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
	 * @see com.ibm.dbwkl.receiver.ASubRequest#getDataSourceConsumer()
	 */
	@Override
	public ADataSourceConsumer getDataSourceConsumer() {
		return null;
	}

}