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


/**
 *
 */
public class StringUtility {

	/**
	 * Tests whether two string are equal with ignoring the case. The difference to
	 * String.equalsIgnoreCase is that in case s1 or s2 is null then this method returns 
	 * false.
	 * 
	 * @param s1 first string to compare
	 * @param s2 second string to comapre
	 * @return true in case they are equal with ignore case
	 */
	public static boolean equalsIgnoreCase(String s1, String s2) {
		
		if (s1 == null || s2 == null) {
			return false;
		}
		
		return s1.equalsIgnoreCase(s2);
	}
	
	/**
	 * Tests if the string sequence starts with the specified prefix, case insensitive.
	 * @param seq 
	 * @param prefix
	 * @return true if the character sequence represented by 'prefix' is a prefix of the character sequence represented by 'seq'; false otherwise. 
	 */
	public static boolean startsWithIgnoreCase(String seq, String prefix){

		return seq.toLowerCase().startsWith(prefix.toLowerCase());
	}
	
	/**
	 * Tests if this string sequence ends with the specified suffix.  
	 * @param seq 
	 * @param suffix
	 * @return true if the character sequence represented by 'suffix' is a suffix of the character sequence represented by 'seq'; false otherwise. 
	 */
	public static boolean endsWithIgnoreCase(String seq, String suffix) {
	
		return seq.toLowerCase().endsWith(suffix.toLowerCase());
	}
	
	/**
	 * Returns true if and only if string 'str1' contains the specified String 'str2'
	 * @param str1 The string to search with
	 * @param str2 The string to search for
	 * @return true if this string contains s, case insensitive; false otherwise. 
	 */
	public static boolean containsIgnoreCase(String str1, String str2) {

		return str1.toLowerCase().contains(str2.toLowerCase());
	}
	
	
	/**
	 * Returns true if and only if the String array contains the given string, case insensitive.
	 * @param array The String array to search with
	 * @param str The String to search for
	 * @return true if the String array contains the given String, case insensitive; false otherwise.
	 */
	public static boolean arrayContainsIgnoreCase(String[] array, String str){
		if(array.length == 0){
			return false;
		}
		for(int i = 0; i < array.length; i++){
			if(array[i].equalsIgnoreCase(str))
				return true;
		}
		
		return false;
		
	}
	
	/**
	 * Returns true if and only if the String array contains only the given strings, case insensitive.
	 * @param array The String array to search with
	 * @param strs The Strings to search for
	 * @return true if the String array contains only the given Strings, case insensitive; false otherwise.
	 */
	public static boolean arrayContainsOnlyIgnoreCase(String[] array, String... strs){
		if(array.length == 0){
			return false;
		}
		
		for(int i = 0; i < array.length; i++){
			if(arrayContainsIgnoreCase(strs, array[i]))
				continue;
			else 
				return false;	
		}
		return true;
	}

	/**
	 * <p>This method will cut a specified string to a specified length
	 * or fill it with " " until it reaches this length. This makes it
	 * possible to write "nice" columns to a file.</p>
	 * 
	 * @param string1 to cut
	 * @param maxLength1 of the string
	 * @return a cut string
	 */
	public static String cutString(String string1, int maxLength1) {
		
		return cutString(string1, maxLength1, true);
	}
	
	/**
	 * <p>This method will cut a specified string to a specified length
	 * or fill it with " " until it reaches this length in case extend is set to true. 
	 * This makes it possible to write "nice" columns to a file.</p>
	 * 
	 * @param string to cut
	 * @param maxLength of the string
	 * @param extend whether to extend spaces in order to always have a resulting string of maxLength characters
	 * @return a cut string
	 */
	public static String cutString(String string, int maxLength, boolean extend) {
		
		int maxLength1 = maxLength + 1;
		String string1 = string;
		
		if(string1 == null)
			string1 = " ";
		
		if(string1.length() >= maxLength1)
			return string1.substring(0, maxLength1-1);
		
		StringBuilder b = new StringBuilder(string1);
		
		if (extend)
			for (int i = string1.length(); i < maxLength1-1; i++)
				b.append(" ");
		
		return b.toString();
		
	}

	/**
	 * @param className to simplify
	 * @return the class name without the package names
	 */
	public static String getSimpleClassName(String className) {
		
		return className.substring(className.lastIndexOf(".") + 1, className.length());
	}

	/**
	 * Returns an empty string if the reference of the string is null
	 * 
	 * @param text the text to check
	 * @return "" in case it is null
	 */
	public static String emptyIfNull(String text) {
		return text == null ? "" : text;
	}

	/**
	 * Returns a replacement string if the reference of the string is null
	 * 
	 * @param text the text to check
	 * @param replacement
	 * @return "" in case it is null
	 */
	public static String replaceIfNull(String text, String replacement) {
		return text == null ? replacement : text;
	}
}
