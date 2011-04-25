/**
 * Copyright (c) 2006, 2010 Bob Swift
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *              notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *     * The names of contributors may not be used to endorse or promote products
 *           derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Created on Jan 19, 2006 by Bob Swift
 */

package org.swift.common.cli;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import sun.misc.BASE64Encoder;

/**
 * Rest Client
 */
@SuppressWarnings("restriction")
public abstract class AbstractRestClient extends AbstractRemoteClient {

    protected Document doc = null; // parsed result stream.
    protected String resultData = null; // last requests returned data
    protected Hashtable<String, String> cookies = new Hashtable<String, String>();
    protected Set<String> valueExcludeSet = new HashSet<String>();

    public static final String CONTENT_TYPE_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_XML = "application/xml";

    /**
     * Setup for remote service - this must be overridden by subclass
     * 
     * <pre>
     * - Example for Confluence:
     *     ConfluenceSoapServiceServiceLocator serviceLocator = new ConfluenceSoapServiceServiceLocator();
     *     serviceLocator.setConfluenceserviceV1EndpointAddress(address);
     *     service = serviceLocator.getConfluenceserviceV1();
     * </pre>
     */
    @Override
    protected void serviceSetup(String address) throws ClientException, RemoteRestException {
        // none
    }

    /**
     * Login to the remote service - this must be overridden by subclass
     * 
     * <pre>
     * - Example for soap:
     *     String user = getString(&quot;user&quot;);
     *     String password = getString(&quot;password&quot;);
     *     token = service.login(user, password);
     *     if (verbose) {
     *         out.println(&quot;Successful login to: &quot; + address + &quot; by user: &quot; + user);
     *     }
     * </pre>
     */
    @Override
    protected void serviceLogin() throws ClientException, RemoteRestException {
        String user = getString("user");
        String password = getString("password");
        restRequest("login", "?username=" + user + "&password=" + password);

        token = getValue(getLoginTokenKey()); // this token is used on other API calls
        if ("".equals(token)) {
            String errors = getValue(getErrorKey());
            throw new RemoteRestException("Login failed.\n Errors: " + errors);
        }
        if (verbose) {
            out.println("Successful login to: " + address + " by user: " + user + " with token: " + token);
        }
    }

