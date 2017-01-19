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

package org.apache.ambari.server.state;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.scheduler.RequestExecution;

import com.google.common.collect.ListMultimap;

public interface Cluster {

  /**
   * Get the cluster ID
   */
  long getClusterId();

  /**
   * Get the Cluster Name
   */
  String getClusterName();

  /**
   * Set the Cluster Name
   */
  void setClusterName(String clusterName);

  /**
   * Gets the Cluster's resource ID
   */
  Long getResourceId();

  /**
   * Add a service to a cluster
   * @param service
   */
  void addService(Service service);

  /**
   * Get a service
   * @param serviceName
   * @return
   */
  Service getService(String serviceName) throws AmbariException;

  /**
   * Get all services
   * @return
   */
  Map<String, Service> getServices();

  /**
   * Get all ServiceComponentHosts on a given host
   * @param hostname
   * @return
   */
  List<ServiceComponentHost> getServiceComponentHosts(String hostname);

  /**
   * Gets a map of components to hosts they are installed on.
   * <p>
   * This may may be filtered by host and/or service by optionally providing a set of hostname
   * and/or service names to use as a filter.  <code>null</code> for either filter indicates no
   * filter (or all), an empty set indicates a complete filter (or none).
   *
   * @param hostNames
   * @param serviceNames
   * @return a map of (filtered) components to hosts
   */
  Map<String, Set<String>> getServiceComponentHostMap(Set<String> hostNames, Set<String> serviceNames);

  /**
   * Get all ServiceComponentHosts for a given service and optional component
   *
   * If the component name is <code>null</code>, all components for the requested service will be returned.
   *
   * @param serviceName the name a the desired service
   * @param componentName the name a the desired component - null indicates all components for the service
   * @return a list of found ServiceComponentHost instances
   */
  List<ServiceComponentHost> getServiceComponentHosts(String serviceName, String componentName);

  /**
   * Get all hosts associated with this cluster.
   *
   * @return collection of hosts that are associated with this cluster
   */
  Collection<Host> getHosts();

  /**
   * Get all of the hosts running the provided service and component.
   * @param serviceName
   * @param componentName
   * @return
   */
  Set<String> getHosts(String serviceName, String componentName);


  /**
   * Adds schs to cluster AND persists them
   * TODO consider making persisting optional
   * @param serviceComponentHosts
   * @throws AmbariException
   */
  void addServiceComponentHosts(Collection<ServiceComponentHost> serviceComponentHosts) throws AmbariException;

  /**
   * Remove ServiceComponentHost from cluster
   * @param svcCompHost
   */
  void removeServiceComponentHost(ServiceComponentHost svcCompHost)
      throws AmbariException;


  /**
   * Get the ClusterVersionEntity object whose state is CURRENT.
   * @return Cluster Version entity to whose state is CURRENT.
   */
  ClusterVersionEntity getCurrentClusterVersion();

  /**
   * Gets the current stack version associated with the cluster.
   * <ul>
   * <li>if there is no upgrade in progress then get the
   * {@link ClusterVersionEntity} object whose state is
   * {@link RepositoryVersionState#CURRENT}.
   * <li>If an upgrade is in progress then based on the direction and the
   * desired stack determine which version to use. Assuming upgrading from HDP
   * 2.2.0.0-1 to 2.3.0.0-2:
   * <ul>
   * <li>RU Upgrade: 2.3.0.0-2 (desired stack id)
   * <li>RU Downgrade: 2.2.0.0-1 (desired stack id)
   * <li>EU Upgrade: while stopping services and before changing desired stack,
   * use 2.2.0.0-1, after, use 2.3.0.0-2
   * <li>EU Downgrade: while stopping services and before changing desired
   * stack, use 2.3.0.0-2, after, use 2.2.0.0-1
   * </ul>
   * </ul>
   *
   * This method must take into account both a running and a suspended upgrade.
   *
   * @return the effective cluster stack version given the current upgrading
   *         conditions of the cluster.
   */
  ClusterVersionEntity getEffectiveClusterVersion() throws AmbariException;

  /**
   * Get all of the ClusterVersionEntity objects for the cluster.
   * @return
   */
  Collection<ClusterVersionEntity> getAllClusterVersions();

  /**
   * Get desired stack version
   * @return
   */
  StackId getDesiredStackVersion();

