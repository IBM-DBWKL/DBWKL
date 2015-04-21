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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.db2wkl.helper.CaseInsensitiveMap;
import com.ibm.db2wkl.helper.CryptionModule;
import com.ibm.db2wkl.helper.StringUtility;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.staf.STAFResult;

/**
 *
 */
public class ARequest {
	
	/**
	 * Empty string substitution. In case of remote request, the request parseResult will be passed as a string array into the new JVM,
	 * and empty string will be therefore omitted, which result to a wrong matching. This string will substitute a "" when passing as 
	 * program arguments, and will be substituted back later.
	 */
	private static final String EMPTY_STRING = "EMPTY_STRING";
	
	/**
	 * the host who initiated this request
	 */
	protected String origin_host;

	/**
	 * the RMI port of the originator
	 */
	private int origin_port;
	
	/**
	 * the host where this request is executed.
	 */
	protected String execute_host;
	
	/**
	 * ID of that request
	 */
	protected Long rid;
	
	/**
	 * complete request string
	 */
	protected String requestString;
	
	/**
	 * The action of the Request
	 */
	protected String action;
	
	/**
	 * The Type of the request
	 */
	protected RequestType type;
	
	/**
	 * Name of the Request
	 */
	protected String name;
	
	/**
	 * request parseResult
	 */
	protected CaseInsensitiveMap parseResult;
	
	/**
	 * start time of that request
	 */
	private Date endTime = null;
	
	/**
	 * start time of that request
	 */
	protected Date startTime = null;
	
	/**
	 * loglevel of the request
	 */
	protected LogLevel logLevel;
	
	/**
	 * flag to log details
	 */
	protected boolean logDetails;
	
	/**
	 * Result of this request
	 */
	protected STAFResult result;

	/**
	 * whether the request is in debugging mode
	 */
	private boolean isDebugging = false;
	
	
	/**
	 * Default Constructor
	 */
	public ARequest() {
		//
	}
	
	/**
	 * Constructor 
	 * @param requestString 
	 * @param action
	 * @param parseResult
	 * @param localhost 
	 * @param localport 
	 */
	public ARequest(String requestString, String action, CaseInsensitiveMap parseResult, String localhost, int localport) {
		this.requestString = requestString;
		this.startTime = new Date();
		this.action = action;
		this.parseResult = parseResult;
		if(this.parseResult.containsKey(Options.RID)){
			this.rid = Long.valueOf(this.parseResult.get(Options.RID));
		} else {
			this.rid = RequestIdGenerator.getNextId();
		}
		this.name = "req" + this.rid;
		if(this.parseResult.containsKey(Options.ORIGIN_HOST)){
			this.type = RequestType.INCOMING;
			this.origin_host = this.parseResult.get(Options.ORIGIN_HOST);
			this.origin_port = Integer.parseInt(this.parseResult.get(Options.ORIGIN_PORT)); 
			this.execute_host = localhost;
		} else if(this.parseResult.containsKey(Options.REDIRECT_TO)){
			this.type = RequestType.REMOTE;
			this.execute_host = this.parseResult.get(Options.REDIRECT_TO);
			this.origin_host = localhost;
			this.origin_port = localport;
		} else if(action.equalsIgnoreCase(Options.WKL_WORKLOAD) || action.equalsIgnoreCase(Options.WKL_SQL) || action.equalsIgnoreCase(Options.SP_SP)){
			this.type = RequestType.LOCAL;
			this.origin_host = localhost;
			this.origin_port = localport;
			this.execute_host = localhost;
		} else  {
			this.type = RequestType.INTERNAL;
			this.origin_host = localhost;
			this.origin_port = localport;
			this.execute_host = localhost;
		}

		if(hasRequestOption(Options.GEN_LOG_LOGLEVEL)){
			this.logLevel = LogLevel.getLevel(getRequestOption(Options.GEN_LOG_LOGLEVEL));
		} else {
			this.logLevel = LogLevel.Info;
		}
		if(hasRequestOption(Options.GEN_LOG_LOGDETAILS)){
			this.logDetails = true;
		} else {
			this.logDetails = false;
		}
	}
	
