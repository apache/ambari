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

import org.apache.ambari.server.AmbariException;

import java.util.Set;

/**
 * Management controller interface.
 */
public interface AmbariManagementController {

  // ----- Create -----------------------------------------------------------

  /**
   * Create the cluster defined by the attributes in the given request object.
   *
   * @param request  the request object which defines the cluster to be created
   *
   * @throws AmbariException thrown if the cluster cannot be created
   */
  public void createCluster(ClusterRequest request) throws AmbariException;

  /**
   * Create the service defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the service to be created
   *
   * @throws AmbariException thrown if the service cannot be created
   */
  public void createServices(Set<ServiceRequest> request)
      throws AmbariException;

  /**
   * Create the component defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the component to be created
   *
   * @throws AmbariException thrown if the component cannot be created
   */
  public void createComponents(Set<ServiceComponentRequest> request)
      throws AmbariException;

  /**
   * Create the host defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the host to be created
   *
   * @throws AmbariException thrown if the host cannot be created
   */
  public void createHosts(Set<HostRequest> request)
      throws AmbariException;

  /**
   * Create the host component defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the host component to be created
   *
   * @throws AmbariException thrown if the host component cannot be created
   */
  public void createHostComponents(
      Set<ServiceComponentHostRequest> request) throws AmbariException;

  /**
   * Creates a configuration.
   *
   * @param request the request object which defines the configuration.
   *
   * @return a track action response
   *
   * @throws AmbariException when the configuration cannot be created.
   */
  public TrackActionResponse createConfiguration(ConfigurationRequest request) throws AmbariException;


  // ----- Read -------------------------------------------------------------

  /**
   * Get the clusters identified by the given request object.
   *
   * @param request  the request object which identifies the clusters to be returned
   *
   * @return a set of cluster responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ClusterResponse> getClusters(ClusterRequest request)
      throws AmbariException;

  /**
   * Get the services identified by the given request object.
   *
   * @param request  the request object which identifies the services
   * to be returned
   *
   * @return a set of service responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ServiceResponse> getServices(ServiceRequest request)
      throws AmbariException;

  /**
   * Get the components identified by the given request object.
   *
   * @param request  the request object which identifies the components to be returned
   *
   * @return a set of component responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ServiceComponentResponse> getComponents(
      ServiceComponentRequest request) throws AmbariException;

  /**
   * Get the hosts identified by the given request object.
   *
   * @param request  the request object which identifies the hosts to be returned
   *
   * @return a set of host responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<HostResponse> getHosts(HostRequest request) throws AmbariException;

  /**
   * Get the host components identified by the given request object.
   *
   * @param request  the request object which identifies the host components
   * to be returned
   *
   * @return a set of host component responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ServiceComponentHostResponse> getHostComponents(
      ServiceComponentHostRequest request) throws AmbariException;

  /**
   * Gets the configurations identified by the given request object.
   *
   * @param request   the request object
   *
   * @return  a set of configuration responses
   *
   * @throws AmbariException if the configurations could not be read
   */
  public Set<ConfigurationResponse> getConfigurations(ConfigurationRequest request) throws AmbariException;


  // ----- Update -----------------------------------------------------------

  /**
   * Update the cluster identified by the given request object with the
   * values carried by the given request object.
   *
   * @param request    the request object which defines which cluster to
   *                   update and the values to set
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public TrackActionResponse updateCluster(ClusterRequest request)
      throws AmbariException;

  /**
   * Update the service identified by the given request object with the
   * values carried by the given request object.
   *
   * @param requests    the request object which defines which service to
   *                   update and the values to set
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public TrackActionResponse updateServices(Set<ServiceRequest> request)
      throws AmbariException;

  /**
   * Update the component identified by the given request object with the
   * values carried by the given request object.
   *
   * @param requests    the request object which defines which component to
   *                   update and the values to set
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public TrackActionResponse updateComponents(
      Set<ServiceComponentRequest> request) throws AmbariException;

  /**
   * Update the host identified by the given request object with the
   * values carried by the given request object.
   *
   * @param requests    the request object which defines which host to
   *                   update and the values to set
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public void updateHosts(Set<HostRequest> request)
      throws AmbariException;

  /**
   * Update the host component identified by the given request object with the
   * values carried by the given request object.
   *
   * @param requests    the request object which defines which host component to
   *                   update and the values to set
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public TrackActionResponse updateHostComponents(
      Set<ServiceComponentHostRequest> request) throws AmbariException;


  // ----- Delete -----------------------------------------------------------

  /**
   * Delete the cluster identified by the given request object.
   *
   * @param request  the request object which identifies which cluster to delete
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public void deleteCluster(ClusterRequest request) throws AmbariException;

  /**
   * Delete the service identified by the given request object.
   *
   * @return a track action response
   *
   * @param requests  the request object which identifies which service to delete
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public TrackActionResponse deleteServices(Set<ServiceRequest> request)
      throws AmbariException;

  /**
   * Delete the component identified by the given request object.
   *
   * @return a track action response
   *
   * @param requests  the request object which identifies which component to delete
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public TrackActionResponse deleteComponents(
      Set<ServiceComponentRequest> request) throws AmbariException;

  /**
   * Delete the host identified by the given request object.
   *
   * @param requests  the request object which identifies which host to delete
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public void deleteHosts(Set<HostRequest> request)
      throws AmbariException;

  /**
   * Delete the host component identified by the given request object.
   *
   * @param requests  the request object which identifies which host component to delete
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public TrackActionResponse deleteHostComponents(
      Set<ServiceComponentHostRequest> request) throws AmbariException;

  public TrackActionResponse createOperations(Set<OperationRequest> request)
      throws AmbariException;

  public void getOperations(Set<OperationRequest> request)
      throws AmbariException;
}
