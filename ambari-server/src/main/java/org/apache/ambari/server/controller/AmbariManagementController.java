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

  public TrackActionResponse createCluster(ClusterRequest request) throws AmbariException;

  public TrackActionResponse createServices(ServiceRequest request) throws AmbariException;

  public TrackActionResponse createComponents(ServiceComponentRequest request)
      throws AmbariException;

  public TrackActionResponse createHosts(HostRequest request) throws AmbariException;

  public TrackActionResponse createHostComponents(ServiceComponentHostRequest request)
      throws AmbariException;


  // ----- Read -------------------------------------------------------------

  public Set<ClusterResponse> getCluster(ClusterRequest request, Predicate predicate);

  public ServiceResponse getServices(ServiceRequest request, Predicate predicate);

  public ServiceComponentResponse getComponents(ServiceComponentRequest request, Predicate predicate);

  public HostResponse getHosts(HostRequest request, Predicate predicate);

  public ServiceComponentHostResponse getHostComponents(ServiceComponentHostRequest request, Predicate predicate);


  // ----- Update -----------------------------------------------------------

  public TrackActionResponse updateCluster(ClusterRequest request);

  public TrackActionResponse updateServices(ServiceRequest request, Predicate predicate);

  public TrackActionResponse updateComponents(ServiceComponentRequest request, Predicate predicate);

  public TrackActionResponse updateHosts(HostRequest request, Predicate predicate);

  public TrackActionResponse updateHostComponents(ServiceComponentHostRequest request, Predicate predicate);


  // ----- Delete -----------------------------------------------------------

  public TrackActionResponse deleteCluster(ClusterRequest request);

  public TrackActionResponse deleteServices(ServiceRequest request);

  public TrackActionResponse deleteComponents(ServiceComponentRequest request);

  public TrackActionResponse deleteHosts(HostRequest request);


  public TrackActionResponse deleteHostComponents(ServiceComponentHostRequest request);
}
