package com.newrelic.event.cisco.rtmt;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.cisco.schemas.ast.soap.ArrayOfCounterType;
import com.cisco.schemas.ast.soap.CounterInfoType;
import com.cisco.schemas.ast.soap.CounterNameType;
import com.cisco.schemas.ast.soap.CounterType;
import com.cisco.schemas.ast.soap.InstanceNameType;
import com.cisco.schemas.ast.soap.InstanceType;
import com.cisco.schemas.ast.soap.ObjectInfoType;
import com.cisco.schemas.ast.soap.ObjectNameType;
import com.cisco.schemas.ast.soap.PerfmonCollectCounterDataDocument;
import com.cisco.schemas.ast.soap.PerfmonCollectCounterDataDocument.PerfmonCollectCounterData;
import com.cisco.schemas.ast.soap.PerfmonCollectCounterDataResponseDocument;
import com.cisco.schemas.ast.soap.PerfmonCollectCounterDataResponseDocument.PerfmonCollectCounterDataResponse;
import com.cisco.schemas.ast.soap.PerfmonListCounterDocument;
import com.cisco.schemas.ast.soap.PerfmonListCounterDocument.PerfmonListCounter;
import com.cisco.schemas.ast.soap.PerfmonListCounterResponseDocument;
import com.cisco.schemas.ast.soap.PerfmonListCounterResponseDocument.PerfmonListCounterResponse;
import com.cisco.schemas.ast.soap.PerfmonListInstanceDocument;
import com.cisco.schemas.ast.soap.PerfmonListInstanceDocument.PerfmonListInstance;
import com.cisco.schemas.ast.soap.PerfmonListInstanceResponseDocument;
import com.cisco.schemas.ast.soap.PerfmonListInstanceResponseDocument.PerfmonListInstanceResponse;
import com.newrelic.event.cisco.rtmt.metrics.AttributeMetric;
import com.newrelic.event.cisco.rtmt.metrics.GaugeMetric;
import com.newrelic.event.cisco.rtmt.metrics.Metric;
import com.newrelic.soap.cisco.PerfmonServiceStub;

public class RTMTClient {

	private String url;
	private List<String> hosts;
	private static Logger LOG = Logger.getLogger(RTMTClient.class);	
	private static final int maxCounters = 200;
	private List<String> counterIgnores = new ArrayList<String>();

	public RTMTClient(String u) {
		url = u;
		hosts = new ArrayList<String>();
	}

	public void addHost(String host) {
		hosts.add(host);
	}
	
	public String getURL() {
		return url;
	}

	public boolean removeHost(String host) {
		return hosts.remove(host);
	}

	public int populate(String agentName,String agentHost) {
		LOG.debug("Collecting Counters");
		int reported = 0;
		for(String host : hosts) {
			try {
				RTMTHost rtmtHost = getCounters(host);
				reported += collectCounters(agentName,agentHost,rtmtHost);
			} catch (RemoteException e) {
				LOG.error("Remote exception occurred", e);
			}

		}
		return reported;
	}

	protected ObjectInfoType[] getCounterInfos(String host) throws RemoteException {
		RTMTHost h = new RTMTHost(host);

		PerfmonServiceStub stub = Stubs.getStub(url);

		PerfmonListCounterDocument inDoc = PerfmonListCounterDocument.Factory.newInstance();
		PerfmonListCounter counters = PerfmonListCounter.Factory.newInstance();
		counters.setHost(host);

		inDoc.setPerfmonListCounter(counters );

		PerfmonListCounterResponseDocument doc = stub.perfmonListCounter(inDoc);

		PerfmonListCounterResponse response = doc.getPerfmonListCounterResponse();
		ObjectInfoType[] counterObjects = response.getPerfmonListCounterReturnArray();

//		for(ObjectInfoType counter : counterObjects) {
//			HashSet<String> current =  new HashSet<String>();
//			ObjectNameType name = counter.getName();
//			String objName = name.getStringValue();
//			ArrayOfCounterType counterArray = counter.getArrayOfCounter();
//			CounterType[] itemArray = counterArray.getItemArray();
//			ObjectName oName = new ObjectName(objName, counter.getMultiInstance());
//			h.addObjectName(oName);
//			for(CounterType counterItem : itemArray) {
//				CounterNameType cName = counterItem.getName();
//				String counterName = cName.getStringValue();
//				if(counterName != null && !counterName.isEmpty()) {
//					current.add(counterName);
//				}
//			}
//			try {
//				h.addCounters(oName, current);
//			} catch (RTMTException e) {
//				LOG.debug("Exception occurred", e);
//			}
//		}
		return counterObjects;
	}
	
