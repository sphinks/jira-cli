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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Random;

import org.swift.common.cli.AbstractRemoteClient.RemoteRestException;
import org.swift.common.cli.CliClient.ClientException;

public abstract class AbstractUserHelper {

    protected AbstractRemoteClient client;
    protected PrintStream out = System.out;
    protected PrintStream err = System.err;

    protected boolean verbose = false;

    public AbstractUserHelper(AbstractRemoteClient client, boolean verbose) {
        this.client = client;
        this.verbose = verbose;
    }

    /**
     * Checks if userid is already defined
     * 
     * @param userid
     * @return true if defined, false otherwise
     */
    public abstract boolean hasUser(final String userId) throws java.rmi.RemoteException, ClientException, RemoteRestException;

    /**
     * Checks if group is already defined
     * 
     * @param group
     * @return true if defined, false otherwise
     */
    public abstract boolean hasGroup(final String group) throws java.rmi.RemoteException, ClientException, RemoteRestException;

    /**
     * Adds a user after most validation has occurred. Subclass may have additional validation and must implement remote service
     * 
     * @param userId
     * @param userPassword
     * @param userEmail
     * @param userFullName
     */
    public abstract void addUserService(final String userId, final String userPassword, final String userEmail, final String userFullName)
            throws java.rmi.RemoteException, ClientException, RemoteRestException;

    /**
     * Removes a user. Info message if not found. after most validation has occurred. Subclass may have additional validation and must implement remote service
     * 
     * @param userId to remove
     * @throws RemoteRestException
     */
    public abstract void removeUserService(final String userId) throws java.rmi.RemoteException, ClientException, RemoteRestException;

    /**
     * Add a group after most validation has occurred. Subclass may have additional validation and must implement remote service
     * 
     * @param group to add
     */
    public abstract void addGroupService(final String group) throws java.rmi.RemoteException, ClientException;

    /**
     * Removes a group. Info message if not found. after most validation has occurred. Subclass may have additional validation and must implement remote service
     * 
     * @param group to remove
     * @param defaultGroup - users in group will be moved to the defaultGroup if specified
     */
    public abstract void removeGroupService(final String group, final String defaultGroup) throws java.rmi.RemoteException, ClientException;

    /**
     * Add user to group after most validation has occurred. Subclass may have additional validation and must implement remote service
     * 
     * @param userId
     * @param group
     */
    public abstract void addUserToGroupService(final String userId, final String group) throws java.rmi.RemoteException, ClientException;

    /**
     * Removes user from group. Info message if not found. after most validation has occurred. Subclass may have additional validation and must implement remote
     * service
     * 
     * @param
     */
    public abstract boolean removeUserFromGroupService(final String userId, final String group) throws java.rmi.RemoteException, ClientException;

    /**
     * Adds a user. Just gives info message if it already exists.
     * 
     * @param userId - usually all lower case name
     * @param userPassword
     * @param userEmail
     * @param userFullName
     * @throws RemoteRestException
     */
    public String addUser(final String inUserId, final String inUserPassword, final String userEmail, final String inUserFullName)
            throws java.rmi.RemoteException, ClientException, RemoteRestException {
        String userId = inUserId.trim().toLowerCase();
        String userPassword = inUserPassword.trim();
        String userFullName = inUserFullName;
        if (userId == "") {
            throw new ClientException("A non-blank userId is required."); // cover the from file case
        }
        // JIRA only - put in client
        // if (userEmail == "") {
        // throw new ClientException("A non-blank userEmail is required."); // cover the from file case
        // }
        if (userFullName.equals("")) { // if blank full name, default to user id
            userFullName = userId;
        }
        if (userPassword.equals("")) { // if blank password, generate a random password
            userPassword = Long.toString(Math.abs((new Random()).nextLong()), 36);
        }
        if (hasUser(userId)) {
            return "User: " + userId + " is already defined.";
        }
        // out.println("addUser: " + userId);

        addUserService(userId, userPassword, userEmail, userFullName);
        return "User: " + userId + " added with password: " + userPassword + ".  Full name is: " + userFullName + ".  Email is: " + userEmail + ".";
    }

    /**
     * Removes a user. Info message if not found. after most validation has occurred. Subclass may have additional validation and must implement remote service
     */
    public String removeUser(final String inUserId) throws java.rmi.RemoteException, ClientException, RemoteRestException {
        String userId = inUserId.trim();
        if (userId.equals("")) {
            throw new ClientException("A non-blank userId is required.");
        }
        if (!hasUser(userId)) {
            return "User: " + userId + " is not defined."; // ignore this - no error
        }
        removeUserService(userId);
        return "User: " + userId + " removed.";
    }

