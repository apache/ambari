/*
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

import org.apache.ambari.server.ClusterSettingNotFoundException;
import org.apache.ambari.server.ServiceGroupNotFoundException;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.scheduler.RequestExecution;

import com.google.common.collect.ListMultimap;

public interface Cluster {

  /**
   * Get the cluster ID
   */
  Long getClusterId();

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
   *
   * @param service
   */
  void addService(Service service);

  Service addService(ServiceGroup serviceGroup, String serviceName, String serviceType,
                     RepositoryVersionEntity repositoryVersion) throws AmbariException;

  Service addDependencyToService(String  serviceGroupName, String serviceName,
                                        Long dependencyServiceId) throws AmbariException;

  Service removeDependencyFromService(String  serviceGroupName, String serviceName, Long dependencyServiceId);

  /**
   * Add service group to the cluster
   *
   * @param serviceGroup
   * @return
   * @throws AmbariException
   */
  void addServiceGroup(ServiceGroup serviceGroup);

  /**
   * Add service group to the cluster
   *
   * @param serviceGroupName Service group name
   * @return
   * @throws AmbariException
   */
  ServiceGroup addServiceGroup(String serviceGroupName, String version) throws AmbariException;

  ServiceGroup addServiceGroup(String serviceGroupName, StackId stackId) throws AmbariException;

  /**
   * Add service group dependency to the service group
   *
   * @param serviceGroupName Service group name
   * @param dependencyServiceGroupId Dependency service group id
   * @return
   * @throws AmbariException
   */
  void addServiceGroupDependency(String serviceGroupName, Long dependencyServiceGroupId) throws AmbariException;


  ClusterSetting addClusterSetting(String clusterSettingName, String clusterSettingValue) throws AmbariException;

  /**
   * Add 'cluster setting' to the cluster
   *
   * @param clusterSetting
   * @return
   * @throws AmbariException
   */
  void addClusterSetting(ClusterSetting clusterSetting);

  /**
   * Update 'cluster setting' in the cluster
   *
   * @param clusterSettingName Cluster setting name
   * @return
   * @throws AmbariException
   */
  ClusterSetting updateClusterSetting(String clusterSettingName, String clusterSettingValue) throws AmbariException;

  /**
   * Add 'cluster setting' in the cluster
   *
   * @param clusterSetting
   * @return
   * @throws AmbariException
   */
  void updateClusterSetting(ClusterSetting clusterSetting);

  //TODO remove when UI starts using service groups
  /**
   * Get a service
   *
   * @param serviceName
   * @return
   */
  Service getService(String serviceName) throws AmbariException;

  Service getService(String serviceGroupName, String serviceName) throws AmbariException;

  Service getService(Long serviceId) throws AmbariException;

  /**
   * Gets a service from the given component name.
   *
   * @param componentName
   * @return
   * @throws AmbariException
   */
  Service getServiceByComponentName(String componentName) throws AmbariException;

  /**
   * Gets a service from the given component Id.
   *
   * @param componentId
   * @return
   * @throws AmbariException
   */

  Service getServiceByComponentId(Long componentId) throws AmbariException;

  Long getComponentId(String componentName) throws AmbariException;

  String getComponentName(Long componentId) throws AmbariException;

  String getComponentType(Long componentId) throws AmbariException;

  /**
   * Get all services
   *
   * @return
   */
  Map<String, Service> getServices();

  Map<Long, Service> getServicesById();

  /**
   * Get a service group
   *
   * @param serviceGroupName
   * @return
   */
  ServiceGroup getServiceGroup(String serviceGroupName) throws ServiceGroupNotFoundException;

  /**
   * Get a service group
   * @param serviceGroupId
   * @return
   */
  ServiceGroup getServiceGroup(Long serviceGroupId) throws ServiceGroupNotFoundException;


  /**
   * Get all service groups
   *
   * @return
   */
  Map<String, ServiceGroup> getServiceGroups() throws AmbariException;

  /**
   * Get a cluster setting
   *
   * @param clusterSettingName
   * @return
   */
  ClusterSetting getClusterSetting(String clusterSettingName) throws ClusterSettingNotFoundException;

  /**
   * Get a cluster setting
   *
   * @param clusterSettingId
   * @return
   */
  ClusterSetting getClusterSetting(Long clusterSettingId) throws ClusterSettingNotFoundException;

  /**
   * Get all cluster settings
   *
   * @return
   */
  Map<String, ClusterSetting> getClusterSettings() throws AmbariException;

  /**
   * Get all cluster settings name and value as Map.
   * If cluster settings are not fonud, returns an Empty Map.
   *
   * @return
   */
  Map<String, String> getClusterSettingsNameValueMap() throws AmbariException;

  /**
   * Get all ServiceComponentHosts on a given host
   *
   * @param hostname
   * @return
   */
  List<ServiceComponentHost> getServiceComponentHosts(String hostname);

  /**
   * Gets a map of components to hosts they are installed on.
   * <p/>
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
   * <p/>
   * If the component name is <code>null</code>, all components for the requested service will be returned.
   *
   * @param serviceName   the name a the desired service
   * @param componentName the name a the desired component - null indicates all components for the service
   * @return a list of found ServiceComponentHost instances
   */
  List<ServiceComponentHost> getServiceComponentHosts(String serviceName, String componentName);

  /**
   * Get all ServiceComponentHosts for this cluster.
   */
  List<ServiceComponentHost> getServiceComponentHosts();

  /**
   * Get all hosts associated with this cluster.
   *
   * @return collection of hosts that are associated with this cluster
   */
  Collection<Host> getHosts();

  /**
   * Get all of the hosts running the provided service and component.
   *
   * @param serviceName
   * @param componentName
   * @return
   */
  Set<String> getHosts(String serviceName, String componentName);

  /**
   * Get specific host info using host name.
   *
   * @param hostName the host name
   * @return Host info {@link Host}
   */
  Host getHost(String hostName);


  /**
   * Adds schs to cluster AND persists them
   * TODO consider making persisting optional
   *
   * @param serviceComponentHosts
   * @throws AmbariException
   */
  Set<ServiceComponentHostResponse> addServiceComponentHosts(Collection<ServiceComponentHost> serviceComponentHosts) throws AmbariException;

  /**
   * Remove ServiceComponentHost from cluster
   *
   * @param svcCompHost
   */
  void removeServiceComponentHost(ServiceComponentHost svcCompHost)
    throws AmbariException;

  /**
   * Get desired stack version
   *
   * @return
   */
  StackId getDesiredStackVersion();

  /**
   * Set desired stack version
   *
   * @param stackVersion
   */
  void setDesiredStackVersion(StackId stackVersion) throws AmbariException;

  /**
   * Get current stack version
   *
   * @return
   */
  StackId getCurrentStackVersion();

  /**
   * Set current stack version
   *
   * @param stackVersion
   */
  void setCurrentStackVersion(StackId stackVersion) throws AmbariException;

  /**
   * Creates or updates host versions for all of the hosts within a cluster
   * based on state of cluster stack version. This is used to transition all
   * hosts into the correct state (which may not be
   * {@link RepositoryVersionState#INSTALLING}).
   * <p/>
   * Hosts that are in maintenance mode will be transitioned directly into
   * {@link RepositoryVersionState#OUT_OF_SYNC} instead. Hosts which do not need
   * the version distributed to them will move into the
   * {@link RepositoryVersionState#NOT_REQUIRED} state.
   *
   * @param repoVersionEntity    the repository that the hosts are being transitioned for (not
   *                             {@code null}).
   * @param versionDefinitionXml the VDF, or {@code null} if none.
   * @param forceInstalled       if {@code true}, then this will transition everything directly to
   *                             {@link RepositoryVersionState#INSTALLED} instead of
   *                             {@link RepositoryVersionState#INSTALLING}. Hosts which should
   *                             received other states (like
   *                             {@link RepositoryVersionState#NOT_REQUIRED} will continue to
   *                             receive those states.
   * @return a list of hosts which need the repository installed.
   * @throws AmbariException
   */
  List<Host> transitionHostsToInstalling(RepositoryVersionEntity repoVersionEntity,
                                         VersionDefinitionXml versionDefinitionXml, boolean forceInstalled) throws AmbariException;

  /**
   * Gets whether the cluster is still initializing or has finished with its
   * deployment requests.
   *
   * @return either {@link State#INIT} or {@link State#INSTALLED}, never
   * {@code null}.
   */
  State getProvisioningState();

  /**
   * Sets the provisioning state of the cluster.
   *
   * @param provisioningState the provisioning state, not {@code null}.
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
   *
   * @param configType the config type to return
   * @return a map of configuration objects that have been set for the given type
   */
  Map<String, Config> getConfigsByType(String configType);

  /**
   * Gets all configs that match the specified type.  Result is not the
   * DESIRED configuration for a cluster.
   *
   * @param configType the config type to return
   *        serviceId the serviceid for the config
   * @return a map of configuration objects that have been set for the given type
   */
  Map<String, Config> getConfigsByServiceIdType(String configType, Long serviceId);

  /**
   * Gets all properties types that mach the specified type.
   *
   * @param configType the config type to return
   * @return properties types for given config type
   */
  Map<PropertyInfo.PropertyType, Set<String>> getConfigPropertiesTypes(String configType);

  /**
   * Gets the specific config that matches the specified type and tag.  This not
   * necessarily a DESIRED configuration that applies to a cluster.
   *
   * @param configType the config type to find
   * @param versionTag the config version tag to find
   * @return a {@link Config} object, or <code>null</code> if the specific type
   * and version have not been set.
   */
  Config getConfig(String configType, String versionTag);

  /**
   * Gets the specific config that matches the specified type and tag.  This not
   * necessarily a DESIRED configuration that applies to a cluster.
   *
   * @param configType the config type to find
   * @param versionTag the config version tag to find
   * @param serviceId the service for the config
   * @return a {@link Config} object, or <code>null</code> if the specific type
   * and version have not been set.
   */
  Config getConfigByServiceId(String configType, String versionTag, Long serviceId);

  /**
   * Get latest (including inactive ones) configurations with any of the given types.
   * This method does not take into account the configuration being enabled.
   *
   * @return the list of configurations with the given types
   */
  List<Config> getLatestConfigsWithTypes(Collection<String> types);

  /**
   * Gets the specific config that matches the specified type and version.  This not
   * necessarily a DESIRED configuration that applies to a cluster.
   *
   * @param configType    the config type to find
   * @param configVersion the config version to find
   * @return a {@link Config} object, or <code>null</code> if the specific type
   * and version have not been set.
   */
  Config getConfigByVersion(String configType, Long configVersion);

  /**
   * Sets a specific config.  NOTE:  This is not a DESIRED configuration that
   * applies to a cluster.
   *
   * @param config the config instance to add
   */
  void addConfig(Config config);

  /**
   * Sets a specific config.  NOTE:  This is not a DESIRED configuration that
   * applies to a cluster.
   *
   * @param config the config instance to add
   *        serviceId service id for the config
   */
  void addConfig(Config config, Long serviceId);

  /**
   * Gets all configurations defined for a cluster.
   *
   * @return the collection of all configs that have been defined.
   */
  Collection<Config> getAllConfigs();

  /**
   * Gets all configurations defined for a cluster service.
   *
   * @return the collection of all configs that have been defined.
   */
  List<Config> getConfigsByServiceId(Long serviceId);

  /**
   * Adds and sets a DESIRED configuration to be applied to a cluster.  There
   * can be only one selected config per type.
   *
   * @param user    the user making the change for audit purposes
   * @param configs the set of {@link org.apache.ambari.server.state.Config} objects to set as desired
   * @return <code>true</code> if the config was added, or <code>false</code>
   * if the config is already set as the current
   */
  ServiceConfigVersionResponse addDesiredConfig(String user, Set<Config> configs) throws AmbariException;

  /**
   * Adds and sets a DESIRED configuration to be applied to a cluster.  There
   * can be only one selected config per type.
   *
   * @param user                     the user making the change for audit purposes
   * @param configs                  the set of {@link org.apache.ambari.server.state.Config} objects to set as desired
   * @param serviceConfigVersionNote note to attach to service config version if created
   * @return <code>true</code> if the config was added, or <code>false</code>
   * if the config is already set as the current
   */
  ServiceConfigVersionResponse addDesiredConfig(String user, Set<Config> configs, String serviceConfigVersionNote) throws AmbariException;

  ServiceConfigVersionResponse createServiceConfigVersion(Long serviceId, String user, String note,
                                                          ConfigGroup configGroup) throws AmbariException;

  Long getServiceForConfigTypes(Collection<String> configTypes);

  /**
   * Apply specified service config version (rollback)
   *
   * @param serviceId   service Id
   * @param version     service config version
   * @param user        the user making the change for audit purposes
   * @param note
   * @return service config version created
   * @throws AmbariException
   */
  ServiceConfigVersionResponse setServiceConfigVersion(Long serviceId, Long version, String user, String note) throws AmbariException;

  /**
   * Get currently active service config versions for stack services
   *
   * @return
   */
  Map<String, Collection<ServiceConfigVersionResponse>> getActiveServiceConfigVersions();

  /**
   * Get active service config version responses for all config groups of a service
   *
   * @param serviceId service ID
   * @return
   */
  List<ServiceConfigVersionResponse> getActiveServiceConfigVersionResponse(Long serviceId);

  /**
   * Get service config version history
   *
   * @return
   */
  List<ServiceConfigVersionResponse> getServiceConfigVersions();

  /**
   * Gets the desired (and selected) config by type.
   *
   * @param configType the type of configuration
   * @return the {@link Config} instance, or <code>null</code> if the type has
   * not been set.
   */
  Config getDesiredConfigByType(String configType);

  /**
   * Check if config type exists in cluster.
   *
   * @param configType the type of configuration
   * @return <code>true</code> if config type exists, else - <code>false</code>
   */
  boolean isConfigTypeExists(String configType);

  /**
   * Gets the active desired configurations for the cluster.
   *
   * @return a map of type-to-configuration information.
   */
  Map<String, DesiredConfig> getDesiredConfigs();

  /**
   * Gets all versions of the desired configurations for the cluster.
   *
   * @return a map of type-to-configuration information.
   */
  Map<String, Set<DesiredConfig>> getAllDesiredConfigVersions();


  /**
   * Creates a cluster response based on the current cluster definition
   *
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
   *
   * @param sb
   */
  void debugDump(StringBuilder sb);

  /**
   * Delete all the services associated with this cluster
   *
   * @throws AmbariException
   */
  void deleteAllServices() throws AmbariException;

  /**
   * Delete the named service associated with this cluster
   *
   * @param serviceName
   * @throws AmbariException
   */
  void deleteService(String serviceName) throws AmbariException;

  /**
   * Delete all the service groups associated with this cluster
   *
   * @throws AmbariException
   */
  void deleteAllServiceGroups() throws AmbariException;

  /**
   * Delete the named service associated with this cluster
   *
   * @param serviceGroupName
   * @throws AmbariException
   */
  void deleteServiceGroup(String serviceGroupName) throws AmbariException;

  /**
   * Delete service group dependency from the service group
   *
   * @param serviceGroupName
   * @param dependencyServiceGroupId
   * @throws AmbariException
   */
  void deleteServiceGroupDependency(String serviceGroupName, Long dependencyServiceGroupId) throws AmbariException;

  /**
   * Get all service groups
   *
   * @return map of service group ids as keys and service group objects as values.
   */
  Map<Long, ServiceGroup> getServiceGroupsById();

  /**
   * Delete all the cluster settings associated with this cluster
   *
   * @throws AmbariException
   */
  void deleteAllClusterSettings() throws AmbariException;

  /**
   * Delete the named cluster setting associated with this cluster
   *
   * @param clusterSettingName
   * @throws AmbariException
   */
  void deleteClusterSetting(String clusterSettingName) throws AmbariException;

  /**
   * Gets if the cluster can be deleted
   *
   * @return
   */
  boolean canBeRemoved();

  /**
   * Delete the cluster
   *
   * @throws AmbariException
   */
  void delete() throws AmbariException;

  /**
   * Fetch desired configs for list of hosts in cluster
   *
   * @param hostIds
   * @return
   */
  Map<Long, Map<String, DesiredConfig>> getHostsDesiredConfigs(Collection<Long> hostIds);

  /**
   * Fetch desired configs for all hosts in cluster
   *
   * @return
   */
  Map<Long, Map<String, DesiredConfig>> getAllHostsDesiredConfigs();

  /**
   * Add a new config group to the set of Config groups associated with this
   * cluster
   *
   * @param configGroup
   * @throws AmbariException
   */
  void addConfigGroup(ConfigGroup configGroup) throws AmbariException;

  /**
   * Get config groups associated with this cluster
   *
   * @return unmodifiable map of config group id to config group.  Will not return null.
   */
  Map<Long, ConfigGroup> getConfigGroups();

  /**
   * Delete this config group identified by the config group id
   *
   * @param id
   * @throws AmbariException
   */
  void deleteConfigGroup(Long id) throws AmbariException, AuthorizationException;

  /**
   * Find all config groups associated with the give hostname
   *
   * @param hostname
   * @return Map of config group id to config group
   */
  Map<Long, ConfigGroup> getConfigGroupsByHostname(String hostname)
    throws AmbariException;

  /**
   * Add a @RequestExecution to the cluster
   *
   * @param requestExecution
   * @throws AmbariException
   */
  void addRequestExecution(RequestExecution requestExecution)
    throws AmbariException;

  /**
   * Get all @RequestExecution objects associated with the cluster
   *
   * @return
   */
  Map<Long, RequestExecution> getAllRequestExecutions();

  /**
   * Delete a @RequestExecution associated with the cluster
   *
   * @param id
   * @throws AmbariException
   */
  void deleteRequestExecution(Long id) throws AmbariException;

  /**
   * Get next version of specified config type
   *
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
   * @param privilegeEntity the privilege
   * @param readOnly        indicate whether or not this check is for a read only operation
   * @return true if the access to this cluster is allowed
   */
  boolean checkPermission(PrivilegeEntity privilegeEntity, boolean readOnly);

  /**
   * Add the given map of attributes to the session for this cluster.
   *
   * @param attributes the session attributes
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
   * Makes the most recent configurations for the specified stack current. This
   * will only modify configurations for the given service.
   * <p/>
   * When completed, all other configurations for any other stack will remain,
   * but will not be marked as selected.
   *
   * @param stackId     the stack to use when finding the latest configurations (not
   *                    {@code null}).
   * @param serviceId the service to modify configurations for (not {@code null}).
   */
  void applyLatestConfigurations(StackId stackId, Long serviceId);

  /**
   * Removes all configurations for the specified service and stack.
   *
   * @param stackId     the stack to use when finding the configurations to remove (not
   *                    {@code null}).
   * @param serviceId the service to remove configurations for (not {@code null}).
   */
  void removeConfigurations(StackId stackId, Long serviceId);

  /**
   * Returns whether this cluster was provisioned by a Blueprint or not.
   *
   * @return true if the cluster was deployed with a Blueprint otherwise false.
   */
  boolean isBluePrintDeployed();

  /**
   * Gets an {@link UpgradeEntity} if there is an upgrade in progress or an
   * upgrade that has been suspended. This will return the associated
   * {@link UpgradeEntity} if it exists.
   *
   * @return an upgrade which will either be in progress or suspended, or
   * {@code null} if none.
   */
  UpgradeEntity getUpgradeInProgress();

  /**
   * Sets or clears the associated upgrade with the cluster.
   *
   * @param upgradeEntity the upgrade entity to set for cluster, or {@code null} for none.
   * @throws AmbariException
   */
  void setUpgradeEntity(UpgradeEntity upgradeEntity) throws AmbariException;

  /**
   * Gets whether there is an upgrade which has been suspended and not yet
   * finalized.
   *
   * @return {@code true} if the last upgrade is suspended
   */
  boolean isUpgradeSuspended();

  /**
   * Returns the service that the passed config type belongs to.
   *
   * @param configType the config type to look up the service by
   * @return returns the service that the config type belongs to if
   * there is any otherwise returns null.
   */
  Service getServiceByConfigType(String configType);

  /**
   * Gets the most recent value of {@code cluster-env/propertyName} where
   * {@code propertyName} is the paramter specified to the method. This will use
   * the desired configuration for {@code cluster-env}.
   * <p/>
   * The value is cached on this {@link Cluster} instance, so subsequent calls
   * will not inclur a lookup penalty. This class also responds to
   * {@link ClusterConfigChangedEvent} in order to clear the cache.
   *
   * @param propertyName the property to lookup in {@code cluster-env} (not {@code null}).
   * @param defaultValue a default value to cache return if none exists (may be
   *                     {@code null}).
   * @return
   */
  String getClusterProperty(String propertyName, String defaultValue);

  /**
   * Returns the number of hosts that form the cluster.
   *
   * @return number of hosts that form the cluster
   */
  int getClusterSize();

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
   * @param commandParams the command parameter map to supplement (not {@code null}).
   * @param roleParams    the role parameter map to supplement (not {@code null}).
   */
  void addSuspendedUpgradeParameters(Map<String, String> commandParams,
                                     Map<String, String> roleParams);

  /**
   * Gets a mapping of service to component/version for every installed
   * component in the cluster which advertises a version and for which the
   * repository has been resolved.
   *
   * @return a mapping of service to component version, or an empty map.
   */
  Map<String, Map<String, String>> getComponentVersionMap();
}
