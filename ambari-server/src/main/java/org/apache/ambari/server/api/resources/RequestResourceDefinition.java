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

import java.util.HashMap;
import java.util.Map;


/**
 * Request resource definition.
 */
public class RequestResourceDefinition extends BaseResourceDefinition {

  /**
   * value of cluster id foreign key
   */
  private String m_clusterId;


  /**
   * Constructor.
   *
   * @param id         operation id value
   * @param clusterId  cluster id value
   */
  public RequestResourceDefinition(String id, String clusterId) {
    super(Resource.Type.Request, id);
    m_clusterId = clusterId;
    setResourceId(Resource.Type.Cluster, m_clusterId);
  }

  @Override
  public String getPluralName() {
    return "requests";
  }

  @Override
  public String getSingularName() {
    return "request";
  }

  @Override
  public Map<String, ResourceDefinition> getSubResources() {
    Map<String, ResourceDefinition> mapChildren = new HashMap<String, ResourceDefinition>();

    TaskResourceDefinition taskResourceDefinition =
        new TaskResourceDefinition(null, m_clusterId, getId());
    taskResourceDefinition.getQuery().addProperty(getClusterController().getSchema(
        Resource.Type.Task).getKeyPropertyId(Resource.Type.Task));
    mapChildren.put(taskResourceDefinition.getPluralName(), taskResourceDefinition);

    return mapChildren;
  }
}
