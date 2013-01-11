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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;


/**
 * Resource provider for host resources.
 */
class HostResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------

  // Hosts
  protected static final String HOST_CLUSTER_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "cluster_name");
  protected static final String HOST_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_name");
  protected static final String HOST_PUBLIC_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "public_host_name");
  protected static final String HOST_IP_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "ip");
  protected static final String HOST_TOTAL_MEM_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "total_mem");
  protected static final String HOST_CPU_COUNT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "cpu_count");
  protected static final String HOST_OS_ARCH_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "os_arch");
  protected static final String HOST_OS_TYPE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "os_type");
  protected static final String HOST_RACK_INFO_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "rack_info");
  protected static final String HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "last_heartbeat_time");
  protected static final String HOST_LAST_REGISTRATION_TIME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "last_registration_time");
  protected static final String HOST_DISK_INFO_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "disk_info");
  protected static final String HOST_HOST_STATUS_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_status");
  protected static final String HOST_HOST_HEALTH_REPORT_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_health_report");
  protected static final String HOST_STATE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_state");

  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          HOST_NAME_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  HostResourceProvider(Set<String> propertyIds,
                       Map<Resource.Type, String> keyPropertyIds,
                       AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
          UnsupportedPropertyException,
          ResourceAlreadyExistsException,
          NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().createHosts(requests);
        return null;
      }
    });

    notifyCreate(Resource.Type.Host, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();

    if (predicate == null) {
      requests.add(getRequest(null));
    }
    else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<HostResponse> responses = getResources(new Command<Set<HostResponse>>() {
      @Override
      public Set<HostResponse> invoke() throws AmbariException {
        return getManagementController().getHosts(requests);
      }
    });

    Set<String>   requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    Set<Resource> resources    = new HashSet<Resource>();

    for (HostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Host);

      // TODO : properly handle more than one cluster
      if (response.getClusterName() != null
          && !response.getClusterName().isEmpty()) {
        setResourceProperty(resource, HOST_CLUSTER_NAME_PROPERTY_ID,
            response.getClusterName(), requestedIds);
      }

      setResourceProperty(resource, HOST_NAME_PROPERTY_ID,
          response.getHostname(), requestedIds);
      setResourceProperty(resource, HOST_PUBLIC_NAME_PROPERTY_ID,
          response.getPublicHostName(), requestedIds);
      setResourceProperty(resource, HOST_IP_PROPERTY_ID,
          response.getIpv4(), requestedIds);
      setResourceProperty(resource, HOST_TOTAL_MEM_PROPERTY_ID,
          response.getTotalMemBytes(), requestedIds);
      setResourceProperty(resource, HOST_CPU_COUNT_PROPERTY_ID,
          (long) response.getCpuCount(), requestedIds);
      setResourceProperty(resource, HOST_OS_ARCH_PROPERTY_ID,
          response.getOsArch(), requestedIds);
      setResourceProperty(resource, HOST_OS_TYPE_PROPERTY_ID,
          response.getOsType(), requestedIds);
      setResourceProperty(resource, HOST_RACK_INFO_PROPERTY_ID,
          response.getRackInfo(), requestedIds);
      setResourceProperty(resource, HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID,
          response.getLastHeartbeatTime(), requestedIds);
      setResourceProperty(resource, HOST_LAST_REGISTRATION_TIME_PROPERTY_ID,
          response.getLastRegistrationTime(), requestedIds);
      setResourceProperty(resource, HOST_HOST_STATUS_PROPERTY_ID,
          response.getHealthStatus().getHealthStatus().toString(),requestedIds);
      setResourceProperty(resource, HOST_HOST_HEALTH_REPORT_PROPERTY_ID,
          response.getHealthStatus().getHealthReport(), requestedIds);
      setResourceProperty(resource, HOST_DISK_INFO_PROPERTY_ID,
          response.getDisksInfo(), requestedIds);
      setResourceProperty(resource, HOST_STATE_PROPERTY_ID,
          response.getHostState(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().updateHosts(requests);
        return null;
      }
    });

    notifyUpdate(Resource.Type.Host, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<HostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().deleteHosts(requests);
        return null;
      }
    });

    notifyDelete(Resource.Type.Host, predicate);

    return getRequestStatus(null);
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a host request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private HostRequest getRequest(Map<String, Object> properties) {

    if (properties == null) {
      return  new HostRequest(null, null, null);
    }

    HostRequest hostRequest = new HostRequest(
        (String) properties.get(HOST_NAME_PROPERTY_ID),
        (String) properties.get(HOST_CLUSTER_NAME_PROPERTY_ID),
        null);
    hostRequest.setPublicHostName((String) properties.get(HOST_PUBLIC_NAME_PROPERTY_ID));
    hostRequest.setRackInfo((String) properties.get(HOST_RACK_INFO_PROPERTY_ID));

    return hostRequest;
  }
}
