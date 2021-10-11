package com.newrelic.event.cisco.rtmt;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

	private static Logger LOG = Logger.getLogger(Main.class);
	protected static final String ACCOUNT_NUMBER = "account_number";
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

		Main m = new Main();
		try {
			m.setup(configFile);
		} catch (Exception e) {
			LOG.error("Failed to start RTMT collector", e);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setup(String pluginConfigFileName) throws Exception {

		LogManager.getRootLogger().setLevel(Level.WARN);
		LogManager.getLogger("org.apache.axiom").setLevel(Level.ERROR);

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
		Boolean useEUEndpoint = false;
		
		if ((globalObj != null) && (globalObj instanceof Map)) {
			globalProperties = (Map<String, Object>) globalObj;
			
			String loggingLevel = (String) globalProperties.get("logging_level");
			Level level = Level.INFO;
			if(loggingLevel != null) {
				if(loggingLevel.equalsIgnoreCase("error")) {
					level = Level.ERROR;
				} else if(loggingLevel.equalsIgnoreCase("warn")) {
					level = Level.WARN;
				} else if(loggingLevel.equalsIgnoreCase("debug")) {
					level = Level.DEBUG;
				} else if(loggingLevel.equalsIgnoreCase("fatal")) {
					level = Level.FATAL;
				} else if(loggingLevel.equalsIgnoreCase("all")) {
					level = Level.ALL;
				}
			}
			LogManager.getLogger("com.newrelic.event.cisco.rtmt").setLevel(level);
			LogManager.getLogger("org.apache").setLevel(Level.WARN);
			
			useEUEndpoint = (Boolean)globalProperties.get("useEU");
			if(useEUEndpoint == null) useEUEndpoint = false;

			Number freqObject = (Number)globalProperties.get("frequency");
			if(freqObject != null && freqObject.longValue() > 0) {
				frequency = freqObject.longValue();
			}
			LOG.info("Will query metrics every " + frequency+" minutes");
			licenseKey = (String) globalProperties.get(LICENSE_KEY);

			accountNumber = (String) globalProperties.get(ACCOUNT_NUMBER);
			if (accountNumber == null || accountNumber.isEmpty()) {
				String msg = ACCOUNT_NUMBER + " is empty or null, unable to start";
				throw new Exception(msg);
			} else {
				accountNumber = accountNumber.trim();
			}

		} else {
			RTMTException e = new RTMTException("Failed to initialize RTMT Collector since global not set in configuration file " + pluginConfigFileName);
			throw e;
		}
		
		Object filtersObj = pluginConfigProperties.get("filters");
		if(filtersObj != null && filtersObj instanceof List) {
			List filters = (ArrayList)filtersObj;
			for(Object obj : filters) {
				LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>)obj;
				String scope = (String) map.get("scope");
				String type = (String)map.get("type");
				boolean isExclude = true;
				if(type != null) {
					if(type.equalsIgnoreCase("include")) {
						isExclude = false;
					}
				}
				String objectName = (String)map.get("objectName");
				
				if(objectName == null || objectName.isEmpty()) {
					LOG.info("Object name must be defined for filter: "+obj);
					continue;
				}
				
				ArrayList<String> instances = (ArrayList<String>)map.get("instances");
				ArrayList<String> counters = (ArrayList<String>)map.get("counters");
				
				if(scope != null && type != null && objectName != null) {
					if(scope.equalsIgnoreCase("global")) {
						GlobalFilter gFilter = new GlobalFilter(objectName);
						gFilter.isExclude = isExclude;
						gFilter.objectName = objectName;
						gFilter.instances = instances;
						gFilter.counters = counters;
						Filters.addGlobalFilter(gFilter);
					} else if(scope.equalsIgnoreCase("agent")) {
						String agentName = (String) map.get("agentName");
						
						if (agentName != null && !agentName.isEmpty()) {
							RTMTAgentFilter aFilter = new RTMTAgentFilter(agentName, objectName);
							aFilter.isExclude = isExclude;
							aFilter.objectName = objectName;
							aFilter.instances = instances;
							aFilter.counters = counters;
							Filters.addRTMTAgentFilter(agentName, aFilter);
						} else {
							LOG.error("Agent Filter declared but no agent name was given");
						}
					} else if(scope.equalsIgnoreCase("host")) {
						String hostName = (String) map.get("hostName");
						
						if (hostName != null && !hostName.isEmpty()) {
							RTMTHostFilter hFilter = new RTMTHostFilter(hostName, objectName);
							hFilter.isExclude = isExclude;
							hFilter.objectName = objectName;
							hFilter.instances = instances;
							hFilter.counters = counters;
							Filters.addHostFilter(hostName, hFilter);
						} else {
							LOG.error("Host Filter declared but no host name was given");
						}
					}
				}
			}
			
		}
		
		if(Filters.haveFilters()) {
			Filters.logFilters();
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
		Runner runner = new Runner(accountNumber, licenseKey,useEUEndpoint);
		
		for (Iterator agentIterator = agents.iterator(); agentIterator.hasNext();) {
			Map<String, Object> agentProperties = (Map<String, Object>) agentIterator.next();

			String agentName = (String) agentProperties.get("name");
			if (agentName == null) {
				String msg = "'name' is a required property for each agent config";
				throw new Exception(msg);
			}

			String hostName = (String) agentProperties.get("host");
			if (hostName == null) {
				hostName = (String) agentProperties.get("hostname");
				if (hostName == null) {
					try {
						hostName = InetAddress.getLocalHost().getHostName();
					} catch (Throwable t) {
						hostName = "localhost";
					}
				}
			}

			RTMTAgentFactory factory = new RTMTAgentFactory();
			RTMTAgent agent = factory.createAgent(agentProperties);
			runner.addAgent(agent);
		}
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(runner, 0L, frequency, TimeUnit.MINUTES);
	}

}