  /**
   * Set desired stack version
   * @param stackVersion
   */
  void setDesiredStackVersion(StackId stackVersion) throws AmbariException;

  /**
   * Sets the desired stack version, optionally setting all owned services,
   * components, and host components
   * @param stackId the stack id
   * @param cascade {@code true} to cascade the desired version
   */
  public void setDesiredStackVersion(StackId stackId, boolean cascade) throws AmbariException;


  /**
   * Get current stack version
   * @return
   */
  StackId getCurrentStackVersion();

  /**
   * Set current stack version
   * @param stackVersion
   */
  void setCurrentStackVersion(StackId stackVersion) throws AmbariException;

  /**
   * Create host versions for all of the hosts that don't already have the stack version.
   * @param hostNames Collection of host names
   * @param currentClusterVersion Entity that contains the cluster's current stack (with its name and version)
   * @param desiredState Desired state must be {@link RepositoryVersionState#CURRENT} or {@link RepositoryVersionState#UPGRADING}
   * @throws AmbariException
   */
  void mapHostVersions(Set<String> hostNames,
      ClusterVersionEntity currentClusterVersion,
      RepositoryVersionState desiredState) throws AmbariException;

  /**
   * Creates or updates host versions for all of the hosts within a cluster
   * based on state of cluster stack version. This is used to transition all
   * hosts into the specified state.
   * <p/>
   * The difference between this method compared to
   * {@link Cluster#mapHostVersions} is that it affects all hosts (not only
   * missing hosts).
   * <p/>
   * Hosts that are in maintenance mode will be transititioned directly into
   * {@link RepositoryVersionState#OUT_OF_SYNC} instead.
   *
   * @param sourceClusterVersion
   *          cluster version to be queried for a stack name/version info and
   *          desired RepositoryVersionState. The only valid state of a cluster
   *          version is {@link RepositoryVersionState#INSTALLING}
   * @param state
   *          the state to transition the cluster's hosts to.
   * @throws AmbariException
   */
  void transitionHosts(ClusterVersionEntity sourceClusterVersion, RepositoryVersionState state)
      throws AmbariException;

  /**
   * For a given host, will either either update an existing Host Version Entity for the given version, or create
   * one if it doesn't exist
   *
   * @param host Host Entity object
   * @param repositoryVersion Repository Version that the host is transitioning to
   * @param stack Stack information with the version
   * @return Returns either the newly created or the updated Host Version Entity.
   * @throws AmbariException
   */
  HostVersionEntity transitionHostVersionState(HostEntity host,
      final RepositoryVersionEntity repositoryVersion, final StackId stack)
      throws AmbariException;

  /**
   * Update state of a cluster stack version for cluster based on states of host versions and stackids.
   * @param repositoryVersion the repository version entity whose version is a value like 2.2.1.0-100)
   * @throws AmbariException
   */
  void recalculateClusterVersionState(RepositoryVersionEntity repositoryVersion) throws AmbariException;

  /**
   * Update state of all cluster stack versions for cluster based on states of host versions.
   * @throws AmbariException
   */
  void recalculateAllClusterVersionStates() throws AmbariException;

  /**
   * Create a cluster version for the given stack and version, whose initial
   * state must either be either {@link RepositoryVersionState#UPGRADING} (if no
   * other cluster version exists) or {@link RepositoryVersionState#INSTALLING}
   * (if at exactly one CURRENT cluster version already exists) or {@link RepositoryVersionState#INIT}
   * (if the cluster is being created using a specific repository version).
   *
   * @param stackId
   *          Stack ID
   * @param version
   *          Stack version
   * @param userName
   *          User performing the operation
   * @param state
   *          Initial state
   * @throws AmbariException
   */
  void createClusterVersion(StackId stackId, String version,
      String userName, RepositoryVersionState state) throws AmbariException;

  /**
   * Transition an existing cluster version from one state to another.
   *
   * @param stackId
   *          Stack ID
   * @param version
   *          Stack version
   * @param state
   *          Desired state
   * @throws AmbariException
   */
  void transitionClusterVersion(StackId stackId, String version,
      RepositoryVersionState state) throws AmbariException;

  /**
   * Gets whether the cluster is still initializing or has finished with its
   * deployment requests.
   *
   * @return either {@link State#INIT} or {@link State#INSTALLED}, never
   *         {@code null}.
   */
  State getProvisioningState();

