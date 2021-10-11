package com.newrelic.event.cisco.rtmt;

import java.util.ArrayList;
import java.util.List;

public abstract class Filter {
	
	protected FilterScope scope;
	
	protected boolean isExclude = true;
	
	protected String objectName = null;
	
	protected List<String> instances = new ArrayList<String>();

	
	protected Filter(String oName) {
		objectName = oName;
	}
}
