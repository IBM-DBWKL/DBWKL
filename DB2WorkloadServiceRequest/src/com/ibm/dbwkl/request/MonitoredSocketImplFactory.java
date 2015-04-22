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
package com.ibm.dbwkl.request;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.Socket;

import com.ibm.dbwkl.request.Logger;

/**
 * This class implements createSocketImpl() function the same as it is in the Socket class, 
 * by creating a SocksSocketImpl instance. In addition, the created SocketImpl is put into a map for monitoring. 
 *
 */
public class MonitoredSocketImplFactory implements SocketImplFactory {

	/**
	 * Singleton Instance of this class
	 */
	private final static MonitoredSocketImplFactory monitoredSocketImplFactory = new MonitoredSocketImplFactory();
	
	/**
	 * The list keep tracking of all sockets created within the JVM
	 */
	private ArrayList<SocketImpl> sockets = new ArrayList<SocketImpl>();
	
	/* (non-Javadoc)
	 * @see java.net.SocketImplFactory#createSocketImpl()
	 */
	@Override
	public synchronized SocketImpl createSocketImpl() {
		//see java.net.Socket#setImpl()
		//it's the same as when no SocketImplFactory is set. 
		SocketImpl socketImpl = null;
		try {	
			Class<?> defaultSocketImpl = Class.forName("java.net.SocksSocketImpl");
			Constructor<?> constructor = defaultSocketImpl.getDeclaredConstructor();
		    constructor.setAccessible(true);
		    socketImpl = (SocketImpl)constructor.newInstance();		
		} catch (Exception e) {
			Logger.logException(e);
			throw new RuntimeException(e);
		}
		
		//this line is added to monitor the sockets' creation. 
		this.sockets.add(socketImpl);
		
		return socketImpl;
	}
	
	/**
	 * get the total number of opened sockets in the JVM.
	 * @return the total number of opened sockets in the JVM.
	 */
	public synchronized int getNumberOfOpenedSockets(){
		int numberOfOpenedSockets = 0;
		
		for(Iterator<SocketImpl> it = this.sockets.iterator(); it.hasNext(); ){
			Socket socket = getSocket(it.next());
			if(!socket.isClosed()){
				numberOfOpenedSockets++;
			} else {
				//clear out the closed sockets
				it.remove();
			}
		}
	
		return numberOfOpenedSockets;
	}

	
	/**
	 * get the Socket object from SocketImpl
	 * @param SocketImpl the SocketImpl object
	 * @return the Socket object
	 */
	private Socket getSocket(SocketImpl SocketImpl) {
        try {
            Method getSocket = SocketImpl.class.getDeclaredMethod("getSocket");
            getSocket.setAccessible(true);
            return (Socket) getSocket.invoke(SocketImpl);
        } catch (Exception e) {
        	Logger.logException(e);
            throw new RuntimeException(e);
        }
    }
	
	/**
	 * get the singleton instance of this class
	 * @return monitoredSocketImplFactory
	 */
	public static MonitoredSocketImplFactory getInstance(){
		return MonitoredSocketImplFactory.monitoredSocketImplFactory;
	}
}
