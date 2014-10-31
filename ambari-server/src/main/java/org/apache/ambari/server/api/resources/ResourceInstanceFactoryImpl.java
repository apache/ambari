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


package org.apache.ambari.server.api.resources;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.query.QueryImpl;
import org.apache.ambari.server.controller.internal.ControllerType;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.view.ViewRegistry;

/**
 * Factory for creating resource instances.
 */
public class ResourceInstanceFactoryImpl implements ResourceInstanceFactory {


  /**
   * Map of external resource definitions (added through views).
   */
  private final static Map<Resource.Type, ResourceDefinition> resourceDefinitions =
      new HashMap<Resource.Type, ResourceDefinition>();


  @Override
  public ResourceInstance createResource(Resource.Type type, Map<Resource.Type, String> mapIds) {

    /**
     * The resource definition for the specified type.
     */
    ResourceDefinition resourceDefinition = getResourceDefinition(type, mapIds);

    return new QueryImpl(mapIds, resourceDefinition, ClusterControllerHelper.getClusterController());
  }

  /**
   * Associate an external resource definition with a type.
   *
   * @param type        the resource type
   * @param definition  the resource definition
   */
  public static void addResourceDefinition(Resource.Type type, ResourceDefinition definition) {
    resourceDefinitions.put(type, definition);
  }

  /**
   * Get a resource definition for the given type.
   *
   * @param type    the resource type
   * @param mapIds  the map of ids
   *
   * @return the resource definition
   */
  public static ResourceDefinition getResourceDefinition(Resource.Type type, Map<Resource.Type, String> mapIds) {
    ResourceDefinition resourceDefinition;

    // Check to see if there is an external resource definition registered for the given type.
    if (resourceDefinitions.containsKey(type)) {
      return resourceDefinitions.get(type);
    }

    //todo: consider ResourceDependencyManager : Map<Resource.Type, ResourceDefinition>
    switch (type.getInternalType()) {
      case Cluster:
        resourceDefinition = new ClusterResourceDefinition();
        break;

      case Service:
        resourceDefinition = new ServiceResourceDefinition();
        break;

      case Host:
        resourceDefinition = mapIds.containsKey(Resource.Type.Cluster) ?
            new HostResourceDefinition() : new DetachedHostResourceDefinition();
        break;

      case Component:
        resourceDefinition = new ComponentResourceDefinition();
        break;

      case HostComponent:
        resourceDefinition = new HostComponentResourceDefinition();
        break;

      case Action:
        resourceDefinition = new ActionResourceDefinition();
        break;

      case Configuration:
        resourceDefinition = new ConfigurationResourceDefinition();
        break;

      case ServiceConfigVersion:
        resourceDefinition = new ServiceConfigVersionResourceDefinition();
        break;

      case Task:
        resourceDefinition = new TaskResourceDefinition();
        break;

      case User:
        resourceDefinition = new UserResourceDefinition();
        break;

      case Group:
        resourceDefinition = new GroupResourceDefinition();
        break;

      case Member:
        resourceDefinition = new MemberResourceDefinition();
        break;

      case Request:
        resourceDefinition = new RequestResourceDefinition();
        break;

      case Stack:
        resourceDefinition = new StackResourceDefinition();
        break;

      case StackVersion:
        resourceDefinition = new StackVersionResourceDefinition();
        break;

      case StackLevelConfiguration:
        resourceDefinition = new StackLevelConfigurationResourceDefinition();
        break;

      case StackService:
        resourceDefinition = new StackServiceResourceDefinition();
        break;

      case StackServiceComponent:
        resourceDefinition = new StackServiceComponentResourceDefinition();
        break;

      case StackServiceComponentDependency:
        resourceDefinition = new StackDependencyResourceDefinition();
        break;

      case StackConfiguration:
        resourceDefinition = new StackConfigurationResourceDefinition();
        break;

      case OperatingSystem:
        resourceDefinition = new OperatingSystemResourceDefinition();
        break;

      case Repository:
        resourceDefinition = new RepositoryResourceDefinition();
        break;

      case DRFeed:
        resourceDefinition = new FeedResourceDefinition();
        break;

      case DRTargetCluster:
        resourceDefinition = new TargetClusterResourceDefinition();
        break;

      case DRInstance:
        resourceDefinition = new InstanceResourceDefinition();
        break;

      case Workflow:
        resourceDefinition = new WorkflowResourceDefinition();
        break;

      case Job:
        resourceDefinition = new JobResourceDefinition();
        break;

      case TaskAttempt:
        resourceDefinition = new TaskAttemptResourceDefinition();
        break;

      case RootService:
        resourceDefinition = new RootServiceResourceDefinition();
        break;

      case RootServiceComponent:
        resourceDefinition = new RootServiceComponentResourceDefinition();
        break;

      case RootServiceHostComponent:
        resourceDefinition = new RootServiceHostComponentResourceDefinition();
        break;

      case ConfigGroup:
        resourceDefinition = new ConfigGroupResourceDefinition();
        break;

      case RequestSchedule:
        resourceDefinition = new RequestScheduleResourceDefinition();
        break;

      case View:
        resourceDefinition = new ViewResourceDefinition();
        break;

      case ViewVersion:
        resourceDefinition = new ViewVersionResourceDefinition();
        break;

      case ViewInstance:
        String viewName = mapIds.get(Resource.Type.View);
        String version  = mapIds.get(Resource.Type.ViewVersion);

        Set<SubResourceDefinition> subResourceDefinitions = (viewName == null || version == null)  ?
            Collections.<SubResourceDefinition>emptySet() :
            ViewRegistry.getInstance().getSubResourceDefinitions(viewName, version);

        resourceDefinition = new ViewInstanceResourceDefinition(subResourceDefinitions);
        break;

      case Blueprint:
        resourceDefinition = new BlueprintResourceDefinition();
        break;

      case Recommendation:
        resourceDefinition = new RecommendationResourceDefinition();
        break;

      case Validation:
        resourceDefinition = new ValidationResourceDefinition();
        break;

      case HostComponentProcess:
        resourceDefinition = new HostComponentProcessResourceDefinition();
        break;

      case Permission:
        resourceDefinition = new PermissionResourceDefinition();
        break;

      case AlertDefinition:
        resourceDefinition = new AlertDefResourceDefinition();
        break;

      case AmbariPrivilege:
        resourceDefinition = new PrivilegeResourceDefinition(Resource.Type.AmbariPrivilege);
        break;

      case ClusterPrivilege:
        resourceDefinition = new PrivilegeResourceDefinition(Resource.Type.ClusterPrivilege);
        break;

      case ViewPrivilege:
        resourceDefinition = new PrivilegeResourceDefinition(Resource.Type.ViewPrivilege);
        break;

      case UserPrivilege:
        resourceDefinition = new PrivilegeResourceDefinition(Resource.Type.UserPrivilege);
        break;

      case ViewPermission:
        resourceDefinition = new ViewPermissionResourceDefinition();
        break;

      case Controller:
        resourceDefinition = new ControllerResourceDefinition(
            ControllerType.getByName(mapIds.get(Resource.Type.Controller)));
        break;

      case ClientConfig:
        resourceDefinition = new ClientConfigResourceDefinition();
        break;

      case LdapSyncEvent:
        resourceDefinition = new LdapSyncEventResourceDefinition();
        break;

      default:
        throw new IllegalArgumentException("Unsupported resource type: " + type);
    }
    return resourceDefinition;
  }
}
