package com.newrelic.event.cisco.rtmt;

public class RTMTHostFilter extends Filter {
	
	protected String hostName = null;
	
	protected RTMTHostFilter(String h, String oName) {
		super(oName);
		hostName = h;
		scope = FilterScope.RTMT_HOST;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Scope: RTMT Host, ");
		sb.append("Host Name: ");
		sb.append(hostName);
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
