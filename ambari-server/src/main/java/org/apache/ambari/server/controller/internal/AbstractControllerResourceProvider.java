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

package org.apache.ambari.server.controller.internal;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.state.Cluster;

/**
 * Abstract resource provider implementation that maps to an Ambari management controller.
 */
public abstract class AbstractControllerResourceProvider extends AbstractAuthorizedResourceProvider {

  private static ResourceProviderFactory resourceProviderFactory;
  /**
   * The management controller to delegate to.
   */
  private final AmbariManagementController managementController;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds          the property ids
   * @param keyPropertyIds       the key property ids
   * @param managementController the management controller
   */
  protected AbstractControllerResourceProvider(Set<String> propertyIds,
                                               Map<Resource.Type, String> keyPropertyIds,
                                               AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds);
    this.managementController = managementController;
  }

  public static void init(ResourceProviderFactory factory) {
    resourceProviderFactory = factory;
  }


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the associated management controller.
   *
   * @return the associated management controller
   */
  protected AmbariManagementController getManagementController() {
    return managementController;
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Gets the resource id for the named cluster
   *
   * @param clusterName the name of the relevant cluster
   * @return the resource id or null if not found
   * @throws AmbariException if the named cluster does not exist
   */
  protected Long getClusterId(String clusterName) throws AmbariException {
    Cluster cluster = (clusterName == null) ? null : managementController.getClusters().getCluster(clusterName);
    return (cluster == null) ? null : cluster.getClusterId();
  }

  /**
   * Gets the resource id for the named cluster
   *
   * @param clusterName the name of the relevant cluster
   * @return the resource id or null if not found
   * @throws AmbariException if the named cluster does not exist
   */
  protected Long getClusterResourceId(String clusterName) throws AmbariException {
    Cluster cluster = (clusterName == null) ? null : managementController.getClusters().getCluster(clusterName);
    return (cluster == null) ? null : cluster.getResourceId();
  }

  /**
   * Gets the resource id for the cluster with the specified id
   *
   * @param clusterId the id of the relevant cluster
   * @return the resource id or null if not found
   * @throws AmbariException if the cluster does not exist
   */
  protected Long getClusterResourceId(Long clusterId) throws AmbariException {
    Cluster cluster = (clusterId == null) ? null : managementController.getClusters().getClusterById(clusterId);
    return (cluster == null) ? null : cluster.getResourceId();
  }

  /**
   * Factory method for obtaining a resource provider based on a given type and management controller.
   *
   * @param type                  the resource type
   * @param propertyIds           the property ids
   * @param managementController  the management controller
   *
   * @return a new resource provider
   */
  public static ResourceProvider getResourceProvider(Resource.Type type,
                                                     Set<String> propertyIds,
                                                     Map<Resource.Type, String> keyPropertyIds,
                                                     AmbariManagementController managementController) {

    switch (type.getInternalType()) {
      case Cluster:
        return new ClusterResourceProvider(managementController);
      case Service:
        return resourceProviderFactory.getServiceResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Component:
        return resourceProviderFactory.getComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Host:
        return resourceProviderFactory.getHostResourceProvider(propertyIds, keyPropertyIds, managementController);
      case HostComponent:
        return resourceProviderFactory.getHostComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Configuration:
        return new ConfigurationResourceProvider(managementController);
      case ServiceConfigVersion:
        return new ServiceConfigVersionResourceProvider(managementController);
      case Action:
        return new ActionResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Request:
        return new RequestResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Task:
        return new TaskResourceProvider(propertyIds, keyPropertyIds, managementController);
      case User:
        return new UserResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Group:
        return new GroupResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Member:
        return resourceProviderFactory.getMemberResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Upgrade:
        return resourceProviderFactory.getUpgradeResourceProvider(managementController);
      case Stack:
        return new StackResourceProvider(propertyIds, keyPropertyIds, managementController);
      case StackVersion:
        return new StackVersionResourceProvider(propertyIds, keyPropertyIds, managementController);
      case ClusterStackVersion:
        return new ClusterStackVersionResourceProvider(managementController);
      case HostStackVersion:
        return new HostStackVersionResourceProvider(managementController);
      case StackService:
        return new StackServiceResourceProvider(propertyIds, keyPropertyIds, managementController);
      case StackServiceComponent:
        return new StackServiceComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case StackConfiguration:
        return new StackConfigurationResourceProvider(propertyIds, keyPropertyIds, managementController);
      case StackConfigurationDependency:
        return new StackConfigurationDependencyResourceProvider(propertyIds, keyPropertyIds, managementController);
      case StackLevelConfiguration:
        return new StackLevelConfigurationResourceProvider(propertyIds, keyPropertyIds, managementController);
      case ExtensionLink:
          return new ExtensionLinkResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Extension:
        return new ExtensionResourceProvider(propertyIds, keyPropertyIds, managementController);
      case ExtensionVersion:
        return new ExtensionVersionResourceProvider(propertyIds, keyPropertyIds, managementController);
      case RootService:
        return new RootServiceResourceProvider(propertyIds, keyPropertyIds, managementController);
      case RootServiceComponent:
        return new RootServiceComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case RootServiceHostComponent:
        return new RootServiceHostComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case ConfigGroup:
        return new ConfigGroupResourceProvider(propertyIds, keyPropertyIds, managementController);
      case RequestSchedule:
        return new RequestScheduleResourceProvider(propertyIds, keyPropertyIds, managementController);
      case HostComponentProcess:
        return new HostComponentProcessResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Blueprint:
        return new BlueprintResourceProvider(propertyIds, keyPropertyIds, managementController);
      case KerberosDescriptor:
        return resourceProviderFactory.getKerberosDescriptorResourceProvider(managementController, propertyIds, keyPropertyIds);
      case Recommendation:
        return new RecommendationResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Validation:
        return new ValidationResourceProvider(propertyIds, keyPropertyIds, managementController);
      case ClientConfig:
        return new ClientConfigResourceProvider(propertyIds, keyPropertyIds, managementController);
      case RepositoryVersion:
        return resourceProviderFactory.getRepositoryVersionResourceProvider();
      case CompatibleRepositoryVersion:
        return new CompatibleRepositoryVersionResourceProvider(managementController);
      case StackArtifact:
        return new StackArtifactResourceProvider(managementController);
      case Theme:
        return new ThemeArtifactResourceProvider(managementController);
      case QuickLink:
        return new QuickLinkArtifactResourceProvider(managementController);
      case ActiveWidgetLayout:
        return new ActiveWidgetLayoutResourceProvider(managementController);
      case WidgetLayout:
        return new WidgetLayoutResourceProvider(managementController);
      case Widget:
        return new WidgetResourceProvider(managementController);
      case HostKerberosIdentity:
        return resourceProviderFactory.getHostKerberosIdentityResourceProvider(managementController);
      case Credential:
        return resourceProviderFactory.getCredentialResourceProvider(managementController);
      case RoleAuthorization:
        return new RoleAuthorizationResourceProvider(managementController);
      case UserAuthorization:
        return new UserAuthorizationResourceProvider(managementController);
      case VersionDefinition:
        return new VersionDefinitionResourceProvider();
      case ClusterKerberosDescriptor:
        return new ClusterKerberosDescriptorResourceProvider(managementController);
      case LoggingQuery:
        return new LoggingResourceProvider(propertyIds, keyPropertyIds, managementController);
      default:
        throw new IllegalArgumentException("Unknown type " + type);
    }
  }

  /**
   * Obtain a resource provider based on type.
   *
   * @param type  resource provider type
   *
   * @return resource provider for the specified type
   */
  ResourceProvider getResourceProvider(Resource.Type type) {
    return ((ClusterControllerImpl) ClusterControllerHelper.getClusterController()).
        ensureResourceProvider(type);
  }

}
