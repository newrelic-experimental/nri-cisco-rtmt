package com.newrelic.event.cisco.rtmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Runner implements Runnable {

	private List<RTMTAgent> agents = null;
	private String accountID = null;
	private String licenseKey = null;
	private static Logger LOG = Logger.getLogger(Runner.class);

	public Runner(String acct, String license, boolean useEU) {
		agents = new ArrayList<RTMTAgent>();
		accountID = acct;
		licenseKey = license;
		if(!Report.initialized) {
			Report.initialize(accountID, licenseKey, useEU);
		}
	}

	public void addAgent(RTMTAgent agent) {
		agents.add(agent);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {

		ExecutorService executor = Executors.newFixedThreadPool(agents.size());


		for(RTMTAgent agent : agents) {
			executor.submit(() -> {
				CloseableHttpClient httpClient = HttpClients.createDefault();
				try {

					agent.collectCountersAndReport();


					Set<RTMTSession> sessions = agent.getSessions();
					JSONArray reportJson2 = new JSONArray();
					for(RTMTSession session : sessions) {
						Map<String, Object> attributes2 = session.reportCounters();
						attributes2.put("eventType", "RTMTSession");
						JSONObject json2 = new JSONObject(attributes2);
						reportJson2.add(json2);
					}
					Report.getInstance().post(reportJson2, httpClient);
				} catch (Exception e) {
					String msg = "Error getting metrics from agent " + agent.getAgentName() + " running on " + agent.getAgentHost();
					LOG.error(msg, e);
				}
			});
		}
		
		executor.shutdown();
		
	}

}
