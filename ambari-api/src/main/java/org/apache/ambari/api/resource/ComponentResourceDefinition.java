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

package org.apache.ambari.api.resource;

import org.apache.ambari.api.services.formatters.CollectionFormatter;
import org.apache.ambari.api.services.formatters.ComponentInstanceFormatter;
import org.apache.ambari.api.services.formatters.ResultFormatter;
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Resource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ComponentResourceDefinition extends BaseResourceDefinition {

  private String m_clusterId;
  private String m_serviceId;

  @Override
  public String getPluralName() {
    return "components";
  }

  @Override
  public String getSingularName() {
    return "component";
  }

  public ComponentResourceDefinition(String id, String clusterId, String serviceId) {
    super(Resource.Type.Component, id);
    m_clusterId = clusterId;
    m_serviceId = serviceId;
    setResourceId(Resource.Type.Cluster, m_clusterId);
    setResourceId(Resource.Type.Service, m_serviceId);
  }

  @Override
  public Set<ResourceDefinition> getChildren() {
    return Collections.emptySet();
  }

  @Override
  public Set<ResourceDefinition> getRelations() {
    Set<ResourceDefinition> setResourceDefinitions = new HashSet<ResourceDefinition>();
    // for host_component collection need host id property
    HostComponentResourceDefinition hostComponentResource = new HostComponentResourceDefinition(
        getId(), m_clusterId, null);
    PropertyId hostIdProperty = getClusterController().getSchema(
        Resource.Type.HostComponent).getKeyPropertyId(Resource.Type.Host);
    hostComponentResource.getQuery().addProperty(hostIdProperty);
    setResourceDefinitions.add(hostComponentResource);
    return setResourceDefinitions;
  }

  @Override
  public ResultFormatter getResultFormatter() {
    //todo: instance formatter
    return getId() == null ? new CollectionFormatter(this) : new ComponentInstanceFormatter(this);
  }
}
