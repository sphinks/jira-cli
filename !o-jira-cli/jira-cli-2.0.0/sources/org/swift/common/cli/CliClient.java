/**
 * Copyright (c) 2006, 2009 Bob Swift
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
 * Created May 2008 by Bob Swift
 */

package org.swift.common.cli;

// see http://sourceforge.net/projects/jsap
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.swift.common.cli.AbstractRemoteClient.RemoteRestException;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.IDMap;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;

/**
 * Cli Client - CLI client that defines a standard implementation for command line clients
 */
public class CliClient {
    protected SimpleJSAP jsap;
    protected JSAPResult jsapResult;
    protected boolean debug = false;
    protected boolean verbose = false;
    protected List<Parameter> parameterList = null;
    protected String actionHelp;

    protected PrintStream out = System.out;
    protected PrintStream err = System.err;

    /**
     * Exit code enumeration - these are returned to the command processor to provide proper external error handling
     */
    public enum ExitCode {

        SUCCESS(0), // Normal exit
        CLIENT_EXCEPTION(-1), // Problem discovered on the client
        PARAMETER_PROBLEM(-2), // Parameter parsing discovered a problem with user provided parameters
        REMOTE_EXCEPTION(-3), // The remote request caused a problem
        CLIENT_SPECIFIC_EXCEPTION(-4), // Optional client defined exception other than others
        FAILURE(-99); // Program error - an unexpected problem with the program

        private int value;

