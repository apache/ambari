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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.apache.ambari.server.security.ldap.LdapSyncDto;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;

/**
 * Management controller interface.
 */
public interface AmbariManagementController {

  /**
   * Get an Ambari endpoint URI for the given path.
   *
   * @param path  the path (e.g. /api/v1/users)
   *
   * @return the Ambari endpoint URI
   */
  public String getAmbariServerURI(String path);


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
  public ConfigurationResponse createConfiguration(ConfigurationRequest request)
      throws AmbariException;

  /**
   * Creates users.
   *
   * @param requests the request objects which define the user.
   *
   * @throws AmbariException when the user cannot be created.
   */
  public void createUsers(Set<UserRequest> requests) throws AmbariException;

  /**
   * Creates groups.
   *
   * @param requests the request objects which define the groups.
   *
   * @throws AmbariException when the groups cannot be created.
   */
  public void createGroups(Set<GroupRequest> requests) throws AmbariException;

  /**
   * Creates members of the group.
   *
   * @param requests the request objects which define the members.
   *
   * @throws AmbariException when the members cannot be created.
   */
  public void createMembers(Set<MemberRequest> requests) throws AmbariException;


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
   * Get service config version history
   * @param requests service config version requests
   * @return service config versions
   * @throws AmbariException
   */
  Set<ServiceConfigVersionResponse> getServiceConfigVersions(Set<ServiceConfigVersionRequest> requests)
      throws AmbariException;

  /**
   * Gets the users identified by the given request objects.
   *
   * @param requests the request objects
   *
   * @return a set of user responses
   *
   * @throws AmbariException if the users could not be read
   */
  public Set<UserResponse> getUsers(Set<UserRequest> requests)
      throws AmbariException;

  /**
   * Gets the user groups identified by the given request objects.
   *
   * @param requests the request objects
   *
   * @return a set of group responses
   *
   * @throws AmbariException if the groups could not be read
   */
  public Set<GroupResponse> getGroups(Set<GroupRequest> requests)
      throws AmbariException;

  /**
   * Gets the group members identified by the given request objects.
   *
   * @param requests the request objects
   *
   * @return a set of member responses
   *
   * @throws AmbariException if the members could not be read
   */
  public Set<MemberResponse> getMembers(Set<MemberRequest> requests)
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
   * Updates the users specified.
   *
   * @param requests the users to modify
   *
   * @throws AmbariException if the resources cannot be updated
   */
  public void updateUsers(Set<UserRequest> requests) throws AmbariException;

  /**
   * Updates the groups specified.
   *
   * @param requests the groups to modify
   *
   * @throws AmbariException if the resources cannot be updated
   */
  public void updateGroups(Set<GroupRequest> requests) throws AmbariException;

  /**
   * Updates the members of the group specified.
   *
   * @param requests the members to be set for this group
   *
   * @throws AmbariException if the resources cannot be updated
   */
  public void updateMembers(Set<MemberRequest> requests) throws AmbariException;


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
   * @param requests the users to delete
   *
   * @throws AmbariException if the resources cannot be deleted
   */
  public void deleteUsers(Set<UserRequest> requests) throws AmbariException;

  /**
   * Deletes the user groups specified.
   *
   * @param requests the groups to delete
   *
   * @throws AmbariException if the resources cannot be deleted
   */
  public void deleteGroups(Set<GroupRequest> requests) throws AmbariException;

  /**
   * Deletes the group members specified.
   *
   * @param requests the members to delete
   *
   * @throws AmbariException if the resources cannot be deleted
   */
  public void deleteMembers(Set<MemberRequest> requests) throws AmbariException;

  /**
   * Create the action defined by the attributes in the given request object.
   * Used only for custom commands/actions.
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
  public void updateRepositories(Set<RepositoryRequest> requests) throws AmbariException;

  /**
   * Verifies repositories' base urls.
   *
   * @param requests the repositories
   *
   * @throws AmbariException if verification of any of urls fails
   */
  public void verifyRepositories(Set<RepositoryRequest> requests) throws AmbariException;

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
  public Set<OperatingSystemResponse> getOperatingSystems(Set<OperatingSystemRequest> requests) throws AmbariException;

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
   * Get service name by cluster instance and component name
   *
   * @param cluster the cluster instance
   * @param componentName the component name in String type
   *
   * @return a service name
   *
   * @throws  AmbariException if service name is null or empty
   */
  public String findServiceName(Cluster cluster, String componentName) throws AmbariException;