    /**
     * Logout of remote service - this must be overridden by subclass
     * 
     * <pre>
     * - Example for soap:
     *     service.logout(token);
     * </pre>
     */
    @Override
    protected void serviceLogout() throws RemoteRestException {

        if (!"".equals(token)) {
            try {
                restRequest("logout", "");
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Get the key used to retrieve the login token - this can be overridden by subclass if it is different than the default Atlassian value - for example,
     * fisheye uses a different value - Note newer Atlassian rest APIs no longer use this :(
     * 
     * @result key used to retrieve the login token from the response message
     */
    protected String getLoginTokenKey() {
        return "auth";
    }

    /**
     * @return
     */
    protected String getResultData() {
        return resultData;
    }

    /**
     * Get key use in response for an error conditions - this can be overridden by subclass if it is different than the default Atlassian value - for example,
     * fisheye uses error instead of errors
     * 
     * @result key used to retrieve error from response
     */
    protected String getErrorKey() {
        return "errors";
    }

    /**
     * Get any qualifier (postfix) that is added to all command requests to the server - this can be overridden by subclass if it is different than the default
     * Atlassian value - for example, fisheye does not use a qualifier
     * 
     * @result valued added to the command before building the URL
     */
    protected String getActionQualifier() {
        return ".action";
    }

    /**
     * Allow requests to exclude data from being included in output
     * 
     * @param valueExcludeSet
     */
    protected void setValueExclude(final String exclude) {
        valueExcludeSet = new HashSet<String>();
        String list[] = exclude.split(",");
        for (int i = 0; i < list.length; i++) {
            valueExcludeSet.add(list[i].trim());
        }
    }

    /**
     * Add parameters to the parameterList - Options must have a value - Switches do NOT have a value - Note that the long version is expressed as: --xxx value,
     * --xxx "value", or --xxx and that short flag version is expressed as: -x value, -x "value", or -x
     */
    @Override
    protected void addParameters() {

        super.addParameters(); // add all the standard parameters for superclass client

        // For new REST api guidelines. -1 for legacy support (depreciated).
        addIntegerOption("api", "API version. Some requests produce different results based on the api version used. Use 0 for latest.", 0);
        // addOption("api", "API version. Some requests produce different results based on the api version used.", "latest");
    }

    /**
     * HTTP request type
     */
    public enum RequestType {
        GET, PUT, POST, DELETE;

        @Override
        public String toString() {
            return name().toString();
        }
    }

    /**
     * Rest request from a parameter map - parameter map is converted to string suitable for restRequest(String action, String parameter) - values are encoded
     * for UTF-8
     * 
     * @param action requested
     * @param parameters map of key value pairs
     * @return response returned from remote service if it was successful, otherwise an exception
     */
    public void restRequest(final String action, final Map<String, String> parameters) throws ClientException, RemoteRestException {
        restRequest(action, generateParameterString(parameters));
    }

    public void restRequest(final String action, final Map<String, String> parameters, final Map<String, String[]> arrayParameters) throws ClientException,
            RemoteRestException {
        restRequest(action, generateParameterString(parameters) + generateParameterStringFromArray(arrayParameters));
    }

    public void restRequest(final String action, final String parameters) throws ClientException, RemoteRestException {
        restRequest(RequestType.GET, action, parameters, null);
    }

    /**
     * Make a rest request to the server - use the default or user provided server address - automatically append the authentication token - add user provided
     * parameters assumed to be of the form "&parm1=value1&parm2=value2"
     * 
     * @param action requested
     * @param parameters assumed to be of the form "&parm1=value1&parm2=value2" etc...
     * @return response returned from remote service if it was successful, otherwise an exception
     */
    public void restRequest(final RequestType requestType, final String action, final String parameters, final String postData) throws ClientException,
            RemoteRestException {

        String address = jsapResult.getString("server") + getService();
        // automatically add authorization token to each request except for login
        String baseUrl = address + action + getActionQualifier() + getAuthorizationToken(action);
        restRequestWithUrl(requestType, baseUrl, parameters, getErrorKey(), postData);
        return;
    }

    /**
     * Make a rest request to an arbitrary URL adding user provided parameters assumed to be of the form "&parm1=value1&parm2=value2"
     * 
     * @param baseUrl
     * @param parameters assumed to be of the form "&parm1=value1&parm2=value2" etc...
     * @param errorKey for automatic error handling if not null
     * @return response returned from remote service if it was successful, otherwise an exception
     */
    public void restRequestWithUrl(final String baseUrl, final String parameters, final String errorKey) throws ClientException, RemoteRestException {
        restRequestWithUrl(RequestType.GET, baseUrl, parameters, errorKey, null);
    }

    @Deprecated
    public void restRequestWithUrl(RequestType requestType, final String baseUrl, final String parameters, final String errorKey) throws ClientException,
            RemoteRestException {
        restRequestWithUrl(requestType, baseUrl, parameters, errorKey, null);
    }

    /**
     * Make a rest request to an arbitrary URL adding user provided parameters assumed to be of the form "&parm1=value1&parm2=value2"
     * 
     * @param baseUrl
     * @param parameters assumed to be of the form "&parm1=value1&parm2=value2" etc...
     * @param errorKey for automatic error handling if not null
     * @return response returned from remote service if it was successful, otherwise an exception
     */
    @Deprecated
    public void restRequestWithUrl(RequestType requestType, final String baseUrl, final String parameters, final String errorKey, final String postData)
            throws ClientException, RemoteRestException {
        String url = baseUrl + ((parameters != null) ? parameters : "");
        restRequestWithFullUrl(requestType, url, errorKey, postData, CONTENT_TYPE_XML);
    }

    /**
     * Make a rest request to an arbitrary URL adding user provided parameters assumed to be of the form "&parm1=value1&parm2=value2"
     * 
     * @param baseUrl
     * @param parameters assumed to be of the form "&parm1=value1&parm2=value2" etc...
     * @param errorKey for automatic error handling if not null
     * @return response returned from remote service if it was successful, otherwise an exception
     */
    public void restRequestWithUrl(RequestType requestType, final String baseUrl, final Map<String, String> parameters, final String errorKey,
            final String postData) throws ClientException, RemoteRestException {
        restRequestWithFullUrl(requestType, baseUrl + generateParameterString(parameters, baseUrl.contains("?")), errorKey, postData, CONTENT_TYPE_XML);
    }

    public void restRequestWithUrl(RequestType requestType, final String baseUrl, final Map<String, String> parameters, final String errorKey,
            final String postData, final String contentType) throws ClientException, RemoteRestException {
        restRequestWithFullUrl(requestType, baseUrl + generateParameterString(parameters, baseUrl.contains("?")), errorKey, postData, contentType);
    }

    /**
     * Make a rest request to an arbitrary URL adding user provided parameters assumed to be of the form "&parm1=value1&parm2=value2"
     * 
     * @param requestType - get, post, etc...
     * @param urlString - full url
     * @param errorKey for automatic error handling if not null
     * @param postData
     * @param content type
     * @return response returned from remote service if it was successful, otherwise an exception
     */
    public void restRequestWithFullUrl(RequestType requestType, final String urlString, final String errorKey, final String postData, final String contentType)
            throws ClientException, RemoteRestException {

        doc = null; // always null previous request parsed data
        HttpURLConnection connection = null;
        URL url = null;
        try {
            if (getVerbose()) {
                out.println("URL requested: " + urlString);
                out.println("Request type: " + requestType.toString());
                if (requestType == RequestType.POST) {
                    out.println("Post data: " + postData);
                }
            }
            CliUtils.setupForHttps(); // Just in case it is https

            url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            if (!(urlConnection instanceof HttpURLConnection)) { // should always be an http connection
                throw new ClientException("Client error, connection should be an HTTP connection");
            }
            connection = (HttpURLConnection) urlConnection;
            if ((cookies != null) && (cookies.size() > 0)) {
                writeCookies(cookies, connection); // put the saved cookies to this request
                if (getDebug()) {
                    printCookies(connection);
                }
            }
            setConnectionProperties(connection); // allow subclass to set additional properties (like basic authentication for instance)
            connection.setAllowUserInteraction(false);
            connection.setRequestMethod(requestType.toString());

            // Problems with atl_token. This helps JIRA 4.1+ and probably will not hurt others -
            // http://confluence.atlassian.com/display/JIRA/Form+Token+Handling
            connection.setRequestProperty("X-Atlassian-Token", "no-check");

            if (requestType != RequestType.GET) {
                connection.setDoOutput(true);

                if (requestType == RequestType.POST) {
                    String type = (contentType == null) ? CONTENT_TYPE_XML : contentType;
                    // String encodedData = encode(postData);
                    String encodedData = postData;

                    connection.setRequestProperty("Content-Type", type);
                    connection.setRequestProperty("Content-Length", Integer.toString(encodedData.length()));
                    connection.setRequestProperty("Content-Language", "en-US");

                    if (getDebug()) {
                        out.println("Encoded data: " + encodedData);
                        out.println("Content type: " + type);
                        out.println("Content length: " + connection.getRequestProperty("Content-Length"));
                    }
                    DataOutputStream output = new DataOutputStream(connection.getOutputStream()); //
                    // http://www.xyzws.com/Javafaq/how-to-use-httpurlconnection-post-data-to-web-server/139
                    // OutputStream output = connection.getOutputStream();
                    // output.writeBytes(URLEncoder.encode(postData, "UTF-8"));
                    // output.writeBytes(encodedData);

                    // int responseCode = connection.getResponseCode();
                    // String responseMessage = connection.getResponseMessage();
                    // out.println("Response code: " + responseCode + ", response message: " + responseMessage);

                    output.write(encodedData.getBytes());
                    output.close();
                    // out.println("postData written");
                }
            }

            if (requestType != RequestType.DELETE) {

                if (cookies.size() == 0) {
                    addCookiesFromUrl(cookies, connection);
                }
                resultData = streamToString(connection.getInputStream()).trim();

                // MORE debug
                // out.println("Cookies: " + cookies.size());
                // int responseCode = connection.getResponseCode();
                // String responseMessage = connection.getResponseMessage();
                // out.println("Response code: " + responseCode + ", response message: " + responseMessage);

                // if (getDebug()) { // comes with the session cookie
                // out.println("Result data: " + resultData);
                // }
                // End more debug

                parseResultData(resultData); // sets up for multiple getValue functions

                // Check for errors and throw exception if necessary
                if (errorKey != null) {
                    String errors = getValue(getErrorKey());
                    if (!errors.equals("")) {
                        throw new RemoteRestException(errors);
                    }
                }
            } else {
                connection.getInputStream();
                // out.println("no parse data");
            }
        } catch (ParserConfigurationException exception) {
            throw new ClientException("Parsing error on data returned from the server. " + exception.toString());
        } catch (SAXException exception) {
            throw new ClientException("Parsing error on data returned from the server. " + exception.toString());
        } catch (MalformedURLException exception) {
            throw new ClientException("Invalid url: " + url);
        } catch (FileNotFoundException exception) {
            throw new RemoteResourceNotFoundException("Resource does not exist: " + url);
        } catch (IOException exception) {
            throw new ClientException("Invalid request: " + exception.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return;
    }

    /**
     * Allow subclass to set additional properties on the connection - for example, basic authentication
     * 
     * @param connection
     */
    protected void setConnectionProperties(URLConnection connection) {
    }

    /**
     * Set basic authentication on a connection - if needed, use in subclass implementation of setConnectionProperties
     * 
     * @param connection
     */
    protected void setBasicAuthentication(URLConnection connection) {
        if (verbose) {
            out.println("Use basic authentication.");
        }
        String basic = getString("user") + ":" + getString("password");
        String encoding = new BASE64Encoder().encode(basic.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encoding);
        return;
    }

    /**
     * Get URL connection
     * 
     * @param action requested
     * @param parameters assumed to be of the form "&parm1=value1&parm2=value2" etc...
     * @return response returned from remote service if it was successful, otherwise an exception
     */
    public URLConnection getUrlConnection(final String action, final Map<String, String> parameters) throws ClientException, RemoteRestException {

        URLConnection connection;
        String address = jsapResult.getString("server") + getService();
        if (debug) {
            out.println("Server address: " + address);
        }
        try {
            // automatically add authorization token to each request except for login
            URL url = new URL(address + action + getActionQualifier() + getAuthorizationToken(action) + generateParameterString(parameters, "&"));
            if (verbose) {
                out.println(url);
            }
            connection = url.openConnection();
        } catch (MalformedURLException exception) {
            throw new ClientException("Error - invalid url:" + exception.toString());
        } catch (IOException exception) {
            throw new ClientException("Error - invalid request: " + exception.toString());
        }
        return connection;
    }

    /**
     * Get authorization token needed to append to a url based request
     */
    protected String getAuthorizationToken(final String action) {
        String auth = "";
        if (!"login".equals(action)) {
            auth = "?";
            if (getLoginTokenKey() != null) {
                auth += getLoginTokenKey();
                if (token != null) {
                    auth += "=";
                }
            }
            if (token != null) {
                auth += token;
            }
        }
        return auth;
    }

    /**
     * Generate parameter string from parameter map starting with either ? or &
     * 
     * @param parameter map of key values pairs, key is parameter name
     * @param notFirstParameter true these parameters will follow other parameters
     */
    public String generateParameterString(final Map<String, String> parameters, final boolean notFirstParameter) {
        return generateParameterString(parameters, notFirstParameter ? "&" : "?");
    }

    @Deprecated
    // use generateParameterString(parameters, true/false)
    public String generateParameterString(final Map<String, String> parameters) {
        return generateParameterString(parameters, "&");
    }

    /**
     * Generate parameter string from parameter map starting with either ? or & or blank lead character
     * 
     * @param parameter map of key values pairs, key is parameter name
     * @param leadChar is leading character
     */
    public String generateParameterString(final Map<String, String> parameters, final String leadString) {
        StringBuilder output = new StringBuilder();
        String separator = leadString;
        if (parameters != null) {
            Iterator<String> iterator = parameters.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next().trim();
                if (!key.equals("")) {
                    String value = encode(parameters.get(key));
                    output.append(separator);
                    output.append(key).append("=").append(value);
                    separator = "&";
                }
            }
        }
        return output.toString();
    }

    /**
     * Generate parameter string from parameter map
     */
    public String generateParameterStringFromArray(final Map<String, String[]> parameters) {
        StringBuilder output = new StringBuilder();
        if (parameters != null) {
            Iterator<String> iterator = parameters.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String values[] = parameters.get(key);
                for (int i = 0; i < values.length; i++) {
                    String value = encode(parameters.get(key)[i]);
                    output.append("&").append(key).append("=").append(value);
                }
            }
        }
        return output.toString();
    }

    /**
     * Encode for url with UTF-8 - just a convenience function
     * 
     * @param string to encode
     * @return encoded string
     */
    protected String encode(final String string) {
        String result = "";
        try {
            if (debug) {
                out.println("Encode: " + string);
            }
            if (string != null) {
                result = URLEncoder.encode(string, "UTF-8");
            }
        } catch (UnsupportedEncodingException discard) {
            // not going to get here
        }
        return result;
    }

    /**
     * Convert the response stream into a document so it can be processed more than once if necessary
     * 
     * @param response - data returned after a remote request
     * @return void - doc class variable is updated
     * @throws RemoteRestException
     */
    protected void parseResultData(final String response) throws ParserConfigurationException, IOException, SAXException, RemoteRestException {

        if ((response != null) && !response.equals("")) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(response.getBytes());

            doc = builder.parse(inputStream); // parse response into a document
        }
        return;
    }

    /**
     * Get value text identified by key from the response document already available
     * 
     * @param key - to get value for from the response
     * @return value of the requested key from the response, blank is returned if the key is not found
     */
    public String getValue(final String key) throws ClientException {

        StringBuffer output = new StringBuffer();
        if (doc != null) {
            try {
                NodeList list = doc.getElementsByTagName(key);

                for (int i = 0; i < list.getLength(); i++) {
                    if (i > 0) {
                        output.append("\n");
                    }
                    output.append(list.item(i).getTextContent());
                }
            } catch (Exception exception) {
                throw new ClientException("Exception getting values from server response. " + exceptionMessage(exception));
            }
        }
        return output.toString();
    }

    /**
     * Get formatted value text identified by key from the response document already available - expands out children values in standard format
     * 
     * <pre>
     * - example:
     *           build:
     *                    projectName . . . . . . . . . : Test
     *                    ...
     * </pre>
     * 
     * @param key - to get value for from the response
     * @return value of the requested key from the response, blank is returned if the key is not found
     */
    protected String getValueExpandChildren(final String inKey, final String header) throws ClientException {
        String key = inKey;
        if (debug) {
            out.println("getValueExpandChildren with key: " + key + ", header: " + header);
        }

        StringBuffer output = new StringBuffer();
        if (doc != null) {
            try {
                if ((key == null) || "".equals(key)) { // then default to root of document (usually response)
                    key = doc.getDocumentElement().getNodeName();
                }
                NodeList list = doc.getElementsByTagName(key);

                for (int i = 0; i < list.getLength(); i++) {
                    if (header != null) {
                        output.append("\n" + header + ":");
                    }
                    output.append(getValues(list.item(i), 0));

                    if (debug) {
                        out.println("getValueExpandChildren: " + list.item(i));
                    }

                }
            } catch (Exception exception) {
                throw new ClientException("Exception getting values from server response. " + exception.toString());
            }
        }
        return output.toString();
    }

    protected String getValueExpandChildren(final String key) throws ClientException {
        return getValueExpandChildren(key, null);
    }

    /**
     * Get value identified by Node from the response document already available
     * 
     * @param node - to get value for from the response
     * @param output - stream to append data to
     * @return result
     */
    protected String getValues(final Node node, int level) throws ClientException {

        StringBuilder output = new StringBuilder();
        try {
            if (node.getNodeType() != Node.TEXT_NODE) {
                if (node.hasAttributes()) {
                    NamedNodeMap list = node.getAttributes();

                    for (int i = 0; i < list.getLength(); i++) {
                        output.append(getValues(list.item(i), level + 1));
                    }
                }
                if (node.hasChildNodes()) {
                    NodeList list = node.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++) {
                        output.append(getValues(list.item(i), level + 1));
                    }
                }
                String result = output.toString(); // result from children or attributes

                output = new StringBuilder(); // now for results to return

                boolean needHeader = (level > 0);
                boolean haveChildResults = false;
                if ("".equals(result)) { // if we are not getting the children to provide the content, get content at
                    // this level
                    result = node.getTextContent();
                    needHeader = true;
                    haveChildResults = true;
                }
                if (haveChildResults) {
                    String name = node.getNodeName();
                    if (!valueExcludeSet.contains(name)) {
                        if (needHeader) {
                            output.append("\n");
                        }
                        output.append(CliUtils.prettyOutput(node.getNodeName(), result, needHeader ? level : 0));
                    }
                } else {
                    if (needHeader) {
                        output.append("\n");
                        for (int i = 0; i < level; i++) {
                            output.append(CliUtils.INDENT);
                        }
                        output.append(node.getNodeName()).append(":");
                    }
                    output.append(result);
                }
            }
        } catch (Exception exception) {
            throw new ClientException("Exception getting values from server response. " + exceptionMessage(exception));
        }
        return output.toString();
    }

    /**
     * Get value text identified by key from the response document already available
     * 
     * @param key - to get value for from the response
     * @return value of the requested key from the response, blank is returned if the key is not found
     */
    public String getValueForElement(final String key) throws ClientException {

        StringBuffer output = new StringBuffer();
        if (doc != null) {
            try {
                NodeList list = doc.getElementsByTagName(key);

                for (int i = 0; i < list.getLength(); i++) {
                    if (i > 0) {
                        output.append("\n");
                    }
                    output.append(list.item(i).getTextContent());
                }
            } catch (Exception exception) {
                throw new ClientException("Exception getting values from server response. " + exceptionMessage(exception));
            }
        }
        return output.toString();
    }

    /**
     * Get value identified by Node from the response document already available
     * 
     * @param node - to get value for from the response
     * @param output - stream to append data to
     */
    /*
     * OBSOLETE protected boolean appendValues(Node node, StringBuffer output, int level) throws ClientException { boolean wasSomethingAppended = false; try {
     * if (node.getNodeType() != Node.TEXT_NODE) { wasSomethingAppended = true; if ((level > 0) || ((level == 0) && node.hasAttributes())) {
     * output.append("\n"); for (int i = 0; i < level; i++) { output.append("\t"); } output.append(CliUtils.prettyOutput(node.getNodeName()));
     * //output.append(node.getNodeName()) // .append(": "); } if (node.hasAttributes()) { NamedNodeMap list = node.getAttributes(); for (int i = 0; i <
     * list.getLength(); i++) { appendValues(list.item(i), output, level + 1); //output.append("\nAttr:> ") // .append(list.item(i).getNodeValue());
     * //output.append("<"); } } boolean childrenAppended = false; if (node.hasChildNodes()) { NodeList list = node.getChildNodes(); for (int i = 0; i <
     * list.getLength(); i++) { boolean appended = appendValues(list.item(i), output, level + 1); childrenAppended = childrenAppended || appended; } } if
     * (!childrenAppended) { // if we are not getting the children to provide the content, get content at this level output.append(node.getTextContent()); } } }
     * catch (Exception exception) { throw new ClientException("Exception getting values from server response. " + exception.toString()); } return
     * wasSomethingAppended; }
     */

    /**
     * Convert the response stream into a string so it can be processed more than once if necessary
     * 
     * @param stream to process
     * @return result as a string
     */
    protected String streamToString(final InputStream stream) {

        StringBuffer buffer = new StringBuffer();
        Scanner scanner = new Scanner(stream);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (debug) {
                out.println(line);
            }
            buffer.append(line);
        }
        return buffer.toString();
    }

    /**
     * Convert the input stream to bytes
     * 
     * @param stream to process
     * @return result as a byte array
     */
    public byte[] streamToByteArray(final InputStream stream, final int size) throws IOException {

        byte data[] = new byte[size];
        int offset = 0;
        int requestSize = size;
        int length;
        while (requestSize > 0) {
            length = stream.read(data, offset, requestSize);
            offset += length;
            requestSize -= length;
        }
        return data;
    }

    /**
     * Send the Hashtable (cookies) as cookies, and write them to the specified URLconnection
     * 
     * @param urlConn The connection to write the cookies to.
     * @param printCookies Print or not the action taken.
     * @return The urlConn with the all the cookies in it.
     */
    public URLConnection writeCookies(Hashtable<String, String> cookies, final URLConnection urlConn) {
        String cookieString = "";
        Enumeration<String> keys = cookies.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            cookieString += key + "=" + cookies.get(key);
            if (keys.hasMoreElements()) {
                cookieString += "; ";
            }
        }
        urlConn.setRequestProperty("Cookie", cookieString);
        if (debug) {
            out.println("Wrote cookies:\n   " + cookieString);
        }
        return urlConn;
    }

