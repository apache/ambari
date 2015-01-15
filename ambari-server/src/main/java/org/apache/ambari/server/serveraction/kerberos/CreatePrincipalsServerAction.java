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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * CreatePrincipalsServerAction is a ServerAction implementation that creates principals as instructed.
 * <p/>
 * This class mainly relies on the KerberosServerAction to iterate through metadata identifying
 * the Kerberos principals that need to be created. For each identity in the metadata, this implementation's
 * {@link KerberosServerAction#processIdentity(java.util.Map, String, KerberosOperationHandler, java.util.Map)}
 * is invoked attempting the creation of the relevant principal.
 */
public class CreatePrincipalsServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(CreatePrincipalsServerAction.class);


  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosServerAction#processIdentities(java.util.Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link org.apache.ambari.server.serveraction.kerberos.CreatePrincipalsServerAction#processIdentities(java.util.Map)}
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
   * For each identity, generate a unique password create a new or update an existing principal in
   * an assume to be configured KDC.
   * <p/>
   * If a password has not been previously created the current evaluatedPrincipal, create a "secure"
   * password using {@link KerberosOperationHandler#createSecurePassword()}.  Then if the principal
   * does not exist in the KDC, create it using the generated password; else if it does exist update
   * its password.  Finally store the generated password in the shared principal-to-password map and
   * store the new key numbers in the shared principal-to-key_number map so that subsequent process
   * may use the data if necessary.
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

    Map<String, String> principalPasswordMap = getPrincipalPasswordMap(requestSharedDataContext);
    Map<String, Integer> principalKeyNumberMap = getPrincipalKeyNumberMap(requestSharedDataContext);

    String password = principalPasswordMap.get(evaluatedPrincipal);

    if (password == null) {
      password = operationHandler.createSecurePassword();

      try {
        if (operationHandler.principalExists(evaluatedPrincipal)) {
          // Create a new password since we need to know what it is.
          // A new password/key would have been generated after exporting the keytab anyways.
          LOG.warn("Principal already exists, setting new password - {}", evaluatedPrincipal);

          Integer keyNumber = operationHandler.setPrincipalPassword(evaluatedPrincipal, password);

          if (keyNumber != null) {
            principalPasswordMap.put(evaluatedPrincipal, password);
            principalKeyNumberMap.put(evaluatedPrincipal, keyNumber);
            LOG.debug("Successfully set password for principal {}", evaluatedPrincipal);
          } else {
            String message = String.format("Failed to set password for principal %s - unknown reason", evaluatedPrincipal);
            LOG.error(message);
            commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", "", message);
          }
        } else {
          LOG.debug("Creating new principal - {}", evaluatedPrincipal);
          boolean servicePrincipal = "service".equalsIgnoreCase(identityRecord.get(KerberosActionDataFile.PRINCIPAL_TYPE));
          Integer keyNumber = operationHandler.createPrincipal(evaluatedPrincipal, password, servicePrincipal);

          if (keyNumber != null) {
            principalPasswordMap.put(evaluatedPrincipal, password);
            principalKeyNumberMap.put(evaluatedPrincipal, keyNumber);
            LOG.debug("Successfully created new principal {}", evaluatedPrincipal);
          } else {
            String message = String.format("Failed to create principal %s - unknown reason", evaluatedPrincipal);
            LOG.error(message);
            commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", "", message);
          }
        }
      } catch (KerberosOperationException e) {
        String message = String.format("Failed to create principal %s - %s", evaluatedPrincipal, e.getMessage());
        LOG.error(message, e);
        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", "", message);
      }
    }

    return commandReport;
  }
}
