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
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;


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
   * Get the Host this object maps to
   * @return Host Object
   */
  Host getHost();

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

  /**
   * Gets the current security state for this ServiceComponent
   * <p/>
   * The returned SecurityState may be any endpoint or transitional state.
   *
   * @return the current SecurityState for this ServiceComponent
   */
  public SecurityState getSecurityState();

  /**
   * Sets the current security state for this ServiceComponent
   * <p/>
   * The new SecurityState may be any endpoint or transitional state.
   *
   * @param state the current SecurityState for this ServiceComponent
   */
  public void setSecurityState(SecurityState state);

  /**
   * Gets the version of the component.
   *
   * @return component version
   */
  public String getVersion();

  /**
   * Sets the version of the component from the stack.
   *
   * @param version component version (e.g. 2.2.0.0-2041)
   */
  public void setVersion(String version);

  /**
   * Gets the desired security state for this ServiceComponent
   * <p/>
   * The returned SecurityState is a valid endpoint state where
   * SecurityState.isEndpoint() == true.
   *
   * @return the desired SecurityState for this ServiceComponent
   */
  public SecurityState getDesiredSecurityState();

  /**
   * Sets the desired security state for this ServiceComponent
   * <p/>
   * It is expected that the new SecurityState is a valid endpoint state such that
   * SecurityState.isEndpoint() == true.
   *
   * @param securityState the desired SecurityState for this ServiceComponent
   * @throws AmbariException if the new state is not an endpoint state
   */
  public void setDesiredSecurityState(SecurityState securityState) throws AmbariException;

  /**
   * @param upgradeState the upgrade state
   */
  public void setUpgradeState(UpgradeState upgradeState);

  /**
   * @return the upgrade state
   */
  public UpgradeState getUpgradeState();

  public StackId getStackVersion();

  public void setStackVersion(StackId stackVersion);

  public HostComponentAdminState getComponentAdminState();

  public void setComponentAdminState(HostComponentAdminState attribute);

  public ServiceComponentHostResponse convertToResponse();

  boolean isPersisted();

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

  /**
   * Changes host version state according to state of the components installed on the host.
   * @return The Repository Version Entity with that component in the host
   * @throws AmbariException if host is detached from the cluster
   */
  public RepositoryVersionEntity recalculateHostVersionState() throws AmbariException;

}
