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
package com.ibm.db2wkl.request.internal;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.db2wkl.DB2WorkloadService;
import com.ibm.db2wkl.helper.RequestComparator;
import com.ibm.db2wkl.helper.StringUtility;
import com.ibm.db2wkl.request.ARequest;
import com.ibm.db2wkl.request.RequestManager;
import com.ibm.db2wkl.request.RequestState;
import com.ibm.db2wkl.request.RequestType;
import com.ibm.db2wkl.request.parser.Options;
import com.ibm.db2wkl.rmi.RequestNotifierRemoteInterface;
import com.ibm.db2wkl.rmi.RequestRemoteInterface;
import com.ibm.staf.STAFMapClassDefinition;
import com.ibm.staf.STAFMarshallingContext;
import com.ibm.staf.STAFResult;

/**
 * 
 */
public class RequestHandler extends InternalRequest {

	/**
	 * table definition for the return of the list action
	 */
	private STAFMapClassDefinition listDetail_listCategoryMapClassDefinition;

	/**
	 * table definition for the return of the list action
	 */
	private STAFMapClassDefinition listMapClassDefinition;

	/**
	 * table definition for the return of the list action
	 */
	private STAFMapClassDefinition requestIDMapClassDefinition;

	/**
	 * The normal text for the output
	 */
	ArrayList<ResultObject> resultObjectList = new ArrayList<ResultObject>();
	
	/**
	 * staf local db2wkl request
	 * 1: list 
	 * 2: list detail || list executing/stopped/brokedown/finished/stored
	 * 3: requestID
	 * 4: stop
	 */
	int requestInput;	
	
