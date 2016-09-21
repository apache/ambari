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
import org.apache.ambari.server.serveraction.ActionLog;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigureAmbariIndetityServerAction is a ServerAction implementation that creates keytab files as
 * instructed.
 * <p/>
 * This class mainly relies on the KerberosServerAction to iterate through metadata identifying
 * the Kerberos keytab files that need to be created. For each identity in the metadata, this
 * implementation's
 * {@link KerberosServerAction#processIdentity(Map, String, KerberosOperationHandler, Map, Map)}
 * is invoked attempting the creation of the relevant keytab file.
 */
public class ConfigureAmbariIndetityServerAction extends KerberosServerAction {


  private static final String KEYTAB_PATTERN = "keyTab=\"(.+)?\"";
  private static final String PRINCIPAL_PATTERN = "principal=\"(.+)?\"";

  private final static Logger LOG = LoggerFactory.getLogger(ConfigureAmbariIndetityServerAction.class);

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(Map)} )}
   * to iterate through the Kerberos identity metadata and call
   * {@link ConfigureAmbariIndetityServerAction#processIdentities(Map)}
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

      if (operationHandler == null) {
        message = String.format("Failed to create keytab file for %s, missing KerberosOperationHandler", evaluatedPrincipal);
        actionLog.writeStdErr(message);
        LOG.error(message);
        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
      } else if (dataDirectory == null) {
        message = "The data directory has not been set. Generated keytab files can not be stored.";
        LOG.error(message);
        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
      } else {

        String hostName = identityRecord.get(KerberosIdentityDataFileReader.HOSTNAME);
        if (hostName != null && hostName.equalsIgnoreCase(KerberosHelper.AMBARI_SERVER_HOST_NAME)) {
          String destKeytabFilePath = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH);
          File hostDirectory = new File(dataDirectory, hostName);
          File srcKeytabFile = new File(hostDirectory, DigestUtils.sha1Hex(destKeytabFilePath));

          if(srcKeytabFile.exists()) {
            installAmbariServerIdentity(evaluatedPrincipal, srcKeytabFile.getAbsolutePath(), destKeytabFilePath, actionLog);
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
   * @param actionLog          the logger
   * @return true if success; false otherwise
   * @throws AmbariException
   */
  public boolean installAmbariServerIdentity(String principal,
                                             String srcKeytabFilePath,
                                             String destKeytabFilePath,
                                             ActionLog actionLog) throws AmbariException {

    // Use sudo to copy the file into place....
    try {
      ShellCommandUtil.Result result;

      // Ensure the parent directory exists...
      File destKeytabFile = new File(destKeytabFilePath);
      result = ShellCommandUtil.mkdir(destKeytabFile.getParent(), true);
      if (!result.isSuccessful()) {
        throw new AmbariException(result.getStderr());
      }

      // Copy the keytab file into place...
      result = ShellCommandUtil.copyFile(srcKeytabFilePath, destKeytabFilePath, true, true);
      if (!result.isSuccessful()) {
        throw new AmbariException(result.getStderr());
      }
    } catch (InterruptedException | IOException e) {
      throw new AmbariException(e.getLocalizedMessage(), e);
    }

    // Create/update the JAASFile...
    configureJAAS(principal, destKeytabFilePath, actionLog);

    return true;
  }

  private void configureJAAS(String evaluatedPrincipal, String keytabFilePath, ActionLog actionLog) {
    String jaasConfPath = System.getProperty(KerberosChecker.JAVA_SECURITY_AUTH_LOGIN_CONFIG);
    if (jaasConfPath != null) {
      File jaasConfigFile = new File(jaasConfPath);
      try {
        String jaasConfig = FileUtils.readFileToString(jaasConfigFile);
        File oldJaasConfigFile = new File(jaasConfPath + ".bak");
        FileUtils.writeStringToFile(oldJaasConfigFile, jaasConfig);
        jaasConfig = jaasConfig.replaceFirst(KEYTAB_PATTERN, "keyTab=\"" + keytabFilePath + "\"");
        jaasConfig = jaasConfig.replaceFirst(PRINCIPAL_PATTERN, "principal=\"" + evaluatedPrincipal + "\"");
        FileUtils.writeStringToFile(jaasConfigFile, jaasConfig);
        String message = String.format("JAAS config file %s modified successfully for principal %s.", jaasConfigFile
          .getName(), evaluatedPrincipal);
        if (actionLog != null) {
          actionLog.writeStdOut(message);
        }
      } catch (IOException e) {
        String message = String.format("Failed to configure JAAS file %s for %s - %s", jaasConfigFile,
          evaluatedPrincipal, e.getMessage());
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

}
