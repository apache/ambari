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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for persisting the creation of a resource in the back end.
 */
public class CreatePersistenceManager extends BasePersistenceManager {
  @Override
  public RequestStatus persist(ResourceDefinition resource, Set<Map<PropertyId, Object>> setProperties) {
    ClusterController controller = getClusterController();
    Map<Resource.Type, String> mapResourceIds = resource.getResourceIds();
    Resource.Type type = resource.getType();
    Schema schema = controller.getSchema(type);

    if (setProperties.size() == 0) {
      setProperties.add(new HashMap<PropertyId, Object>());
    }

    for (Map<PropertyId, Object> mapProperties : setProperties) {
      for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
        PropertyId property = schema.getKeyPropertyId(entry.getKey());
        if (! mapProperties.containsKey(property)) {
          mapProperties.put(property, entry.getValue());
        }
      }
    }

    try {
      return controller.createResources(type, createControllerRequest(setProperties));
    } catch (AmbariException e) {
      //todo: handle exception
      throw new RuntimeException("Create of resource failed: " + e, e);
    }
  }
}
