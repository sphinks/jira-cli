/*
 * Copyright (c) 2009 Bob Swift.
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
 */

/*
 *  Created on: Sep 13, 2009
 *      Author: bob
 */

package org.swift.common.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.swift.common.cli.AbstractRemoteClient.RemoteRestException;
import org.swift.common.cli.CliClient.ClientException;

public abstract class AbstractPluginHelper {

    protected AbstractRestClient client;
    protected final String PLUGIN_REQUEST_STRING = "/admin/viewplugins.action"; // default

    protected final String pluginExchangeUrl = "https://plugins.atlassian.com";
    protected final String pluginDetails = "/plugin/details/"; // plugin exchange details

    protected PrintStream out = System.out;

    public boolean getVerbose() {
        return client.getVerbose();
    }

    public boolean getDebug() {
        return client.getDebug();
    }

    public AbstractPluginHelper(AbstractRestClient client) {
        this.client = client;
    }

/**
     * Regex values for finding application specific data for plugin information
     *
     * <pre>
     * Examples:
     *   if (key.equals("plugin")) {
     *       result = "plugins.action\\?pluginKey=([^\"]*)\"[^>]*>([^<]*)<"; // gets key and plugin name
     *   } else if (key.equals("plugin-version")) {
     *       result = "Plugin Version[^:]*:\\s*([^<]*)<";
     *   } else if (key.equals("plugin-vendor")) {
     *       result = "Vendor[^>]*>[^>]*>\\s*([^<]*)<";
     *   } else if (key.equals("plugin-enabled")) {
     *       result = ">\\s*This plugin is (disabled).\\s*[^<]*<";
     *   }
     * </pre>
     * @param key
     * @return regex string appropriate for the key specified
     */
    public abstract String getPluginRegex(final String key);

    /**
     * Allow client to override this if product varies from the standard
     * 
     * @param pluginRequestString
     */
    public String getPluginRequestString() {
        return PLUGIN_REQUEST_STRING;
    }

    /**
     * GetPluginList - get all plugins for this installation
     * 
     * @param plugin - for subsetting the list - plugin keys must contain this value
     * @param outputFormat - default and 2 (that includes plugin exchange information (long running)
     * @return result message
     */
    public String getPluginList(final String plugin, final int outputFormat, final int count) throws ClientException, RemoteRestException,
            java.rmi.RemoteException {

        // http://ubuntu2:8202/admin/viewplugins.action?os_username=automation&os_password=automation

        Map<String, String> parameters = new HashMap<String, String>(); // this handles encoding etc...
        client.restRequest(getPluginRequestString(), parameters); // handles the error case with exception
        List<String[]> list = getValueList("plugin", (plugin.equals("") ? count : Integer.MAX_VALUE));
        // System.out.println("list: " + list.size() + ", plugin: " + plugin + ", count: " + count);
        StringBuffer result = new StringBuffer();

        // Use sorted map of plugin key to plugin information so result is ordered by plugin key
        Map<String, Map<String, String>> keyToPluginInfo = new TreeMap<String, Map<String, String>>();

        for (String[] entry : list) {
            String key = entry[0];
            String name = entry[1];
            // System.out.println("key: " + key + ", name: " + name);

            // hack - not sure why this extra data is injected on one of our systems, but this seems to solve the problem. TODO investigate further
            if (key.contains(";jsession")) {
                key = key.substring(0, key.indexOf(";jsession"));
            }
            Map<String, String> pluginInfo = getPluginInfo(key); // maps attributes to values for a specific plugin
            pluginInfo.put("name", name);
            keyToPluginInfo.put(key, pluginInfo);
        }

        int counter = 0;
        for (String key : keyToPluginInfo.keySet()) {
            if (key.contains(plugin)) {
                Map<String, String> pluginInfo = keyToPluginInfo.get(key);
                counter++;
                result.append("\n\"" + pluginInfo.get("name") + "\", \"" + key + "\", \"" + pluginInfo.get("version") + "\", \"" + pluginInfo.get("vendor")
                        + "\", " + pluginInfo.get("enabled"));

                if (outputFormat == 2) {
                    Map<String, String> pluginExchangeInfo = getPluginExchangeInfo(key, null);
                    if (pluginExchangeInfo != null) {
                        result.append(", " + pluginExchangeInfo.get("exchange-url") + ", " + pluginExchangeInfo.get("exchange-latest-version") + ", "
                                + pluginExchangeInfo.get("exchange-latest-version-download"));
                    }
                }
            }
            if (counter >= count) {
                break;
            }
        }
        if (result.length() > 0) {
            if (outputFormat == 2) {
                result.insert(0, ", Plugin Exchange Link, Latest Version, Latest Version Download Link");
            }
            result.insert(0, "Name, Key, Version, Vendor, Status");
        }
        String message = counter + " plugins in list";
        return client.standardFinish(message, result.toString(), client.getString("encoding")); // handle file parameter in standard way
    }

