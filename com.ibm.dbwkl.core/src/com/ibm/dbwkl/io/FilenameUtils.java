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
package com.ibm.dbwkl.io;

import java.io.File;

/**
 * File Name Utilities
 * 
 *
 */
public class FilenameUtils {

	/**
	 * Gets the base name, minus the full path and extension, from a full filename.
	 * 
	 * This method will handle a file in either Unix or Windows format. The text after the last forward or backslash and before the last dot is returned.
	 * 
	 * <code>
	 * a/b/c.txt --> c
	 * a.txt     --> a
	 * a/b/c     --> c
	 * a/b/c/    --> ""
	 * </code>
	 * 
	 * The output will be the same irrespective of the machine that the code is running on.
	 * 
	 * @param filename the filename to query, null returns null
	 * @return the name of the file without the path, or an empty string if none exists
	 */
	public static String getBaseName(String filename) {
		
		if(filename == null)
			return null;
		
		File file = new File(filename);
		String name = file.getName();
		
		String nameWithoutExtension = name.substring(0, name.lastIndexOf("."));
		
		return nameWithoutExtension;
	}

}
