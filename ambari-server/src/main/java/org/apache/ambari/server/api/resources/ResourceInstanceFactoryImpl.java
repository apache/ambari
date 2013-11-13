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

import org.apache.ambari.server.api.query.QueryImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;

import java.util.Map;

/**
 * Factory for creating resource instances.
 */
public class ResourceInstanceFactoryImpl implements ResourceInstanceFactory {

  @Override
  public ResourceInstance createResource(Resource.Type type, Map<Resource.Type, String> mapIds) {

    /**
     * The resource definition for the specified type.
     */
    ResourceDefinition resourceDefinition = getResourceDefinition(type, mapIds);

    return new QueryImpl(mapIds, resourceDefinition, ClusterControllerHelper.getClusterController());
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

    //todo: consider ResourceDependencyManager : Map<Resource.Type, ResourceDefinition>
    switch (type) {
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

      case Task:
        resourceDefinition = new TaskResourceDefinition();
        break;

      case User:
        resourceDefinition = new UserResourceDefinition();
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

      case StackService:
        resourceDefinition = new StackServiceResourceDefinition();
        break;

      case StackServiceComponent:
        resourceDefinition = new StackServiceComponentResourceDefinition();
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

      default:
        throw new IllegalArgumentException("Unsupported resource type: " + type);
    }
    return resourceDefinition;
  }
}
