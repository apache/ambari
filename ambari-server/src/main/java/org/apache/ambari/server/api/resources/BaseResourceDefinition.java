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


import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.query.QueryImpl;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.api.util.TreeNode;

import java.util.*;

/**
 * Base resource definition.  Contains behavior common to all resource types.
 */
public abstract class BaseResourceDefinition implements ResourceDefinition {

  /**
   * Resource type.  One of {@link Resource.Type}
   */
  private Resource.Type m_type;

  /**
   * Value of the id property for the resource.
   */
  private String m_id;

  /**
   * Query associated with the resource definition.
   */
  private Query m_query;

  /**
   * Map of primary and foreign keys and values necessary to identify the resource.
   */
  private Map<Resource.Type, String> m_mapResourceIds = new HashMap<Resource.Type, String>();

  //TODO: Refactor out of this class when setProperties is moved.
  private Map<PropertyId, String> m_properties = new HashMap<PropertyId, String>();


  /**
   * Constructor.
   *
   * @param resourceType resource type
   * @param id           value of primary key
   */
  public BaseResourceDefinition(Resource.Type resourceType, String id) {
    m_type = resourceType;
    setId(id);
    m_query = new QueryImpl(this);
  }

  @Override
  public void setParentId(Resource.Type type, String value) {
    setResourceId(type, value);
  }

  @Override
  public String getId() {
    return m_id;
  }

  void setId(String val) {
    setResourceId(getType(), val);
    m_id = val;
  }

  @Override
  public Resource.Type getType() {
    return m_type;
  }


  @Override
  public Query getQuery() {
    return m_query;
  }

  protected void setResourceId(Resource.Type resourceType, String val) {
    m_mapResourceIds.put(resourceType, val);
  }

  @Override
  public Map<Resource.Type, String> getResourceIds() {
    return m_mapResourceIds;
  }

  ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = new ArrayList<PostProcessor>();
    listProcessors.add(new BaseHrefPostProcessor());

    return listProcessors;
  }

  //todo: refactor set/get property methods out of this class
  @Override
  public void setProperty(PropertyId property, String value) {
    m_properties.put(property, value);
  }

  @Override
  public void setProperties(Map<PropertyId, String> mapProperties) {
    m_properties.putAll(mapProperties);
  }

  @Override
  public Map<PropertyId, String> getProperties() {
    return m_properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseResourceDefinition)) return false;

    BaseResourceDefinition that = (BaseResourceDefinition) o;

    if (m_id != null ? !m_id.equals(that.m_id) : that.m_id != null) return false;
    if (m_mapResourceIds != null ? !m_mapResourceIds.equals(that.m_mapResourceIds) : that.m_mapResourceIds != null)
      return false;

    return m_type == that.m_type;

  }

  @Override
  public int hashCode() {
    int result = m_type != null ? m_type.hashCode() : 0;
    result = 31 * result + (m_id != null ? m_id.hashCode() : 0);
    result = 31 * result + (m_mapResourceIds != null ? m_mapResourceIds.hashCode() : 0);
    return result;
  }

  class BaseHrefPostProcessor implements PostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      Resource r = resultNode.getObject();
      TreeNode<Resource> parent = resultNode.getParent();

      if (parent.getName() != null) {
        String parentName = parent.getName();
        Schema schema = getClusterController().getSchema(r.getType());
        String id = r.getPropertyValue(schema.getKeyPropertyId(r.getType()));

        int i = href.indexOf("?");
        if (i != -1) {
          href = href.substring(0, i);
        }

        if (!href.endsWith("/")) {
          href = href + '/';
        }
        String isCollectionResource = parent.getProperty("isCollection");
        href = "true".equals(isCollectionResource) ? href + id : href + parentName + '/' + id;
      }
      resultNode.setProperty("href", href);
    }
  }
}
