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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.state;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.state.configgroup.ConfigGroup;

public interface Cluster {

  /**
   * Get the cluster ID
   */
  public long getClusterId();

  /**
   * Get the Cluster Name
   */
  public String getClusterName();

  /**
   * Set the Cluster Name
   */
  public void setClusterName(String clusterName);

  /**
   * Add a service to a cluster
   * @param service
   */
  public void addService(Service service) throws AmbariException;

  /**
   * Get a service
   * @param serviceName
   * @return
   */
  public Service getService(String serviceName) throws AmbariException;

  /**
   * Get all services
   * @return
   */
  public Map<String, Service> getServices();

  /**
   * Get all ServiceComponentHosts on a given host
   * @param hostname
   * @return
   */
  public List<ServiceComponentHost> getServiceComponentHosts(String hostname);

  /**
   * Remove ServiceComponentHost from cluster
   * @param ServiceComponentHost
   */  
  public void removeServiceComponentHost(ServiceComponentHost svcCompHost) throws AmbariException;
  
  
  /**
   * Get desired stack version
   * @return
   */
  public StackId getDesiredStackVersion();

  /**
   * Set desired stack version
   * @param stackVersion
   */
  public void setDesiredStackVersion(StackId stackVersion);

  /**
   * Get current stack version
   * @return
   */
  public StackId getCurrentStackVersion();

  /**
   * Set current stack version
   * @param stackVersion
   */
  public void setCurrentStackVersion(StackId stackVersion) throws AmbariException;

  /**
   * Gets all configs that match the specified type.  Result is not the
   * DESIRED configuration for a cluster.
   * @param configType  the config type to return
   * @return  a map of configuration objects that have been set for the given type
   */
  public Map<String, Config> getConfigsByType(String configType);

  /**
   * Gets the specific config that matches the specified type and tag.  This not
   * necessarily a DESIRED configuration that applies to a cluster.
   * @param configType  the config type to find
   * @param versionTag  the config version to find
   * @return  a {@link Config} object, or <code>null</code> if the specific type
   *          and version have not been set.
   */
  public Config getConfig(String configType, String versionTag);

  /**
   * Sets a specific config.  NOTE:  This is not a DESIRED configuration that
   * applies to a cluster.
   * @param config  the config instance to add
   */
  public void addConfig(Config config);

  /**
   * Gets all configurations defined for a cluster.
   * @return  the collection of all configs that have been defined.
   */
  public Collection<Config> getAllConfigs();

  /**
   * Adds and sets a DESIRED configuration to be applied to a cluster.  There
   * can be only one selected config per type.
   * @param user the user making the change for audit purposes
   * @param config  the {@link Config} object to set as desired
   * @return <code>true</code> if the config was added, or <code>false</code>
   * if the config is already set as the current
   */
  public boolean addDesiredConfig(String user, Config config);

  /**
   * Gets the desired (and selected) config by type.
   * @param configType  the type of configuration
   * @return  the {@link Config} instance, or <code>null</code> if the type has
   * not been set.
   */
  public Config getDesiredConfigByType(String configType);

  /**
   * Gets the desired configurations for the cluster.
   * @return a map of type-to-configuration information.
   */
  public Map<String, DesiredConfig> getDesiredConfigs();


  /**
   * Creates a cluster response based on the current cluster definition
   * @return
   * @throws AmbariException
   */
  public ClusterResponse convertToResponse() throws AmbariException;

  /**
   * Refreshes the cluster details
   */
  public void refresh();

  /**
   * Creates a debug dump based on the current cluster state
   * @param sb
   */
  public void debugDump(StringBuilder sb);

  /**
   * Delete all the services associated with this cluster
   * @throws AmbariException
   */
  public void deleteAllServices() throws AmbariException;

  /**
   * Delete the named service associated with this cluster
   * @param serviceName
   * @throws AmbariException
   */
  public void deleteService(String serviceName) throws AmbariException;

  /**
   * Gets if the cluster can be deleted
   * @return
   */
  public boolean canBeRemoved();

  /**
   * Delete the cluster
   * @throws AmbariException
   */
  public void delete() throws AmbariException;

  /**
   * Add service to the cluster
   * @param serviceName
   * @return
   * @throws AmbariException
   */
  Service addService(String serviceName) throws AmbariException;

  /**
   * Get lock to control access to cluster structure
   * @return cluster-global lock
   */
  ReadWriteLock getClusterGlobalLock();

  /**
   * Fetch desired configs for list of hosts in cluster
   * @param hostnames
   * @return
   */
  Map<String, Map<String, DesiredConfig>> getHostsDesiredConfigs(Collection<String> hostnames);

  /**
   * Fetch desired configs for all hosts in cluster
   * @return
   */
  Map<String, Map<String, DesiredConfig>> getAllHostsDesiredConfigs();

  /**
   * Add a new config group to the set of Config groups associated with this
   * cluster
   * @param configGroup
   * @throws AmbariException
   */
  public void addConfigGroup(ConfigGroup configGroup) throws AmbariException;

  /**
   * Get all config groups associated with this cluster
   * @return
   * @throws AmbariException
   */
  public Map<Long, ConfigGroup> getConfigGroups() throws AmbariException;

  /**
   * Delete this config group identified by the config group id
   * @param id
   * @throws AmbariException
   */
  public void deleteConfigGroup(Long id) throws AmbariException;

  /**
   * Find all config groups associated with the give hostname
   * @param hostname
   * @return Map of config group id to config group
   */
  public Map<Long, ConfigGroup> getConfigGroupsByHostname(String hostname) throws AmbariException;
}
