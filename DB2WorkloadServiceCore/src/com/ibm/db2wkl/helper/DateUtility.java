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
package com.ibm.db2wkl.helper;

import java.util.Calendar;
import java.util.Date;

/**
 * <p>This class compares two date objects and will erase hour, minute, second and millisecond values of the provided date
 * objects so it will only detect changes on a daily base. </p>
 * 
 */
public class DateUtility {
	
	/**
	 * Default Constructor
	 */
	public DateUtility() {
		//
	}
	
	/**
	 * <p>Returns 0 if dates are equal</p>
	 * <p>Returns &gt;0 if date1 is after date2</p>
	 * <p>Returns &lt;0 if date1 is before date2</p>
	 * @param date1 - the first date to compare
	 * @param date2 - the second date to compare
	 * @return value 0 if dates are equal, value less than 0 if date1 is before date2, value large than 0 if date1 is after date2
	 */
	public int compare(Date date1, Date date2) {
		
		Date cutDate1 = cutDate(date1);
		Date cutDate2 = cutDate(date2);
	
		return cutDate1.compareTo(cutDate2);
	}
	
	/**
	 * This method deletes hours, minutes, seconds and milliseconds from a date object so that it only contains year, month and day values.
	 * @param date
	 * @return a date object that only contains year, month and day values.
	 */
	private Date cutDate(Date date) {
		
		Calendar cal = Calendar.getInstance();
		cal.setTime( date );
		cal.set( Calendar.HOUR_OF_DAY, 0 );
		cal.set( Calendar.MINUTE, 0 );
		cal.set( Calendar.SECOND, 0 );
		cal.set( Calendar.MILLISECOND, 0 );
		
		return cal.getTime();
	}
	
}
