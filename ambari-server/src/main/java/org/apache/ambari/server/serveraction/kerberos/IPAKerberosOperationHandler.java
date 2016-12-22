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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.utils.Closeables;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPAKerberosOperationHandler is an implementation of a KerberosOperationHandler providing
 * functionality specifically for IPA managed KDC. See http://www.freeipa.org
 * <p/>
 * It is assumed that the IPA admin tools are installed and that the ipa shell command is
 * available
 */
public class IPAKerberosOperationHandler extends KerberosOperationHandler {
  private final static Logger LOG = LoggerFactory.getLogger(IPAKerberosOperationHandler.class);

  private String adminServerHost = null;

  private HashMap<String, Keytab> cachedKeytabs = null;
  /**
   * This is where user principals are members of. Important as the password should not expire
   * and thus a separate password policy should apply to this group
   */
  private String userPrincipalGroup = null;

  /**
   * The format used for krbPasswordExpiry
   */
  private final SimpleDateFormat expiryFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'");

  /**
   * Time zone for krbPasswordExpiry
   */
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  /**
   * Years to add for password expiry
   */
  private static final int PASSWORD_EXPIRY_YEAR = 30;

  /**
   * A regular expression pattern to use to parse the key number from the text captured from the
   * kvno command
   */
  private final static Pattern PATTERN_GET_KEY_NUMBER = Pattern.compile("^.*?: kvno = (\\d+).*$", Pattern.DOTALL);

  /**
   * A String containing the resolved path to the ipa executable
   */
  private String executableIpaGetKeytab = null;

  /**
   * A String containing the resolved path to the ipa executable
   */
  private String executableIpa = null;

  /**
   * A String containing the resolved path to the kinit executable
   */
  private String executableKinit = null;

  /**
   * A String containing the resolved path to the ipa-getkeytab executable
   */
  private String executableKvno = null;

  /**
   * A boolean indicating if password expiry should be set
   */
  private boolean usePasswordExpiry = false;

  /**
   * An int indicating the time out in seconds for the password chat;
   */
  private int timeout = DEFAULT_PASSWORD_CHAT_TIMEOUT;

