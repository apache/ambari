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
import org.apache.ambari.server.audit.event.kerberos.ChangeSecurityStateKerberosAuditEvent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class FinalizeKerberosServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(FinalizeKerberosServerAction.class);

  /**
   * Processes an identity as necessary.
   * <p/>
   * This method is not used since the {@link #processIdentities(java.util.Map)} is not invoked
   *
   * @param identityRecord           a Map containing the data for the current identity record
   * @param evaluatedPrincipal       a String indicating the relevant principal
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param kerberosConfiguration    a Map of configuration properties from kerberos-env
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request  @return null, always
   * @throws AmbariException
   */
  @Override
  protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {

    return null;
  }

  /**
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return
   * @throws AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    String dataDirectoryPath = getCommandParameterValue(DATA_DIRECTORY);

    // Set the ServiceComponentHost from a transitional state to the desired endpoint state
    Map<String, Host> hosts = getClusters().getHostsForCluster(getClusterName());
    if ((hosts != null) && !hosts.isEmpty()) {
      Cluster cluster = getCluster();
      for (String hostname : hosts.keySet()) {
        List<ServiceComponentHost> serviceComponentHosts = cluster.getServiceComponentHosts(hostname);

        for (ServiceComponentHost sch : serviceComponentHosts) {
          SecurityState securityState = sch.getSecurityState();
          if (securityState.isTransitional()) {
            String message = String.format("Setting securityState for %s/%s on host %s to state %s",
                sch.getServiceName(), sch.getServiceComponentName(), sch.getHostName(),
                sch.getDesiredSecurityState().toString());
            LOG.info(message);
            actionLog.writeStdOut(message);

            sch.setSecurityState(sch.getDesiredSecurityState());
            ChangeSecurityStateKerberosAuditEvent auditEvent = ChangeSecurityStateKerberosAuditEvent.builder()
              .withTimestamp(System.currentTimeMillis())
              .withService(sch.getServiceName())
              .withComponent(sch.getServiceComponentName())
              .withHostName(sch.getHostName())
              .withState(sch.getDesiredSecurityState().toString())
              .build();
            auditLog(auditEvent);
          }
        }
      }
    }

    // Make sure this is a relevant directory. We don't want to accidentally allow _ANY_ directory
    // to be deleted.
    if ((dataDirectoryPath != null) && dataDirectoryPath.contains("/" + DATA_DIRECTORY_PREFIX)) {
      File dataDirectory = new File(dataDirectoryPath);
      File dataDirectoryParent = dataDirectory.getParentFile();

      // Make sure this directory has a parent and it is writeable, else we wont be able to
      // delete the directory
      if ((dataDirectoryParent != null) && dataDirectory.isDirectory() &&
          dataDirectoryParent.isDirectory() && dataDirectoryParent.canWrite()) {
        try {
          FileUtils.deleteDirectory(dataDirectory);
        } catch (IOException e) {
          // We should log this exception, but don't let it fail the process since if we got to this
          // KerberosServerAction it is expected that the the overall process was a success.
          String message = String.format("The data directory (%s) was not deleted due to an error condition - {%s}",
              dataDirectory.getAbsolutePath(), e.getMessage());
          LOG.warn(message, e);
        }
      }
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }
}
