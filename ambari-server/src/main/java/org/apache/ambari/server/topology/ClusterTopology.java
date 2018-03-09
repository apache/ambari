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

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.StackId;

/**
 * Represents a full cluster topology including all instance information as well as the associated
 * blueprint which provides all abstract topology information.
 */
public interface ClusterTopology {

  /**
   * Get the id of the cluster.
   *
   * @return cluster id
   */
  Long getClusterId();

  /**
   * Get the blueprint associated with the cluster.
   *
   * @return associated blueprint
   */
  Blueprint getBlueprint();

  /**
   * Get the name of the blueprint associated with the cluster.
   *
   * @return associated blueprint's name
   */
  String getBlueprintName();

  /**
   * Get the stack associated with the blueprint.
   * For mpack-based installation this is a composite stack
   * that provides a single unified view of all underlying mpacks,
   * but does not have any identifier.
   *
   * @return associated stack
   */
  StackDefinition getStack();

  /**
   * @return the set of stack (mpack) IDs associated with the cluster
   */
  Set<StackId> getStackIds();

  /**
   * Get the cluster scoped configuration for the cluster.
   * This configuration has the blueprint cluster scoped
   * configuration set as it's parent.
   *
   * @return cluster scoped configuration
   */
  Configuration getConfiguration();

  /**
   * Get the Blueprint cluster scoped setting.
   * The blueprint cluster scoped setting has the setting properties
   * with the setting names associated with the blueprint.
   *
   * @return blueprint cluster scoped setting
   */
  Setting getSetting();

  /**
   * Get host group information.
   *
   * @return map of host group name to host group information
   */
  Map<String, HostGroupInfo> getHostGroupInfo();

  /**
   * Get the names of  all of host groups which contain the specified component.
   *
   * @param component  component name
   *
   * @return collection of host group names which contain the specified component
   */
  @Deprecated // 1. component name is not enough, 2. only used for stack-specific checks/updates
  Collection<String> getHostGroupsForComponent(String component);

  /**
   * Get the name of the host group which is mapped to the specified host.
   *
   * @param hostname  host name
   *
   * @return name of the host group which is mapped to the specified host or null if
   *         no group is mapped to the host
   */
  String getHostGroupForHost(String hostname);

  /**
   * Get all hosts which are mapped to a host group which contains the specified component.
   * The host need only to be mapped to the hostgroup, not actually provisioned.
   *
   * @param component  component name
   *
   * @return collection of hosts for the specified component; will not return null
   */
  @Deprecated
  Collection<String> getHostAssignmentsForComponent(String component);

  /**
   * Get all of the services represented in the blueprint.
   *
   * @return collection of all represented service names
   */
  Collection<String> getServices();

  /**
   * Get all of the components represented in the blueprint.
   *
   * @return collection of all represented components
   */
  Stream<ResolvedComponent> getComponents();

  /**
   * Get the components that are included in the specified host group.
   *
   * @param hostGroup host group name
   * @return stream of components for the service
   */
  @Nonnull
  Stream<ResolvedComponent> getComponentsInHostGroup(String hostGroup);

  /**
   * A config type is valid if there are services related to except cluster-env and global.
   */
  boolean isValidConfigType(String configType);

  /**
   * Update the existing topology based on the provided topology request.
   *
   * @param topologyRequest  request modifying the topology
   *
   * @throws InvalidTopologyException if the request specified invalid topology information or if
   *                                  making the requested changes would result in an invalid topology
   */
  void update(TopologyRequest topologyRequest) throws InvalidTopologyException;

  /**
   * Add a new host to the topology.
   *
   * @param hostGroupName  name of associated host group
   * @param host           name of host
   *
   * @throws InvalidTopologyException if the host being added is already registered to a different host group
   * @throws NoSuchHostGroupException if the specified host group is invalid
   */
  void addHostToTopology(String hostGroupName, String host) throws InvalidTopologyException, NoSuchHostGroupException;

  /**
   * Determine if the cluster is kerberos enabled.
   *
   * @return true if the cluster is kerberos enabled; false otherwise
   */
  boolean isClusterKerberosEnabled();

  /**
   * Install the specified host.
   *
   * @param hostName  host name
   * @param skipInstallTaskCreate
   * @return install response
   */
  RequestStatusResponse installHost(String hostName, boolean skipInstallTaskCreate, boolean skipFailure);

  /**
   * Start the specified host.
   *
   * @param hostName  host name
   * @return start response
   */
  RequestStatusResponse startHost(String hostName, boolean skipFailure);

  ConfigRecommendationStrategy getConfigRecommendationStrategy();

  ProvisionAction getProvisionAction();

  Map<String, AdvisedConfiguration> getAdvisedConfigurations();

  //todo: don't expose ambari context from this class
  AmbariContext getAmbariContext();

  /**
   * Removes host from stateful ClusterTopology
   * @param hostname
   */
  void removeHost(String hostname);

  String getDefaultPassword();

  /**
   * Determine if the host group contains a master component.
   *
   * @return true if the host group contains a master component; false otherwise
   */
  boolean containsMasterComponent(String hostGroup);

  Collection<HostGroup> getHostGroups();
}
