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
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for host resources.
 */
class HostResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------

  // Hosts
  protected static final PropertyId HOST_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("cluster_name", "Hosts");
  protected static final PropertyId HOST_NAME_PROPERTY_ID         = PropertyHelper.getPropertyId("host_name", "Hosts");
  protected static final PropertyId HOST_IP_PROPERTY_ID           = PropertyHelper.getPropertyId("ip", "Hosts");
  protected static final PropertyId HOST_TOTAL_MEM_PROPERTY_ID    = PropertyHelper.getPropertyId("total_mem", "Hosts");
  protected static final PropertyId HOST_CPU_COUNT_PROPERTY_ID    = PropertyHelper.getPropertyId("cpu_count", "Hosts");
  protected static final PropertyId HOST_OS_ARCH_PROPERTY_ID      = PropertyHelper.getPropertyId("os_arch", "Hosts");
  protected static final PropertyId HOST_OS_TYPE_PROPERTY_ID      = PropertyHelper.getPropertyId("os_type", "Hosts");

  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          HOST_NAME_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  HostResourceProvider(Set<PropertyId> propertyIds,
                       Map<Resource.Type, PropertyId> keyPropertyIds,
                       AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {
    Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    getManagementController().createHosts(requests);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
    Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    HostRequest     hostRequest  = getRequest(getProperties(predicate));

    // TODO : handle multiple requests
    Set<HostResponse> responses = getManagementController().getHosts(Collections.singleton(hostRequest));

    Set<Resource> resources = new HashSet<Resource>();
    for (HostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Host);

      // TODO : properly handle more than one cluster
      if (null != hostRequest.getClusterNames()) {
        for (String clusterName : hostRequest.getClusterNames()) {
          if (response.getClusterNames().contains(clusterName)) {
            setResourceProperty(resource, HOST_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
          }
        }
      }

      setResourceProperty(resource, HOST_NAME_PROPERTY_ID, response.getHostname(), requestedIds);
      setResourceProperty(resource, HOST_IP_PROPERTY_ID, response.getIpv4(), requestedIds);
      setResourceProperty(resource, HOST_TOTAL_MEM_PROPERTY_ID, Long.valueOf(response.getTotalMemBytes()), requestedIds);
      setResourceProperty(resource, HOST_CPU_COUNT_PROPERTY_ID, Long.valueOf(response.getCpuCount()), requestedIds);
      setResourceProperty(resource, HOST_OS_ARCH_PROPERTY_ID, response.getOsArch(), requestedIds);
      setResourceProperty(resource, HOST_OS_TYPE_PROPERTY_ID, response.getOsType(), requestedIds);
      // TODO ...
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
    Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      requests.add(getRequest(propertyMap));
    }
    getManagementController().updateHosts(requests);
    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
      requests.add(getRequest(propertyMap));
    }
    getManagementController().deleteHosts(requests);
    return getRequestStatus(null);
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
  private HostRequest getRequest(Map<PropertyId, Object> properties) {
    return new HostRequest(
        (String)  properties.get(HOST_NAME_PROPERTY_ID),
        // TODO : more than one cluster
        properties.containsKey(HOST_CLUSTER_NAME_PROPERTY_ID) ?
            Collections.singletonList((String)  properties.get(HOST_CLUSTER_NAME_PROPERTY_ID)) :
              Collections.<String>emptyList(),
        null);
  }
}
