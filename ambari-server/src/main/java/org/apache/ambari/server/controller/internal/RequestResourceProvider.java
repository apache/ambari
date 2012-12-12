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
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for request resources.
 */
class RequestResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------
  // Requests
  protected static final PropertyId REQUEST_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("cluster_name","Requests");
  protected static final PropertyId REQUEST_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("id","Requests");


  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          REQUEST_ID_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  RequestResourceProvider(Set<PropertyId> propertyIds,
                          Map<Resource.Type, PropertyId> keyPropertyIds,
                          AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
    Set<PropertyId>         requestedIds         = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    Map<PropertyId, Object> predicateProperties  = getProperties(predicate);
    RequestStatusRequest requestStatusRequest = getRequest(predicateProperties);

    String clusterName = (String) predicateProperties.get(REQUEST_CLUSTER_NAME_PROPERTY_ID);

    Set<RequestStatusResponse> responses = getManagementController()
        .getRequestStatus(requestStatusRequest);
    Set<Resource> resources = new HashSet<Resource>();
    for (RequestStatusResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Request);
      setResourceProperty(resource, REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
      setResourceProperty(resource, REQUEST_ID_PROPERTY_ID, response.getRequestId(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<PropertyId> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private RequestStatusRequest getRequest(Map<PropertyId, Object> properties) {
    Long requestId = null;
    if (properties.get(REQUEST_ID_PROPERTY_ID) != null) {
      requestId = Long.valueOf((String) properties
          .get(REQUEST_ID_PROPERTY_ID));
    }
    return new RequestStatusRequest(requestId);
  }
}
