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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ibm.db2wkl.DB2WorkloadService;
import com.ibm.db2wkl.DB2WorkloadServiceDirectory;
import com.ibm.db2wkl.STAFHandler;
import com.ibm.db2wkl.helper.CaseInsensitiveArrayList;
import com.ibm.db2wkl.helper.CryptionModule;
import com.ibm.db2wkl.helper.Settings;
import com.ibm.db2wkl.helper.TwoKeysMap;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.logging.Logger;
import com.ibm.db2wkl.report.Report;
import com.ibm.db2wkl.request.internal.HelpHandler;
import com.ibm.db2wkl.request.internal.InternalRequest;
import com.ibm.db2wkl.request.internal.ListHandler;
import com.ibm.db2wkl.request.internal.LoggingHandler;
import com.ibm.db2wkl.request.internal.MiscHandler;
import com.ibm.db2wkl.request.internal.ReportHandler;
import com.ibm.db2wkl.request.internal.RequestHandler;
import com.ibm.db2wkl.request.internal.SetupHandler;
import com.ibm.db2wkl.request.internal.VersionHandler;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.rmi.RequestManagerRemoteInterface;
import com.ibm.db2wkl.rmi.RequestRemoteInterface;
import com.ibm.db2wkl.rmi.RequestReporterRemoteInterface;
import com.ibm.db2wkl.workloadtypes.model.WorkloadClass;
import com.ibm.db2wkl.workloadtypes.viewmodel.WorkloadClassViewModel;
import com.ibm.staf.STAFResult;

/**
 *
 */
public class RequestManager implements RequestManagerRemoteInterface {

	/**
	 * Singleton instance of the request manager
	 */
	private static RequestManager requestManager;
	
	/**
	 * This is the list that contains a reference to all requests. 
	 * These are dummy requests which are used to start the real requests(InternalRequest, Request JVM).
	 * Since the Request instance(RequestRemoteInterface) will be destroyed along the Request JVM once the request is finished,
	 * its ARequest instance is then used to store the request's information(end time, result...) 
	 */
	private TwoKeysMap<String, Long, ARequest> requests;
	
	/**
	 * This map contains all the stub of requests, which are identified by their originator, and id.
	 */
	private TwoKeysMap<String, Long, RequestRemoteInterface> runningRequests;
	
	/**
	 * processes of all running requests on this machine(local and incoming requests)
	 */
	private TwoKeysMap<String, Long, Process> requestProcesses;

	/**
	 * Linked Queue with requests that are put on hold 
	 */
	private ConcurrentLinkedQueue<ARequest> requestsOnHold;
	
	/**
	 * available internal requests
	 */
	private HashMap<String, Class<? extends InternalRequest>> availableInternalRequests;

	/**
	 * brokendown requests
	 */
	private ArrayList<ARequest> brokedownRequests;

	/**
	 * finished requests
	 */
	private ArrayList<ARequest> finishedRequests;

	/**
	 * stopped requests
	 */
	private ArrayList<ARequest> stoppedRequests;
	
	/**
	 * internal requests
	 */
	private ArrayList<ARequest> internalRequests;	
	
	/**
	 * Singleton constructor
	 */
	private RequestManager() {
		// initialize the request lists
		this.finishedRequests = new ArrayList<ARequest>();
		this.requestsOnHold = new ConcurrentLinkedQueue<ARequest>();
		this.brokedownRequests = new ArrayList<ARequest>();
		this.stoppedRequests = new ArrayList<ARequest>();
		this.internalRequests  =  new ArrayList<ARequest>();
		this.requests = new TwoKeysMap<String, Long, ARequest>();
		this.runningRequests = new TwoKeysMap<String, Long, RequestRemoteInterface>();
		this.requestProcesses = new TwoKeysMap<String, Long, Process>();
		this.availableInternalRequests = new HashMap<String, Class<? extends InternalRequest>>();
		
		registerAllInternalRequests();
	}
	
	/**
	 * Returns or creates the singleton request manager
	 * 
	 * @return singleton instance of this manager
	 */
	public static RequestManager getInstance() {
		if (requestManager == null) {
			requestManager = new RequestManager();
		}
		return requestManager;
	}
	
