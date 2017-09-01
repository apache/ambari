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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PrepareKerberosIdentitiesServerAction is a ServerAction implementation that prepares metadata needed
 * to process Kerberos identities (principals and keytabs files).
 */
public class PrepareKerberosIdentitiesServerAction extends AbstractPrepareKerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(PrepareKerberosIdentitiesServerAction.class);

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link PrepareKerberosIdentitiesServerAction#processIdentities(Map)}
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

    KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster, false);
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

    KerberosHelper kerberosHelper = getKerberosHelper();
    Set<String> services = cluster.getServices().keySet();
    Map<String, Set<String>> propertiesToRemove = new HashMap<>();
    Map<String, Set<String>> propertiesToIgnore = new HashMap<>();
    boolean includeAmbariIdentity = "true".equalsIgnoreCase(getCommandParameterValue(commandParameters, KerberosServerAction.INCLUDE_AMBARI_IDENTITY));

    // Calculate the current host-specific configurations. These will be used to replace
    // variables within the Kerberos descriptor data
    Map<String, Map<String, String>> configurations = kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor, false, false);

    processServiceComponentHosts(cluster, kerberosDescriptor, schToProcess, identityFilter, dataDirectory,
        configurations, kerberosConfigurations, includeAmbariIdentity, propertiesToIgnore);

    kerberosHelper.applyStackAdvisorUpdates(cluster, services, configurations, kerberosConfigurations,
        propertiesToIgnore, propertiesToRemove, true);

    if ("true".equalsIgnoreCase(getCommandParameterValue(commandParameters, UPDATE_CONFIGURATIONS))) {
      Map<String, Map<String, String>> calculatedConfigurations = kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor, false, false);
      processAuthToLocalRules(cluster, calculatedConfigurations, kerberosDescriptor, schToProcess, kerberosConfigurations, getDefaultRealm(commandParameters), false);
      processConfigurationChanges(dataDirectory, kerberosConfigurations, propertiesToRemove);
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }

  @Override
  protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {
    throw new UnsupportedOperationException();
  }

  /**
   * Calls {@link KerberosHelper#getServiceComponentHostsToProcess(Cluster, KerberosDescriptor, Map, Collection, Collection, KerberosHelper.Command)}
   * with no filter on ServiceComponentHosts
   * <p/>
   * The <code>shouldProcessCommand</code> implementation passed to KerberosHelper#getServiceComponentHostsToProcess
   * always returns true, indicating to process all ServiceComponentHosts.
   *
   * @param cluster            the cluster
   * @param kerberosDescriptor the current Kerberos descriptor
   * @param identityFilter     a list of identities to include, or all if null  @return the list of ServiceComponentHosts to process
   * @throws AmbariException
   * @see KerberosHelper#getServiceComponentHostsToProcess(Cluster, KerberosDescriptor, Map, Collection, Collection, KerberosHelper.Command)
   */
  protected List<ServiceComponentHost> getServiceComponentHostsToProcess(Cluster cluster,
                                                                         KerberosDescriptor kerberosDescriptor,
                                                                         Collection<String> identityFilter)
      throws AmbariException {
    return getKerberosHelper().getServiceComponentHostsToProcess(cluster,
        kerberosDescriptor,
        getServiceComponentFilter(),
        getHostFilter(), identityFilter,
        new KerberosHelper.Command<Boolean, ServiceComponentHost>() {
          @Override
          public Boolean invoke(ServiceComponentHost sch) throws AmbariException {
            return true;
          }
        });
  }

  /**
   * Calls {@link KerberosHelper#getKerberosDescriptor(Cluster, boolean)}
   *
   * @param cluster                 cluster instance
   * @param includePreconfigureData <code>true</code> to include the preconfigure data; <code>false</code> otherwise
   * @return the kerberos descriptor associated with the specified cluster
   * @throws AmbariException if unable to obtain the descriptor
   * @see KerberosHelper#getKerberosDescriptor(Cluster, boolean)
   */
  protected KerberosDescriptor getKerberosDescriptor(Cluster cluster, boolean includePreconfigureData)
      throws AmbariException {
    return getKerberosHelper().getKerberosDescriptor(cluster, includePreconfigureData);
  }

  /**
   * Conditionally calls {@link KerberosHelper#setAuthToLocalRules(Cluster, KerberosDescriptor, String, Map, Map, Map, boolean)}
   * if there are ServiceComponentHosts to process
   *
   * @param cluster                  the cluster
   * @param calculatedConfiguration  the configurations for the current cluster, used for replacements
   * @param kerberosDescriptor       the current Kerberos descriptor
   * @param schToProcess             a list of ServiceComponentHosts to process
   * @param kerberosConfigurations   the Kerberos-specific configuration map
   * @param defaultRealm             the default realm
   * @param includePreconfiguredData true to include services flagged to be pre-configured; false otherwise
   * @throws AmbariException
   * @see KerberosHelper#setAuthToLocalRules(Cluster, KerberosDescriptor, String, Map, Map, Map, boolean)
   */
  void processAuthToLocalRules(Cluster cluster, Map<String, Map<String, String>> calculatedConfiguration,
                               KerberosDescriptor kerberosDescriptor,
                               List<ServiceComponentHost> schToProcess,
                               Map<String, Map<String, String>> kerberosConfigurations,
                               String defaultRealm, boolean includePreconfiguredData)
      throws AmbariException {
    if (!schToProcess.isEmpty()) {
      actionLog.writeStdOut("Creating auth-to-local rules");

      Map<String, Set<String>> services = new HashMap<>();
      for (ServiceComponentHost sch : schToProcess) {
        Set<String> components = services.get(sch.getServiceName());
        if (components == null) {
          components = new HashSet<>();
          services.put(sch.getServiceName(), components);
        }

        components.add(sch.getServiceComponentName());
      }

      KerberosHelper kerberosHelper = getKerberosHelper();
      kerberosHelper.setAuthToLocalRules(cluster, kerberosDescriptor, defaultRealm, services,
          calculatedConfiguration, kerberosConfigurations, includePreconfiguredData);
    }
  }
}

