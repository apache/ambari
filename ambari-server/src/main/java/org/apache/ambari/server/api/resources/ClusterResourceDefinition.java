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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Cluster resource definition.
 */
public class ClusterResourceDefinition extends BaseResourceDefinition {

  /**
   * Constructor.
   *
   * @param id value of primary key
   */
  public ClusterResourceDefinition(String id) {
    super(Resource.Type.Cluster, id);

    if (id == null) {
      getQuery().addProperty(getClusterController().getSchema(
          Resource.Type.Cluster).getKeyPropertyId(Resource.Type.Cluster));
    }
  }

  @Override
  public String getPluralName() {
    return "clusters";
  }

  @Override
  public String getSingularName() {
    return "cluster";
  }

  @Override
  public Map<String, ResourceDefinition> getSubResources() {
    Map<String, ResourceDefinition> mapChildren = new HashMap<String, ResourceDefinition>();

    ServiceResourceDefinition serviceResource = new ServiceResourceDefinition(null, getId());
    serviceResource.getQuery().addProperty(getClusterController().getSchema(
        Resource.Type.Service).getKeyPropertyId(Resource.Type.Service));
    mapChildren.put(serviceResource.getPluralName(), serviceResource);

    HostResourceDefinition hostResource = new HostResourceDefinition(null, getId());
    hostResource.getQuery().addProperty(getClusterController().getSchema(
        Resource.Type.Host).getKeyPropertyId(Resource.Type.Host));
    mapChildren.put(hostResource.getPluralName(), hostResource);
    
    ConfigurationResourceDefinition configResource = new ConfigurationResourceDefinition(null, null, getId());
    configResource.getQuery().addProperty(getClusterController().getSchema(
        Resource.Type.Configuration).getKeyPropertyId(Resource.Type.Configuration));
    mapChildren.put(configResource.getPluralName(), configResource);

    RequestResourceDefinition requestResource = new RequestResourceDefinition(null, getId());
    requestResource.getQuery().addProperty(getClusterController().getSchema(
        Resource.Type.Request).getKeyPropertyId(Resource.Type.Request));
    mapChildren.put(requestResource.getPluralName(), requestResource);

    return mapChildren;
  }
}
