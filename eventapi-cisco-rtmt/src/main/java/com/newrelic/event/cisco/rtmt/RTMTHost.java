package com.newrelic.event.cisco.rtmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class RTMTHost {
	
	private String host;
	private List<ObjectName> objectNames = new ArrayList<ObjectName>();
	private HashMap<String, HashSet<String>> counters = new HashMap<String, HashSet<String>>();
	
	
	public RTMTHost(String h) {
		host = h;
	}

	public String getHost() {
		return host;
	}

	public void addObjectName(ObjectName objName) {
		objectNames.add(objName);
	}
	
	public void addCounters(ObjectName objName, HashSet<String> objCounters) throws RTMTException {
		if(!objectNames.contains(objName)) throw new RTMTException("Trying to add counters for object name that has not been added.  Object name: "+objName);
		counters.put(objName.getObjectName(), objCounters);
	}
	
	public List<ObjectName> getObjectNames() {
		return objectNames;
	}
	
	public HashSet<String> getCounters(String objName) {
		return counters.get(objName);
	}
}
