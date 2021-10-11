package com.newrelic.event.cisco.rtmt;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cisco.schemas.ast.soap.CounterInfoType;
import com.cisco.schemas.ast.soap.CounterNameType;
import com.cisco.schemas.ast.soap.CounterType;
import com.cisco.schemas.ast.soap.PerfmonAddCounterDocument;
import com.cisco.schemas.ast.soap.PerfmonAddCounterDocument.PerfmonAddCounter;
import com.cisco.schemas.ast.soap.PerfmonAddCounterResponseDocument;
import com.cisco.schemas.ast.soap.PerfmonAddCounterResponseDocument.PerfmonAddCounterResponse;
import com.cisco.schemas.ast.soap.PerfmonCloseSessionDocument;
import com.cisco.schemas.ast.soap.PerfmonCloseSessionDocument.PerfmonCloseSession;
import com.cisco.schemas.ast.soap.PerfmonCloseSessionResponseDocument;
import com.cisco.schemas.ast.soap.PerfmonCloseSessionResponseDocument.PerfmonCloseSessionResponse;
import com.cisco.schemas.ast.soap.PerfmonCollectSessionDataDocument;
import com.cisco.schemas.ast.soap.PerfmonCollectSessionDataDocument.PerfmonCollectSessionData;
import com.cisco.schemas.ast.soap.PerfmonCollectSessionDataResponseDocument;
import com.cisco.schemas.ast.soap.PerfmonCollectSessionDataResponseDocument.PerfmonCollectSessionDataResponse;
import com.cisco.schemas.ast.soap.PerfmonOpenSessionDocument;
import com.cisco.schemas.ast.soap.PerfmonOpenSessionDocument.PerfmonOpenSession;
import com.cisco.schemas.ast.soap.PerfmonOpenSessionResponseDocument;
import com.cisco.schemas.ast.soap.PerfmonOpenSessionResponseDocument.PerfmonOpenSessionResponse;
import com.cisco.schemas.ast.soap.RequestArrayOfCounterType;
import com.cisco.schemas.ast.soap.SessionHandleType;
import com.newrelic.soap.cisco.PerfmonServiceStub;

public class RTMTSession {
	
	private String sessionName;
	private String rtmtSessionName;
	private Set<String> counters;
	private String agentName;
	private String agentHost;
	private boolean isOpen = false;
	private String url;
	private boolean isSetup = false;
	
	public boolean isOpen() {
		return isOpen;
	}


	public RTMTSession(String aName,String aHost, String sn, String u) {
		sessionName = sn;
		counters = new HashSet<String>();
		url = u;
		agentName = aName;
		agentHost = aHost;
	}

	public void addCounter(String counter) {
		counters.add(counter);
	}
	
	public Set<String> getCounters() {
		return counters;
	}
	
	public void addCounters(Collection<String> counterCollection) {
		counters.addAll(counterCollection);
	}
	
	public void setupCounters() throws RemoteException {
		PerfmonServiceStub stub = Stubs.getStub(url);
		PerfmonAddCounterDocument inDoc = PerfmonAddCounterDocument.Factory.newInstance();
		
		PerfmonAddCounter addCounter = PerfmonAddCounter.Factory.newInstance();
		SessionHandleType sessionHandle = SessionHandleType.Factory.newInstance();
		sessionHandle.setStringValue(rtmtSessionName);
		addCounter.setSessionHandle(sessionHandle);
		
		List<CounterType> counterList = new ArrayList<CounterType>();
		for(String counterName : counters) {
			CounterType counterType = CounterType.Factory.newInstance();
			CounterNameType name = CounterNameType.Factory.newInstance();
			name.setStringValue(counterName);
			counterType.setName(name);
			counterList.add(counterType);
		}
		CounterType[] arrayOfCounters = new CounterType[counterList.size()];
		counterList.toArray(arrayOfCounters);
		RequestArrayOfCounterType requestArray = RequestArrayOfCounterType.Factory.newInstance();
		requestArray.setCounterArray(arrayOfCounters);
		addCounter.setArrayOfCounter(requestArray);
		inDoc.setPerfmonAddCounter(addCounter);

		PerfmonAddCounterResponseDocument responseDoc = stub.perfmonAddCounter(inDoc);
		PerfmonAddCounterResponse response = responseDoc.getPerfmonAddCounterResponse();
		if(response != null) {
			isSetup = true;
		}
	}
	
