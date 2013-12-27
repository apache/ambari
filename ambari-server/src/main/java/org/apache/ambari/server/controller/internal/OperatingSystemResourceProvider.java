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

package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.OperatingSystemRequest;
import org.apache.ambari.server.controller.OperatingSystemResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

public class OperatingSystemResourceProvider extends ReadOnlyResourceProvider {

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("OperatingSystems", "stack_name");

  private static final String STACK_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("OperatingSystems", "stack_version");

  private static final String OS_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("OperatingSystems", "os_type");

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { STACK_NAME_PROPERTY_ID,
          STACK_VERSION_PROPERTY_ID, OS_TYPE_PROPERTY_ID }));

  protected OperatingSystemResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<OperatingSystemRequest> requests = new HashSet<OperatingSystemRequest>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<OperatingSystemResponse> responses = getResources(new Command<Set<OperatingSystemResponse>>() {
      @Override
      public Set<OperatingSystemResponse> invoke() throws AmbariException {
        return getManagementController().getStackOperatingSystems(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();

    for (OperatingSystemResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.OperatingSystem);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);
      
      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);
      
      setResourceProperty(resource, OS_TYPE_PROPERTY_ID,
          response.getOsType(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  private OperatingSystemRequest getRequest(Map<String, Object> properties) {
    return new OperatingSystemRequest((String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(OS_TYPE_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
