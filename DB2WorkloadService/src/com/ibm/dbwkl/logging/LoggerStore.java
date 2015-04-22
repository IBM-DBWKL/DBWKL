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
package com.ibm.dbwkl.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.dbwkl.helper.StringUtility;
import com.ibm.dbwkl.logging.LoggerEntry;

/**
 * This class stores all log entries.
 * 
 */
public class LoggerStore {

	/**
	 * The maximal number of stored entries
	 */
	private int storeLimit = 100;

	/**
	 * A list of stored entries
	 */
	private final ArrayList<LoggerEntry> entries = new ArrayList<LoggerEntry>();
	
	/**
	 * @return the stored entries
	 */
	public ArrayList<LoggerEntry> getEntries() {
	
		return this.entries;
	}
	
	/**
	 * Returns the last n log entries and filters for the level
	 * 
	 * @param n if n > 0 : number of last log entries; if n <= 0 : all the log entries
	 * @param levels levels to filter on
	 * @return last n log entries
	 */
	public List<LoggerEntry> getEntries(int n, String[] levels) {
		
		List<LoggerEntry> result = new ArrayList<LoggerEntry>();
		
		// costly operation, thus make sure that no other thread is currently logging
		synchronized (this.entries) {
			int num = 0;
			if(n <= 0){
				num = this.entries.size();
			} else { 
				num = n;
			}
			int found = 0;
			int current = this.entries.size() - 1;
			while (found < num && current >= 0) {
				if (StringUtility.arrayContainsIgnoreCase(levels, this.entries.get(current).getLevel().trim())) {
					result.add(this.entries.get(current));
					found++;
				}
				current--;
			}
			
		}
		//let the list be chronological
		Collections.reverse(result);
		return result;
	}
	
	/**
	 * Get the last n log entries.
	 * @param n if n > 0 : number of last log entries; if n <= 0 : all the log entries
	 * @return the stored entries
	 */
	public List<LoggerEntry> getEntries(int n) {
	
		if(n <= 0){
			return getEntries();
		}
		
		// costly operation, thus make sure that no other thread is currently logging
		synchronized (this.entries) {
			
			int first = this.entries.size() - n;
			return this.entries.subList(first < 0 ? 0 : first, this.entries.size());

		}
		
	}

	/**
	 * Returns the last n log entries and filters for the level and reqID
	 * 
	 * @param n if n > 0 : number of last log entries if n <= 0 : all the log entries
	 * @param levels The levels to filter on
	 * @param reqid The request ID to filter on
	 * @return last n log entries
	 */
	public List<LoggerEntry> getEntries(int n, String[] levels, int reqid) {

		List<LoggerEntry> result = new ArrayList<LoggerEntry>();

		// costly operation, thus make sure that no other thread is currently logging
		synchronized (this.entries) {
			int num = 0;
			if(n <= 0){
				num = this.entries.size();
			} else { 
				num = n;
			}
			int found = 0;
			int current = this.entries.size() - 1;
			while (found < num && current >= 0) {
				if (StringUtility.arrayContainsIgnoreCase(levels, this.entries.get(current).getLevel().trim())
						&& this.entries.get(current).getRequestName().equalsIgnoreCase("req" + reqid)) {
					result.add(this.entries.get(current));
					found++;
				}
				current--;
			}
		}
		//let the list be chronological
		Collections.reverse(result);
		return result;
	}

	/**
	 * Returns the last n log entries and filters for the reqID
	 * 
	 * @param n if n > 0 : number of last log entries if n <= 0 : all the log entries
	 * @param reqid The request ID to filter on
	 * @return last n log entries
	 */
	public List<LoggerEntry> getEntries(int n, int reqid) {

		List<LoggerEntry> result = new ArrayList<LoggerEntry>();
		
		// costly operation, thus make sure that no other thread is currently
		// logging
		synchronized (this.entries) {
			int num = 0;
			if(n <= 0){
				num = this.entries.size();
			} else {
				num = n;
			}
			int found = 0;
			int current = this.entries.size() - 1;
			while (found < num && current >= 0) {
				if (this.entries.get(current).getRequestName().equalsIgnoreCase("req" + reqid)) {
					result.add(this.entries.get(current));
					found++;
				}
				current--;
			}
		}
		
		Collections.reverse(result);
		return result;
	}

	/**
	 * @param entry to add to the list
	 */
	public void addEntry(LoggerEntry entry) {
		
		if(this.entries.size() == this.storeLimit) 
			this.entries.remove(0);
		
		this.entries.add(entry);
	}
	
	/**
	 * @return the maximal number of stored entries
	 */
	public int getStoreLimit() {
		
		return this.storeLimit;
	}

	/**
	 * Sets the maximal number of stored entries
	 * 
	 * @param storeLimit to set
	 */
	public void setStoreLimit(int storeLimit) {
	
		this.storeLimit = storeLimit;
	}
	
	/**
	 * @return the last log entry within the stored logger entries
	 */
	public LoggerEntry getLastLogEntry() {
		return this.entries.get(this.entries.size()-1);
	}
}