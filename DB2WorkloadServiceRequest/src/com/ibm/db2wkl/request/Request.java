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

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.ibm.db2wkl.helper.NumberCounter;
import com.ibm.db2wkl.logging.LogLevel;
import com.ibm.db2wkl.request.connection.DB2WklDataSource;
import com.ibm.db2wkl.request.connection.DataSourceFactory;
import com.ibm.db2wkl.request.handler.SPHandler;
import com.ibm.db2wkl.request.handler.SQLHandler;
import com.ibm.db2wkl.request.handler.WorkloadHandler;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.rmi.RequestNotifierRemoteInterface;
import com.ibm.db2wkl.rmi.RequestRemoteInterface;
import com.ibm.db2wkl.rmi.RequestReporterRemoteInterface;
import com.ibm.db2wkl.workloadservice.ADataSourceConsumer;
import com.ibm.staf.STAFResult;

/**
 * A Request object all information that is given by a request. In other words
 * each request is represented by an instance of this object.
 * 
 * <br/>
 * <br/>
 */
public class Request extends ARequest implements Runnable, RequestRemoteInterface {
	
	/**
	 * Request on itself. Each JVM will only have one instance of request
	 */
	public static Request request;
	
	/**
	 * This is the private output formatter for the request result
	 */
	protected Output output = null;

	/**
	 * Current state this request is in
	 */
	protected RequestState state;
	
	/**
	 * Holds a flag to indicate whether this request requires a data source
	 */
	protected boolean dataSourceRequired;
	
	/**
	 * This is the list of sub requests that are available in DB2WKL. It is not the
	 * list of sub requests in this service.
	 */
	private HashMap<String, Class<? extends ASubRequest>> availableSubRequests;
	
	/**
	 * List of notifiers that get notified when the request state changes
	 */
	protected ArrayList<RequestStateChangedListener> listeners;

	/**
	 * The sub requests for this request. It depends on the action
	 * in the current request.
	 */
	protected HashMap<String, ASubRequest> subRequests; 
	
	/**
	 * Counter for child threads for this request
	 */
	protected NumberCounter threadCounter = null;
	
	/**
	 * This is the private connection manager for this request which is mainly,
	 * but not exclusively used by workloads
	 */
	protected DB2WklDataSource db2wklDataSource = null;

	/**
	 *  the remote object of this request
	 */
	protected RequestRemoteInterface remoteObject;

	/**
	 * RequestNotifier for the request
	 */
	private RequestNotifier requestNotifier;

	/**
	 * request Notifier's Stub
	 */
	private RequestNotifierRemoteInterface requestNotifierStub;

	/**
	 * RequestReporter for the request
	 */
	private RequestReporter requestReporter;

	/**
	 * request Reporter's Stub
	 */
	private RequestReporterRemoteInterface requestReporterStub;

	/* ******************************************************
	 * Methods 
	 * ******************************************************
	 */

	/**
	 * Default constructor
	 * 
	 * @throws RemoteException 
	 * @throws IllegalStateException 
	 */
	public Request() throws RemoteException, IllegalStateException {
		if(Request.request == null){
			Request.request = this;
		} else {
			throw new IllegalStateException("Request is already created. Cannot create more than one request within a JVM.");
		}
		this.setRequestState(RequestState.NEW);
		this.listeners = new ArrayList<RequestStateChangedListener>();
		this.subRequests = new HashMap<String, ASubRequest>();
		this.threadCounter = new NumberCounter();
		this.remoteObject = (RequestRemoteInterface) UnicastRemoteObject.exportObject(this, 0);
		registerAllSubRequests();
		//when use this constructor(mostly with classloader), ARequest.init() should be called to initialize the request
	}
	
	/**
	 * @param aRequest
	 * @throws RemoteException 
	 */
	public Request(ARequest aRequest) throws RemoteException {
		this();
		init(aRequest);
	}
	