	/**
	 * Starts a request
	 * 
	 * @param request request to start
	 * @return result of starting the request as thread
	 * @throws InterruptedException - The current Thread has to wait of the internal Thread
	 */
	public STAFResult startRequest(ARequest request) {
		
		this.requests.put(request.getOrigin_host(), request.getRid(), request);
		
		STAFResult result = currentlyExcecutable(request);
		if(result.rc != STAFResult.Ok){
			return result;
		}
		
		switch(request.getType()){
			case REMOTE:
				result = redirectRequest(request);
				break;
			case INCOMING:
			case LOCAL:
				result = startNewJVMForRequest(request);
				break;
			case INTERNAL:
				result = startInternalRequest(request);
				break;
			default:
				result = new STAFResult(STAFResult.JavaError, "Unknown request type");
		}
		
		return result;
	}
	
	/**
	 * @param request
	 * @return whether the request is executable now, according to the current system status (memory consumption, etc)
	 */
	private STAFResult currentlyExcecutable(ARequest request) {
		//TODO  if max memory is exceeded, or max db2 connection is exceeded
		Report systemStatus = getCurrentSystemStatus();
		if(systemStatus.getNumber_of_db2Connection() > Settings.tryMaxConnections()){
			this.requestsOnHold.add(request);
			return new STAFResult(STAFResult.MaximumSizeExceeded, "Request " + request.getRid() + " is put on hold.");
		}
		return new STAFResult(STAFResult.Ok);
	}

	/**
	 * if redirectto option is presented, the request will be redirected to the specified machine, 
	 *  however it will be managed and monitored by this machine(with Request Manager, Logger and Reporter). 
	 * @param request
	 * 
	 * @return staf result
	 */
	private STAFResult redirectRequest(ARequest request) {
		String host = request.getRequestOption(Options.REDIRECT_TO);
		//reconstruct a new request for the other machine
		StringBuilder newRequest = new StringBuilder();
		newRequest.append(request.getAction() + " ");
		for(Entry<String, String> e : request.getParseResult().entrySet()){
			if(e.getKey().equalsIgnoreCase(Options.REDIRECT_TO) || e.getKey().equalsIgnoreCase(request.getAction())){
				continue;
			} else if(e.getKey().equalsIgnoreCase(Options.DB_PASSWORD)){
				//Encrypt the password first.
				String password = e.getValue();
				if(!password.startsWith(CryptionModule.SECURITY_PREFIX)){
					if(password.startsWith(CryptionModule.PASSFILE_PREFIX)){
						try {
							password = CryptionModule.SECURITY_PREFIX + CryptionModule.getInstance().readPassfile(password.replace(CryptionModule.PASSFILE_PREFIX, ""));
						} catch (Exception exception) {
							return new STAFResult(STAFResult.JavaError, exception.getMessage());
						}
					} else {
						try {
							password = CryptionModule.SECURITY_PREFIX + CryptionModule.getInstance().getEncryptedPassword(password).result;
						} catch (Exception exception) {
							return new STAFResult(STAFResult.JavaError, exception.getMessage());
						} 
					}
				}
				newRequest.append(e.getKey() + " ");
				newRequest.append(password + " ");
			} else {
				newRequest.append(e.getKey() + " ");
				newRequest.append(e.getValue() + " ");
			}
		}
		//new options are added to make the new request a INCOMING request for the other machine.
		newRequest.append(Options.RID + " " + request.getRid() + " ");
		newRequest.append(Options.ORIGIN_HOST + " " + DB2WorkloadService.getLOCALHOST() + " ");
		newRequest.append(Options.ORIGIN_PORT + " " + RMIRegistry.INSTANCE.getPort());
		//new request will be submitted to host, and a result will be returned.
		return STAFHandler.instance.performRequest(host, "db2wkl", newRequest.toString());
	}

	/**
	 * @param request as the InternalRequest to start
	 * @return STAFResult of the InternalRequest
	 * @throws InterruptedException - The current Thread has to wait of the internal Thread
	 */
	private STAFResult startInternalRequest(ARequest request) {
		this.internalRequests.add(request);
		InternalRequest intReq = null;
		//retrieve the internal request class from the list according to the request action
		Class<? extends InternalRequest> clazz = this.availableInternalRequests.get(request.getAction().toUpperCase());
		if(clazz == null){
			return new STAFResult(STAFResult.InvalidRequestString, "The specified action \'" + request.getAction() + "\' does'nt have a corresponding request handler.");
		}
		try {
			intReq = clazz.newInstance();
		} catch (IllegalAccessException e) {
			return new STAFResult(STAFResult.JavaError, e.getMessage());
		} catch (InstantiationException e) {
			return new STAFResult(STAFResult.JavaError, e.getMessage());
		}
		intReq.init(request);
		Thread.currentThread().setName(intReq.getName());
		intReq.run();
		request.setEndTime(new Date());
		STAFResult result = intReq.getResult();
		return result;
	}

