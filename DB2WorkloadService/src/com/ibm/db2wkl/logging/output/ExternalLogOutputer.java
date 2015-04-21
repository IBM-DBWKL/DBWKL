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
package com.ibm.db2wkl.logging.output;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.JAXBException;

import com.ibm.db2wkl.helper.xml.XMLSerializer;
import com.ibm.db2wkl.logging.ILoggerService;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.logging.Logger;
import com.ibm.db2wkl.logging.LoggerEntry;

/**
 * The ExternalLogOutputer works like the LiveLogger: It will serialize request specific
 * log entries and sends it to a listener service that will unmarshall this services log entries.
 * This Logger is usually used for rerouted requests
 */
public class ExternalLogOutputer implements ILoggerService {

    /**
     * Threaded Client that sends the log entries to the origin Workload Service
     */
    private Thread clientThread = null;
    
    /**
     * synchronized lock object for notifying the threaded client
     */
    final Object synchroLock = new Object();
    
    /**
     * boolean true if this logger is finished
     */
    boolean finished = false;
    
    /**
     * ConcurrentLinkedQueue with LoggerEntries to send to a listener service
     */
    ConcurrentLinkedQueue<LoggerEntry> logEntries = new ConcurrentLinkedQueue<LoggerEntry>();
	
	/**
	 * default constructor as private because the host must be set
	 */
	@SuppressWarnings("unused")
	private ExternalLogOutputer() {
		//default constructor
	}
	
	/**
	 * @param hostToLogTo
	 * @param rid 
	 */
	public ExternalLogOutputer(String hostToLogTo, Long rid) {
		InnerClient innerClient = new InnerClient(this, hostToLogTo);
		this.clientThread = new Thread(Thread.currentThread().getThreadGroup(), innerClient, "ExternalLogOutputer" + rid);
		this.clientThread.start();
	}

	@Override
	public String getName() { 
		return "ExternalLogOutputer";
	}

	@Override
	public void log(LoggerEntry entry) {
		if(this.clientThread!=null) {
			synchronized(this.synchroLock) {
				this.logEntries.add(entry);
				this.synchroLock.notify();
			}
		}
	}
	
	/**
	 * Terminates the client thread that sends the messages by interrupting it
	 */
	private void terminateClient() {
		if(this.clientThread != null) {
			synchronized(this.clientThread) {
				this.clientThread.interrupt();
			}
		}
		Logger.log("ExternalLogOutputer terminated", LogLevel.Debug);
	}
	
	/**
	 * Client Thread that sends the request specific log entries to the server(= origin service that
	 * initiated this rerouted Request).
	 * The log entries will be serialized as XML String and remote logged via DataOutputStream 
	 */
	class InnerClient implements Runnable {
		/**
	     * Host to send the data to
	     */
	    private String host = null;
	  
		/**
	     * DataOutputStream for sending the messages as UTF String
	     */
	    private DataOutputStream dataOutputStream;

		/**
	     * First message is usually the schema for the serialized objects
	     */
		private boolean firstMessage = true;
		
		/**
	     * ExternalLogOutputer instance as the logger that controls this thread
	     */
		private ExternalLogOutputer externalLogOutputer = null;
		
		/**
		 * synchronized lock object for waiting for the threaded client
		 */
		final Object lock;
		
		/**
		 * @param threadGroup as the currentThreads ThreadGroup
		 * @param eLogOutputer as the ExternalLogOutputer
		 * @param host as the origin services host name that initiated the rerouted request
		 * @param id as the rerouted requests ID
		 */
		public InnerClient(ExternalLogOutputer eLogOutputer, String host) {
			this.host = host;
			this.lock = eLogOutputer.synchroLock;
			this.externalLogOutputer = eLogOutputer;
		}

		@Override
		public void run() {
			Socket client = null;
			
			try {
				client = connectToLogServer();
				Logger.log("ExternalLogOutputer started!", LogLevel.Info);
				this.dataOutputStream = new DataOutputStream(client.getOutputStream());
				while(client.isConnected() && !this.externalLogOutputer.finished && !Thread.currentThread().isInterrupted()) {
					synchronized(this.lock) {
						this.lock.wait();
						for(@SuppressWarnings("unused") LoggerEntry entry:this.externalLogOutputer.logEntries)
							sendLogEntryToLogServer(this.externalLogOutputer.logEntries.poll());
					}
				}
			} catch(IOException e) {
				Logger.logException(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					termClient(client);
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
			return;
		}

		/**
		 * @return client Socket if connecting to the server was successful
		 * @throws IOException
		 */
		private Socket connectToLogServer() throws IOException {
			Socket client = new Socket(this.host, 2778);
			client.shutdownInput();
			client.setSoTimeout(60000);
			return client;
		}
		
		/**
		 * @param logEntry as the loggerEntry object to serialize and send over the
		 * DataOutputStream to the rerouted request server
		 * @param client as the client Socket to send the messages to
		 * @throws IOException
		 * @throws JAXBException 
		 */
		private void sendLogEntryToLogServer(LoggerEntry logEntry) throws IOException, JAXBException {
			
			if(logEntry==null) return;
			
			String message = XMLSerializer.marshallXMLToString(logEntry);
				
			if(message==null || message.isEmpty()) throw new IOException("Error within marshalling the log entry!");
			
			if(this.firstMessage) {
				this.dataOutputStream.writeUTF(XMLSerializer.createXMLSchema(logEntry.getClass()));
				this.dataOutputStream.writeUTF(message);
				this.firstMessage=false;
			} else {
				this.dataOutputStream.writeUTF(message);
			}
		}
		
		/**
		 * Terminates the client socket
		 * @param client as the socket to terminate
		 * @throws IOException
		 */
		private void termClient(Socket client) throws IOException {
			this.dataOutputStream.close();
			this.dataOutputStream = null;
			client.shutdownOutput();
			client.close();
		}
	}

	@Override
	public void term() {
		terminateClient();
		this.logEntries.clear();
		this.finished = true;
	}
	
	@Override
	public void clean() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void init() {
		//		
	}
}
