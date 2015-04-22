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
package com.ibm.dbwkl.helper.xml;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>This class provides methods to map a Java object to XML. It will map
 * all marked fields and getter-methods (with the @ToXML-annotation) to a 
 * corresponding XML tag.</p>
 * 
 * <p><b>Examples:</b></p>
 * 
 * <ul>
 * 		<li>Attribute String name = "test" will be <name>test</name></li>
 * 		<li>Method String getName(); will be <name>...</name></li>
 * </ul>
 * 
 */
public class ObjectToXMLConverter {

    /**
     * This method transforms a XML document to a string. This string will not
     * be formatted.
     * @param doc
     * @return a String from a document
     */
    public static String xmlToString(Document doc) {
        try {
        	
            Source source = new DOMSource(doc);
            
            StringWriter stringWriter = new StringWriter();
            
            Result result = new StreamResult(stringWriter);
            
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            
            transformer.transform(source, result);
            
            return stringWriter.getBuffer().toString();
        } 
        catch (TransformerConfigurationException e) {
            //
        } 
        catch (TransformerException e) {
            //
        }
        return null;
    }
    
    /**
     * @param object
     * @return a Document with value of the Object
     */
    public static Document getXML(Object object) {
    	
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	
    	// create a new document
		Document asXML = null;
		
		try {
			
			asXML = factory.newDocumentBuilder().newDocument();
		} 
		catch (ParserConfigurationException e) {
			
			return null;
		}
		
		// create a root node
		Element root = asXML.createElement(object.getClass().getSimpleName());
		asXML.appendChild(root);

		// get all methods
    	Method[] methods = object.getClass().getMethods();
    	ArrayList<Method> filteredMethods = new ArrayList<Method>();
    	
    	// get all fields
    	Field[] fields = object.getClass().getFields();
    	ArrayList<Field> filteredFields = new ArrayList<Field>();
    	
    	/**
    	 * Filter methods with @ToXML, that don't need arguments and starts with 'get'
    	 */
    	for(Method method: methods) {

    		if(method.isAnnotationPresent(ToXML.class) && method.getParameterTypes().length == 0 && method.getName().startsWith("get"))
    			filteredMethods.add(method);
    	}
    	
    	/**
    	 * Filter fields with @ToXML that have not a 
    	 */
    	for(Field field: fields) {
    		
    		boolean hasAllreadyMethod = false;

    		for(Method method: filteredMethods) {
    			
    			if(method.getName().equalsIgnoreCase("get" + field.getName()))
    				hasAllreadyMethod = true;
    		}
    		
    		if(field.isAnnotationPresent(ToXML.class) && !hasAllreadyMethod)
    			filteredFields.add(field);
    	}
    	
    	/**
    	 * Create nodes from fields
    	 */
    	for(Field field: filteredFields) {

    		try {
    			
    			Element tmp = asXML.createElement(field.getName());
				tmp.setTextContent(field.get(object).toString());
				root.appendChild(tmp);
			} 
    		catch (DOMException e) {
				//
			} 
    		catch (IllegalArgumentException e) {
				//
			} 
    		catch (IllegalAccessException e) {
				//
			}
    	}
    	
    	/**
    	 * Create nodes from methods
    	 */
    	for(Method method: filteredMethods) {
    		
    		try {
    			
    			Element tmp = asXML.createElement(method.getName().substring(3, method.getName().length()));
				tmp.setTextContent(method.invoke(object, new Object[0]).toString());
				root.appendChild(tmp);
			} 
    		catch (DOMException e) {
				//
			} 
    		catch (IllegalArgumentException e) {
				//
			} 
    		catch (IllegalAccessException e) {
				//
			} 
    		catch (InvocationTargetException e) {
				//
			}
    		catch (NullPointerException e) {
    			//
    		}
    	}
 
    	return asXML;
    }
}