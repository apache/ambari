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
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.ambari.server.serveraction.ActionLog;
import org.apache.commons.io.FileUtils;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

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
   * KerberosPrincipalDAO used to set and get Kerberos principal details
   */
  @Inject
  private KerberosPrincipalDAO kerberosPrincipalDAO;


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
          String keytabFilePath = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH);
          String keytabOwner = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME);
          String keytabAccess = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS);
          createAndConfigureAmbariKeytab(evaluatedPrincipal, operationHandler, keytabFilePath, keytabOwner, keytabAccess, actionLog);
        }
      }
    }

    return commandReport;
  }

  public boolean createAndConfigureAmbariKeytab(String principal, KerberosOperationHandler operationHandler,
                                    String keytabFilePath, String keytabOwner, String keytabAccess, ActionLog
                                      actionLog) throws AmbariException {

    KerberosPrincipalEntity principalEntity = kerberosPrincipalDAO.find(principal);
    String cachedKeytabPath = (principalEntity == null) ? null : principalEntity.getCachedKeytabPath();

    if (cachedKeytabPath == null) {
      return false;
    }

    Keytab keytab = null;
    try {
      keytab = Keytab.read(new File(cachedKeytabPath));
    } catch (IOException e) {
      String message = String.format("Failed to read the cached keytab for %s, recreating if possible - %b",
        principal, e.getMessage());
      if (LOG.isDebugEnabled()) {
        LOG.warn(message, e);
      } else {
        LOG.warn(message, e);
      }
    }

    if (keytab == null) {
      return false;
    }

    File keytabFile = new File(keytabFilePath);
    ensureKeytabFolderExists(keytabFilePath);
    try {
      boolean created = operationHandler.createKeytabFile(keytab, keytabFile);
      String message = String.format("Keytab successfully created: %s for principal %s", created, principal);
      if (actionLog != null) {
        actionLog.writeStdOut(message);
      }
      if (created) {
        ensureAmbariOnlyAccess(keytabFile);
        configureJAAS(principal, keytabFilePath, actionLog);
      }
      return created;
    } catch (KerberosOperationException e) {
      String message = String.format("Failed to create keytab file for %s - %s", principal, e.getMessage());
      if (actionLog != null) {
        actionLog.writeStdErr(message);
      }
      LOG.error(message, e);
    }

    return false;
  }

  private void ensureKeytabFolderExists(String keytabFilePath) {
    String keytabFolderPath = keytabFilePath.substring(0, keytabFilePath.lastIndexOf("/"));
    File keytabFolder = new File(keytabFolderPath);
    if (!keytabFolder.exists() || !keytabFolder.isDirectory()) {
      keytabFolder.mkdir();
    }
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

  /**
   * Ensures that the owner of the Ambari server process is the only local user account able to
   * read and write to the specified file or read, write to, and execute the specified directory.
   *
   * @param file the file or directory for which to modify access
   */
  protected void ensureAmbariOnlyAccess(File file) throws AmbariException {
    if (file.exists()) {
      if (!file.setReadable(false, false) || !file.setReadable(true, true)) {
        String message = String.format("Failed to set %s readable only by Ambari", file.getAbsolutePath());
        LOG.warn(message);
        throw new AmbariException(message);
      }

      if (!file.setWritable(false, false) || !file.setWritable(true, true)) {
        String message = String.format("Failed to set %s writable only by Ambari", file.getAbsolutePath());
        LOG.warn(message);
        throw new AmbariException(message);
      }

      if (file.isDirectory()) {
        if (!file.setExecutable(false, false) || !file.setExecutable(true, true)) {
          String message = String.format("Failed to set %s executable by Ambari", file.getAbsolutePath());
          LOG.warn(message);
          throw new AmbariException(message);
        }
      } else {
        if (!file.setExecutable(false, false)) {
          String message = String.format("Failed to set %s not executable", file.getAbsolutePath());
          LOG.warn(message);
          throw new AmbariException(message);
        }
      }
    }
  }

}
