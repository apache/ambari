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

import static java.util.stream.Collectors.toMap;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_AND_START;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_ONLY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.ConfigurationContext;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.state.PropertyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a cluster topology.
 * Topology includes the the associated blueprint, cluster configuration and hostgroup -> host mapping.
 */
public class ClusterTopologyImpl implements ClusterTopology {

  private final static Logger LOG = LoggerFactory.getLogger(ClusterTopologyImpl.class);
  private final Configuration configuration;

  private Long clusterId;

  //todo: currently topology is only associated with a single bp
  //todo: this will need to change to allow usage of multiple bp's for the same cluster
  //todo: for example: provision using bp1 and scale using bp2
  private final BlueprintV2 blueprint;
  private final Collection<Service> serviceConfigs;
  private ConfigRecommendationStrategy configRecommendationStrategy;
  private ProvisionAction provisionAction = ProvisionAction.INSTALL_AND_START;
  private final Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
  private final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();
  private final AmbariContext ambariContext;
  private final String defaultPassword;

  //todo: will need to convert all usages of hostgroup name to use fully qualified name (BP/HG)
  //todo: for now, restrict scaling to the same BP
  public ClusterTopologyImpl(AmbariContext ambariContext, TopologyRequest topologyRequest) throws InvalidTopologyException {
    this.clusterId = topologyRequest.getClusterId();
    // provision cluster currently requires that all hostgroups have same BP so it is ok to use root level BP here
    this.blueprint = topologyRequest.getBlueprint();
    this.configuration = blueprint.getConfiguration();
    this.configuration.setParentConfiguration(new Configuration(Collections.singletonMap("cluster-env", getDefaultClusterSettings()), new HashMap<>()));
    this.serviceConfigs = topologyRequest.getServiceConfigs();
    if (topologyRequest instanceof ProvisionClusterRequest) {
      this.defaultPassword = ((ProvisionClusterRequest) topologyRequest).getDefaultPassword();
    } else {
      this.defaultPassword = null;
    }

    registerHostGroupInfo(topologyRequest.getHostGroupInfo());

    // todo extract validation to specialized service
    validateTopology();
    this.ambariContext = ambariContext;
  }

  @Override
  public void update(TopologyRequest topologyRequest) throws InvalidTopologyException {
    registerHostGroupInfo(topologyRequest.getHostGroupInfo());
  }

  @Override
  public Long getClusterId() {
    return clusterId;
  }

