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

import org.apache.ambari.api.services.formatters.ClusterInstanceFormatter;
import org.apache.ambari.api.services.formatters.CollectionFormatter;
import org.apache.ambari.api.services.formatters.ResultFormatter;
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Resource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ClusterResourceDefinition extends BaseResourceDefinition {

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
  public Set<ResourceDefinition> getChildren() {
    Set<ResourceDefinition> setChildren = new HashSet<ResourceDefinition>();

    ServiceResourceDefinition serviceResource = new ServiceResourceDefinition(null, getId());
    PropertyId serviceIdProperty = getClusterController().getSchema(
        Resource.Type.Service).getKeyPropertyId(Resource.Type.Service);
    serviceResource.getQuery().addProperty(serviceIdProperty);
    setChildren.add(serviceResource);

    HostResourceDefinition hostResource = new HostResourceDefinition(null, getId());
    PropertyId hostIdProperty = getClusterController().getSchema(
        Resource.Type.Host).getKeyPropertyId(Resource.Type.Host);
    hostResource.getQuery().addProperty(hostIdProperty);
    setChildren.add(hostResource);

    return setChildren;
  }

  @Override
  public Set<ResourceDefinition> getRelations() {
    return Collections.emptySet();
  }

  @Override
  public ResultFormatter getResultFormatter() {
    //todo: instance formatter
    return getId() == null ? new CollectionFormatter(this) : new ClusterInstanceFormatter(this);
  }
}
