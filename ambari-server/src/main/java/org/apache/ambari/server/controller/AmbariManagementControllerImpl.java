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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceRequest.PerServiceRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.live.ClustersImpl;

public class AmbariManagementControllerImpl implements
    AmbariManagementController {

  private final Clusters clusters;
  
  public AmbariManagementControllerImpl() {
    this.clusters = new ClustersImpl();
  }
  
  @Override
  public TrackActionResponse createCluster(ClusterRequest request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // TODO throw error
    }
    clusters.addCluster(request.getClusterName());
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse createServices(ServiceRequest request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // TODO throw error
    }
    Cluster cluster = clusters.getCluster(request.getClusterName());
    for (PerServiceRequest service : request.getServices()) {
      Map<String, Config> configs = new HashMap<String, Config>();
      // TODO initialize configs based off service.configVersions
      // TODO error check if service is already added and/or enabled/deployed      
      cluster.addService(new ServiceImpl(cluster, service.getServiceName(),
          State.valueOf(service.getDesiredState()), configs));
      
      // TODO take action based on desired state
    }
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse createComponents(
      ServiceComponentRequest request) throws AmbariException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TrackActionResponse createHosts(HostRequest request) throws AmbariException {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse createHostComponents(
      ServiceComponentHostRequest request) throws AmbariException {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public Set<ClusterResponse> getCluster(ClusterRequest request,
      Predicate predicate) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ServiceResponse getServices(ServiceRequest request, Predicate predicate) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ServiceComponentResponse getComponents(
      ServiceComponentRequest request, Predicate predicate) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public HostResponse getHosts(HostRequest request, Predicate predicate) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ServiceComponentHostResponse getHostComponents(
      ServiceComponentHostRequest request, Predicate predicate) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TrackActionResponse updateCluster(ClusterRequest request) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse updateServices(ServiceRequest request,
      Predicate predicate) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse updateComponents(
      ServiceComponentRequest request, Predicate predicate) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse updateHosts(HostRequest request, Predicate predicate) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse updateHostComponents(
      ServiceComponentHostRequest request, Predicate predicate) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse deleteCluster(ClusterRequest request) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse deleteServices(ServiceRequest request) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse deleteComponents(ServiceComponentRequest request) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse deleteHosts(HostRequest request) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

  @Override
  public TrackActionResponse deleteHostComponents(ServiceComponentHostRequest request) {
    // TODO Auto-generated method stub
    // TODO
    return null;
  }

}