  /**
   * Sets the provisioning state of the cluster.
   *
   * @param provisioningState
   *          the provisioning state, not {@code null}.
   */
  void setProvisioningState(State provisioningState);

  /**
   * Gets the cluster's security type.
   *
   * @return this Cluster's security type
   */
  SecurityType getSecurityType();

  /**
   * Sets this Cluster's security type.
   *
   * @param securityType a SecurityType to set
   */
  void setSecurityType(SecurityType securityType);

  /**
   * Gets all configs that match the specified type.  Result is not the
   * DESIRED configuration for a cluster.
   * @param configType  the config type to return
   * @return  a map of configuration objects that have been set for the given type
   */
  Map<String, Config> getConfigsByType(String configType);

  /**
   * Gets all properties types that mach the specified type.
   * @param configType the config type to return
   * @return properties types for given config type
   */
  Map<PropertyInfo.PropertyType, Set<String>> getConfigPropertiesTypes(String configType);

  /**
   * Gets the specific config that matches the specified type and tag.  This not
   * necessarily a DESIRED configuration that applies to a cluster.
   * @param configType  the config type to find
   * @param versionTag  the config version tag to find
   * @return  a {@link Config} object, or <code>null</code> if the specific type
   *          and version have not been set.
   */
  Config getConfig(String configType, String versionTag);

  /**
   * Gets the specific config that matches the specified type and version.  This not
   * necessarily a DESIRED configuration that applies to a cluster.
   * @param configType  the config type to find
   * @param configVersion  the config version to find
   * @return  a {@link Config} object, or <code>null</code> if the specific type
   *          and version have not been set.
   */
  Config getConfigByVersion(String configType, Long configVersion);

  /**
   * Sets a specific config.  NOTE:  This is not a DESIRED configuration that
   * applies to a cluster.
   * @param config  the config instance to add
   */
  void addConfig(Config config);

  /**
   * Gets all configurations defined for a cluster.
   * @return  the collection of all configs that have been defined.
   */
  Collection<Config> getAllConfigs();

  /**
   * Adds and sets a DESIRED configuration to be applied to a cluster.  There
   * can be only one selected config per type.
   * @param user the user making the change for audit purposes
   * @param configs  the set of {@link org.apache.ambari.server.state.Config} objects to set as desired
   * @return <code>true</code> if the config was added, or <code>false</code>
   * if the config is already set as the current
   */
  ServiceConfigVersionResponse addDesiredConfig(String user, Set<Config> configs);

  /**
   * Adds and sets a DESIRED configuration to be applied to a cluster.  There
   * can be only one selected config per type.
   * @param user the user making the change for audit purposes
   * @param configs  the set of {@link org.apache.ambari.server.state.Config} objects to set as desired
   * @param serviceConfigVersionNote note to attach to service config version if created
   * @return <code>true</code> if the config was added, or <code>false</code>
   * if the config is already set as the current
   */
  ServiceConfigVersionResponse addDesiredConfig(String user, Set<Config> configs, String serviceConfigVersionNote);

  ServiceConfigVersionResponse createServiceConfigVersion(String serviceName, String user, String note,
                                                          ConfigGroup configGroup);

  String getServiceForConfigTypes(Collection<String> configTypes);

  /**
   * Apply specified service config version (rollback)
   * @param serviceName service name
   * @param version service config version
   * @param user the user making the change for audit purposes
   * @param note
   * @return service config version created
   * @throws AmbariException
   */
  ServiceConfigVersionResponse setServiceConfigVersion(String serviceName, Long version, String user, String note) throws AmbariException;

  /**
   * Get currently active service config versions for stack services
   * @return
   */
  Map<String, Collection<ServiceConfigVersionResponse>> getActiveServiceConfigVersions();

  /**
   * Get active service config version responses for all config groups of a service
   * @param serviceName service name
   * @return
   */
  public List<ServiceConfigVersionResponse> getActiveServiceConfigVersionResponse(String serviceName);

  /**
   * Get service config version history
   * @return
   */
  List<ServiceConfigVersionResponse> getServiceConfigVersions();

  /**
   * Gets the desired (and selected) config by type.
   * @param configType  the type of configuration
   * @return  the {@link Config} instance, or <code>null</code> if the type has
   * not been set.
   */
  Config getDesiredConfigByType(String configType);

