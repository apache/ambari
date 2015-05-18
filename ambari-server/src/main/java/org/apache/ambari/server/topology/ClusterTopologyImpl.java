/**
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

import org.apache.ambari.server.controller.RequestStatusResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a cluster topology.
 * Topology includes the the associated blueprint, cluster configuration and hostgroup -> host mapping.
 */
public class ClusterTopologyImpl implements ClusterTopology {

  private String clusterName;
  //todo: currently topology is only associated with a single bp
  //todo: this will need to change to allow usage of multiple bp's for the same cluster
  //todo: for example: provision using bp1 and scale using bp2
  private Blueprint blueprint;
  private Configuration configuration;
  private final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<String, HostGroupInfo>();
  private final AmbariContext ambariContext;


  //todo: will need to convert all usages of hostgroup name to use fully qualified name (BP/HG)
  //todo: for now, restrict scaling to the same BP
  public ClusterTopologyImpl(AmbariContext ambariContext, TopologyRequest topologyRequest) throws InvalidTopologyException {
    this.clusterName = topologyRequest.getClusterName();
    // provision cluster currently requires that all hostgroups have same BP so it is ok to use root level BP here
    this.blueprint = topologyRequest.getBlueprint();
    this.configuration = topologyRequest.getConfiguration();

    registerHostGroupInfo(topologyRequest.getHostGroupInfo());

    validateTopology(topologyRequest.getTopologyValidators());
    this.ambariContext = ambariContext;
  }

  //todo: only used in tests, remove.  Validators not invoked when this constructor is used.
  public ClusterTopologyImpl(AmbariContext ambariContext,
                             String clusterName,
                             Blueprint blueprint,
                             Configuration configuration,
                             Map<String, HostGroupInfo> hostGroupInfo)
                                throws InvalidTopologyException {

    this.clusterName = clusterName;
    this.blueprint = blueprint;
    this.configuration = configuration;

    registerHostGroupInfo(hostGroupInfo);
    this.ambariContext = ambariContext;
  }

  @Override
  public void update(TopologyRequest topologyRequest) throws InvalidTopologyException {
    registerHostGroupInfo(topologyRequest.getHostGroupInfo());
  }

  @Override
  public String getClusterName() {
    return clusterName;
  }

  @Override
  public Blueprint getBlueprint() {
    return blueprint;
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public Map<String, HostGroupInfo> getHostGroupInfo() {
    return hostGroupInfoMap;
  }

  //todo: do we want to return groups with no requested hosts?
  @Override
  public Collection<String> getHostGroupsForComponent(String component) {
    Collection<String> resultGroups = new ArrayList<String>();
    for (HostGroup group : getBlueprint().getHostGroups().values() ) {
      if (group.getComponents().contains(component)) {
        resultGroups.add(group.getName());
      }
    }
    return resultGroups;
  }

  @Override
  public String getHostGroupForHost(String hostname) {
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
    }
  }

  @Override
  public Collection<String> getHostAssignmentsForComponent(String component) {
    //todo: ordering requirements?
    Collection<String> hosts = new ArrayList<String>();
    Collection<String> hostGroups = getHostGroupsForComponent(component);
    for (String group : hostGroups) {
      hosts.addAll(getHostGroupInfo().get(group).getHostNames());
    }
    return hosts;
  }

  @Override
  public boolean isNameNodeHAEnabled() {
    return isNameNodeHAEnabled(configuration.getFullProperties());
  }

  public static boolean isNameNodeHAEnabled(Map<String, Map<String, String>> configurationProperties) {
    return configurationProperties.containsKey("hdfs-site") &&
        configurationProperties.get("hdfs-site").containsKey("dfs.nameservices");
  }

  @Override
  public boolean isYarnResourceManagerHAEnabled() {
    return isYarnResourceManagerHAEnabled(configuration.getFullProperties());
  }