    /**
     * Adds a group. Just gives info message if it already exists.
     * 
     * @throws RemoteRestException
     */
    public String addGroup(final String inGroup) throws java.rmi.RemoteException, ClientException, RemoteRestException {

        String group = inGroup.toLowerCase().trim(); // groups are not case sensitive, so lowercase them all
        if (group.equals("")) {
            throw new ClientException("A non-blank group is required.");
        }
        if (hasGroup(group)) {
            return "Group: " + group + " is already defined.";
        }
        addGroupService(group);
        return "Group: " + group + " added.";
    }

    /**
     * Removes a group. If group has users and default group is specified, users added to default group. Default group must exist or be blank. Group names are
     * converted to lowercase as that is what jira does.
     * 
     * @throws RemoteRestException
     */
    public String removeGroup(final String inGroup, final String inDefaultGroup) throws java.rmi.RemoteException, ClientException, RemoteRestException {
        String group = inGroup.toLowerCase();
        String defaultGroup = inDefaultGroup.toLowerCase();
        String defaultGroupMessage = "";

        if (!hasGroup(group)) {
            return "Group: " + group + " is not defined."; // Ignore - not an error
        }
        if (!defaultGroup.equals("")) {
            if (!hasGroup(defaultGroup)) {
                throw new RemoteException(group + " is not a valid group.");
            }
            defaultGroupMessage = "Users moved to default group: " + defaultGroup + ".";
        } else {
            defaultGroup = null;
        }
        removeGroupService(group, defaultGroup);
        return "Group: " + group + " removed." + defaultGroupMessage;
    }

