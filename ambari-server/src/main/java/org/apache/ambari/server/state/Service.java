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

import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceResponse;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

public interface Service {

  public String getName();

  public long getClusterId();

  public Cluster getCluster();

  public ServiceComponent getServiceComponent(String componentName)
      throws AmbariException;

  public Map<String, ServiceComponent> getServiceComponents();

  public void addServiceComponents(Map<String, ServiceComponent> components)
      throws AmbariException;

  public void addServiceComponent(ServiceComponent component)
      throws AmbariException;

  public State getDesiredState();

  public void setDesiredState(State state);

  public StackId getDesiredStackVersion();

  public void setDesiredStackVersion(StackId stackVersion);

  public ServiceResponse convertToResponse();

  public void debugDump(StringBuilder sb);

  boolean isPersisted();

  @Transactional
  void persist();

  void refresh();

  ServiceComponent addServiceComponent(String serviceComponentName)
      throws AmbariException;

  /**
   * Find out whether the service and its components
   * are in a state that it can be removed from a cluster
   * @return
   */
  public boolean canBeRemoved();

  public void deleteAllComponents() throws AmbariException;

  public void deleteServiceComponent(String componentName)
      throws AmbariException;

  public boolean isClientOnlyService();

  public void delete() throws AmbariException;

  /**
   * Get lock to control access to cluster structure
   * @return cluster-global lock
   */
  ReadWriteLock getClusterGlobalLock();
  
  /**
   * Sets the maintenance state for the service
   * @param state the state
   */
  public void setMaintenanceState(MaintenanceState state);
  
  /**
   * @return the maintenance state
   */
  public MaintenanceState getMaintenanceState();

  public enum Type {
    HDFS,
    GLUSTERFS,
    MAPREDUCE,
    HBASE,
    HIVE,
    OOZIE,
    WEBHCAT,
    SQOOP,
    NAGIOS,
    GANGLIA,
    ZOOKEEPER,
    PIG,
    FLUME,
    YARN,
    MAPREDUCE2
  }
}
