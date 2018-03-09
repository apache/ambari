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

package org.apache.ambari.server.state;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.ServiceKey;
import org.apache.ambari.server.controller.ServiceDependencyResponse;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;

public interface Service {

  Long getClusterId();

  Long getServiceGroupId();

  String getServiceGroupName();

  Long getServiceId();

  String getName();

  String getDisplayName();

  String getServiceType();

  Cluster getCluster();

  Set<ServiceDependencyResponse> getServiceDependencyResponses();

  List<ServiceKey> getServiceDependencies();

  ServiceComponent getServiceComponent(String componentName)
      throws AmbariException;

  Map<String, ServiceComponent> getServiceComponents();

  void addServiceComponents(Map<String, ServiceComponent> components)
      throws AmbariException;

  void addServiceComponent(ServiceComponent component)
      throws AmbariException;

  State getDesiredState();

  void setDesiredState(State state);

  StackId getDesiredStackId();

  ServiceResponse convertToResponse();

  void debugDump(StringBuilder sb);

  ServiceComponent addServiceComponent(String serviceComponentName, String serviceComponentType)
      throws AmbariException;

  /**
   * Find out whether the service and its components
   * are in a state that it can be removed from a cluster
   * @return
   */
  boolean canBeRemoved();

  void deleteAllComponents() throws AmbariException;

  void deleteServiceComponent(String componentName)
      throws AmbariException;

  boolean isClientOnlyService();

  ClusterServiceEntity removeDependencyService(Long dependencyServiceId);

  ClusterServiceEntity addDependencyService(Long dependencyServiceId) throws AmbariException;

  void delete() throws AmbariException;

  /**
   * Sets the maintenance state for the service
   * @param state the state
   */
  void setMaintenanceState(MaintenanceState state);

  /**
   * @return the maintenance state
   */
  MaintenanceState getMaintenanceState();

  /**
   * Refresh Service info due to current stack
   * @throws AmbariException
   */
  void updateServiceInfo() throws AmbariException;


  /**
   * Get a true or false value specifying
   * whether credential store is supported by this service.
   * @return true or false
   */
  boolean isCredentialStoreSupported();

  /**
   * Get a true or false value specifying
   * whether credential store is required by this service.
   * @return true or false
   */
  boolean isCredentialStoreRequired();

  /**
   * Get a true or false value specifying whether
   * credential store use is enabled for this service.
   *
   * @return true or false
   */
  boolean isCredentialStoreEnabled();

  /**
   * Set a true or false value specifying whether this
   * service is to be enabled for credential store use.
   *
   * @param credentialStoreEnabled - true or false
   */
  void setCredentialStoreEnabled(boolean credentialStoreEnabled);

  /**
   * @return
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  RepositoryVersionEntity getDesiredRepositoryVersion();

  /**
   * @param desiredRepositoryVersion
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  void setDesiredRepositoryVersion(RepositoryVersionEntity desiredRepositoryVersion);

  /**
   * Gets the repository for the desired version of this service by consulting
   * the repository states of all known components.
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  RepositoryVersionState getRepositoryState();

  enum Type {
    HDFS,
    GLUSTERFS,
    MAPREDUCE,
    HBASE,
    HIVE,
    OOZIE,
    WEBHCAT,
    SQOOP,
    GANGLIA,
    ZOOKEEPER,
    PIG,
    FLUME,
    YARN,
    MAPREDUCE2,
    AMBARI_METRICS,
    KERBEROS
  }
}
