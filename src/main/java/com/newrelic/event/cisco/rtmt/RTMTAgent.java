package com.newrelic.event.cisco.rtmt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class RTMTAgent {

	private RTMTClient client = null;
	private List<String> hosts = new ArrayList<String>();
	private Set<RTMTSession> sessions = new HashSet<RTMTSession>();
	private static Logger LOG = Logger.getLogger(RTMTAgent.class);
	
	private String agentName = null;
	private String agentHost = null;

	public RTMTAgent(String aName,String url, List<String> hostList) {
		agentName = aName;
		hosts = hostList;
		client =new RTMTClient(url);
		for(String host : hosts) {
			client.addHost(host);
		}
		try {
			agentHost = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOG.error("Failed to get host name due to error", e);
		}
		
	}

	public RTMTClient getClient() {
		return client;
	}
	
	public List<String> getHosts() {
		return hosts;
	}
	
	public Set<RTMTSession> getSessions() {
		return sessions;
	}

	
	public String getAgentName() {
		return agentName;
	}

	public String getAgentHost() {
		return agentHost;
	}

	public void collectCountersAndReport() {
		 client.populate(agentName,agentHost);
		 
	}

	public void addSession(RTMTSession session) {
		sessions.add(session);
	}
	
	public String getURL() {
		return client.getURL();
	}
}
