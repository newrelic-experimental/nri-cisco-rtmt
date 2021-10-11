package com.newrelic.event.cisco.rtmt;

import java.util.ArrayList;
import java.util.List;

public class ObjectName {

	private String objectName = null;
	private boolean multiInstance;
	private List<String> instances = new ArrayList<String>();
	
	public ObjectName(String name) {
		this(name,false);
	}
	
	public ObjectName(String name, boolean multi) {
		objectName = name;
		multiInstance = multi;
	}
	
	public void addInstance(String instance) {
		if(!multiInstance) multiInstance = true;
		instances.add(instance);
	}
	
	public int getNumberOfInstances() {
		if(multiInstance) return instances.size();
		return -1;
	}
	
	public boolean isMultiInstance() {
		return multiInstance;
	}
  	
	public List<String> getInstances() {
		return instances;
	}
	
	public String getObjectName() {
		return objectName;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ObjectName) {
			ObjectName objectName2 = (ObjectName)obj;
			if(objectName == objectName2.objectName)  {
				if(multiInstance == objectName2.multiInstance) {
					return true;
				}
			}
				
		}
		
		return false;
	}
	
	
}