    /**
     * Get plugin download url and optionally download the file to the directory or file specified
     * 
     * @param key - plugin key
     * @param version - external version name, null to get latest version
     * @param fileName - name of file (or directory). Used if not blank to store the download
     * @return string representing the url for downloading the version provided
     * @throws ClientException if the version is not found in the plugin exchange
     * @throws RemoteException
     * @throws IOException
     */
    public String getPluginDownload(final String key, final String version, final String fileName) throws ClientException, RemoteRestException, RemoteException {

        String urlString;
        Map<String, String> valueMap = getPluginExchangeInfo(key, version);

        if (valueMap != null) {
            String download = ((version == null) || version.equals("") ? "exchange-latest-version-download" : "exchange-version-download");
            urlString = valueMap.get(download);
            urlString = CliUtils.getRedirectedUrl(urlString);
            if ((fileName != null) && !fileName.equals("")) {
                CliUtils.copyUrlToFile(urlString, fileName);
            }
        } else {
            throw new java.rmi.RemoteException("Not able to find information for plugin: " + key
                    + ((version == null) || version.equals("") ? "" : " with version '" + version + "'"));
        }
        return urlString;
    }

    /**
     * Get information for a plugin identified by plugin key
     * 
     * @param key - plugin key
     * @return version
     */
    public Map<String, String> getPluginInfo(final String key) throws ClientException, RemoteRestException, java.rmi.RemoteException {

        // http://ubuntu2:8202/admin/viewplugins.action?os_username=automation&os_password=automation

        Map<String, String> parameters = new HashMap<String, String>(); // this handles encoding etc...

        parameters.put("pluginKey", key);

        client.restRequest(getPluginRequestString(), parameters); // handles the error case with exception

        Map<String, String> valueMap = new HashMap<String, String>();

        valueMap.put("version", getValue("plugin-version"));
        valueMap.put("enabled", getValue("plugin-enabled"));
        valueMap.put("vendor", getValue("plugin-vendor"));

        return valueMap;
    }

    /**
     * Get plugin exchange information by key and version
     * 
     * <pre>
     * Here are the basics:
     * - plugin key is used to search plugin exchange site to get plugin id and url for detail page
     * - detailed plugin page has latest version and latest version download
     * - detailed plugin page also has list of earlier versions with link to version detail page
     * - version detail page has a link to the download
     * </pre>
     * 
     * @param key - plugin key (exact match)
     * @param version - plugin version name or null
     * @return map of information or null if plugin not found
     * @throws RemoteRestException
     * @throws RemoteException
     */
    public Map<String, String> getPluginExchangeInfo(final String key, final String version) throws ClientException, RemoteRestException, RemoteException {

        // https://plugins.atlassian.com/search/with?q=org.swift.confluence.cache&product=confluence

        Map<String, String> parameters = new HashMap<String, String>(); // this handles encoding etc...
        // parameters.put("product", "confluence");

        String requestUrl = pluginExchangeUrl + "/search/with?q=" + key;

        client.restRequestWithUrl(requestUrl, client.generateParameterString(parameters, true), null); // handles the error case with exception

        Map<String, String> valueMap = null; // indicates not found

        // System.out.println("getPluginExchangeInfo: " + key);

        if (getValue("exchange-notfound").equals("")) { // found a plugin

            valueMap = new HashMap<String, String>();
            String pluginId = getValue("exchange-id");
            valueMap.put("exchange-id", pluginId);
            valueMap.put("exchange-url", pluginExchangeUrl + pluginDetails + pluginId);

            // Now get plugin details

            requestUrl = pluginExchangeUrl + pluginDetails + pluginId;

            client.restRequestWithUrl(requestUrl, null, null); // handles the error case with exception
            String latestVersion = getValue("exchange-latest-version");
            valueMap.put("exchange-latest-version", latestVersion);
            valueMap.put("exchange-latest-version-download", getValue("exchange-latest-version-download"));

            if ((version != null) && !version.equals("")) {
                valueMap.put("exchange-version", version);
                String versionUrl = getValue("exchange-version:" + version);
                // System.out.println("versionUrl: " + versionUrl);

                if (versionUrl.equals("")) { // version not found
                    throw new java.rmi.RemoteException("Version " + version + " not found for plugin " + key);
                } else {
                    // need to go to that versions detail page to get the download link
                    int index = versionUrl.lastIndexOf('=');
                    if (index >= 0) {
                        String versionId = versionUrl.substring(index + 1);
                        requestUrl = pluginExchangeUrl + versionUrl;
                        // System.out.println("index: " + index + ", versionId: " + versionId + ", request Url: " + requestUrl);
                        client.restRequestWithUrl(requestUrl, null, null); // handles the error case with exception
                        valueMap.put("exchange-version-download", getValue("exchange-version-download:" + versionId));
                        // System.out.println("version download: " + valueMap.get("exchange-version-download"));
                    }
                    // https://plugins.atlassian.com/server/1.0/download/pluginVersion/7402
                    // https://plugins.atlassian.com/plugin/details/156?versionId=7402
                }
            }
        }
        return valueMap;
    }

