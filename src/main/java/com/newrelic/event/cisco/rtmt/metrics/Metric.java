package com.newrelic.event.cisco.rtmt.metrics;

public interface Metric {

	public SourceType getSourceType();
	
	public String getName();
	
	public Object getValue();

}