	/**
	 * Date format
	 */
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * default constructor
	 */
	public RequestHandler() {

		/*
		 * build the table definition for the return of the list detail action
		 */
		this.listDetail_listCategoryMapClassDefinition = new STAFMapClassDefinition("STAF/DB2WKL/Request/List/Detail");
		this.listDetail_listCategoryMapClassDefinition.addKey("requestID", "RequestID");
		this.listDetail_listCategoryMapClassDefinition.addKey("status", "Status");
		this.listDetail_listCategoryMapClassDefinition.addKey("start_time", "Start Time");
		this.listDetail_listCategoryMapClassDefinition.addKey("end_time", "End Time");
		this.listDetail_listCategoryMapClassDefinition.addKey("type", "Type");

		/*
		 * build the table definition for the return of the list action
		 */
		this.listMapClassDefinition = new STAFMapClassDefinition("STAF/DB2WKL/Request/List");
		this.listMapClassDefinition.addKey("type", "Type");
		this.listMapClassDefinition.addKey("number", "#");

		/*
		 * build the table definition for the return of the details of the
		 * requestID
		 */
		this.requestIDMapClassDefinition = new STAFMapClassDefinition("STAF/DB2WKL/Request");
		this.requestIDMapClassDefinition.addKey("requestID", "RequestID");
		this.requestIDMapClassDefinition.addKey("wklID", "WklID");
		this.requestIDMapClassDefinition.addKey("status", "Status");
		this.requestIDMapClassDefinition.addKey("start_time", "Start Time");
		this.requestIDMapClassDefinition.addKey("end_time", "End Time");
		this.requestIDMapClassDefinition.addKey("type", "Type");
		this.requestIDMapClassDefinition.addKey("message", "Message");
		this.requestIDMapClassDefinition.addKey("list_of_workloads", "List of workloads");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.db2wkl.receiver.ASubRequest#acceptRequest
	 * (com.ibm.staf.STAFHandle,
	 * com.ibm.staf.service.STAFServiceInterfaceLevel30.RequestInfo,
	 * com.ibm.staf.service.STAFCommandParseResult)
	 */
	@SuppressWarnings( { "unchecked" })
	@Override
	public STAFResult execute() {

		// create the result map and the marshaller that converts the map into a
		// STAF result object
		STAFMarshallingContext mc = new STAFMarshallingContext();
		Map resultMap = null;
		List resultList = new ArrayList();
		ResultObject resultObject;
		
		int stored = RequestManager.getInstance().getRequestsOnHold().size();
		int finished = RequestManager.getInstance().getFinishedRequests().size();
		int brokedown = RequestManager.getInstance().getBrokedownRequests().size();
		int running = RequestManager.getInstance().getOriginatedRequests().size();
		int total = RequestManager.getInstance().getRequests().size();
		int stopped = RequestManager.getInstance().getStoppedRequests().size();
		int internal = RequestManager.getInstance().getInternalRequests().size();
		
		String req_value = getRequestOption(Options.REQ_REQUEST);
		Long req_id = null;
		if(!req_value.equals("")){
			try {
				req_id = Long.valueOf(req_value);
			} catch(NumberFormatException e){
				return new STAFResult(STAFResult.JavaError, e.getMessage());
			}
		}
		
		if(req_id != null && !RequestManager.getInstance().getRequests().containsKey(DB2WorkloadService.getLOCALHOST(), req_id)){
			return new STAFResult(STAFResult.InvalidRequestString, "The specified request " + req_value + " doesn't exist.");
		}
		
		/*
		 * list
		 */
		if (req_id == null && hasRequestOption(Options.REQ_LIST) && !hasRequestOption(Options.REQ_DETAIL)) {
			mc.setMapClassDefinition(this.listMapClassDefinition);
			this.requestInput = 1;
				
			resultMap = this.listMapClassDefinition.createInstance();
			resultMap.put("type", "Stored");
			resultMap.put("number", Integer.toString(stored));
			resultObject = new ResultObject("Stored", stored);
			this.resultObjectList.add(resultObject);
			resultList.add(resultMap);

			resultMap = this.listMapClassDefinition.createInstance();
			resultMap.put("type", "Internal");
			resultMap.put("number", Integer.toString(internal));
			resultObject = new ResultObject("Internal", internal);
			this.resultObjectList.add(resultObject);
			resultList.add(resultMap);

			resultMap = this.listMapClassDefinition.createInstance();
			resultMap.put("type", "Executing");
			resultMap.put("number", Integer.toString(running));
			resultObject = new ResultObject("Executing", running);
			this.resultObjectList.add(resultObject);
			resultList.add(resultMap);

			resultMap = this.listMapClassDefinition.createInstance();
			resultMap.put("type", "Finished");
			resultMap.put("number", Integer.toString(finished));
			resultObject = new ResultObject("Finished", finished);
			this.resultObjectList.add(resultObject);
			resultList.add(resultMap);

			resultMap = this.listMapClassDefinition.createInstance();
			resultMap.put("type", "Brokedown");
			resultMap.put("number", Integer.toString(brokedown));
			resultObject = new ResultObject("Brokedown", brokedown);
			this.resultObjectList.add(resultObject);
			resultList.add(resultMap);
			
			resultMap = this.listMapClassDefinition.createInstance();
			resultMap.put("type", "Stopped");
			resultMap.put("number", Integer.toString(stopped));
			resultObject = new ResultObject("Stopped", stopped);
			this.resultObjectList.add(resultObject);
			resultList.add(resultMap);

			resultMap = this.listMapClassDefinition.createInstance();
			resultMap.put("type", "Total");
			resultMap.put("number", Integer.toString(total));
			resultObject = new ResultObject("Total", total);
			this.resultObjectList.add(resultObject);
			resultList.add(resultMap);

			mc.setRootObject(resultList);
			
			return new STAFResult(STAFResult.Ok, mc.marshall());
		}
		
		/*
		 * list detail
		 */
		else if (req_id == null && hasRequestOption(Options.REQ_LIST) && hasRequestOption(Options.REQ_DETAIL)) {
			mc.setMapClassDefinition(this.listDetail_listCategoryMapClassDefinition);
			this.requestInput = 2;  
			
			List<ARequest> reqList = new ArrayList<ARequest>(RequestManager.getInstance().getRequests().get(DB2WorkloadService.getLOCALHOST()).values());
			
			Collections.sort(reqList, new RequestComparator());
			
			for (ARequest request : reqList) {
				if (request.getType() != RequestType.INTERNAL) {
					resultMap = putMapForListDetailAndListCategory(request);					
					resultList.add(resultMap);	
				}
			}
			
			mc.setRootObject(resultList);

			return new STAFResult(STAFResult.Ok, mc.marshall());
		}
		
		/*
		 * give the count of the RequestType back
		 */
		else if (req_id == null && hasRequestOption(Options.REQ_COUNT)){
			int count = 0;
			if(hasRequestOption(Options.REQ_STORED)) {
				count = stored;
				
			} else if(hasRequestOption(Options.REQ_EXECUTING)){
				count = running;
				
			} else if(hasRequestOption(Options.REQ_FINISHED)){
				count = finished;
				
			} else if(hasRequestOption(Options.REQ_BROKEDOWN)){
				count = brokedown;
				
			} else if(hasRequestOption(Options.REQ_STOPPED)){
				count = stopped;
			} else {
				count = total;
			}
			return new STAFResult(STAFResult.Ok, String.valueOf(count));
		}
		
		/*
		 * clean
		 */
		else if (req_id == null && hasRequestOption(Options.REQ_CLEAN)) {
			RequestManager.getInstance().clean();
			return new STAFResult(STAFResult.Ok);
		}
		
		/*
		 * Details about the Request with the requestID
		 */
		else if (req_id != null && !hasRequestOption(Options.REQ_STOP) && !hasRequestOption(Options.REQ_START)) {
			mc.setMapClassDefinition(this.requestIDMapClassDefinition);

			this.requestInput = 3;

			ARequest request = RequestManager.getInstance().getRequests().get(DB2WorkloadService.getLOCALHOST(), req_id);
			RequestRemoteInterface theRequest = RequestManager.getInstance().getOriginatedRequests().get(req_id);
			
			resultMap = this.requestIDMapClassDefinition.createInstance();

			this.sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String start_time_string = this.sdf.format(request.getStartTime());
			String end_time_string = "non-completed";
			
			resultMap.put("requestID", req_id.toString());
			
			if(theRequest == null){
				if(RequestManager.getInstance().getBrokedownRequests().contains(request)){
					resultMap.put("status", RequestState.BROKEDOWN.name());
				} else if(RequestManager.getInstance().getFinishedRequests().contains(request)){
					resultMap.put("status", RequestState.FINISHED.name());
				} else if(RequestManager.getInstance().getStoppedRequests().contains(request)){
					resultMap.put("status", RequestState.STOPPED.name());
				} else {
					resultMap.put("status", "unknown");
				}
			} else {
				try {
					resultMap.put("status", theRequest.getState().toString());
					resultMap.put("list_of_workloads", "as following");
				} catch (RemoteException e) {
					try {
						RequestManager.getInstance().notify(request.getRid(), request.getOrigin_host(), RequestState.BROKEDOWN.name(), STAFResult.CommunicationError, e.getMessage());
					} catch (RemoteException e1) {
						//local calls won't get the exception
					}
				}
			}
			
			resultMap.put("start_time", start_time_string);
			if (request.getEndTime() == null) {
				resultMap.put("end_time", end_time_string);
			} else { // the end time is only for workloads which are finished, stopped or brokendown
				end_time_string = this.sdf.format(request.getEndTime());
				resultMap.put("end_time", end_time_string);
			}
			resultMap.put("message", request.getResult() == null ? "" : request.getResult().result);
			resultMap.put("type", request.getType().toString());
			
			resultList.add(resultMap);
			
			if(theRequest != null) {
				try {
					ArrayList<HashMap<String, String>> workloads = theRequest.getWorkloadInfo();
					for(HashMap<String, String> workload : workloads){
						resultMap = this.requestIDMapClassDefinition.createInstance();
						resultMap.putAll(workload);
						resultList.add(resultMap);
					}	
				} catch (RemoteException e) {
					try {
						RequestManager.getInstance().notify(request.getRid(), request.getOrigin_host(), RequestState.BROKEDOWN.name(), STAFResult.CommunicationError, e.getMessage());
					} catch (RemoteException e1) {
						//local calls won't get the exception
					}
				}
			}
		
			mc.setRootObject(resultList);
			
			return new STAFResult(STAFResult.Ok, mc.marshall());
		
		}

		/*
		 * start an stored request that was put on hold
		 */
		else if(req_id != null && hasRequestOption(Options.REQ_START)) {
			
			return RequestManager.getInstance().startQueuedRequests(req_id);
		}
		
		/*
		 * stop the Request with the requestID
		 */
		else if (req_id != null && hasRequestOption(Options.REQ_STOP)) {
					
			ARequest request = RequestManager.getInstance().getRequests().get(DB2WorkloadService.getLOCALHOST(), req_id);
			
			/*
			 * stopping process of an QueuedRequest
			 */
			if(RequestManager.getInstance().getRequestsOnHold().contains(request)) {
				return RequestManager.getInstance().cancelQueuedRequest(req_id);
			}
			
			else if(RequestManager.getInstance().getOriginatedRequests().containsKey(req_id)){
				RequestRemoteInterface theRequest = RequestManager.getInstance().getOriginatedRequests().get(req_id);
				try {
					RequestNotifierRemoteInterface rNotifier = theRequest.getRequestNotifier();
					rNotifier.setStop(true);
				} catch (RemoteException e) {
					try {
						RequestManager.getInstance().notify(request.getRid(), request.getOrigin_host(), RequestState.BROKEDOWN.name(), STAFResult.CommunicationError, e.getMessage());
					} catch (RemoteException e1) {
						//local calls won't get the exception
					}
				}
				return new STAFResult(STAFResult.Ok, "Request " + req_id + " has been stopped." );
			} else {
				return new STAFResult(STAFResult.JavaError, "The Request " + req_id + " is not a stoppable request.");
			}			
		}
		/*
		 * show all requests and their status which are in the category executing/stopped/brokedown/finished/stored
		 */
/*		if(list > 0 && (count_executing > 0 || count_finished > 0 || count_brokedown > 0 || count_stopped > 0 || count_stored > 0 || count_internal == 0 || count_unknown == 0) && list_detail == 0 && count == 0){
				mc.setMapClassDefinition(this.listDetail_listCategoryMapClassDefinition);
				this.requestInput = 2;
				ArrayList<ARequest> requestList = new ArrayList<ARequest>();
				
				// search the right request list
				if(count_executing > 0){
			//		requestList = RequestManager.getInstance().getRunningRequests();
				}if(count_finished > 0){
			//		requestList = RequestManager.getInstance().getFinishedRequests();
				}if(count_brokedown > 0){
			//		requestList = RequestManager.getInstance().getBrokedownRequests();
				}if(count_stopped > 0){
			//		requestList = RequestManager.getInstance().getStoppedRequests();
				}if(count_unknown > 0){
			//		requestList = RequestManager.getInstance().getUnknownRequests();
				}if(count_internal > 0){
			//		requestList = RequestManager.getInstance().getInternalRequests();
				}if(count_stored > 0 ){ //count_stored
					ConcurrentLinkedQueue<ARequest> storedRequests = RequestManager.getInstance().getRequestsOnHold();
					for(ARequest storedRequest : storedRequests){
						requestList.add(storedRequest);
					}
				}
				
				
				if(requestList.isEmpty()){
					return new STAFResult(STAFResult.Ok, "There isn't any running request.");
				}
					
				for(ARequest request : requestList){		
					resultMap = putMapForListDetailAndListCategory(request);
					resultList.add(resultMap);		
				}
				
				mc.setRootObject(resultList);
				
				return new STAFResult(STAFResult.Ok, mc.marshall());
		
			}
*/
		return new STAFResult(STAFResult.InvalidRequestString, "Unknown request. Check the syntax for the correct request format.");
	}
	
	/**
	 * put the information of the request in a map 
	 * but for only options like list detail and list executing/stopped/brokedown/finished/stored which has the same map definition
	 * 
	 * @param request
	 * @return Map with Request informations
	 */
	@SuppressWarnings({ "unchecked" })
	private Map putMapForListDetailAndListCategory(ARequest request) {
		
		Map resultMap = this.listDetail_listCategoryMapClassDefinition.createInstance();
		
		String start_time_string = this.sdf.format(request.getStartTime());
		String end_time_string = "non-completed";
		
		resultMap.put("requestID", String.valueOf(request.getRid()));
		RequestRemoteInterface theRequest = RequestManager.getInstance().getOriginatedRequests().get(request.getRid());
		if(theRequest != null) {
			try {
				resultMap.put("status", theRequest.getState().toString());
			} catch (RemoteException e) {
				try {
					RequestManager.getInstance().notify(request.getRid(), request.getOrigin_host(), RequestState.BROKEDOWN.name(), STAFResult.CommunicationError, e.getMessage());
				} catch (RemoteException e1) {
					//local calls won't get the exception
				}
			}
		} else {
			if(RequestManager.getInstance().getBrokedownRequests().contains(request)){
				resultMap.put("status", RequestState.BROKEDOWN.name());
			} else if(RequestManager.getInstance().getFinishedRequests().contains(request)){
				resultMap.put("status", RequestState.FINISHED.name());
			} else if(RequestManager.getInstance().getStoppedRequests().contains(request)){
				resultMap.put("status", RequestState.STOPPED.name());
			} else {
				resultMap.put("status", "unknown");
			}
		}
		resultMap.put("start_time", start_time_string);
		if (request.getEndTime() == null) {
			resultMap.put("end_time", end_time_string);
		} else { // the end time is only for workloads which are finished, abend or brokedown
			end_time_string = this.sdf.format(request.getEndTime());
			resultMap.put("end_time", end_time_string);
		}
		resultMap.put("type", request.getType().toString());
		
		ResultObject resultObject = new ResultObject(String.valueOf(request.getRid()), "", start_time_string, end_time_string, request.getType().toString());
		this.resultObjectList.add(resultObject);
		
		return resultMap;
	}

	
	/**
	 * @return text output
	 */
	@Override
	public String getText() {
		// create the string builder that will hold the result
		StringBuilder builder = new StringBuilder();
		
		if(this.getResult().rc == 0){
	
			ArrayList<ResultObject> list = new ArrayList<ResultObject>();
			
			if(this.resultObjectList.size() == 0){
				builder.append(this.getResult().result);
			}
			
			/*
			 * search all resultObject which are for list/ list detail/ rID / runningList and add it to an ArrayList
			 */
			for(int i = 0 ; i<this.resultObjectList.size(); i++){
				// request list
				if(this.requestInput == 1){
					if(!(Integer.toString(this.resultObjectList.get(i).getNumber()) == null)){
						list.add(this.resultObjectList.get(i));
					}
				}
				// request list detail || list executing/stopped/brokedown/finished/stored
				if(this.requestInput == 2){
					if(!(this.resultObjectList.get(i).getRequestID() == null)){
						list.add(this.resultObjectList.get(i));
					}
				}
				// request requestID
				if(this.requestInput == 3){
					if(!(this.resultObjectList.get(i).getRequestID() == null)){
						list.add(this.resultObjectList.get(i));
					}
				}	
				// requestID stop
				if(this.requestInput == 4){
					builder.append(this.getResult().result);
				}
			}
			
			/*
			 * Append all ResultObject in the ArrayList to the StringBuilder
			 */
			for(int j = 0; j<list.size(); j++){
				// request list
				if(this.requestInput == 1){
					if(!(Integer.toString(list.get(j).getNumber()) == null)){
						ResultObject last_resultObject = list.get(list.size()-1);
						if(list.get(j) == last_resultObject){
							builder.append(list.get(j).getType() + " : "+ list.get(j).getNumber());
						}else{
							builder.append(list.get(j).getType() + " : "+ list.get(j).getNumber() + ", ");
						}
					}
				}
				// request list detail || list executing/stopped/brokedown/finished/stored
				if(this.requestInput == 2){
					if(!(this.resultObjectList.get(j).getRequestID() == null)){
						ResultObject last_resultObject = list.get(list.size()-1);
						if(list.get(j) == last_resultObject){
							builder.append("RequestID : " + list.get(j).getRequestID() + " , Status : " + list.get(j).getStatus() + " , Start Time : " + list.get(j).getStart_time() + " , End Time : " + list.get(j).getEnd_time() + " , Type : " + list.get(j).getType()+ " , Message : " + StringUtility.replaceIfNull(list.get(j).getMessage(), " - "));
						}else{
							builder.append("RequestID : " + list.get(j).getRequestID() + " , Status : " + list.get(j).getStatus() + " , Start Time : " + list.get(j).getStart_time() + " , End Time : " + list.get(j).getEnd_time() + " , Type : " + list.get(j).getType() + " , Message : " + StringUtility.replaceIfNull(list.get(j).getMessage(), " - ") + " \n\n");
						}
						
					}
				}
				// request requestID 
				if(this.requestInput == 3){
					if(!(this.resultObjectList.get(j).getRequestID() == null)){
						ResultObject last_resultObject = list.get(list.size()-1);
						if(list.get(j) == last_resultObject){
							builder.append("RequestID : " + list.get(j).getRequestID() + " , WklID : " + list.get(j).getWklID() + " , Status : " + list.get(j).getStatus() + " , Start Time : " + list.get(j).getStart_time() + " , End Time : " + list.get(j).getEnd_time() + " , Type : " + list.get(j).getType() + " , Message : " + StringUtility.replaceIfNull(list.get(j).getMessage(), " - ") + " , List of Workloads : " + list.get(j).getList_of_workloads());
						}else{
							builder.append("RequestID : " + list.get(j).getRequestID() + " , WklID : " + list.get(j).getWklID() + " , Status : " + list.get(j).getStatus() + " , Start Time : " + list.get(j).getStart_time() + " , End Time : " + list.get(j).getEnd_time() + " , Type : " + list.get(j).getType() + " , Message : " + StringUtility.replaceIfNull(list.get(j).getMessage(), " - ") + " , List of Workloads : " + list.get(j).getList_of_workloads() + " \n\n");
						}
					}
				}
			}
		}else{
			builder.append(this.getResult().result);
		}
		
		//clearing the result object list
		this.resultObjectList.clear();
	
		return builder.toString();
	}

	
	/**
	 * @return xml output
	 */
	@Override
	public Document getXML() {
		try {
			// create the document
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();
			
			// create the document root node
			Element root = document.createElement("db2wklresult");
			document.appendChild(root);
			
			// create the result information
			Element rc = document.createElement("rc");
			rc.setTextContent(Integer.toString(this.getResult().rc));
			root.appendChild(rc);
			
			Element object = document.createElement("resultObject");
			object.setTextContent(this.getResult().resultObj == null ? "" : this.getResult().resultObj.toString());	
			
			Element message = null;

			if(this.resultObjectList.size() != 0){
				for(int i = 0 ; i<this.resultObjectList.size(); i++){
					// request list
					if(this.requestInput == 1){
						if(!(Integer.toString(this.resultObjectList.get(i).getNumber()) == null)){
							message = document.createElement(this.resultObjectList.get(i).getType().toLowerCase());
							message.setTextContent(String.valueOf(this.resultObjectList.get(i).getNumber()));
							// add the result info object - nodes
							object.appendChild(message);
						}
					}
					// request list detail || request list executing/stopped/brokedown/finished/stored
					if(this.requestInput == 2){ 
						if(!(this.resultObjectList.get(i).getRequestID() == null)){
							message = document.createElement("RequestID".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getRequestID());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("Status".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getStatus());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("Start_Time".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getStart_time());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("End_Time".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getEnd_time());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("Type".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getType());
							// add the result info object - nodes
							object.appendChild(message);
						}
					}
					// request requestID
					if(this.requestInput == 3){ 
						if(!(this.resultObjectList.get(i).getRequestID() == null)){
							message = document.createElement("RequestID".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getRequestID());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("WklID".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getWklID());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("Status".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getStatus());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("Start_Time".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getStart_time());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("End_Time".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getEnd_time());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("Type".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getType());
							// add the result info object - nodes
							object.appendChild(message);
							message = document.createElement("List_of_Workloads".toLowerCase());
							message.setTextContent(this.resultObjectList.get(i).getList_of_workloads());
							// add the result info object - nodes
							object.appendChild(message);
						}
					}
					// stop
					if(this.requestInput == 4){
						message = document.createElement("stopping");
						message.setTextContent(this.resultObjectList.get(i).getRequestID());
						object.appendChild(message);
					}
				}
			}	else{
				message = document.createElement("count");
				message.setTextContent(this.getResult().result);
				object.appendChild(message);				
			}
					
			// add the result info nodes
			root.appendChild(object);
			
			// return the resulting document
			return document;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
