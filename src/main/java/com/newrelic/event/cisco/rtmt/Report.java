package com.newrelic.event.cisco.rtmt;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;

public class Report {
	
	private static Logger LOG = Logger.getLogger(Report.class);
	
	private String uri = null;
	private String licenseKey = null;

	private static Report instance = null;
	public static boolean initialized = false;
	
	private Report(String accountNumber, String key, boolean useEU) {
		if(useEU) {
			uri = "https://insights-collector.eu01.nr-data.net/v1/accounts/" +accountNumber+"/events";
		} else {
			uri = "https://insights-collector.newrelic.com/v1/accounts/" +accountNumber+"/events";
		}
		licenseKey = key;
	}
	
	public static Report getInstance() {
		return instance;
	}
	
	public static void initialize(String acctNum, String lKey, boolean useEU) {
		initialized = true;
		instance = new Report(acctNum, lKey, useEU);
	}
	
	public void post(JSONArray json, CloseableHttpClient httpClient) throws ClientProtocolException, IOException {
		HttpPost post = new HttpPost(uri);
		post.addHeader("Content-Type", "application/json");
		post.addHeader("Api-Key", licenseKey);
		post.addHeader("Content-Encoding", "gzip");
		
		HttpEntity entity = new StringEntity(json.toJSONString());
		GzipCompressingEntity gzippedEntity = new GzipCompressingEntity(entity);
		post.setEntity(gzippedEntity);
		
		LOG.debug("Call to send data");
		CloseableHttpResponse response = httpClient.execute(post);
		LOG.debug("Data sent");
		int code = response.getStatusLine().getStatusCode();
		if(code != 200) {
			LOG.error("Got response code "+code+" while trying to post data");
		} else {
			LOG.debug("Sucessfully sent data to New Relic");
		}
		response.close();
		
	}

}
