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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.utilities.KerberosChecker;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalHostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.serveraction.ActionLog;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * ConfigureAmbariIdentitiesServerAction is a ServerAction implementation that creates keytab files as
 * instructed.
 * <p/>
 * This class mainly relies on the KerberosServerAction to iterate through metadata identifying
 * the Kerberos keytab files that need to be created. For each identity in the metadata, this
 * implementation's
 * {@link KerberosServerAction#processIdentity(Map, String, KerberosOperationHandler, Map, Map)}
 * is invoked attempting the creation of the relevant keytab file.
 */
public class ConfigureAmbariIdentitiesServerAction extends KerberosServerAction {


  private static final String KEYTAB_PATTERN = "keyTab=\"(.+)?\"";
  private static final String PRINCIPAL_PATTERN = "principal=\"(.+)?\"";

  private final static Logger LOG = LoggerFactory.getLogger(ConfigureAmbariIdentitiesServerAction.class);

  @Inject
  private KerberosPrincipalHostDAO kerberosPrincipalHostDAO;

  @Inject
  private HostDAO hostDAO;

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(Map)} )}
   * to iterate through the Kerberos identity metadata and call
   * {@link ConfigureAmbariIdentitiesServerAction#processIdentities(Map)}
   * for each identity to process.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport indicating the result of this action
   * @throws AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws
      AmbariException, InterruptedException {
    return processIdentities(requestSharedDataContext);
  }


  /**
   * Creates keytab file for ambari-server identity.
   * <p/>
   * It is expected that the {@link CreatePrincipalsServerAction}
   * (or similar) and {@link CreateKeytabFilesServerAction} has executed before this action.
   *
   * @param identityRecord           a Map containing the data for the current identity record
   * @param evaluatedPrincipal       a String indicating the relevant principal
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param kerberosConfiguration    a Map of configuration properties from kerberos-env
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request  @return a CommandReport, indicating an error
   *                                 condition; or null, indicating a success condition
   * @throws AmbariException if an error occurs while processing the identity record
   */
  @Override
  protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {
    CommandReport commandReport = null;

    if (identityRecord != null) {
      String message;
      String dataDirectory = getDataDirectoryPath();

      if (dataDirectory == null) {
        message = "The data directory has not been set. Generated keytab files can not be stored.";
        LOG.error(message);
        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
      } else {

        String hostName = identityRecord.get(KerberosIdentityDataFileReader.HOSTNAME);
        if (hostName != null && hostName.equalsIgnoreCase(KerberosHelper.AMBARI_SERVER_HOST_NAME)) {
          String destKeytabFilePath = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH);
          File hostDirectory = new File(dataDirectory, hostName);
          File srcKeytabFile = new File(hostDirectory, DigestUtils.sha1Hex(destKeytabFilePath));

          if (srcKeytabFile.exists()) {
            String ownerAccess = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS);
            boolean ownerWritable = "w".equalsIgnoreCase(ownerAccess) || "rw".equalsIgnoreCase(ownerAccess);
            boolean ownerReadable = "r".equalsIgnoreCase(ownerAccess) || "rw".equalsIgnoreCase(ownerAccess);
            String groupAccess = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS);
            boolean groupWritable = "w".equalsIgnoreCase(groupAccess) || "rw".equalsIgnoreCase(groupAccess);
            boolean groupReadable = "r".equalsIgnoreCase(groupAccess) || "rw".equalsIgnoreCase(groupAccess);

            installAmbariServerIdentity(evaluatedPrincipal, srcKeytabFile.getAbsolutePath(), destKeytabFilePath,
                identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME), ownerReadable, ownerWritable,
                identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME), groupReadable, groupWritable, actionLog);

            if ("AMBARI_SERVER_SELF".equals(identityRecord.get(KerberosIdentityDataFileReader.COMPONENT))) {
              // Create/update the JAASFile...
              configureJAAS(evaluatedPrincipal, destKeytabFilePath, actionLog);
            }
          }
        }
      }
    }

    return commandReport;
  }

  /**
   * Installs the Ambari Server Kerberos identity by copying its keytab file to the specified location
   * and then creating the Ambari Server JAAS File.
   *
   * @param principal          the ambari server principal name
   * @param srcKeytabFilePath  the source location of the ambari server keytab file
   * @param destKeytabFilePath the destination location of the ambari server keytab file
   * @param ownerName          the username for the owner of the generated keytab file
   * @param ownerReadable      true if the owner should be able to read this file; otherwise false
   * @param ownerWritable      true if the owner should be able to write to this file; otherwise false
   * @param groupName          the name of the group for the generated keytab file
   * @param groupReadable      true if the group should be able to read this file; otherwise false
   * @param groupWritable      true if the group should be able to write to this file; otherwise false
   * @param actionLog          the logger
   * @return true if success; false otherwise
   * @throws AmbariException
   */
  public boolean installAmbariServerIdentity(String principal,
                                             String srcKeytabFilePath,
                                             String destKeytabFilePath,
                                             String ownerName, boolean ownerReadable, boolean ownerWritable,
                                             String groupName, boolean groupReadable, boolean groupWritable,
                                             ActionLog actionLog) throws AmbariException {

    try {
      // Copy the keytab file into place (creating the parent directory, if necessary...
      copyFile(srcKeytabFilePath, destKeytabFilePath);
      setFileACL(destKeytabFilePath,
          ownerName, ownerReadable, ownerWritable,
          groupName, groupReadable, groupWritable);

      String ambariServerHostName = StageUtils.getHostName();
      HostEntity ambariServerHostEntity = hostDAO.findByName(ambariServerHostName);
      Long ambariServerHostID = (ambariServerHostEntity == null)
          ? null
          : ambariServerHostEntity.getHostId();

      if (ambariServerHostID == null) {
        String message = String.format("Failed to add the kerberos_principal_host record for %s on " +
                "the Ambari server host since the host id for Ambari server host, %s, was not found." +
                "  This is not an error if an Ambari agent is not installed on the Ambari server host.",
            principal, ambariServerHostName);
        LOG.warn(message);
        if (actionLog != null) {
          actionLog.writeStdErr(message);
        }
      } else if (!kerberosPrincipalHostDAO.exists(principal, ambariServerHostID)) {
        kerberosPrincipalHostDAO.create(principal, ambariServerHostID);
      }

      if (actionLog != null) {
        actionLog.writeStdOut(String.format("Created Ambari server keytab file for %s at %s", principal, destKeytabFilePath));
      }
    } catch (InterruptedException | IOException e) {
      throw new AmbariException(e.getLocalizedMessage(), e);
    }

    return true;
  }

  /**
   * Configure Ambari's JAAS file to reflect the principal name and keytab file for Ambari's Kerberos
   * identity.
   *
   * @param principal      the Ambari server's principal name
   * @param keytabFilePath the absolute path to the Ambari server's keytab file
   * @param actionLog      the logger
   */
  public void configureJAAS(String principal, String keytabFilePath, ActionLog actionLog) {
    String jaasConfPath = getJAASConfFilePath();
    if (jaasConfPath != null) {
      File jaasConfigFile = new File(jaasConfPath);
      try {
        String jaasConfig = FileUtils.readFileToString(jaasConfigFile);
        File oldJaasConfigFile = new File(jaasConfPath + ".bak");
        FileUtils.writeStringToFile(oldJaasConfigFile, jaasConfig);
        jaasConfig = jaasConfig.replaceFirst(KEYTAB_PATTERN, "keyTab=\"" + keytabFilePath + "\"");
        jaasConfig = jaasConfig.replaceFirst(PRINCIPAL_PATTERN, "principal=\"" + principal + "\"");
        FileUtils.writeStringToFile(jaasConfigFile, jaasConfig);
        String message = String.format("JAAS config file %s modified successfully for principal %s.",
            jaasConfigFile.getName(), principal);
        if (actionLog != null) {
          actionLog.writeStdOut(message);
        }
      } catch (IOException e) {
        String message = String.format("Failed to configure JAAS file %s for %s - %s",
            jaasConfigFile, principal, e.getMessage());
        if (actionLog != null) {
          actionLog.writeStdErr(message);
        }
        LOG.error(message, e);
      }
    } else {
      String message = String.format("Failed to configure JAAS, config file should be passed to Ambari server as: " +
          "%s.", KerberosChecker.JAVA_SECURITY_AUTH_LOGIN_CONFIG);
      if (actionLog != null) {
        actionLog.writeStdErr(message);
      }
      LOG.error(message);
    }
  }

  /**
   * Copies the specified source file to the specified destination path, creating any needed parent
   * directories.
   * <p>
   * This method is mocked in unit tests to avoid dealing with ShellCommandUtil in a mocked env.
   *
   * @param srcKeytabFilePath  the source location of the ambari server keytab file
   * @param destKeytabFilePath the destination location of the ambari server keytab file
   * @throws IOException
   * @throws InterruptedException
   * @throws AmbariException
   * @see ShellCommandUtil#mkdir(String, boolean);
   * @see ShellCommandUtil#copyFile(String, String, boolean, boolean)
   */
  void copyFile(String srcKeytabFilePath, String destKeytabFilePath)
      throws IOException, InterruptedException {

    ShellCommandUtil.Result result;

    // Create the parent directory if necessary (using sudo)
    File destKeytabFile = new File(destKeytabFilePath);
    result = ShellCommandUtil.mkdir(destKeytabFile.getParent(), true);
    if (!result.isSuccessful()) {
      throw new AmbariException(result.getStderr());
    }

    // Copy the file (using sudo)
    result = ShellCommandUtil.copyFile(srcKeytabFilePath, destKeytabFilePath, true, true);
    if (!result.isSuccessful()) {
      throw new AmbariException(result.getStderr());
    }
  }

  /**
   * Sets the access control list for this specified file.
   * <p>
   * The owner and group for the file is set as well as the owner's and group's ability to read and write
   * the file.
   * <p>
   * The result of the operation to set the group for the file is ignored since it is possible that
   * the group does not exist when performing this operation. It is expected this issue will be remedied
   * when the group becomes available.
   * <p>
   * Access for other users is denied and the file is assumed to not be executeable by anyone.
   *
   * @param filePath      the path to the file
   * @param ownerName     the username for the owner of the generated keytab file
   * @param ownerWritable true if the owner should be able to write to this file; otherwise false
   * @param ownerReadable true if the owner should be able to read this file; otherwise false
   * @param groupName     the name of the group for the generated keytab file
   * @param groupWritable true if the group should be able to write to this file; otherwise false
   * @param groupReadable true if the group should be able to read this file; otherwise false
   * @throws AmbariException if an error occurs setting the permissions on the fils
   */
  void setFileACL(String filePath,
                          String ownerName, boolean ownerReadable, boolean ownerWritable,
                          String groupName, boolean groupReadable, boolean groupWritable)
      throws AmbariException {

    ShellCommandUtil.Result result;

    result = ShellCommandUtil.setFileOwner(filePath, ownerName);

    if (result.isSuccessful()) {
      result = ShellCommandUtil.setFileGroup(filePath, groupName);

      if (!result.isSuccessful()) {
        // Ignore, but log, this it is possible that the group does not exist when performing this operation
        LOG.warn("Failed to set the group for the file at {} to {}: {}", filePath, groupName, result.getStderr());
      }

      result = ShellCommandUtil.setFileMode(filePath,
          ownerReadable, ownerWritable, false,
          groupReadable, groupWritable, false,
          false, false, false);
    }

    if (!result.isSuccessful()) {
      throw new AmbariException(result.getStderr());
    }
  }

  /**
   * Gets the location of Ambari's JAAS config file.
   * <p>
   * This method is mocked in unit tests to avoid having to alter the System properties in
   * order to locate the test JAAS config file.
   *
   * @return the path to Ambari's JAAS config file
   */
  String getJAASConfFilePath() {
    return System.getProperty(KerberosChecker.JAVA_SECURITY_AUTH_LOGIN_CONFIG);
  }
}
