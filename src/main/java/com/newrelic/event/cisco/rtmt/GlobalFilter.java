package com.newrelic.event.cisco.rtmt;

public class GlobalFilter extends Filter {

	public GlobalFilter(String oName) {
		super(oName);
		scope = FilterScope.GLOBAL;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Scope: Global, Object Name: ");
		sb.append(objectName);
		sb.append(", ");
		if(isExclude) {
			sb.append("Type: Exclude, ");
		} else {
			sb.append("Type: Include, ");
		}
		if(counters == null || counters.isEmpty()) {
			sb.append("Counters: None");
		} else {
			sb.append("Counters: ");
			int i = 0;
			for(String counter : counters) {
				sb.append(counter);
				if(i < counters.size()-1)
					sb.append(',');
			}
		}
		if(instances == null || instances.isEmpty()) {
			sb.append("Instances: None");
		} else {
			sb.append("Instances: ");
			int i = 0;
			for(String instance : instances) {
				sb.append(instance);
				if(i < instances.size()-1)
					sb.append(',');
			}
		}
		return sb.toString();
	}
	
	
}
