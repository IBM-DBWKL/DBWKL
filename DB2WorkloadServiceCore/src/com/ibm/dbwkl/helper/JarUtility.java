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
package com.ibm.dbwkl.helper;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.ibm.dbwkl.DB2WorkloadServiceDirectory;

/**
 * 
 */
public class JarUtility {

	/**
	 * Retrieve the full class name of the workload in the workload jar files under the default '/workloads' directory.
	 * @param wklName the workload name
	 * @return the full class name (with package names). Null if the workload doesn't exist.
	 * @throws IOException 
	 */
	public static String getWklClassName(String wklName) throws IOException {
		
		return getWklClassName(wklName, DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryWorkloads());
	}
	
	/**
	 * Search through a directory or a jar file for the workload's class name.
	 * @param wklName the Workload Name
	 * @param dir The directory name, or the jar file name
	 * @return the workload's class name if it is found; otherwise, null. 
	 * @throws IOException 
	 */
	public static String getWklClassName(String wklName, String dir) throws IOException{
			
		//if only a simple jar file name, we look for it in the default /workloads directory.
		File jarPath = null;
		
		if(!dir.contains("/") && StringUtility.endsWithIgnoreCase(dir, ".jar")){
			jarPath = new File(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryWorkloads() + dir);
		} else {
			jarPath = new File(dir);
		}
		
		File[] jarFiles = null;
		
		if(jarPath.isDirectory()){
			//if it's a directory, search through all the jar files 
			jarFiles = jarPath.listFiles(new JarFileFilter());
			if(jarFiles.length == 0){
				return null;
			}
		} else if(jarPath.isFile() && StringUtility.endsWithIgnoreCase(jarPath.getName(), ".jar")){
			//if it's a jar file
			jarFiles = new File[] { jarPath };
		} else {
			return null;
		}
		
		// search through all jar files under the directory, or the jar file.
		for (int i = 0; i < jarFiles.length; i++) {
			
			JarFile jar = new JarFile(jarFiles[i]);
			//search through all files in the jar
			Enumeration<JarEntry> e = jar.entries();
			while (e.hasMoreElements()) {
				JarEntry je = e.nextElement();
				// TODO: WARNING: if inner classes are moved out as individual java files, there will be a problem. 
				// Search through all class files, except inner classes(with $). 
				// Under the assumption that each class file is a workload and no workloads(in different jars) are with the same name.
				if (je.isDirectory() 
						|| !StringUtility.endsWithIgnoreCase(je.getName(), ".class")
						|| je.getName().contains("$")) {
					continue;
				}
				// -6 because of .class
				String className = je.getName().substring(0, je.getName().length() - 6).replace('/', '.');
				if (StringUtility.endsWithIgnoreCase(className, wklName)) {
					jar.close();
					return className;
				}
			}
			jar.close();
		}
		return null;
	}
	
	
	/**
	 * load all Workloads' names from the jar files in the default '/workloads' directory.
	 * @return an ArrayList<String> contains all the Workloads' Names
	 * @throws IOException 
	 */
	public static CaseInsensitiveMap loadWklNames() throws IOException{
		
		return loadWklNames(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryWorkloads());
	}
	
