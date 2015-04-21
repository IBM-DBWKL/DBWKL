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
package com.ibm.db2wkl.request;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.staf.STAFResult;

/**
 * <p>This class represents an abstract subrequest. A subrequest is an object,
 * that can be requested with a command. When the user enters a command, the acceptRequest
 * method of the class DB2WorkloadService will accept this command. It will iterate
 * trough the list of all registered receivers. If it finds a requested receiver, it will
 * pass the whole command to it. After that it will go on with iterating trough the list.</p>
 * 
 * Use Annotation "@DataSourceRequired" to annotate a subrequest if it requires datasource. 
 * 
 * <p>This class will also registre all receivers in the package <code>com.ibm.db2wkl.receiver.receivers
 * </code> to the class Db2WorkloadService.<p>
 * 
 */
public abstract class ASubRequest implements Runnable {

	/**
	 * The ID of that subrequest within the request
	 */
	private int id = Integer.MIN_VALUE;
	
	/**
	 * Name of the request
	 */
	private String name = ":";
	
	/**
	 * The request for this subrequest
	 */
	private ARequest request;
	
	/**
	 * Result of this request
	 */
	private STAFResult result;
	
	/**
	 * The url that this subrequest is used for in case  
	 * the URLS option is specified
	 */
	protected String url;
	
	/**
	 * List of sub request finished event listeners
	 */
	private List<SubRequestFinishedEventListener> subRequestFinishedEventListeners; 
	
	/**
	 * This is the abstract method, that must be implemented be each receiver object. The
	 * method acceptRequest of the class DB2WorkloadService will call this method, if a 
	 * user command was entered for this receiver.
	 * @param request 
	 * 
	 * @param handle STAF handle 
	 * @param requestInfo request information provided by STAF
	 * @param parseResult the parser result
	 * @return a result with an user message
	 */
	public abstract STAFResult acceptRequest();
	
	/**
	 * @return the action that will cause this receiver to be called
	 */
	public abstract String getAction();
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		this.result = acceptRequest();
		
		for (SubRequestFinishedEventListener listener : this.subRequestFinishedEventListeners) {
			listener.subRequestFinished(this);
		}
	}
	
	/**
	 * Adds the given listener to the list of sub request finished event listeners
	 * 
	 * @param eventListener the event listener
	 */
	public void addSubRequestFinishedEventListener(SubRequestFinishedEventListener eventListener) {
		if (this.subRequestFinishedEventListeners == null) {
			this.subRequestFinishedEventListeners = new ArrayList<SubRequestFinishedEventListener>();
		}
		this.subRequestFinishedEventListeners.add(eventListener);
	}
	
	/**
	 * Returns the result formatted as text. This method can be overridden by a 
	 * sub request if required.
	 * 
	 * @param result STAF result that was returned by the acceptRequest method
	 * @return the result formatted as text
	 */
	public String getText() {
		// create the string builder that will hold the result
		StringBuilder builder = new StringBuilder();
		
		builder.append(this.result.result);
		
		return builder.toString();
	}
	
	/**
	 * Returns the result formatted as STAF object. This method can be overridden by
	 * a sub request if required.
	 * 
	 * @param result STAF result that was returned by the acceptRequest method
	 * @return the result formatted as STAF object (JSON)
	 */
	public String getStaf() {
		// create the string builder that will hold the result
		StringBuilder builder = new StringBuilder();
		
		builder.append(this.result.result);
		
		return builder.toString();
	}
	
	/**
	 * Returns the result formatted as XML. This method can be overridden by a
	 * subrequest if required.
	 * 
	 * @param result STAF result that was returned by the accept Request method
	 * @return the result formatted as XML
	 * @throws ParserConfigurationException 
	 */
	public Document getXML() throws ParserConfigurationException {
		
			// create the document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		
			// create the document root node
		Element root = document.createElement("db2wklresult");
		document.appendChild(root);
		
			// create the result information
		Element rc = document.createElement("rc");
		rc.setTextContent(Integer.toString(this.result.rc));
			
		Element message = document.createElement("result");
		message.setTextContent(this.result.result);
			
		Element object = document.createElement("resultObject");
		object.setTextContent(this.result.resultObj == null ? "" : this.result.resultObj.toString());

			// add the result info nodes
		root.appendChild(rc);
		root.appendChild(message);
		root.appendChild(object);
			
			// return the resulting document
		return document;
	}
	
	/**
	 * Stop the subrequest
	 */
	public abstract void stopForce();

	/**
	 * @return list of DataSourceConsumer 
	 */
	public abstract ADataSourceConsumer getDataSourceConsumer();

	/**
	 * @return the id
	 */
	public int getId() {
		return this.id;
	}
	
	/**
	 * sets the id
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * @param name
	 */
	public void setName(String name){
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param request the request to set
	 */
	public void setRequest(ARequest request) {
		this.request = request;
	}

	/**
	 * @return the request
	 */
	public ARequest getRequest() {
		return this.request;
	}
	
	/**
	 * @return result 
	 */
	public STAFResult getResult() {
		return this.result;
	}
	
	/**
	 * @param result
	 */
	public void setResult(STAFResult result){
		this.result  = result;
	}
	
	/**
	 * @return the url
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}


}