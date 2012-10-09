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

package org.apache.ambari.api.services;

import org.apache.ambari.api.resources.ResourceDefinition;
import org.apache.ambari.server.AmbariException;

/**
 * Responsible for persisting the deletion of a resource in the back end.
 */
public class DeletePersistenceManager extends BasePersistenceManager {
  @Override
  public void persist(ResourceDefinition resource) {
    try {
      getClusterController().deleteResources(resource.getType(),
          resource.getQuery().getPredicate());
    } catch (AmbariException e) {
      //todo: handle exception
      throw new RuntimeException("Delete of resource failed: " + e, e);
    }
  }
}
