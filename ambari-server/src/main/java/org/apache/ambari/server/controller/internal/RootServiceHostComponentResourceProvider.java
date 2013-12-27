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
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.RootServiceHostComponentRequest;
import org.apache.ambari.server.controller.RootServiceHostComponentResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

public class RootServiceHostComponentResourceProvider extends
    ReadOnlyResourceProvider {
  
  public static final String SERVICE_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("RootServiceHostComponents", "service_name");
  public static final String HOST_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("RootServiceHostComponents", "host_name");
  public static final String COMPONENT_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("RootServiceHostComponents", "component_name");
  public static final String COMPONENT_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("RootServiceHostComponents", "component_version");
  public static final String COMPONENT_STATE_PROPERTY_ID = PropertyHelper
      .getPropertyId("RootServiceHostComponents", "component_state");
  public static final String PROPERTIES_PROPERTY_ID = PropertyHelper
      .getPropertyId("RootServiceHostComponents", "properties");
  
  
  private Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { SERVICE_NAME_PROPERTY_ID, HOST_NAME_PROPERTY_ID, COMPONENT_NAME_PROPERTY_ID }));


  public RootServiceHostComponentResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  
  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<RootServiceHostComponentRequest> requests = new HashSet<RootServiceHostComponentRequest>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<RootServiceHostComponentResponse> responses = getResources(new Command<Set<RootServiceHostComponentResponse>>() {
      @Override
      public Set<RootServiceHostComponentResponse> invoke() throws AmbariException {
        return getRootServiceHostComponents(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();

    for (RootServiceHostComponentResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RootServiceHostComponent);

      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);
      
      setResourceProperty(resource, HOST_NAME_PROPERTY_ID,
          response.getHostName(), requestedIds);
      
      setResourceProperty(resource, COMPONENT_NAME_PROPERTY_ID,
          response.getComponentName(), requestedIds);
      
      setResourceProperty(resource, COMPONENT_STATE_PROPERTY_ID,
          response.getComponentState(), requestedIds);
      
      setResourceProperty(resource, COMPONENT_VERSION_PROPERTY_ID,
          response.getComponentVersion(), requestedIds);
      
      setResourceProperty(resource, PROPERTIES_PROPERTY_ID,
          response.getProperties(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }
  
  private RootServiceHostComponentRequest getRequest(Map<String, Object> properties) {
    return new RootServiceHostComponentRequest((String) properties.get(SERVICE_NAME_PROPERTY_ID),
                                               (String) properties.get(HOST_NAME_PROPERTY_ID),
                                               (String) properties.get(COMPONENT_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  // Get the root service host components for the given set of requests
  protected Set<RootServiceHostComponentResponse> getRootServiceHostComponents(
      Set<RootServiceHostComponentRequest> requests) throws AmbariException {
    Set<RootServiceHostComponentResponse> response = new HashSet<RootServiceHostComponentResponse>();
    for (RootServiceHostComponentRequest request : requests) {
      String serviceName = request.getServiceName();
      try {
        Set<RootServiceHostComponentResponse> rootServiceHostComponents = getRootServiceHostComponents(request);
        for (RootServiceHostComponentResponse rootServiceHostComponentResponse : rootServiceHostComponents ) {
          rootServiceHostComponentResponse.setServiceName(serviceName);
        }

        response.addAll(rootServiceHostComponents);
      } catch (AmbariException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  // Get the root service host components for the given request
  private Set<RootServiceHostComponentResponse> getRootServiceHostComponents(
      RootServiceHostComponentRequest request) throws AmbariException{

    AmbariManagementController controller = getManagementController();
    //Get all hosts of all clusters
    Set<HostResponse> hosts = HostResourceProvider.getHosts(controller,
        new HostRequest(request.getHostName(), null, null));

    return controller.getRootServiceResponseFactory().getRootServiceHostComponent(request, hosts);
  }
}