  /**
   * Check if config type exists in cluster.
   * @param configType the type of configuration
   * @return <code>true</code> if config type exists, else - <code>false</code>
   */
  boolean isConfigTypeExists(String configType);
  /**
   * Gets the active desired configurations for the cluster.
   * @return a map of type-to-configuration information.
   */
  Map<String, DesiredConfig> getDesiredConfigs();

  /**
   * Gets all versions of the desired configurations for the cluster.
   * @return a map of type-to-configuration information.
   */
  Map<String, Set<DesiredConfig>> getAllDesiredConfigVersions();


  /**
   * Creates a cluster response based on the current cluster definition
   * @return
   * @throws AmbariException
   */
  ClusterResponse convertToResponse() throws AmbariException;

  /**
   * Refreshes the cluster details
   */
  void refresh();

  /**
   * Creates a debug dump based on the current cluster state
   * @param sb
   */
  void debugDump(StringBuilder sb);

  /**
   * Delete all the services associated with this cluster
   * @throws AmbariException
   */
  void deleteAllServices() throws AmbariException;

  /**
   * Delete the named service associated with this cluster
   * @param serviceName
   * @throws AmbariException
   */
  void deleteService(String serviceName) throws AmbariException;

  /**
   * Gets if the cluster can be deleted
   * @return
   */
  boolean canBeRemoved();

  /**
   * Delete the cluster
   * @throws AmbariException
   */
  void delete() throws AmbariException;

  /**
   * Add service to the cluster
   * @param serviceName
   * @return
   * @throws AmbariException
   */
  Service addService(String serviceName) throws AmbariException;

  /**
   * Fetch desired configs for list of hosts in cluster
   * @param hostIds
   * @return
   */
  Map<Long, Map<String, DesiredConfig>> getHostsDesiredConfigs(Collection<Long> hostIds);

  /**
   * Fetch desired configs for all hosts in cluster
   * @return
   */
  Map<Long, Map<String, DesiredConfig>> getAllHostsDesiredConfigs();

  /**
   * Add a new config group to the set of Config groups associated with this
   * cluster
   * @param configGroup
   * @throws AmbariException
   */
  void addConfigGroup(ConfigGroup configGroup) throws AmbariException;

  /**
   * Get config groups associated with this cluster
   * @return unmodifiable map of config group id to config group.  Will not return null.
   */
  Map<Long, ConfigGroup> getConfigGroups();

  /**
   * Delete this config group identified by the config group id
   * @param id
   * @throws AmbariException
   */
  void deleteConfigGroup(Long id) throws AmbariException, AuthorizationException;

  /**
   * Find all config groups associated with the give hostname
   * @param hostname
   * @return Map of config group id to config group
   */
  Map<Long, ConfigGroup> getConfigGroupsByHostname(String hostname)
      throws AmbariException;

  /**
   * Add a @RequestExecution to the cluster
   * @param requestExecution
   * @throws AmbariException
   */
  void addRequestExecution(RequestExecution requestExecution)
      throws AmbariException;

  /**
   * Get all @RequestExecution objects associated with the cluster
   * @return
   */
  Map<Long, RequestExecution> getAllRequestExecutions();

  /**
   * Delete a @RequestExecution associated with the cluster
   * @param id
   * @throws AmbariException
   */
  void deleteRequestExecution(Long id) throws AmbariException;

  /**
   * Get next version of specified config type
   * @param type config type
   * @return next version of config
   */
  Long getNextConfigVersion(String type);

  /**
   * Bulk handle service component host events
   *
   * @param eventMap serviceName - event mapping
   * @return map of failed events where key is event and value is short message
   */
  Map<ServiceComponentHostEvent, String> processServiceComponentHostEvents(ListMultimap<String, ServiceComponentHostEvent> eventMap);

  /**
   * Determine whether or not access to this cluster resource should be allowed based
   * on the given privilege.
   *
   * @param privilegeEntity  the privilege
   * @param readOnly         indicate whether or not this check is for a read only operation
   *
   * @return true if the access to this cluster is allowed
   */
  boolean checkPermission(PrivilegeEntity privilegeEntity, boolean readOnly);

  /**
   * Add the given map of attributes to the session for this cluster.
   *
   * @param attributes  the session attributes
   */
  void addSessionAttributes(Map<String, Object> attributes);