	/**
	 * Starts a new JVM for the request
	 * @param request to start, local or incoming request
	 * @return STAFResult
	 */
	private STAFResult startNewJVMForRequest(ARequest request) {
		//replace simple workload name with its full name when the action is 'wkl'
		if(request.getAction().equalsIgnoreCase(Options.WKL_WORKLOAD)){
			if(request.hasRequestOption(Options.WKL_START)){
				
				try {
					WorkloadClassViewModel workloadClassViewModel = new WorkloadClassViewModel();
					WorkloadClass workloadClass = workloadClassViewModel.getWorkloadClass(request.getRequestOption(Options.WKL_START));
					
					if (workloadClass == null) {
						return new STAFResult(STAFResult.InvalidRequestString, "The specified workload " + request.getRequestOption(Options.WKL_START) + " can not be found. Use 'list workloads' request to obtain a list of available workloads.");
					}
				} catch (IOException e) {
					Logger.log("Loading the workload libraries failed", LogLevel.Error);
					return new STAFResult(STAFResult.JavaError, e.getLocalizedMessage());
				}				
			}
		}
		
		//used for RMI code base
		URL coreJar;
		try {
			coreJar = new File(DB2WorkloadServiceDirectory.getDB2WorkloadServiceCoreJar()).toURI().toURL();
		} catch (MalformedURLException e) {
			Logger.logException(e);
			return new STAFResult(STAFResult.JavaError, e.getLocalizedMessage());
		}
		//setup classpath
		String classpath = System.getProperty("java.class.path") + File.pathSeparator + 
							DB2WorkloadServiceDirectory.getDB2WorkloadServiceRequestJar() + File.pathSeparator +
							DB2WorkloadServiceDirectory.getDB2WorkloadServiceCoreJar() + File.pathSeparator +
							DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryLibs() + File.separator + "*" + File.pathSeparator +
							DB2WorkloadServiceDirectory.getDb2WorkloadServiceDirectoryWorkloads() + File.separator + "*" + File.pathSeparator;
		
		//add jcc libs to class path. 
		File jccLibFolder = null;
		if(request.hasRequestOption(Options.JCCLIBS)){
			jccLibFolder = new File(request.getRequestOption(Options.JCCLIBS));
		} else {
			//TODO use JCCLIB folder without STAF variable?
			jccLibFolder = new File(Settings.tryJCCLibs());
		}
		
		if(jccLibFolder.isDirectory()){
			classpath = jccLibFolder.getAbsolutePath() + File.separator + "*" + File.pathSeparator + classpath + File.pathSeparator;
		} else {
			return new STAFResult(STAFResult.JavaError, jccLibFolder.getAbsolutePath() + " is not a folder or does not exist.");
		}
		
		//add pdq libs
		if(request.hasRequestOption(Options.PDQLIBS)){
			File pdqLibsDir = new File(request.getRequestOption(Options.PDQLIBS));
			if(pdqLibsDir.isDirectory()){
				classpath = pdqLibsDir.getAbsolutePath() + File.separator + "*" + File.pathSeparator + classpath + File.pathSeparator;
			} else {
				return new STAFResult(STAFResult.JavaError, pdqLibsDir.getAbsolutePath() + " is not a folder or does not exist.");
			}
		}
		
		CaseInsensitiveArrayList command = new CaseInsensitiveArrayList();

		command.addAll(request.getJVMArguments());
		
		//JVM options for RMI
		command.add("-Djava.security.policy=" + convertToCommandLineCompliantPath(DB2WorkloadService.getSecuriyPolicy().toString()));
		command.add("-Djava.rmi.server.codebase=" + convertToCommandLineCompliantPath(coreJar.toString()));
		
		//classpath
		command.add("-cp");
		command.add("\"" + classpath + "\"");
		
		//class where the main method is
		command.add("com.ibm.db2wkl.request.RequestPerformer");
		
		//the following are passed as parameter args[] for main method
		//used to setup RMI connection
		command.add(request.getOrigin_host());
		command.add(String.valueOf(request.getOrigin_port()));
		
		command.add(DB2WorkloadService.getLOCALHOST());
		command.add(String.valueOf(RMIRegistry.INSTANCE.getPort()));
		
		//used to initialize the DB2WorkloadServiceDirectory in the request JVM, 
		command.add("\"" + DB2WorkloadServiceDirectory.getDB2WorkloadServiceDirectory() + "\"");
		
 		try {
			command.addAll(request.getProgramArguments());
		} catch (Exception e1) {
			Logger.log(e1.getMessage(), LogLevel.Error);
			return new STAFResult(STAFResult.JavaError, "Creating program arguments failed (check if your request is valid):" + e1.getMessage());
		}
 		
 		//print the command
 		String printableCommand = "";
 		for (String cmd : command) {
			printableCommand += cmd.trim() + " ";
		}
 		Logger.log(printableCommand, LogLevel.Info);
 		
 		ProcessBuilder pb = new ProcessBuilder(command);
// 		ProcessBuilder pb = new ProcessBuilder(b);
 		Process process = null;
		try {
			process = pb.start();
		} catch (IOException e) {
			Logger.logException(e);
			return new STAFResult(STAFResult.JavaError, e.getMessage());
		} 
		
		int loopAttempt = 0; // to avoid unexpected infinite loop
		//this loop checks whether the new process(JVM) exit before the request is added to the remoteRequest map via RMI.(see com.ibm.db2wkl.request.RequestPerformer)
		//Any exception occurs before the request is added is caught which causes the process to exit, and is written to the error stream.
		//beyond that point, exceptions can be logged using RMI
		//When debugging the new JVM, it might take a lot longer till the request to be added, hence the infinite loop detection is disabled.
		while(!this.runningRequests.containsKey(request.getOrigin_host(), request.getRid()) && (request.isDebugging() ? true : loopAttempt < 300)){
			loopAttempt++;
			try {
				//check if the request process exit with error in the beginning
				int exitValue = process.exitValue();
				//if the above line returns successfully, it means the new JVM has exited, but the request is not added to the map via RMI.
				//The exception which causes this will be written to the error stream in the new JVM.
				BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String error = "";
				String line = "";
				while((line = br.readLine()) != null){
					error += line + "\n";
				}
				Logger.log("Request JVM exits unexpectedly. Reason: " + error, LogLevel.Error);
				return new STAFResult(STAFResult.JavaError, "Request JVM exits unexpectedly with value " + exitValue + ". \n" + error);
			} catch(IllegalThreadStateException e){
				//this means the process did not yet exit, which is desired.
			} catch (IOException e) {
				Logger.log(e.getMessage(), LogLevel.Error);
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				//
			}
		}
		//if the new JVM execute correctly, the request will added to the map 'remoteRequest' using RMI, hence the request is successfully accepted.
		if(this.runningRequests.containsKey(request.getOrigin_host(), request.getRid())){
			
			this.requestProcesses.put(request.getOrigin_host(), request.getRid(), process);
			if(request.getType() == RequestType.LOCAL) {
				return new STAFResult(STAFResult.Ok, "Request (id=" + request.getRid() + ") is accepted. " + "Use REQUEST " + request.getRid() + " for status info. Review log in case of unexpected behaviour.");		
			} else {
				return new STAFResult(STAFResult.Ok, "Request (id=" + request.getRid() + ") is accepted on " + DB2WorkloadService.getLOCALHOST() + ". Use REQUEST " + request.getRid() + " for status info. Review log in case of unexpected behaviour.");		
			}
		} else {
			//this happens when the new JVM did neither exit, nor is the request added to the map after a certain amount of time.
			process.destroy();
			return new STAFResult(STAFResult.JavaError, "Unexpected behaviour. Please start a new request.");
		}
	}
	
