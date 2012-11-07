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
import org.apache.ambari.server.controller.utilities.PropertyHelper;


/**
 * Host resource definition.
 */
public class HostResourceDefinition extends BaseResourceDefinition {

  /**
   * value of cluster id foreign key
   */
  private String m_clusterId;

  /**
   * Whether the host resource is associated with a cluster.
   */
  private boolean m_attached;

  /**
   * Constructor.
   *
   * @param id        host id value
   * @param clusterId cluster id value
   * @param attached
   */
  public HostResourceDefinition(String id, String clusterId, boolean attached) {
    super(Resource.Type.Host, id);
    m_attached = attached;
    m_clusterId = clusterId;
    setResourceId(Resource.Type.Cluster, m_clusterId);
    
    if (null != clusterId) {
      getQuery().addProperty(PropertyHelper.getPropertyId("cluster_name", "Hosts"));      
    }
    
    if (null == id) {
      getQuery().addProperty(getClusterController().getSchema(
          Resource.Type.Host).getKeyPropertyId(Resource.Type.Host));
    } else {
      getQuery().addProperty(null, "*", null);
    }
  }

  @Override
  public String getPluralName() {
    return "hosts";
  }

  @Override
  public String getSingularName() {
    return "host";
  }

  @Override
  public Map<String, ResourceDefinition> getSubResources() {
    Map<String, ResourceDefinition> mapChildren = new HashMap<String, ResourceDefinition>();

    if (m_attached) {
      HostComponentResourceDefinition hostComponentResource =
          new HostComponentResourceDefinition(null, m_clusterId, getId());
      hostComponentResource.getQuery().addProperty(getClusterController().getSchema(
          Resource.Type.HostComponent).getKeyPropertyId(Resource.Type.HostComponent));
      mapChildren.put(hostComponentResource.getPluralName(), hostComponentResource);
    }

    return mapChildren;
  }
}