	/**
	 * Load all Workloads' names from the specified directory or the jar file.
	 * @param dir The directory name, or the jar file name
	 * @return an ArrayList<String> contains all the Workloads' Names
	 * @throws IOException 
	 */
	public static CaseInsensitiveMap loadWklNames(String dir) throws IOException{
		
		CaseInsensitiveMap wklNames = new CaseInsensitiveMap();
		
		//if only a simple jar file name, we look for it in the default /workloads directory.
		File jarPath = null;
		if(!dir.contains("/") && StringUtility.endsWithIgnoreCase(dir, ".jar")){
			jarPath = new File(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryWorkloads() + dir);
		} else { 
			jarPath = new File(dir);
		}

		File[] jarFiles = null;
		if(jarPath.isDirectory()){
			//if it's a directory, search through all the jar files 
			jarFiles = jarPath.listFiles();
			if(jarFiles.length == 0){
				return wklNames;
			}
		} else if(jarPath.isFile() && StringUtility.endsWithIgnoreCase(jarPath.getName(), ".jar")){
			//if it's a jar file
			jarFiles = new File[] { jarPath };
		} else {
			return wklNames;
		}
		
		// search through all jar files under the directory, or the jar file.
		for (int i = 0; i < jarFiles.length; i++) {
			if (!StringUtility.endsWithIgnoreCase(jarFiles[i].getName(), ".jar")) {
				continue;
			}
			JarFile jar = new JarFile(jarFiles[i]);
			//search through all files in the jar
			Enumeration<JarEntry> e = jar.entries();
			while (e.hasMoreElements()) {
				JarEntry je = e.nextElement();
				// WARNING: if inner classes are moved out as individual java files, there will be a problem. 
				// Search through all class files, except inner classes(with $). 
				// Under the assumption that each class file is a workload and no workloads(in different jars) are with the same name.
				if (je.isDirectory() 
						|| !StringUtility.endsWithIgnoreCase(je.getName(), ".class")
						|| je.getName().contains("$")) {
					continue;
				}
				// -6 because of .class
				String className = je.getName().substring(0, je.getName().length() - 6).replace('/', '.');
				// discard the package name
				String simpleName = className.substring(className.lastIndexOf(".") + 1);
				wklNames.put(simpleName, className);
			}
			
			jar.close();
		
		}

		return wklNames;
	}
	
	/**
	 * Load all Workloads' names from the specified directory or the jar file.
	 * @param dir The directory name, or the jar file name
	 * @return an ArrayList<String> contains all the Workloads' Names
	 * @throws IOException 
	 */
	public static CaseInsensitiveMap loadWklNamesWithPackages(String dir) throws IOException{
		
		CaseInsensitiveMap wklNames = new CaseInsensitiveMap();
		
		//if only a simple jar file name, we look for it in the default /workloads directory.
		File jarPath = null;
		if(!dir.contains("/") && StringUtility.endsWithIgnoreCase(dir, ".jar")){
			jarPath = new File(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryWorkloads() + dir);
		} else { 
			jarPath = new File(dir);
		}

		File[] jarFiles = null;
		if(jarPath.isDirectory()){
			//if it's a directory, search through all the jar files 
			jarFiles = jarPath.listFiles();
			if(jarFiles.length == 0){
				return wklNames;
			}
		} else if(jarPath.isFile() && StringUtility.endsWithIgnoreCase(jarPath.getName(), ".jar")){
			//if it's a jar file
			jarFiles = new File[] { jarPath };
		} else {
			return wklNames;
		}
		
		// search through all jar files under the directory, or the jar file.
		for (int i = 0; i < jarFiles.length; i++) {
			if (!StringUtility.endsWithIgnoreCase(jarFiles[i].getName(), ".jar")) {
				continue;
			}
			JarFile jar = new JarFile(jarFiles[i]);
			//search through all files in the jar
			Enumeration<JarEntry> e = jar.entries();
			while (e.hasMoreElements()) {
				JarEntry je = e.nextElement();
				// WARNING: if inner classes are moved out as individual java files, there will be a problem. 
				// Search through all class files, except inner classes(with $). 
				// Under the assumption that each class file is a workload and no workloads(in different jars) are with the same name.
				if (je.isDirectory() 
						|| !StringUtility.endsWithIgnoreCase(je.getName(), ".class")
						|| je.getName().contains("$")) {
					continue;
				}
				// -6 because of .class
				String className = je.getName().substring(0, je.getName().length() - 6).replace('/', '.');

				wklNames.put(className, jarFiles[i].getName());
			}
			
			jar.close();
		
		}

		return wklNames;
	}
	
	
}