	/**
	 * Replaces the blanks in a path with %20 so that there are no issues when
	 * used on a command line process call 
	 * 
	 * @param pathWithBlanks input path
	 * @return the new path where all blanks are replaced with %20
	 */
	private String convertToCommandLineCompliantPath(String pathWithBlanks)
	{
		return pathWithBlanks.replaceAll(" ", "%20");
	}

	/**
	 * This method will poll and start the first request stored within this service 
	 * that could not be started before
	 * @return STAFResult OK if a request is stored and can be executed
	 */
	public synchronized STAFResult startQueuedRequests() {

		ARequest request = this.requestsOnHold.peek();
		
		if(request==null) 
			return new STAFResult(STAFResult.JavaError, "No stored request found");
		
		STAFResult result = startRequest(request);
		
		if(result.rc == STAFResult.Ok){
			Logger.log("Stored Request with the ID " + request.getRid() + "is started!" + result.result, LogLevel.Info);
			this.requestsOnHold.poll();
			return new STAFResult(STAFResult.Ok, "Stored Request with the id " + request.getRid() + "is successfully started!" + result.result);
		} else {
			Logger.log("Stored Request with the id " + request.getRid() + " cannot be started!" + result.result, LogLevel.Error);
			return new STAFResult(STAFResult.JavaError, "Stored Request with the id " + request.getRid() + " cannot be started!" + result.result);
		}
	}
	
