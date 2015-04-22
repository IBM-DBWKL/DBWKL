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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * Extends from HashMap, with string as key and value. Any behavior of the map will be case insensitive.
 * 
 * The lookup is done by one-by-one scanning, hence the performance will be reduced.
 * 
 *
 */
public class CaseInsensitiveMap extends HashMap<String, String> {

	/**
	 * auto generated
	 */
	private static final long serialVersionUID = -8072079427546054108L;
	
	/**
	 * Constructs an empty map with the default initial capacity (16) and the default load factor (0.75).
	 */
	public CaseInsensitiveMap(){
		super();
	}
	
	/**
	 * Constructs an empty map with the specified initial capacity and the default load factor (0.75).
	 * @param initialCapacity
	 */
	public CaseInsensitiveMap(int initialCapacity){
		super(initialCapacity);
	}
	
	/**
	 * Constructs an empty map with the specified initial capacity and load factor.
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public CaseInsensitiveMap(int initialCapacity, float loadFactor){
		super(initialCapacity, loadFactor);
	}
	
	/**
	 * Constructs a new map with the same mappings as the specified Map.
	 * @param m
	 */
	public CaseInsensitiveMap(Map<String, String> m){
		super(m);
	}
	
	@Override
	public boolean containsKey(Object key){
		synchronized (this) {
			if(key instanceof String){
				for(String s: this.keySet()){
					if(s.equalsIgnoreCase((String)key)){
						return true;
					}
				}
				return false;
				
			} else {
				return false;
			}
		}
	}
	
	@Override
	public boolean containsValue(Object value) {
		synchronized (this) {
			if(value instanceof String){
				for(String v : this.values()){
					if(v.equalsIgnoreCase((String)value)){
						return true;
					}
				}
				return false;
			}
			return false;
		}
	}
	
	@Override
	public String get(Object key){
		synchronized (this) {
			if(key instanceof String){
				for(String s: this.keySet()){
					if(s.equalsIgnoreCase((String)key)){
						return super.get(s);
					}
				}
				return null;
				
			} else {
				return null;
			}
		}
	}
	
	@Override
	public String remove(Object key){
		synchronized (this) {
			if(key instanceof String){
				boolean exist = false;
				String oldKey = null;
				for(String s: this.keySet()){
					if(s.equalsIgnoreCase((String)key)){
						exist = true;
						oldKey = s;
						break;
					}
					exist = false;
				}
				if(exist){
					return super.remove(oldKey);
				} else{
					return null;
				}
				
			} else {
				return null;
			}
		}
	}
	
	@Override
	public String put(String key, String value){
		synchronized (this) {
			boolean exist = false;
			String oldKey = null;
			for(String s: this.keySet()){
				if(s.equalsIgnoreCase(key)){
					exist = true;
					oldKey = s;
					break;
				}
				exist = false;
			}
			if(exist){
				return super.put(oldKey, value);
			} else {
				return super.put(key, value);
			}

		}
	}
	
	@Override
	public void putAll(Map<? extends String,? extends String> m){
		synchronized (this) {
			if(!m.isEmpty()){
				for(Entry<? extends String, ? extends String> e : m.entrySet()){
					this.put(e.getKey(), e.getValue());
				}
			}
		}
	}
}
