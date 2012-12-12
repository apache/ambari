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
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
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
 * Resource provider for cluster resources.
 */
class ClusterResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------

  // Clusters
  protected static final PropertyId CLUSTER_ID_PROPERTY_ID      = PropertyHelper.getPropertyId("cluster_id", "Clusters");
  protected static final PropertyId CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("cluster_name", "Clusters");
  protected static final PropertyId CLUSTER_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("version", "Clusters");
  protected static final PropertyId CLUSTER_HOSTS_PROPERTY_ID   = PropertyHelper.getPropertyId("hosts", "Clusters");


  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          CLUSTER_ID_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  ClusterResourceProvider(Set<PropertyId> propertyIds,
                          Map<Resource.Type, PropertyId> keyPropertyIds,
                          AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

// ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {

    for (Map<PropertyId, Object> properties : request.getProperties()) {
      getManagementController().createCluster(getRequest(properties));
    }
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
    ClusterRequest clusterRequest = getRequest(getProperties(predicate));
    Set<PropertyId> requestedIds   = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);

    // TODO : handle multiple requests
    Set<ClusterResponse> responses = getManagementController().getClusters(Collections.singleton(clusterRequest));

    Set<Resource> resources = new HashSet<Resource>();
    for (ClusterResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Cluster);
      setResourceProperty(resource, CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
      setResourceProperty(resource, CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      // FIXME requestedIds does not seem to be filled in properly for
      // non-partial responses
      resource.setProperty(CLUSTER_VERSION_PROPERTY_ID,
          response.getDesiredStackVersion());
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      ClusterRequest clusterRequest = getRequest(propertyMap);
      getManagementController().updateCluster(clusterRequest);
    }
    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
      ClusterRequest clusterRequest = getRequest(propertyMap);
      getManagementController().deleteCluster(clusterRequest);
    }
    return getRequestStatus(null);
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<PropertyId> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a cluster request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the cluster request object
   */
  private ClusterRequest getRequest(Map<PropertyId, Object> properties) {

    Long id = (Long) properties.get(CLUSTER_ID_PROPERTY_ID);
    String stackVersion = (String) properties.get(CLUSTER_VERSION_PROPERTY_ID);

    return new ClusterRequest(
        id == null ? null : id,
        (String) properties.get(CLUSTER_NAME_PROPERTY_ID),
        stackVersion == null ? "HDP-0.1" : stackVersion,    // TODO : looks like version is required
        /*properties.get(CLUSTER_HOSTS_PROPERTY_ID)*/ null);
  }
}
