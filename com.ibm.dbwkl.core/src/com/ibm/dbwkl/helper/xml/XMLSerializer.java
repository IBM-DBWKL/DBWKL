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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 * This class offers Marshalling and Unmarshalling services to the framework, 
 * using the JAXB technologies provided by java.
 * 
 *
 */
public class XMLSerializer {
	
	/**
	 * Writes to an XML file the task after execution completion in a replay mode
	 * @param <T> The Object Type to Marshal
	 * @param object the Object to Marshal
	 * @param schemaPath path for the schema
	 * @param contextPath The context Path for this Marshaler (Normally obtained through ObjectFactory.class.getPackage().getName())
	 * @param outputPath the path to write the marshaled file to
	 * @param cl The ClassLoader for this object (Normally obtained through ObjectFactory.class.getClassLoader())
	 * @param validateXML a boolean that specifies whether to check the object structure against the schema before serializing or not.
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 * @throws SAXException 
	 */
	public static <T> void marshallXMLFile(T object,String schemaPath, String contextPath, String outputPath, ClassLoader cl, boolean validateXML) throws FileNotFoundException, JAXBException, SAXException {
		Marshaller marshaller;
			//create the JAXBContext
		JAXBContext jc = JAXBContext.newInstance(contextPath, cl);
			//create the Marshaler
		marshaller = jc.createMarshaller();
			//validate only under request
		if(validateXML){
			SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			marshaller.setSchema(sf.newSchema(new File(schemaPath)));
		}
			//make the output file pretty
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
			//write the file
		marshaller.marshal(object, new FileOutputStream(outputPath));
		
	}
	
	/**
	 * Writes to an XML file the task after execution completion in a replay mode
	 * @param <T> The Object Type to Marshal
	 * @param object the Object to Marshal
	 * @param contextPath The context Path for this Marshaler (Normally obtained through ObjectFactory.class.getPackage().getName())
	 * @param cl The ClassLoader for this object (Normally obtained through ObjectFactory.class.getClassLoader())
	 * @param outputStream as the OutputStream to write to
	 * @throws JAXBException 
	 */
	public static <T> void marshallXMLToOutputStream(T object, OutputStream outputStream) throws JAXBException {
		Marshaller marshaller;
			//create the JAXBContext
		JAXBContext jc = JAXBContext.newInstance(object.getClass());
			//create the Marshaler
		marshaller = jc.createMarshaller();
			//make the output file pretty
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
			//write the file
		marshaller.marshal(object, outputStream);
	}
	
	/**
	 * This marshals an object to an XML String without using a OutputStream
	 * @param <T>
	 * @param object as the object to XMLize
	 * @return XMLized object as XML String
	 * @throws JAXBException 
	 */
	public static <T> String marshallXMLToString(T object) throws JAXBException {
		Marshaller marshaller;
			//create the JAXBContext
		JAXBContext jc = JAXBContext.newInstance(object.getClass());
			//create the Marshaler
		marshaller = jc.createMarshaller();
			//make the output file pretty
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
			//write the file
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		marshaller.marshal(object,byteArrayOutputStream);
			
		String xmlstring = new String(byteArrayOutputStream.toByteArray());
		try {
			byteArrayOutputStream.close();
		} catch (IOException e) {
			//ignore
		}
			
		return xmlstring;
		
	}
	
	/**
	 * this method creates a XSD Schema of XMLRooted class
	 * @param <T> as the place holder for any class
	 * @param classe as any class to create the schema
	 * @return XSD Schema for the objects class as String
	 * @throws IOException 
	 * @throws JAXBException 
	 */
	public static <T> String createXMLSchema(Class<T> classe) throws IOException, JAXBException {
		String xsd = null;
			//create the JAXBContext
		JAXBContext jc = JAXBContext.newInstance(classe);
		MySchemaOutputResolver mySchemaOutputResolver = new MySchemaOutputResolver();
		jc.generateSchema(mySchemaOutputResolver);
		xsd = mySchemaOutputResolver.getSchemaString();
		
		return xsd;
	}
	
