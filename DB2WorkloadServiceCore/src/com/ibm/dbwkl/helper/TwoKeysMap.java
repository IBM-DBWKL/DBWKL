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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @param <K1> key 1
 * @param <K2> key 2
 * @param <V> value
 *
 */
public class TwoKeysMap<K1, K2, V> {

	/**
	 * the actual map holding the data
	 */
	private ConcurrentHashMap<K1, ConcurrentHashMap<K2, V>> map;
	
	/**
	 * default constructor
	 */
	public TwoKeysMap(){
		this.map = new ConcurrentHashMap<K1, ConcurrentHashMap<K2,V>>();
	}
	
	/**
	 * clear map
	 */
	public void clear(){
		this.map.clear();
	}
	
	/**
	 * @param key1
	 * @param key2
	 * @return value
	 */
	public V get(Object key1, Object key2){
		if(this.map.get(key1) == null){
			return null;
		} else {
			return this.map.get(key1).get(key2);
		}
	}
	
	/**
	 * @param key1
	 * @return the values that map to key1 as the first key
	 */
	public Collection<V> values(Object key1){
		return this.map.get(key1).values();
	}
	
	/**
	 * @param key1
	 * @param key2
	 * @param value
	 * @return the previous value associated with key, or null if there was no mapping for key. (A null return can also indicate that the map previously associated null with key.)
	 */
	public V put(K1 key1, K2 key2, V value){
		if(this.map.get(key1) == null){
			this.map.put(key1, new ConcurrentHashMap<K2, V>());
			this.map.get(key1).put(key2, value);
			return null;
		} else {
			return this.map.get(key1).put(key2, value);
		}
	}
	
	/**
	 * @param key1
	 * @param key2
	 * @return true if this map contains a mapping for the specified key.
	 */
	public boolean containsKey(Object key1, Object key2){
		if(!this.map.containsKey(key1) || this.map.get(key1) == null){
			return false;
		} else {
			return this.map.get(key1).containsKey(key2);
		}
	}
	
	/**
	 * @return true if this map contains no key-value mappings
	 */
	public boolean isEmpty(){
		return this.map.isEmpty();
	}
	
	/**
	 * @return the number of key-value mappings in this map
	 */
	public int size(){
		int size = 0;
		for(ConcurrentHashMap<K2, V> entry : this.map.values()){
			if(entry != null){
				size += entry.size();
			}
		}
		return size;
	}
	
	/**
	 * @param key1
	 * @param key2
	 * @return the previous value associated with key1, key2, or null if there was no mapping for key. (A null return can also indicate that the map previously associated null with key.)
	 */
	public V remove(Object key1, Object key2){
		if(!this.containsKey(key1, key2)){
			return null;
		} else {
			V value = this.map.get(key1).remove(key2);
			if(this.map.get(key1).isEmpty()){
				this.map.remove(key1);
			}
			return value;
		}
	}
	
	/**
	 * @param value
	 * @return true if this map maps one or more keys to the specified value
	 */
	public boolean containsValue(Object value){
		for(ConcurrentHashMap<K2, V> entry : this.map.values()){
			if(entry.containsValue(value)){
				return true;
			}
		}
		return false;
	}

	/**
	 * @param key1 
	 * @return the map of K2 and V that is mapped with key1
	 */
	public ConcurrentHashMap<K2, V> get(Object key1) {
		return this.map.get(key1);
	}
	
	/**
	 * @return a enumeration of key1
	 */
	public Enumeration<K1> key1s(){
		return this.map.keys();
	}

	/**
	 * @param k1 
	 * @param newMap 
	 */
	public void put(K1 k1, ConcurrentHashMap<K2, V> newMap) {
		this.map.put(k1, newMap);
	}

	/**
	 * @return key1 set
	 */
	public Set<K1> key1Set() {
		return this.map.keySet();
	}
}
