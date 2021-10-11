package com.newrelic.event.cisco.rtmt;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.axis2.AxisFault;
import org.apache.axis2.java.security.TrustAllTrustManager;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.impl.httpclient3.HttpTransportPropertiesImpl;
import org.apache.axis2.transport.http.security.SSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.log4j.Logger;

import com.newrelic.soap.cisco.PerfmonServiceStub;

public class Stubs {

	private static HashMap<String, PerfmonServiceStub> stubs = new HashMap<String, PerfmonServiceStub>();
	private static HashMap<String, Authentication> authentications = new HashMap<String, Authentication>();
	private static Logger LOG = Logger.getLogger(Stubs.class);

	public static void addAuthentication(String url, Authentication auth) {
		authentications.put(url, auth);
	}

	public static PerfmonServiceStub initializeStub(String urlStr) throws AxisFault {
		PerfmonServiceStub stub = new PerfmonServiceStub(urlStr);
		
		try {
			URL url = new URL(urlStr);
			
			String urlProtocol = url.getProtocol();
			int port = url.getPort();
			
			if (urlProtocol.equalsIgnoreCase("https")) {
				
				if(port == -1) port = 443;
				SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
				sslCtx.init(new KeyManager[0], new TrustManager[] { new TrustAllTrustManager() }, new SecureRandom());
				stub._getServiceClient().getOptions().setProperty(HTTPConstants.CUSTOM_PROTOCOL_HANDLER,new Protocol("https", (ProtocolSocketFactory) new SSLProtocolSocketFactory(sslCtx), port));
			}
		} catch (KeyManagementException e) {
			LOG.debug("Error parsing URL due to KeyManagementException", e);
		} catch (MalformedURLException e) {
			LOG.debug("Error parsing URL due to MalformedURLException", e);
		} catch (NoSuchAlgorithmException e) {
			LOG.debug("Error parsing URL due to NoSuchAlgorithmException", e);
		}

		Authentication authentication = authentications.get(urlStr);
		if(authentication != null) {
			HttpTransportPropertiesImpl.Authenticator auth = new HttpTransportPropertiesImpl.Authenticator();
			auth.setUsername(authentication.getUsername());
			auth.setPassword(authentication.getPassword());
			//auth.setPreemptiveAuthentication(true);

			stub._getServiceClient().getOptions().setProperty(HTTPConstants.AUTHENTICATE, auth);
		}
		stubs.put(urlStr, stub);

		return stub;
	}

	public static PerfmonServiceStub getStub(String url) throws AxisFault {
		PerfmonServiceStub stub = stubs.get(url);
		if(stub == null) {
			stub = initializeStub(url);
		}
		return stub;
	}
}