  /**
   * Credentials context stores a handler to the ccache so it can be reused and removed on request
   */
  private CredentialsContext credentialsContext;

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   * <p/>
   * The kerberosConfiguration Map is not being used.
   *
   * @param administratorCredentials a KerberosCredential containing the administrative credentials
   *                                 for the relevant IPA KDC
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
      // todo: ignore if ipa managed krb5.conf?
      setKeyEncryptionTypes(translateEncryptionTypes(kerberosConfiguration.get(KERBEROS_ENV_ENCRYPTION_TYPES), "\\s+"));
      setExecutableSearchPaths(kerberosConfiguration.get(KERBEROS_ENV_EXECUTABLE_SEARCH_PATHS));
      setUserPrincipalGroup(kerberosConfiguration.get(KERBEROS_ENV_USER_PRINCIPAL_GROUP));
      setAdminServerHost(kerberosConfiguration.get(KERBEROS_ENV_ADMIN_SERVER_HOST));
      setUsePasswordExpiry(kerberosConfiguration.get(KERBEROS_ENV_SET_PASSWORD_EXPIRY));
      setTimeout(kerberosConfiguration.get(KERBEROS_ENV_PASSWORD_CHAT_TIMEOUT));
    } else {
      setKeyEncryptionTypes(null);
      setAdminServerHost(null);
      setExecutableSearchPaths((String) null);
      setUserPrincipalGroup(null);
      setUsePasswordExpiry(null);
      setTimeout(null);
    }

    // Pre-determine the paths to relevant Kerberos executables
    executableIpa = getExecutable("ipa");
    executableKvno = getExecutable("kvno");
    executableKinit = getExecutable("kinit");
    executableIpaGetKeytab = getExecutable("ipa-getkeytab");

    credentialsContext = new CredentialsContext(administratorCredentials);
    cachedKeytabs = new HashMap<>();
    expiryFormat.setTimeZone(UTC);

    setOpen(true);
  }

  private void setUsePasswordExpiry(String usePasswordExpiry) {
    if (usePasswordExpiry == null) {
      this.usePasswordExpiry = false;
      return;
    }

    if (usePasswordExpiry.equalsIgnoreCase("true")) {
      this.usePasswordExpiry = true;
    } else {
      this.usePasswordExpiry = false;
    }
  }

  private void setTimeout(String timeout) {
    if (timeout == null || timeout.isEmpty()) {
      this.timeout = DEFAULT_PASSWORD_CHAT_TIMEOUT;
      return;
    }

    try {
      this.timeout = Integer.parseInt(timeout);
    } catch (NumberFormatException e) {
      this.timeout = DEFAULT_PASSWORD_CHAT_TIMEOUT;
    }
  }

  @Override
  public void close() throws KerberosOperationException {
    if (isOpen()) {
      credentialsContext.delete();
    }

    // There is nothing to do here.
    setOpen(false);

    executableIpa = null;
    executableKvno = null;
    executableIpaGetKeytab = null;
    executableKinit = null;
    credentialsContext = null;
    cachedKeytabs = null;
  }

  /**
   * Test to see if the specified principal exists in a previously configured IPA KDC
   * <p/>
   * This implementation creates a query to send to the ipa shell command and then interrogates
   * the result from STDOUT to determine if the presence of the specified principal.
   *
   * @param principal a String containing the principal to test
   * @return true if the principal exists; false otherwise
   * @throws KerberosOperationException if an unexpected error occurred
   */
  @Override
  public boolean principalExists(String principal)
          throws KerberosOperationException {

    LOG.debug("Entering principal exists");

    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (principal == null) {
      return false;
    } else if (isServicePrincipal(principal)) {
      return true;
    } else {
      // TODO: fix exception check to only check for relevant exceptions
      try {
        DeconstructedPrincipal deconstructedPrincipal = createDeconstructPrincipal(principal);
        LOG.debug("Running IPA command user-show");

        // Create the ipa query to execute:
        ShellCommandUtil.Result result = invokeIpa(String.format("user-show %s", deconstructedPrincipal.getPrincipalName()));
        if (result.isSuccessful()) {
          return true;
        }
      } catch (KerberosOperationException e) {
        LOG.error("Cannot invoke IPA: " + e);
        throw e;
      }
    }

    return false;
  }


  /**
   * Creates a new principal in a previously configured IPA Realm
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the result from STDOUT to determine if the operation executed successfully.
   *
   * @param principal a String containing the principal add
   * @param password  a String containing the password to use when creating the principal
   * @param service   a boolean value indicating whether the principal is to be created as a service principal or not
   * @return an Integer declaring the generated key number
   * @throws KerberosKDCConnectionException if a connection to the KDC cannot be made
   */
  @Override
  public Integer createPrincipal(String principal, String password, boolean service)
          throws KerberosOperationException {

    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to create new principal - no principal specified");
    } else if (((password == null) || password.isEmpty()) && service) {
      throw new KerberosOperationException("Failed to create new user principal - no password specified");
    } else {
      DeconstructedPrincipal deconstructedPrincipal = createDeconstructPrincipal(principal);

      if (service) {
        // Create the ipa query:  service-add --ok-as-delegate <principal>
        ShellCommandUtil.Result result = invokeIpa(String.format("service-add %s", principal));
        if (result.isSuccessful()) {
          // IPA does not generate encryption types when no keytab has been generated
          // So getKeyNumber(principal) cannot be used.
          // createKeytabCredentials(principal, password);
          // return getKeyNumber(principal);
          return 0;
        } else {
          LOG.error("Failed to execute ipa query: service-add --ok-as-delegate=TRUE {}\nSTDOUT: {}\nSTDERR: {}",
                  principal, result.getStdout(), result.getStderr());
          throw new KerberosOperationException(String.format("Failed to create service principal for %s\nSTDOUT: %s\nSTDERR: %s",
                  principal, result.getStdout(), result.getStderr()));
        }
      } else {
        if (!StringUtils.isAllLowerCase(deconstructedPrincipal.getPrincipalName())) {
          LOG.warn(deconstructedPrincipal.getPrincipalName() + " is not in lowercase. FreeIPA does not recognize user " +
                  "principals that are not entirely in lowercase. This can lead to issues with kinit and keytabs. Make " +
                  "sure users are in lowercase ");
        }
        // Create the ipa query: user-add <username> --principal=<principal_name> --first <primary> --last <primary>
        // set-attr userPassword="<password>"
        // first and last are required for IPA so we make it equal to the primary
        // the --principal arguments makes sure that Kerberos keys are available for use in getKeyNumber
        ShellCommandUtil.Result result = invokeIpa(String.format("user-add %s --principal=%s --first %s --last %s --setattr userPassword=%s",
                deconstructedPrincipal.getPrimary(), deconstructedPrincipal.getPrincipalName(),
                deconstructedPrincipal.getPrimary(), deconstructedPrincipal.getPrimary(), password));

        if (!result.isSuccessful()) {
          throw new KerberosOperationException(String.format("Failed to create user principal for %s\nSTDOUT: %s\nSTDERR: %s",
                  principal, result.getStdout(), result.getStderr()));
        }

        if (getUserPrincipalGroup() != null && !getUserPrincipalGroup().isEmpty()) {
          result = invokeIpa(String.format("group-add-member %s --users=%s",
                  getUserPrincipalGroup(), deconstructedPrincipal.getPrimary()));
          if (!result.isSuccessful()) {
            throw new KerberosOperationException(String.format("Failed to create user principal for %s\nSTDOUT: %s\nSTDERR: %s",
                    principal, result.getStdout(), result.getStderr()));
          }
        }

        if (!usePasswordExpiry) {
          updatePassword(deconstructedPrincipal.getPrimary(), password);
          return getKeyNumber(principal);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, PASSWORD_EXPIRY_YEAR);

        result = invokeIpa(String.format("user-mod %s --setattr krbPasswordExpiration=%s",
                deconstructedPrincipal.getPrimary(), expiryFormat.format(calendar.getTime())));

        if (result.isSuccessful()) {
          return getKeyNumber(principal);
        }

        throw new KerberosOperationException(String.format("Unknown error while creating principal for %s\n" +
                        "STDOUT: %s\n" +
                        "STDERR: %s\n",
                principal, result.getStdout(), result.getStderr()));
      }
    }
  }

  /**
   * Updates the password for an existing user principal in a previously configured IPA KDC
   * <p/>
   * This implementation creates a query to send to the ipa shell command and then interrogates
   * the exit code to determine if the operation executed successfully.
   *
   * @param principal a String containing the principal to update
   * @param password  a String containing the password to set
   * @return an Integer declaring the new key number
   * @throws KerberosOperationException if an unexpected error occurred
   */
  @Override
  public Integer setPrincipalPassword(String principal, String password) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to set password - no principal specified");
    } else if ((password == null) || password.isEmpty()) {
      throw new KerberosOperationException("Failed to set password - no password specified");
    } else if (!isServicePrincipal(principal)) {
      DeconstructedPrincipal deconstructedPrincipal = createDeconstructPrincipal(principal);

      if (usePasswordExpiry) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, PASSWORD_EXPIRY_YEAR);

        // Create the ipa query:  user-mod <user> --setattr userPassword=<password>
        invokeIpa(String.format("user-mod %s --setattr userPassword=%s", deconstructedPrincipal.getPrimary(), password));

        List<String> command = new ArrayList<>();
        command.add(executableIpa);
        command.add("user-mod");
        command.add(deconstructedPrincipal.getPrimary());
        command.add("--setattr");
        command.add(String.format("krbPasswordExpiration=%s", expiryFormat.format(calendar.getTime())));
        ShellCommandUtil.Result result = executeCommand(command.toArray(new String[command.size()]));
        if (!result.isSuccessful()) {
          throw new KerberosOperationException("Failed to set password expiry");
        }
      } else {
        updatePassword(deconstructedPrincipal.getPrimary(), password);
      }
    } else {
      ShellCommandUtil.Result result = invokeIpa(String.format("service-show %s", principal));
      // ignore the keytab but set the password for this principal
      if (result.isSuccessful() && result.getStdout().contains("Keytab: False")) {
        LOG.debug("Found service principal " + principal + " without password/keytab. Setting one");
        createKeytab(principal, password, 0);
      }
    }
    return getKeyNumber(principal);
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

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to remove new principal - no principal specified");
    } else {
      ShellCommandUtil.Result result = null;
      if (isServicePrincipal(principal)) {
        result = invokeIpa(String.format("service-del %s", principal));
      } else {
        DeconstructedPrincipal deconstructedPrincipal = createDeconstructPrincipal(principal);
        result = invokeIpa(String.format("user-del %s", deconstructedPrincipal.getPrincipalName()));
      }
      return result.isSuccessful();
    }
  }

  /**
   * Sets the name of the group where user principals should be members of
   *
   * @param userPrincipalGroup the name of the group
   */
  public void setUserPrincipalGroup(String userPrincipalGroup) {
    this.userPrincipalGroup = userPrincipalGroup;
  }

  /**
   * Gets the name of the group where user principals should be members of
   *
   * @return name of the group where user principals should be members of
   */
  public String getUserPrincipalGroup() {
    return this.userPrincipalGroup;
  }

  /**
   * Sets the KDC administrator server host address
   *
   * @param adminServerHost the ip address or FQDN of the IPA administrator server
   */
  public void setAdminServerHost(String adminServerHost) {
    this.adminServerHost = adminServerHost;
  }

  /**
   * Gets the IP address or FQDN of the IPA administrator server
   *
   * @return the IP address or FQDN of the IPA administrator server
   */
  public String getAdminServerHost() {
    return this.adminServerHost;
  }

  /**
   * Reads data from a stream without blocking and when available. Allows some time for the
   * stream to become ready.
   *
   * @param stdin  the stdin BufferedReader to read from
   * @param stderr the stderr BufferedReader in case something goes wrong
   * @return a String with available data
   * @throws KerberosOperationException if a timeout happens
   * @throws IOException                when somethings goes wrong with the underlying stream
   * @throws InterruptedException       if the thread is interrupted
   */
  private String readData(BufferedReader stdin, BufferedReader stderr) throws KerberosOperationException, IOException, InterruptedException {
    char[] data = new char[1024];
    StringBuilder sb = new StringBuilder();

    int count = 0;
    while (!stdin.ready()) {
      Thread.sleep(1000L);
      if (count >= timeout) {
        char[] err_data = new char[1024];
        StringBuilder err = new StringBuilder();
        while (stderr.ready()) {
          stderr.read(err_data);
          err.append(err_data);
        }
        throw new KerberosOperationException("No answer data available from stdin stream. STDERR: " + err.toString());
      }
      count++;
    }

    while (stdin.ready()) {
      stdin.read(data);
      sb.append(data);
    }

    return sb.toString();
  }

  /**
   * Updates a  password for a (user) principal. This is done by first setting a random password and
   * then invoking kInit to directly set the password. This is done to circumvent issues with expired
   * password in IPA, as IPA needs passwords set by the admin to be set again by the user. Note that
   * this resets the current principal to the principal specified here. To invoke further administrative
   * commands a new kInit to admin is required.
   *
   * @param principal The principal user name that needs to be updated
   * @param password  The new password
   * @throws KerberosOperationException if something is not as expected
   */
  private void updatePassword(String principal, String password) throws KerberosOperationException {
    BufferedReader reader = null;
    BufferedReader stderr = null;
    OutputStreamWriter out = null;

    LOG.debug("Updating password for: " + principal);

    UUID uuid = UUID.randomUUID();
    String fileName = System.getProperty("java.io.tmpdir") +
            File.pathSeparator +
            "krb5cc_" + uuid.toString();

    try {
      ShellCommandUtil.Result result = invokeIpa(String.format("user-mod %s --random", principal));
      if (!result.isSuccessful()) {
        throw new KerberosOperationException(result.getStderr());
      }
      Pattern pattern = Pattern.compile("password: (.*)");
      Matcher matcher = pattern.matcher(result.getStdout());
      if (!matcher.find()) {
        throw new KerberosOperationException("Unexpected response from ipa: " + result.getStdout());
      }
      String old_password = matcher.group(1);

      String credentialsCache = String.format("FILE:%s", fileName);
      Process process = Runtime.getRuntime().exec(new String[]{executableKinit, "-c", credentialsCache, principal});
      reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      stderr = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      out = new OutputStreamWriter(process.getOutputStream());

      String data = readData(reader, stderr);
      if (!data.startsWith("Password")) {
        process.destroy();
        throw new KerberosOperationException("Unexpected response from kinit while trying to password for "
                + principal + " got: " + data);
      }
      LOG.debug("Sending old password");
      out.write(old_password);
      out.write('\n');
      out.flush();

      data = readData(reader, stderr);
      if (!data.contains("Enter")) {
        process.destroy();
        throw new KerberosOperationException("Unexpected response from kinit while trying to password for "
                + principal + " got: " + data);
      }
      LOG.debug("Sending new password");
      out.write(password);
      out.write('\n');
      out.flush();

      data = readData(reader, stderr);
      if (!data.contains("again")) {
        process.destroy();
        throw new KerberosOperationException("Unexpected response from kinit while trying to password for "
                + principal + " got: " + data);
      }
      LOG.debug("Sending new password again");
      out.write(password);
      out.write('\n');
      out.flush();

      process.waitFor();
    } catch (IOException e) {
      LOG.error("Cannot read stream: " + e);
      throw new KerberosOperationException(e.getMessage());
    } catch (InterruptedException e) {
      LOG.error("Process interrupted: " + e);
      throw new KerberosOperationException(e.getMessage());
    } finally {
      try {
        if (out != null)
          out.close();
      } catch (IOException e) {
        LOG.warn("Cannot close out stream: " + e);
      }
      try {
        if (reader != null)
          reader.close();
      } catch (IOException e) {
        LOG.warn("Cannot close stdin stream: " + e);
      }
      try {
        if (stderr != null)
          stderr.close();
      } catch (IOException e) {
        LOG.warn("Cannot close stderr stream: " + e);
      }
      File ccache = new File(fileName);
      ccache.delete();
    }

  }

  /**
   * Invokes the ipa shell command with administrative credentials to issue queries
   *
   * @param query a String containing the query to send to the kdamin command
   * @return a ShellCommandUtil.Result containing the result of the operation
   * @throws KerberosOperationException if an unexpected error occurred
   */
  protected ShellCommandUtil.Result invokeIpa(String query)
          throws KerberosOperationException {
    LOG.debug("Entering invokeipa");

    ShellCommandUtil.Result result = null;

    if ((query == null) || query.isEmpty()) {
      throw new KerberosOperationException("Missing ipa query");
    }
    PrincipalKeyCredential administratorCredentials = getAdministratorCredential();
    String defaultRealm = getDefaultRealm();

    List<String> command = new ArrayList<String>();
    List<String> kinit = new ArrayList<String>();

    String adminPrincipal = (administratorCredentials == null)
            ? null
            : administratorCredentials.getPrincipal();

    if ((adminPrincipal == null) || adminPrincipal.isEmpty()) {
      throw new KerberosOperationException("No admin principal for ipa available - " +
              "this KerberosOperationHandler may not have been opened.");
    }

    if ((executableIpa == null) || executableIpa.isEmpty()) {
      throw new KerberosOperationException("No path for ipa is available - " +
              "this KerberosOperationHandler may not have been opened.");
    }

    // Set the ipa interface to be ipa
    command.add(executableIpa);
    command.add(query);

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Executing: %s", createCleanCommand(command)));
    }

    List<String> fixedCommand = fixCommandList(command);
    result = executeCommand(fixedCommand.toArray(new String[fixedCommand.size()]));


    LOG.debug("Done invokeipa");
    return result;
  }

  /**
   * Executes a shell command in a credentials context
   * <p/>
   * See {@link org.apache.ambari.server.utils.ShellCommandUtil#runCommand(String[])}
   *
   * @param command an array of String value representing the command and its arguments
   * @return a ShellCommandUtil.Result declaring the result of the operation
   * @throws KerberosOperationException
   */
  @Override
  protected ShellCommandUtil.Result executeCommand(String[] command)
          throws KerberosOperationException {
    return credentialsContext.executeCommand(command);
  }

  /**
   * Rebuilds the command line to make sure space are converted to arguments
   *
   * @param command a List of items making up the command
   * @return the fixed command
   */
  private List<String> fixCommandList(List<String> command) {
    List<String> fixedCommandList = new ArrayList<>();
    Iterator<String> iterator = command.iterator();

    if (iterator.hasNext()) {
      fixedCommandList.add(iterator.next());
    }

    while (iterator.hasNext()) {
      String part = iterator.next();

      // split arguments
      if (part.contains(" ")) {
        StringTokenizer st = new StringTokenizer(part, " ");
        while (st.hasMoreElements()) {
          fixedCommandList.add(st.nextToken());
        }
      } else {
        fixedCommandList.add(part);
      }
    }

    return fixedCommandList;
  }

  /**
   * Build the ipa command string, replacing administrator password with "********"
   *
   * @param command a List of items making up the command
   * @return the cleaned command string
   */
  private String createCleanCommand(List<String> command) {
    StringBuilder cleanedCommand = new StringBuilder();
    Iterator<String> iterator = command.iterator();

    if (iterator.hasNext()) {
      cleanedCommand.append(iterator.next());
    }

    while (iterator.hasNext()) {
      String part = iterator.next();

      cleanedCommand.append(' ');
      cleanedCommand.append(part);

      if ("--setattr".equals(part)) {
        // Skip the password and use "********" instead
        String arg= null;
        if (iterator.hasNext()) {
          arg = iterator.next();
          if (arg.contains("userPassword")) {
            cleanedCommand.append("userPassword=******");
          } else {
            cleanedCommand.append(arg);
          }
        }
      }
    }

    return cleanedCommand.toString();
  }

  /**
   * Determine is a principal is a service principal
   *
   * @param principal
   * @return true if the principal is a (existing) service principal
   * @throws KerberosOperationException
   */
  private boolean isServicePrincipal(String principal)
          throws KerberosOperationException {

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to determine principal type- no principal specified");
    } else if (!principal.contains("/")) {
      return false;
    }

    try {
      ShellCommandUtil.Result result = invokeIpa(String.format("service-show %s", principal));

      // TODO: unfortunately we can be in limbo if the "Keytab: False" is present
      if (result.isSuccessful()) {
        return true;
      }
    } catch (KerberosOperationException e) {
      LOG.warn("Exception while invoking ipa service-show: " + e);
      return false;
    }

    return false;
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

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to get key number for principal  - no principal specified");
    } else {
      // Create the kvno query:  <principal>
      List<String> command = new ArrayList<>();
      command.add(executableKvno);
      command.add(principal);

      ShellCommandUtil.Result result = executeCommand(command.toArray(new String[command.size()]));
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
   * Creates a key tab by using the ipa commandline utilities.
   *
   * @param principal a String containing the principal to test
   * @param password  a String containing the password to use when creating the principal
   * @return
   * @throws KerberosOperationException
   */
  /*private Keytab createKeytabCredentials(String principal, String password)
          throws KerberosOperationException {

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to create keytab file, missing principal");
    }

    BufferedReader reader = null;
    BufferedReader stderr = null;
    OutputStreamWriter out = null;

    UUID uuid = UUID.randomUUID();
    String fileName = System.getProperty("java.io.tmpdir") +
            File.pathSeparator +
            "ambari." + uuid.toString();

    try {
      // TODO: add ciphers
      Process p = credentialsContext.exec(new String[]{executableIpaGetKeytab, "-s",
              getAdminServerHost(), "-p", principal, "-k", fileName, "-P"});
      reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
      stderr = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8));
      out = new OutputStreamWriter(p.getOutputStream());

      String data = readData(reader, stderr);
      if (!data.startsWith("New")) {
        p.destroy();
        throw new KerberosOperationException("Unexpected response from ipa-getkeytab while trying to password for "
                + principal + " got: " + data);
      }
      LOG.debug("Sending password");
      out.write(password);
      out.write('\n');
      out.flush();

      data = readData(reader, stderr);
      if (!data.contains("Verify")) {
        p.destroy();
        throw new KerberosOperationException("Unexpected response from ipa-getkeytab while trying to password for "
                + principal + " got: " + data);
      }
      LOG.debug("Sending new password");
      out.write(password);
      out.write('\n');
      out.flush();

      p.waitFor();
    } catch (IOException e) {
      LOG.error("Cannot read stream: " + e);
      throw new KerberosOperationException(e.getMessage());
    } catch (InterruptedException e) {
      LOG.error("Process interrupted: " + e);
      throw new KerberosOperationException(e.getMessage());
    } finally {
      try {
        if (out != null)
          out.close();
      } catch (IOException e) {
        LOG.warn("Cannot close out stream: " + e);
      }
      try {
        if (reader != null)
          reader.close();
      } catch (IOException e) {
        LOG.warn("Cannot close stdin stream: " + e);
      }
      try {
        if (stderr != null)
          stderr.close();
      } catch (IOException e) {
        LOG.warn("Cannot close stderr stream: " + e);
      }
    }

    File keytabFile = new File(fileName);
    Keytab keytab = readKeytabFile(keytabFile);
    keytabFile.delete();

    return keytab;
  }*/

  /**
   * Creates a key tab by using the ipa commandline utilities. It ignores key number and password
   * as this will be handled by IPA
   *
   * @param principal a String containing the principal to test
   * @param password  (IGNORED) a String containing the password to use when creating the principal
   * @param keyNumber (IGNORED) a Integer indicating the key number for the keytab entries
   * @return
   * @throws KerberosOperationException
   */
  @Override
  protected Keytab createKeytab(String principal, String password, Integer keyNumber)
          throws KerberosOperationException {

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to create keytab file, missing principal");
    }

    // use cache if available
    if (cachedKeytabs.containsKey(principal)) {
      return cachedKeytabs.get(principal);
    }

    UUID uuid = UUID.randomUUID();
    String fileName = System.getProperty("java.io.tmpdir") +
            File.pathSeparator +
            "ambari." + uuid.toString();

    // TODO: add ciphers
    List<String> command = new ArrayList<>();
    command.add(executableIpaGetKeytab);
    command.add("-s");
    command.add(getAdminServerHost());
    command.add("-p");
    command.add(principal);
    command.add("-k");
    command.add(fileName);

    // TODO: is it really required to set the password?
    ShellCommandUtil.Result result = executeCommand(command.toArray(new String[command.size()]));
    if (!result.isSuccessful()) {
      String message = String.format("Failed to get key number for %s:\n\tExitCode: %s\n\tSTDOUT: %s\n\tSTDERR: %s",
              principal, result.getExitCode(), result.getStdout(), result.getStderr());
      LOG.warn(message);
      throw new KerberosOperationException(message);
    }

    File keytabFile = new File(fileName);
    Keytab keytab = readKeytabFile(keytabFile);
    keytabFile.delete();

    cachedKeytabs.put(principal, keytab);
    return keytab;
  }


  /**
   * Credentials context executes commands wrapped with kerberos credentials
   */
  class CredentialsContext {
    private PrincipalKeyCredential credentials;
    Map<String, String> env = new HashMap<>();
    private String fileName;
    private List<Process> processes = new ArrayList<>();

    public CredentialsContext(PrincipalKeyCredential credentials) throws KerberosOperationException {
      this.credentials = credentials;

      UUID uuid = UUID.randomUUID();
      fileName = System.getProperty("java.io.tmpdir") +
              File.pathSeparator +
              "krb5cc_" + uuid.toString();
      env.put("KRB5CCNAME", String.format("FILE:%s", fileName));

      init(credentials, fileName);
    }

    protected ShellCommandUtil.Result executeCommand(String[] command)
            throws KerberosOperationException {

      if ((command == null) || (command.length == 0)) {
        return null;
      } else {
        try {
          return ShellCommandUtil.runCommand(command, env);
        } catch (IOException e) {
          String message = String.format("Failed to execute the command: %s", e.getLocalizedMessage());
          LOG.error(message, e);
          throw new KerberosOperationException(message, e);
        } catch (InterruptedException e) {
          String message = String.format("Failed to wait for the command to complete: %s", e.getLocalizedMessage());
          LOG.error(message, e);
          throw new KerberosOperationException(message, e);
        }
      }
    }

    /**
     * Does a kinit to obtain a ticket for the specified principal and stores it in the specified cache
     *
     * @param credentials Credentials to be used to obtain the ticket
     * @param fileName    Filename where to store the credentials
     * @throws KerberosOperationException In case the ticket cannot be obtained
     */
    private void init(PrincipalKeyCredential credentials, String fileName) throws KerberosOperationException {
      Process process;
      BufferedReader reader = null;
      OutputStreamWriter osw = null;

      LOG.debug("Entering doKinit");
      try {
        String credentialsCache = String.format("FILE:%s", fileName);

        LOG.debug("start subprocess " + executableKinit + " " + credentials.getPrincipal());
        process = Runtime.getRuntime().exec(new String[]{executableKinit, "-c", credentialsCache, credentials.getPrincipal()});
        reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        osw = new OutputStreamWriter(process.getOutputStream());

        char[] data = new char[1024];
        StringBuilder sb = new StringBuilder();

        int count = 0;
        while (!reader.ready()) {
          Thread.sleep(1000L);
          if (count >= 5) {
            process.destroy();
            throw new KerberosOperationException("No answer from kinit");
          }
          count++;
        }

        while (reader.ready()) {
          reader.read(data);
          sb.append(data);
        }

        String line = sb.toString();
        LOG.debug("Reading a line: " + line);
        if (!line.startsWith("Password")) {
          throw new KerberosOperationException("Unexpected response from kinit while trying to get ticket for "
                  + credentials.getPrincipal() + " got: " + line);
        }
        osw.write(credentials.getKey());
        osw.write('\n');
        osw.close();

        process.waitFor();

        LOG.debug("done subprocess");
      } catch (IOException e) {
        String message = String.format("Failed to execute the command: %s", e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      } catch (InterruptedException e) {
        String message = String.format("Failed to execute the command: %s", e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      } finally {
        Closeables.closeSilently(osw);
        Closeables.closeSilently(reader);
      }

      if (process.exitValue() != 0) {
        throw new KerberosOperationException("kinit failed for " + credentials.getPrincipal() + ". Wrong password?");
      }

    }

    public Process exec(String[] args) throws IOException {
      Process process = Runtime.getRuntime().exec(args);
      processes.add(process);

      return process;
    }

    public void delete() {
      File ccache = new File(fileName);
      ccache.delete();
      for (Process p : processes) {
        p.destroy();
      }
    }

  }

}
