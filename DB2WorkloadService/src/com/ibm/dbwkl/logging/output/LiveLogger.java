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
package com.ibm.dbwkl.logging.output;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBException;

import com.ibm.dbwkl.helper.xml.XMLSerializer;
import com.ibm.dbwkl.logging.ILoggerService;
import com.ibm.dbwkl.logging.LogLevel;
import com.ibm.dbwkl.logging.Logger;
import com.ibm.dbwkl.logging.LoggerEntry;


/**
 * Singleton threaded LiveLogger which starts a socket server connections and sends
 * live logging data to the registered clients
 *
 */
public class LiveLogger implements ILoggerService {
    
    /**
     * HashMap with all registered LiveLogConsumers
     */
    private static ConcurrentHashMap<InnerLiveLogConsumer, Socket> registeredLiveLogConsumers = null;
    
    /**
     * singleton LiveLogger instance to ensure
     */
    private static LiveLogger liveLogger = null;
    
    /**
     * singleton ghost thread liveLogServer instance
     */
    private static InnerLiveLogServer liveLogServer = null;
    
    /**
     * sets the new Live LogMessage that should be noted by the LiveLogConsumers
     */
    private static String newLogMessage = null;
    
    /**
     * Host as host name or address that will be recognized by the LiveLogServer
     */
    private static String allowedHost = null;

    /**
     * default private constructor for the singleton LiveLogger instance which initiates
     * the 
     */
    private LiveLogger() {
        registeredLiveLogConsumers = new ConcurrentHashMap<InnerLiveLogConsumer, Socket>();
    }

    /**
     * Adds a new LiveLogConsumer with a Client Socket and initiate the live log processing
     * with this new LiveLogConsumer
     * @param socket
     */
    public void addLiveLogConsumer(Socket socket) {
        InnerLiveLogConsumer liveLogConsumer = new InnerLiveLogConsumer(socket);
        
        //liveLogConsumer.setDaemon(true);
        
        registeredLiveLogConsumers.put(liveLogConsumer, socket);
        
        liveLogConsumer.setName("LiveLogConsumer" + registeredLiveLogConsumers.size());
        liveLogConsumer.start();
    }

    /**
     * This method will notify all live log consumers to send the new log message
     * @param logMessage
     */
    private void sendLogMessageToLiveLogConsumers(String logMessage) {
        for (InnerLiveLogConsumer liveLogConsumer : registeredLiveLogConsumers.keySet()) {
                synchronized (liveLogConsumer) {
                    liveLogConsumer.notify();
                }
        }
    }
    
    /**
     * Removes a registered liveLogConsumer after terminating the thread
     * @param liveLogConsumer
     */
    public static void removeLiveLogConsumer(InnerLiveLogConsumer liveLogConsumer) {
    	registeredLiveLogConsumers.remove(liveLogConsumer);
    }

    /**
     * Used by the live log consumers to get the new log message
     * @return new LogMessage
     */
    static String getNewLogMessage() {
        return newLogMessage;
    }

    /**
     * @param logMessage
     */
    private static void setNewLogMessage(String logMessage) {
        newLogMessage = logMessage;
    }

    /**
     * This class is used to handle the socket server
     * it will wait once initiated and accept a connection within the time out
     * this server will be terminated after a interrupt by LiveLogger happens
     *
     */
    class InnerLiveLogServer extends Thread {
    	/**
    	 * ServerSocket instance to control the socket server
    	 */
    	private ServerSocket server;
    	
    	/**
    	 * default LiveLogServer constructor
    	 * @param threadGroup for this daemon threaded server
    	 */
    	public InnerLiveLogServer(ThreadGroup threadGroup) {
    		super(threadGroup, "LiveLogServer" + new Date().getTime());
    	}
    	