  /**
   * Sets or adds an attribute in the session for this cluster
   *
   * @param key   the name of the key which identifies the attribute in the map
   * @param value the value to set
   */
  void setSessionAttribute(String key, Object value);

  /**
   * Removes an attribute from the session for this cluster
   *
   * @param key the name of the key which identifies the attribute in the map
   */
  void removeSessionAttribute(String key);

  /**
   * Get the map of session attributes for this cluster.
   *
   * @return the map of session attributes for this cluster; never null
   */
  Map<String, Object> getSessionAttributes();

  /**
   * Makes the most recent configurations in the specified stack the current set
   * of configurations. This method will first ensure that the cluster's current
   * stack matches that of the configuration stack specified.
   * <p/>
   * When completed, all other configurations for any other stack will remain,
   * but will not be marked as selected.
   *
   * @param stackId
   *          the stack to use when finding the latest configurations (not
   *          {@code null}).
   */
  void applyLatestConfigurations(StackId stackId);

  /**
   * Removes all cluster configurations and service configurations that belong
   * to the specified stack.
   *
   * @param stackId
   *          the stack to use when finding the configurations to remove (not
   *          {@code null}).
   */
  void removeConfigurations(StackId stackId);

  /**
   * Returns whether this cluster was provisioned by a Blueprint or not.
   * @return true if the cluster was deployed with a Blueprint otherwise false.
   */
  boolean isBluePrintDeployed();

  /**
   * @return upgrade that is in progress for a cluster. If no upgrade is going
   * on, a null is returned.
   */
  UpgradeEntity getUpgradeEntity();

  /**
   * The value is explicitly set on the ClusterEntity when Creating,
   * Aborting (switching to downgrade), Resuming, or Finalizing an upgrade.
   * @param upgradeEntity the upgrade entity to set for cluster
   * @throws AmbariException
   */
  void setUpgradeEntity(UpgradeEntity upgradeEntity) throws AmbariException;

  /**
   * Gets whether there is an upgrade which has been suspended and not yet
   * finalized.
   *
   * @return {@code true} if the last upgrade is in the
   *         {@link UpgradeState#SUSPENDED}.
   */
  boolean isUpgradeSuspended();

  /**
   * Gets an {@link UpgradeEntity} if there is an upgrade in progress or an
   * upgrade that has been suspended. This will first check
   * {@link #getUpgradeEntity()} and return that if it is not {@code null}.
   * Otherwise, this will perform a search for the most recent upgrade/downgrade
   * which has not been completed.
   *
   * @return an upgrade which will either be in progress or suspended, or
   *         {@code null} if none.
   */
  UpgradeEntity getUpgradeInProgress();

  /**
   * Returns the name of the service that the passed config type belongs to.
   *
   * @param configType
   *          the config type to look up the service by
   * @return returns the name of the service that the config type belongs to if
   *         there is any otherwise returns null.
   */
  String getServiceByConfigType(String configType);

  /**
   * Gets the most recent value of {@code cluster-env/propertyName} where
   * {@code propertyName} is the paramter specified to the method. This will use
   * the desired configuration for {@code cluster-env}.
   * <p/>
   * The value is cached on this {@link Cluster} instance, so subsequent calls
   * will not inclur a lookup penalty. This class also responds to
   * {@link ClusterConfigChangedEvent} in order to clear the cache.
   *
   * @param propertyName
   *          the property to lookup in {@code cluster-env} (not {@code null}).
   * @param defaultValue
   *          a default value to cache return if none exists (may be
   *          {@code null}).
   * @return
   */
  String getClusterProperty(String propertyName, String defaultValue);

  /**
   * Returns the number of hosts that form the cluster.
   * @return number of hosts that form the cluster
   */
  int  getClusterSize();

  /**
   * Gets a new instance of a {@link RoleCommandOrder} for this cluster.
   *
   * @return the role command order instance (not {@code null}).
   */
  RoleCommandOrder getRoleCommandOrder();

  /**
   * Adds upgrade specific command and role parameters to the command maps if
   * there is a suspended upgrade. If there is not a suspended upgrade, then the
   * maps are not modified.
   * <p/>
   *
   * @param commandParams
   *          the command parameter map to supplement (not {@code null}).
   * @param roleParams
   *          the role parameter map to supplement (not {@code null}).
   */
  void addSuspendedUpgradeParameters(Map<String, String> commandParams,
      Map<String, String> roleParams);
}
