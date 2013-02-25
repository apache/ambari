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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ClusterResponse;

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
   * Get desired config based on the given config type name
   * @param configType
   * @return
   */
  public Map<String, Config> getDesiredConfigsByType(String configType);

  /**
   * Get the desired config of given type and version
   * @param configType
   * @param versionTag
   * @return
   */
  public Config getDesiredConfig(String configType, String versionTag);

  /**
   * Add the desired config for the cluster
   * @param config
   */
  public void addDesiredConfig(Config config);

  /**
   * Get all configs associated with the cluster
   * @return
   */
  public Collection<Config> getAllConfigs();

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
}
