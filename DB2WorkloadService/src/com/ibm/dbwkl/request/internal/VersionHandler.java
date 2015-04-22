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

import java.io.IOException;

import org.w3c.dom.Document;

import com.ibm.dbwkl.helper.FileLoader;
import com.ibm.staf.STAFResult;

/**
 * This class provides the current version of the build
 * 
 *
 */
public class VersionHandler extends InternalRequest {

    /**
     * Current version number. Needs to be update for every new release!
     * 
     * @return the current version
     */
    public String getVersion() {
    	String version;
		try {
			version = FileLoader.getResource("/com/ibm/db2wkl/files/version.txt", this);
		} catch (IOException e) {
			version = e.getMessage();
		}
    	return version.trim();	
    }
    
	@Override
	public STAFResult execute() {
		
		return new STAFResult(STAFResult.Ok, "Version " + getVersion());
	}

	
	/**
	 * @return staf 
	 */
	public String getStaf() {

		return getVersion().toString();
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.request.internal.InternalRequest#getXML()
	 */
	@Override
	protected Document getXML() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.dbwkl.request.internal.InternalRequest#getText()
	 */
	@Override
	protected String getText() {
		// TODO Auto-generated method stub
		return null;
	}
}