	/**
	 * This method will poll and start the first request stored within this service 
	 * that could not be started before
	 * @param id with the stored requests id
	 * @return STAFResult OK if a request is stored and can be executed
	 */
	public synchronized STAFResult startQueuedRequests(Long id) {
		ARequest request = this.requests.get(DB2WorkloadService.getLOCALHOST(), id);
		
		if(!this.requestsOnHold.contains(request)) 
			return new STAFResult(STAFResult.JavaError, "This request is not stored!");
		
		STAFResult result = startRequest(request);
		
		if(result.rc == STAFResult.Ok){
			Logger.log("Stored Request with the ID " + request.getRid() + " is started!" + result.result, LogLevel.Info);
			this.requestsOnHold.remove(request);
			return new STAFResult(STAFResult.Ok, "Stored Request with the id " + request.getRid() + "is successfully started!" + result.result);
		} else {
			Logger.log("Stored Request with the id " + request.getRid() + " cannot be started!" + result.result, LogLevel.Error);
			return new STAFResult(STAFResult.JavaError, "Stored Request with the id " + request.getRid() + " cannot be started!" + result.result);
		}
	}
	
	/**
	 * @param id as the request id of the request that shall be removed
	 * @return STAFResult OK if the request was successfully removed from the queue
	 */
	public synchronized STAFResult cancelQueuedRequest(Long id) {
		ARequest request = this.requests.get(DB2WorkloadService.getLOCALHOST(), id);
		
		if(!this.requestsOnHold.contains(request)) 
			return new STAFResult(STAFResult.JavaError, "This request is not stored!");
		
		if(this.requestsOnHold.remove(request)) {
			return new STAFResult(STAFResult.Ok, "Stored Request was successfully canceled!");
		}
		
		return new STAFResult(STAFResult.JavaError, "Stored Request could not be canceled!");
	}

	/**
	 * @return the requests
	 */
	public TwoKeysMap<String, Long, ARequest> getRequests() {
		return this.requests;
	}

	/**
	 * @return the requestsOnHold
	 */
	public ConcurrentLinkedQueue<ARequest> getRequestsOnHold() {
		return this.requestsOnHold;
	}
	
	@Override
	public void addRequest(Long id, String host, RequestRemoteInterface stub) throws RemoteException {
		this.runningRequests.put(host, id, stub);
	}

	@Override
	public void notify(Long rid, String host, String state, int resultCode, String result) throws RemoteException {
		switch (RequestState.valueOf(state)) {
			case NEW:
				break;
			case INITIALIZED:
				break;
			case EXECUTED:
				break;
			case FINISHED:
				if(host.equalsIgnoreCase(DB2WorkloadService.getLOCALHOST())){
					//the request is originated by this machine.
					this.requests.get(host, rid).setEndTime(new Date());
					this.requests.get(host, rid).setResult(new STAFResult(resultCode, result));
					this.finishedRequests.add(this.requests.get(host, rid));
					this.runningRequests.remove(host, rid);
					//only local request has process on this machine. removing for remote request simply does nothing.
					this.requestProcesses.remove(host, rid);
					startQueuedRequests();
				} else {
					//the request is incoming request, simply remove it from the map
					this.runningRequests.remove(host, rid);
					this.requestProcesses.remove(host, rid);
				}
				break;
			case BROKEDOWN:
				if(host.equalsIgnoreCase(DB2WorkloadService.getLOCALHOST())){
					//the request is originated by this machine.
					this.requests.get(host, rid).setEndTime(new Date());
					this.requests.get(host, rid).setResult(new STAFResult(resultCode, result));
					this.brokedownRequests.add(this.requests.get(host, rid));
					this.runningRequests.remove(host, rid);
					//only local request has process on this machine. removing for remote request simply does nothing.
					this.requestProcesses.remove(host, rid);
					startQueuedRequests();
				} else {
					//the request is incoming request, simply remove it from the map
					this.runningRequests.remove(host, rid);
					this.requestProcesses.remove(host, rid);
				}
				break;
			case STOPPED:
				if(host.equalsIgnoreCase(DB2WorkloadService.getLOCALHOST())){
					//the request is originated by this machine.
					this.requests.get(host, rid).setEndTime(new Date());
					this.requests.get(host, rid).setResult(new STAFResult(resultCode, result));
					this.stoppedRequests.add(this.requests.get(host, rid));
					this.runningRequests.remove(host, rid);
					//only local request has process on this machine. removing for remote request simply does nothing.
					this.requestProcesses.remove(host, rid);
					startQueuedRequests();
				} else {
					//the request is incoming request, simply remove it from the map
					this.runningRequests.remove(host, rid);
					this.requestProcesses.remove(host, rid);
				}
				break;
			case CLEANING:
				break;
			case EXECUTING:
				break;
			case INITIALIZING:
				break;
			case STOPPING:
				break;
		}
	}
	
