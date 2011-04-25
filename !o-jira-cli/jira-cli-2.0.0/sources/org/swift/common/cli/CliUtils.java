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
 *      Author: Bob Swift
 */

package org.swift.common.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.swift.common.cli.CliClient.ClientException;
import org.swift.common.cli.CliClient.ParameterClientException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Static utilities that need to be used in clients and other helper functions
 * 
 * @author bob
 */
public class CliUtils {

    static protected boolean verbose = false; // Debugging

    /**
     * Constants for prettyOutput
     * 
     * <pre>
     *   label . . . . . . . . . . . . : value
     *     label2  . . . . . . . . . . : value2
     *       label3  . . . . . . . . . : value3
     * </pre>
     */
    static final String INDENT = "  ";
    static final String PAD = "  . . . . . . . . . . . . . . . . . ";
    static final int LABEL_MAX = 30;

    public CliUtils() {
    }

    /**
     * String simpleMatch - case insensitive match of regex with single group
     * 
     * @param string to search in
     * @param regex expression with at least one group, otherwise entire string will be returned on match
     * @return - matched group or blank
     */
    static public String matchRegex(final String string, final String regex) {
        String result = "";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            try {
                result = matcher.group(1); // return first group
            } catch (Exception exception) {
                result = matcher.group(0); // return error indicator if no find group specified
            }
        }
        return result;
    }

    /**
     * Simple Regex match - case insensitive match of regex with multiple groups
     * 
     * @param string to search in
     * @param regex expression
     * @param captureNumber - number of capture groups in regex
     * @param maxEntryCount - maximum number of entries to return in list, 0 lists all
     * @return - matched group array or blank
     */
    static public List<String[]> matchRegex(final String string, final String regex, final int captureNumber, final int maxEntryCount) {
        List<String[]> result = new ArrayList<String[]>();
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(string);
        int count = 1;
        // System.out.println("regex: " + regex + ", string: " + string);

        while (matcher.find()) {
            String entry[] = new String[captureNumber];
            for (int i = 0; i < captureNumber; i++) {
                entry[i] = (matcher.group(i + 1)).trim(); // return the error message
            }
            result.add(entry);
            if ((maxEntryCount > 0) && (++count > maxEntryCount)) {
                break;
            }
        }
        return result;
    }

    /**
     * Copy URL content to file specified in file parameter. If it could be a redirected file, the caller should first use getRedirectedUrl
     * 
     * @param urlString - full urlSting
     * @param fileName - where to put the contents retrieve from the URL - usually getRequiredString("file")
     * @throws ClientException
     */
    @Deprecated
    static public File copyUrlToFile(final String urlString, final String fileName) throws ClientException {
        return copyUrlToFile(urlString, new File(fileName), null);
    }

    static public File copyUrlToFile(final String urlString, final File inFile, final String encoding) throws ClientException {
        try {
            return copyUrlToFile(new URL(urlString).openConnection(), inFile, encoding);
        } catch (IOException exception) {
            if (verbose) {
                exception.printStackTrace();
            }
            throw new ClientException(exception.toString());
        }
    }

    static public File copyUrlToFile(final AbstractRestClient client, final String urlString, final File inFile, final String encoding) throws ClientException {
        try {
            URLConnection connection = new URL(urlString).openConnection();
            client.setConnectionProperties(connection);
            return copyUrlToFile(connection, inFile, encoding);
        } catch (IOException exception) {
            if (verbose) {
                exception.printStackTrace();
            }
            throw new ClientException(exception.toString());
        }
    }

    /**
     * Copy URL content to file specified in file parameter. If it could be a redirected file, the caller should first use getRedirectedUrl
     * 
     * @param urlString - full urlSting
     * @param inFile - where to put the contents retrieve from the URL - usually getRequiredString("file"). If it is a directory, use the url to determine file
     *            name
     * @return file that was written StringBuilder inputBuilder = new StringBuilder(); Reader reader = getReader(input, encoding); try { char[] buffer = new
     *         char[1024]; while (true) { int readCount = reader.read(buffer); if (readCount < 0) { break; } inputBuilder.append(buffer, 0, readCount); } }
     *         finally { try { reader.close(); } catch (Exception ignore) { } } return inputBuilder.toString();
     */
    static public File copyUrlToFile(final URLConnection connection, final File inFile, final String encoding) throws ClientException {

        // System.out.println("copyUrlToFile: " + urlString);
        setupForHttps(); // Just in case it is https

        InputStream in = null;
        FileOutputStream output = null;
        Writer writer = null;
        try {
            URL url = connection.getURL();
            // URL url = new URL(urlString);
            // URLConnection connection = url.openConnection();
            // setConnectionProperties(connection);

            in = connection.getInputStream();

            File file; // = new File(fileName);
            if (inFile.isDirectory()) {
                file = new File(inFile.getPath(), (new File(url.getPath())).getName());
            } else {
                file = inFile;
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            output = new FileOutputStream(file);
            // writer = CliUtils.getWriter(output, encoding);

            byte[] buffer = new byte[4096];
            while (true) {
                int readCount = in.read(buffer, 0, buffer.length);
                if (readCount < 0) {
                    break;
                }
                output.write(buffer, 0, readCount);
            }

            // int ch;
            // while ((ch = in.read()) != -1) {
            // output.write(ch);
            // writer.write(ch);
            // }
            return file;
        } catch (IOException exception) {
            if (verbose) {
                exception.printStackTrace();
            }
            throw new ClientException(exception.toString());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Get the redirected url string for a url - essentially follows the url to get its actual location
     * 
     * @param urlString
     * @return redirected url or the original url if there is no redirect
     * @throws ClientException
     */
    static public String getRedirectedUrl(final String urlString) throws ClientException {

        String result = urlString; // default to return the same as input
        if ((urlString != null) && !urlString.equals("")) {
            try {
                setupForHttps(); // Just in case it is https

                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                String redirect = connection.getHeaderField("Location");

                // System.out.println("urlString: " + urlString + ", redirect: " + redirect);
                // System.out.println("location: " + connection.getHeaderField("location"));

                // Map<String, List<String>> fields = connection.getHeaderFields();
                // System.out.println(fields.toString());

                if (redirect != null) {
                    // System.out.println("Redirect location: " + redirect);
                    result = redirect;
                }
            } catch (IOException exception) {
                throw new ClientException(exception.toString());
            }
        }
        return result;
    }

    /**
     * Setup for HTTPS cases. Need to handle HTTPS requests where url host name does not match session host name. Likely one is a DNS name and the other is
     * direct IP address
     */
    static public void setupForHttps() {
        final HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equals(session.getPeerHost())) {
                    // System.out.println("Warning: URL host: " + urlHostName + " does not match " + session.getPeerHost());
                }
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        // trustAllHttpsCertificates(); // Use if we want to tell JRE to trust any https server
        return;
    }

    /**
     * Escape regex characters in a string - use '\\' since '\\' is java for '\' which is the escape character for regex - use (c|c|c| ... |c) where c is one of
     * the special regex charachters, and we are looking for any of these characters - use ([cccccccccc]) where c is any one of the special characters, (...) is
     * for a capturing group so $1 is defined
     */
    static public String escapeRegex(final String string) {

        // char specialCharacters[] = {'/','.','*','+','?','|','(',')','[',']','{','}','\\'};
        // String specialCharacters = "(\\/|\\.|\\*|\\+|\\?|\\||\\(|\\)|\\[|\\]|\\{|\\}|\\\\)"; // double escape all
        // regex special characters
        String specialCharacters = "([\\/\\.\\*\\+\\?\\|\\(\\)\\[\\]\\{\\}\\\\])"; // double escape all regex special
        // characters
        return string.replaceAll(specialCharacters, "\\\\$1"); // double escape \ since it is a regex special character
        // as well
    }

    /**
     * @param resultSet
     * @return
     * @throws SQLException
     */
    static public String getRowAsString(final ResultSet resultSet) throws SQLException {
        StringBuilder builder = new StringBuilder();
        int columnCount = resultSet.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) { // sql columns start at 1
            builder.append(resultSet.getString(i));
            // System.out.println("i: " + i + ", value: " + resultSet.getString(i));
        }
        return builder.toString();
    }

    /**
     * Create a database connection
     * 
     * @param inDriver
     * @param inUrl
     * @param host
     * @param port
     * @param database
     * @param user
     * @param password
     * @return
     * @throws ClientException
     */
    static public Connection getDatabaseConnection(final String inDriverName, String inUrl, final String host, final String port, final String database,
            final String user, final String password) throws ClientException {
        Connection connection = null;
        String driverName = getDatabaseDriver(inDriverName);
        String url = getDatabaseUrl(inUrl, driverName, host, port, database);
        if (verbose) {
            System.out.println("Get database connection with driver: " + driverName + ", url: " + url);
        }
        // Dynamically load jdbc jars available in the lib/jdbc directory and then get the specific driver
        ClassLoader loader = getJdbcClassLoader(); // will throw exception if null
        try {
            Driver driver = (Driver) Class.forName(driverName, true, loader).newInstance();
            DriverManager.registerDriver(new DelegatingDriver(driver));
            DriverManager.getDriver(url);
            connection = DriverManager.getConnection(url, user, password);
            // System.out.println("Successfully loaded class " + driverName);
        } catch (InstantiationException exception) {
            throw new ClientException("Unexpected exception loading jdbc driver: " + driverName + ". " + exception.getMessage());
        } catch (IllegalAccessException exception) {
            throw new ClientException("Unexpected exception loading jdbc driver: " + driverName + ". " + exception.getMessage());
        } catch (ClassNotFoundException exception) {
            throw new ClientException("Could not load database driver: " + driverName + ". Ensure the jdbc driver is available in the lib/jdbc directory.");
        } catch (SQLException exception) {
            throw new ClientException("Exception getting database connection: " + exception.getMessage());
        }
        return connection;
    }

    /**
     * Get the jdbc driver string based on the input driver. Handles some pre-defined values, otherwise it just returns the input value
     * 
     * @param driver
     * @return
     */
    static public String getDatabaseDriver(final String driver) {
        if (driver.equals("postgresql")) {
            return "org.postgresql.Driver";
        } else if (driver.equals("mysql")) {
            return "com.mysql.jdbc.Driver";
        } else if (driver.equals("mssql")) {
            return "net.sourceforge.jtds.Driver";
        } else if (driver.equals("oracle")) {
            return "oracle.jdbc.driver.OracleDriver";
        } else if (driver.equals("as400")) {
            return "com.ibm.as400.access.AS400JDBCDriver";
        }
        return driver;
    }

    /**
     * Get the database url from other parameters if url is not specified
     * 
     * @param url
     * @param driver
     * @param host
     * @param port
     * @param database
     * @return jdbc url string
     * @throws ClientException
     */
    static public String getDatabaseUrl(final String url, final String driver, final String host, final String port, final String database)
            throws ClientException {
        if (url.trim().equals("")) {
            if (driver.contains("postgresql")) {
                return "jdbc:postgresql://" + host + ":" + (((port == null) || port.equals("")) ? "5432" : port) + "/" + database;
            } else if (driver.contains("mysql")) {
                return "jdbc:mysql://" + host + ":" + "/" + database + "?autoReconnect=true";
            } else if (driver.equals("mssql")) {
                return "jdbc:jtds:sqlserver://" + host + ":" + (((port == null) || port.equals("")) ? "1433" : port) + "/" + database;
            } else if (driver.contains("oracle")) {
                return "jdbc:oracle:thin:@" + host + ":" + (((port == null) || port.equals("")) ? "1521" : port) + ":" + database;
            } else if (driver.contains("as400")) {
                return "jdbc:as400://" + host + ";prompt=false;translate binary=true; extended metadata=true";
            }
            throw new ClientException("Could not default the database connection URL from: " + driver + ". Provide a connection URL.");
        }
        return url;
    }

    /**
     * Are the arrays provided equivalent as sets of objects, ie. each contains all the elements of the other
     * 
     * @param array1
     * @param array2
     * @return
     */
    static public boolean areArraysSetEquivalent(Object array1[], Object array2[]) {
        Set<Object> set1 = new HashSet<Object>(Arrays.asList(array1));
        Set<Object> set2 = new HashSet<Object>(Arrays.asList(array2));
        return set1.equals(set2);
    }

    /**
     * Are the arrays provided equivalent in that each object is equal to the same array position
     * 
     * @param array1
     * @param array2
     * @return
     */
    static public boolean areArraysEquivalent(Object array1[], Object array2[]) {
        boolean match = (array1.length == array2.length);

        for (int i = 0; match && (i < array1.length); i++) {
            match = array1[i].equals(array2[i]);
        }
        return match;
    }

    /**
     * Get a reader respecting the encoding requested. Handle exceptions.
     * 
     * @param in
     * @param encoding
     * @return reader
     * @throws ClientException
     */
    static public Reader getReader(final InputStream in, String encoding) throws ClientException {
        if ((encoding != null) && !encoding.equals("")) {
            validateEncoding(encoding); // client exception if not valid
            try {
                return new InputStreamReader(in, encoding);
            } catch (UnsupportedEncodingException exception) {
                // handled in validate
            }
        }
        return new InputStreamReader(in);
    }

    /**
     * Get a writer respecting the encoding requested. Handle exceptions.
     * 
     * @param in
     * @param encoding
     * @return writer
     * @throws ClientException
     */
    static public Writer getWriter(final OutputStream out, String encoding) throws ClientException {
        if ((encoding != null) && !encoding.equals("")) {
            validateEncoding(encoding); // client exception if not valid
            try {
                return new OutputStreamWriter(out, encoding);
            } catch (UnsupportedEncodingException exception) {
                // handled in validate
            }
        }
        return new OutputStreamWriter(out);
    }

    /**
     * Create parent directories
     * 
     * @param file to create parents for
     */
    static public void createParentDirectories(final File file) {
        String directoryName = file.getParent();
        if (directoryName != null) {
            File directory = new File(directoryName);
            if (!directory.exists()) {
                directory.mkdirs(); // creates all directories necessary
            }
        }
        return;
    }

    /**
     * Validate the encoding provided and provide meaningful failure exception if not valid
     * 
     * @param encoding - null or blank are considered valid
     * @throws ClientException
     */
    static public void validateEncoding(final String encoding) throws ClientException {
        if ((encoding != null) && !encoding.equals("")) {
            try {
                Charset.forName(encoding);
            } catch (Exception exception) {
                throw new ParameterClientException("Invalid encoding: " + encoding + ". Valid encodings and aliases are: " + getValidEncodings());
            }
        }
    }

    /**
     * Provide a string of supported encodings and aliases
     * 
     * @return string with new line for each encoding followed by comma separated list of aliases
     */
    static public String getValidEncodings() {
        StringBuilder result = new StringBuilder();
        SortedMap<String, Charset> charsets = Charset.availableCharsets();

        for (String name : Charset.availableCharsets().keySet()) {
            Charset charset = charsets.get(name);
            result.append("\n  " + charset);
            for (String alias : charset.aliases()) {
                result.append(", " + alias);
            }
        }
        return result.toString();
    }

    /**
     * Clean out invalid XML characters according to http://www.w3.org/TR/xml11/#charsets. Invalid characters will cause problems with the remote API when
     * saving page source and similar.
     * 
     * <pre>
     * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
     * any Unicode character, excluding the surrogate blocks, FFFE, and FFFF.
     * </pre>
     * 
     * @param text
     * @return filtered text with invalid characters removed
     */
    static public String removeInvalidXmlChars(final String text) {
        // return text.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\xD7FF\\xE000-\\xFFFD\\x10000-x10FFFF]", "");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == 0x09) || (c == 0x0A) || (c == 0x0D) || ((c >= 0x20) && (c <= 0xD7FF)) || ((c >= 0xE000) && (c <= 0xFFFD))
                    || ((c >= 0x10000) && (c <= 0x10FFFF))) {
                builder.append(c);
            } else {
                // System.out.println("ignore char: " + Character.getNumericValue(c));
            }
        }
        return builder.toString();
    }

    static public String getExtension(final String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1);
    }

    /**
     * Generate a separated string from a list of strings
     * 
     * @param list - list of strings
     * @param inSeparator - separator (defaulting to comma separated)
     * @return separated string
     */
    static public String listToSeparatedString(final List<String> list, final String inSeparator) {

        String separator = (inSeparator == null) ? ", " : inSeparator; // default to comma separated
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {

            if (i != 0) {
                output.append(separator);
            }
            output.append(list.get(i));

        }
        return output.toString();
    }

    static public String listToSeparatedString(final List<String> list) {
        return listToSeparatedString(list, null); // standard comma separated
    }

    /**
     * Walk the node tree to the depth requested looking for a node with given value
     * 
     * @param nodeList - node list to start with
     * @param names - array of node names to look for - match nodes if name at same depth match or it is null
     * @param value - node value to look for
     * @param depth - how deep to look, must be exact, 0 will find first node with value specified by names[0]
     */
    static public Node findNodeWithValue(final NodeList nodeList, final String names[], final String value, final int depth) {
        Node result = null;
        if (nodeList != null) {
            for (int i = 0; (i < nodeList.getLength()) && (result == null); i++) {
                Node node = nodeList.item(i);
                printNodeInformation(node, 1);
                if ((names[depth] == null) || node.getNodeName().equals(names[depth])) {
                    if (depth > 0) {
                        if (node.hasChildNodes()) {
                            result = findNodeWithValue(node.getChildNodes(), names, value, depth - 1);
                        }
                    } else {
                        if (value.equals(node.getTextContent())) {
                            result = node;
                        }
                    }
                    // System.out.println("find node with value - node: " + node.getNodeName() + ", depth: " + depth + ", result: " + result);
                }
            }
        }
        return result;
    }

    /**
     * Walk the node tree to the depth requested looking for a node with node text or attribute name[0]
     * 
     * @param node - node to start with
     * @param names - array of node/attribute names to look for - match nodes if name at same depth match or it is null
     * @param depth - how deep to look, must be exact, 0 will find first node with value in list
     * @param isAttribute - true if depth 0 value to retrieve is an attribute, otherwise it is the node text that is returned
     */
    static public String findValueForNode(final Node node, final String names[], final int depth, final boolean isAttribute) {
        String result = null;
        if (node != null) {
            if (depth > 0) {
                if (node.hasChildNodes()) {
                    NodeList nodeList = node.getChildNodes();
                    for (int i = 0; (i < nodeList.getLength()) && (result == null); i++) {
                        result = findValueForNode(nodeList.item(i), names, depth - 1, isAttribute);
                    }
                }
                // System.out.out.println("find attribute for node: " + node.getNodeName() + ", depth: " + depth + ", result: " + result);
            } else {
                if (isAttribute) {
                    if (node.hasAttributes()) {
                        result = node.getAttributes().getNamedItem(names[0]).getNodeValue();
                    }
                } else {
                    result = node.getTextContent();
                }
            }
        }
        return result;
    }

    /**
     * Walk the node tree to the depth requested looking for a node with node text or attribute name[0]
     * 
     * @param node - node to start with
     * @param names - array of node/attribute names to look for - match nodes if name at same depth match or it is null
     * @param depth - how deep to look, must be exact, 0 will find first node with value in list
     * @param isAttribute - true if depth 0 value to retrieve is an attribute, otherwise it is the node text that is returned
     */
    static public List<String> findValueListForNode(final Node node, final String names[], final int depth, final boolean isAttribute) {
        List<String> result = new ArrayList<String>();
        if (node != null) {
            if (depth > 0) {
                if (node.hasChildNodes()) {
                    NodeList nodeList = node.getChildNodes();
                    for (int i = 0; (i < nodeList.getLength()); i++) {
                        for (String value : findValueListForNode(nodeList.item(i), names, depth - 1, isAttribute)) {
                            result.add(value);
                        }
                    }
                }
                // System.out.out.println("find attribute for node: " + node.getNodeName() + ", depth: " + depth + ", result: " + result);
            } else {
                if (isAttribute) {
                    if (node.hasAttributes()) {
                        Node item = node.getAttributes().getNamedItem(names[0]);
                        if (item != null) {
                            String value = node.getAttributes().getNamedItem(names[0]).getNodeValue();
                            if (value != null) {
                                result.add(value);
                            }
                        }
                    }
                } else {
                    result.add(node.getTextContent());
                }
            }
        }
        return result;
    }

    /**
     * Walk the node tree to the depth requested looking for a node with node text or attribute name[0]
     * 
     * @param node - node to start with
     * @param names - array of node/attribute names to look for - match nodes if name at same depth match or it is null
     * @param depth - how deep to look, must be exact, 0 will find first node with value in list
     * @param isAttribute - true if depth 0 value to retrieve is an attribute, otherwise it is the node text that is returned
     */
    static public List<String> findValueListForNodeList(final NodeList nodeList, final String names[], final int depth, final boolean isAttribute) {
        List<String> result = new ArrayList<String>();
        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                for (String value : findValueListForNode(nodeList.item(i), names, depth, isAttribute)) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    /**
     * Get rest parameter string if the value is not blank. Only the &name is displayed if value is null
     * 
     * @param name
     * @param value
     * @return string like "&name=value
     */
    static public String getRestParameter(final String name, final String value) {
        return value.equals("") ? "" : "&" + name + ((value == null) ? "" : "=" + value);
    }

    /**
     * Print node information to depth requested. Walk node tree printing information at each level.
     * 
     * @param node - node to start from
     * @param depth - how deep to go reporting information
     */
    static public void printNodeInformation(final Node node, final int depth) {
        if ((node != null)) { // && !node.getNodeName().startsWith("#")) {
            // System.out.out.println("node name: " + node.getNodeName() + ", value: " + node.getTextContent());
            // out.println("node name: " + node.getNodeName() + ((node.getTextContent() != null) ? ", value: " + node.getTextContent() : ""));
            // out.println("node name: " + node.getNodeName() + ((node.getNodeValue() != null) ? ", value: " + node.getNodeValue() : ""));
            if (node.hasAttributes()) {
                NamedNodeMap attributes = node.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    // System.out.out.println("attribute: " + attributes.item(i).getNodeName() + ", value: " + attributes.item(i).getNodeValue());
                }
            }
            if (node.hasChildNodes()) {
                NodeList list = node.getChildNodes();
                for (int i = 0; i < list.getLength(); i++) {
                    if (depth > 0) {
                        printNodeInformation(list.item(i), depth - 1);
                    }
                }
            }
        }
    }

    /**
     * Pretty output for label - "label  . . . . . . . . . . . . : value"
     * 
     * @param label - first part
     * @return formatted string
     */
    static public String prettyOutput(String label) {
        return prettyOutput(label, "");
    }

    /**
     * Pretty output for label and value - "label  . . . . . . . . . . . . : value"
     * 
     * @param label - first part
     * @param value - part after the :
     * @return formatted string
     */
    static public String prettyOutput(String label, String value) {
        return prettyOutput(label, value, 0);
    }

    /**
     * Pretty output for label and value - "label  . . . . . . . . . . . . : value"
     * 
     * @param label - first part
     * @param value - part after the :
     * @param level - level of indent, 0 is no indent
     * @return formatted string
     */
    static public String prettyOutput(String label, String value, int level) {
        int padLength = 0;
        int padStart = 0;
        int labelMax = LABEL_MAX - (level * INDENT.length());

        if (label.length() < labelMax) {
            padLength = labelMax - label.length();
            if (padLength % 2 == 1) {
                padLength++;
                padStart = 1;
            }
        }
        StringBuilder output = new StringBuilder(80);
        for (int i = 0; i < level; i++) {
            output.append(INDENT);
        }
        output.append(label).append(PAD.substring(padStart, padLength)).append(": ").append(value);
        return output.toString();
    }

    /**
     * Pick out a value from separated list based on position.
     * 
     * @formatter:off - Position 1 is characters before first separator - Position 2 is characters between first and second separators etc ...
     * @formatter:on
     * @param key
     * @param position
     * @param separator
     * @return
     */
    static public String getSeparatedValue(final String key, final int position, final char separator) {
        if ((key != null) && (position > 0)) {
            int index1 = 0;
            int index2 = -1;
            int count;
            for (count = 1; count <= position; count++) {
                index1 = index2 + 1;
                index2 = key.indexOf(separator, index1);
                // System.out.println("count: " + count + ", index1: " + index1 + ", index2: " + index2);
                if (index2 < 0) {
                    index2 = key.length();
                    if (count < position) {
                        index1 = key.length() + 1; // did not find value
                    }
                    break;
                }
            }
            if (index1 < key.length()) {
                // System.out.println("index1: " + index1 + ", index2: " + index2 + ", key subset: " + key.substring(index1, index2));
                return key.substring(index1, index2);
            }
        }
        return "";
    }

    /**
     * Specific version of getSepartedValue for most commonly used separator
     * 
     * @param key
     * @param position
     * @return
     */
    static public String getDashSeparatedValue(final String key, final int position) {
        return getSeparatedValue(key, position, '-');
    }

    /**
     * Get class loader for loading JDBC drivers
     * 
     * @return
     * @throws ClientException
     */
    static ClassLoader getJdbcClassLoader() throws ClientException {
        URLClassLoader loader = null;
        // String directoryName = new File(CliUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "/../../jdbc";
        String pathToPrimaryJar = CliUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath().replace("\\", "/");
        int index = pathToPrimaryJar.lastIndexOf('/');
        String pathToPrimaryDirectory = (index >= 0) ? pathToPrimaryDirectory = pathToPrimaryJar.substring(0, index) : "";

        File directory = new File(pathToPrimaryDirectory + "/jdbc");
        if (directory.exists()) {
            if (verbose) {
                System.out.println("jdbc directory: " + directory.getAbsolutePath());
            }
            String[] fileList = directory.list(new FilterByExtension("jar")); // get all jars in directory
            if (fileList.length > 0) {
                try {
                    URL[] urlList = new URL[fileList.length];
                    for (int i = 0; i < fileList.length; i++) {
                        File jarFile = new File(directory, fileList[i]);
                        urlList[i] = new URL("jar:file://" + jarFile.getAbsolutePath() + "!/");
                        // System.out.println("urlList: " + urlList[i].toString());
                    }
                    loader = URLClassLoader.newInstance(urlList, ClassLoader.getSystemClassLoader());
                } catch (MalformedURLException exception) {
                    throw new ClientException("Error constructing url path based on JDBC jars from " + directory.getAbsolutePath());
                }
            }
        }
        if (loader == null) {
            throw new ClientException("No JDBC jars could be loaded from " + directory.getAbsolutePath());
        }
        return loader;
    }

    /**
     * Filter file list by extension.
     */
    static public class FilterByExtension implements FilenameFilter {
        String extension;

        public FilterByExtension(final String extension) {
            this.extension = "." + extension;
        }

        public boolean accept(File dir, String name) {
            return name.endsWith(extension);
        }
    }

    /**
     * For dynamically loading JDBC drivers http://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location
     * 
     * @author bob
     */
    static public class DelegatingDriver implements Driver {
        private final Driver driver;

        public DelegatingDriver(Driver driver) throws ClientException {
            if (driver == null) {
                throw new ClientException("JDBC driver must not be null.");
            }
            this.driver = driver;
        }

        public Connection connect(final String url, final Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        public boolean acceptsURL(final String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }
    }
}
