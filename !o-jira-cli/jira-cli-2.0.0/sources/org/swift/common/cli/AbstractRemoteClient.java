/**
 * Copyright (c) 2006, 2008 Bob Swift
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

import java.rmi.RemoteException;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.Switch;

/**
 * Remote Client - abstraction for SOAP and REST remote clients
 */
public abstract class AbstractRemoteClient extends CliClient {
    protected String token;
    protected String address;
    protected boolean logoutOnExit = false;
    protected long loginTime = Long.MAX_VALUE; // time in milliseconds of last successful login
    protected int TIME_OUT_LIMIT = 1200000; // 20 minutes in milliseconds

    // protected org.apache.axis.client.Service service; // Soap client would add the service here

    /**
     * Extension to server address that is the location of the service to use - this is the default location that the user can override by parameter. Note that
     * the parameter value carries this value!!!
     * 
     * @return - service url extention - example: /api/rest/
     */
    protected abstract String getDefaultServiceExtension();

    /**
     * Setup for request processing - this should be overridden by subclass for specific handling
     * 
     * @return - appropriate exit code based on processing
     */
    protected ExitCode handleSetupRemote() {
        try {
            setupRemote();
        } catch (RemoteServiceException exception) {
            err.println("Unable to access remote service." + ".\n Cause: " + exceptionMessage(exception));
            if (verbose) {
                exception.printStackTrace(err);
            }
            return ExitCode.REMOTE_EXCEPTION;
        } catch (RemoteException exception) {
            err.println("Unable to log in to server: " + address + " with user: " + jsapResult.getString("user") + ".\n Cause: " + exceptionMessage(exception));
            if (verbose) {
                exception.printStackTrace(err);
            }
            return ExitCode.REMOTE_EXCEPTION;
        } catch (RemoteRestException exception) {
            err.println("Unable to log in to server: " + address + " with user: " + jsapResult.getString("user") + ".\n Cause: " + exceptionMessage(exception));
            if (verbose) {
                exception.printStackTrace(err);
            }
            return ExitCode.REMOTE_EXCEPTION;
        } catch (Exception exception) {
            err.println("Exception: " + exceptionMessage(exception));
            if (verbose) {
                exception.printStackTrace(err);
            }
            return ExitCode.FAILURE;
        }
        return ExitCode.SUCCESS;
    }

    /**
     * Setup for request processing and then handle the request - this should be overridden by subclass for specific handling
     * 
     * @return - appropriate exit code based on processing
     */
    @Override
    protected ExitCode process() {
        try {
            ExitCode exitCode = handleSetupRemote();
            if (exitCode != ExitCode.SUCCESS) {
                return exitCode;
            }
            try {
                String message = handleRequest();
                if (message != null) {
                    out.println(message);
                }
            } catch (RemoteException exception) {
                err.println("\nRemote error: " + exceptionMessage(exception));
                // err.println("Use -v option to get more details on the failure.");
                if (verbose) {
                    exception.printStackTrace(err);
                }
                return ExitCode.REMOTE_EXCEPTION;
            } catch (RemoteRestException exception) {
                err.println("\nRemote error: " + exceptionMessage(exception));
                if (verbose) {
                    exception.printStackTrace(err);
                }
                return ExitCode.REMOTE_EXCEPTION;
            } finally {
                try {
                    if (logoutOnExit) {
                        logout();
                    }
                } catch (Exception discard) {
                }
            }
        } catch (ClientException exception) {
            err.println("\nClient error: " + exceptionMessage(exception));
            if (verbose) {
                exception.printStackTrace(err);
            }
            return ExitCode.CLIENT_EXCEPTION;
        } catch (Exception exception) {
            err.println("Exception: " + exceptionMessage(exception));
            if (verbose) {
                exception.printStackTrace(err);
            }
            return ExitCode.FAILURE;
        }
        return ExitCode.SUCCESS;
    }

    /**
     * Get the service - default to return the service parameter. Override this for guest clients!
     */
    protected String getService() {
        return jsapResult.getString("service");
    }

    /**
     * Setup for remote service - this must be overridden by subclass - Example for Confluence: ConfluenceSoapServiceServiceLocator serviceLocator = new
     * ConfluenceSoapServiceServiceLocator(); serviceLocator.setConfluenceserviceV1EndpointAddress(address); service = serviceLocator.getConfluenceserviceV1();
     */
    protected abstract void serviceSetup(String address) throws ClientException, RemoteException, RemoteRestException;

    /**
     * Login to the remote service - this must be overridden by subclass - Example for soap: String user = getString("user"); String password =
     * getString("password"); token = service.login(user, password); if (verbose) { out.println("Successful login to: " + address + " by user: " + user); }
     */
    protected abstract void serviceLogin() throws ClientException, RemoteException, RemoteRestException;

    /**
     * Logout of remote service - this must be overridden by subclass - Example for soap: service.logout(token);
     */
    protected abstract void serviceLogout() throws RemoteException, RemoteRestException;

    /**
     * Perform service setup for remote login
     */
    protected void setupRemote() throws ClientException, RemoteException, Exception {

        address = getString("server") + getString("service");
        if (verbose) {
            out.println("Server address: " + address);
        }
        serviceSetup(address);
        token = null;

        if (jsapResult.userSpecified("login")) {
            token = getString("login");
        } else if (jsapResult.userSpecified("loginFromStandardInput")) {
            try {
                StringBuffer input = new StringBuffer(20);
                int in;
                // read until get end, line feed, carriage return, or blank
                while (((in = System.in.read()) != -1) && (in != 10) && (in != 13) && (in != 32)) {
                    input.append((char) in);
                }
                token = input.toString();
            } catch (Exception discard) {
            }
        }
        if (token == null) {
            remoteLogin();
        }
        return;
    }