	/**
	 * Whenever a remote exception is thrown while communicating with a request, this method should be used to notify the 
	 * RequestManager of the brokendown of communication.
	 * @param rid request id
	 * @param host request's originated host
	 * @param e exception
	 */
	public void notifyRemoteException(Long rid, String host, RemoteException e) {
		try {
			notify(rid, host, RequestState.BROKEDOWN.name(), STAFResult.CommunicationError, e.getMessage());
		} catch (RemoteException e1) {
			//local call won't call such exception
		}
	}

	/**
	 * @return the requests that was originated by current machine.
	 */
	public ConcurrentHashMap<Long, RequestRemoteInterface> getOriginatedRequests(){
		if(this.runningRequests.get(DB2WorkloadService.getLOCALHOST()) == null){
			this.runningRequests.put(DB2WorkloadService.getLOCALHOST(), new ConcurrentHashMap<Long, RequestRemoteInterface>());
		}
		return this.runningRequests.get(DB2WorkloadService.getLOCALHOST());
	}

	/**
	 * 
	 */
	public void clean() {
		this.brokedownRequests.clear();
		this.finishedRequests.clear();
		this.stoppedRequests.clear();
	}
	
	/**
	 * Registers all internal requests
	 */
	private void registerAllInternalRequests(){
		this.availableInternalRequests.put(Options.HELP_HELP, HelpHandler.class);
		this.availableInternalRequests.put(Options.LOG_LOG, LoggingHandler.class);
		this.availableInternalRequests.put(Options.VERSION_VERSION, VersionHandler.class);
		this.availableInternalRequests.put(Options.MISC_MISC, MiscHandler.class);
		this.availableInternalRequests.put(Options.REPORT_REPORT, ReportHandler.class);
		this.availableInternalRequests.put(Options.REQ_REQUEST, RequestHandler.class);
		this.availableInternalRequests.put(Options.LIST_LIST, ListHandler.class);
		this.availableInternalRequests.put(Options.SETUP_SETUP, SetupHandler.class);
	}

	/**
	 * @return finished requests
	 */
	public ArrayList<ARequest> getFinishedRequests() {
		return this.finishedRequests;
	}

	/**
	 * @return brokendown requests
	 */
	public ArrayList<ARequest> getBrokedownRequests() {
		return this.brokedownRequests;
	}
	
	/**
	 * @return stopped requests
	 */
	public ArrayList<ARequest> getStoppedRequests() {
		return this.stoppedRequests;
	}
	
	/**
	 * @return internal requests
	 */
	public ArrayList<ARequest> getInternalRequests(){
		return this.internalRequests;
	}
	
	/**
	 * @return a report of current system status
	 */
	public Report getCurrentSystemStatus(){
		Report report = new Report();
		//memory and threads of the staf service JVM. Other metrics are not monitored on this JVM
		report.setConsumed_memory(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
		report.setNumber_of_threads(Thread.currentThread().getThreadGroup().activeCount());
		//looking for all local running requests and add up their reports
		for(String host : this.requestProcesses.key1Set()){
			for(Long rid : this.requestProcesses.get(host).keySet()){
				try {
					RequestRemoteInterface request = this.runningRequests.get(host, rid);
					if(request != null){
						RequestReporterRemoteInterface reporter = request.getRequestReporter();
						Report latestReport = request.getRequestReporter().getLatestReport();
						if(reporter != null && latestReport != null){
							report.add(latestReport);
						}
					}
				} catch (RemoteException e) {
					Logger.logException(e);
					notifyRemoteException(rid, host, e);
					continue;
				}
			}
		}
		
		return report;
	}
}