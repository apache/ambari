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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for request resources.
 */
class RequestResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------
  // Requests
  protected static final String REQUEST_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Requests", "cluster_name");
  protected static final String REQUEST_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("Requests", "id");
  protected static final String REQUEST_STATUS_PROPERTY_ID       = PropertyHelper.getPropertyId("Requests", "request_status");
  protected static final String REQUEST_CONTEXT_ID               = PropertyHelper.getPropertyId("Requests", "request_context");

  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          REQUEST_ID_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  RequestResourceProvider(Set<String> propertyIds,
                          Map<Resource.Type, String> keyPropertyIds,
                          AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources    = new HashSet<Resource>();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);

    // get the request objects by processing the given predicate
    Map<String, Set<RequestStatusRequest>> requestStatusRequestSetMap = getRequests(getPropertyMaps(predicate));

    for(Map.Entry<String, Set<RequestStatusRequest>> entry: requestStatusRequestSetMap.entrySet()) {

      String clusterName = entry.getKey();
      Set<RequestStatusRequest> requestStatusRequestSet = entry.getValue();

      for (RequestStatusRequest requestStatusRequest : requestStatusRequestSet){

        // we need to make a separate request for each request object
        final RequestStatusRequest finalRequest = requestStatusRequest;
        Set<RequestStatusResponse> responses = getResources(new Command<Set<RequestStatusResponse>>() {
          @Override
          public Set<RequestStatusResponse> invoke() throws AmbariException {
            return getManagementController().getRequestStatus(finalRequest);
          }
        });

        for (RequestStatusResponse response : responses) {
          Resource resource = new ResourceImpl(Resource.Type.Request);
          setResourceProperty(resource, REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
          setResourceProperty(resource, REQUEST_ID_PROPERTY_ID, response.getRequestId(), requestedIds);
          setResourceProperty(resource, REQUEST_CONTEXT_ID, response.getRequestContext(), requestedIds);
          if (requestStatusRequest.getRequestStatus() != null) {
            setResourceProperty(resource, REQUEST_STATUS_PROPERTY_ID, requestStatusRequest.getRequestStatus(),
                requestedIds);
          }
          resources.add(resource);
        }
      }
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a map of component request objects from the given maps of property values.
   *
   * @param propertiesSet  the set of property maps from the predicate
   *
   * @return the map of component request objects keyed by cluster name
   */
  private Map<String, Set<RequestStatusRequest>> getRequests(Set<Map<String, Object>> propertiesSet) {

    Map<String, Set<RequestStatusRequest>> requestSetMap = new HashMap<String, Set<RequestStatusRequest>>();

    for (Map<String, Object> properties : propertiesSet) {
      Long   requestId   = null;
      String clusterName = (String) properties.get(REQUEST_CLUSTER_NAME_PROPERTY_ID);

      // group the requests by cluster name
      Set<RequestStatusRequest> requestSet = requestSetMap.get(clusterName);

      if (requestSet == null) {
        requestSet = new HashSet<RequestStatusRequest>();
        requestSetMap.put(clusterName, requestSet);
      }

      if (properties.get(REQUEST_ID_PROPERTY_ID) != null) {
        requestId = Long.valueOf((String) properties
            .get(REQUEST_ID_PROPERTY_ID));
      }
      String requestStatus = null;
      if (properties.get(REQUEST_STATUS_PROPERTY_ID) != null) {
        requestStatus = (String)properties.get(REQUEST_STATUS_PROPERTY_ID);
      }
      requestSet.add(new RequestStatusRequest(requestId, requestStatus));
    }
    return requestSetMap;
  }
}
