package com.newrelic.event.cisco.rtmt;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class RTMTAgentFactory {
	
	private static Logger LOG = Logger.getLogger(RTMTAgentFactory.class);

	@SuppressWarnings("unchecked")
	public RTMTAgent createAgent(Map<String, Object> agentProperties) throws Exception {
		String url = (String)agentProperties.get("url");
		LOG.info("Creating agent for url: "+url);
		String hostString = (String)agentProperties.get("hosts");
		List<String> hosts = new ArrayList<String>();
		String agentName = (String) agentProperties.get("name");
		String agentHost = null;
		try {
			agentHost = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			LOG.error("Failed to get host name due to error", e);
		}
		
		LinkedHashMap<?,?> security = (LinkedHashMap<?, ?>) agentProperties.get("security");
		if(security != null) {
			String uName = (String)security.get("username");
			String pass = (String)security.get("password");
			Authentication auth = new Authentication(uName, pass);
			Stubs.addAuthentication(url, auth);
			LOG.debug("adding username "+uName+" and password "+pass+" for url "+url);
		} else {
			LOG.debug("No security object found for url "+url);
		}
		
		StringTokenizer st = new StringTokenizer(hostString, ",");
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			hosts.add(token);
		}
		RTMTAgent agent = new RTMTAgent(agentName,url,hosts);
		
		ArrayList<Object> sessions = (ArrayList<Object>) agentProperties.get("sessions");
		if(sessions == null) {
			LOG.debug("Sessions not set");
		} else {
			int size = sessions.size();
			LOG.debug("Found "+size+" sessions");
			if(size > 0) {
				for(int i=0;i<size;i++) {
					Object object = sessions.get(i);
					LinkedHashMap<?,?> map = (LinkedHashMap<?, ?>)object;
					String sessionName = (String)map.get("name");
					ArrayList<String> sessionCounters = (ArrayList<String>)map.get("counters");
					RTMTSession rtmtSession = new RTMTSession(agentName,agentHost,sessionName,url);
					rtmtSession.openSession();
					rtmtSession.addCounters(sessionCounters);
					agent.addSession(rtmtSession);
				}
			}
		}
		return agent;
	}

}