	public Map<String, Object> reportCounters() throws RemoteException {
		if(!isOpen) return null;

		if(!isSetup) {
			setupCounters();
		}
		
		HashMap<String, Object>  attributes = new HashMap<String, Object>();
		
		
		PerfmonServiceStub stub = Stubs.getStub(url);
		PerfmonCollectSessionDataDocument inDoc = PerfmonCollectSessionDataDocument.Factory.newInstance();
		PerfmonCollectSessionData collectData = PerfmonCollectSessionData.Factory.newInstance();
		SessionHandleType sessionHandle = SessionHandleType.Factory.newInstance();
		sessionHandle.setStringValue(rtmtSessionName);
		collectData.setSessionHandle(sessionHandle);
		inDoc.setPerfmonCollectSessionData(collectData);
		PerfmonCollectSessionDataResponseDocument responseDoc = stub.perfmonCollectSessionData(inDoc);
		
		PerfmonCollectSessionDataResponse response = responseDoc.getPerfmonCollectSessionDataResponse();
		CounterInfoType[] resultArray = response.getPerfmonCollectSessionDataReturnArray();
		if(resultArray != null) {
			for(CounterInfoType info : resultArray) {
				long cStatus = info.getCStatus();
				if(cStatus != 0 && cStatus != 1) {
					reset();
					return reportCounters();
				}
				CounterNameType nametype = info.getName();
				String name = null;
				if(nametype != null) {
					name = nametype.getStringValue();
				}
				long value = info.getValue();
				attributes.put(name, value);
				
			}
		}
		
		if(!attributes.isEmpty()) {
			attributes.put("Session", sessionName);
			attributes.put("RTMTSession", rtmtSessionName);
			attributes.put("AgentName", agentName);
			if(agentHost != null) {
				attributes.put("AgentHost", agentHost);
			}
			try {
				URL theUrl = new URL(url);
				if(theUrl != null) {
					String rtmtHostName = theUrl.getHost();
					if(rtmtHostName != null && !rtmtHostName.isEmpty()) {
						attributes.put("RTMT Hostname", rtmtHostName);
					}
					int port = theUrl.getPort();
					attributes.put("RTMT Port", port);
				}
			} catch (MalformedURLException e) {
			}
		}
		return attributes;
	}
	
	private void reset() throws RemoteException {
		isOpen = false;
		isSetup = false;
		closeSession();
		openSession();
		
	}
	
	public boolean removeCounter(String counter) {
		return counters.remove(counter);
	}
	
	public String getSessionName() {
		return sessionName;
	}
	
	public void closeSession() throws RemoteException {
		PerfmonServiceStub stub = Stubs.getStub(url);
		
		PerfmonCloseSessionDocument doc = PerfmonCloseSessionDocument.Factory.newInstance();
		PerfmonCloseSession closeSession = PerfmonCloseSession.Factory.newInstance();
		SessionHandleType handleType = SessionHandleType.Factory.newInstance();
		handleType.setStringValue(rtmtSessionName);
		doc.setPerfmonCloseSession(closeSession);
		
		PerfmonCloseSessionResponseDocument responseDoc = stub.perfmonCloseSession(doc);
		PerfmonCloseSessionResponse response = responseDoc.getPerfmonCloseSessionResponse();
		
		if(response != null) {
			rtmtSessionName = null;
			isOpen = false;
		}
	}
	
	public void openSession() throws RemoteException {
		PerfmonServiceStub stub = Stubs.getStub(url);
		PerfmonOpenSessionDocument doc = PerfmonOpenSessionDocument.Factory.newInstance();
		PerfmonOpenSession openSession = PerfmonOpenSession.Factory.newInstance();
		
		doc.setPerfmonOpenSession(openSession);
		
		PerfmonOpenSessionResponseDocument responseDoc = stub.perfmonOpenSession(doc);
		PerfmonOpenSessionResponse response = responseDoc.getPerfmonOpenSessionResponse();
		
		if(response != null) {
			SessionHandleType sessionReturn = response.getPerfmonOpenSessionReturn();
			if(sessionReturn != null) {
				rtmtSessionName = sessionReturn.getStringValue();
				isOpen = true;
			}
		}
		
	}
}
