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
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * Resource provider for cluster resources.
 */
class ClusterResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // Clusters
  protected static final String CLUSTER_ID_PROPERTY_ID      = PropertyHelper.getPropertyId("Clusters", "cluster_id");
  protected static final String CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("Clusters", "cluster_name");
  protected static final String CLUSTER_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("Clusters", "version");
  protected static final String CLUSTER_DESIRED_CONFIGS_PROPERTY_ID = PropertyHelper.getPropertyId("Clusters", "desired_configs");


  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          CLUSTER_ID_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  ClusterResourceProvider(Set<String> propertyIds,
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

    for (final Map<String, Object> properties : request.getProperties()) {
      createResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          getManagementController().createCluster(getRequest(properties));
          return null;
        }
      });
    }
    notifyCreate(Resource.Type.Cluster, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ClusterRequest> requests = new HashSet<ClusterRequest>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<ClusterResponse> responses = getResources(new Command<Set<ClusterResponse>>() {
      @Override
      public Set<ClusterResponse> invoke() throws AmbariException {
        return getManagementController().getClusters(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Found clusters matching getClusters request"
          + ", clusterResponseCount=" + responses.size());
    }
    for (ClusterResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Cluster);
      setResourceProperty(resource, CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
      setResourceProperty(resource, CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, CLUSTER_DESIRED_CONFIGS_PROPERTY_ID, response.getDesiredConfigs(), requestedIds);

      resource.setProperty(CLUSTER_VERSION_PROPERTY_ID,
          response.getDesiredStackVersion());

      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding ClusterResponse to resource"
            + ", clusterResponse=" + response.toString());
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ClusterRequest>   requests = new HashSet<ClusterRequest>();
    RequestStatusResponse       response;

    for (Map<String, Object> requestPropertyMap : request.getProperties()) {
      Set<Map<String, Object>> propertyMaps = getPropertyMaps(requestPropertyMap, predicate);
      for (Map<String, Object> propertyMap : propertyMaps) {
        requests.add(getRequest(propertyMap));
      }
    }
    response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().updateClusters(requests, request.getRequestInfoProperties());
      }
    });
    notifyUpdate(Resource.Type.Cluster, request, predicate);
    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final ClusterRequest clusterRequest = getRequest(propertyMap);
      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          getManagementController().deleteCluster(clusterRequest);
          return null;
        }
      });
    }
    notifyDelete(Resource.Type.Cluster, predicate);
    return getRequestStatus(null);
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a cluster request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the cluster request object
   */
  private ClusterRequest getRequest(Map<String, Object> properties) {
    ClusterRequest cr = new ClusterRequest(
        (Long) properties.get(CLUSTER_ID_PROPERTY_ID),
        (String) properties.get(CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(CLUSTER_VERSION_PROPERTY_ID),
        null);

    
    ConfigurationRequest configRequest = getConfigurationRequest("Clusters", properties);
    
    if (null != configRequest)
      cr.setDesiredConfig(configRequest);
    
    return cr;
  }
  
  /**
   * {@inheritDoc}  Overridden to support configuration.
   */
  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> baseUnsupported = super.checkPropertyIds(propertyIds);
    
    return checkConfigPropertyIds(baseUnsupported, "Clusters");
  }
}
