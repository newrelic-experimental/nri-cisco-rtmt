package com.newrelic.event.cisco.rtmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class Filters {

	private static Logger LOG = Logger.getLogger(Filters.class);

	private static List<GlobalFilter> globalFilters = new ArrayList<GlobalFilter>();
	
	private static HashMap<String, List<RTMTAgentFilter>> agentFilters = new HashMap<String, List<RTMTAgentFilter>>();
	
	private static HashMap<String, List<RTMTHostFilter>> hostFilters = new HashMap<String, List<RTMTHostFilter>>();
	
	public static void addGlobalFilter(GlobalFilter f) {
		globalFilters.add(f);
	}
	
	public static List<GlobalFilter> getGlobalFilters() {
		return globalFilters;
	}
	
	public static void addRTMTAgentFilter(String agent, RTMTAgentFilter f) {
		List<RTMTAgentFilter> list = agentFilters.get(agent);
		if(list == null) {
			list = new ArrayList<RTMTAgentFilter>();
		}
		list.add(f);
		agentFilters.put(agent, list);
	}
	
	public static List<RTMTAgentFilter> getAgentFilters(String agent) {
		List<RTMTAgentFilter> list = agentFilters.get(agent);
		if(list == null) list = new ArrayList<RTMTAgentFilter>();
		return list;
	}
	
	public static void addHostFilter(String host, RTMTHostFilter f) {
		List<RTMTHostFilter> list = hostFilters.get(host);
		if(list == null) {
			list = new ArrayList<RTMTHostFilter>();
		}
		list.add(f);
		hostFilters.put(host, list);
	}
	
	public static List<RTMTHostFilter> getHostFilters(String host) {
		List<RTMTHostFilter> list =  hostFilters.get(host);
		if(list == null) list = new ArrayList<RTMTHostFilter>();
		return list;
	}
	
	public static boolean haveFilters() {
		return !globalFilters.isEmpty() || !agentFilters.isEmpty() || !hostFilters.isEmpty();
	}

	public static void logFilters() {
		if(globalFilters.isEmpty()) {
			LOG.info("No global filters configured");
		} else {
			LOG.info("Will apply the following global filters");
			for(GlobalFilter f : globalFilters) {
				LOG.info("\t"+f);
			}
		}
		if(agentFilters.isEmpty()) {
			LOG.info("No agent filters configured");
		} else {
			LOG.info("Will apply the following agent filters");
			Set<String> keys = agentFilters.keySet();
			for(String key : keys) {
				List<RTMTAgentFilter> list = agentFilters.get(key);
				LOG.info("\tAgent Name: "+key);
				for(RTMTAgentFilter f : list) {
					LOG.info("\t\t"+f);
				}
			}
		}
		if(hostFilters.isEmpty()) {
			LOG.info("No host filters configured");
		} else {
			LOG.info("Will apply the following host filters");
			Set<String> keys = hostFilters.keySet();
			for(String key : keys) {
				List<RTMTHostFilter> list = hostFilters.get(key);
				LOG.info("\tHost Name: "+key);
				for(RTMTHostFilter f : list) {
					LOG.info("\t\t"+f);
				}
			}
		}
	}
}