        ExitCode(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /**
     * Simple constructor
     */
    public CliClient() {
    }

    public void setVerbose(boolean verbose) {
        // out.println("set verbose: " + verbose);
        this.verbose = verbose;
    }

    public boolean getVerbose() {
        return verbose;
    }

    public void setDebug(boolean debug) {
        // out.println("set debug: " + debug);
        this.debug = debug;
        if (debug) {
            setVerbose(true); // if we want debug, verbose message should come out too!
        }
    }

    public boolean getDebug() {
        return debug;
    }

    /**
     * Get a new instance of this client - override in subclass
     * 
     * @return new client instance
     */
    public CliClient getNewClient() {
        return new CliClient();
    }

    /**
     * Command entry point - use as model for subclass implementation
     * 
     * @param args - command line arguments
     */
    public static void main(String[] args) {
        if ((args == null) || (args.length == 0)) {
            args = new String[] {"--help"};
        }

        ExitCode code = new CliClient().doWork(args);

        System.exit(code.value());
    }

    /**
     * Define parameters, parse the command line, setup logging, and process the request
     * 
     * @param args - command line arguments
     * @return - appropriate exit code based on processing
     */
    public ExitCode doWork(final String[] args) {

        try {
            parameterList = new ArrayList<Parameter>(); // initialize
            actionHelp = getActionHelp();
            addParameters(); // this will get all the parameters from subclasses to the parameterList variable
            jsap = new SimpleJSAP(getClientName(), getGeneralHelp(), parameterList.toArray(new Parameter[parameterList.size()]));
            jsap.setScreenWidth(132);

            jsapResult = jsap.parse(args);
            if (jsap.messagePrinted()) {
                return (ExitCode.PARAMETER_PROBLEM);
            }
        } catch (Exception exception) {
            out.println("Exception: " + exceptionMessage(exception));
            return (ExitCode.FAILURE);
        }
        if (jsapResult.userSpecified("verbose")) { // allows setting default for privately constructed cli class
            setVerbose(jsapResult.getBoolean("verbose"));
        }
        if (jsapResult.userSpecified("debug")) {
            setDebug(jsapResult.getBoolean("debug"));
        }
        setupLogging(); // setup log4j properties
        if (debug) {
            printJsapParameters();
        }
        return process(); // now process the request
    }

    /**
     * Do work from a string instead of a string array for testcase convenience
     * 
     * @param args - string of command line arguments
     * @return - appropriate exit code based on processing
     */
    public ExitCode doWork(final String args) {
        String list[] = splitCsvData(args, " +", "\""); // split on one or more blanks, allow double quote
        for (int i = 0; i < list.length; i++) {
            list[i] = stripQuotes(list[i], '"');
        }
        return doWork(list);
    }

    /**
     * Setup for request processing and then handle the request - this should be overridden by subclass for specific handling
     * 
     * @return - appropriate exit code based on processing
     */
    protected ExitCode process() {
        try {
            setup();
        } catch (Exception exception) {
            err.println("An error occurred during setup.\nCause: " + exceptionMessage(exception));
            if (debug) {
                exception.printStackTrace();
            }
            return ExitCode.CLIENT_EXCEPTION;
        }
        try {
            String message = handleRequest();
            if (message != null) {
                out.println(message);
            }
        } catch (ParameterClientException exception) {
            err.println(exceptionMessage(exception));
            if (debug) {
                exception.printStackTrace();
            }
            return ExitCode.PARAMETER_PROBLEM;
        } catch (ClientSpecificException exception) {
            err.println(exceptionMessage(exception));
            if (debug) {
                exception.printStackTrace();
            }
            return ExitCode.CLIENT_SPECIFIC_EXCEPTION;
        } catch (Exception exception) {
            err.println("An error occurred handling request.\nCause: " + exceptionMessage(exception));
            if (debug) {
                exception.printStackTrace();
            }
            return ExitCode.CLIENT_EXCEPTION;
        }
        return ExitCode.SUCCESS;
    }

    /**
     * Perform any setup or initialization prior to handling the request - for example, remote login should be done here for remote clients - should be
     * overridden in subclass - no parameters or returns
     */
    protected void setup() {
        return;
    }

    /**
     * Enumerate all the valid actions - all command actions must be listed in this enumeration - this needs to be overridden in a subclass - borrow switch
     * technique from http://www.xefer.com/2006/12/switchonstring
     */
    protected enum Action {

        EXAMPLE,

        NOTFOUND; // the last one

        private static Action toAction(String string) {
            try {
                return valueOf(string);
            } catch (Exception discard) {
                return NOTFOUND;
            }
        }
    }

    /**
     * Map the user request to implementing code - this should be overridden by subclass for specific handling
     * 
     * @return - the output from the request that will be put on the out stream
     */
    protected String handleRequest() throws InvalidActionClientException, Exception {

        String action = getString("action").toUpperCase();

        switch (Action.toAction(action)) {

        case EXAMPLE:
            return ("Example message"); // normally this would be a function call

        case NOTFOUND:
        default:
            throw new InvalidActionClientException("Invalid action specified: " + action + ".  Use --help for more information.");
        }
    }

    /**
     * Map the action to the action help text - this should be overridden by subclass for specific handling
     * 
     * @action - action name
     * @return - the formatted help text for the action
     */
    protected String getActionHelp(Action action) {

        switch (action) {

        case EXAMPLE:
            return formatActionHelp(action.name(), "Example description.", "req1, req2", "opt1");

        case NOTFOUND:
        default:
            return "";
        }
    }

    /**
     * Get the name of the client that will appear in help text - for example: "confluence" as a command name - override this method to insert specific client
     * name if desired, other blank is used
     * 
     * @return client name
     */
    protected String getClientName() {
        return "";
    }

    /**
     * Add parameters to the parameterList - Options must have a value - Switches do NOT have a value - Note that the long version is expressed as: --xxx value,
     * --xxx "value", or --xxx and that short flag version is expressed as: -x value, -x "value", or -x
     * 
     * @param - actionHelp - formatted help for the action parameter
     */
    protected void addParameters() {
        // Options id type default required? short flag(-) long flag (--) help text
        parameterList.add(new FlaggedOption("action", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'a', "action", actionHelp));
        parameterList.add(new FlaggedOption("file", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'f', "file",
                "Path to file based content or result output"));
        addOption("encoding", "Character encoding (character set) for text based file content - must be an encoding supported by your JAVA platform.");

        // Switches id short flag(-) long flag (--) help text
        parameterList.add(new Switch("debug", JSAP.NO_SHORTFLAG, "debug", "Requests debug output, example: stack traces."));
        parameterList.add(new Switch("verbose", 'v', "verbose", "Requests verbose output."));
        parameterList.add(new Switch("quiet", JSAP.NO_SHORTFLAG, "quiet", "Limit some output messages."));

        addIntegerOption("outputFormat", "Specify output format for an action.", 1);
        // addLongOptionWithDefault("id", "Numeric id of an item.", null); // only add if more that one client needs this

        addOption("sql", "SQL select statement used to generate a run script.");
        addOption("driver", "JDBC driver class or predefined value: postgresql, mysql, mssql, oracle, or db2400. Required for SQL actions.");
        addOption("url", "Action specific setting. Example: Database access url for SQL actions. Optional when host is provided.");
        addOption("host", "Database host server for SQL actions. Not used if url is provided.", "localhost");
        addOption("port", "Database host port for SQL actions. Optional, defaults to database default. Not used if url is provided.");
        addOption("database", "Database name is required for SQL actions.");

        addSwitch("continue", "Continue processing even after errors are encountered."); // primarily for the run action
        addSwitch("simulate", "Simulate running actions. Log the action that would be taken."); // primarily for the run sql action
    }

    /**
     * Get help text for each client action
     * 
     * @param actionName - the name of the action as known to the user
     * @param actionDescription - information on what the action does
     * @param requiredParameters - comma separated list of parameters required by this action
     * @param optionalParameters - comma separated list of parameters that are optional for this action
     */
    protected String formatActionHelp(String actionName, String actionDescription, String requiredParameters, String optionalParameters) {
        StringBuilder help = new StringBuilder(256);
        help.append("\n" + actionName + " - " + actionDescription);
        if ((requiredParameters != null) && (!"".equals(requiredParameters.trim()))) {
            help.append("\n\t Required parameters: " + requiredParameters);
        }
        if ((optionalParameters != null) && (!"".equals(optionalParameters.trim()))) {
            help.append("\n\t Optional parameters: " + optionalParameters);
        }
        return help.toString();
    }

    /**
     * Get help text for each client action
     * 
     * @actionName - the name of the action as known to the user
     * @actionDescription - information on what the action does
     * @param requiredParameters - list of parameters required by this action
     * @param optionalParameters - list of parameters that are optional for this action
     */
    protected String formatActionHelp(String actionName, String actionDescription, String[] requiredParameters, String[] optionalParameters) {
        return formatActionHelp(actionName, actionDescription, listToCommaSeparatedString(requiredParameters), listToCommaSeparatedString(optionalParameters));
    }

    /**
     * Get help text for each client action
     * 
     * @return all the help text that is defined
     */
    protected String getActionHelp() {
        StringBuilder help = new StringBuilder(4096);
        help.append("Requested operation to perform. Valid actions (not case sensitive) are:\n");

        // for (Action action : Action.values()) {
        // help.append(getActionHelp(action));
        // }
        // Action[] list = getActionValues();
        // for (int i = 0; i < list.length; i++) {
        // help.append(getActionHelp(list[i]));
        // }
        appendActionHelpValues(help); // this is overridden at subclass

        help.append("\n");
        return help.toString();
    }

    /**
     * Append action help values to the string buffer - this is used so that the Action enum for this class is used
     * 
     * @param string buffer to append the help text
     * @return general help for this client
     */
    protected void appendActionHelpValues(StringBuilder help) {
        for (Action action : Action.values()) {
            help.append(getActionHelp(action));
        }
    }

    /**
     * Use StringBuilder version of this function now we support 1.5
     */
    // protected void appendActionHelpValues(StringBuffer help) {
    // }

    /**
     * Get command general help
     * 
     * @return general help for this client
     */
    protected String getGeneralHelp() {
        return "\tProvides capability to make requests." + "\n\tRequired parameters: action, ..." + "\n\tOptional parameters: ..."
                + "\n\tOther required and optional parameters depending on action requested." + "";
    }

    /**
     * Add a standard option
     * 
     * @param name - option name
     * @param help - help text
     */
    protected void addOption(final String name, final String help) {
        addOption(name, help, JSAP.NO_DEFAULT);
    }

    protected void addOption(final String name, final String help, final String defaultValue) {
        parameterList.add(new FlaggedOption(name, JSAP.STRING_PARSER, defaultValue, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, name, help));
    }

    /**
     * Add option with a default value
     * 
     * @param name - option name
     * @param help - help text
     * @param default - default value
     */
    @Deprecated
    protected void addOptionWithDefault(final String name, final String help, final String defaultValue) {
        parameterList.add(new FlaggedOption(name, JSAP.STRING_PARSER, defaultValue, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, name, help));
    }

    /**
     * Add integer option
     * 
     * @param name - option name
     * @param help - help text
     * @param default - default value
     */
    protected void addIntegerOption(final String name, final String help) {
        parameterList.add(new FlaggedOption(name, JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, name, help));
    }

    protected void addIntegerOption(final String name, final String help, final int defaultValue) {
        parameterList.add(new FlaggedOption(name, JSAP.INTEGER_PARSER, Integer.toString(defaultValue), JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, name, help));
    }

    /**
     * Add float option
     * 
     * @param name
     * @param help
     */
    protected void addFloatOption(final String name, final String help) {
        parameterList.add(new FlaggedOption(name, JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, name, help));
    }

    protected void addFloatOption(final String name, final String help, final Float defaultValue) {
        parameterList.add(new FlaggedOption(name, JSAP.FLOAT_PARSER, Float.toString(defaultValue), JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, name, help));
    }

    /**
     * Add integer option with a default value
     * 
     * @param name - option name
     * @param help - help text
     * @param default - default value
     */
    @Deprecated
    protected void addIntegerOptionWithDefault(final String name, final String help, final int defaultValue) {
        parameterList.add(new FlaggedOption(name, JSAP.INTEGER_PARSER, Integer.toString(defaultValue), JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, name, help));
    }

    /**
     * Add long option
     * 
     * @param name - option name
     * @param help - help text
     * @param default - default value
     */
    protected void addLongOption(final String name, final String help) {
        parameterList.add(new FlaggedOption(name, JSAP.LONG_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, name, help));
    }

    protected void addLongOption(final String name, final String help, final Long defaultValue) {
        parameterList.add(new FlaggedOption(name, JSAP.LONG_PARSER, (defaultValue == null) ? JSAP.NO_DEFAULT : Long.toString(defaultValue), JSAP.NOT_REQUIRED,
                JSAP.NO_SHORTFLAG, name, help));
    }

    /**
     * Add a standard switch
     * 
     * @param name - switch name
     * @param help - help text
     */
    protected void addSwitch(final String name, final String help) {
        parameterList.add(new Switch(name, JSAP.NO_SHORTFLAG, name, help));
    }

    /**
     * Run a script of actions for the client CLI. Need CTRL-D to end standard input on MAC and Unix
     * 
     * @param continueOnError - true to keep going even if there a command failed
     * @param quiet - true to reduce the number of messages logged
     * @param simulate - true to only simulate running commands, log what would have been run
     * @return standard message
     * @throws ClientException - if one or more actions fail
     * @throws RemoteRestException
     */
    public String run(final boolean continueOnError, final boolean quiet, final boolean simulate) throws ClientException, RemoteRestException {

        int successCount = 0;
        int failCount = 0;
        String fileName = getString("file");
        String fileMessage;

        if (!fileName.equals("")) {
            File file = new File(fileName);
            fileMessage = "from file: " + file.getAbsolutePath();
            if (file.exists()) {
                try {
                    BufferedReader input = new BufferedReader(new FileReader(file));
                    try {
                        String line = input.readLine();
                        while (line != null) {
                            if (doSingleRunLine(line, quiet, simulate)) {
                                successCount++;
                            } else {
                                failCount++;
                                if (!continueOnError) {
                                    break; // stop further processing
                                }
                            }
                            line = input.readLine(); // get next line
                        }
                    } finally {
                        input.close();
                    }
                } catch (IOException exception) {
                    if (debug) {
                        exception.printStackTrace(err);
                    }
                    throw new ClientException("Error reading " + fileMessage + "\n" + exceptionMessage(exception));
                }
            } else {
                throw new ClientException("File not found: " + file.getAbsolutePath());
            }
        } else {
            fileMessage = "from standard input";
            try {
                int in = 0;
                // int lastIn = -1; // input will be terminated if 2 end of line indicators are read consecutively
                while (in >= 0) {
                    StringBuilder input = new StringBuilder();
                    // read until get end, line feed, carriage return
                    while (((in = System.in.read()) != -1) && (in != 10) && (in != 13)) {
                        input.append((char) in);
                    }
                    if (input.length() > 0) {
                        String line = input.toString().trim();
                        if (!line.equals("")) {
                            if (doSingleRunLine(line, quiet, simulate)) {
                                successCount++;
                            } else {
                                failCount++;
                                if (!continueOnError) {
                                    break; // stop further processing
                                }
                            }
                        }
                    } else {
                        // if (lastIn == in) { // 2 consecutive end line indicators means we get out
                        // break;
                        // }
                        // lastIn = in;
                    }
                }
            } catch (Exception discard) {
            }
        }

        String successMessage = ((successCount == 0) && (failCount != 0)) ? "" : successCount + " actions were successful ";
        if (failCount > 0) {
            throw new ClientException(failCount + " actions failed" + ((successCount > 0) ? ", " : " ") + successMessage + fileMessage);
        }
        return (quiet ? "" : "\n") + "Run completed successfully. " + successMessage + fileMessage;
    }

    /**
     * Run a script of actions for the client CLI
     * 
     * @param inLine - line to process
     * @param quiet - true to limit messages that are logged
     * @param simulate - just show the commands to be run, without running it
     * @return standard message
     * @throws RemoteException
     * @throws ClientException - if one or more actions fail
     * @throws RemoteRestException
     */
    public boolean doSingleRunLine(final String inLine, final boolean quiet, final boolean simulate) throws RemoteException, ClientException,
            RemoteRestException {
        boolean result = true;

        String line = inLine.trim();
        if (line.equals("") || line.startsWith("#")) {
            if (!quiet) {
                out.println(line); // Write out comment line as is
            }
        } else {
            ExitCode exitCode;
            if (simulate) {
                out.println("\nSimulate: " + line);
                exitCode = ExitCode.SUCCESS;
            } else {
                if (!quiet) {
                    out.println("\nRun: " + line);
                }
                exitCode = processRunLine(line);
            }
            result = (exitCode == ExitCode.SUCCESS);
            if (!result) {
                out.println(exitCode + " running: " + line);
            }
        }
        return result;
    }

    /**
     * Process line for running a script
     * 
     * @throws RemoteRestException
     * @throws ClientException
     * @throws java.rmi.RemoteException
     */
    protected ExitCode processRunLine(final String line) throws java.rmi.RemoteException, ClientException, RemoteRestException {
        CliClient runner = getNewClient();
        runner.setVerbose(verbose); // carry over these setting as default for runner
        runner.setDebug(debug);
        return runner.doWork(line);
    }

    /**
     * Run a script of actions for the client CLI from SQL
     * 
     * @return standard message
     * @throws ClientException - if one or more actions fail
     * @throws RemoteRestException
     * @throws java.rmi.RemoteException
     */
    public String runFromSql(final String inSql, final String driver, final String url, final String host, final String port, final String database,
            final String user, final String password, final boolean continueOnError, final boolean quiet, final boolean simulate) throws ClientException,
            RemoteRestException, java.rmi.RemoteException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection;
        try {
            connection = CliUtils.getDatabaseConnection(driver, url, (host.equals("") ? new URL(getString("server")).getHost() : host), port, database, user,
                    password);
        } catch (MalformedURLException e) {
            throw new ClientException("Could not determine host from server parameter. Provide a valid host parameter.");
        }

        int successCount = 0;
        int failCount = 0;

        String sql = inSql;
        String fileMessage = "";
        if ("".equals(sql)) {
            String file = getRequiredString("file");
            fileMessage = "from file: " + new File(file).getAbsolutePath();
            sql = getFileAsString(file, getString("charset"));
        }

        try {
            statement = connection.prepareStatement(sql);
            if (statement.execute()) {
                resultSet = statement.getResultSet();

                while (resultSet.next()) {
                    String line = CliUtils.getRowAsString(resultSet);
                    // int columnCount = resultSet.getMetaData().getColumnCount();
                    // for (int i = 1; i <= columnCount; i++) { // sql columns start at 1
                    // System.out.println("i: " + i + ", value: " + resultSet.getString(i));
                    // }
                    /*
                     * ExitCode exitCode; if (simulate) { out.println(line); exitCode = ExitCode.SUCCESS; } else { if (!quiet) { out.println("\nRun: " + line);
                     * } exitCode = processRunLine(line); }
                     */
                    // if (exitCode == ExitCode.SUCCESS) {
                    if (doSingleRunLine(line, quiet, simulate)) {
                        successCount++;
                    } else {
                        failCount++;
                        if (!continueOnError) {
                            break; // stop further processing
                        }
                    }
                }
            }
        } catch (SQLException exception) {
            throw new ClientException("SQL reported: " + exception.toString());
        } finally {
            try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }

        String successMessage = ((successCount == 0) && (failCount != 0)) ? "" : successCount + " actions were successful ";
        if (failCount > 0) {
            throw new ClientException(failCount + " actions failed" + ((successCount > 0) ? ", " : " ") + successMessage + fileMessage);
        }
        return "\nRun completed successfully. " + successMessage + fileMessage;
    }

