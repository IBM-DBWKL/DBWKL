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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.Logger;
import com.ibm.dbwkl.request.parser.Options;
import com.ibm.dbwkl.workloadtypes.model.WorkloadClass;
import com.ibm.dbwkl.workloadtypes.viewmodel.WorkloadClassViewModel;
import com.ibm.staf.STAFMapClassDefinition;
import com.ibm.staf.STAFMarshallingContext;
import com.ibm.staf.STAFResult;

/**
 * It is use to list different lists. 
 * 
 * For example a list of all available <b>workloads</b>.
 *
 */
public class ListHandler extends InternalRequest {

	/**
	 * all available workloads as a String
	 */
	StringBuilder workloadsBuilder;
	
	/**
	 * table definition for the return of the list action
	 */
	private STAFMapClassDefinition listWorkloads;
	
	/**
	 * 
	 */
	public ListHandler() {
		// build up the returned table of workloads
		this.listWorkloads = new STAFMapClassDefinition("STAF/DB2WKL/List/Workloads");
		this.listWorkloads.addKey("long_name", 	"Long Name");
		this.listWorkloads.addKey("short_name", "Short Name");
		this.listWorkloads.addKey("name", 		"Class Name");
		this.listWorkloads.addKey("jar", 		"Workload Library");
		
	}
	
	@Override
	public STAFResult execute() {
		
		if(hasRequestOption(Options.LIST_WORKLOADS)){
			
			// get the viewmodel for the workload class model
			WorkloadClassViewModel workloadClassViewModel;
			try {
				workloadClassViewModel = new WorkloadClassViewModel();
			} catch (IOException e) {
				Logger.log("Could not load the workloads: " + e.getLocalizedMessage(), LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, e.getLocalizedMessage());
			}
			
			// create the result map and the marshaller that converts the map into a
			// STAF result object
			STAFMarshallingContext marshallingContext = new STAFMarshallingContext();
			marshallingContext.setMapClassDefinition(this.listWorkloads);
			
			// get the workloads
			List<WorkloadClass> workloadClasses = workloadClassViewModel.getWorkloadClasses();
			
			// declare the result object
			List<Map<String, String>> workloadClassesResultList = new ArrayList<Map<String, String>>();
			
			// fill the result map
			for (WorkloadClass workloadClass : workloadClasses) {
				
				@SuppressWarnings("unchecked")
				Map<String, String> stafResultTableEntry = this.listWorkloads.createInstance();
				stafResultTableEntry.put("long_name", workloadClass.getLongName());
				stafResultTableEntry.put("short_name", workloadClass.getShortName());
				stafResultTableEntry.put("name", workloadClass.getName());
				stafResultTableEntry.put("jar", workloadClass.getJarFileName());
				workloadClassesResultList.add(stafResultTableEntry);
			}
			
			marshallingContext.setRootObject(workloadClassesResultList);
			
			return new STAFResult(STAFResult.Ok, marshallingContext.marshall());
		}
		this.result = new STAFResult(STAFResult.InvalidValue, "You must specify one of the options of LIST");
		return this.result;
	}
	
	/**
	 * @return xml output
	 */
	@Override
	public Document getXML() {
		
		try {
			// create the document
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();
			
			// create the document root node
			Element root = document.createElement("db2wklresult");
			document.appendChild(root);
			
			// create the result information
			Element rc = document.createElement("rc");
			rc.setTextContent(Integer.toString(this.getResult().rc));
			root.appendChild(rc);
			
			Element object = document.createElement("resultObject");
			object.setTextContent(this.getResult().resultObj == null ? "" : this.getResult().resultObj.toString());

			Element message = null;
			
			String[] workloads = this.workloadsBuilder.toString().split("\n");
			
			for(String workload : workloads){
				workload.trim();
				message = document.createElement("workload");
				message.setTextContent(workload.toString());
				object.appendChild(message);
			}
					
			root.appendChild(object);
			
			// return the resulting document
			return document;
		} catch (ParserConfigurationException e) {
			Logger.log(e.getMessage(), LogLevel.Error);
		}
		
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