  /**
   * Get the clusters for this management controller.
   *
   * @return the clusters
   */
  public Clusters getClusters();

  /**
   * Get config helper
   *
   * @return config helper
   */
  ConfigHelper getConfigHelper();

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
   * Get the role graph factory for this management controller.
   *
   * @return the role graph factory
   */
  public RoleGraphFactory getRoleGraphFactory();

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
   * Chooses a healthy host from the list of candidate hosts randomly. If there
   * are no healthy hosts, then this method will return {@code null}.
   * <p/>
   * The host's {@link HostState} is used to determine if a host is healthy.
   *
   * @return a random healthy host, or {@code null}.
   * @throws AmbariException
   * @see {@link HostState#HEALTHY}
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
   * Get cached clusterUpdateResults, used only for service config versions currently
   * @param clusterRequest
   * @return
   */
  ClusterResponse getClusterUpdateResults(ClusterRequest clusterRequest);

  /**
   * Get JobTracker hostname
   * HDP-1.x is not supported anymore
   */
  @Deprecated
  public String getJobTrackerHost(Cluster cluster);

  /**
   * Gets the effective maintenance state for a host component
   * @param sch the service component host
   * @return the maintenance state
   * @throws AmbariException
   */
  public MaintenanceState getEffectiveMaintenanceState(ServiceComponentHost sch)
      throws AmbariException;

  /**
   * Get Role Command Order
   */
  public RoleCommandOrder getRoleCommandOrder(Cluster cluster);

  /**
   * Performs a test if LDAP server is reachable.
   *
   * @return true if connection to LDAP was established
   */
  public boolean checkLdapConfigured();

  /**
   * Retrieves groups and users from external LDAP.
   *
   * @return ldap sync DTO
   * @throws AmbariException if LDAP is configured incorrectly
   */
  public LdapSyncDto getLdapSyncInfo() throws AmbariException;

  /**
   * Synchronizes local users and groups with given data.
   *
   * @param userRequest  users to be synchronized
   * @param groupRequest groups to be synchronized
   *
   * @return the results of the LDAP synchronization
   *
   * @throws AmbariException if synchronization data was invalid
   */
  public LdapBatchDto synchronizeLdapUsersAndGroups(
      LdapSyncRequest userRequest, LdapSyncRequest groupRequest) throws AmbariException;

  /**
   * Checks if LDAP sync process is running.
   *
   * @return true if LDAP sync is in progress
   */
  public boolean isLdapSyncInProgress();

  /**
   * Get configurations which are specific for a cluster (!not a service).
   * @param requests
   * @return
   * @throws AmbariException
   */
  public Set<StackConfigurationResponse> getStackLevelConfigurations(Set<StackLevelConfigurationRequest> requests) throws AmbariException;

  /**
   * @param serviceInfo service info for a given service
   * @param hostParams parameter map. May be changed during method execution
   * @param osFamily os family for host
   * @return a full list of package dependencies for a service that should be
   * installed on a host
   */
  List<ServiceOsSpecific.Package> getPackagesForServiceHost(ServiceInfo serviceInfo,
                                                            Map<String, String> hostParams, String osFamily);

  /**
   * Register a change in rack information for the hosts of the given cluster.
   *
   * @param clusterName  the name of the cluster
   *
   * @throws AmbariException if an error occurs during the rack change registration
   */
  public void registerRackChange(String clusterName) throws AmbariException;

  /**
   * Initialize cluster scoped widgets and widgetLayouts for different stack
   * components.
   *
   * @param cluster @Cluster object
   * @param service @Service object
   */
  public void initializeWidgetsAndLayouts(Cluster cluster, Service service) throws AmbariException;

  /**
   * Gets an execution command for host component life cycle command
   * @return
   */
  public ExecutionCommand getExecutionCommand(Cluster cluster,
                                              ServiceComponentHost scHost,
                                              RoleCommand roleCommand) throws AmbariException;

  /**
   * Get configuration dependencies which are specific for a specific service configuration property
   * @param requests
   * @return
   */
  Set<StackConfigurationDependencyResponse> getStackConfigurationDependencies(Set<StackConfigurationDependencyRequest> requests) throws AmbariException;

  TimelineMetricCacheProvider getTimelineMetricCacheProvider();

  /**
   * Returns KerberosHelper instance
   * @return
   */
  KerberosHelper getKerberosHelper();
}

