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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.audit.event.kerberos.ChangeSecurityStateKerberosAuditEvent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinalizeKerberosServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(FinalizeKerberosServerAction.class);

  /**
   * Processes an identity as necessary.
   * <p/>
   * This implementation ensures that keytab files for the Ambari identities have the correct
   * permissions.  This is important in the event a secure cluster was created via Blueprints since
   * some user accounts and groups may not have been available (at the OS level) when the keytab files
   * were created.
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

    if (identityRecord != null) {
      // If the record's HOSTNAME value is "ambari-server", rather than an actual hostname it will
      // not match the Ambari server's host name. This will occur if the there is no agent installed
      // on the Ambari server host.  This is ok, since any keytab files installed on the Ambari server
      // host will already have the permissions set so that only the Ambari server can read it.
      // There is no need to update the permissions for those keytab files so that installed services
      // can access them since no services will be installed on the host.
      if (StageUtils.getHostName().equals(identityRecord.get(KerberosIdentityDataFile.HOSTNAME))) {

        // If the principal name exists in one of the shared data maps, it has been processed by the
        // current "Enable Kerberos" or "Add component" workflow and therefore should already have
        // the correct permissions assigned. The relevant keytab files can be skipped.
        Map<String, String> principalPasswordMap = getPrincipalPasswordMap(requestSharedDataContext);
        if ((principalPasswordMap == null) || !principalPasswordMap.containsKey(evaluatedPrincipal)) {

          String keytabFilePath = identityRecord.get(KerberosIdentityDataFile.KEYTAB_FILE_PATH);

          if (!StringUtils.isEmpty(keytabFilePath)) {
            Set<String> visited = (Set<String>) requestSharedDataContext.get(this.getClass().getName() + "_visited");

            if (!visited.contains(keytabFilePath)) {
              String ownerName = identityRecord.get(KerberosIdentityDataFile.KEYTAB_FILE_OWNER_NAME);
              String ownerAccess = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS);
              boolean ownerWritable = "w".equalsIgnoreCase(ownerAccess) || "rw".equalsIgnoreCase(ownerAccess);
              boolean ownerReadable = "r".equalsIgnoreCase(ownerAccess) || "rw".equalsIgnoreCase(ownerAccess);
              String groupName = identityRecord.get(KerberosIdentityDataFile.KEYTAB_FILE_GROUP_NAME);
              String groupAccess = identityRecord.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS);
              boolean groupWritable = "w".equalsIgnoreCase(groupAccess) || "rw".equalsIgnoreCase(groupAccess);
              boolean groupReadable = "r".equalsIgnoreCase(groupAccess) || "rw".equalsIgnoreCase(groupAccess);

              ShellCommandUtil.Result result;
              String message;

              result = ShellCommandUtil.setFileOwner(keytabFilePath, ownerName);
              if (result.isSuccessful()) {
                message = String.format("Updated the owner of the keytab file at %s to %s",
                    keytabFilePath, ownerName);
                LOG.info(message);
                actionLog.writeStdOut(message);
              } else {
                message = String.format("Failed to update the owner of the keytab file at %s to %s: %s",
                    keytabFilePath, ownerName, result.getStderr());
                LOG.error(message);
                actionLog.writeStdOut(message);
                actionLog.writeStdErr(message);
              }

              result = ShellCommandUtil.setFileGroup(keytabFilePath, groupName);
              if (result.isSuccessful()) {
                message = String.format("Updated the group of the keytab file at %s to %s",
                    keytabFilePath, groupName);
                LOG.info(message);
                actionLog.writeStdOut(message);
              } else {
                message = String.format("Failed to update the group of the keytab file at %s to %s: %s",
                    keytabFilePath, groupName, result.getStderr());
                LOG.error(message);
                actionLog.writeStdOut(message);
                actionLog.writeStdErr(message);
              }

              result = ShellCommandUtil.setFileMode(keytabFilePath,
                  ownerReadable, ownerWritable, false,
                  groupReadable, groupWritable, false,
                  false, false, false);
              if (result.isSuccessful()) {
                message = String.format("Updated the access mode of the keytab file at %s to owner:'%s' and group:'%s'",
                    keytabFilePath, ownerAccess, groupAccess);
                LOG.info(message);
                actionLog.writeStdOut(message);
              } else {
                message = String.format("Failed to update the access mode of the keytab file at %s to owner:'%s' and group:'%s': %s",
                    keytabFilePath, ownerAccess, groupAccess, result.getStderr());
                LOG.error(message);
                actionLog.writeStdOut(message);
                actionLog.writeStdErr(message);
              }

              visited.add(keytabFilePath);
            }
          }
        }
      }
    }

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
                .withRequestId(getHostRoleCommand().getRequestId())
                .withTaskId(getHostRoleCommand().getTaskId())
                .build();
            auditLog(auditEvent);
          }
        }
      }
    }

    // Ensure the keytab files for the Ambari identities have the correct permissions
    // This is important in the event a secure cluster was created via Blueprints since some
    // user accounts and group may not have been created when the keytab files were created.
    requestSharedDataContext.put(this.getClass().getName() + "_visited", new HashSet<String>());
    processIdentities(requestSharedDataContext);
    requestSharedDataContext.remove(this.getClass().getName() + "_visited");

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
