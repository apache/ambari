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

package org.apache.ambari.server.state.live.svccomphost;

import java.util.List;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.job.Job;


public interface ServiceComponentHost {

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
   * Get ServiceComponent-Host State
   * @return ServiceComponentHostState
   */
  public ServiceComponentHostState getState();

  /**
   * Set the State for this ServiceComponent-Host
   * @param state ServiceComponentHostState to set to
   */
  public void setState(ServiceComponentHostState state);

  /**
   * Send a ServiceComponentHostState event to the StateMachine
   * @param event Event to handle
   * @throws InvalidStateTransitonException
   */
  public void handleEvent(ServiceComponentHostEvent event)
      throws InvalidStateTransitonException;

}
