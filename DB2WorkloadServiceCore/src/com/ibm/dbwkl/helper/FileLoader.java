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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import com.ibm.dbwkl.DB2WorkloadServiceDirectory;

/**
 * <p>This utility class will simply load a file. You can load a file with a
 * absolute path or a relative path.</p>
 * 
 * <p><b>Note:</b> If you load a file, you should use this class to make sure, that
 * the relative paths are resolved correctly and the logging is performed in the right
 * way!</p>
 * 
 */
public class FileLoader {
    
    /**
     * The base directory of the file loader. You can load files, relative
     * to this folder.
     */
    private final String baseDirectory;
    
    /**
     * @param baseDirectory the base directory to load by default as relative paths
     * @param FileLoader constructor with own base directory to set
     */
    public FileLoader(String baseDirectory) {

    	if(baseDirectory.endsWith("/") || baseDirectory.endsWith("\\"))
    		this.baseDirectory = baseDirectory;
    	else
    		this.baseDirectory = baseDirectory + File.separator;
	}

    /**
     * @return default FileLoader with base directory set to the WorkloadService directory
     */
    public static FileLoader getDefaultFileLoader() {
    	
    	return new FileLoader(DB2WorkloadServiceDirectory.getDB2WorkloadServiceDirectory());
    }
    
	/**
     * <p>This method will simply load a File. If a absolute path is specified, it will
     * load the file directly. If a relative path is specified, it will load the file under the base directory
     * 
     * @param path of the file
     * @return the loaded file or null if no file exists
     */
    public File loadFile(String path)
    {	
    	if(isAbsolutePath(path)){
    		
    		return new File(path);
    	}
    	
    	// get the absolute path of the file
		String location = this.baseDirectory + path;
    	
		return new File(location);
    }
    
    /**
     * @param path as file path to load
     * @return boolean true if the path is Absolute
     */
    protected static boolean isAbsolutePath(String path) {
    	return new File(path).isAbsolute();
    }

	/**
	 * @param files as a String ArrayList with all written paths
	 * @return a file ArrayList with all Files
	 */
	public ArrayList<File> loadFiles(ArrayList<String> files) {

		ArrayList<File> loadedFiles = new ArrayList<File>();

		for(String file: files)
			loadedFiles.add(loadFile(file));
		
		return loadedFiles;
	}
    
    /**
     * This methods returns the whole content of the file as a string.
     * 
     * @param filepath of the file
     * @return the content of the file
     * @throws IOException 
     */
	public String readFile(String filepath) throws IOException
	{
		// we create our reader
		BufferedReader reader = null;
		
		// a string builder
		StringBuilder builder = new StringBuilder();
		
		try {
			// we get the file
			reader = new BufferedReader(new FileReader(loadFile(filepath)));

			// we read the file line by line into our variable raw
			String line = null;
			
			while ((line = reader.readLine()) != null)
				builder.append(line + "\n");
		} catch (IOException e)	{
			throw e;
		} finally {
			if(reader != null)
				reader.close();
		}

		return builder.toString();
	}
	
	/**
	 * Returns the content of a given file as string. Note that for huge files 
	 * this requires a lot of space in memory as the whole file is read at once.
	 * 
	 * @param file the file to read the complete content from
	 * @return content of a given file as string
	 * @throws IOException 
	 */
	public static String readFile(File file) throws IOException {
		BufferedReader reader = null;
		StringBuilder builder = new StringBuilder();
		
		try	{
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while ((line = reader.readLine()) != null)
				builder.append(line + "\n");
		} catch (IOException e) {
			throw e;
		} finally {
			if(reader != null)
				reader.close();
		}

		return builder.toString();
	}
	
	/**
	 * 
	 * @param string
	 * @param object of the JAR that contains the resource (e.g. 'this')
	 * @return result as String
	 * @throws IOException 
	 */
	public static String getResource(String string, Object object) throws IOException {
			
	    BufferedReader br = new BufferedReader(new InputStreamReader(getResourceAsStream(string, object)));
		    
	    String res;
	    StringBuilder result = new StringBuilder("");
	    try {
			while ((res = br.readLine()) != null) {    	
			   	result.append(res);
			   	result.append("\n");
			}
	    } catch(IOException e){
	    	throw e;
	    } finally {
	    	br.close();
	    }
		return result.toString();
		
		
	}
	

	/**
	 * @param string
	 * @param object
	 * @return InputStream instance
	 */
	private static InputStream getResourceAsStream(String string, Object object) {

		return object.getClass().getResourceAsStream(string);
	}
	
	/**
	 * writes the file within the set path or creates it if relative in the base directory
	 * @param content to write
	 * @param path1 as the new file path to create the file
	 * @throws IOException 
	 */
	public void writeFile(String content, String path1) throws IOException {

		Writer out = null;
		
		String path = path1;
		
    	if(!isAbsolutePath(path)){
    		
    		// get the absolute path of the file
    		path = this.baseDirectory + path;
    	}

	    try {
	    	out = new OutputStreamWriter(new FileOutputStream(path));
	    	out.write(content);
	    	out.flush();
	    } catch (IOException e) {
	    	throw e;
		} finally {
	    	if(out != null) {
				out.close();
	    	}
	    }
	}
	
	/**
	 * append the content to the file specified with path1
	 * @param content content to append to the file
	 * @param path1 the file path
	 * @throws IOException 
	 */
	public void appendToFile(String content, String path1) throws IOException{
		Writer out = null;
		
		String path = path1;
		
    	if(!isAbsolutePath(path)){
    		// get the absolute path of the file
    		path = this.baseDirectory + path;
    	}

	    try {
	    	
	    	out = new OutputStreamWriter(new FileOutputStream(path, true));
	    	
	    	out.write(content);
	    	
	    	out.flush();
	    } catch (IOException e) {
	    	
	    	throw e;
		} finally {
	    	if(out != null) {
				out.close();
			} 
	    }
	}
}