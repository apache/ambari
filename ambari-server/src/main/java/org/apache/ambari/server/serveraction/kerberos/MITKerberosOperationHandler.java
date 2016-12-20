/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.serveraction.kerberos;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * MITKerberosOperationHandler is an implementation of a KerberosOperationHandler providing
 * functionality specifically for an MIT KDC. See http://web.mit.edu/kerberos.
 * <p/>
 * It is assumed that a MIT Kerberos client is installed and that the kdamin shell command is
 * available
 */
public class MITKerberosOperationHandler extends KerberosOperationHandler {

  @Inject
  private Configuration configuration;

  /**
   * A regular expression pattern to use to parse the key number from the text captured from the
   * get_principal kadmin command
   */
  private final static Pattern PATTERN_GET_KEY_NUMBER = Pattern.compile("^.*?Key: vno (\\d+).*$", Pattern.DOTALL);

  private final static Logger LOG = LoggerFactory.getLogger(MITKerberosOperationHandler.class);

  /**
   * A String containing user-specified attributes used when creating principals
   */
  private String createAttributes = null;

  private String adminServerHost = null;

  /**
   * A String containing the resolved path to the kdamin executable
   */
  private String executableKadmin = null;

  /**
   * A String containing the resolved path to the kdamin.local executable
   */
  private String executableKadminLocal = null;

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   * <p/>
   * The kerberosConfiguration Map is not being used.
   *
   * @param administratorCredentials a PrincipalKeyCredential containing the administrative credential
   *                                 for the relevant KDC
   * @param realm                    a String declaring the default Kerberos realm (or domain)
   * @param kerberosConfiguration    a Map of key/value pairs containing data from the kerberos-env configuration set
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  @Override
  public void open(PrincipalKeyCredential administratorCredentials, String realm,
                   Map<String, String> kerberosConfiguration)
      throws KerberosOperationException {

    setAdministratorCredential(administratorCredentials);
    setDefaultRealm(realm);

    if (kerberosConfiguration != null) {
      setKeyEncryptionTypes(translateEncryptionTypes(kerberosConfiguration.get(KERBEROS_ENV_ENCRYPTION_TYPES), "\\s+"));
      setAdminServerHost(kerberosConfiguration.get(KERBEROS_ENV_ADMIN_SERVER_HOST));
      setExecutableSearchPaths(kerberosConfiguration.get(KERBEROS_ENV_EXECUTABLE_SEARCH_PATHS));
      setCreateAttributes(kerberosConfiguration.get(KERBEROS_ENV_KDC_CREATE_ATTRIBUTES));
    } else {
      setKeyEncryptionTypes(null);
      setAdminServerHost(null);
      setExecutableSearchPaths((String) null);
      setCreateAttributes(null);
    }

    // Pre-determine the paths to relevant Kerberos executables
    executableKadmin = getExecutable("kadmin");
    executableKadminLocal = getExecutable("kadmin.local");

    setOpen(true);
  }

  @Override
  public void close() throws KerberosOperationException {
    // There is nothing to do here.
    setOpen(false);

    executableKadmin = null;
    executableKadminLocal = null;
  }

