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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFile.HOSTNAME;
import static org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFile.KEYTAB_FILE_PATH;

/**
 * CreateKeytabFilesServerAction is a ServerAction implementation that creates keytab files as
 * instructed.
 * <p/>
 * This class mainly relies on the KerberosServerAction to iterate through metadata identifying
 * the Kerberos keytab files that need to be created. For each identity in the metadata, this
 * implementation's
 * {@link KerberosServerAction#processIdentity(java.util.Map, String, KerberosOperationHandler, java.util.Map)}
 * is invoked attempting the creation of the relevant keytab file.
 */
public class CreateKeytabFilesServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(CreateKeytabFilesServerAction.class);

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosServerAction#processIdentities(java.util.Map)} )}
   * to iterate through the Kerberos identity metadata and call
   * {@link org.apache.ambari.server.serveraction.kerberos.CreateKeytabFilesServerAction#processIdentities(java.util.Map)}
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
   * For each identity, create a keytab and append to a new or existing keytab file.
   * <p/>
   * It is expected that the {@link org.apache.ambari.server.serveraction.kerberos.CreatePrincipalsServerAction}
   * (or similar) has executed before this action and a set of passwords has been created, map to
   * their relevant (evaluated) principals and stored in the requestSharedDataContext.
   * <p/>
   * If a password exists for the current evaluatedPrincipal, use a
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosOperationHandler} to generate
   * the keytab file. To help avoid filename collisions and to build a structure that is easy to
   * discover, each keytab file is stored in host-specific
   * ({@link org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFile#HOSTNAME})
   * directory using the SHA1 hash of its destination file path
   * ({@link org.apache.ambari.server.serveraction.kerberos.KerberosActionDataFile#KEYTAB_FILE_PATH})
   * <p/>
   * <pre>
   *   data_directory
   *   |- host1
   *   |  |- 16a054404c8826cd604a27ac970e8cc4b9c7a3fa   (keytab file)
   *   |  |- ...                                        (keytab files)
   *   |  |- a3c09cae73406912e8c55296d1c85b674d24f576   (keytab file)
   *   |- host2
   *   |  |- ...
   * </pre>
   *
   * @param identityRecord           a Map containing the data for the current identity record
   * @param evaluatedPrincipal       a String indicating the relevant principal
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport, indicating an error condition; or null, indicating a success condition
   * @throws AmbariException if an error occurs while processing the identity record
   */
  @Override
  protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {
    CommandReport commandReport = null;

    if (identityRecord != null) {
      String message = String.format("Creating keytab file for %s", evaluatedPrincipal);
      LOG.info(message);
      actionLog.writeStdOut(message);

      if (operationHandler == null) {
        message = String.format("Failed to create keytab file for %s, missing KerberosOperationHandler", evaluatedPrincipal);
        actionLog.writeStdErr(message);
        LOG.error(message);
        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
      } else {
        Map<String, String> principalPasswordMap = getPrincipalPasswordMap(requestSharedDataContext);
        Map<String, Integer> principalKeyNumberMap = getPrincipalKeyNumberMap(requestSharedDataContext);

        String host = identityRecord.get(HOSTNAME);
        String keytabFilePath = identityRecord.get(KEYTAB_FILE_PATH);

        if ((host != null) && !host.isEmpty() && (keytabFilePath != null) && !keytabFilePath.isEmpty()) {
          // Look up the current evaluatedPrincipal's password.
          // If found create th keytab file, else skip it.
          String password = principalPasswordMap.get(evaluatedPrincipal);

          if (password == null) {
            message = String.format("Failed to create keytab file for %s, missing password", evaluatedPrincipal);
            actionLog.writeStdErr(message);
            LOG.error(message);
            commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
          } else {
            // Determine where to store the keytab file.  It should go into a host-specific
            // directory under the previously determined data directory.
            File hostDirectory = new File(getDataDirectoryPath(), host);

            // Ensure the host directory exists...
            if (hostDirectory.exists() || hostDirectory.mkdirs()) {
              File keytabFile = new File(hostDirectory, DigestUtils.sha1Hex(keytabFilePath));
              Integer keyNumber = principalKeyNumberMap.get(evaluatedPrincipal);

              try {
                if (operationHandler.createKeytabFile(evaluatedPrincipal, password, keyNumber, keytabFile)) {
                  message = String.format("Successfully created keytab file for %s at %s", evaluatedPrincipal, keytabFile.getAbsolutePath());
                  LOG.debug(message);
                } else {
                  message = String.format("Failed to create keytab file for %s at %s", evaluatedPrincipal, keytabFile.getAbsolutePath());
                  actionLog.writeStdErr(message);
                  LOG.error(message);
                  commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
                }
              } catch (KerberosOperationException e) {
                message = String.format("Failed to create keytab file for %s - %s", evaluatedPrincipal, e.getMessage());
                actionLog.writeStdErr(message);
                LOG.error(message, e);
                commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
              }
            } else {
              message = String.format("Failed to create keytab file for %s, the container directory does not exist: %s",
                  evaluatedPrincipal, hostDirectory.getAbsolutePath());
              actionLog.writeStdErr(message);
              LOG.error(message);
              commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
            }
          }
        }
      }
    }

    return commandReport;
  }
}
