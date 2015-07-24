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

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceResponse;

public interface Service {

  String getName();

  long getClusterId();

  Cluster getCluster();

  ServiceComponent getServiceComponent(String componentName)
      throws AmbariException;

  Map<String, ServiceComponent> getServiceComponents();

  void addServiceComponents(Map<String, ServiceComponent> components)
      throws AmbariException;

  void addServiceComponent(ServiceComponent component)
      throws AmbariException;

  State getDesiredState();

  void setDesiredState(State state);

  /**
   * Gets this Service's security state.
   *
   * @return this services desired SecurityState
   */
  SecurityState getSecurityState();

  /**
   * Sets this Service's desired security state
   * <p/>
   * It is expected that the new SecurityState is a valid endpoint state such that
   * SecurityState.isEndpoint() == true.
   *
   * @param securityState the desired SecurityState for this Service
   * @throws AmbariException if the new state is not an endpoint state
   */
  void setSecurityState(SecurityState securityState) throws AmbariException;

  StackId getDesiredStackVersion();

  void setDesiredStackVersion(StackId stackVersion);

  ServiceResponse convertToResponse();

  void debugDump(StringBuilder sb);

  boolean isPersisted();

  void persist();

  void refresh();

  ServiceComponent addServiceComponent(String serviceComponentName)
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

  void delete() throws AmbariException;

  /**
   * Get lock to control access to cluster structure
   * @return cluster-global lock
   */
  ReadWriteLock getClusterGlobalLock();

  /**
   * Sets the maintenance state for the service
   * @param state the state
   */
  void setMaintenanceState(MaintenanceState state);

  /**
   * @return the maintenance state
   */
  MaintenanceState getMaintenanceState();

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
