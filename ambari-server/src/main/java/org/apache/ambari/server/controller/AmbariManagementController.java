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
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;

import java.util.Collection;
import java.util.List;
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
  

  // ----- Update -----------------------------------------------------------

  /**
   * Update the cluster identified by the given request object with the
   * values carried by the given request object.
   *
   *
   * @param requests          request objects which define which cluster to
   *                          update and the values to set
   * @param requestProperties request specific properties independent of resource
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public RequestStatusResponse updateClusters(Set<ClusterRequest> requests,
                                              Map<String, String> requestProperties)
      throws AmbariException;

  /**
   * Update the host component identified by the given request object with the
   * values carried by the given request object.
   *
   *
   *
   * @param requests           the request object which defines which host component to
   *                           update and the values to set
   * @param requestProperties  the request properties
   * @param runSmokeTest       indicates whether or not to run a smoke test
   *
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
   * @param actionRequest the request object which defines the action to be created
   * @param requestProperties the request properties
   *
   * @throws AmbariException thrown if the action cannot be created
   */
  public RequestStatusResponse createAction(ExecuteActionRequest actionRequest, Map<String, String> requestProperties)
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
   * Update stacks from the files at stackRoot.
   *
   * @return a track action response
   * @throws AmbariException if
   */
  public RequestStatusResponse updateStacks() throws AmbariException;
  
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
   * Updates repositories by stack name, version and operating system.
   * 
   * @param requests the repositories
   * 
   * @throws AmbariException
   */
  void updateRespositories(Set<RepositoryRequest> requests) throws AmbariException;

  
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

  /**
   * Get all top-level services of Ambari, not related to certain cluster.
   * 
   * @param requests the top-level services 
   * 
   * @return a set of top-level services 
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  
  public Set<RootServiceResponse> getRootServices(Set<RootServiceRequest> requests) throws AmbariException;
  /**
   * Get all components of top-level services of Ambari, not related to certain cluster.
   * 
   * @param requests the components of top-level services 
   * 
   * @return a set of components 
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public Set<RootServiceComponentResponse> getRootServiceComponents(Set<RootServiceComponentRequest> requests) throws AmbariException;


  // ----- Common utility methods --------------------------------------------

  /**
   * Get the clusters for this management controller.
   *
   * @return the clusters
   */
  public Clusters getClusters();

  /**
   * Get the meta info for this management controller.
   *
   * @return the meta info
   */
  public AmbariMetaInfo getAmbariMetaInfo();

  /**
   * Get the service factory for this management controller.
   *
   * @return the service factory
   */
  public ServiceFactory getServiceFactory();

  /**
   * Get the service component factory for this management controller.
   *
   * @return the service component factory
   */
  public ServiceComponentFactory getServiceComponentFactory();

  /**
   * Get the root service response factory for this management controller.
   *
   * @return the root service response factory
   */
  public AbstractRootServiceResponseFactory getRootServiceResponseFactory();

  /**
   * Get the config group factory for this management controller.
   *
   * @return the config group factory
   */
  public ConfigGroupFactory getConfigGroupFactory();

  /**
    * Get the action manager for this management controller.
    *
    * @return the action manager
    */
  public ActionManager getActionManager();

  /**
   * Get the authenticated user's name.
   *
   * @return the authenticated user's name
   */
  public String getAuthName();

  /**
   * Create and persist the request stages and return a response containing the
   * associated request and resulting tasks.
   *
   * @param cluster             the cluster
   * @param requestProperties   the request properties
   * @param requestParameters   the request parameters; may be null
   * @param changedServices     the services being changed; may be null
   * @param changedComponents   the components being changed
   * @param changedHosts        the hosts being changed
   * @param ignoredHosts        the hosts to be ignored
   * @param runSmokeTest        indicates whether or not the smoke tests should be run
   * @param reconfigureClients  indicates whether or not the clients should be reconfigured
   *
   * @return the request response
   *
   * @throws AmbariException is thrown if the stages can not be created
   */
  public RequestStatusResponse createAndPersistStages(Cluster cluster, Map<String, String> requestProperties,
                                                      Map<String, String> requestParameters,
                                                      Map<State, List<Service>> changedServices,
                                                      Map<State, List<ServiceComponent>> changedComponents,
                                                      Map<String, Map<State, List<ServiceComponentHost>>> changedHosts,
                                                      Collection<ServiceComponentHost> ignoredHosts,
                                                      boolean runSmokeTest, boolean reconfigureClients)
                                                      throws AmbariException;

  /**
   * Add stages to the request.
   *
   * @param requestStages       Stages currently associated with request
   * @param cluster             cluster being acted on
   * @param requestProperties   the request properties
   * @param requestParameters   the request parameters; may be null
   * @param changedServices     the services being changed; may be null
   * @param changedComponents   the components being changed
   * @param changedHosts        the hosts being changed
   * @param ignoredHosts        the hosts to be ignored
   * @param runSmokeTest        indicates whether or not the smoke tests should be run
   * @param reconfigureClients  indicates whether or not the clients should be reconfigured
   *
   * @return request stages
   *
   * @throws AmbariException if stages can't be created
   */
  public RequestStageContainer addStages(RequestStageContainer requestStages, Cluster cluster, Map<String, String> requestProperties,
                                 Map<String, String> requestParameters,
                                 Map<State, List<Service>> changedServices,
                                 Map<State, List<ServiceComponent>> changedComponents,
                                 Map<String, Map<State, List<ServiceComponentHost>>> changedHosts,
                                 Collection<ServiceComponentHost> ignoredHosts,
                                 boolean runSmokeTest, boolean reconfigureClients) throws AmbariException;

  /**
   * Getter for the url of JDK, stored at server resources folder
   */
  public String getJdkResourceUrl();

  /**
   * Getter for the java home, stored in ambari.properties
   */
  public String getJavaHome();

  /**
   * Getter for the jdk name, stored in ambari.properties
   */
  public String getJDKName();

  /**
   * Getter for the jce name, stored in ambari.properties
   */
  public String getJCEName();

  /**
   * Getter for the name of server database
   */
  public String getServerDB();

  /**
   * Getter for the url of Oracle JDBC driver, stored at server resources folder
   */
  public String getOjdbcUrl();

  /**
   * Getter for the url of MySQL JDBC driver, stored at server resources folder
   */
  public String getMysqljdbcUrl();

  /**
   * Return a healthy host if found otherwise any random host
   * @throws AmbariException
   */
  public String getHealthyHost(Set<String> hostList) throws AmbariException;


  /**
   * Find configuration tags with applied overrides
   *
   * @param cluster   the cluster
   * @param hostName  the host name
   *
   * @return the configuration tags
   *
   * @throws AmbariException if configuration tags can not be obtained
   */
  public Map<String, Map<String,String>> findConfigurationTagsWithOverrides(
          Cluster cluster, String hostName) throws AmbariException;

  /**
   * Returns parameters for RCA database
   *
   * @return the map with parameters for RCA db
   *
   */
  public Map<String, String> getRcaParameters();

  /**
   * Get the Factory to create Request schedules
   * @return the request execution factory
   */
  public RequestExecutionFactory getRequestExecutionFactory();

  /**
   * Get Execution Schedule Manager
   */
  public ExecutionScheduleManager getExecutionScheduleManager();

  /**
   * Get JobTracker hostname
   */
  public String getJobTrackerHost(Cluster cluster);

  /**
   * Gets the effective maintenance state for a host component
   * @param sch the service component host
   * @return the maintenance state
   * @throws AmbariException
   */
  public MaintenanceState getEffectiveMaintenanceState(ServiceComponentHost sch)
      throws AmbariException;
}
  
