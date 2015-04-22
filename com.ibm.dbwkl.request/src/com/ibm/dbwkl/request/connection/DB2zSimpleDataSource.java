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
package com.ibm.dbwkl.request.connection;

import java.sql.Connection;
import java.sql.SQLException;

import com.ibm.db2.jcc.DB2BaseDataSource;
import com.ibm.db2.jcc.DB2SimpleDataSource;

/**
 *
 */
public class DB2zSimpleDataSource extends DB2zDataSource {

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.workloadService.connectionManager.ConnectionManager2#getConnectionInternal()
	 */
	@Override
	public Connection getConnection(DB2BaseDataSource dataSource, String user, String password) throws SQLException {

		DB2SimpleDataSource _dataSource = (DB2SimpleDataSource) dataSource;
		Connection con = _dataSource.getConnection(user,password);			
		
		return con;
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.workloadService.connectionManager.ConnectionManager2#initializeDataSourceInternal()
	 */
	@Override
	protected DB2BaseDataSource initializeDataSourceInternal() {
		return new DB2SimpleDataSource();
	}
}