	/**
	 * @param aRequest
	 */
	public final void init(ARequest aRequest){
		this.requestString = aRequest.getRequestString();
		this.rid = aRequest.getRid();
		this.name = aRequest.getName();
		this.startTime = aRequest.getStartTime();
		this.action = aRequest.getAction();
		this.parseResult = aRequest.getParseResult();
		this.type = aRequest.getType();
		this.logDetails = aRequest._isLogDetails();
		this.logLevel = aRequest._getLogLevel();
		this.origin_host = aRequest.getOrigin_host();
		this.execute_host = aRequest.getExecute_host();
	}

	/**
	 * @param option
	 * @return whether the option exists
	 */
	public boolean hasRequestOption(String option){
		return this.parseResult.containsKey(option);
	}
	
	/**
	 * @param option
	 * @return the value of the option
	 */
	public String getRequestOption(String option){		
		String value = this.parseResult.get(option);
		if(value == null)
			return "";
		else
			return value; 
	}
	
	/**
	 * @return request parseResult
	 */
	public CaseInsensitiveMap getParseResult(){
		return this.parseResult;
	}

	/**
	 * @return the id
	 */
	public Long getRid() {
		return this.rid;
	}

	/**
	 * @return action of the Request
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * @return Type
	 */
	public RequestType getType() {
		return this.type;
	}
	
	/**
	 * @return name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return end time
	 */
	public Date getEndTime() {
		return this.endTime;
	}

	/**
	 * @param endTime
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
	/**
	 * @return the start Time
	 */
	public Date getStartTime() {
		return this.startTime;
	}
	
	/**
	 * @param type0
	 * @return whether the request is the specified type
	 */
	public boolean isType(RequestType type0){
		return this.type.equals(type0);
	}
	
	/**
	 * @return loglevel
	 */
	public LogLevel _getLogLevel(){
		return this.logLevel;
	}
	
	/**
	 * @return whether log details
	 */
	public boolean _isLogDetails(){
		return this.logDetails;
	}	
	
	/**
	 * @return the execute_host
	 */
	public String getExecute_host() {
		return this.execute_host;
	}
	
	/**
	 * @return the origin_host
	 */
	public String getOrigin_host() {
		return this.origin_host;
	}
	
	/**
	 * @return originator's RMI port
	 */
	public int getOrigin_port() {
		return this.origin_port;
	}
	
	/**
	 * @return result 
	 */
	public STAFResult getResult() {
		return this.result;
	}
	
	/**
	 * @param result
	 */
	public void setResult(STAFResult result){
		this.result  = result;
	}

	/**
	 * @return JVM arguments specified in the command line including the JAVA executable(simply "java" per default)
	 */
	public List<String> getJVMArguments() {
		ArrayList<String> jvmArgs = new ArrayList<String>();
		jvmArgs.add(0, "java");
		if(hasRequestOption(Options.JVM_OPTIONS)){
			for(String arg : getRequestOption(Options.JVM_OPTIONS).split(";")){
				if(StringUtility.startsWithIgnoreCase(arg, "jvm=")){
					//replace "java" with the specified java executable
					jvmArgs.remove(0);
					jvmArgs.add(0, arg.substring(arg.indexOf("=") + 1, arg.length()));
				} else if(arg.equalsIgnoreCase("debug")){
					jvmArgs.add("-agentlib:jdwp=transport=dt_socket,address=" + this.getOrigin_host() + ":8000,suspend=y");
					this.isDebugging = true;
				} else {
					jvmArgs.add(arg);
				}
			}
		}
		return jvmArgs;
	}


	/**
	 * @return request string
	 */
	public String getRequestString() {
		
		return this.requestString;
	}
	