  /**
   * Test to see if the specified principal exists in a previously configured MIT KDC
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the result from STDOUT to determine if the presence of the specified principal.
   *
   * @param principal a String containing the principal to test
   * @return true if the principal exists; false otherwise
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  @Override
  public boolean principalExists(String principal)
      throws KerberosOperationException {

    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (principal == null) {
      return false;
    } else {
      // Create the KAdmin query to execute:
      ShellCommandUtil.Result result = invokeKAdmin(String.format("get_principal %s", principal), null);

      // If there is data from STDOUT, see if the following string exists:
      //    Principal: <principal>
      String stdOut = result.getStdout();
      return (stdOut != null) && stdOut.contains(String.format("Principal: %s", principal));
    }
  }


  /**
   * Creates a new principal in a previously configured MIT KDC
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the result from STDOUT to determine if the operation executed successfully.
   *
   * @param principal a String containing the principal add
   * @param password  a String containing the password to use when creating the principal
   * @param service   a boolean value indicating whether the principal is to be created as a service principal or not
   * @return an Integer declaring the generated key number
   * @throws KerberosKDCConnectionException          if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException    if the administrator credentials fail to authenticate
   * @throws KerberosRealmException                  if the realm does not map to a KDC
   * @throws KerberosPrincipalAlreadyExistsException if the principal already exists
   * @throws KerberosOperationException              if an unexpected error occurred
   */
  @Override
  public Integer createPrincipal(String principal, String password, boolean service)
      throws KerberosOperationException {

    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (StringUtils.isEmpty(principal)) {
      throw new KerberosOperationException("Failed to create new principal - no principal specified");
    } else if (StringUtils.isEmpty(password)) {
      throw new KerberosOperationException("Failed to create new principal - no password specified");
    } else {
      String createAttributes = getCreateAttributes();
      // Create the kdamin query:  add_principal <-randkey|-pw <password>> [<options>] <principal>
      ShellCommandUtil.Result result = invokeKAdmin(String.format("add_principal %s %s",
          (createAttributes == null) ? "" : createAttributes, principal), password);

      // If there is data from STDOUT, see if the following string exists:
      //    Principal "<principal>" created
      String stdOut = result.getStdout();
      String stdErr = result.getStderr();
      if ((stdOut != null) && stdOut.contains(String.format("Principal \"%s\" created", principal))) {
        return getKeyNumber(principal);
      } else if ((stdErr != null) && stdErr.contains(String.format("Principal or policy already exists while creating \"%s\"", principal))) {
        throw new KerberosPrincipalAlreadyExistsException(principal);
      } else {
        LOG.error("Failed to execute kadmin query: add_principal -pw \"********\" {} {}\nSTDOUT: {}\nSTDERR: {}",
            (createAttributes == null) ? "" : createAttributes, principal, stdOut, result.getStderr());
        throw new KerberosOperationException(String.format("Failed to create service principal for %s\nSTDOUT: %s\nSTDERR: %s",
            principal, stdOut, result.getStderr()));
      }
    }
  }