    	@Override
		public void run() {
			try {
				this.server = new ServerSocket(2777);
				this.server.setSoTimeout(30000);
				Logger.log("Server opened", LogLevel.Debug);
				
				while(!interrupted()) {
					Logger.log("Server will now accept a connection", LogLevel.Debug);
					Socket client = null;
					client = this.server.accept();
					client.shutdownInput();
					client.setSoTimeout(60000);
					
					if(client.getInetAddress().equals(InetAddress.getByName(LiveLogger.getAllowedHost()))) {
						LiveLogger.getInstance().addLiveLogConsumer(client);
						Logger.log("new LiveLogConsumer added", LogLevel.Debug);
					}
					else {
						throw new IOException("Invalid unregistered Host detected");
					}
				}
				
			} catch (IOException e) {
				interrupt();
				Logger.logException(e);
			} finally {
				try {
					this.server.close();
					Logger.log("Server terminated: " + this.server.isClosed(), LogLevel.Debug);
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
    	}
    }
    
    /**
     * This class represents a live log consumer that was accepted by the live log server
     * it will run as long as messages are incoming within the connection
     * or a interrupt occurs by the LiveLogger
     *
     */
    class InnerLiveLogConsumer extends Thread {

        /**
         * client socket instance as the live log consumer that wants the live logged events
         */
        private Socket client;
        
        /**
         * DataOutputStream that will be used to send log messages to client
         */
        private DataOutputStream dataOutputStream = null;

		/**
		 * switch to handle a separate first Message like schema of the outgoing objects
		 */
		private boolean firstMessage = true;
        
        /**
         * default constructor with private declaration because we have to set the client
         */
        @SuppressWarnings("unused")
		private InnerLiveLogConsumer() {
        	//NOT ALLOWED
        }

        /**
         * LiveLogConsumer needs a socket to be set within the constructor
         * @param socket
         */
        public InnerLiveLogConsumer(Socket socket) {
        	super("LiveLogConsumer"+new Date().getTime());
            this.client = socket;
        }

        @Override
        public void run() {
            try {
                Logger.log("Running " + this.getName() + " / " + this.client.getPort(), LogLevel.Debug);
                this.dataOutputStream = new DataOutputStream(this.client.getOutputStream());
                while (this.client.isConnected() && !isInterrupted()) {
                    synchronized (this) {
                        wait();
                        //sendLogMessageToClient(this.client.getPort() + ": " + LiveLogger.getNewLogMessage());
                        LoggerEntry logEntry = Logger.getLastLogEntry();
                        if(logEntry!=null)
                        	sendLogEntryToClient(logEntry);
                    }
                }
                
            } catch (IOException e) {
            	Logger.logException(e);
            } catch (InterruptedException ex) {
                interrupt();
            } catch (JAXBException e) {
            	Logger.logException(e);
			}
            finally {
            	terminateClient();
            }
        }

		/**
		 * Sends the log message directly to the live log consumer via data output stream
		 * @param logmessage as the message to send
		 * @throws IOException to cause an interrupt within the running process
		 */
		@SuppressWarnings("unused")
		private void sendLogMessageToClient(String logmessage) throws IOException {
                this.dataOutputStream.writeUTF(logmessage);
        }
		
		/**
		 * @param logEntry
		 * @throws IOException
		 * @throws JAXBException 
		 */
		private void sendLogEntryToClient(LoggerEntry logEntry) throws IOException, JAXBException {
			//XMLSerializer.marshallXMLToOutputStream(logEntry, this.nonClosureFilterOutputStream);
			if(this.firstMessage) {
				this.dataOutputStream.writeUTF(XMLSerializer.createXMLSchema(logEntry.getClass()));
				this.dataOutputStream.writeUTF(XMLSerializer.marshallXMLToString(logEntry));
				this.firstMessage=false;
			}
			else {
				this.dataOutputStream.writeUTF(XMLSerializer.marshallXMLToString(logEntry));
			}
		}
		
        /**
         * this method will be called after a error occurs within the run process
         * and clean up by closing the open output stream and the connection
         */
        private void terminateClient() {
            try {
                if(this.dataOutputStream!=null)this.dataOutputStream.close();
                if(this.client.isConnected()) {
	                this.client.shutdownOutput();
	            	this.client.close();
                }
            	LiveLogger.removeLiveLogConsumer(this);
                Logger.log("Client at Port " + this.client.getPort() + " terminated!", LogLevel.Debug);
            } catch (IOException ex) {
                Logger.logException(ex);
            }		
		}
    }

    /**
     * this method returns a singleton liveLogger instance
     * @return a singleton liveLogger instance
     */
    public static LiveLogger getInstance() {
        if (liveLogger == null) {
            liveLogger = new LiveLogger();
        }
        return liveLogger;
    }

	/**
	 * opens the ghost thread liveLogServer
	 * once activated it will wait for incoming live log requests
	 * and accept a socket connection
	 * this ghost thread will only be terminated after a remove of the live logger
	 * or the end of the WorkloadService
	 * @param host as the host that will connect to this server
	 */
	public void openServer(String host) {
		allowedHost = host;
		
		if(liveLogServer!=null) {
			synchronized(liveLogServer) {
				liveLogServer.interrupt();
			}
		}
		
		liveLogServer = new InnerLiveLogServer(Thread.currentThread().getThreadGroup());
		//liveLogServer.setDaemon(true);
		liveLogServer.start();
	}
	
	/**
	 * this method terminates the live log server if the server is alive
	 */
	public void terminateServer() {
		if(liveLogServer!=null) {
			synchronized(liveLogServer) {
				if(liveLogServer.isAlive())
					liveLogServer.interrupt();
			}
			liveLogServer = null;
		}
	}
	
	/**
	 * this method will check all registered live log consumer
	 * and interrupt the live log consumer process if a live log consumer is still alive
	 * and remove it from the registeredliveLogConsumers hash map
	 */
	public void terminateLiveLogConsumer() {
		for(InnerLiveLogConsumer liveLogConsumer:registeredLiveLogConsumers.keySet()) {
			if(liveLogConsumer.isAlive())
				liveLogConsumer.interrupt();
				registeredLiveLogConsumers.remove(liveLogConsumer);
		}
	}

	/**
	 * @return allowedHost as the for the live logging process registered host to get accepted by the server
	 */
	public static String getAllowedHost() {
		return allowedHost;
	}

	/**
	 * @param allowedHost
	 */
	public static void setAllowedHost(String allowedHost) {
		LiveLogger.allowedHost = allowedHost;
	}

	@Override
	public void log(LoggerEntry entry) {
        setNewLogMessage(entry.getFormatedTime() +" - " + entry.getMessage());
        sendLogMessageToLiveLogConsumers(entry.getFormatedTime() +" - " + entry.getMessage());
	}

	@Override
	public String getName() {
		return "LiveLogger";
	}

	@Override
	public void term() {
		if(liveLogServer!=null) {
			terminateLiveLogConsumer();
			terminateServer();
		}
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