	protected RTMTHost getCounters(String host) throws RemoteException {
		LOG.debug("Getting list of counters for host: "+host);
		RTMTHost h = new RTMTHost(host);

		PerfmonServiceStub stub = Stubs.getStub(url);

		PerfmonListCounterDocument inDoc = PerfmonListCounterDocument.Factory.newInstance();
		PerfmonListCounter counters = PerfmonListCounter.Factory.newInstance();
		counters.setHost(host);

		inDoc.setPerfmonListCounter(counters );

		PerfmonListCounterResponseDocument doc = stub.perfmonListCounter(inDoc);

		PerfmonListCounterResponse response = doc.getPerfmonListCounterResponse();
		ObjectInfoType[] counterObjects = response.getPerfmonListCounterReturnArray();

		for(ObjectInfoType counter : counterObjects) {
			HashSet<String> current =  new HashSet<String>();
			ObjectNameType name = counter.getName();
			String objName = name.getStringValue();
			ArrayOfCounterType counterArray = counter.getArrayOfCounter();
			CounterType[] itemArray = counterArray.getItemArray();
			ObjectName oName = new ObjectName(objName, counter.getMultiInstance());
			h.addObjectName(oName);
			for(CounterType counterItem : itemArray) {
				CounterNameType cName = counterItem.getName();
				String counterName = cName.getStringValue();
				if(counterName != null && !counterName.isEmpty()) {
					current.add(counterName);
				}
			}
			try {
				h.addCounters(oName, current);
			} catch (RTMTException e) {
				LOG.debug("Exception occurred", e);
			}
		}

		return h;

	}