  /**
   * Updates the password for an existing principal in a previously configured MIT KDC
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the exit code to determine if the operation executed successfully.
   *
   * @param principal a String containing the principal to update
   * @param password  a String containing the password to set
   * @return an Integer declaring the new key number
   * @throws KerberosKDCConnectionException         if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException   if the administrator credentials fail to authenticate
   * @throws KerberosRealmException                 if the realm does not map to a KDC
   * @throws KerberosPrincipalDoesNotExistException if the principal does not exist
   * @throws KerberosOperationException             if an unexpected error occurred
   */
  @Override
  public Integer setPrincipalPassword(String principal, String password) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (StringUtils.isEmpty(principal)) {
      throw new KerberosOperationException("Failed to set password - no principal specified");
    } else if (StringUtils.isEmpty(password)) {
      throw new KerberosOperationException("Failed to set password - no password specified");
    } else {
      // Create the kdamin query:  change_password <-randkey|-pw <password>> <principal>
      ShellCommandUtil.Result result = invokeKAdmin(String.format("change_password %s", principal), password);

      String stdOut = result.getStdout();
      String stdErr = result.getStderr();
      if ((stdOut != null) && stdOut.contains(String.format("Password for \"%s\" changed", principal))) {
        return getKeyNumber(principal);
      } else if ((stdErr != null) && stdErr.contains("Principal does not exist")) {
        throw new KerberosPrincipalDoesNotExistException(principal);
      } else {
        LOG.error("Failed to execute kadmin query: change_password -pw \"********\" {} \nSTDOUT: {}\nSTDERR: {}",
            principal, stdOut, result.getStderr());
        throw new KerberosOperationException(String.format("Failed to update password for %s\nSTDOUT: %s\nSTDERR: %s",
            principal, stdOut, result.getStderr()));
      }
    }
  }

  /**
   * Removes an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to remove
   * @return true if the principal was successfully removed; otherwise false
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  @Override
  public boolean removePrincipal(String principal) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (StringUtils.isEmpty(principal)) {
      throw new KerberosOperationException("Failed to remove new principal - no principal specified");
    } else {
      ShellCommandUtil.Result result = invokeKAdmin(String.format("delete_principal -force %s", principal), null);

      // If there is data from STDOUT, see if the following string exists:
      //    Principal "<principal>" created
      String stdOut = result.getStdout();
      return (stdOut != null) && !stdOut.contains("Principal does not exist");
    }
  }

  /**
   * Sets the KDC administrator server host address
   *
   * @param adminServerHost the ip address or FQDN of the KDC administrator server
   */
  public void setAdminServerHost(String adminServerHost) {
    this.adminServerHost = adminServerHost;
  }

  /**
   * Gets the IP address or FQDN of the KDC administrator server
   *
   * @return the IP address or FQDN of the KDC administrator server
   */
  public String getAdminServerHost() {
    return this.adminServerHost;
  }

  /**
   * Sets the (additional) principal creation attributes
   *
   * @param createAttributes the additional principal creations attributes
   */
  public void setCreateAttributes(String createAttributes) {
    this.createAttributes = createAttributes;
  }

  /**
   * Gets the (additional) principal creation attributes
   *
   * @return the additional principal creations attributes or null
   */
  public String getCreateAttributes() {
    return createAttributes;
  }

  /**
   * Retrieves the current key number assigned to the identity identified by the specified principal
   *
   * @param principal a String declaring the principal to look up
   * @return an Integer declaring the current key number
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  private Integer getKeyNumber(String principal) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (StringUtils.isEmpty(principal)) {
      throw new KerberosOperationException("Failed to get key number for principal  - no principal specified");
    } else {
      // Create the kdamin query:  get_principal <principal>
      ShellCommandUtil.Result result = invokeKAdmin(String.format("get_principal %s", principal), null);

      String stdOut = result.getStdout();
      if (stdOut == null) {
        String message = String.format("Failed to get key number for %s:\n\tExitCode: %s\n\tSTDOUT: NULL\n\tSTDERR: %s",
            principal, result.getExitCode(), result.getStderr());
        LOG.warn(message);
        throw new KerberosOperationException(message);
      }

      Matcher matcher = PATTERN_GET_KEY_NUMBER.matcher(stdOut);
      if (matcher.matches()) {
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        String keyNumber = matcher.group(1);

        numberFormat.setGroupingUsed(false);
        try {
          Number number = numberFormat.parse(keyNumber);
          return (number == null) ? 0 : number.intValue();
        } catch (ParseException e) {
          String message = String.format("Failed to get key number for %s - invalid key number value (%s):\n\tExitCode: %s\n\tSTDOUT: NULL\n\tSTDERR: %s",
              principal, keyNumber, result.getExitCode(), result.getStderr());
          LOG.warn(message);
          throw new KerberosOperationException(message);
        }
      } else {
        String message = String.format("Failed to get key number for %s - unexpected STDOUT data:\n\tExitCode: %s\n\tSTDOUT: NULL\n\tSTDERR: %s",
            principal, result.getExitCode(), result.getStderr());
        LOG.warn(message);
        throw new KerberosOperationException(message);
      }
    }
  }

  /**
   * Invokes the kadmin shell command to issue queries
   *
   * @param query        a String containing the query to send to the kdamin command
   * @param userPassword a String containing the user's password to set or update if necessary,
   *                     null if not needed
   * @return a ShellCommandUtil.Result containing the result of the operation
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  protected ShellCommandUtil.Result invokeKAdmin(String query, String userPassword)
      throws KerberosOperationException {
    if (StringUtils.isEmpty(query)) {
      throw new KerberosOperationException("Missing kadmin query");
    }

    ShellCommandUtil.Result result = null;
    PrincipalKeyCredential administratorCredential = getAdministratorCredential();
    String defaultRealm = getDefaultRealm();

    List<String> command = new ArrayList<String>();

    String adminPrincipal = (administratorCredential == null)
        ? null
        : administratorCredential.getPrincipal();

    ShellCommandUtil.InteractiveHandler interactiveHandler = null;

    if (StringUtils.isEmpty(adminPrincipal)) {
      // Set the kdamin interface to be kadmin.local
      if (StringUtils.isEmpty(executableKadminLocal)) {
        throw new KerberosOperationException("No path for kadmin.local is available - this KerberosOperationHandler may not have been opened.");
      }

      command.add(executableKadminLocal);

      if (userPassword != null) {
        interactiveHandler = new InteractivePasswordHandler(null, userPassword);
      }
    } else {
      if (StringUtils.isEmpty(executableKadmin)) {
        throw new KerberosOperationException("No path for kadmin is available - this KerberosOperationHandler may not have been opened.");
      }
      char[] adminPassword = administratorCredential.getKey();

      // Set the kdamin interface to be kadmin
      command.add(executableKadmin);

      // Add explicit KDC admin host, if available
      if (!StringUtils.isEmpty(getAdminServerHost())) {
        command.add("-s");
        command.add(getAdminServerHost());
      }

      // Add the administrative principal
      command.add("-p");
      command.add(adminPrincipal);

      if (!ArrayUtils.isEmpty(adminPassword)) {
        interactiveHandler = new InteractivePasswordHandler(String.valueOf(adminPassword), userPassword);
      } else if (userPassword != null) {
        interactiveHandler = new InteractivePasswordHandler(null, userPassword);
      }
    }

    if (!StringUtils.isEmpty(defaultRealm)) {
      // Add default realm clause
      command.add("-r");
      command.add(defaultRealm);
    }

    // Add kadmin query
    command.add("-q");
    command.add(query);

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Executing: %s", command));
    }

    int retryCount = configuration.getKerberosOperationRetries();
    int tries = 0;

    while (tries <= retryCount) {
      try {
        result = executeCommand(command.toArray(new String[command.size()]), null, interactiveHandler);
      } catch (KerberosOperationException exception) {
        if (tries == retryCount) {
          throw exception;
        }
      }

      if (result != null && result.isSuccessful()) {
        break; // break on successful result
      }
      tries++;

      try {
        Thread.sleep(1000 * configuration.getKerberosOperationRetryTimeout());
      } catch (InterruptedException ignored) {
      }

      String message = String.format("Retrying to execute kadmin after a wait of %d seconds :\n\tCommand: %s",
          configuration.getKerberosOperationRetryTimeout(),
          command);
      LOG.warn(message);
    }


    if (!result.isSuccessful()) {
      String message = String.format("Failed to execute kadmin:\n\tCommand: %s\n\tExitCode: %s\n\tSTDOUT: %s\n\tSTDERR: %s",
          command, result.getExitCode(), result.getStdout(), result.getStderr());
      LOG.warn(message);

      // Test STDERR to see of any "expected" error conditions were encountered...
      String stdErr = result.getStderr();
      // Did admin credentials fail?
      if (stdErr.contains("Client not found in Kerberos database")) {
        throw new KerberosAdminAuthenticationException(stdErr);
      } else if (stdErr.contains("Incorrect password while initializing")) {
        throw new KerberosAdminAuthenticationException(stdErr);
      }
      // Did we fail to connect to the KDC?
      else if (stdErr.contains("Cannot contact any KDC")) {
        throw new KerberosKDCConnectionException(stdErr);
      } else if (stdErr.contains("Cannot resolve network address for admin server in requested realm while initializing kadmin interface")) {
        throw new KerberosKDCConnectionException(stdErr);
      }
      // Was the realm invalid?
      else if (stdErr.contains("Missing parameters in krb5.conf required for kadmin client")) {
        throw new KerberosRealmException(stdErr);
      } else if (stdErr.contains("Cannot find KDC for requested realm while initializing kadmin interface")) {
        throw new KerberosRealmException(stdErr);
      } else {
        throw new KerberosOperationException(String.format("Unexpected error condition executing the kadmin command. STDERR: %s", stdErr));
      }
    }

    return result;
  }

  /**
   * InteractivePasswordHandler is a {@link org.apache.ambari.server.utils.ShellCommandUtil.InteractiveHandler}
   * implementation that answers queries from kadmin or kdamin.local command for the admin and/or user
   * passwords.
   */
  protected static class InteractivePasswordHandler implements ShellCommandUtil.InteractiveHandler {
    /**
     * The queue of responses to return
     */
    private LinkedList<String> responses;
    private Queue<String> currentResponses;

    /**
     * Constructor.
     *
     * @param adminPassword the KDC administrator's password (optional)
     * @param userPassword  the user's password (optional)
     */
    public InteractivePasswordHandler(String adminPassword, String userPassword) {
      responses = new LinkedList<String>();

      if (adminPassword != null) {
        responses.offer(adminPassword);
      }

      if (userPassword != null) {
        responses.offer(userPassword);
        responses.offer(userPassword);  // Add a 2nd time for the password "confirmation" request
      }

      currentResponses = new LinkedList<String>(responses);
    }

    @Override
    public boolean done() {
      return currentResponses.size() == 0;
    }

    @Override
    public String getResponse(String query) {
      return currentResponses.poll();
    }

    @Override
    public void start() {
      currentResponses = new LinkedList<String>(responses);
    }
  }
}