	/**
	 * Execute the request not threaded
	 * 
	 * @return the request execution result
	 */
	protected STAFResult go() {
		
		// do the initialisation
		if(this.getRequestState() == RequestState.STOPPED){
			return cleanupRequest();
		}
		this.setRequestState(RequestState.INITIALIZING);
		notifyStateChangedListeners(this.getResult());
		
		if(this.getRequestState() == RequestState.STOPPED){
			return cleanupRequest();
		}
		STAFResult initResult = initialize();
		
		if (initResult.rc != STAFResult.Ok) {
			this.setRequestState(RequestState.BROKEDOWN);
			this.setEndTime(new Date());
			notifyStateChangedListeners(initResult);
			this.setResult(initResult);
			return this.getResult();
		}

		// change state into initialized
		if(this.getRequestState() == RequestState.STOPPED){
			return cleanupRequest();
		}
		this.setRequestState(RequestState.INITIALIZED);
		notifyStateChangedListeners(initResult);

		// now run the request
		if(this.getRequestState() == RequestState.STOPPED){
			return cleanupRequest();
		}
		this.setRequestState(RequestState.EXECUTING);
		notifyStateChangedListeners(this.getResult());

		if(this.getRequestState() == RequestState.STOPPED){
			return cleanupRequest();
		}
		this.setResult(execute());
		if (this.getResult().rc != STAFResult.Ok) {
			this.setRequestState(RequestState.BROKEDOWN);
			this.setEndTime(new Date());
			notifyStateChangedListeners(this.getResult());
			return this.getResult();
		}
	
		// change state into executed
		if(this.getRequestState() == RequestState.STOPPED){
			return cleanupRequest();
		}
		this.setRequestState(RequestState.EXECUTED);
		notifyStateChangedListeners(this.getResult());
		
		// finished
		if(this.getRequestState() == RequestState.STOPPED){
			return cleanupRequest();
		}
		this.setRequestState(RequestState.CLEANING);
		notifyStateChangedListeners(this.getResult());
		
		if(this.getRequestState() == RequestState.STOPPED){
			return cleanupRequest();
		}

		this.setResult(cleanupRequest());
		if (this.getResult().rc != STAFResult.Ok) {
			this.setRequestState(RequestState.BROKEDOWN);
			this.setEndTime(new Date());
			notifyStateChangedListeners(this.getResult());
			return this.getResult();
		}

		this.setRequestState(RequestState.FINISHED);
		this.setEndTime(new Date());
		notifyStateChangedListeners(this.getResult());
		
		return this.getResult();
	}

	/**
	 * Initializes the request. This has to be done every time a request is
	 * created.
	 * 
	 * @return the result of the run
	 */
	public STAFResult initialize() {
		
		/*
		 * validation passed, thus provide the handlers with the request
		 */
		STAFResult res;
	
		/*
		 * output formatter
		 */
		this.output = new Output();
		
		if(Request.hasOption(Options.GEN_OUTFORMAT)){
			try {
				this.output.setFormat(Enum.valueOf(Output.Format.class, Request.getOption(Options.GEN_OUTFORMAT)));
			} catch (Exception e) {
				return new STAFResult(STAFResult.InvalidValue, "The value '" + Request.getOption(Options.GEN_OUTFORMAT) + "' is not allowed as output format. "
						+ "Use one of the following instead: TEXT STAF XML");
			}
		}


		res = setDataSourceRequired();
		
		if(res.rc != STAFResult.Ok){
			return res;
		}
		if(Request.isDataSourceRequired()) {
			// initialize the data source for this request
			DB2WklDataSource dataSourceManager = DataSourceFactory.createDataSource();
			this.db2wklDataSource = dataSourceManager;
			try {
				if(!DataSourceFactory.initializeDataSources(dataSourceManager)) 
					return new STAFResult(STAFResult.JavaError, "Connection test failed: " + DataSourceFactory.getErrorMessage());
			} catch (Exception e) {
				return new STAFResult(STAFResult.InvalidRequestString, e.getMessage());
			} 
		}	
			
		/*
		 * set the subrequests
		 */
		List<ASubRequest> sr = retrieveSubRequests();
		if (sr == null || sr.size() == 0) {
			return new STAFResult(STAFResult.JavaError, "No subrequest found");
		}
		this.subRequests.clear();

		for (ASubRequest receiver : sr) {
			this.subRequests.put(receiver.getName(), receiver);
		}

		return res;
	}
	
