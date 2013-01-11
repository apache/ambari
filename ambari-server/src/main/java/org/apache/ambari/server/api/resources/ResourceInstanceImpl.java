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

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.query.QueryImpl;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resource instance which contains request specific state.
 */
public class ResourceInstanceImpl implements ResourceInstance {

  /**
   * Query associated with the resource definition.
   */
  private Query m_query;

  /**
   * Map of primary and foreign keys and values necessary to identify the resource.
   */
  private Map<Resource.Type, String> m_mapResourceIds = new HashMap<Resource.Type, String>();

  /**
   * Definition for the resource type.  The definition contains all information specific to the
   * resource type.
   */
  private ResourceDefinition m_resourceDefinition;

  /**
   * Sub-resource instances of this resource.
   * Map of resource resource name to resource instance.
   */
  private Map<String, ResourceInstance> m_mapSubResources;

  /**
   * Factory for creating resource instances.
   * Used to create sub-resource instances.
   */
  private ResourceInstanceFactory m_resourceFactory;

  /**
   * Cluster controller reference.
   */
  //todo: should be injected.
  private ClusterController m_controller = ClusterControllerHelper.getClusterController();


  public ResourceInstanceImpl(Map<Resource.Type, String> mapIds, ResourceDefinition resourceDefinition,
                              ResourceInstanceFactory resourceFactory) {

    m_resourceDefinition = resourceDefinition;
    m_query              = new QueryImpl(this);
    m_resourceFactory    = resourceFactory;

    setIds(mapIds);
  }

  @Override
  public void setIds(Map<Resource.Type, String> mapIds) {
    m_mapResourceIds.putAll(mapIds);
  }

  @Override
  public Map<Resource.Type, String> getIds() {
    return new HashMap<Resource.Type, String>((m_mapResourceIds));
  }

  @Override
  public Query getQuery() {
    return m_query;
  }

  @Override
  public ResourceDefinition getResourceDefinition() {
    return m_resourceDefinition;
  }


  @Override
  public Map<String, ResourceInstance> getSubResources() {
    if (m_mapSubResources == null) {
      m_mapSubResources = new HashMap<String, ResourceInstance>();
      Set<SubResourceDefinition> setSubResourceDefs = getResourceDefinition().getSubResourceDefinitions();

      for (SubResourceDefinition subResDef : setSubResourceDefs) {
        ResourceInstance resource = m_resourceFactory.createResource(subResDef.getType(), getIds());

        // ensure pk is returned
        resource.getQuery().addProperty(m_controller.getSchema(
            subResDef.getType()).getKeyPropertyId(subResDef.getType()));
        // add additionally required fk properties
        for (Resource.Type fkType : subResDef.getAdditionalForeignKeys()) {
          resource.getQuery().addProperty(m_controller.getSchema(subResDef.getType()).getKeyPropertyId(fkType));
        }

        String subResourceName = subResDef.isCollection() ? resource.getResourceDefinition().getPluralName() :
            resource.getResourceDefinition().getSingularName();

        m_mapSubResources.put(subResourceName, resource);
      }
    }
    return m_mapSubResources;
  }

  @Override
  public boolean isCollectionResource() {
    return getIds().get(getResourceDefinition().getType()) == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ResourceInstanceImpl that = (ResourceInstanceImpl) o;

    return m_mapResourceIds.equals(that.m_mapResourceIds) &&
           m_query.equals(that.m_query) &&
           m_resourceDefinition.equals(that.m_resourceDefinition) &&
           m_mapSubResources == null ? that.m_mapSubResources == null :
               m_mapSubResources.equals(that.m_mapSubResources);
  }

  @Override
  public int hashCode() {
    int result =m_query.hashCode();
    result = 31 * result + m_mapResourceIds.hashCode();
    result = 31 * result + m_resourceDefinition.hashCode();
    result = 31 * result + (m_mapSubResources != null ? m_mapSubResources.hashCode() : 0);
    return result;
  }
}
