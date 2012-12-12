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


public class ActionResourceDefinition extends BaseResourceDefinition {

  private String m_clusterName;
  private String m_serviceName;

  public ActionResourceDefinition(String id, String clusterName, String serviceName) {
    super(Resource.Type.Action, id);
    m_clusterName = clusterName;
    m_serviceName = serviceName;
    setResourceId(Resource.Type.Cluster, m_clusterName);
    setResourceId(Resource.Type.Service, m_serviceName);
    getQuery().addProperty(getClusterController().getSchema(
        Resource.Type.Action).getKeyPropertyId(Resource.Type.Action));
  }
  
  @Override
  public String getPluralName() {
    return "actions";
  }

  @Override
  public String getSingularName() {
    return "action";
  }

  @Override
  public Map<String, ResourceDefinition> getSubResources() {
    return new HashMap<String, ResourceDefinition>();
  }
}