	/**
	 * Returns a new subrequest for the given request. It is recommended to
	 * do the validation before calling this method
	 * 
	 * @param consecutiveNumber a number that identifies the subrequest within the number of SRs in a request
	 * @param url the URL for this subrequest
	 * @return the new subrequest
	 */
	public ASubRequest createSubRequest(int consecutiveNumber, String url) {

		// get the list of available subrequests
		if (this.availableSubRequests == null) {
			return null;
		}
		
		Class<? extends ASubRequest> subRequest = this.availableSubRequests.get(this.action.toUpperCase());
		if(subRequest != null){
			try {
				ASubRequest sr = subRequest.newInstance();
				sr.setUrl(url);
				sr.setId(consecutiveNumber);
				sr.setName(this.name + ":" + consecutiveNumber);
				return sr;
				
			} catch (IllegalAccessException e) {
				Logger.log(
						"An IllegalAccessException occurred while trying to initialize"
								+ "the subrequest " + this.action + " : "
								+ e.getLocalizedMessage(), LogLevel.Error);
				return null;
			} catch (InstantiationException e) {
				Logger.log(
						"An InstantiationException occurred while trying to initialize"
								+ "the subrequest " + this.action + " : "
								+ e.getLocalizedMessage(), LogLevel.Error);
				return null;
			}
		} else {
			return null;
		}
	}
	

	/**
	 * @param subRequests
	 */
	public final void setAvailableSubRequests(HashMap<String, Class<? extends ASubRequest>> subRequests) {
		this.availableSubRequests = subRequests;
	}
	
	/**
	 * Check whether this request requires data source based on the action.
	 * This is done by checking the DataSourceRequired Annotation on the corresponding SubRequest
	 * @return result
	 */
	protected STAFResult setDataSourceRequired() {
		if(this.availableSubRequests == null){
			return new STAFResult(STAFResult.JavaError, "Error occurs while checking if data source is required. The available subrequests are not present.");
		} 
		if(this.action == null) {
			return new STAFResult(STAFResult.JavaError, "Error occurs while checking if data source is required. The action for this request is not yet set.");
		}
		Class<? extends ASubRequest> subRequest = this.availableSubRequests.get(this.action.toUpperCase());
		if(subRequest == null){
			return new STAFResult(STAFResult.JavaError, "The providing action " + this.action + " doesn't have a corresponding sub request.");
		} else {
			if(subRequest.isAnnotationPresent(DataSourceRequired.class)){
				this.dataSourceRequired = true;
			}else {
				this.dataSourceRequired = false;
			}
			return new STAFResult(STAFResult.Ok);
		}
	}
	
