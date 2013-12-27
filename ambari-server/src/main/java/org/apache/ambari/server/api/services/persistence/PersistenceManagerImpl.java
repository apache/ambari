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

package org.apache.ambari.server.api.services.persistence;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.*;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Persistence Manager implementation.
 */
public class PersistenceManagerImpl implements PersistenceManager {

  /**
   * Cluster Controller reference.
   */
  private ClusterController m_controller;

  /**
   * Constructor.
   *
   * @param controller  the cluster controller
   */
  public PersistenceManagerImpl(ClusterController controller) {
    m_controller = controller;
  }

  @Override
  public RequestStatus create(ResourceInstance resource, RequestBody requestBody)
      throws UnsupportedPropertyException,
             SystemException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    Map<Resource.Type, String> mapResourceIds = resource.getKeyValueMap();
    Resource.Type type = resource.getResourceDefinition().getType();
    Schema schema = m_controller.getSchema(type);

    Set<NamedPropertySet> setProperties = requestBody.getNamedPropertySets();
    if (setProperties.size() == 0) {
      requestBody.addPropertySet(new NamedPropertySet("", new HashMap<String, Object>()));
    }

    for (NamedPropertySet propertySet : setProperties) {
      for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
        Map<String, Object> mapProperties = propertySet.getProperties();
        String property = schema.getKeyPropertyId(entry.getKey());
        if (! mapProperties.containsKey(property)) {
          mapProperties.put(property, entry.getValue());
        }
      }
    }
    return m_controller.createResources(type, createControllerRequest(requestBody));
  }

  @Override
  public RequestStatus update(ResourceInstance resource, RequestBody requestBody)
      throws UnsupportedPropertyException, SystemException, NoSuchParentResourceException, NoSuchResourceException {

    // Allow for multiple property sets in an update request body...
    Set<NamedPropertySet> setProperties = requestBody.getNamedPropertySets();
    if (setProperties.size() > 1) {
      Map<Resource.Type, String> mapResourceIds = resource.getKeyValueMap();
      Resource.Type type = resource.getResourceDefinition().getType();
      Schema schema = m_controller.getSchema(type);

      for (NamedPropertySet propertySet : setProperties) {
        for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
          Map<String, Object> mapProperties = propertySet.getProperties();
          String property = schema.getKeyPropertyId(entry.getKey());
          if (! mapProperties.containsKey(property)) {
            mapProperties.put(property, entry.getValue());
          }
        }
      }
    }
    return m_controller.updateResources(resource.getResourceDefinition().getType(),
        createControllerRequest(requestBody), resource.getQuery().getPredicate());
  }

  @Override
  public RequestStatus delete(ResourceInstance resource, RequestBody requestBody)
      throws UnsupportedPropertyException, SystemException, NoSuchParentResourceException, NoSuchResourceException {
    //todo: need to account for multiple resources and user predicate
    return m_controller.deleteResources(resource.getResourceDefinition().getType(),
        resource.getQuery().getPredicate());

  }

  protected Request createControllerRequest(RequestBody body) {
    return PropertyHelper.getCreateRequest(body.getPropertySets(), body.getRequestInfoProperties());
  }
}
