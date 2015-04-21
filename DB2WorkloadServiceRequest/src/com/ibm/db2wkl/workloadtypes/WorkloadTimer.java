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
package com.ibm.db2wkl.workloadtypes;

import java.util.Date;

/**
 *
 */
class WorkloadTimer {
	
	/**
	 * <p>This date object, contains the date of the creation of the object.</p>
	 */
	private Date creationDate;
	
	/**
	 * <p>This date object, contains the date of the workload start.</p>
	 */
	private Date startDate;
	
	/**
	 * <p>This date object, contains the date of the workload end.</p>
	 */
	private Date endDate;

	/**
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(Date creationDate) {
		
		this.creationDate = creationDate;
	}

	/**
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		
		return this.creationDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		
		this.startDate = startDate;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		
		return this.startDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		
		this.endDate = endDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		
		return this.endDate;
	}
}