	@SuppressWarnings("unchecked")
	protected int collectCounters(String agentName,String agentHost, RTMTHost host) throws RemoteException {
		long start = System.currentTimeMillis();

		List<Filter> filtersToApply = new ArrayList<Filter>();
		filtersToApply.addAll(Filters.getGlobalFilters());
		filtersToApply.addAll(Filters.getAgentFilters(agentName));
		filtersToApply.addAll(Filters.getHostFilters(host.getHost()));

		HashMap<String, Integer> counts = new HashMap<String, Integer>();

		LOG.debug("Getting counters for host: "+host.getHost());
		CloseableHttpClient httpClient = HttpClients.createDefault();
		PerfmonServiceStub stub = Stubs.getStub(url);
		JSONArray reportJson = new JSONArray();

		for(ObjectName objectName : host.getObjectNames()) {
			String objName = objectName.getObjectName();

			List<String> countersToSkip = new ArrayList<String>();
			List<String> countersToInclude = new ArrayList<String>();
			boolean multiInstance = objectName.isMultiInstance();

			List<String> instancesToSkip = new ArrayList<String>();
			List<String> instancesToInclude = new ArrayList<String>();

			boolean ignore = false;

			for(int i=0;i<filtersToApply.size(); i++) {
				Filter filter = filtersToApply.get(i);
				boolean haveCounters = filter.counters != null && !filter.counters.isEmpty();
				boolean haveInstances = multiInstance ? filter.instances != null && !filter.instances.isEmpty() : false;

				if(filter.isExclude) {
					// logic to exclude collection of certain counters

					if(objName.equalsIgnoreCase(filter.objectName)) {
						if(filter.scope == FilterScope.GLOBAL) {
							if(!haveCounters && !haveInstances) {
								ignore = true;
							} else if(haveCounters) {
								countersToSkip.addAll(filter.counters);
								if(haveInstances) {
									instancesToSkip.addAll(filter.instances);
								}
							} else if(haveInstances) {
								instancesToSkip.addAll(filter.instances);
							}		
						} else if(filter.scope == FilterScope.RTMT_AGENT) {		
							RTMTAgentFilter f = (RTMTAgentFilter)filter;
							if(f.agentName.equalsIgnoreCase(agentName)) {
								if(!haveCounters && !haveInstances) {
									ignore = true;
								} else if(haveCounters) {
									countersToSkip.addAll(filter.counters);
									if(haveInstances) {
										instancesToSkip.addAll(filter.instances);
									}
								} else if(haveInstances) {
									instancesToSkip.addAll(filter.instances);
								}							
							}
						} else if(filter.scope == FilterScope.RTMT_HOST) {
							RTMTHostFilter f = (RTMTHostFilter)filter;
							if(f.hostName.equalsIgnoreCase(host.getHost())) {
								if(!haveCounters && !haveInstances) {
									ignore = true;
								} else if(haveCounters) {
									countersToSkip.addAll(filter.counters);
									if(haveInstances) {
										instancesToSkip.addAll(filter.instances);
									}
								} else if(haveInstances) {
									instancesToSkip.addAll(filter.instances);
								}							
							}
						}

					}

				} else {
					// logic to only collect certain counters
					
					if(!objName.equalsIgnoreCase(filter.objectName)) {
						if(filter.scope == FilterScope.GLOBAL) {
							ignore = true;
						} else if(filter.scope == FilterScope.RTMT_AGENT) {		
							RTMTAgentFilter f = (RTMTAgentFilter)filter;
							if(f.agentName.equalsIgnoreCase(agentName)) {
								ignore = true;
							}
						} else if(filter.scope == FilterScope.RTMT_HOST) {
							RTMTHostFilter f = (RTMTHostFilter)filter;
							if(f.hostName.equalsIgnoreCase(host.getHost())) {
								ignore = true;
							}
						}
					} else {
						if(filter.scope == FilterScope.GLOBAL) {
							if(haveCounters) {
								countersToInclude.addAll(filter.counters);
							}
							if(haveInstances) {
								instancesToInclude.addAll(filter.instances);
							}
						} else if(filter.scope == FilterScope.RTMT_AGENT) {		
							RTMTAgentFilter f = (RTMTAgentFilter)filter;
							if(f.agentName.equalsIgnoreCase(agentName)) {
								if(haveCounters) {
									countersToInclude.addAll(filter.counters);
								}
								if(haveInstances) {
									instancesToInclude.addAll(filter.instances);
								}								
							}
						} else if(filter.scope == FilterScope.RTMT_HOST) {
							RTMTHostFilter f = (RTMTHostFilter)filter;
							if(f.hostName.equalsIgnoreCase(host.getHost())) {
								if(haveCounters) {
									countersToInclude.addAll(filter.counters);
								}
								if(haveInstances) {
									instancesToInclude.addAll(filter.instances);
								}
							}
						}
					}
					

				}
			}
			
			if(ignore && instancesToInclude.isEmpty() && instancesToSkip.isEmpty() && countersToInclude.isEmpty() && countersToSkip.isEmpty()) {
				continue;
			}

			boolean skippingInstances = !instancesToSkip.isEmpty();
			boolean includingInstances = !instancesToInclude.isEmpty();			

			List<String> instances = new ArrayList<String>();
			if(objectName.isMultiInstance()) {
				PerfmonListInstanceDocument doc = PerfmonListInstanceDocument.Factory.newInstance();
				PerfmonListInstance listInstance = doc.addNewPerfmonListInstance();
				listInstance.setHost(host.getHost());
				ObjectNameType objNameType = listInstance.addNewObject();
				objNameType.setStringValue(objName);
				listInstance.setObject(objNameType );
				PerfmonListInstanceResponseDocument responseDoc = stub.perfmonListInstance(doc);

				PerfmonListInstanceResponse response = responseDoc.getPerfmonListInstanceResponse();
				InstanceType[] instanceTypes = response.getPerfmonListInstanceReturnArray();

				for(InstanceType instanceType : instanceTypes) {
					InstanceNameType instanceName = instanceType.getName();
					String iName = instanceName.getStringValue();
					if(skippingInstances) {
						if(!instancesToSkip.contains(iName)) {
							instances.add(iName);
						}
					} else if(includingInstances) {
						if(instancesToInclude.contains(iName)) {
							instances.add(iName);
						}
					}
				}
			}
			List<Metric> metricsList = collectObjectCounters(host.getHost(), objName, stub);
			int size = instances.size() > 0 ? metricsList.size()/instances.size() : metricsList.size();

			counts.put(objName, size);
			if( counterIgnores.contains(objName)) continue;

			if(size > maxCounters) {
				if(!counterIgnores.contains(objName)) {
					LOG.warn("Object Name "+objName +" is reporting "+size+" counters which exceeds the max number of counters: "+maxCounters);
					LOG.warn("Object Name "+objName +" will be ignored");
					counterIgnores.add(objName);
				}
				continue;
			}

			if (objectName.isMultiInstance()) {
				HashMap<String, List<Metric>> instanceMetrics = new HashMap<String, List<Metric>>();
				for (Metric metric : metricsList) {
					String metricName = metric.getName();
					
					int index = metricName.indexOf('(');
					if(index > -1) {
						int index2 = metricName.indexOf(')');
						if(index2 > -1) {
							String instanceName = metricName.substring(index+1, index2);
							if (instances.contains(instanceName)) {
								index = metricName.lastIndexOf('\\');
								if (index == -1)
									index = metricName.lastIndexOf('-');
								if (index > -1) {
									String metricName2 = metricName.substring(index + 1);
									List<Metric> metricList = instanceMetrics.get(instanceName);
									if (metricList == null)
										metricList = new ArrayList<Metric>();
									if (metric instanceof AttributeMetric) {
										AttributeMetric aMetric = (AttributeMetric) metric;
										AttributeMetric aMetric2 = new AttributeMetric(metricName2, aMetric.getValue());
										metricList.add(aMetric2);
									} else if (metric instanceof GaugeMetric) {
										GaugeMetric gMetric = (GaugeMetric) metric;
										GaugeMetric gMetric2 = new GaugeMetric(metricName2, gMetric.getValue());
										metricList.add(gMetric2);
									}
									instanceMetrics.put(instanceName, metricList);
								} 
							}
						}
					}
				}


				Set<String> keys = instanceMetrics.keySet();

				for(String key : keys) {
					List<Metric> metricList = instanceMetrics.get(key);
					metricList.add(new AttributeMetric("AgentName", agentName));
					metricList.add(new AttributeMetric("InstanceName", key));
					try {
						metricList.add(new AttributeMetric("AgentName", agentName));
						URL theUrl = new URL(url);
						if (theUrl != null) {
							String rtmtHostName = theUrl.getHost();
							if (rtmtHostName != null && !rtmtHostName.isEmpty()) {
								metricList.add(new AttributeMetric("RTMT Hostname", rtmtHostName));
							}
							int port = theUrl.getPort();
							metricList.add(new AttributeMetric("RTMT Port", port));
						}
					} catch (MalformedURLException e) {
					}
					if (agentHost != null) {
						metricList.add(new AttributeMetric("AgentHost", agentHost));
					}
					metricList.add(new AttributeMetric("ObjectName", objName));
					Map<String, Object> attributes = new HashMap<String, Object>();
					for (Metric metric : metricList) {
						String metricName = metric.getName();

						int index = metricName.lastIndexOf('\\');
						if(index > -1) {
							metricName = metricName.substring(index+1);
						}
						if(!countersToInclude.isEmpty() && !countersToInclude.contains(metricName)) {
							continue;
						}
						if(!countersToSkip.isEmpty() && countersToSkip.contains(metricName)) {
							continue;
						}
						attributes.put(metricName, metric.getValue());
					}
					attributes.put("eventType", "RTMT");
					attributes.put("host", host.getHost());
					JSONObject json = new JSONObject(attributes);
					reportJson.add(json);			

				}
			} else {
				metricsList.add(new AttributeMetric("AgentName", agentName));
				try {
					metricsList.add(new AttributeMetric("AgentName", agentName));
					URL theUrl = new URL(url);
					if (theUrl != null) {
						String rtmtHostName = theUrl.getHost();
						if (rtmtHostName != null && !rtmtHostName.isEmpty()) {
							metricsList.add(new AttributeMetric("RTMT Hostname", rtmtHostName));
						}
						int port = theUrl.getPort();
						metricsList.add(new AttributeMetric("RTMT Port", port));
					}
				} catch (MalformedURLException e) {
				}
				if (agentHost != null) {
					metricsList.add(new AttributeMetric("AgentHost", agentHost));
				}
				metricsList.add(new AttributeMetric("ObjectName", objName));
				Map<String, Object> attributes = new HashMap<String, Object>();
				for (Metric metric : metricsList) {
					String metricName = metric.getName();
					int index = metricName.lastIndexOf('\\');
					if(index > -1) {
						metricName = metricName.substring(index+1);
					}
					if(!countersToInclude.isEmpty() && !countersToInclude.contains(metricName)) {
						continue;
					}
					if(!countersToSkip.isEmpty() && countersToSkip.contains(metricName)) {
						continue;
					}
					attributes.put(metricName, metric.getValue());
				}
				attributes.put("eventType", "RTMT");
				attributes.put("host", host.getHost());
				JSONObject json = new JSONObject(attributes);
				reportJson.add(json);
			}

		}

		JSONObject json = new JSONObject(counts);
		json.put("eventType", "RTMT_Counts");
		json.put("host", host.getHost());
		json.put("Events", reportJson.size());
		long end = System.currentTimeMillis();
		json.put("QueryTime(ms)", end-start);
		reportJson.add(json);
		try {
			Report.getInstance().post(reportJson, httpClient);
			httpClient.close();
		} catch (IOException e) {
			LOG.error("IOException Occurred", e);
		}

		int total = 0;
		for(String key : counts.keySet()) {
			total += counts.get(key);
		}
		return total;
	}

