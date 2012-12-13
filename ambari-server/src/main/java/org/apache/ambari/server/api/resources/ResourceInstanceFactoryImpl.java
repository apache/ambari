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

import org.apache.ambari.server.controller.spi.Resource;

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
        resourceDefinition = new  ComponentResourceDefinition();
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

      default:
        throw new IllegalArgumentException("Unsupported resource type: " + type);
    }

    return new ResourceInstanceImpl(mapIds, resourceDefinition, this);
  }
}
