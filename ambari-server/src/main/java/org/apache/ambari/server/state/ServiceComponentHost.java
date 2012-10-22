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

import com.google.inject.persist.Transactional;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.job.Job;


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
   * Get the list of Jobs that are currently being tracked at the
   * ServiceComponentHost level
   * @return List of Jobs
   */
  public List<Job> getJobs();

  /**
   * Send a ServiceComponentHostState event to the StateMachine
   * @param event Event to handle
   * @throws InvalidStateTransitionException
   */
  public void handleEvent(ServiceComponentHostEvent event)
      throws InvalidStateTransitionException;

  public State getDesiredState();

  public void setDesiredState(State state);

  public Map<String, Config> getDesiredConfigs();

  public void updateDesiredConfigs(Map<String, Config> configs);

  public StackVersion getDesiredStackVersion();

  public void setDesiredStackVersion(StackVersion stackVersion);

  public State getState();

  public void setState(State state);

  public Map<String, Config> getConfigs();

  public void updateConfigs(Map<String, Config> configs);

  public StackVersion getStackVersion();

  public void setStackVersion(StackVersion stackVersion);

  public ServiceComponentHostResponse convertToResponse();

  boolean isPersisted();

  @Transactional
  void persist();

  void refresh();

  public void debugDump(StringBuilder sb);
}
