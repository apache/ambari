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

package org.apache.ambari.server.state.live;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.host.HostEvent;
import org.apache.ambari.server.state.live.host.HostState;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostEvent;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostState;

public interface Cluster {

  /**
   * Get the Cluster Name
   */
  public String getClusterName();

  /**
   * Add a new host to the cluster
   * @throws AmbariException
   */
  public void addHost(String host) throws AmbariException;

  /**
   * Add a new ServiceComponentHost to the cluster
   * @param serviceName
   * @param componentName
   * @param hostName
   * @param isClient
   */
  public void addServiceComponentHost(String serviceName, String componentName,
      String hostName, boolean isClient) throws AmbariException;

  /**
   * Get the State for a given Host
   * @param hostName Host hostname for which to retrieve state
   * @return
   * @throws AmbariException
   */
  public HostState getHostState(String hostName) throws AmbariException;

  /**
   * Set the State for a given Host
   * @param hostName Host's hostname for which state is to be set
   * @param state HostState to set
   */
  public void setHostState(String hostName, HostState state)
      throws AmbariException;

  /**
   * Send event to the given Host
   * @param hostName Host's hostname
   * @param event Event to be handled
   */
  public void handleHostEvent(String hostName, HostEvent event)
      throws AmbariException, InvalidStateTransitonException;

  /**
   * Get the State for a given ServiceComponentHost
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param hostName Host name
   * @return ServiceComponentHostState
   */
  public ServiceComponentHostState getServiceComponentHostState(String service,
      String serviceComponent, String hostName) throws AmbariException;

  /**
   * Set the State for a given ServiceComponentHost
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param hostName Host name
   * @param state State to set
   */
  public void setServiceComponentHostState(String service,
      String serviceComponent, String hostName,
      ServiceComponentHostState state) throws AmbariException;

  /**
   * Send an Event to a given ServiceComponentHost
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param hostName Host name
   * @param event Event to be handled
   */
  public void handleServiceComponentHostEvent(String service,
      String serviceComponent, String hostName,
      ServiceComponentHostEvent event)
          throws AmbariException, InvalidStateTransitonException;

}
