/*
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


package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackVersionRequest;
import org.apache.ambari.server.controller.StackVersionResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

@StaticallyInject
public class StackVersionResourceProvider extends ReadOnlyResourceProvider {

  public static final String STACK_VERSION_PROPERTY_ID     = PropertyHelper.getPropertyId("Versions", "stack_version");
  public static final String STACK_NAME_PROPERTY_ID        = PropertyHelper.getPropertyId("Versions", "stack_name");
  public static final String STACK_MIN_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("Versions", "min_upgrade_version");
  public static final String STACK_ACTIVE_PROPERTY_ID      = PropertyHelper.getPropertyId("Versions", "active");
  public static final String STACK_VALID_PROPERTY_ID      = PropertyHelper.getPropertyId("Versions", "valid");
  public static final String STACK_ERROR_SET      = PropertyHelper.getPropertyId("Versions", "stack-errors");
  public static final String STACK_CONFIG_TYPES            = PropertyHelper.getPropertyId("Versions", "config_types");
  public static final String STACK_PARENT_PROPERTY_ID      = PropertyHelper.getPropertyId("Versions", "parent_stack_version");
  public static final String UPGRADE_PACKS_PROPERTY_ID = PropertyHelper.getPropertyId("Versions", "upgrade_packs");
  public static final String STACK_MIN_JDK     = PropertyHelper.getPropertyId("Versions", "min_jdk");
  public static final String STACK_MAX_JDK     = PropertyHelper.getPropertyId("Versions", "max_jdk");

  /**
   * The key property ids for a StackVersion resource.
   */
  protected static Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Stack, STACK_NAME_PROPERTY_ID)
      .put(Type.StackVersion, STACK_VERSION_PROPERTY_ID)
      .build();

  /**
   * The property ids for a StackVersion resource.
   */
  protected static Set<String> propertyIds = Sets.newHashSet(
      STACK_VERSION_PROPERTY_ID,
      STACK_NAME_PROPERTY_ID,
      STACK_MIN_VERSION_PROPERTY_ID,
      STACK_ACTIVE_PROPERTY_ID,
      STACK_VALID_PROPERTY_ID,
      STACK_ERROR_SET,
      STACK_CONFIG_TYPES,
      STACK_PARENT_PROPERTY_ID,
      UPGRADE_PACKS_PROPERTY_ID,
      STACK_MIN_JDK,
      STACK_MAX_JDK);

  protected StackVersionResourceProvider(AmbariManagementController managementController) {
    super(Type.StackVersion, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<StackVersionRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackVersionResponse> responses = getResources(new Command<Set<StackVersionResponse>>() {
      @Override
      public Set<StackVersionResponse> invoke() throws AmbariException {
        return getManagementController().getStackVersions(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (StackVersionResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.StackVersion);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);

      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);

      setResourceProperty(resource, STACK_MIN_VERSION_PROPERTY_ID,
          response.getMinUpgradeVersion(), requestedIds);

      setResourceProperty(resource, STACK_ACTIVE_PROPERTY_ID,
          response.isActive(), requestedIds);

      setResourceProperty(resource, STACK_VALID_PROPERTY_ID,
          response.isValid(), requestedIds);
      
      setResourceProperty(resource, STACK_ERROR_SET,
          response.getErrors(), requestedIds);
      
      setResourceProperty(resource, STACK_PARENT_PROPERTY_ID,
        response.getParentVersion(), requestedIds);

      setResourceProperty(resource, STACK_CONFIG_TYPES,
          response.getConfigTypes(), requestedIds);
      
      setResourceProperty(resource, UPGRADE_PACKS_PROPERTY_ID,
          response.getUpgradePacks(), requestedIds);

      setResourceProperty(resource, STACK_MIN_JDK,
              response.getMinJdk(), requestedIds);

      setResourceProperty(resource, STACK_MAX_JDK,
              response.getMaxJdk(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  private StackVersionRequest getRequest(Map<String, Object> properties) {
    return new StackVersionRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
