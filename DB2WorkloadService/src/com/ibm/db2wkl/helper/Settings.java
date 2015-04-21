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
package com.ibm.db2wkl.helper;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.ibm.db2wkl.DB2WorkloadServiceDirectory;
import com.ibm.db2wkl.STAFHandler;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.logging.Logger;
import com.ibm.staf.STAFResult;
import com.ibm.staf.STAFUtil;

/**
 * This class loads the service definition within this service
 * The service definition file declares options like activating the coordination of a request based
 * on rules or locking this service at the coordination service to not receive other
 * coordinated requests
 */
public enum Settings {
	
	/**
	 * Singleton instance for this ServiceDefinitionLoader
	 */
	INSTANCE;
	
	/**
	 * XMLInputFactory to create the XMLEventReaders to read the XML Service Definition
	 * files and create ComparingHelper based on this rules
	 */
	private XMLInputFactory xmlInputFactory = null;
	
	/**
	 * Default path for the db2jcc libraries
	 */
	private final static String JCCLIBS = "C:/Program Files/IBM/SQLLIB/java";
	
	/**
	 * declares this service as coordinate able
	 * if this option is set to true, then the service will check its internal rules and
	 * send requests to the coordination service
	 */
	private final static boolean COORDINATEABLE = true;
	
	/**
	 * declares this service as visible to other services within the coordination service
	 * note: if this option is set to true, then other services can execute an request at this
	 * service!
	 */
	private final static boolean ACCEPTS_COORDINATED_REQUESTS = false;
	
	/**
	 * host of the coordination service that will coordinate the request to other services
	 */
	private final static String COORDINATOR = "local";
	
	/**
	 * maximum allowed connections before coordination
	 */
	private static int MAX_CONNECTIONS = 200;
	
	/**
	 * maximum allowed requests before coordination
	 */
	private static int MAX_REQUESTS = 100;
	
	/**
	 * temporary test value to test the coordination
	 */
	private static int TESTCONNECTIONS = 1;
	
	/**
	 * private default constructor for initiating this ServiceDefinitionLoader
	 */
	private Settings() {
		this.xmlInputFactory = XMLInputFactory.newInstance();
	}
	
