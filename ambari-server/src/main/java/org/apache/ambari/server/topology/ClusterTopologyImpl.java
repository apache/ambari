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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_AND_START;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_ONLY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.BaseClusterRequest;
import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Represents a cluster topology.
 * Topology includes the the associated blueprint, cluster configuration and hostgroup -> host mapping.
 */
public class ClusterTopologyImpl implements ClusterTopology {

  private final static Logger LOG = LoggerFactory.getLogger(ClusterTopologyImpl.class);

  private final Set<StackId> stackIds;
  private final StackDefinition stack;
  private Long clusterId;
  private final Blueprint blueprint;
  private final Configuration configuration;
  private final ConfigRecommendationStrategy configRecommendationStrategy;
  private final ProvisionAction provisionAction;
  private final Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
  private final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();
  private final AmbariContext ambariContext;
  private final BlueprintBasedClusterProvisionRequest provisionRequest;
  private final String defaultPassword;
  private final Map<String, Set<ResolvedComponent>> resolvedComponents;

  public ClusterTopologyImpl(AmbariContext ambariContext, TopologyRequest topologyRequest) throws InvalidTopologyException {
    this.ambariContext = ambariContext;
    this.clusterId = topologyRequest.getClusterId();
    this.blueprint = topologyRequest.getBlueprint();
    this.configuration = topologyRequest.getConfiguration();
    configRecommendationStrategy = ConfigRecommendationStrategy.NEVER_APPLY;
    provisionAction = topologyRequest instanceof BaseClusterRequest ? ((BaseClusterRequest) topologyRequest).getProvisionAction() : INSTALL_AND_START; // FIXME

    provisionRequest = null;
    defaultPassword = null;
    stackIds = ImmutableSet.of();
    stack = null;
    resolvedComponents = ImmutableMap.of();

    registerHostGroupInfo(topologyRequest.getHostGroupInfo());
  }

  // FIXME 2. replayed request should simply be a provision or scale request
  // FIXME 3. do not create a ClusterTopologyImpl for scale request -- create for original provision request only
  public ClusterTopologyImpl(
    AmbariContext ambariContext,
    BlueprintBasedClusterProvisionRequest request,
    Map<String, Set<ResolvedComponent>> resolvedComponents
  ) throws InvalidTopologyException {
    this.ambariContext = ambariContext;
    this.blueprint = request.getBlueprint();
    this.configuration = request.getConfiguration();
    this.provisionRequest = request;
    this.resolvedComponents = resolvedComponents;
    configRecommendationStrategy = request.getConfigRecommendationStrategy();
    provisionAction = request.getProvisionAction();

    defaultPassword = provisionRequest.getDefaultPassword();
    stackIds = request.getStackIds();
    stack = request.getStack();

    blueprint.getConfiguration().setParentConfiguration(stack.getConfiguration(getServices()));
    registerHostGroupInfo(request.getHostGroupInfo());
  }

  @Override
  public void update(TopologyRequest topologyRequest) throws InvalidTopologyException {
    registerHostGroupInfo(topologyRequest.getHostGroupInfo());
  }

  @Override
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public Blueprint getBlueprint() {
    return blueprint;
  }

  @Override
  public String getBlueprintName() {
    return blueprint.getName();
  }

  @Override
  public Set<StackId> getStackIds() {
    return stackIds;
  }

