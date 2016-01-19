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
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * PrepareEnableKerberosServerAction is a ServerAction implementation that prepares metadata needed
 * to enable Kerberos on the cluster.
 */
public class PrepareEnableKerberosServerAction extends PrepareKerberosIdentitiesServerAction {

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link PrepareEnableKerberosServerAction#processIdentities(Map)}
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

    Cluster cluster = getCluster();

    if (cluster == null) {
      throw new AmbariException("Missing cluster object");
    }

    KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster);
    Collection<String> identityFilter = getIdentityFilter();
    List<ServiceComponentHost> schToProcess = getServiceComponentHostsToProcess(cluster, kerberosDescriptor, identityFilter);

    Map<String, String> commandParameters = getCommandParameters();
    String dataDirectory = getCommandParameterValue(commandParameters, DATA_DIRECTORY);
    Map<String, Map<String, String>> kerberosConfigurations = new HashMap<String, Map<String, String>>();

    int schCount = schToProcess.size();
    if (schCount == 0) {
      actionLog.writeStdOut("There are no components to process");
    } else if (schCount == 1) {
      actionLog.writeStdOut(String.format("Processing %d component", schCount));
    } else {
      actionLog.writeStdOut(String.format("Processing %d components", schCount));
    }

    processServiceComponentHosts(cluster, kerberosDescriptor, schToProcess, identityFilter, dataDirectory, kerberosConfigurations, true);
    processAuthToLocalRules(cluster, kerberosDescriptor, schToProcess, kerberosConfigurations, getDefaultRealm(commandParameters));

    // Ensure the cluster-env/security_enabled flag is set properly
    Map<String, String> clusterEnvProperties = kerberosConfigurations.get(KerberosHelper.SECURITY_ENABLED_CONFIG_TYPE);
    if (clusterEnvProperties == null) {
      clusterEnvProperties = new HashMap<String, String>();
      kerberosConfigurations.put(KerberosHelper.SECURITY_ENABLED_CONFIG_TYPE, clusterEnvProperties);
    }
    clusterEnvProperties.put(KerberosHelper.SECURITY_ENABLED_PROPERTY_NAME, "true");

    processConfigurationChanges(dataDirectory, kerberosConfigurations);

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }

  @Override
  protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal, KerberosOperationHandler operationHandler, Map<String, String> kerberosConfiguration, Map<String, Object> requestSharedDataContext) throws AmbariException {
    throw new UnsupportedOperationException();
  }
}

