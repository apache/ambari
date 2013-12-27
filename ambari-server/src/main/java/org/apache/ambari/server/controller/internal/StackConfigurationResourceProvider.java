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
import org.apache.ambari.server.controller.StackConfigurationRequest;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

public class StackConfigurationResourceProvider extends
    ReadOnlyResourceProvider {

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurations", "stack_name");

  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurations", "stack_version");

  public static final String SERVICE_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurations", "service_name");

  public static final String PROPERTY_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurations", "property_name");

  public static final String PROPERTY_VALUE_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurations", "property_value");

  public static final String PROPERTY_DESCRIPTION_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurations", "property_description");

  public static final String PROPERTY_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurations", "type");


  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { STACK_NAME_PROPERTY_ID,
          STACK_VERSION_PROPERTY_ID, SERVICE_NAME_PROPERTY_ID, PROPERTY_NAME_PROPERTY_ID }));

  protected StackConfigurationResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<StackConfigurationRequest> requests = new HashSet<StackConfigurationRequest>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackConfigurationResponse> responses = getResources(new Command<Set<StackConfigurationResponse>>() {
      @Override
      public Set<StackConfigurationResponse> invoke() throws AmbariException {
        return getManagementController().getStackConfigurations(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();
    
    for (StackConfigurationResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.StackConfiguration);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);

      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);

      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);

      setResourceProperty(resource, PROPERTY_NAME_PROPERTY_ID,
          response.getPropertyName(), requestedIds);

      setResourceProperty(resource, PROPERTY_VALUE_PROPERTY_ID,
          response.getPropertyValue(), requestedIds);

      setResourceProperty(resource, PROPERTY_DESCRIPTION_PROPERTY_ID,
          response.getPropertyDescription(), requestedIds);
      
      setResourceProperty(resource, PROPERTY_TYPE_PROPERTY_ID,
          response.getType(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  private StackConfigurationRequest getRequest(Map<String, Object> properties) {
    return new StackConfigurationRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(PROPERTY_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