    /**
     * Request remote login
     */
    protected void remoteLogin() throws ClientException, RemoteException, Exception {
        if (token != null) {
            logout(); // logout previous login
        }
        try {
            serviceLogin();
        } catch (Exception exception) { // retry once to avoid occasional, random errors
            if (verbose) {
                out.println("Login failed for " + getString("server") + ", retry once to avoid occasional timeouts.");
            }
            if (debug) {
                out.println(exception.toString());
                exception.printStackTrace();
            }
            Thread.sleep(2000);
            serviceLogin();
        }
        loginTime = System.currentTimeMillis();
        logoutOnExit = true; // automatic login done, so clean-up on way out
    }

    /**
     * Request remote logout
     */
    protected void remoteLogout() throws RemoteException, RemoteRestException {

        serviceLogout();
        logoutOnExit = false;
        if (verbose && (token != null)) {
            out.println(token + " logged out.");
        }
        token = null;
    }

    /**
     * Check if login is too long lived and login again if necessary
     */
    protected void checkLogin() throws RemoteException, ClientException, RemoteRestException {
        if (logoutOnExit) { // only do this is user is not controlling login directly
            if (System.currentTimeMillis() - TIME_OUT_LIMIT > loginTime) { // if longer than 20 minutes old
                logout();
                try {
                    remoteLogin();
                } catch (Exception exception) {
                    if (verbose) {
                        exception.printStackTrace(err);
                    }
                    throw new ClientException("Long running task required a new login to avoid expiring existing login.  The new login failed.");
                }
                if (debug) {
                    err.println("Login reset.");
                }
            }
        }
    }

    /**
     * Enumerate all the valid actions - all command actions must be listed in this enumeration - this needs to be overridden in a subclass - borrow switch
     * technique from http://www.xefer.com/2006/12/switchonstring
     */
    protected enum Action {
        LOGIN, LOGOUT,

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
    @Override
    protected String handleRequest() throws ClientException, RemoteException, RemoteRestException {

        String action = getString("action").toUpperCase();

        switch (Action.toAction(action)) {

        case LOGIN:
            return login();
        case LOGOUT:
            return logout();

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

        case LOGIN:
            return formatActionHelp(action.name(), "Login to remote server. Returns login token.", "password", "user");
        case LOGOUT:
            return formatActionHelp(action.name(), "Logout of remote server.", "", "");

        case NOTFOUND:
        default:
            return "";
        }
    }

    /**
     * Login to the remote service - note that with automatic login on setup, this just tracks that user requested explicit login
     * 
     * @return login token
     */
    public String login() {
        logoutOnExit = false; // user requested login explicitly, therefore don't logout on exit
        return token;
    }

    /**
     * Logout of the remote service
     * 
     * @return a message
     */
    public String logout() throws RemoteException, RemoteRestException {
        String result = (token == null) ? "" : token + " logged out";
        remoteLogout();
        return result;
    }

    /**
     * Get command general help
     * 
     * @return general help for this client
     */
    @Override
    public String getGeneralHelp() {

        return "\n\tProvides capability to make requests to a remote server." + "\n\tRequired parameters: action, server, password."
                + "\n\tOptional parameters: user (likely required for your installation)."
                + "\n\tOther required and optional parameters depending on action requested." + "";
    }

    /**
     * Add parameters to the parameterList - Options must have a value - Switches do NOT have a value - Note that the long version is expressed as: --xxx value,
     * --xxx "value", or --xxx and that short flag version is expressed as: -x value, -x "value", or -x
     */
    @Override
    protected void addParameters() {

        super.addParameters(); // add all the standard parameters for CLI client

        // Options id type default required? short flag(-) long flag (--) help text
        parameterList.add(new FlaggedOption("server", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 's', "server", "Server URL."));
        parameterList.add(new FlaggedOption("user", JSAP.STRING_PARSER, "automation", JSAP.REQUIRED, 'u', "user", "User name for remote login."));
        parameterList
                .add(new FlaggedOption("password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password", "User password for remote login."));
        parameterList.add(new FlaggedOption("login", JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "login",
                "Login token from previous login request."));
        parameterList.add(new FlaggedOption("service", JSAP.STRING_PARSER, getDefaultServiceExtension(), JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "service",
                "Service address extension."));

        // Switches id short flag(-) long flag (--) help text
        parameterList.add(new Switch("loginFromStandardInput", 'l', "loginFromStandardInput", "Get login token from standard input."));
    }

    /**
     * Process line for running scripts
     * 
     * @throws RemoteRestException
     * @throws ClientException
     * @throws java.rmi.RemoteException
     */
    @Override
    protected ExitCode processRunLine(final String line) throws java.rmi.RemoteException, ClientException, RemoteRestException {
        checkLogin(); // this handles login token expiry automatically
        CliClient runner = getNewClient();
        runner.setVerbose(verbose); // carry over these setting as default for runner
        runner.setDebug(debug);
        String standard = " --login " + token + " --server " + getString("server") + " --user " + getString("user") + " --password " + getString("password");
        return runner.doWork(line + standard);
    }

    /**
     * Remote exception
     */
    public static class RemoteRestException extends Exception {
        private static final long serialVersionUID = 1L;

        public RemoteRestException() {
        }

        public RemoteRestException(String message) {
            super(message);
        }
    }

    /**
     * Remote resource not found for a rest request
     */
    public static class RemoteResourceNotFoundException extends RemoteRestException {
        private static final long serialVersionUID = 1L;

        public RemoteResourceNotFoundException() {
        }

        public RemoteResourceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Remote service exception
     */
    public static class RemoteServiceException extends RemoteException {
        private static final long serialVersionUID = 1L;

        public RemoteServiceException() {
        }

        public RemoteServiceException(String message) {
            super(message);
        }
    }
}