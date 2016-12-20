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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.audit.event.kerberos.DestroyPrincipalKerberosAuditEvent;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * DestroyPrincipalsServerAction is a ServerAction implementation that destroys principals as instructed.
 * <p/>
 * This class mainly relies on the KerberosServerAction to iterate through metadata identifying
 * the Kerberos principals that need to be removed from the relevant KDC. For each identity in the
 * metadata, this implementation's
 * {@link KerberosServerAction#processIdentity(Map, String, KerberosOperationHandler, Map, Map)}
 * is invoked attempting the removal of the relevant principal.
 */
public class DestroyPrincipalsServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(DestroyPrincipalsServerAction.class);

  @Inject
  private KerberosPrincipalDAO kerberosPrincipalDAO;

  /**
   * A set of visited principal names used to prevent unnecessary processing on already processed
   * principal names
   */
  private Set<String> seenPrincipals = new HashSet<String>();

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(java.util.Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link org.apache.ambari.server.serveraction.kerberos.DestroyPrincipalsServerAction#processIdentities(java.util.Map)}
   * for each identity to process.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport indicating the result of this action
   * @throws org.apache.ambari.server.AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws
      AmbariException, InterruptedException {
    return processIdentities(requestSharedDataContext);
  }


  /**
   * For each identity, remove the principal from the configured KDC.
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
   * @throws org.apache.ambari.server.AmbariException if an error occurs while processing the identity record
   */
  @Override
  protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {

    // Only process this principal if we haven't already processed it
    if (!seenPrincipals.contains(evaluatedPrincipal)) {
      seenPrincipals.add(evaluatedPrincipal);

      String message = String.format("Destroying identity, %s", evaluatedPrincipal);
      LOG.info(message);
      actionLog.writeStdOut(message);
      DestroyPrincipalKerberosAuditEvent.DestroyPrincipalKerberosAuditEventBuilder auditEventBuilder = DestroyPrincipalKerberosAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestId(getHostRoleCommand().getRequestId())
          .withTaskId(getHostRoleCommand().getTaskId())
          .withPrincipal(evaluatedPrincipal);

      try {
        try {
          operationHandler.removePrincipal(evaluatedPrincipal);
        } catch (KerberosOperationException e) {
          message = String.format("Failed to remove identity for %s from the KDC - %s", evaluatedPrincipal, e.getMessage());
          LOG.warn(message);
          actionLog.writeStdErr(message);
          auditEventBuilder.withReasonOfFailure(message);
        }

        try {
          KerberosPrincipalEntity principalEntity = kerberosPrincipalDAO.find(evaluatedPrincipal);

          if (principalEntity != null) {
            String cachedKeytabPath = principalEntity.getCachedKeytabPath();

            kerberosPrincipalDAO.remove(principalEntity);

            // If a cached  keytabs file exists for this principal, delete it.
            if (cachedKeytabPath != null) {
              if (!new File(cachedKeytabPath).delete()) {
                LOG.debug(String.format("Failed to remove cached keytab for %s", evaluatedPrincipal));
              }
            }
          }

          // delete Ambari server keytab
          String hostName = identityRecord.get(KerberosIdentityDataFileReader.HOSTNAME);
          if (hostName != null && hostName.equalsIgnoreCase(KerberosHelper.AMBARI_SERVER_HOST_NAME)) {
            String keytabFilePath = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH);
            if (keytabFilePath != null) {
              try {
                ShellCommandUtil.Result result = ShellCommandUtil.delete(keytabFilePath, true, true);
                if (!result.isSuccessful()) {
                  LOG.warn("Failed to remove ambari keytab for {} due to {}", evaluatedPrincipal, result.getStderr());
                }
              } catch (IOException|InterruptedException e) {
                LOG.warn("Failed to remove ambari keytab for " + evaluatedPrincipal, e);
              }
            }
          }
        } catch (Throwable t) {
          message = String.format("Failed to remove identity for %s from the Ambari database - %s", evaluatedPrincipal, t.getMessage());
          LOG.warn(message);
          actionLog.writeStdErr(message);
          auditEventBuilder.withReasonOfFailure(message);
        }
      } finally {
        auditLog(auditEventBuilder.build());
      }
    }

    // There is no reason to fail this task if an identity was not removed. The cluster will work
    // just fine if this cleanup process fails.
    return null;
  }
}
