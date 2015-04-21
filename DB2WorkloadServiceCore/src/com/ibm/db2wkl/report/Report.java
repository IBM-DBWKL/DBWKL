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

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Vector;

/**
 * A report summarize the current resource allocations of a request. 
 * 
 * It implements {@link java.io.Serializable} interface so that it can be passed as arguments or return value in RMI
 * 
 * The number are presented as double, because this class can also be used to present an average value of reports.
 * 
 * 
 *
 */
public class Report implements Serializable {

	/**
	 * auto generated
	 */
	private static final long serialVersionUID = -8724042816023937108L;

	/**
	 * creation time stamp
	 */
	private Date timeStamp;
	
	/**
	 * number format when print the report to string
	 */
	private NumberFormat numberFormat = null;
	
	/**
	 * Number of threads started in the request
	 */
	private double number_of_threads = 0;
	
	/**
	 * Number of DB2 Connections opened in the request
	 */
	private double number_of_db2Connection = 0;
	
	/**
	 * Number of Statements created in the request(it refers to the number of created java.sql.Statement object, not executed SQL Statement)
	 */
	private double number_of_statements = 0;
	
	/**
	 * Number of sockets opened in the request
	 */
	private double number_of_sockets = 0;
	
	/**
	 * Consumed Memory(bytes) in the JVM of the request
	 */
	private double consumed_memory = 0;
	
	/**
	 * 
	 */
	public Report(){
		this.timeStamp = new Date(); 
		this.numberFormat = NumberFormat.getNumberInstance();
		this.numberFormat.setMaximumFractionDigits(2);
	}


	/**
	 * @param number_of_threads the number_of_threads to set
	 */
	public void setNumber_of_threads(double number_of_threads) {
		this.number_of_threads = number_of_threads;
	}

	/**
	 * @return the number_of_threads
	 */
	public double getNumber_of_threads() {
		return this.number_of_threads;
	}

	/**
	 * @param number_of_db2Connection the number_of_db2Connection to set
	 */
	public void setNumber_of_db2Connection(double number_of_db2Connection) {
		this.number_of_db2Connection = number_of_db2Connection;
	}

	/**
	 * @return the number_of_db2Connection
	 */
	public double getNumber_of_db2Connection() {
		return this.number_of_db2Connection;
	}

	/**
	 * @param number_of_statements the number_of_statements to set
	 */
	public void setNumber_of_statements(double number_of_statements) {
		this.number_of_statements = number_of_statements;
	}

	/**
	 * @return the number_of_statements
	 */
	public double getNumber_of_statements() {
		return this.number_of_statements;
	}

	/**
	 * @param number_of_sockets the number_of_sockets to set
	 */
	public void setNumber_of_sockets(double number_of_sockets) {
		this.number_of_sockets = number_of_sockets;
	}

	/**
	 * @return the number_of_sockets
	 */
	public double getNumber_of_sockets() {
		return this.number_of_sockets;
	}

	/**
	 * @param consumed_memory the consumed_memory to set
	 */
	public void setConsumed_memory(double consumed_memory) {
		this.consumed_memory = consumed_memory;
	}

	/**
	 * @return the consumed_memory
	 */
	public double getConsumed_memory() {
		return this.consumed_memory;
	}
	
	/**
	 * @return time stamp of the create time of the report
	 */
	public Date getTimestamp(){
		return this.timeStamp;
	}
	
	@Override
	public boolean equals(Object o){
		if(o != null && o instanceof Report){
			Report report = (Report) o;
			if(report.getConsumed_memory() == this.getConsumed_memory() && report.getNumber_of_db2Connection() == this.getNumber_of_db2Connection()
				&&report.getNumber_of_sockets() == this.getNumber_of_sockets() && report.getNumber_of_statements() == report.getNumber_of_statements()
				&& report.getNumber_of_threads() == this.getNumber_of_threads()){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public String toString(){
		return "Number of Threads = " + this.numberFormat.format(getNumber_of_threads()) + "; " + 
		       "Number of DB2 Connections = " + this.numberFormat.format(getNumber_of_db2Connection()) + "; " +
		       "Number of Statements = " + this.numberFormat.format(getNumber_of_statements()) + "; " +
		       "Number of Sockets = " + this.numberFormat.format(getNumber_of_sockets()) + "; " +
		       "Consumed Memory(MB) = " + this.numberFormat.format(getConsumed_memory()/1024/1024) + "; ";
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	/**
	 * @param report
	 */
	public void add(Report report){
		if(report != null) {
			this.setConsumed_memory(this.getConsumed_memory() + report.getConsumed_memory());
			this.setNumber_of_db2Connection(this.getNumber_of_db2Connection() + report.getNumber_of_db2Connection());
			this.setNumber_of_sockets(this.getNumber_of_sockets() + report.getNumber_of_sockets());
			this.setNumber_of_statements(this.getNumber_of_statements() + report.getNumber_of_statements());
			this.setNumber_of_threads(this.getNumber_of_threads() + report.getNumber_of_threads());
		}
	}
	
	/**
	 * @param reports
	 * @return average of the reports
	 */
	public static Report average(Vector<Report> reports){
		Report average = new Report();
		if(reports != null && reports.size() > 0) {
			for(Report report : reports){
				average.add(report);
			}
			average.setConsumed_memory(average.getConsumed_memory() / reports.size());
			average.setNumber_of_db2Connection(average.getNumber_of_db2Connection() / reports.size());
			average.setNumber_of_sockets(average.getNumber_of_sockets() / reports.size());
			average.setNumber_of_statements(average.getNumber_of_statements() / reports.size());
			average.setNumber_of_threads(average.getNumber_of_threads() / reports.size());
		}
		return average;
	}
}
