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
import org.apache.ambari.server.orm.dao.KerberosKeytabDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
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
 * {@link KerberosServerAction#processIdentity(ResolvedKerberosPrincipal, KerberosOperationHandler, Map, Map)}
 * is invoked attempting the removal of the relevant principal.
 */
public class DestroyPrincipalsServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(DestroyPrincipalsServerAction.class);

  @Inject
  private KerberosPrincipalDAO kerberosPrincipalDAO;

  @Inject
  private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  @Inject
  private KerberosKeytabDAO kerberosKeytabDAO;

  /**
   * A set of visited principal names used to prevent unnecessary processing on already processed
   * principal names
   */
  private Set<String> seenPrincipals = new HashSet<>();

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
   * @param resolvedPrincipal        a ResolvedKerberosPrincipal object to process
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
  protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {

    // Only process this principal if we haven't already processed it
    if (!seenPrincipals.contains(resolvedPrincipal.getPrincipal())) {
      seenPrincipals.add(resolvedPrincipal.getPrincipal());

      String message = String.format("Destroying identity, %s", resolvedPrincipal.getPrincipal());
      LOG.info(message);
      actionLog.writeStdOut(message);
      DestroyPrincipalKerberosAuditEvent.DestroyPrincipalKerberosAuditEventBuilder auditEventBuilder = DestroyPrincipalKerberosAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestId(getHostRoleCommand().getRequestId())
          .withTaskId(getHostRoleCommand().getTaskId())
          .withPrincipal(resolvedPrincipal.getPrincipal());

      try {
        try {
          boolean servicePrincipal = resolvedPrincipal.isService();
          operationHandler.removePrincipal(resolvedPrincipal.getPrincipal(), servicePrincipal);
        } catch (KerberosOperationException e) {
          message = String.format("Failed to remove identity for %s from the KDC - %s", resolvedPrincipal.getPrincipal(), e.getMessage());
          LOG.warn(message);
          actionLog.writeStdErr(message);
          auditEventBuilder.withReasonOfFailure(message);
        }

        try {
          KerberosPrincipalEntity principalEntity = kerberosPrincipalDAO.find(resolvedPrincipal.getPrincipal());

          if (principalEntity != null) {
            String cachedKeytabPath = principalEntity.getCachedKeytabPath();
            KerberosKeytabEntity kke = kerberosKeytabDAO.find(resolvedPrincipal.getResolvedKerberosKeytab().getFile());
            kerberosKeytabDAO.remove(kke);
            kerberosPrincipalDAO.remove(principalEntity);

            // If a cached  keytabs file exists for this principal, delete it.
            if (cachedKeytabPath != null) {
              if (!new File(cachedKeytabPath).delete()) {
                LOG.debug("Failed to remove cached keytab for {}", resolvedPrincipal.getPrincipal());
              }
            }
          }

          // delete Ambari server keytab
          String hostName = resolvedPrincipal.getHostName();
          if (hostName != null && hostName.equalsIgnoreCase(KerberosHelper.AMBARI_SERVER_HOST_NAME)) {
            ResolvedKerberosKeytab resolvedKeytab = resolvedPrincipal.getResolvedKerberosKeytab();
            if (resolvedKeytab != null) {
              String keytabFilePath = resolvedKeytab.getFile();
              if (keytabFilePath != null) {
                try {
                  ShellCommandUtil.Result result = ShellCommandUtil.delete(keytabFilePath, true, true);
                  if (!result.isSuccessful()) {
                    LOG.warn("Failed to remove ambari keytab for {} due to {}", resolvedPrincipal.getPrincipal(), result.getStderr());
                  }
                } catch (IOException|InterruptedException e) {
                  LOG.warn("Failed to remove ambari keytab for " + resolvedPrincipal.getPrincipal(), e);
                }
              }
            }
          }
        } catch (Throwable t) {
          message = String.format("Failed to remove identity for %s from the Ambari database - %s", resolvedPrincipal.getPrincipal(), t.getMessage());
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
