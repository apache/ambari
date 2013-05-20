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
import org.apache.ambari.server.ParentObjectNotFoundException;

import java.util.Map;
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
  public void createServices(Set<ServiceRequest> requests)
      throws AmbariException, ParentObjectNotFoundException;

  /**
   * Create the component defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the component to be created
   *
   * @throws AmbariException thrown if the component cannot be created
   */
  public void createComponents(Set<ServiceComponentRequest> requests)
      throws AmbariException;

  /**
   * Create the host defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the host to be created
   *
   * @throws AmbariException thrown if the host cannot be created
   */
  public void createHosts(Set<HostRequest> requests)
      throws AmbariException;

  /**
   * Create the host component defined by the attributes in the given request object.
   *
   * @param requests  the request object which defines the host component to be created
   *
   * @throws AmbariException thrown if the host component cannot be created
   */
  public void createHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException;

  /**
   * Creates a configuration.
   *
   * @param request the request object which defines the configuration.
   *
   * @throws AmbariException when the configuration cannot be created.
   */
  public void createConfiguration(ConfigurationRequest request)
      throws AmbariException;
  
  /**
   * Creates users.
   * 
   * @param requests the request objects which defines the user.
   * 
   * @throws AmbariException when the user cannot be created.
   */
  public void createUsers(Set<UserRequest> requests) throws AmbariException;


  // ----- Read -------------------------------------------------------------

  /**
   * Get the clusters identified by the given request objects.
   *
   * @param requests  the request objects which identify the clusters to be returned
   *
   * @return a set of cluster responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ClusterResponse> getClusters(Set<ClusterRequest> requests)
      throws AmbariException;

  /**
   * Get the services identified by the given request objects.
   *
   * @param requests  the request objects which identify the services
   * to be returned
   *
   * @return a set of service responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ServiceResponse> getServices(Set<ServiceRequest> requests)
      throws AmbariException;

  /**
   * Get the components identified by the given request objects.
   *
   * @param requests  the request objects which identify the components to be returned
   *
   * @return a set of component responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ServiceComponentResponse> getComponents(
      Set<ServiceComponentRequest> requests) throws AmbariException;

  /**
   * Get the hosts identified by the given request objects.
   *
   * @param requests  the request objects which identify the hosts to be returned
   *
   * @return a set of host responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<HostResponse> getHosts(Set<HostRequest> requests)
      throws AmbariException;

  /**
   * Get the host components identified by the given request objects.
   *
   * @param requests  the request objects which identify the host components
   * to be returned
   *
   * @return a set of host component responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ServiceComponentHostResponse> getHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException;

  /**
   * Gets the configurations identified by the given request objects.
   *
   * @param requests   the request objects
   *
   * @return  a set of configuration responses
   *
   * @throws AmbariException if the configurations could not be read
   */
  public Set<ConfigurationResponse> getConfigurations(
      Set<ConfigurationRequest> requests) throws AmbariException;

  /**
   * Gets the request status identified by the given request object.
   *
   * @param request   the request object
   *
   * @return  a set of request status responses
   *
   * @throws AmbariException if the request status could not be read
   */
  public Set<RequestStatusResponse> getRequestStatus(RequestStatusRequest request)
      throws AmbariException;

  /**
   * Gets the task status identified by the given request objects.
   *
   * @param requests   the request objects
   *
   * @return  a set of task status responses
   *
   * @throws AmbariException if the configurations could not be read
   */
  public Set<TaskStatusResponse> getTaskStatus(Set<TaskStatusRequest> requests)
      throws AmbariException;

  /**
   * Gets the users identified by the given request objects.
   *
   * @param requests  the request objects
   * 
   * @return  a set of user responses
   * 
   * @throws AmbariException if the users could not be read
   */
  public Set<UserResponse> getUsers(Set<UserRequest> requests)
      throws AmbariException;
  
  /**
   * Gets the host component config mappings
   * 
   * @param request the host component request
   * 
   * @return the configuration mappings
   * 
   * @throws AmbariException
   */
  public Map<String, String> getHostComponentDesiredConfigMapping(
      ServiceComponentHostRequest request) throws AmbariException;

  // ----- Update -----------------------------------------------------------

  /**
   * Update the cluster identified by the given request object with the
   * values carried by the given request object.
   *
   *
   * @param request           request object which defines which cluster to
   *                          update and the values to set
   * @param requestProperties request specific properties independent of resource
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public RequestStatusResponse updateCluster(ClusterRequest request,
                                             Map<String, String> requestProperties)
      throws AmbariException;

  /**
   * Update the service identified by the given request object with the
   * values carried by the given request object.
   *
   *
   *
   * @param requests    the request object which defines which service to
   *                   update and the values to set
   *
   * @param requestProperties
   * @param reconfigureClients
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public RequestStatusResponse updateServices(Set<ServiceRequest> requests,
      Map<String, String> requestProperties, boolean runSmokeTest,
      boolean reconfigureClients) throws AmbariException;

  /**
   * Update the component identified by the given request object with the
   * values carried by the given request object.
   *
   *
   *
   * @param requests    the request object which defines which component to
   *                   update and the values to set
   *
   * @param requestProperties
   * @param runSmokeTest
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public RequestStatusResponse updateComponents(
      Set<ServiceComponentRequest> requests, Map<String, String> requestProperties,
      boolean runSmokeTest) throws AmbariException;

  /**
   * Update the host identified by the given request object with the
   * values carried by the given request object.
   *
   * @param requests    the request object which defines which host to
   *                   update and the values to set
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public void updateHosts(Set<HostRequest> requests)
      throws AmbariException;

  /**
   * Update the host component identified by the given request object with the
   * values carried by the given request object.
   *
   *
   *
   * @param requests    the request object which defines which host component to
   *                   update and the values to set
   *
   * @param requestProperties
   * @param runSmokeTest
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public RequestStatusResponse updateHostComponents(
      Set<ServiceComponentHostRequest> requests, Map<String, String> requestProperties, boolean runSmokeTest) throws AmbariException;
  
  /**
   * Updates the users specified.
   * 
   * @param requests  the users to modify
   * 
   * @throws  AmbariException if the resources cannot be updated
   */
  public void updateUsers(Set<UserRequest> requests) throws AmbariException;


  // ----- Delete -----------------------------------------------------------

  /**
   * Delete the cluster identified by the given request object.
   *
   * @param request  the request object which identifies which cluster to delete
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public void deleteCluster(ClusterRequest request) throws AmbariException;

  /**
   * Delete the service identified by the given request object.
   *
   * @param requests  the request object which identifies which service to delete
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public RequestStatusResponse deleteServices(Set<ServiceRequest> requests)
      throws AmbariException;

  /**
   * Delete the component identified by the given request object.
   *
   * @param requests  the request object which identifies which component to delete
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public RequestStatusResponse deleteComponents(
      Set<ServiceComponentRequest> requests) throws AmbariException;

  /**
   * Delete the host identified by the given request object.
   *
   * @param requests  the request object which identifies which host to delete
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public void deleteHosts(Set<HostRequest> requests)
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
  public RequestStatusResponse deleteHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException;
  
  /**
   * Deletes the users specified.
   * 
   * @param requests  the users to delete
   * 
   * @throws  AmbariException if the resources cannot be deleted
   */
  public void deleteUsers(Set<UserRequest> requests) throws AmbariException;
  
  /**
   * Create the action defined by the attributes in the given request object.
   *
   * @param request the request object which defines the action to be created
   * @param requestProperties the request properties
   *
   * @throws AmbariException thrown if the action cannot be created
   */
  public RequestStatusResponse createActions(Set<ActionRequest> request, Map<String, String> requestProperties)
      throws AmbariException;
  
  /**
   * Get the actions identified by the given request objects.
   *
   * @param request  the request objects which identify the actions to be returned
   *
   * @return a set of actions responses
   *
   * @throws AmbariException thrown if the resource cannot be read
   */
  public Set<ActionResponse> getActions(Set<ActionRequest> request)
      throws AmbariException;


  /**
   * Get supported stacks.
   * 
   * @param requests the stacks
   * 
   * @return a set of stacks responses
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public Set<StackResponse> getStacks(Set<StackRequest> requests) throws AmbariException;
  
  
  /**
   * Get supported stacks versions.
   * 
   * @param requests the stacks versions
   * 
   * @return a set of stacks versions responses
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public Set<StackVersionResponse> getStackVersions(Set<StackVersionRequest> requests) throws AmbariException;

  
  /**
   * Get repositories by stack name, version and operating system.
   * 
   * @param requests the repositories 
   * 
   * @return a set of repositories
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public Set<RepositoryResponse> getRepositories(Set<RepositoryRequest> requests) throws AmbariException;

  
  /**
   * Get repositories by stack name, version.
   * 
   * @param requests the services 
   * 
   * @return a set of services
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public Set<StackServiceResponse> getStackServices(Set<StackServiceRequest> requests) throws AmbariException;

  
  /**
   * Get configurations by stack name, version and service.
   * 
   * @param requests the configurations 
   * 
   * @return a set of configurations
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public Set<StackConfigurationResponse> getStackConfigurations(Set<StackConfigurationRequest> requests) throws AmbariException;
  
  
  /**
   * Get components by stack name, version and service.
   * 
   * @param requests the components 
   * 
   * @return a set of components
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public Set<StackServiceComponentResponse> getStackComponents(Set<StackServiceComponentRequest> requests) throws AmbariException;
  
  
  /**
   * Get operating systems by stack name, version.
   * 
   * @param requests the operating systems 
   * 
   * @return a set of operating systems 
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public Set<OperatingSystemResponse> getStackOperatingSystems(Set<OperatingSystemRequest> requests) throws AmbariException;
}
