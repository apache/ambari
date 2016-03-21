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

import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@StaticallyInject
public class StackServiceResourceProvider extends ReadOnlyResourceProvider {

  protected static final String SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "service_name");
  
  protected static final String SERVICE_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId(
		  "StackServices", "service_type"); 

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "stack_name");

  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "stack_version");

  private static final String SERVICE_DISPLAY_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "display_name");

  private static final String USER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "user_name");

  private static final String COMMENTS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "comments");

  private static final String VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "service_version");

  private static final String CONFIG_TYPES = PropertyHelper.getPropertyId(
      "StackServices", "config_types");
  
  private static final String REQUIRED_SERVICES_ID = PropertyHelper.getPropertyId(
      "StackServices", "required_services");

  private static final String SERVICE_CHECK_SUPPORTED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "service_check_supported");

  private static final String CUSTOM_COMMANDS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "custom_commands");

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { STACK_NAME_PROPERTY_ID,
          STACK_VERSION_PROPERTY_ID, SERVICE_NAME_PROPERTY_ID }));

  /**
   * KerberosServiceDescriptorFactory used to create KerberosServiceDescriptor instances
   */
  @Inject
  private static KerberosServiceDescriptorFactory kerberosServiceDescriptorFactory;

  protected StackServiceResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<StackServiceRequest> requests = new HashSet<StackServiceRequest>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackServiceResponse> responses = getResources(new Command<Set<StackServiceResponse>>() {
      @Override
      public Set<StackServiceResponse> invoke() throws AmbariException {
        return getManagementController().getStackServices(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();

    for (StackServiceResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.StackService);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);

      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);

      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);
      
      setResourceProperty(resource, SERVICE_TYPE_PROPERTY_ID,
    		  response.getServiceType(), requestedIds); 

      setResourceProperty(resource, SERVICE_DISPLAY_NAME_PROPERTY_ID,
          response.getServiceDisplayName(), requestedIds);

      setResourceProperty(resource, USER_NAME_PROPERTY_ID,
          response.getUserName(), requestedIds);

      setResourceProperty(resource, COMMENTS_PROPERTY_ID,
          response.getComments(), requestedIds);

      setResourceProperty(resource, VERSION_PROPERTY_ID,
          response.getServiceVersion(), requestedIds);

      setResourceProperty(resource, CONFIG_TYPES,
          response.getConfigTypes(), requestedIds);

      setResourceProperty(resource, REQUIRED_SERVICES_ID,
          response.getRequiredServices(), requestedIds);

      setResourceProperty(resource, SERVICE_CHECK_SUPPORTED_PROPERTY_ID,
          response.isServiceCheckSupported(), requestedIds);

      setResourceProperty(resource, CUSTOM_COMMANDS_PROPERTY_ID,
          response.getCustomCommands(), requestedIds);

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
