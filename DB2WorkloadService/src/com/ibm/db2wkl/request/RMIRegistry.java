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
package com.ibm.db2wkl.request;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 */
public class RMIRegistry {

	/**
	 * singleton instance
	 */
	public static RMIRegistry INSTANCE = new RMIRegistry();
	
	/**
	 * registry port
	 */
	private int port = Registry.REGISTRY_PORT;
	
	/**
	 * the actual RMI registry
	 */
	private Registry registry; 
	
	/**
	 * keep track of the exported remote objects
	 */
	private ArrayList<Remote> exportedObjects = new ArrayList<Remote>();
	
	/**
	 * @param name
	 * @param obj
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws AlreadyBoundException
	 */
	public synchronized void bind(String name, Remote obj) throws AccessException, RemoteException, AlreadyBoundException{
		
		while(this.registry == null){
			try {
				this.registry = LocateRegistry.createRegistry(this.port);
			} catch(ExportException e){
				//occurs when the port is occupied, use another port instead
				this.port = 49152 +  new Random().nextInt(65535 - 49152 + 1);  //[49152, 65535]
			}
		}
		//export the remote object and bind it with the name in the registry
		this.registry.bind(name, UnicastRemoteObject.exportObject(obj, 0));
		//and the obejct in the list to keep track
		this.exportedObjects.add(obj);
	}
	
	/**
	 * @return port
	 */
	public int getPort(){
		return this.port;
	}
	
	/**
	 * unbind all names, and unexport every object that is exported
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public synchronized void terminate() throws AccessException, RemoteException, NotBoundException{
		if(this.registry != null){
			String[] names = this.registry.list();
			for(int i = 0; i < names.length; i++){
				this.registry.unbind(names[i]);
			}
			for(Remote remote : this.exportedObjects){
				UnicastRemoteObject.unexportObject(remote, true);
			}
		}
	}
}
