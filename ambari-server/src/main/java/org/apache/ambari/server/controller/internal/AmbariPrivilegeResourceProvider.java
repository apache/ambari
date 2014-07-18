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
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ResourceEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for Ambari privileges.
 */
public class AmbariPrivilegeResourceProvider extends PrivilegeResourceProvider<Object> {

  /**
   * The property ids for an Ambari privilege resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(PRIVILEGE_ID_PROPERTY_ID);
    propertyIds.add(PERMISSION_NAME_PROPERTY_ID);
    propertyIds.add(PRINCIPAL_NAME_PROPERTY_ID);
    propertyIds.add(PRINCIPAL_TYPE_PROPERTY_ID);
  }

  /**
   * The key property ids for a privilege resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();
  static {
    keyPropertyIds.put(Resource.Type.AmbariPrivilege, PRIVILEGE_ID_PROPERTY_ID);
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an AmbariPrivilegeResourceProvider.
   */
  public AmbariPrivilegeResourceProvider() {
    super(propertyIds, keyPropertyIds, Resource.Type.AmbariPrivilege);
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  public Map<Long, Object> getResourceEntities(Map<String, Object> properties) {
    // the singleton Ambari entity is implied
    return Collections.singletonMap(ResourceEntity.AMBARI_RESOURCE_ID, null);
  }
}
