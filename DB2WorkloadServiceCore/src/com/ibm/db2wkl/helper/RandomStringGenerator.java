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

import java.util.Random;

/**
 *
 */
public class RandomStringGenerator {

	/**
	 * The alphabet which is used as to generate the string
	 */
	private static String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
	
	/**
	 * Randomizer as static to increase the quality of the randomized strings
	 */
	private static Random random = new Random(System.currentTimeMillis());
	
	/**
	 * Generates a new randomized string of the specified length
	 * 
	 * @param length length of the string to generate
	 * @return the generated string
	 */
	public static synchronized String get(int length) {
		
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			//
		}
		
		String result = "";
		for (int i = 0; i < length; i++) {
			char c = alphabet.charAt(random.nextInt(alphabet.length()));
			result += c;
		}
		
		return result;
	}
}
