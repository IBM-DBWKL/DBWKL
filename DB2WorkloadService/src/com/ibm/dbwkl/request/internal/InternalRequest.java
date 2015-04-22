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
package com.ibm.dbwkl.request.internal;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.dbwkl.request.ARequest;
import com.ibm.staf.STAFResult;

/**
 *
 */
public abstract class InternalRequest extends ARequest implements Runnable{
	
	/**
	 * @return staf result
	 */
	protected abstract STAFResult execute();
	 
	/**
	 * @return xml output
	 * @throws ParserConfigurationException 
	 */
	protected Document getXML() throws ParserConfigurationException {
		
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
	 * @return text output
	 */
	protected String getText(){
		// create the string builder that will hold the result
		StringBuilder builder = new StringBuilder();
			
		builder.append(this.result.result);
		
		return builder.toString();
	}
	
	
	@Override
	public void run(){
		
		this.setResult(execute());
		
	}	
}
