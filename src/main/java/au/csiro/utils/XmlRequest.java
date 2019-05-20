//=============================================================================
//===	Copyright (C) 2001-2005 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: GeoNetwork@fao.org
//==============================================================================

package au.csiro.utils;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.protocol.Protocol;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//=============================================================================

public class XmlRequest
{
	public enum Method { GET, POST }

	//---------------------------------------------------------------------------
	//---
	//--- Constructor
	//---
	//---------------------------------------------------------------------------

	public XmlRequest() { this(null, 80); }

	//---------------------------------------------------------------------------

	public XmlRequest(String host) { this(host, 80); }

	//---------------------------------------------------------------------------

    public XmlRequest(String host, int port)
    {
        this(host, port, "http");
    }
    
	public XmlRequest(String host, int port, String protocol)
	{
		this.host = host;
		this.port = port;
        this.protocol = protocol;

		setMethod(Method.GET);
		state.addCookie(cookie);
		client.setState(state);
		client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		client.setHostConfiguration(config);
		List<String> authPrefs = new ArrayList<String>(2); 
		authPrefs.add(AuthPolicy.DIGEST); 
		authPrefs.add(AuthPolicy.BASIC); 
		// This will exclude the NTLM authentication scheme 
		client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
	}

	//---------------------------------------------------------------------------

	public XmlRequest(URL url)
	{
		this(url.getHost(), url.getPort() == -1 ? url.getDefaultPort() : url.getPort(), url.getProtocol());

		address = url.getPath();
		query   = url.getQuery();
	}

	//---------------------------------------------------------------------------
	//---
	//--- API methods
	//---
	//---------------------------------------------------------------------------

	public String getHost()         { return host;         }
	public int    getPort()         { return port;         }
	public String getAddress()      { return address;      }
	public Method getMethod()       { return method;       }
	public String getSentData()     { return sentData;     }
	public String getReceivedData() { return receivedData; }

	//---------------------------------------------------------------------------

	public void setHost(String host)
	{
		this.host = host;
	}

	//---------------------------------------------------------------------------

	public void setPort(int port)
	{
		this.port = port;
	}

	//---------------------------------------------------------------------------

	public void setAddress(String address)
	{
		this.address = address;
	}

	//---------------------------------------------------------------------------

	public void setUrl(URL url)
	{
		host    = url.getHost();
		port    = (url.getPort() == -1) ? url.getDefaultPort(): url.getPort();
		protocol= url.getProtocol();
        address = url.getPath();
		query   = url.getQuery();
	}

	//---------------------------------------------------------------------------

	public void setMethod(Method m)
	{
		method = m;
	}

	//---------------------------------------------------------------------------

	public void setUseSOAP(boolean yesno)
	{
		useSOAP = yesno;
	}

	//---------------------------------------------------------------------------

	public void setUseProxy(boolean yesno)
	{
		useProxy = yesno;
	}

	//---------------------------------------------------------------------------

	public void setProxyHost(String host)
	{
		proxyHost = host;
	}

	//---------------------------------------------------------------------------

	public void setProxyPort(int port)
	{
		proxyPort = port;
	}

	//---------------------------------------------------------------------------

	public void setProxyCredentials(String username, String password)
	{
		if (username == null || username.trim().length() == 0)
			return;

		Credentials cred = new UsernamePasswordCredentials(username, password);
		AuthScope   scope= new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);

		client.getState().setProxyCredentials(scope, cred);
		
