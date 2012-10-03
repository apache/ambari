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

package org.apache.ambari.server.controller;

import java.util.Set;

import org.apache.ambari.api.controller.spi.Predicate;
import org.apache.ambari.server.AmbariException;

/**
 * Management controller interface.
 */
public interface AmbariManagementController {


  // ----- Create -----------------------------------------------------------

  public void createCluster(ClusterRequest request) throws AmbariException;

  public void createServices(ServiceRequest request) throws AmbariException;

  public void createComponents(ServiceComponentRequest request)
      throws AmbariException;

  public void createHosts(HostRequest request) throws AmbariException;

  public void createHostComponents(ServiceComponentHostRequest request)
      throws AmbariException;


  // ----- Read -------------------------------------------------------------

  public Set<ClusterResponse> getCluster(ClusterRequest request, Predicate predicate);

  public ServiceResponse getServices(ServiceRequest request, Predicate predicate);

  public ServiceComponentResponse getComponents(ServiceComponentRequest request, Predicate predicate);

  public HostResponse getHosts(HostRequest request, Predicate predicate);

  public ServiceComponentHostResponse getHostComponents(ServiceComponentHostRequest request, Predicate predicate);


  // ----- Update -----------------------------------------------------------

  public void updateCluster(ClusterRequest request);

  public void updateServices(ServiceRequest request, Predicate predicate);

  public void updateComponents(ServiceComponentRequest request, Predicate predicate);

  public void updateHosts(HostRequest request, Predicate predicate);

  public void updateHostComponents(ServiceComponentHostRequest request, Predicate predicate);


  // ----- Delete -----------------------------------------------------------

  public void deleteCluster(ClusterRequest request);

  public void deleteServices(ServiceRequest request);

  public void deleteComponents(ServiceComponentRequest request);

  public void deleteHosts(HostRequest request);


  public void deleteHostComponents(ServiceComponentHostRequest request);
}
