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
import org.apache.ambari.api.services.formatters.HostInstanceFormatter;
import org.apache.ambari.api.services.formatters.ResultFormatter;
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Resource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class HostResourceDefinition extends BaseResourceDefinition {

  private String m_clusterId;

  @Override
  public String getPluralName() {
    return "hosts";
  }

  @Override
  public String getSingularName() {
    return "host";
  }

  public HostResourceDefinition(String id, String clusterId) {
    super(Resource.Type.Host, id);
    m_clusterId = clusterId;
    setResourceId(Resource.Type.Cluster, m_clusterId);
  }

  @Override
  public Set<ResourceDefinition> getChildren() {
    Set<ResourceDefinition> setChildren = new HashSet<ResourceDefinition>();

    HostComponentResourceDefinition hostComponentResource = new HostComponentResourceDefinition(
        null, m_clusterId, getId());
    PropertyId hostComponentIdProperty = getClusterController().getSchema(
        Resource.Type.HostComponent).getKeyPropertyId(Resource.Type.HostComponent);
    hostComponentResource.getQuery().addProperty(hostComponentIdProperty);
    setChildren.add(hostComponentResource);
    return setChildren;
  }

  @Override
  public Set<ResourceDefinition> getRelations() {
    return Collections.emptySet();
  }

  @Override
  public ResultFormatter getResultFormatter() {
    //todo: instance formatter
    return getId() == null ? new CollectionFormatter(this) : new HostInstanceFormatter(this);
  }
}
