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
package com.ibm.dbwkl.logging.helper;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This simple class implements a FilenameFilter to filter text log files. 
 *
 */
public class TextLogFileFilter implements FilenameFilter {

	/* (non-Javadoc)
	 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	 */
	
	@Override
	public boolean accept(File directory, String filename) {
        return (filename.startsWith("log") && filename.endsWith(".txt"));
    }

}