	/**
	 * All initialisation of this request is done, now that initialize() was
	 * called. This method will now start the real execution of this request
	 * 
	 * @return the STAF result of this request
	 */
	@SuppressWarnings("boxing")
	public STAFResult execute() {

		STAFResult executeResult;
		
		// print some debug info for sub requests
		Logger.log("Number of subrequests: " + Request.getSubRequests().size(), LogLevel.Debug);

		// use the sub requests and start it
		List<Thread> threads = new ArrayList<Thread>();
		final HashMap<ASubRequest, Boolean> runningSubRequests = new HashMap<ASubRequest, Boolean>();
		for (Entry<String, ASubRequest> sr : Request.getSubRequests().entrySet()) {
			
			// set up the subrequest
			ASubRequest subRequest = sr.getValue();
			subRequest.setRequest(this);
			subRequest.addSubRequestFinishedEventListener(new SubRequestFinishedEventListener() {
				
				@Override
				public void subRequestFinished(@SuppressWarnings("hiding") ASubRequest subRequest) {
					synchronized (runningSubRequests) {
						runningSubRequests.put(subRequest, Boolean.FALSE);
					}
				}
			});
			
			// capsulate the sub request into a managed framework thread to execute them in parallel
			ManagedFrameworkThread subRequestThread = new ManagedFrameworkThread(subRequest);

			// put the thread into a queue to wait until it is finished
			threads.add(subRequestThread);
			Logger.log("Start subrequest " + subRequest.getName(), LogLevel.Debug);
			// start the thread
			runningSubRequests.put(subRequest, Boolean.TRUE);
			subRequestThread.start();
		}
		
		// wait for the subrequests to end
		int stillRunning = 0;
		do {
			stillRunning = 0;
			
			for (ASubRequest subRequest : this.subRequests.values()) {
				if(runningSubRequests.get(subRequest))
					stillRunning++;
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// nop
			}
		} while (stillRunning > 0);
		
		Logger.log("All sub requests have finished", LogLevel.Info);
		
		//set the right end-time of the Request
		ArrayList<ADataSourceConsumer> dataSourceConsumers = getDataSourceConsumers();
		if(dataSourceConsumers.size() <= 0){
			setEndTime(new Date());
		} else {
			Date lastEndTime = dataSourceConsumers.get(0).getEndDate();
			for(ADataSourceConsumer workload : dataSourceConsumers){
				Date compareEndTime = workload.getEndDate();
				if(lastEndTime.before(compareEndTime)){
					lastEndTime = compareEndTime;
				}
			}
			setEndTime(lastEndTime);
		}
		
		this.setRequestState(RequestState.STOPPED);
		
		STAFResult stopResult = new STAFResult(STAFResult.Ok, getName() + " is successfully stopped");
		notifyStateChangedListeners(stopResult);
		
		// reformat the result in case another output format was specified
		switch (this.output.getFormat()) {
		case TEXT:
			int maxrc = 0;
			
			StringBuilder resultText = new StringBuilder();
			resultText.append("\n");
			
			boolean onlyGoodReturnCodes = true;
			
			// check for the number of results
			if (Request.getSubRequests().size() == 0) {
				Logger.log("There is no subrequest in the request", LogLevel.Error);
			} else {
				for (ASubRequest sr : Request.getSubRequests().values()) {
					if(sr.getResult() != null && sr.getResult().rc > 0) {
						resultText.append("[Thread: ");
						resultText.append(sr.getName());
						resultText.append("] [Return Code: ");
						resultText.append(sr.getResult().rc);						
						resultText.append("] ");
						resultText.append(sr.getText());
						resultText.append("\n");
						
						// set max rc
						if (maxrc < sr.getResult().rc) 
							maxrc = sr.getResult().rc;
						
						if (sr.getResult().rc != STAFResult.Ok && 
							sr.getResult().rc != STAFResult.RequestCancelled) {
							onlyGoodReturnCodes = false;
						}
					}
				}
			}
			
			if (onlyGoodReturnCodes) {
				executeResult = new STAFResult(STAFResult.Ok);	
			} else {
				executeResult = new STAFResult(maxrc, resultText.toString());
			}
			
			break;
		case STAF:
			
			if (Request.getSubRequests().size() == 0) {
				Logger.log("There is no subrequest in the request", LogLevel.Error);
				executeResult = new STAFResult(STAFResult.JavaError, "There is no subrequest in the request");
			} else if (Request.getSubRequests().size() == 1) {
				
				// get the only subrequest in the queue
				String key = (String) Request.getSubRequests().keySet().toArray()[0];
				ASubRequest rr = Request.getSubRequests().get(key); 
				
				// get the result
				executeResult = new STAFResult(STAFResult.Ok, rr.getStaf());
				
			} else {
				executeResult = new STAFResult(STAFResult.Ok, "2");
			}
			
			break;
		case XML:
			//TODO iterate over all rrs and build up compulsory XML
//			Document resultDocument = Request.getSubRequests().getXML(executeResult);
//			String resultDocumentString = "";
//			if (resultDocument != null) {
//				try {
//					Source source = new DOMSource(resultDocument);
//					StringWriter stringWriter = new StringWriter();
//					javax.xml.transform.Result transformResult = new StreamResult(stringWriter);
//					TransformerFactory factory = TransformerFactory.newInstance();
//					Transformer transformer = factory.newTransformer();
//
//					transformer.setOutputProperty(OutputKeys.VERSION, "yes");
//					transformer.setOutputProperty(OutputKeys.ENCODING, "yes");
//					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//
//					transformer.transform(source, transformResult);
//					resultDocumentString = stringWriter.getBuffer().toString();
//
//				} catch (TransformerConfigurationException e) {
//					executeResult = new STAFResult(STAFResult.JavaError, "("
//							+ e.getClass().getName() + ")"
//							+ e.getLocalizedMessage());
//				} catch (TransformerFactoryConfigurationError e) {
//					executeResult = new STAFResult(STAFResult.JavaError, "("
//							+ e.getClass().getName() + ")"
//							+ e.getLocalizedMessage());
//				} catch (TransformerException e) {
//					executeResult = new STAFResult(STAFResult.JavaError, "("
//							+ e.getClass().getName() + ")"
//							+ e.getLocalizedMessage());
//				}
//			} else {
//				resultDocumentString = "result document is null";
//			}
//			executeResult = new STAFResult(executeResult.rc, resultDocumentString);
			executeResult = new STAFResult(STAFResult.Ok);
			break;
		default:
			executeResult = new STAFResult(STAFResult.UnknownError);
			break;
		}

		// return the formatted output
		return executeResult;
	}

