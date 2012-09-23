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


import org.apache.ambari.api.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.api.query.Query;
import org.apache.ambari.api.query.QueryImpl;
import org.apache.ambari.api.controller.spi.ClusterController;
import org.apache.ambari.api.controller.spi.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class BaseResourceDefinition implements ResourceDefinition {

  private Resource.Type m_type;
  private String m_id;
  private Query m_query = new QueryImpl(this);
  Map<Resource.Type, String> m_mapResourceIds = new HashMap<Resource.Type, String>();

  public BaseResourceDefinition(Resource.Type resourceType, String id) {
    m_type = resourceType;
    m_id = id;

    if (id != null) {
      setResourceId(resourceType, id);
    }
  }

  @Override
  public String getId() {
    return m_id;
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
    //todo: hack for case where service id is null when getting a component from hostComponent
    if (val != null) {
      m_mapResourceIds.put(resourceType, val);
    }
  }

  @Override
  public Map<Resource.Type, String> getResourceIds() {
    return m_mapResourceIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseResourceDefinition)) return false;

    BaseResourceDefinition that = (BaseResourceDefinition) o;

    if (m_id != null ? !m_id.equals(that.m_id) : that.m_id != null) return false;
    if (m_mapResourceIds != null ? !m_mapResourceIds.equals(that.m_mapResourceIds) : that.m_mapResourceIds != null)
      return false;
    if (m_type != that.m_type) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = m_type != null ? m_type.hashCode() : 0;
    result = 31 * result + (m_id != null ? m_id.hashCode() : 0);
    result = 31 * result + (m_mapResourceIds != null ? m_mapResourceIds.hashCode() : 0);
    return result;
  }

  ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }
}
