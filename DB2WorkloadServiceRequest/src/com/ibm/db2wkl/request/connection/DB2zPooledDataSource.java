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
package com.ibm.db2wkl.request.connection;

import java.sql.Connection;
import java.sql.SQLException;

import com.ibm.db2.jcc.DB2BaseDataSource;
import com.ibm.db2.jcc.DB2ConnectionPoolDataSource;

/**
 *
 */
public class DB2zPooledDataSource extends DB2zDataSource {

	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadService.connectionManager.ConnectionManager2#getConnectionInternal()
	 */
	@Override
	public Connection getConnection(DB2BaseDataSource dataSource, String user, String password) throws SQLException {

		DB2ConnectionPoolDataSource _dataSource = (DB2ConnectionPoolDataSource) dataSource;
		Connection con = _dataSource.getDB2PooledConnection(
				user, 
				password,
				null).getConnection();
		
		return con;
	}

	/* (non-Javadoc)
	 * @see com.ibm.db2wkl.workloadService.connectionManager.ConnectionManager2#initializeDataSourceInternal()
	 */
	@Override
	protected DB2BaseDataSource initializeDataSourceInternal() {
		return new DB2ConnectionPoolDataSource();
	}
}