  @Override
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public BlueprintV2 getBlueprint() {
    return blueprint;
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  public Collection<Service> getServiceConfigs() {
    return serviceConfigs;
  }

  @Override
  public Map<String, HostGroupInfo> getHostGroupInfo() {
    return hostGroupInfoMap;
  }

  //todo: do we want to return groups with no requested hosts?
  @Override
  public Collection<String> getHostGroupsForComponent(String component) {
    Collection<String> resultGroups = new ArrayList<>();
    for (HostGroupV2 group : getBlueprint().getHostGroups().values() ) {
      if (group.getComponentNames().contains(component)) {
        resultGroups.add(group.getName());
      }
    }
    return resultGroups;
  }

  @Override
  public String getHostGroupForHost(String hostname) {
    return hostGroupInfoMap.values().stream()
      .filter(g -> g.getHostNames().contains(hostname))
      .findAny()
      .map(HostGroupInfo::getHostGroupName)
      .orElse(null);
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
  public boolean isNameNodeHAEnabled(ConfigurationContext configurationContext) {
    return configurationContext.isNameNodeHAEnabled();
  }

  /**
   * Static convenience function to determine if Yarn ResourceManager HA is enabled
   * @param configurationContext configuration context
   * @return true if Yarn ResourceManager HA is enabled
   *         false if Yarn ResourceManager HA is not enabled
   */
  @Override
  public boolean isYarnResourceManagerHAEnabled(ConfigurationContext configurationContext) {
    return configurationContext.isYarnResourceManagerHAEnabled();
  }

  private void validateTopology()
      throws InvalidTopologyException {

    Collection<Service> hdfsServices = getBlueprint().getServicesByType("HDFS");
    for (Service hdfsService : hdfsServices) {
      ConfigurationContext configContext = new ConfigurationContext(hdfsService.getStack(), hdfsService.getConfiguration());
      if(isNameNodeHAEnabled(configContext)) {

        Collection<String> nnHosts = getHostAssignmentsForComponent("NAMENODE");
        if (nnHosts.size() != 2) {
          throw new InvalidTopologyException("NAMENODE HA requires exactly 2 hosts running NAMENODE but there are: " +
            nnHosts.size() + " Hosts: " + nnHosts);
        }

        Map<String, String> hadoopEnvConfig = hdfsService.getConfiguration().getProperties().get("hadoop-env");
        if(hadoopEnvConfig != null && !hadoopEnvConfig.isEmpty() && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_active") && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_standby")) {
          if((!HostGroup.HOSTGROUP_REGEX.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_active")).matches() && !nnHosts.contains(hadoopEnvConfig.get("dfs_ha_initial_namenode_active")))
            || (!HostGroup.HOSTGROUP_REGEX.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby")).matches() && !nnHosts.contains(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby")))){
            throw new IllegalArgumentException("NAMENODE HA hosts mapped incorrectly for properties 'dfs_ha_initial_namenode_active' and 'dfs_ha_initial_namenode_standby'. Expected hosts are: " + nnHosts);
          }
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
      HostGroupV2 hostGroup = this.blueprint.getHostGroup(hostGroupName);

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
      HostGroupV2 hostGroup = this.blueprint.getHostGroup(hostGroupName);

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
  public void setConfigRecommendationStrategy(ConfigRecommendationStrategy strategy) {
    this.configRecommendationStrategy = strategy;
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
  public void setProvisionAction(ProvisionAction provisionAction) {
    this.provisionAction = provisionAction;
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

  private void registerHostGroupInfo(Map<String, HostGroupInfo> requestHostGroups) throws InvalidTopologyException {
    LOG.debug("Registering requested host group information for {} host groups", requestHostGroups.size());
    checkForDuplicateHosts(requestHostGroups);

    for (HostGroupInfo requestHostGroup : requestHostGroups.values()) {
      String hostGroupName = requestHostGroup.getHostGroupName();

      //todo: doesn't support using a different blueprint for update (scaling)
      HostGroupV2 bpHostGroup = getBlueprint().getHostGroup(hostGroupName);
      if (bpHostGroup == null) {
        String msg = String.format("The host group '%s' is not present in the blueprint '%s'", hostGroupName, blueprint.getName());
        LOG.error(msg);
        throw new InvalidTopologyException(msg);
      }

      //todo: split into two methods
      HostGroupInfo currentHostGroupInfo = hostGroupInfoMap.get(hostGroupName);
      if (currentHostGroupInfo == null) {
        // blueprint host group config
        Configuration bpHostGroupConfig = bpHostGroup.getConfiguration();
        // parent config is BP host group config but with parent set to topology cluster scoped config
        Configuration parentConfiguration = new Configuration(bpHostGroupConfig, getConfiguration());

        requestHostGroup.getConfiguration().setParentConfiguration(parentConfiguration);
        requestHostGroup.setServiceConfigs(bpHostGroup.getServices());

        hostGroupInfoMap.put(hostGroupName, requestHostGroup);
      } else {
        // Update.  Either add hosts or increment request count
        if (!requestHostGroup.getHostNames().isEmpty()) {
          try {
            // this validates that hosts aren't already registered with groups
            addHostsToTopology(requestHostGroup);
          } catch (NoSuchHostGroupException e) {
            //todo
            throw new InvalidTopologyException("Attempted to add hosts to unknown host group: " + hostGroupName);
          }
        } else {
          currentHostGroupInfo.setRequestedCount(
              currentHostGroupInfo.getRequestedHostCount() + requestHostGroup.getRequestedHostCount());
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

  private static Map<String, String> getDefaultClusterSettings() { // TODO temporary
    return AmbariContext.getController().getAmbariMetaInfo().getClusterProperties().stream()
      .collect(toMap(PropertyInfo::getName, PropertyInfo::getValue));
  }

}
