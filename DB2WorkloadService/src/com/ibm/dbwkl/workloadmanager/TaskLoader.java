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
package com.ibm.dbwkl.workloadmanager;

import java.io.File;
import java.util.ArrayList;

import com.ibm.dbwkl.DB2WorkloadServiceDirectory;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.Logger;

/**
 * This class shows all possible tasks in the default workload service directory
 * 
 * 
 */
public class TaskLoader {

	/**
	 * Singelton TaskLoader Instance
	 */
	private static TaskLoader taskLoader;

	/**
	 * ArrayList with all tasks in the default directory
	 */
	private ArrayList<String> tasklist;

	/**
	 * path of the default task folder
	 */
	private static final String TASKSPATH = DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryTasks();

	/**
	 * default constructor
	 */
	public TaskLoader() {
		this.tasklist = new ArrayList<String>();
	}

	/**
	 * @return tasklist with all tasks in the default directory
	 */
	public ArrayList<String> loadTasks() {
		Logger.log("Looking for tasks in directory " + TASKSPATH, Logger.Info);

		File folder = new File(TASKSPATH);
		File[] fileList = folder.listFiles();

		int i = 0;
		for (File file : fileList) {
			if (file.getName().endsWith(".xml")) {
				i++;
				this.tasklist.add(file.getName().replace(".xml", ""));
			}
		}

		if (this.tasklist.size() != 0) {
			Logger.log("Found " + i + " stored procedure tasks:", LogLevel.Info);
		} else {
			Logger.log("Zero Stored Procedures Tasks found", LogLevel.Info);
		}
		return this.tasklist;
	}

	/**
	 * @return taskloader as Singelton Instance
	 */
	public static TaskLoader getInstance() {
		if (taskLoader == null) {
			taskLoader = new TaskLoader();
		}
		return taskLoader;
	}
}