	private List<Metric> collectObjectCounters(String host, String objectName, PerfmonServiceStub stub) throws RemoteException {
		LOG.debug("Getting object counters for host: "+host+" and Object Name: "+objectName);
		List<Metric> metrics = new ArrayList<Metric>();
		metrics.add(new AttributeMetric("Host", host));

		PerfmonCollectCounterDataDocument inDoc = PerfmonCollectCounterDataDocument.Factory.newInstance();
		PerfmonCollectCounterData counterData = PerfmonCollectCounterData.Factory.newInstance();
		counterData.setHost(host);
		ObjectNameType oNameType = ObjectNameType.Factory.newInstance();
		oNameType.setStringValue(objectName);
		counterData.setObject(oNameType );
		inDoc.setPerfmonCollectCounterData(counterData );
		PerfmonCollectCounterDataResponseDocument doc = stub.perfmonCollectCounterData(inDoc);

		PerfmonCollectCounterDataResponse response = doc.getPerfmonCollectCounterDataResponse();
		CounterInfoType[] counterArray = response.getPerfmonCollectCounterDataReturnArray();
		for(CounterInfoType cInfo : counterArray) {
			CounterNameType cName = cInfo.getName();
			String name = cName.getStringValue();
			Metric metric = new GaugeMetric(name, cInfo.getValue());
			metrics.add(metric);
		}
		LOG.debug("Returning "+metrics.size()+" counters for host: "+host+" and object name: "+objectName);
		return metrics;
	}
}