	/**
	 * Cleanup processing for this request
	 * 
	 * @return the result of the cleanup process
	 */
	public STAFResult cleanupRequest() {
		if(Request.isDataSourceRequired())
			getDataSource().terminate();
		
		return new STAFResult(STAFResult.Ok, getName() + " is successfully cleaned up.");
	}

	/**
	 * @return STAFResult
	 */
	public STAFResult stopRequest() {
		this.setRequestState(RequestState.STOPPING);
		
		//stop all workloads
		for (ASubRequest subRequest : this.subRequests.values()) {
			subRequest.stopForce();
		}
		
		//set the right end-time of the Request
//		ArrayList<ADataSourceConsumer> dataSourceConsumers = getDataSourceConsumers();
//		if(dataSourceConsumers.size() <= 0){
//			setEndTime(new Date());
//		} else {
//			Date lastEndTime = dataSourceConsumers.get(0).getEndDate();
//			for(ADataSourceConsumer workload : dataSourceConsumers){
//				Date compareEndTime = workload.getEndDate();
//				if(lastEndTime.before(compareEndTime)){
//					lastEndTime = compareEndTime;
//				}
//			}
//			setEndTime(lastEndTime);
//		}
//		
//		this.setRequestState(RequestState.STOPPED);
//		
		STAFResult stopResult = new STAFResult(STAFResult.Ok, getName() + " is stopping");
//		notifyStateChangedListeners(stopResult);
		
		return stopResult;
	}


	/**
	 * Registers all subrequest. For each new subrequest, a new 
	 * line has to be added to this method to add it to the hash map for
	 * subrequests.
	 */
	private void registerAllSubRequests() {
		// register the subrequests
		HashMap<String,Class<? extends ASubRequest>> srs = new HashMap<String, Class<? extends ASubRequest>>();
		
		srs.put(Options.SQL_SQL, SQLHandler.class);
		srs.put(Options.WKL_WORKLOAD, WorkloadHandler.class);
		srs.put(Options.SP_SP, SPHandler.class);
		
		this.setAvailableSubRequests(srs);
	}
	
	/**
	 * @return the data base connection manager
	 */
	public static DB2WklDataSource getDataSource() {
		return Request.getRequest().db2wklDataSource;
	}
	