  /**
   * Static convenience function to determine if Yarn ResourceManager HA is enabled
   * @param configProperties configuration properties for this cluster
   * @return true if Yarn ResourceManager HA is enabled
   *         false if Yarn ResourceManager HA is not enabled
   */
  static boolean isYarnResourceManagerHAEnabled(Map<String, Map<String, String>> configProperties) {
    return configProperties.containsKey("yarn-site") && configProperties.get("yarn-site").containsKey("yarn.resourcemanager.ha.enabled")
      && configProperties.get("yarn-site").get("yarn.resourcemanager.ha.enabled").equals("true");
  }

  private void validateTopology(List<TopologyValidator> validators)
      throws InvalidTopologyException {

    for (TopologyValidator validator : validators) {
      validator.validate(this);
    }
  }

  @Override
  public boolean isClusterKerberosEnabled() {
    return ambariContext.isClusterKerberosEnabled(getClusterName());
  }

  @Override
  public RequestStatusResponse installHost(String hostName) {
    return ambariContext.installHost(hostName, getClusterName());
  }

  @Override
  public RequestStatusResponse startHost(String hostName) {
    return ambariContext.startHost(hostName, getClusterName());
  }

  @Override
  public AmbariContext getAmbariContext() {
    return ambariContext;
  }

  private void registerHostGroupInfo(Map<String, HostGroupInfo> groupInfoMap) throws InvalidTopologyException {
    checkForDuplicateHosts(groupInfoMap);
    for (HostGroupInfo hostGroupInfo : groupInfoMap.values() ) {
      String hostGroupName = hostGroupInfo.getHostGroupName();
      //todo: doesn't support using a different blueprint for update (scaling)
      HostGroup baseHostGroup = getBlueprint().getHostGroup(hostGroupName);
      if (baseHostGroup == null) {
        throw new IllegalArgumentException("Invalid host_group specified: " + hostGroupName +
            ".  All request host groups must have a corresponding host group in the specified blueprint");
      }
      //todo: split into two methods
      HostGroupInfo existingHostGroupInfo = hostGroupInfoMap.get(hostGroupName);
      if (existingHostGroupInfo == null) {
        // blueprint host group config
        Configuration bpHostGroupConfig = baseHostGroup.getConfiguration();
        // parent config is BP host group config but with parent set to topology cluster scoped config
        Configuration parentConfiguration = new Configuration(bpHostGroupConfig.getProperties(),
            bpHostGroupConfig.getAttributes(), getConfiguration());

        hostGroupInfo.getConfiguration().setParentConfiguration(parentConfiguration);
        hostGroupInfoMap.put(hostGroupName, hostGroupInfo);
      } else {
        // Update.  Either add hosts or increment request count
        if (! hostGroupInfo.getHostNames().isEmpty()) {
          try {
            // this validates that hosts aren't already registered with groups
            addHostsToTopology(hostGroupInfo);
          } catch (NoSuchHostGroupException e) {
            //todo
            throw new InvalidTopologyException("Attempted to add hosts to unknown host group: " + hostGroupName);
          }
        } else {
          existingHostGroupInfo.setRequestedCount(
              existingHostGroupInfo.getRequestedHostCount() + hostGroupInfo.getRequestedHostCount());
        }
        //todo: throw exception in case where request attempts to modify HG configuration in scaling operation
      }
    }
  }

  private void addHostsToTopology(HostGroupInfo hostGroupInfo) throws InvalidTopologyException, NoSuchHostGroupException {
    for (String host: hostGroupInfo.getHostNames()) {
      addHostToTopology(hostGroupInfo.getHostGroupName(), host);
    }
  }

  private void checkForDuplicateHosts(Map<String, HostGroupInfo> groupInfoMap) throws InvalidTopologyException {
    Set<String> hosts = new HashSet<String>();
    Set<String> duplicates = new HashSet<String>();
    for (HostGroupInfo group : groupInfoMap.values()) {
      // check for duplicates within the new groups
      Collection<String> groupHosts = group.getHostNames();
      Collection<String> groupHostsCopy = new HashSet<String>(group.getHostNames());
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
      throw new InvalidTopologyException("The following hosts are mapped to multiple host groups: " + duplicates);
    }
  }
}