	/**
	 * @return STAFResult.Ok if the definitions loading was successful
	 */
	public STAFResult loadServiceDefinition() {
		FileLoader fileLoader = new FileLoader(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryConfig());
		
		String xml = null;
		try {
			xml = fileLoader.readFile("DB2WKLServiceDefinition.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(xml==null) return new STAFResult(STAFResult.JavaError, "No service definition in config directory found!");
		
		return readServiceDefinition(xml);
	}
	
	/**
	 * this creates the comparing definition for a service type
	 * @param xml as XMLString with the service definition
	 * @param serviceClass as the for this definition needed class to reflect the rules to
	 * @return ComparingHelperWithXMLRules instance that creates the rules based on the XML
	 */
	@SuppressWarnings("boxing")
	private STAFResult readServiceDefinition(String xml) {
		STAFResult result = null;
		XMLEventReader serviceDefinition = null;

		try {
			serviceDefinition = this.xmlInputFactory.createXMLEventReader(new StringReader(xml));
		    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		    Schema schema = factory.newSchema(new FileLoader(DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectorySchema()).loadFile("DB2WKLServiceDefinition.xsd"));
		    schema.newValidator().validate(new StAXSource(serviceDefinition));
		    serviceDefinition = this.xmlInputFactory.createXMLEventReader(new StringReader(xml));

		    StartElement startElement = null;
			while(serviceDefinition.hasNext()) {
				XMLEvent event = serviceDefinition.nextEvent();
				if(event.isStartElement()) { 
					startElement = event.asStartElement();
					for(Method method:this.getClass().getDeclaredMethods()) {
						if(method.getName().equalsIgnoreCase("set"+startElement.getName().getLocalPart())) {
							event = serviceDefinition.nextEvent();
							String value = event.asCharacters().getData();
							Class<?> types = method.getParameterTypes()[0];
									if (types == Integer.TYPE)
										method.invoke(this, new Integer(value).intValue());
									else if(types == Boolean.TYPE)
										method.invoke(this, new Boolean(value).booleanValue());
									else if (types == Integer.class) 
										method.invoke(this, new Object[] { Integer.getInteger(value) });
									else if (types == Boolean.class) 
										method.invoke(this, new Object[] { Boolean.valueOf(value) });
									else if (types == String.class) 
										method.invoke(this, new Object[] { value });
									else if (types == Short.class) 
										method.invoke(this, new Object[] { Short.valueOf(value) });
									else if (types == Long.class) 
										method.invoke(this, new Object[] { Long.getLong(value) });
							break;
						}
					}
				}
			}
		    Logger.log("ServiceDefinition loaded: " + xml, LogLevel.Info);
			result = new STAFResult(STAFResult.Ok, "Service rules setting was successful!");
		} catch (Exception e) {
			Logger.logException(e);
			e.printStackTrace();
			result = new STAFResult(STAFResult.JavaError, "Service Definition file within config directory is not valid!");
		} finally {
			if(serviceDefinition!=null) {
				try {
					serviceDefinition.close();
				} catch (XMLStreamException e) {
					e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Tries to resolve the system var IBM/OMPE/DB2WKL/JCCLIB and returns it
	 * Otherwise the default value will be returned
	 * @return the path to the db2jcc libraries
	 */
	public static String tryJCCLibs() {
		STAFResult result = STAFUtil.resolveInitVar("{IBM/OMPE/DB2WKL/JCCLIB}", STAFHandler.instance.getSTAFHandle());
		if(result.rc==STAFResult.Ok) return result.result;
		
		return JCCLIBS;
	}

	/**
	 * Tries to resolve the system var IBM/OMPE/DB2WKL/Coordinateable and returns it
	 * If the resolving fails than the default setting will be returned
	 * @return coordinateable as if the coordination is possible
	 */
	public static boolean tryCoordinateable() {
		try {
			STAFResult result = STAFUtil.resolveInitVar("{IBM/OMPE/DB2WKL/Coordinateable}", STAFHandler.instance.getSTAFHandle());
			if(result.rc==STAFResult.Ok) return new Boolean(result.result).booleanValue();
		} catch(Exception e) {
			return COORDINATEABLE;
		} 
		
		return COORDINATEABLE;
	}

	/**
	 * Tries to resolve the system var IBM/OMPE/DB2WKL/AcceptsCoordinatedRequests and returns it
	 * Otherwise the default value will be returned
	 * @return the acceptscoordinatedrequests true if this service will accept coordinated requests
	 */
	public static boolean tryAcceptsCoordinatedRequests() {
		try {
			STAFResult result = STAFUtil.resolveInitVar("{IBM/OMPE/DB2WKL/AcceptsCoordinatedRequests}", STAFHandler.instance.getSTAFHandle());
			if(result.rc==STAFResult.Ok) return new Boolean(result.result).booleanValue();
		} catch(Exception e) {
			return ACCEPTS_COORDINATED_REQUESTS;
		}
		
		return ACCEPTS_COORDINATED_REQUESTS;
	}

	/**
	 * Tries to resolve the system var IBM/OMPE/DB2WKL/Coordinator and returns it
	 * Otherwise the default value will be returned
	 * @return the coordinator as the host that runs the coordination service
	 */
	public static String tryCoordinator() {
		STAFResult result = STAFUtil.resolveInitVar("{IBM/OMPE/DB2WKL/Coordinator}", STAFHandler.instance.getSTAFHandle());
		if(result.rc==STAFResult.Ok) return result.result;
		
		return COORDINATOR;
	}

	/**
	 * Tries to resolve the system var IBM/OMPE/DB2WKL/MaxConnections otherwise it will
	 * return the default value of maximum allowed connections
	 * @return the maxConnections
	 */
	public static int tryMaxConnections() {
		try {
			STAFResult result = STAFUtil.resolveInitVar("{IBM/OMPE/DB2WKL/MaxConnections}", STAFHandler.instance.getSTAFHandle());
			if(result.rc==STAFResult.Ok) return new Integer(result.result).intValue();
		} catch(NumberFormatException e) {
			return MAX_CONNECTIONS;
		}
		
		return MAX_CONNECTIONS;
	}

	/**
	 * Tries to resolve the system var IBM/OMPE/DB2WKL/MaxRequests otherwise it will
	 * return the default value of maximum allowed running requests
	 * @return the maxRequests
	 */
	public static int tryMaxRequests() {
		try {
			STAFResult result = STAFUtil.resolveInitVar("{IBM/OMPE/DB2WKL/MaxRequests}", STAFHandler.instance.getSTAFHandle());
			if(result.rc==STAFResult.Ok) return new Integer(result.result).intValue();
		} catch(NumberFormatException e) {
			return MAX_REQUESTS;
		}
		
		return MAX_REQUESTS;
	}
	
	/**
	 * Tries to resolve the system var IBM/OMPE/DB2WKL/MaxConnections otherwise it will
	 * return the default value of maximum allowed connections
	 * @return the maxConnections
	 */
	public static int tryTestConnections() {
		try {		
			STAFResult result = STAFUtil.resolveInitVar("{IBM/OMPE/DB2WKL/TestConnections}", STAFHandler.instance.getSTAFHandle());
			if(result.rc==STAFResult.Ok) return new Integer(result.result).intValue();
		} catch(NumberFormatException e) {
			return TESTCONNECTIONS;
		}
		
		return TESTCONNECTIONS;
	}
}