  @Override
  public StackDefinition getStack() {
    return stack;
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public Setting getSetting() {
    return provisionRequest.getSetting();
  }

  @Override
  public Map<String, HostGroupInfo> getHostGroupInfo() {
    return hostGroupInfoMap;
  }

  @Override
  public Collection<HostGroup> getHostGroups() {
    return blueprint.getHostGroups().values();
  }

  @Override
  public Collection<String> getHostGroupsForComponent(String component) {
    return resolvedComponents.entrySet().stream()
      .filter(e -> e.getValue().stream().anyMatch(c -> component.equals(c.componentName())))
      .map(Map.Entry::getKey)
      .collect(toSet());
  }

  @Override
  public String getHostGroupForHost(String hostname) {
    // FIXME change to map lookup
    for (HostGroupInfo groupInfo : hostGroupInfoMap.values() ) {
      if (groupInfo.getHostNames().contains(hostname)) {
        // a host can only be associated with a single host group
        return groupInfo.getHostGroupName();
      }
    }
    return null;
  }

  //todo: host info?
  @Override
  public void addHostToTopology(String hostGroupName, String host) throws InvalidTopologyException, NoSuchHostGroupException {
    if (blueprint.getHostGroup(hostGroupName) == null) {
      throw new NoSuchHostGroupException("Attempted to add host to non-existing host group: " + hostGroupName);
    }

    // check for host duplicates
    String groupContainsHost = getHostGroupForHost(host);
    // in case of reserved host, hostgroup will already contain host
    if (groupContainsHost != null && ! hostGroupName.equals(groupContainsHost)) {
      throw new InvalidTopologyException(String.format(
          "Attempted to add host '%s' to hostgroup '%s' but it is already associated with hostgroup '%s'.",
          host, hostGroupName, groupContainsHost));
    }

    synchronized(hostGroupInfoMap) {
      HostGroupInfo existingHostGroupInfo = hostGroupInfoMap.get(hostGroupName);
      if (existingHostGroupInfo == null) {
        throw new RuntimeException(String.format("An attempt was made to add host '%s' to an unregistered hostgroup '%s'",
            host, hostGroupName));
      }
      // ok to add same host multiple times to same group
      existingHostGroupInfo.addHost(host);

      LOG.info("ClusterTopologyImpl.addHostTopology: added host = " + host + " to host group = " + existingHostGroupInfo.getHostGroupName());
    }
  }

  @Override
  public Collection<String> getHostAssignmentsForComponent(String component) {
    //todo: ordering requirements?
    Collection<String> hosts = new ArrayList<>();
    Collection<String> hostGroups = getHostGroupsForComponent(component);
    for (String group : hostGroups) {
      HostGroupInfo hostGroupInfo = getHostGroupInfo().get(group);
      if (hostGroupInfo != null) {
        hosts.addAll(hostGroupInfo.getHostNames());
      } else {
        LOG.warn("HostGroup {} not found, when checking for hosts for component {}", group, component);
      }
    }
    return hosts;
  }

  @Override
  public Collection<String> getServices() {
    return getComponents()
      .map(ResolvedComponent::effectiveServiceName)
      .collect(toSet());
  }

  @Override
  public Stream<ResolvedComponent> getComponents() {
    return resolvedComponents.values().stream()
      .flatMap(Collection::stream);
  }

  @Override @Nonnull
  public Stream<ResolvedComponent> getComponentsInHostGroup(String hostGroup) {
    return resolvedComponents.computeIfAbsent(hostGroup, __ -> ImmutableSet.of()).stream();
  }

  @Override
  public boolean containsMasterComponent(String hostGroup) {
    return resolvedComponents.getOrDefault(hostGroup, ImmutableSet.of()).stream()
      .anyMatch(ResolvedComponent::masterComponent);
  }

  @Override
  public boolean isValidConfigType(String configType) {
    if (ConfigHelper.CLUSTER_ENV.equals(configType) || "global".equals(configType)) {
      return true;
    }
    try {
      String service = getStack().getServiceForConfigType(configType);
      return getServices().contains(service);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  // FIXME move out
  private void validateTopology()
      throws InvalidTopologyException {

    if (BlueprintConfigurationProcessor.isNameNodeHAEnabled(getConfiguration().getFullProperties())) {
        Collection<String> nnHosts = getHostAssignmentsForComponent("NAMENODE");
        if (nnHosts.size() != 2) {
            throw new InvalidTopologyException("NAMENODE HA requires exactly 2 hosts running NAMENODE but there are: " +
                nnHosts.size() + " Hosts: " + nnHosts);
        }
        Map<String, String> hadoopEnvConfig = configuration.getFullProperties().get("hadoop-env");
        if(hadoopEnvConfig != null && !hadoopEnvConfig.isEmpty() && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_active") && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_standby")) {
           if ((!BlueprintConfigurationProcessor.HOST_GROUP_PLACEHOLDER_PATTERN.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_active")).matches() && !nnHosts.contains(hadoopEnvConfig.get("dfs_ha_initial_namenode_active")))
             || (!BlueprintConfigurationProcessor.HOST_GROUP_PLACEHOLDER_PATTERN.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby")).matches() && !nnHosts.contains(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby")))) {
              throw new IllegalArgumentException("NAMENODE HA hosts mapped incorrectly for properties 'dfs_ha_initial_namenode_active' and 'dfs_ha_initial_namenode_standby'. Expected hosts are: " + nnHosts);
        }
        }
    }
  }

  @Override
  public boolean isClusterKerberosEnabled() {
    return ambariContext.isClusterKerberosEnabled(getClusterId());
  }

  @Override
  public RequestStatusResponse installHost(String hostName, boolean skipInstallTaskCreate, boolean skipFailure) {
    try {
      String hostGroupName = getHostGroupForHost(hostName);
      HostGroup hostGroup = this.blueprint.getHostGroup(hostGroupName);

      Collection<String> skipInstallForComponents = new ArrayList<>();
      if (skipInstallTaskCreate) {
        skipInstallForComponents.add("ALL");
      } else {
        // get the set of components that are marked as START_ONLY for this hostgroup
        skipInstallForComponents.addAll(hostGroup.getComponentNames(ProvisionAction.START_ONLY));
      }

      Collection<String> dontSkipInstallForComponents = hostGroup.getComponentNames(INSTALL_ONLY);
      dontSkipInstallForComponents.addAll(hostGroup.getComponentNames(INSTALL_AND_START));

      return ambariContext.installHost(hostName, ambariContext.getClusterName(getClusterId()),
        skipInstallForComponents, dontSkipInstallForComponents, skipFailure);
    } catch (AmbariException e) {
      LOG.error("Cannot get cluster name for clusterId = " + getClusterId(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public RequestStatusResponse startHost(String hostName, boolean skipFailure) {
    try {
      String hostGroupName = getHostGroupForHost(hostName);
      HostGroup hostGroup = this.blueprint.getHostGroup(hostGroupName);

      // get the set of components that are marked as INSTALL_ONLY
      // for this hostgroup
      Collection<String> installOnlyComponents =
        hostGroup.getComponentNames(ProvisionAction.INSTALL_ONLY);

      return ambariContext.startHost(hostName, ambariContext.getClusterName(getClusterId()), installOnlyComponents, skipFailure);
    } catch (AmbariException e) {
      LOG.error("Cannot get cluster name for clusterId = " + getClusterId(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
    return this.configRecommendationStrategy;
  }

  @Override
  public ProvisionAction getProvisionAction() {
    return provisionAction;
  }

  @Override
  public Map<String, AdvisedConfiguration> getAdvisedConfigurations() {
    return this.advisedConfigurations;
  }

  @Override
  public AmbariContext getAmbariContext() {
    return ambariContext;
  }

  @Override
  public void removeHost(String hostname) {
    for(Map.Entry<String,HostGroupInfo> entry : hostGroupInfoMap.entrySet()) {
      entry.getValue().removeHost(hostname);
    }
  }

  @Override
  public String getDefaultPassword() {
    return defaultPassword;
  }

  private void registerHostGroupInfo(Map<String, HostGroupInfo> requestedHostGroupInfoMap) throws InvalidTopologyException {
    LOG.debug("Registering requested host group information for {} hostgroups", requestedHostGroupInfoMap.size());
    checkForDuplicateHosts(requestedHostGroupInfoMap);

    for (HostGroupInfo requestedHostGroupInfo : requestedHostGroupInfoMap.values()) {
      String hostGroupName = requestedHostGroupInfo.getHostGroupName();

      //todo: doesn't support using a different blueprint for update (scaling)
      HostGroup baseHostGroup = getBlueprint().getHostGroup(hostGroupName);

      if (baseHostGroup == null) {
        throw new IllegalArgumentException("Invalid host_group specified: " + hostGroupName +
            ".  All request host groups must have a corresponding host group in the specified blueprint");
      }
      //todo: split into two methods
      HostGroupInfo currentHostGroupInfo = hostGroupInfoMap.get(hostGroupName);
      if (currentHostGroupInfo == null) {
        // blueprint host group config
        Configuration bpHostGroupConfig = baseHostGroup.getConfiguration();
        // parent config is BP host group config but with parent set to topology cluster scoped config
        Configuration parentConfiguration = new Configuration(bpHostGroupConfig.getProperties(),
            bpHostGroupConfig.getAttributes(), getConfiguration());

        requestedHostGroupInfo.getConfiguration().setParentConfiguration(parentConfiguration);
        hostGroupInfoMap.put(hostGroupName, requestedHostGroupInfo);
      } else {
        // Update.  Either add hosts or increment request count
        if (!requestedHostGroupInfo.getHostNames().isEmpty()) {
          try {
            // this validates that hosts aren't already registered with groups
            addHostsToTopology(requestedHostGroupInfo);
          } catch (NoSuchHostGroupException e) {
            //todo
            throw new InvalidTopologyException("Attempted to add hosts to unknown host group: " + hostGroupName);
          }
        } else {
          currentHostGroupInfo.setRequestedCount(
              currentHostGroupInfo.getRequestedHostCount() + requestedHostGroupInfo.getRequestedHostCount());
        }
        //todo: throw exception in case where request attempts to modify HG configuration in scaling operation
      }
    }
  }

  private void addHostsToTopology(HostGroupInfo hostGroupInfo) throws InvalidTopologyException, NoSuchHostGroupException {
    for (String host: hostGroupInfo.getHostNames()) {
      registerRackInfo(hostGroupInfo, host);
      addHostToTopology(hostGroupInfo.getHostGroupName(), host);
    }
  }

  private void registerRackInfo(HostGroupInfo hostGroupInfo, String host) {
    synchronized (hostGroupInfoMap) {
      HostGroupInfo cachedHGI = hostGroupInfoMap.get(hostGroupInfo.getHostGroupName());
      if (null != cachedHGI) {
        cachedHGI.addHostRackInfo(host, hostGroupInfo.getHostRackInfo().get(host));
      }
    }
  }


  private void checkForDuplicateHosts(Map<String, HostGroupInfo> groupInfoMap) throws InvalidTopologyException {
    Set<String> hosts = new HashSet<>();
    Set<String> duplicates = new HashSet<>();
    for (HostGroupInfo group : groupInfoMap.values()) {
      // check for duplicates within the new groups
      Collection<String> groupHosts = group.getHostNames();
      Collection<String> groupHostsCopy = new HashSet<>(group.getHostNames());
      groupHostsCopy.retainAll(hosts);
      duplicates.addAll(groupHostsCopy);
      hosts.addAll(groupHosts);

      // check against existing groups
      for (String host : groupHosts) {
        if (getHostGroupForHost(host) != null) {
          duplicates.add(host);
        }
      }
    }
    if (! duplicates.isEmpty()) {
      throw new InvalidTopologyException("The following hosts are mapped to multiple host groups: " + duplicates + "." +
        " Be aware that host names are converted to lowercase, case differences do not matter in Ambari deployments.");
    }
  }
}
