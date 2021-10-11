package com.newrelic.event.cisco.rtmt;

public class RTMTAgentFilter extends Filter {
	
	protected String agentName;

	protected RTMTAgentFilter(String aName,String oName) {
		super(oName);
		agentName = aName;
		scope = FilterScope.RTMT_AGENT;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Scope: RTMT Agent, ");
		sb.append("Agent Name: ");
		sb.append(agentName);
		sb.append(", Object Name: ");
		sb.append(objectName);
		sb.append(", ");
		if(isExclude) {
			sb.append("Type: Exclude, ");
		} else {
			sb.append("Type: Include, ");
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