	/**
	 * @return list of data source consumers
	 */
	public ArrayList<ADataSourceConsumer> getDataSourceConsumers() {
		if(getSubRequests() != null){
			ArrayList<ADataSourceConsumer> consumers = new ArrayList<ADataSourceConsumer>();
			for (ASubRequest receiver : getSubRequests().values()) {
				consumers.add(receiver.getDataSourceConsumer());
			}
			return consumers;
		} else { 
			return null;
		}
	}
	
	
	@Override
	public ArrayList<HashMap<String, String>> getWorkloadInfo(){
		//refer to the RequestHandler for the map keys 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		if(getSubRequests() != null){
			for (ASubRequest sr : getSubRequests().values()) {
				ADataSourceConsumer workload = sr.getDataSourceConsumer();
				String workload_start_time = "non-started";
				String workload_end_time = "non-completed";
				
				HashMap<String, String> resultMap = new HashMap<String, String>();
				resultMap.put("requestID", " - ");
				resultMap.put("wklID", String.valueOf(workload.getId()));
				resultMap.put("status", workload.getStatus().toString());
				if (workload.getStartDate() != null) {
					resultMap.put("start_time", sdf.format(workload.getStartDate()));
				} else {
					resultMap.put("start_time", workload_start_time);
				}
				
				if (workload.getEndDate() == null) {
					resultMap.put("end_time", workload_end_time);
				} else { // the end time is only for workloads which are finished, abend or brokedown
					workload_end_time = sdf.format(workload.getEndDate());
					resultMap.put("end_time", workload_end_time);
				}
				resultMap.put("type", workload.getName());
				resultMap.put("list_of_workloads", " - ");
				list.add(resultMap);
			}
		}
		return list;
	}
	
	/**
	 * @return the state
	 */
	public RequestState getRequestState() {
		return this.state;
	}

	/**
	 * @param state
	 */
	public void setRequestState(RequestState state) {
		this.state = state;
	}
	


	/**
	 * @return the request
	 */
	public static Request getRequest() {
		if(Request.request == null){
			throw new IllegalStateException("Request is not yet initilized.");
		} else {
			return Request.request;
		}
	}
	
	/**
	 * @return requestName
	 */
	public static String getRequestName() {
		return Request.getRequest().name;
	}

	/**
	 * @return the subrequests
	 */
	public static HashMap<String, ASubRequest> getSubRequests() {
		return Request.getRequest().subRequests;
	}
	
	/**
	 * @return the thread counter
	 */
	public static NumberCounter getThreadCounter() {
		return Request.getRequest().threadCounter;
	}
	
	/**
	 * @return whether a data source is required or not
	 */
	public static boolean isDataSourceRequired() {
		return Request.getRequest().dataSourceRequired;
	}
	
	/**
	 * @return request parseResult
	 */
	public static HashMap<String, String> getRequestOptions(){
		return Request.getRequest().getParseResult();
	}
	
	/**
	 * @param option
	 * @return has option
	 */
	public static boolean hasOption(String option){
		return Request.getRequest().hasRequestOption(option);
	}
	
	/**
	 * @param option
	 * @return value of the option
	 */
	public static String getOption(String option){
		return Request.getRequest().getRequestOption(option);
	}
	
	/**
	 * @return loglevel 
	 */
	public static LogLevel getLogLevel(){
		return Request.getRequest().logLevel;
	}
	
	/**
	 * @return log details
	 */
	public static boolean isLogDetails() {
		return Request.getRequest().logDetails;
	}
	
	/**
	 * adds a listener to the list of state change listeners
	 * 
	 * @param listener
	 *            listener to add
	 */
	public void addRequestStateChangedListener(RequestStateChangedListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * removes a listener from the list of state change listeners
	 * 
	 * @param listener
	 *            listener to remove
	 */
	public void removeRequestStateChangedListener(RequestStateChangedListener listener) {
		this.listeners.remove(listener);
	}

	/**
	 * This method creates a list of subrequests. For internal requests, per default
	 * only one request is returned. But for threaded requests, this method is overridden
	 * and the List of ASR objects contains at least one ASR.
	 * 
	 * @return the subrequests for this request
	 */
	protected List<ASubRequest> retrieveSubRequests() {
	
		// list of subrequests
		List<ASubRequest> rrs = new ArrayList<ASubRequest>();
	
		// get the URLs from the Request
		ArrayList<String> urls = null;
		if(Request.isDataSourceRequired()){
			urls = Request.getDataSource().getURLsList();
		}
	
		/*
		 * handles the THREADS option
		 */
		
		if (Request.hasOption(Options.THREADED_INSTANCES)) {
			try {
				// get the number of threads
				int noOfThreads = Integer.parseInt(Request.getOption(Options.THREADED_INSTANCES));
				
				// build up the sub requests depending on the number of URLs
				if (urls == null || urls.size() == 0) {
					for (int i = 0; i < noOfThreads; i++) {
						rrs.add(createSubRequest(i, null));
					}
				} else {
					int j = 0;
					for (String url : urls) {
						for (int i = 0; i < noOfThreads; i++) {
							rrs.add(createSubRequest(j, url));
							j++;
						}
					}
				}
				
				Logger.log("Created " + rrs.size() + " subrequests (threaded)", LogLevel.Debug);
				
			} catch (NumberFormatException e) {
				Logger.log(Request.getOption(Options.THREADED_INSTANCES) + 
						" is no valid positive number", LogLevel.Error);
				return null;
			} 
		} else {
			if (urls == null) {
				rrs.add(createSubRequest(0, null));
			} else {
				int i = 0;
				for (String url : urls) {
					rrs.add(createSubRequest(i, url));
					i++;
				}
			}
		}
		
		return rrs;
	}

	@Override
	public void run() {
		
		// initialize the requestReporter within the request thread 
		// to ensure that the timer thread is in the same thread group as the request
//		this.requestReporter.init();
		
		// start the requestReporter only when the Reporter is on. 
		try {
			if(RequestPerformer.getReporterStub().getStatus()){
				this.requestReporter.start();
			}
		} catch (RemoteException e) {
			this.result = new STAFResult(STAFResult.JavaError, e.getMessage());
			Logger.log(this.getResult().result, LogLevel.Error);
			return;
		}
	
		go();
	
		// create a log entry
		if (this.getResult().rc != STAFResult.Ok) {
			Logger.log("The execution " + getAction().toUpperCase() + " was not successful due to the following reason: " +
																							this.getResult().result, LogLevel.Error);
		} else {
			Logger.log("The workload execution was successful", LogLevel.Info);
		}
		
		
		if(this.requestReporter != null){
			//wait a while to terminate the requestReporter to report after the request is finished.
			try {
				Thread.sleep(RequestReporter.PERIOD);
			} catch (InterruptedException e) {
				//
			}
			try {
				this.requestReporter.terminate();
			} catch (RemoteException e) {
				this.result = new STAFResult(STAFResult.JavaError, e.getMessage());
				Logger.log(this.getResult().result, LogLevel.Error);
				return;
			}
		}
		terminateRMI();
		// remove the references that are not required anymore, thus release the memory
		this.subRequests.clear();
	}

	/**
	 * @return requestNotifier
	 * @throws RemoteException 
	 */
	@Override
	public RequestNotifierRemoteInterface getRequestNotifier() throws RemoteException {
		return this.requestNotifierStub;
	}

	/**
	 * @return the requestReporter
	 * @throws RemoteException 
	 */
	@Override
	public RequestReporterRemoteInterface getRequestReporter() throws RemoteException {
		return this.requestReporterStub;
	}

	/**
	 * @return remote object of the request
	 * @throws RemoteException 
	 */
	public RequestRemoteInterface getRemoteObject() {
		return this.remoteObject;
	}

	/**
	 * Notifies all listeners of changed state
	 * 
	 * @param changeResult
	 *            result that caused the change
	 */
	protected void notifyStateChangedListeners(STAFResult changeResult) {
		for (RequestStateChangedListener listener : this.listeners) {
			listener.notify(this, changeResult);
		}
		try {
			RequestPerformer.getRequestManagerStub().notify(getRid(), getOrigin_host(), getRequestState().name(), changeResult == null? -1 : changeResult.rc, changeResult == null? "" : changeResult.result);
		} catch (RemoteException e) {
			Logger.log(e.getMessage(), LogLevel.Error);
			System.exit(1);
		}
	}

	/**
	 * @param requestNotifier
	 * @throws RemoteException 
	 */
	public void setRequestNotifier(RequestNotifier requestNotifier) throws RemoteException {
		this.requestNotifier = requestNotifier;
		this.requestNotifierStub = (RequestNotifierRemoteInterface) UnicastRemoteObject.exportObject(this.requestNotifier, 0);
	}

	/**
	 * @param requestReporter
	 * @throws RemoteException 
	 */
	public void setRequestReporter(RequestReporter requestReporter) throws RemoteException {
		this.requestReporter = requestReporter;
		this.requestReporterStub = (RequestReporterRemoteInterface) UnicastRemoteObject.exportObject(this.requestReporter, 0);
	}

	@Override
	public RequestState getState() throws RemoteException {
		return this.getRequestState();
	}
	
	/**
	 * 
	 */
	public void terminateRMI(){
		//unexport all exported object to end the rmi thread
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			//
		} 
		if(this.requestNotifier != null) {
			try {
				UnicastRemoteObject.unexportObject(this.requestNotifier, true);
			} catch (NoSuchObjectException e) {
				//
			}
		}
		if(this.requestReporter != null) {
			try {
				UnicastRemoteObject.unexportObject(this.requestReporter, true);
			} catch (NoSuchObjectException e) {
				//
			}
		}
	}
}