    /*
     * Helper functions
     */

    /**
     * Use the best message format available - I don't know why getMessage doesn't always give a detailed message :(
     */
    protected String exceptionMessage(final Exception exception) {
        String result = exception.getLocalizedMessage();
        return (result != null) ? result : exception.toString();
    }

    /**
     * Convert a list of strings into a comma separated string
     * 
     * @param list
     * @return Common separated string
     */
    public String listToCommaSeparatedString(String[] list) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < list.length; i++) {
            if (i != 0) {
                result.append(", ");
            }
            result.append(list[i]);
        }
        return result.toString();
    }

    /**
     * Get contents of a file as a string
     * 
     * @param fileName
     * @param encoding - null or blank to use platform default
     * @return string representing file contents
     * @throws ClientException
     */
    public String getFileAsString(final String fileName, final String encoding) throws ClientException {
        return getFileAsString(new File(fileName), encoding);
    }

    /**
     * Get contents of a file respecting user request for specific character encoding
     * http://illegalargumentexception.blogspot.com/2009/05/java-rough-guide-to-character-encoding.html#javaencoding_autodetect
     * 
     * @param file - file to read
     * @param charset - null or blank to use platform default
     * @return string representing file contents
     * @throws ClientException
     */
    public String getFileAsString(final File file, final String encoding) throws ClientException {
        StringBuilder inputBuilder = new StringBuilder();
        Closeable stream;
        try {
            InputStream in = new FileInputStream(file);
            stream = in;
            try {
                Reader reader = CliUtils.getReader(in, encoding);
                stream = reader;

                char[] buffer = new char[4096];
                while (true) {
                    int readCount = reader.read(buffer);
                    if (readCount < 0) {
                        break;
                    }
                    inputBuilder.append(buffer, 0, readCount);
                }
            } catch (IOException exception) {
                if (debug) {
                    exception.printStackTrace(err);
                }
                throw new ClientException("Error reading file: " + file.getAbsolutePath() + "\n" + exceptionMessage(exception));
            } finally {
                try {
                    stream.close();
                } catch (Exception ignore) {
                }
            }
        } catch (FileNotFoundException exception) {
            throw new ClientException("File not found: " + file.getAbsolutePath());
        }
        return inputBuilder.toString();
    }

    /**
     * Get contents of a file as byte array
     * 
     * @param filename - file to read
     * @return file contents as a byte array
     */
    public byte[] getFileAsBytes(final File file) throws ClientException {
        byte[] data;
        if (file.exists()) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(file);
                data = new byte[(new Long(file.length())).intValue()];
                int bytesRead = stream.read(data);
                if (verbose) {
                    out.println("File: '" + file.getAbsolutePath() + "' had " + bytesRead + " bytes read.");
                }
            } catch (IOException exception) {
                if (debug) {
                    exception.printStackTrace(err);
                }
                throw new ClientException("Error reading file: " + file.getAbsolutePath() + "\n" + exceptionMessage(exception));
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        } else {
            throw new ClientException("File not found: " + file.getAbsolutePath());
        }
        return data;
    }

    public byte[] getFileAsBytes(final String fileName) throws ClientException {
        return getFileAsBytes(new File(fileName));
    }

    /**
     * Write TEXT data to file specified by file parameter - use the other form of this method for writing binary data
     * 
     * @param data - byte array of data to be written
     * @param fileName - file to write data to
     * @param encoding - how the file data should be encoded
     */
    protected void writeToFile(final String string, final String fileName, final String encoding) throws ClientException {
        writeToFile(string, new File(fileName), encoding);
    }

    /**
     * Write TEXT data to file specified by file parameter - use the other form of this method for writing binary data
     * 
     * @param data - string to be written
     * @param file - file to write data to, it will be created if it doesn't exist
     * @param encoding - how the file data should be encoded
     */
    protected void writeToFile(final String string, final File file, final String encoding) throws ClientException {
        CliUtils.validateEncoding(encoding); // validate before opening/creating file
        try {
            CliUtils.createParentDirectories(file); // make sure all parents exist as well
            file.createNewFile(); // make sure file created if it did not exist before
            OutputStream out = new FileOutputStream(file);
            Closeable stream = out;
            try {
                Writer writer = CliUtils.getWriter(out, encoding);
                stream = writer;
                writer.write(string);
            } finally {
                stream.close();
            }
        } catch (FileNotFoundException exception) {
            // should be handled by createNewFile
            throw new ClientException("Unexpected exception: " + exception.toString());
        } catch (ClientException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ClientException("Error writing to file: " + file.getAbsoluteFile() + ". Exception is: " + exception.toString());
        }
    }

    /**
     * Write TEXT data to file specified by file parameter - use the other form of this method for writing non-text data
     * 
     * @param data - byte array of data to be written
     * @param fileName - file to write data to
     */
    protected void OLDwriteToFile(byte[] data, String fileName) throws ClientException {
        FileWriter out = null;
        try {
            out = new FileWriter(fileName);
            for (int i = 0; i < data.length; i++) {
                out.write(data[i]);
            }
        } catch (Exception exception) {
            throw new ClientException("Error writing to file: " + fileName);
        } finally {
            try {
                out.close();
            } catch (Exception ignore) {
            }
        }
        return;
    }

    /**
     * Write byte data to file specified by file parameter
     * 
     * @param data - byte array of data to be written
     * @param fileName - file to write data to
     * @param mimeType - type of content, text data or not - if mimeType is not a text type, data is writing as binary data
     * @param encoding - how the file data should be encoded only for mimeType of text
     */
    protected void writeToFile(byte[] data, String fileName, String mimeType, final String encoding) throws ClientException {
        writeToFile(data, new File(fileName), mimeType, encoding);
    }

    /**
     * Write byte data to file specified by file parameter
     * 
     * @param data - byte array of data to be written
     * @param file - file to write data to
     * @param mimeType - type of content, text data or not - if mimeType is not a text type, data is writing as binary data
     * @param encoding - how the file data should be encoded only for mimeType of text
     */
    protected void writeToFile(byte[] data, File file, String mimeType, final String encoding) throws ClientException {
        if ((mimeType != null) && "text/".equalsIgnoreCase(mimeType.substring(0, 5))) {
            writeToFile(new String(data), file, encoding);
        } else {
            CliUtils.createParentDirectories(file); // make sure all parents exist as well
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                for (int i = 0; i < data.length; i++) {
                    out.write(data[i]);
                }
            } catch (Exception exception) {
                throw new ClientException("Error writing to file: " + file.getAbsoluteFile());
            } finally {
                try {
                    out.close();
                } catch (Exception ignore) {
                }
            }
        }
        return;
    }

    /**
     * Splits the data into meaningful strings to produce a 1 dimensional array - similar to how String split works with addition of ignoring embedded
     * delimiters within quoted strings - almost does everything but leaves quotes in place - these need to be handled String patternString =
     * "(?:^|,)(?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))";
     * 
     * @param data - string to split
     * @param delimiter - what to split on
     * @param quote - what is used to quote strings that may contain delimiter
     * @return string array
     */
    protected static String[] splitCsvData(String data, String delimiter, String quote) {
        final String eolString = "\n"; // Confluence uses this
        String patternString = "(?:^|" + delimiter + ")(?=(?:[^" + quote + "]*" + quote + "[^" + quote + "]*" + quote + ")*(?![^" + quote + "]*" + quote + "))";

        Pattern pattern;
        String tempDelimiter = delimiter;
        if (delimiter.equals("\\|")) { // remove the escaping now we have the pattern defined
            tempDelimiter = "|";
        }
        if (delimiter.length() == 1) {
            pattern = Pattern.compile(eolString + delimiter);
            Matcher matcher = pattern.matcher(data);
            data = matcher.replaceAll(eolString + " " + tempDelimiter);
        }

        // now do main parsing
        pattern = Pattern.compile(patternString, Pattern.MULTILINE);
        String[] values = pattern.split(data);

        // this parse technique has some anomalies, so repair those now
        if (values.length > 1) { // if more than one entry found, then first entry is blank and needs to be ignored
            // shift the array to the right
            String[] values2 = new String[values.length - 1];
            System.arraycopy(values, 1, values2, 0, values.length - 1);
            return values2;
        } else { // single entries (no delimiter) are ok as is
            return values;
        }
    }

    /**
     * Simple function to remove single quotes
     * 
     * @param string - to remove quotes
     * @return string stripped of single quotes
     */
    public String stripQuotes(final String string) {
        return stripQuotes(string, '\'');
    }

    /**
     * Reverse action for quoteString by removing quotes and removing internal doubled quotes. Nothing is done if string is not quoted
     * 
     * @param string
     * @param quote character - either ' or "
     * @return string with leading, trailing, and double quotes remove
     */
    public String stripQuotes(final String inString, final char quote) {
        String string = inString.trim();
        if ((string.length() > 1) && (string.charAt(0) == quote) && (string.charAt(string.length() - 1) == quote)) {
            StringBuilder result = new StringBuilder(string.length());
            boolean previousWasQuote = false; // deal with multiple quotes in a row
            for (int i = 1; i < string.length() - 1; i++) {
                if (previousWasQuote || ((string.charAt(i) != quote) || (i == string.length() - 1) || (string.charAt(i + 1) != quote))) {
                    result.append(string.charAt(i));
                    previousWasQuote = false;
                } else {
                    previousWasQuote = true;
                }
            }
            return result.toString();
        }
        return string;
    }

    /**
     * Test for single quoted string (after trim)
     * 
     * @param string - to test
     * @return true if trimmed string was quoted
     */
    public boolean isQuoted(final String string) {
        return isSurrounded(string, '\'');
    }

    /**
     * Simple function to remove quotes from a quoted string - trimmed string must start and end with quote character - reverse escaped quotes (doubled)
     * 
     * @param string
     * @param quote - character to remove from start and end
     * @return string with quote characters removed
     */
    // public String stripQuotes(final String inString, final char quote) {
    // String string = inString.trim();
    // if (isSurrounded(inString, quote)) {
    // String quoteString = Character.toString(quote);
    // return string.substring(1, string.length() - 1).replace(quoteString + quoteString, quoteString);
    // }
    // return string;
    // }

    /**
     * Test for surrounded string (after trim)
     * 
     * @param string - to test
     * @param surrounding character
     * @return true if the trimmed string was surrounded by character provided
     */
    public boolean isSurrounded(final String inString, final char character) {
        String string = inString.trim();
        return ((string.length() > 1) && (string.charAt(0) == character) && (string.charAt(string.length() - 1) == character));
    }

    /**
     * Quote a string by escaping or doubling up internal quotes - defaults to single quote, double
     * 
     * @param string to quote, if null returns blank
     * @param quote character (usually ' or ")
     * @param beforeQuote character added before any quote character found within the string (usually the same as quote, but could be an escape character)
     * @result string starting and ending with quote character with internal quote characters handled
     */
    public String quoteString(final String string, final char quote, final char beforeQuote) {
        StringBuilder result = new StringBuilder();
        if (string != null) {
            result.append(quote);
            for (int i = 0; i < string.length(); i++) {
                char aChar = string.charAt(i);
                if (aChar == quote) {
                    result.append(beforeQuote);
                }
                result.append(aChar);
            }
            result.append(quote);
        }
        return result.toString();
    }

    /**
     * Single quote string with standard doubling
     * 
     * @param string
     * @return single quoted string
     */
    public String quoteString(final String string) {
        return quoteString(string, '\'', '\'');
    }

    /**
     * Double quote string with standard doubling
     * 
     * @param string
     * @return double quoted string
     */
    public String doubleQuoteString(final String string) {
        return quoteString(string, '"', '"');
    }

    /**
     * Verify required parameter - throws exception if parameter is not provided. USE getRequiredString if it is a String parameter!!!
     * 
     * @param id - the parameter identifier
     * @return the parameter trimmed value specified by user or defaulted
     */
    protected void verifyRequired(final String id) throws ClientException {
        if (!jsapResult.userSpecified(id)) {
            throw new ParameterClientException("This function requires a non-blank value for parameter: " + id);
        }
    }

    /**
     * Get required parameter - throws exception if parameter is not provided or is blank
     * 
     * @param id - the parameter identifier
     * @return the parameter trimmed value specified by user or defaulted
     */
    protected String getRequiredString(final String id) throws ClientException {

        String value = getString(id);
        if (value.equals("")) {
            throw new ParameterClientException("This function requires a non-blank value for parameter: " + id);
        }
        return value;
    }

    /**
     * Get a parameter - helper function - return the user specified value or "" - this simplifies the code and allows for NO_DEFAULT parameters
     * 
     * @param id - the parameter identifier
     * @return the parameter trimmed value specified by user or defaulted
     */
    protected String getString(final String id) {

        String value = jsapResult.getString(id);
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    /**
     * Get a parameter - helper function - return the user specified value or default value
     * 
     * @param id - the parameter identifier
     * @param defaultValue - default value if parameter is not specified or blank
     * @return the parameter trimmed value specified by user or defaulted
     */
    protected String getString(final String id, final String defaultValue) {

        String value = getString(id);
        if (value.equals("")) {
            value = defaultValue;
        }
        return value;
    }

    protected int getInteger(final String id) {
        return jsapResult.getInt(id);
    }

    /**
     * Find and replace all text matching the replaceText map
     */
    public String findReplace(final String text) throws ClientException {
        String result = text;
        if (jsapResult.userSpecified("findReplace")) {
            String[] list = splitCsvData(getString("findReplace"), ",", "'");
            for (int i = 0; i < list.length; i++) {
                String entry = stripQuotes(list[i].trim());
                if (!"".equals(entry)) {
                    String[] colonList = splitCsvData(entry, ":", "'");
                    String key = (colonList.length > 0) ? stripQuotes(colonList[0].trim()) : ""; // if available
                    String value = (colonList.length > 1) ? stripQuotes(colonList[1].trim()) : ""; // if available
                    result = StringUtils.replace(result, key, convertNewLine(value));
                }
            }
        }
        return result;
    }

    /**
     * New line convert - use for command line fields that need way to do multiple lines
     * 
     * @result - text with " \n" to convert to new line
     */
    public String convertNewLine(final String text) throws ClientException {
        return StringUtils.replace(text, " \\n ", Character.toString((char) 0x0a));
    }

    /**
     * Get date from the dateString specified
     * 
     * @return date object for the date provided
     */
    public Date getDate(final String dateString, final String dateFormat) throws ClientException {

        Date date;
        SimpleDateFormat format = getDateFormat(dateFormat);

        format.setLenient(true); // allow some variance on the input
        try {
            date = format.parse(dateString);
        } catch (ParseException exception) { // try a simplified format and see if that works
            DateFormat format2 = DateFormat.getDateInstance(DateFormat.SHORT);
            try {
                date = format2.parse(dateString);
            } catch (ParseException exception2) {
                throw new ClientException("Date '" + dateString + "' not valid. Valid format: '" + format.toPattern() + "'");
            }
        }
        return date;
    }

    /**
     * Get default date format given a string representing a date format - normally will be getString('format')
     * 
     * @param dateFormat - string
     * @return
     */
    public SimpleDateFormat getDateFormat(final String dateFormat) {
        return ((dateFormat == null || dateFormat.equals("")) ? new SimpleDateFormat() : new SimpleDateFormat(dateFormat));
    }

    /**
     * Get calendar for the date specified
     * 
     * @return calendar object for the date provided
     */
    public Calendar getCalendar(final String date, final String dateFormat) throws ClientException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getDate(date, dateFormat)); // validates date matches format
        return calendar;
    }

    /**
     * Get string representation of date with dateFormat or client default
     * 
     * @return calendar object for the date provided
     */
    public String getDateAsString(final Date date, final String dateFormat) {
        return getDateFormat(dateFormat).format(date);
    }

    /**
     * Get string representation of date with dateFormat or client default
     * 
     * @return calendar object for the date provided
     */
    public String getCalendarAsString(final Calendar calendar, final String dateFormat) {
        if (calendar != null) {
            return getDateAsString(calendar.getTime(), dateFormat);
        }
        return "";
    }

    /**
     * Standard finish that handles output to file and messaging
     * 
     * @param message - base of message returned to command line
     * @param result - detail to be written to file or appended after the message to command line
     * @result - response message
     */
    public String standardFinish(final String message, final String result, final String encoding) throws ClientException {

        if (jsapResult.userSpecified("file")) {
            String file = getString("file");
            if (file.equals("")) {
                return message + " discarded";
            } else {
                writeToFile(result, file, encoding);
                return message + " written to file: " + file;
            }
        }
        return message + "\n" + result;
    }

    /**
     * Setup log4j - this is primarily required for the underlying SOAP libraries - may be useful for client logging, but can be overridden for non-SOAP clients
     * not wanting logging
     */
    protected void setupLogging() {
        /*
         * String level = (debug ? "DEBUG" : "INFO"); Properties log4j = new Properties(); log4j.setProperty("log4j.rootLogger", "INFO, A1"); // Set root logger
         * level to INFO and its only appender to A1. log4j.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
         * log4j.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout"); log4j.setProperty("log4j.appender.A1.layout.ConversionPattern",
         * "%-4r [%t] %-5p %c %x - %m%n"); // Have console output goes to standard error so not to interfere with standard output handling of CLI program
         * log4j.setProperty("log4j.appender.A1.target", "System.err"); log4j.setProperty("log4j.logger.org.swift.common.cli", level);
         * PropertyConfigurator.configure(log4j); return;
         */
    }

    /**
     * Print parameter values to err stream for debug
     */
    @SuppressWarnings("unchecked")
    private void printJsapParameters() {

        IDMap idMap = jsap.getIDMap();

        for (int i = 0; i < 2; i++) {
            Iterator<String> iterator = idMap.idIterator();
            if (i == 0) {
                err.println("  User parameters (non-blank):");
            } else {
                err.println("  Default parameters (non-blank):");
            }
            while (iterator.hasNext()) {
                String id = (iterator.next()).trim();
                boolean process = ((i == 0) && jsapResult.userSpecified(id)) || ((i != 0) && !jsapResult.userSpecified(id));
                if (process && jsapResult.contains(id) && jsapResult.getObject(id) != null) {
                    String string;
                    try {
                        string = jsapResult.getString(id).trim();
                    } catch (Exception discard) {
                        string = jsapResult.getObject(id).toString();
                    }
                    if (!"".equals(string)) {
                        err.println(CliUtils.prettyOutput(id, string));
                        /*
                         * int padLength = 26 - id.length(); int padStart = 0; if (padLength % 2 == 1) { padLength++; padStart = 1; } err.println(id +
                         * padString.substring(padStart, padLength) + ": " + string);
                         */
                    }
                }
            }
        }
        return;
    }

    /**
     * Client exception classes
     */
    public static class ClientException extends Exception {
        private static final long serialVersionUID = -3973348710770633831L;

        public ClientException() {
        }

        public ClientException(String message) {
            super(message);
        }
    }

    public static class InvalidActionClientException extends ClientException {
        private static final long serialVersionUID = -3973348710770633831L;

        public InvalidActionClientException() {
        }

        public InvalidActionClientException(String message) {
            super(message);
        }
    }

    public static class ParameterClientException extends ClientException {
        private static final long serialVersionUID = -3973348710770633831L;

        public ParameterClientException() {
        }

        public ParameterClientException(String message) {
            super(message);
        }
    }

    public static class ClientSpecificException extends ClientException {
        private static final long serialVersionUID = -3973348710770633831L;

        public ClientSpecificException() {
        }

        public ClientSpecificException(String message) {
            super(message);
        }
    }
}