    /**
     * Get value text identified by key from the response document already available
     * 
     * <pre>
     * Example - plugin
     *     &lt;a href=&quot;viewplugins.action?pluginKey=com.atlassian.confluence.plugins.attachmentExtractors&quot; &gt;Attachment Extractors&lt;/a&gt;&lt;br&gt;
     * Example - plugin version
     *     &lt;strong&gt;Plugin Version&lt;/strong&gt;: 1.5&lt;br&gt;
     * Example - plugin version
     *     &lt;b&gt;Vendor&lt;/b&gt;: &lt;a href=&quot;http://www.adaptavist.com/&quot;&gt;Adaptavist.com Ltd&lt;/a&gt;&lt;br&gt;
     * Example - latest version
     *     Latest Version:  &lt;/dt&gt;   &lt;dd&gt;3.1.0&lt;/dd&gt;    &lt;th&gt;  &lt;a href=&quot;/plugin/details/152?versionId=1044&quot;&gt;3.1.0&lt;/a&gt;      &lt;/th&gt;
     * Example - download url for version
     *     &lt;dt&gt;&lt;a href=&quot;/plugin/details/152?versionId=1043&quot;&gt;3.0.2&lt;/a&gt;&lt;/dt&gt;
     * 
     * </pre>
     * 
     * @param key - to get value for from the response
     * @return value of the requested key from the response, blank is returned if the key is not found
     */
    public String getValue(final String key) throws ClientException {

        String result = "";
        String resultData = client.getResultData();
        if (key.equals(client.getErrorKey())) {
            result = CliUtils.matchRegex(resultData, "<span class=\"" + key + "\">([^<]*)</span>.*"); // stop on any character not <)
        } else if (key.equals("plugin-version")) {
            result = CliUtils.matchRegex(resultData, getPluginRegex("plugin-version"));
        } else if (key.equals("plugin-vendor")) {
            result = CliUtils.matchRegex(resultData, getPluginRegex("plugin-vendor"));
        } else if (key.equals("plugin-enabled")) {
            result = CliUtils.matchRegex(resultData, getPluginRegex("plugin-enabled"));
            if (result.equals("")) {
                result = "enabled";
            }

            // Plugin exchange values - these are the same for any CliUtil
        } else if (key.equals("exchange-id")) {
            result = CliUtils.matchRegex(resultData, "=\"/plugin/details/([0-9]*)\"");
        } else if (key.equals("exchange-notfound")) {
            result = CliUtils.matchRegex(resultData, "(There are no plugins matching your search criteria)");
        } else if (key.equals("exchange-latest-version")) {
            int index = resultData.indexOf("Latest");
            if (index >= 0) {
                result = CliUtils.matchRegex(resultData.substring(index), "<dd>([^<]*)</dd>");
            }
        } else if (key.equals("exchange-latest-version-download")) {
            String regex = "=\"(https://[^\"]*/download/pluginVersion/[0-9]*)\"";
            // System.out.println("regex: " + regex);
            result = CliUtils.matchRegex(resultData, regex);
        } else if (key.startsWith("exchange-version")) { // look for exchange-version: <version> and exchange-version-download: <version>
            int index = key.indexOf(":") + 1;
            String version = key.substring(index).trim();
            String regex;
            if (key.contains("download")) {
                regex = "=\"(https://[^\"]*/download/pluginVersion/" + version + ")\"";
            } else {
                regex = "=\"(/plugin/details/[0-9]*\\?versionId=[0-9]*)\">" + version + "<";
            }
            // System.out.println("request: " + key + ", version: " + version + ", regex: " + regex);
            result = CliUtils.matchRegex(resultData, regex);
        }

        // <dt><a href="/plugin/details/152?versionId=1043">3.0.2</a></dt>

        // if (verbose) {
        // int find = confluenceResponse.indexOf(key);
        // if (find > 0) {
        // out.println("found in: " + confluenceResponse.substring(find - 20, find + 50));
        // }
        // out.println("getValue: " + key + ", result: " + result + ", confluenceResponse size: " + confluenceResponse.length());
        // out.println("confluenceResponse: " + confluenceResponse);
        // }
        return result.trim();
    }

