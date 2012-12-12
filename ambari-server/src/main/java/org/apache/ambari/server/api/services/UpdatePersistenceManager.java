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
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.RequestStatus;

import java.util.Map;
import java.util.Set;


/**
 * Responsible for persisting the updating of a resource in the back end.
 */
public class UpdatePersistenceManager extends BasePersistenceManager {
  @Override
  public RequestStatus persist(ResourceDefinition resource, Set<Map<PropertyId, Object>> setProperties) {
    try {
      return getClusterController().updateResources(resource.getType(), createControllerRequest(
          setProperties), resource.getQuery().getInternalPredicate());
    } catch (AmbariException e) {
      //todo: handle exception
      throw new RuntimeException("Update of resource failed: " + e, e);
    }
  }
}