		proxyAuthent = true;
	}

	//---------------------------------------------------------------------------

	public void clearParams()
	{
		alSimpleParams.clear();
		postParams = null;
	}

	//--------------------------------------------------------------------------

	public void addParam(String name, Object value)
	{
		if (value != null)
			alSimpleParams.add(new NameValuePair(name, value.toString()));

		method = Method.GET;
	}

	//---------------------------------------------------------------------------

	public void setRequest(Element request)
	{
		postParams = (Element) request.detach();
		method     = Method.POST;
	}

	//---------------------------------------------------------------------------
	/** Sends an xml request and obtains an xml response */

	public Element execute(Element request) throws IOException, Exception
	{
		setRequest(request);
		return execute();
	}

	//---------------------------------------------------------------------------
	/** Sends a request and obtains an xml response. The request can be a GET or a
	  * POST depending on the method used to set parameters. Calls to the 'addParam'
	  * method set a GET request while the setRequest method sets a POST/xml request.
	  */

	public Element execute() throws IOException, Exception
	{
		HttpMethodBase httpMethod = setupHttpMethod();

		Element response = doExecute(httpMethod);

		return response;
	}

	//---------------------------------------------------------------------------
	/** Sends the content of a file using a POST request and gets the response in
	  * xml format.
	  */

	public Element send(String name, File inFile) throws IOException, Exception
	{
		Part[] parts = new Part[alSimpleParams.size()+1]; 
		
		int partsIndex = 0;
		
		parts[partsIndex] = new FilePart(name, inFile);
		
		for (NameValuePair nv : alSimpleParams) 
			parts[++partsIndex] = new StringPart(nv.getName(), nv.getValue());

		PostMethod post = new PostMethod();
		post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
		post.addRequestHeader("Accept", "application/xml");
		post.setPath(address);
		post.setDoAuthentication(useAuthent());

		//--- execute request

		Element response = doExecute(post);

		return response;
	}

	//---------------------------------------------------------------------------

	public void setCredentials(String username, String password)
	{
		Credentials cred = new UsernamePasswordCredentials(username, password);
		AuthScope   scope= new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);

		client.getState().setCredentials(scope, cred);
		client.getParams().setAuthenticationPreemptive(true);
		serverAuthent = true;
	}

	//---------------------------------------------------------------------------
	//---
	//--- Private methods
	//---
	//---------------------------------------------------------------------------

	private Element doExecute(HttpMethodBase httpMethod) throws IOException, Exception
	{
		config.setHost(host, port, Protocol.getProtocol(protocol));

		if (useProxy)
			config.setProxy(proxyHost, proxyPort);

		byte[] data = null;

		try
		{
			client.executeMethod(httpMethod);
			data = httpMethod.getResponseBody();

			// HttpClient is unable to automatically handle redirects of entity
			// enclosing methods such as POST and PUT.
			// Get the location header and run the request against it.
			String redirectLocation;
			Header locationHeader = httpMethod.getResponseHeader("location");
			if (locationHeader != null) {
			    redirectLocation = locationHeader.getValue();
			    httpMethod.setPath(redirectLocation);
			    client.executeMethod(httpMethod);
			    data = httpMethod.getResponseBody();
			}
			return Xml.loadStream(new ByteArrayInputStream(data));
		}

		catch(JDOMException e)
		{
			throw new Exception(new String(data, "UTF8"));
		}

		finally
		{
			httpMethod.releaseConnection();

			sentData     = getSentData(httpMethod);
			receivedData = getReceivedData(httpMethod, data);
		}
	}

	//---------------------------------------------------------------------------

	private HttpMethodBase setupHttpMethod() throws UnsupportedEncodingException
	{
		HttpMethodBase httpMethod;

		if (method == Method.GET)
		{
			httpMethod = new GetMethod();

			if (query != null && !query.equals(""))
				httpMethod.setQueryString(query);

			else if (alSimpleParams.size() != 0)
				httpMethod.setQueryString(alSimpleParams.toArray(new NameValuePair[alSimpleParams.size()]));

			httpMethod.addRequestHeader("Accept", "application/xml");
			httpMethod.setFollowRedirects(true);
		}
		else
		{
			PostMethod post = new PostMethod();

				postData = (postParams == null) ? "" : Xml.getString(new Document(postParams));
				post.setRequestEntity(new StringRequestEntity(postData, "application/xml", "UTF8"));

			httpMethod = post;
		}

		httpMethod.setPath(address);
		httpMethod.setDoAuthentication(useAuthent());

		return httpMethod;
	}

	//---------------------------------------------------------------------------

	private String getSentData(HttpMethodBase httpMethod)
	{
        StringBuilder sentData = new StringBuilder(httpMethod.getName()).append(" ").append(httpMethod.getPath());

		if (httpMethod.getQueryString() != null) {
			sentData.append("?"+ httpMethod.getQueryString());
		}

		sentData.append("\r\n");

		for (Header h : httpMethod.getRequestHeaders()) {
			sentData.append(h);
		}

		sentData.append("\r\n");

		if (httpMethod instanceof PostMethod) {
			sentData.append(postData);
		}

		return sentData.toString();
	}

	//---------------------------------------------------------------------------

	private String getReceivedData(HttpMethodBase httpMethod, byte[] response)
	{
		StringBuilder receivedData = new StringBuilder();

		try
		{
			//--- if there is a connection error (the server is unreachable) this
			//--- call causes a NullPointerEx

			receivedData.append(httpMethod.getStatusText()).append("\r\r");

			for (Header h : httpMethod.getResponseHeaders()) {
				receivedData.append(h);
			}

			receivedData.append("\r\n");

			if (response != null) {
				receivedData.append(new String(response, "UTF8"));
			}
		}
		catch (Exception e)
		{
			receivedData.setLength(0);
		}

		return receivedData.toString();
	}

	//---------------------------------------------------------------------------

	private boolean useAuthent() {
		return proxyAuthent||serverAuthent;
	}

	//---------------------------------------------------------------------------
	//---
	//--- Variables
	//---
	//---------------------------------------------------------------------------

	private String  host;
	private int     port;
    private String  protocol;
	private String  address;
	private boolean serverAuthent;
	private String  query;
	private Method  method;
	private boolean useSOAP;
	private Element postParams;
	private boolean useProxy;
	private String  proxyHost;
	private int     proxyPort;
	private boolean proxyAuthent;

	private HttpClient client = new HttpClient();
	private HttpState  state  = new HttpState();
	private Cookie     cookie = new Cookie();

	private HostConfiguration config = new HostConfiguration();

	private ArrayList<NameValuePair> alSimpleParams = new ArrayList<NameValuePair>();

	//--- transient vars

	private String sentData;
	private String receivedData;
	private String postData;
}

//=============================================================================

