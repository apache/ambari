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

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHost;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostEvent;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostState;

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
   * Add a new ServiceComponentHost to the cluster
   * @param serviceName
   * @param componentName
   * @param hostname
   * @param isClient
   */
  public void addServiceComponentHost(String serviceName, String componentName,
      String hostname, boolean isClient) throws AmbariException;

  /**
   * Get the State for a given ServiceComponentHost
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param hostname Host name
   * @return ServiceComponentHostState
   */
  public ServiceComponentHostState getServiceComponentHostState(String service,
      String serviceComponent, String hostname) throws AmbariException;

  /**
   * Set the State for a given ServiceComponentHost
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param hostname Host name
   * @param state State to set
   */
  public void setServiceComponentHostState(String service,
      String serviceComponent, String hostname,
      ServiceComponentHostState state) throws AmbariException;

  /**
   * Send an Event to a given ServiceComponentHost
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param hostname Host name
   * @param event Event to be handled
   */
  public void handleServiceComponentHostEvent(String service,
      String serviceComponent, String hostname,
      ServiceComponentHostEvent event)
          throws AmbariException, InvalidStateTransitonException;

  /**
   * Retrieve a ServiceComponentHost object
   * @param serviceName Service name
   * @param serviceComponentName ServiceComponent name
   * @param hostname Host name
   * @return
   * @throws AmbariException
   */
  public ServiceComponentHost getServiceComponentHost(String serviceName,
      String componentName, String hostname) throws AmbariException;

  public List<ServiceComponentHost> getServiceComponentHosts(String hostname);

}
