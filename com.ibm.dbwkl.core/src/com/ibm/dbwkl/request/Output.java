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
package com.ibm.dbwkl.request;

import org.w3c.dom.Element;

import com.ibm.staf.STAFResult;

/**
 * Stephan Arenswald (arens@de.ibm.com)
 *
 */
public class Output {

	/**
	 * This enumeration contains all available output formats
	 * 
	 * Stephan Arenswald (arens@de.ibm.com)
	 *
	 */
	public static enum Format {
		/**
		 * Output as human readable text including a header  
		 */
		TEXT,
		
		/**
		 * Output as XML. This is primarily used for the UI 
		 */
		XML,
		
		/**
		 * Output as STAF. This is primarily used for test automation where
		 * the result of the request is important
		 */
		STAF
	}
	
	/**
	 * Global output format setting
	 */
	public static Output.Format format = Output.Format.TEXT; 
	
	/**
	 * Format used by the request
	 */
	private Format _format = Output.format;

	/**
	 * Default constructor 
	 */
	public Output() {
		// 
	}
	
	/**
	 * Returns the result of the request as human readable text
	 * 
	 * @return the request result as text
	 */
	public String getText() {
		return null;
	}
	
	/**
	 * Returns the result of the request as XML. This is primarily
	 * used for the UI.
	 * 
	 * @return the request result as XML ({@link Element})
	 */
	public Element getXML() {
		return null;
	}
	
	/**
	 * Returns the result of the request as STAF formatted output
	 * 
	 * @return the request as {@link STAFResult} object
	 */
	public STAFResult getSTAFResult() {
		return null;
	}

	/**
	 * @param _format the _format to set
	 */
	public void setFormat(Format _format) {
		this._format = _format;
	}

	/**
	 * @return the _format
	 */
	public Format getFormat() {
		return this._format;
	}
}