	/**
	 * Reads an XML Document thats valid against the StoredProcedures schema and creates objects that
	 * represent its content, using the <code>com.ibm.dbwkl.storedproceduresmodule.datatypes</code> package objects. 
	 * @param <T> The type of object to undo marshal to
	 * @param classe as the class that should contain the deserialized objects
	 * @param inputStream as the InputStream that should by read
	 * @return <T> The T object that will be returned by undo marshal
	 * @throws JAXBException 
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T extends Object> T unmarshallXMLFromInputStream(Class classe, InputStream inputStream) throws JAXBException {
		T tObject=null;
		//create the JAXBContext
		JAXBContext jc = JAXBContext.newInstance(classe);
		//Create the Unmarshaller object 
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		//retrieve the object from the XML file
		tObject = ((JAXBElement<T>)unmarshaller.unmarshal(inputStream)).getValue();
		
		return tObject;
	}
	/**
	 * Reads an XML Document thats valid against the StoredProcedures schema and creates objects that
	 * represent its content, using the <code>com.ibm.dbwkl.storedproceduresmodule.datatypes</code> package objects. 
	 * @param <T> The type of object to unmarshall to
	 * @param schemaPath path for the schema
	 * @param contextPath The context Path for this Marshaler (Normally obtained through ObjectFactory.class.getPackage().getName())
	 * @param file the path to the XML file to unmarshall
	 * @param cl The ClassLoader for this object (Normally obtained through ObjectFactory.class.getClassLoader())
	 * @return a Task object, representing an "instance" of the XML file´s objects
	 * @throws JAXBException 
	 * @throws SAXException
	 * @throws IllegalArgumentException 
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T unmarshallXMLFile(String schemaPath, String contextPath, String file, ClassLoader cl ) throws JAXBException, SAXException, IllegalArgumentException {
		File serializedObject = new File(file);
		if(!serializedObject.exists() || !serializedObject.toString().endsWith(".xml"))
			throw new IllegalArgumentException("Error while unmarshalling: file "+serializedObject.toString()+" does not exist, or is not an xml file.");
		
			//create the JAXBContext
		JAXBContext jc = JAXBContext.newInstance(contextPath, cl);
			//Create the Unmarshaller object 
		Unmarshaller unmarshaller = jc.createUnmarshaller();
			//Create the schema
		SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			//and pass it to the unmarshaller
		unmarshaller.setSchema(sf.newSchema(new File(schemaPath)));
			//retrieve the object from the XML file
		T t = (((JAXBElement<T>)unmarshaller.unmarshal(serializedObject)).getValue());
	
		return t;
	}
	
	/**
	 * This method accepts XML and XSD as strings and unmarshalls them into objects of the
	 * given XMLRooted class
	 * @param <T> as the place holder
	 * @param xml as string with the JAXB generated XML
	 * @param xsd as string with the JAXB generated XSD
	 * @param classe as the class for the object
	 * @return t object as the deserialized object
	 * @throws JAXBException 
	 * @throws SAXException 
	 */
	public static <T extends Object> T unmarshallXMLFile(String xml, String xsd, Class<T> classe) throws JAXBException, SAXException {
		
		//create the JAXBContext
		JAXBContext jc = JAXBContext.newInstance(classe);
		//Create the Unmarshaller object 
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		//Create the schema
		SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		//and pass it to the unmarshaller
		StreamSource streamSource = new StreamSource(new StringReader(xsd));
		unmarshaller.setSchema(sf.newSchema(streamSource));
		//retrieve the object from the XML file
		T t = (unmarshaller.unmarshal(new StreamSource(new StringReader(xml)),classe)).getValue();
		return t;
	}
	
	/**
	 * This inner class is a SchemaOutputResolver to work internal and creating a string 
	 * output of the XSD Schema, because we do not need a file based output
	 *
	 */
	public static class MySchemaOutputResolver extends SchemaOutputResolver {
		 /**
		 * byteArrayOutputStream for internal string building of the schema
		 */
		ByteArrayOutputStream byteArrayOutputStream;
		 
	    @Override
		public Result createOutput(String namespaceURI, String filename) throws IOException {
	        StreamResult result = new StreamResult();
	        this.byteArrayOutputStream = new ByteArrayOutputStream();
	        result.setOutputStream(this.byteArrayOutputStream);
	        result.setSystemId("CreatedXSD");
	        return result;
	    }
	    
	    /**
	     * @return XSD Schema String
	     */
	    public String getSchemaString() {
	    	return new String(this.byteArrayOutputStream.toByteArray());
	    }
	    
	    /**
	     * closes the used byteArrayOutputStream
	     */
	    public void closeStream() {
	    	try {
				this.byteArrayOutputStream.close();
			} catch (IOException e) {
				//nothing to do
			}
	    }
	}
}
