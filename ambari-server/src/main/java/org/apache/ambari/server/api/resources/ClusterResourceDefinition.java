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

import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;

import java.util.*;

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
    PropertyId serviceIdProperty = getClusterController().getSchema(
        Resource.Type.Service).getKeyPropertyId(Resource.Type.Service);
    serviceResource.getQuery().addProperty(serviceIdProperty);
    mapChildren.put(serviceResource.getPluralName(), serviceResource);

    HostResourceDefinition hostResource = new HostResourceDefinition(null, getId());
    PropertyId hostIdProperty = getClusterController().getSchema(
        Resource.Type.Host).getKeyPropertyId(Resource.Type.Host);
    hostResource.getQuery().addProperty(hostIdProperty);
    mapChildren.put(hostResource.getPluralName(), hostResource);

    return mapChildren;
  }
}