    /**
     * Add user using a file. File is a comma separated list of users with their user information Each line is: userId, userPassword, userEmail, userFullName
     * Each line will be processed even if there are errors. - if user already exists, line will be ignored - if a group does not exist, it will be added
     */
    public String addUserWithFile(final String fileName, boolean autoGroup) throws java.rmi.RemoteException, ClientException, RemoteRestException {
        // String fileName = getRequiredString("file");
        // boolean autoGroup = jsapResult.userSpecified("autoGroup");

        BufferedReader in = null;
        int errorCount = 0;
        int goodCount = 0;
        int alreadyDefined = 0;
        try {
            in = new BufferedReader(new FileReader(fileName));

            String line;
            while (true) {
                line = in.readLine();
                if (line == null) { // break on end of file
                    break;
                }
                if (!line.trim().equals("")) { // ignore blank lines
                    client.checkLogin(); // handle long processing timeouts
                    String[] list = line.split(",");
                    String userId = list[0];
                    String userPassword = "";
                    String userEmail = "";
                    String userFullName = "";
                    if (list.length > 1) {
                        userPassword = list[1].trim();
                    }
                    if (list.length > 2) {
                        userEmail = list[2].trim();
                    }
                    if (list.length > 3) {
                        userFullName = list[3].trim();
                    }
                    try {
                        if (hasUser(userId)) {
                            alreadyDefined++;
                            out.println("User: " + userId + " already defined.");
                        } else {
                            String message = addUser(userId, userPassword, userEmail, userFullName);
                            out.println(message);
                            for (int i = 4; i < list.length; i++) {
                                String group = list[i].trim();
                                if (!group.equals("")) { // ignore blank groups
                                    message = addUserToGroup(userId, group, autoGroup);
                                    out.println(message);
                                }
                            }
                            goodCount++;
                        }
                    } catch (ClientException exception) {
                        out.println(exception.toString());
                        errorCount++;
                    } catch (java.rmi.RemoteException exception) {
                        out.println(exception.toString());
                        errorCount++;
                    }
                }
            }
        } catch (FileNotFoundException exception) {
            throw new ClientException("File not found: " + fileName);
        } catch (IOException exception) {
            // if (log.isDebugEnabled()) {
            // exception.printStackTrace(err);
            // }
            throw new ClientException("Error reading file: " + fileName + "\n" + exception.toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
        String message = "Successful adds: " + goodCount + "  errors: " + errorCount + "  already defined users: " + alreadyDefined;
        if ((errorCount > 0) || (alreadyDefined > 0)) {
            throw new ClientException(message);
        }
        return message;
    }

    /**
     * Remove user using a file. File is a comma separated list of users Each line is at least: userId (other information is ignored) Each line will be
     * processed even if there are errors. - if user is not defined, line will be ignored
     */
    public String removeUserWithFile(final String fileName) throws java.rmi.RemoteException, ClientException, RemoteRestException {
        // String fileName = getRequiredString("file");
        BufferedReader in = null;
        int errorCount = 0;
        int goodCount = 0;
        int notDefined = 0;
        try {
            in = new BufferedReader(new FileReader(fileName));

            String line;
            while (true) {
                line = in.readLine();
                if (line == null) { // break on end of file
                    break;
                }
                if (!line.trim().equals("")) { // ignore blank lines
                    client.checkLogin(); // handle long processing timeouts
                    String[] list = line.split(",");
                    String userId = list[0].trim();
                    try {
                        if (!hasUser(userId)) {
                            notDefined++;
                            out.println("User: " + userId + " not found.");
                        } else {
                            String message = removeUser(userId);
                            if (message != null) {
                                out.println(message);
                            }
                            goodCount++;
                        }
                    } catch (ClientException exception) {
                        out.println(exception.toString());
                        errorCount++;
                    }
                }
            }
        } catch (FileNotFoundException exception) {
            throw new ClientException("File not found: " + fileName);
        } catch (IOException exception) {
            if (verbose) {
                out.println(exception.toString());
                exception.printStackTrace(err);
            }
            throw new ClientException("Error reading file: " + fileName);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
        String message = "Successful removes: " + goodCount + "  errors: " + errorCount + "  not defined users: " + notDefined;
        if ((errorCount > 0) || (notDefined > 0)) {
            throw new ClientException(message);
        }
        return message;
    }

    /**
     * Add user to group using parameters. Adds a user to a group. User must be valid. Set addGroup=true if you need a non-existing group to be added. Otherwise
     * group must exist.
     * 
     * @throws RemoteRestException
     */
    public String addUserToGroup(final String inUserId, final String inGroup, boolean autoGroup) throws java.rmi.RemoteException, ClientException,
            RemoteRestException {
        String userId = inUserId.trim();
        String group = inGroup.trim();

        // out.println("auto group: " + autoGroup);
        // out.println("addUserToGroup: " + group);

        if (userId.equals("")) {
            throw new ClientException("A non-blank userId is required.");
        }
        if (group.equals("")) {
            throw new ClientException("A non-blank group is required.");
        }
        if (!hasUser(userId)) {
            throw new RemoteException(userId + " is not a valid user.");
        }

        if (!hasGroup(group)) {
            if (autoGroup) {
                String message = addGroup(group);
                out.println(message);
            } else {
                throw new RemoteException(group + " is not a valid group.");
            }
        }
        addUserToGroupService(userId, group);
        return "User: " + userId + " added to group: " + group;
    }

    /**
     * Removes user from group. Remove user from existing group. Both user and group must be valid.
     */
    public String removeUserFromGroup(final String inUserId, final String inGroup) throws java.rmi.RemoteException, ClientException {
        assert inUserId != null;
        assert inGroup != null;
        String userId = inUserId.trim();
        String group = inGroup.trim();
        if (userId.equals("")) {
            throw new ClientException("A non-blank userId parameter is required.");
        }
        if (group.equals("")) {
            throw new ClientException("A non-blank group parameter is required.");
        }
        if (!removeUserFromGroupService(userId, group)) {
            return null; // warning message is sufficient
        }
        return "User: " + userId + " removed from group: " + group;
    }

    /**
     * Add/remove user to/from group using a file. File is a comma separated list of users and groups. Each line is one user and one group separated by a comma
     * Each line will be processed even if there are errors. - for add: - invalid user id will give an failure message - if a group does not exist, it will be
     * added if autoGroup is true
     */
    public String addOrRemoveUserToOrFromGroupWithFile(final String fileName, final boolean add, final boolean autoGroup) throws java.rmi.RemoteException,
            ClientException, RemoteRestException {
        BufferedReader in = null;
        int errorCount = 0;
        int goodCount = 0;
        try {
            in = new BufferedReader(new FileReader(fileName));

            String line;
            while (true) {
                line = in.readLine();
                if (line == null) { // break on end of file
                    break;
                }
                if (!line.trim().equals("")) { // ignore blank lines
                    client.checkLogin(); // handle long processing timeouts
                    String[] list = line.split(",");
                    if (list.length > 1) { // not 2 entries on a line
                        String userId = list[0];
                        String group = list[1];
                        try {
                            String message;
                            if (add) {
                                message = addUserToGroup(userId, group, autoGroup);
                            } else {
                                message = removeUserFromGroup(userId, group);
                            }
                            if (message != null) {
                                out.println(message);
                            }
                            goodCount++;
                        } catch (ClientException exception) {
                            out.println(client.exceptionMessage(exception));
                            errorCount++;
                            // } catch (RemoteValidationException exception) {
                            // out.println(client.exceptionMessage(exception));
                            // errorCount++;
                        } catch (java.rmi.RemoteException exception) {
                            out.println(client.exceptionMessage(exception));
                            errorCount++;
                        }
                    } else {
                        out.println("Line must have both userId and group separated by comma: " + line);
                        errorCount++;
                    }
                }
            }
        } catch (FileNotFoundException exception) {
            throw new ClientException("File not found: " + fileName);
        } catch (IOException exception) {
            // if (log.isDebugEnabled()) {
            // exception.printStackTrace(err);
            // }
            throw new ClientException("Error reading file: " + fileName + "\n" + exception.toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
        String message;
        if (add) {
            message = "Successful adds: " + goodCount + "  errors: " + errorCount;
        } else {
            message = "Successful removes: " + goodCount + "  errors: " + errorCount;
        }
        if (errorCount > 0) {
            throw new ClientException(message);
        }
        return message;
    }

}
