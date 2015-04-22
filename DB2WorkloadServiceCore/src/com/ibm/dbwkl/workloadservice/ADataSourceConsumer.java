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
package com.ibm.dbwkl.workloadservice;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import com.ibm.dbwkl.helper.ThreadGroupLocal;

/**
 * It is used to define for every thread his own url.
 * For example Workload, SQL and SPM.
 * 
 */
public abstract class ADataSourceConsumer {
 
	/**
	 * URL to the database on which the workload should run.
	 */
	private static ThreadGroupLocal<String> url = new ThreadGroupLocal<String>();
	
	/**
	 * List of statements in the workload
	 */
	private HashMap<Long, String> statements = new HashMap<Long, String>();
	
	/**
	 * The URL of the Workload
	 */
	public String workloadURL;

	/**
	 * @return workloadURL
	 */
	public String getWorkloadURL() {
		return this.workloadURL;
	}

	/**
	 * @param workloadURL
	 */
	public void setWorkloadURL(String workloadURL) {
		this.workloadURL = workloadURL;
	}
	
	/**
	 * Registers/Caches a statement text which can then be used to run statically 
	 *
	 * @param id the statement id to identify the statement 
	 * @param statementText the statement text
	 * 
	 * @throws Exception the key already exists
	 */
	@SuppressWarnings("boxing")
	protected final void registerStatement(long id, String statementText) throws Exception {
		if (this.statements.containsKey(id)) {
			throw new Exception("Statement ID key already exists");
		}
		this.statements.put(id, statementText);
	}
	
	/**
	 * Returns the statement text identified by the given ID
	 * 
	 * @param statementId statement ID
	 * @return the statement text
	 */
	@SuppressWarnings({ "boxing" })
	public final String getStatement(long statementId) {
		return this.statements.get(statementId);
	}
	
	/**
	 * Returns all statements without the ID
	 * 
	 * @return statements
	 */
	public final Collection<String> getStatementTexts() {
		return this.statements.values();
	}
	
	/**
	 * @return the URL of the workload
	 */
	public static final String getUrl() {	
		return ADataSourceConsumer.url.get();
	}

	/**
	 * @param url
	 */
	public static final void setUrl(String url) {
		ADataSourceConsumer.url.set(url);
	}
	
	/**
	 * @return the name of the DataSourceConsumer
	 */
	public abstract String getName();
	
	/**
	 * @return WorkloadStatus
	 */
	public abstract WorkloadStatus getStatus();
	
	/**
	 * @return ID
	 */
	public abstract Long getId();
	
	/**
	 * @return EndDate
	 */
	public abstract Date getEndDate();
	
	/**
	 * @return StartDate
	 */
	public abstract Date getStartDate();

}
