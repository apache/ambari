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
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

public class StackServiceResourceProvider extends ReadOnlyResourceProvider {

  protected static final String SERVICE_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackServices", "service_name");

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackServices", "stack_name");

  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackServices", "stack_version");

  private static final String USER_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackServices", "user_name");

  private static final String COMMENTS_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackServices", "comments");

  private static final String VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackServices", "service_version");;

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { STACK_NAME_PROPERTY_ID,
          STACK_VERSION_PROPERTY_ID, SERVICE_NAME_PROPERTY_ID }));

  protected StackServiceResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final StackServiceRequest stackServiceRequest = getRequest(PredicateHelper
        .getProperties(predicate));
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackServiceResponse> responses = getResources(new Command<Set<StackServiceResponse>>() {
      @Override
      public Set<StackServiceResponse> invoke() throws AmbariException {
        return getManagementController().getStackServices(
            Collections.singleton(stackServiceRequest));
      }
    });

    Set<Resource> resources = new HashSet<Resource>();

    for (StackServiceResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.StackService);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          stackServiceRequest.getStackName(), requestedIds);

      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          stackServiceRequest.getStackVersion(), requestedIds);

      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);

      setResourceProperty(resource, USER_NAME_PROPERTY_ID,
          response.getUserName(), requestedIds);

      setResourceProperty(resource, COMMENTS_PROPERTY_ID,
          response.getComments(), requestedIds);
      
      setResourceProperty(resource, VERSION_PROPERTY_ID,
          response.getServiceVersion(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  private StackServiceRequest getRequest(Map<String, Object> properties) {
    return new StackServiceRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(SERVICE_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