    /**
     * Get list of value arrays identified by key from the response document already available
     * 
     * <pre>
     * Example - plugin
     * &lt;a href=&quot;viewplugins.action?pluginKey=com.atlassian.confluence.plugins.attachmentExtractors&quot; &gt;Attachment Extractors&lt;/a&gt;&lt;br&gt;
     * </pre>
     * 
     * @param key - to get value for from the response
     * @return value of the requested key from the response, blank is returned if the key is not found
     */
    public List<String[]> getValueList(final String key, final int maxEntryCount) throws ClientException {

        List<String[]> result = null;
        String resultData = client.getResultData();
        if (key.equals("plugin")) {
            result = CliUtils.matchRegex(resultData, getPluginRegex("plugin"), 2, maxEntryCount); // plugin key, plugin
            // name
        }
        // if (verbose) {
        // int find = confluenceResponse.indexOf(key);
        // if (find > 0) {
        // out.println("found in: " + confluenceResponse.substring(find - 20, find + 50));
        // }
        // out.println("getValue: " + key + ", result: " + result + ", confluenceResponse size: " + confluenceResponse.length());
        // out.println("confluenceResponse: " + confluenceResponse);
        // }
        return result;
    }

    // TODO

    /**
     * Copy URL content to file specified in file parameter.
     * 
     * @param urlString - full urlSting
     * @param fileName - where to put the contents retrieve from the URL - usually getRequiredString("file")
     */
    public File copyUrlToFile(final String urlString, final String fileName) throws java.rmi.RemoteException, IOException, MalformedURLException,
            ClientException {

        if (getVerbose()) {
            System.out.println("copyUrlToFile: " + urlString);
        }
        setupForHttps(); // Just in case it is https

        InputStream in = null;
        FileOutputStream output = null;
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            // rws modification for redirected url
            String redirect = connection.getHeaderField("Location");
            if (redirect != null) {
                System.out.println("Redirect location: " + redirect);
                url = new URL(redirect);
                connection = url.openConnection();
            }

            in = connection.getInputStream();

            // in = url.openStream();

            // TODO
            // System.out.println("url getFile: " + url.getFile());
            // System.out.println("content type: " + connection.getContentType());
            // System.out.println("properties: " + connection.getRequestProperties().toString());

            File file = new File(fileName);
            if (file.isDirectory()) {
                file = new File(file.getPath(), (new File(url.getFile())).getName());
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            output = new FileOutputStream(file);

            int ch;
            while ((ch = in.read()) != -1) {
                output.write(ch);
            }
            return file;
        } catch (IOException exception) {
            if (getVerbose()) {
                exception.printStackTrace();
            }
            throw exception;
        } finally {
            if (in != null) {
                in.close();
            }
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Setup for HTTPS cases. Need to handle HTTPS requests where url host name does not match session host name. Likely one is a DNS name and the other is
     * direct IP address
     */
    public void setupForHttps() {
        final HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                if (getVerbose() && (!urlHostName.equals(session.getPeerHost()))) {
                    System.out.println("Warning: URL host: " + urlHostName + " does not match " + session.getPeerHost());
                }
                return true;

            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
        // If we want to tell JRE to trust any https server, we could do the following:
        // trustAllHttpsCertificates();
        return;
    }

}