	/**
	 * Takes the request string as input and encrypts or decrypts the password if any exists.
	 * Whether the password is decrypted or encrypted depends on whether it is preceded by
	 * "secured:"
	 * 
	 * @param request the request string
	 * @return the password encrypted/decrypted request string
	 * 
	 * @throws Exception
	 */
	private String findAndChangePasswordSecurityInRequestString(String request) throws Exception {
		
		// find the password
		Pattern pwdPattern = Pattern.compile("(?<=\\b" + Options.DB_PASSWORD + "\\s)(\\S+)", Pattern.CASE_INSENSITIVE);
		Matcher pwdMatcher = pwdPattern.matcher(request);
		String pwd = null;
		while (pwdMatcher.find()) {
			pwd = pwdMatcher.group();
		}
		
		// securedPwd is either the encrypted or decrypted password, depending on
		// whether it starts with the security prefix or not
		String securedPwd = null;
		if (pwd != null) {
			if (StringUtility.startsWithIgnoreCase(pwd, CryptionModule.SECURITY_PREFIX)) {
				securedPwd = pwd;
			} else {
				securedPwd = CryptionModule.SECURITY_PREFIX + CryptionModule.getInstance().getEncryptedPassword(pwd).result;
			}
		} else {
			return request;
		}
		
		// replace the password with the encrypted one
		Matcher pwdReplaceMatcher = pwdPattern.matcher(request);
		String securedRequestString = pwdReplaceMatcher.replaceAll(securedPwd);
		
		return securedRequestString;
	}
	
	/**
	 * @return jvm arguments
	 * @throws Exception encryption of the request string failed
	 */
	public List<String> getProgramArguments() throws Exception {
		
		ArrayList<String> args = new ArrayList<String>();
		args.add(this.origin_host);
		args.add(String.valueOf(this.origin_port));
		args.add(this.execute_host);
		
		// encrypt the password in the string
		String encryptedRequestString = findAndChangePasswordSecurityInRequestString(this.requestString);
		args.add("\"" + encryptedRequestString + "\"");
		
		args.add(String.valueOf(this.rid));
		args.add(this.action);
		args.add(this.type.name());
		args.add(this.name);
		args.add(this.logLevel.toString().trim());
		args.add(String.valueOf(this.logDetails));
		for(Entry<String, String> e : this.parseResult.entrySet()){
			String option = e.getKey();
			String value = e.getValue();
			
			// check for empty value
			if (value.equals("")) {
				value = EMPTY_STRING;
			}
			
			// check for password and encrypt the value in in this case
			if (option.equalsIgnoreCase(Options.DB_PASSWORD)) {
				if (!StringUtility.startsWithIgnoreCase(value, CryptionModule.SECURITY_PREFIX))
					value = CryptionModule.SECURITY_PREFIX + CryptionModule.getInstance().getEncryptedPassword(value).result;
			}
			
			args.add(option);
			args.add(value);
		}
		
		return args;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public ARequest(String[] args) throws Exception {
		int i = -1;
		this.origin_host = args[++i];
		this.origin_port = Integer.parseInt(args[++i]);
		this.execute_host = args[++i];
		
		// encrypt the password in the string
		String decryptedRequestString = findAndChangePasswordSecurityInRequestString(args[++i]);
		this.requestString = decryptedRequestString;
		
		this.rid = Long.valueOf(args[++i]);
		this.action = args[++i];
		this.type = RequestType.valueOf(args[++i]);
		this.name = args[++i];
		this.logLevel = LogLevel.getLevel(args[++i].trim());
		this.logDetails = Boolean.parseBoolean(args[++i]);
		this.parseResult = new CaseInsensitiveMap();
		for(; i < args.length - 2; ){
			String key = args[++i];
			String value = args[++i];
			
			// check for empty value
			if(value.equals(EMPTY_STRING)){
				value = "";
			}
			
			// check for password and decrypt the value in this case
			if (key.equalsIgnoreCase(Options.DB_PASSWORD)) {
				value = CryptionModule.getInstance().getDecryptedPassword(value);
			}
			
			this.parseResult.put(key, value);
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof ARequest){
			ARequest request = (ARequest)o;
			if(request.getOrigin_host().equalsIgnoreCase(this.getOrigin_host()) && 
				request.getRid().equals(this.getRid())){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		return (this.getOrigin_host() + this.getRid()).hashCode();
	}

	/**
	 * @return whether the request is in debug mode
	 */
	public boolean isDebugging() {
		return this.isDebugging;
	}
}
