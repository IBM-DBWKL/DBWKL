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
package com.ibm.dbwkl.workloadtypes.viewmodel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.ibm.dbwkl.DB2WorkloadServiceDirectory;
import com.ibm.dbwkl.helper.CaseInsensitiveMap;
import com.ibm.dbwkl.helper.JarUtility;
import com.ibm.dbwkl.helper.StringUtility;
import com.ibm.dbwkl.workloadtypes.model.Workload;
import com.ibm.dbwkl.workloadtypes.model.WorkloadClass;
import com.ibm.dbwkl.workloadtypes.model.WorkloadDescription;
import com.ibm.dbwkl.workloadtypes.model.WorkloadLongName;
import com.ibm.dbwkl.workloadtypes.model.WorkloadShortName;

/**
 *
 */
public class WorkloadClassViewModel {

	/**
	 * List of all workload classes (with this the actual binary compilation unit
	 * in a JAR is meant
	 */
	private List<WorkloadClass> workloadClasses;
	
	/**
	 * Default constructor 
	 * @throws IOException Could not load the workload jars
	 */
	public WorkloadClassViewModel() throws IOException {
		
		setWorkloadClasses(new ArrayList<WorkloadClass>());
		
		loadWorkloadClasses();
	}
	
	/**
	 * Returns the workload class independent of the name given
	 * 
	 * @param name name of the workload which can be either the class name, 
	 * the simple class name, the long name (without spaces) or the short name
	 * 
	 * @return workload class 
	 */
	public WorkloadClass getWorkloadClass(String name) {
		
		// search the list of available workloads
		for (WorkloadClass workloadClass : getWorkloadClasses()) {
			if (StringUtility.equalsIgnoreCase(StringUtility.getSimpleClassName(workloadClass.getName()), name) ||
					StringUtility.equalsIgnoreCase(workloadClass.getName(), name) ||
					StringUtility.equalsIgnoreCase(workloadClass.getLongName(), name) ||
					StringUtility.equalsIgnoreCase(workloadClass.getShortName(), name)) {
				
				return workloadClass;
			}
		}
		
		return null;
	}
	
	/**
	 * Loads the workload classes from the actual workload libraries
	 * 
	 * @throws IOException Could not load the workload jars
	 */
	private void loadWorkloadClasses() throws IOException {
		
		// load the workload jars using URLClassLoader into current JVM
		try {
			// get all workload names
			CaseInsensitiveMap workloadNames = JarUtility.loadWklNamesWithPackages(
					DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryWorkloads());
			
			// search for the URLs
			File workloadsDirectory = new File(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryWorkloads());
			ArrayList<URL> workloadLibraryUrls = new ArrayList<URL>();
			ArrayList<URI> workloadLibraryUris = new ArrayList<URI>();
			for (File workloadLibrary : workloadsDirectory.listFiles()) {
				workloadLibraryUris.add(workloadLibrary.toURI());
			}
			for (URI uri : workloadLibraryUris) {
				workloadLibraryUrls.add(uri.toURL());
			}
			
			// add the request jar to have all the workload dependencies available
			workloadLibraryUrls.add(new File(DB2WorkloadServiceDirectory.getDB2WorkloadServiceRequestJar()).toURI().toURL());
			
			// load the workload libraries
			URLClassLoader ucl = new URLClassLoader(workloadLibraryUrls.toArray(new URL[]{}), WorkloadClassViewModel.class.getClassLoader());
			
			// go through the list of classes found and identify them
			for (Entry<String, String> workloadClassInfo : workloadNames.entrySet()) {
				Class<?> workloadClassBinary;
				try {
					workloadClassBinary = Class.forName(workloadClassInfo.getKey(), false, ucl);
					
					Object wcl = workloadClassBinary.newInstance();

					// first check if the class that was found is actually marked as a workload
					if (wcl.getClass().isAnnotationPresent(Workload.class)) {
						
						WorkloadClass workloadClass = new WorkloadClass();
						workloadClass.setLongName(wcl.getClass().getSimpleName());
						workloadClass.setName(wcl.getClass().getName());
						workloadClass.setJarFileName(workloadClassInfo.getValue());
						
						// get the long name
						WorkloadLongName antWorkloadLongName = wcl.getClass().getAnnotation(WorkloadLongName.class);
						if (antWorkloadLongName != null) {
							workloadClass.setLongName(antWorkloadLongName.value());
						}
						
						// get the short name
						WorkloadShortName antWorkloadShortName = wcl.getClass().getAnnotation(WorkloadShortName.class);
						if (antWorkloadShortName != null) {
							workloadClass.setShortName(antWorkloadShortName.value());
						}
						
						// get description
						WorkloadDescription antWorkloadDescription = wcl.getClass().getAnnotation(WorkloadDescription.class);
						if (antWorkloadDescription != null) {
							workloadClass.setDescription(antWorkloadDescription.value());
						}
						
						this.workloadClasses.add(workloadClass);
					}
					
				} catch (Exception e) {
					// do not throw anything here because it is ok to receive an exception
					// when a class was found that is no error
				} catch (Error e) {
					// do not throw anything here because it is ok to receive an error
					// when a class was found that is no error
				}
			}				
			
		} catch (MalformedURLException e) {
			MalformedURLException mue = new MalformedURLException("Getting the workload lib URLs failed: " + e.getLocalizedMessage());
			mue.initCause(e);
			throw mue;
		} catch (IOException e) {
			IOException ioe = new IOException("Getting the workload names failed: " + e.getLocalizedMessage());
			ioe.initCause(e);
			throw ioe;
		}

		
	}

	/**
	 * @param workloadClasses the workloadClasses to set
	 */
	public void setWorkloadClasses(List<WorkloadClass> workloadClasses) {
		this.workloadClasses = workloadClasses;
	}

	/**
	 * @return the workloadClasses
	 */
	public List<WorkloadClass> getWorkloadClasses() {
		return this.workloadClasses;
	}
	
}
