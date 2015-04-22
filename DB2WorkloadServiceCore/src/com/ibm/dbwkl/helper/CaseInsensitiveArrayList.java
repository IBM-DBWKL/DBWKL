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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
public class CaseInsensitiveArrayList extends ArrayList<String> implements Serializable{

	/**
	 * auto generated
	 */
	private static final long serialVersionUID = 6210880285262610553L;
	
	@Override
	public boolean contains(Object object){
		if(object instanceof String){
			String string = (String)object;
			for(String s : this){
				if(string.equalsIgnoreCase(s)){
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}
	
	@Override
	/**
	 * removes first instance in the list which equals to the object regardless of their case.
	 */
	public boolean remove(Object object){
		if(object instanceof String){
			String string = (String) object;
			for(Iterator<String> it = this.iterator(); it.hasNext(); ){
				if(string.equalsIgnoreCase(it.next())){
					it.remove();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}
	
	@Override
	public int indexOf(Object o){
		if(o instanceof String){
			String string = (String)o;
			for(int i = 0; i < this.size(); i++){
				if(this.get(i).equalsIgnoreCase(string)){
					return i;
				}
			}
			return -1;
		} else {
			return -1;
		}	
	}
}