    /**
     * Read cookies from a specified URLConnection, and insert them to the Hashtable The hashtable represents the Cookies.
     * 
     * @param urlConn the connection to read from
     * @param reset Clean the Hashtable or not
     */
    public Hashtable<String, String> addCookiesFromUrl(Hashtable<String, String> cookies, final URLConnection urlConn) {

        int i = 1;
        String hdrKey;
        String hdrString;
        String aCookie;
        while ((hdrKey = urlConn.getHeaderFieldKey(i)) != null) {
            if (hdrKey.equals("Set-Cookie")) {
                hdrString = urlConn.getHeaderField(i);
                if (getDebug()) {
                    out.println("cookie string: " + hdrString);
                }
                StringTokenizer st = new StringTokenizer(hdrString, ",");
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    int index = s.indexOf(";");
                    if (index >= 0) {
                        aCookie = s.substring(0, s.indexOf(";"));
                    } else {
                        aCookie = s;
                    }
                    // aCookie = hdrString.substring(0, s.indexOf(";"));
                    int j = aCookie.indexOf("=");
                    if (j >= 0) {
                        // crowd has duplicate cookies and the second one is the valid one! So comment out following.
                        // if (!cookies.containsKey(aCookie.substring(0, j))) {
                        // if the Cookie do not already exist then when keep it,
                        // you may want to add some logic to update
                        // the stored Cookie instead. thanks to rwhelan
                        cookies.put(aCookie.substring(0, j), aCookie.substring(j + 1));
                        if (verbose) {
                            out.println("Reading Key: " + aCookie.substring(0, j));
                            out.println("        Val: " + aCookie.substring(j + 1));
                        }
                    }
                }
            }
            i++;
        }
        return cookies;
    }

    /**
     * Print elements of a map provided it is not empty
     * 
     * @param map to print
     */
    public void printMap(final Map<String, String> map) {
        if ((map != null) && map.size() > 0) {
            out.println("Map:");
            for (String key : map.keySet()) {
                out.println("\n    " + key + ": " + map.get(key));
            }
        }
    }

    /**
     * Display the current cookies in the URLConnection, searching for the: "Cookie" header This is Valid only after a writeCookies operation.
     * 
     * @param urlConn The URL to print the associates cookies in.
     */
    public void printCookies(final URLConnection urlConn) {
        out.print("Cookies in this URLConnection are:\n   ");
        out.println(urlConn.getRequestProperty("Cookie"));
    }

    /**
     * Display the current cookies in the URLConnection, searching for the: "Cookie" header This is Valid only after a writeCookies operation.
     * 
     * @param urlConn The URL to print the associates cookies in.
     */
    public void viewUrlProperties(final URLConnection urlConn) {
        Map<String, java.util.List<String>> properties = urlConn.getRequestProperties();
        out.println("There are " + properties.size() + " properties for this connection");
        for (String property : properties.keySet()) {
            out.println("Property: " + property);
        }
    }

    /**
     * Add a specific cookie, by hand, to the HastTable of the Cookies
     * 
     * @param key The Key/Name of the Cookie
     * @param val The Calue of the Cookie
     * @return updated hashtable
     */
    public Hashtable<String, String> addCookie(Hashtable<String, String> cookies, final String key, final String value) {
        if (!cookies.containsKey(key)) {
            cookies.put(key, value);
            if (verbose) {
                out.println("Adding Cookie: ");
                out.println("   " + key + " = " + value);
            }
        }
        return cookies;
    }

    /**
     * New standard Rest apis have a version qualifier - handle this automatically
     * 
     * @param request - without version qualifier
     * @return
     */
    protected String getStandardRequest(final String request) {
        return "/" + getApiString() + (request.startsWith("/") ? "" : "/") + request;
    }

    /**
     * Convert integer api identifier to string By using integer, get better control of input values.
     * 
     * @return
     */
    protected String getApiString() {
        Integer number = jsapResult.getInt("api");
        return ((number <= 0) ? "latest" : number.toString());
    }
}