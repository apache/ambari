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

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;

import com.google.inject.persist.Transactional;


public interface ServiceComponentHost {

  /**
   * Get the Cluster that this object maps to
   */
  public long getClusterId();

  /**
   * Get the Cluster that this object maps to
   */
  public String getClusterName();

  /**
   * Get the Service this object maps to
   * @return Name of the Service
   */
  public String getServiceName();

  /**
   * Get the ServiceComponent this object maps to
   * @return Name of the ServiceComponent
   */
  public String getServiceComponentName();

  /**
   * Get the Host this object maps to
   * @return Host's hostname
   */
  public String getHostName();

  /**
   * Send a ServiceComponentHostState event to the StateMachine
   * @param event Event to handle
   * @throws InvalidStateTransitionException
   */
  public void handleEvent(ServiceComponentHostEvent event)
      throws InvalidStateTransitionException;

  public State getDesiredState();

  public void setDesiredState(State state);

  public StackId getDesiredStackVersion();

  public void setDesiredStackVersion(StackId stackVersion);

  public State getState();

  public void setState(State state);
  
  public StackId getStackVersion();

  public void setStackVersion(StackId stackVersion);

  public HostComponentAdminState getComponentAdminState();

  public void setComponentAdminState(HostComponentAdminState attribute);

  public ServiceComponentHostResponse convertToResponse();

  boolean isPersisted();

  @Transactional
  void persist();

  void refresh();

  public void debugDump(StringBuilder sb);

  public boolean canBeRemoved();

  public void delete() throws AmbariException;

  /**
   * Updates the tags that have been recognized by a START action.
   * @param configTags
   */
  public void updateActualConfigs(Map<String, Map<String, String>> configTags);
  
  /**
   * Gets the actual config tags, if known.
   * @return the actual config map
   */
  public Map<String, HostConfig> getActualConfigs();

  public HostState getHostState();

  /**
   * @param state the maintenance state
   */
  public void setMaintenanceState(MaintenanceState state);
  
  /**
   * @return the maintenance state
   */
  public MaintenanceState getMaintenanceState();

  /**
   * @param procs a list containing a map describing each process
   */
  public void setProcesses(List<Map<String, String>> procs);
  
  
  /**  
   * @return the list of maps describing each process
   */
  public List<Map<String, String>> getProcesses();

  /**
   * @return whether restart required
   */
  public boolean isRestartRequired();

  /**
   * @param restartRequired the restartRequired flag
   */
  public void setRestartRequired(boolean restartRequired);

}
