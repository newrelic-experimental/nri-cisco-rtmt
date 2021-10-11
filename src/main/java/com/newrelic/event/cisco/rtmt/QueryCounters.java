package com.newrelic.event.cisco.rtmt;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.beust.jcommander.JCommander;
import com.cisco.schemas.ast.soap.ArrayOfCounterType;
import com.cisco.schemas.ast.soap.CounterNameType;
import com.cisco.schemas.ast.soap.CounterType;
import com.cisco.schemas.ast.soap.InstanceNameType;
import com.cisco.schemas.ast.soap.InstanceType;
import com.cisco.schemas.ast.soap.ObjectInfoType;
import com.cisco.schemas.ast.soap.ObjectNameType;
import com.cisco.schemas.ast.soap.PerfmonListInstanceDocument;
import com.cisco.schemas.ast.soap.PerfmonListInstanceDocument.PerfmonListInstance;
import com.cisco.schemas.ast.soap.PerfmonListInstanceResponseDocument;
import com.cisco.schemas.ast.soap.PerfmonListInstanceResponseDocument.PerfmonListInstanceResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.soap.cisco.PerfmonServiceStub;

public class QueryCounters {
	
	private static Logger LOG = Logger.getLogger(QueryCounters.class);
	protected static final String ACCOUNT_ID = "account_number";
	protected static final String LICENSE_KEY = "license_key";
	
	private ObjectMapper objectMapper = new ObjectMapper();
	
	protected static Map<String, Object> globalProperties;
	protected String accountNumber = null;
	protected String licenseKey = null;
	private long frequency = 1;
	
	public static void main(String[] argv) {
		Args args = new Args();
		JCommander jct = JCommander.newBuilder().addObject(args).build();
		jct.parse(argv);
		if (args.isHelp()) {
			jct.usage();
		}

		String configFile = System.getenv("CONFIG_FILE");
		if (configFile == null) {
			configFile = args.getConfigFile();
			if (configFile == null) {
				System.err.println("Error: Please specify a valid config_file argument");
				System.exit(-1);
			}
		}
		
		QueryCounters m = new QueryCounters();
		try {
			m.setup(configFile);
		} catch (Exception e) {
			LOG.error("Failed to start RTMT collector", e);
		}

	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setup(String pluginConfigFileName) throws Exception {
		
		PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %5p %l %c - %m%n");
		RollingFileAppender appender = new RollingFileAppender(layout, "logs/cisco_rtmt.log");
		BasicConfigurator.configure(appender);
		
		Map<String, Object> pluginConfigProperties = null;
		File pluginConfigFile = new File(pluginConfigFileName);
		if (pluginConfigFile.exists()) {
			Reader reader = new FileReader(pluginConfigFile);
			pluginConfigProperties = objectMapper.readValue(reader, new TypeReference<Map<String, Object>>() {});
		} else {
			String msg = "Config File [" + pluginConfigFileName + "] does not exist";
			throw new Exception(msg);
		}

		Object globalObj = pluginConfigProperties.get("global");
		if ((globalObj != null) && (globalObj instanceof Map)) {
			globalProperties = (Map<String, Object>) globalObj;
			
			Number freqObject = (Number)globalProperties.get("frequency");
			if(freqObject != null && freqObject.longValue() > 0) {
				frequency = freqObject.longValue();
			}
			LOG.info("Will query metrics every " + frequency+" minutes");
			licenseKey = (String) globalProperties.get(LICENSE_KEY);
			
				accountNumber = (String) globalProperties.get(ACCOUNT_ID);
				if (accountNumber == null || accountNumber.isEmpty()) {
					String msg = ACCOUNT_ID + " is empty or null, unable to start";
					throw new Exception(msg);
				} else {
					accountNumber = accountNumber.trim();
				}
			
		} else {
			RTMTException e = new RTMTException("Failed to initialize RTMT Collector since global not set in configuration file " + pluginConfigFileName);
			throw e;
		}

		Object agentsObj = pluginConfigProperties.get("agents");
		if (agentsObj == null) {
				throw new Exception("'agents' configuration entry must not be null");
		}
		if (!(agentsObj instanceof List)) {
			String msg = "'agents' configuration entry must be a list";
			throw new Exception(msg);
		}

		List agents = (ArrayList) agentsObj;
		PrintWriter writer = new PrintWriter("RTMT-Counters.txt");
		for (Iterator agentIterator = agents.iterator(); agentIterator.hasNext();) {
			Map<String, Object> agentProperties = (Map<String, Object>) agentIterator.next();

			String agentName = (String) agentProperties.get("name");
			if (agentName == null) {
				String msg = "'name' is a required property for each agent config";
				throw new Exception(msg);
			}

			RTMTAgentFactory factory = new RTMTAgentFactory();
			RTMTAgent agent = factory.createAgent(agentProperties);
			
			PerfmonServiceStub stub = Stubs.getStub(agent.getURL());
			
			RTMTClient client = agent.getClient();
			List<String> hosts = agent.getHosts();
			
			for(String host : hosts) {
				ObjectInfoType[] objects = client.getCounterInfos(host);
				writer.println("Counters from host "+host);

				for(ObjectInfoType objectType : objects) {
					ObjectNameType name = objectType.getName();
					String objName = name.getStringValue();
					
					writer.println("\tObject Name: "+objName);
					boolean multiInstance = objectType.getMultiInstance();
					if(multiInstance) {
						PerfmonListInstanceDocument doc = PerfmonListInstanceDocument.Factory.newInstance();
						PerfmonListInstance listInstance = doc.addNewPerfmonListInstance();
						listInstance.setHost(host);
						ObjectNameType objNameType = listInstance.addNewObject();
						objNameType.setStringValue(objName);
						listInstance.setObject(objNameType );
						PerfmonListInstanceResponseDocument responseDoc = stub.perfmonListInstance(doc);

						PerfmonListInstanceResponse response = responseDoc.getPerfmonListInstanceResponse();
						InstanceType[] instanceTypes = response.getPerfmonListInstanceReturnArray();

						if (instanceTypes != null && instanceTypes.length > 0) {
							writer.println("\t\tInstances");
							for(InstanceType iType : instanceTypes) {
								InstanceNameType iName = iType.getName();
								writer.println("\t\t\t"+iName.getStringValue());
							}
						}
					}
					
					ArrayOfCounterType counterArray = objectType.getArrayOfCounter();
					if(counterArray != null) {
						writer.println("\t\tCounters");
						CounterType[] counterItems = counterArray.getItemArray();
						for(CounterType cType : counterItems) {
							CounterNameType cName = cType.getName();
							writer.println("\t\t\t"+cName.getStringValue());
						}
					}
				}
//				RTMTHost rtmtHost = client.getCounters(host);
//				List<ObjectName> objectNames = rtmtHost.getObjectNames();
//				writer.println("Counters from host "+host);
//				for(ObjectName objectName : objectNames) {
//					writer.println("\tObject Name: "+objectName.getObjectName());
//					HashSet<String> counters = rtmtHost.getCounters(objectName.getObjectName());
//					for(String counter : counters) {
//						writer.println("\t\tCounter Name: "+counter);
//					}
//				}
			}
			
			writer.close();
			
		}
	}

}
