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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.alert.MetricAlert;

import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * ResourceProvider for Alert Definitions
 */
public class AlertDefinitionResourceProvider extends AbstractControllerResourceProvider {

  protected static final String ALERT_DEF_CLUSTER_NAME = "AlertDefinition/cluster_name";
  protected static final String ALERT_DEF_ID = "AlertDefinition/id";
  protected static final String ALERT_DEF_NAME = "AlertDefinition/name";
  protected static final String ALERT_DEF_INTERVAL = "AlertDefinition/interval";
  protected static final String ALERT_DEF_SOURCE_TYPE = "AlertDefinition/source";
  protected static final String ALERT_DEF_SERVICE_NAME = "AlertDefinition/service_name";
  protected static final String ALERT_DEF_COMPONENT_NAME = "AlertDefinition/component_name";
  protected static final String ALERT_DEF_ENABLED = "AlertDefinition/enabled";
  protected static final String ALERT_DEF_SCOPE = "AlertDefinition/scope";
  
  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(ALERT_DEF_ID, ALERT_DEF_NAME));
  private static AlertDefinitionDAO alertDefinitionDAO = null;
  
  /**
   * @param instance
   */
  @Inject
  public static void init(AlertDefinitionDAO instance) {
    alertDefinitionDAO = instance;
  }
  
  AlertDefinitionResourceProvider(Set<String> propertyIds,
      Map<Resource.Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }
  
  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);
    
    Set<Resource> results = new HashSet<Resource>();
    
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(ALERT_DEF_CLUSTER_NAME);
      
      if (null == clusterName || clusterName.isEmpty())
        throw new IllegalArgumentException("Invalid argument, cluster name is required");
      
      String id = (String) propertyMap.get(ALERT_DEF_ID);
      if (null != id) {
        AlertDefinitionEntity entity = alertDefinitionDAO.findById(Long.parseLong(id));
        if (null != entity) {
          results.add(toResource(false, clusterName, entity, requestPropertyIds));
        }
      } else {
        
        Cluster cluster = null;
        try {
          cluster = getManagementController().getClusters().getCluster(clusterName);
        } catch (AmbariException e) {
          throw new NoSuchResourceException("Parent Cluster resource doesn't exist", e);
        }
        
        List<AlertDefinitionEntity> entities = alertDefinitionDAO.findAll(
            cluster.getClusterId());

        for (AlertDefinitionEntity entity : entities) {
          results.add(toResource(true, clusterName, entity, requestPropertyIds));
        }
      }
    }
    
    return results;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  
  private Resource toResource(boolean isCollection, String clusterName,
      AlertDefinitionEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.AlertDefinition);
    
    setResourceProperty(resource, ALERT_DEF_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, ALERT_DEF_ID, entity.getDefinitionId(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_NAME, entity.getDefinitionName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_INTERVAL, entity.getScheduleInterval(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SOURCE_TYPE, entity.getSourceType(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SERVICE_NAME, entity.getServiceName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_COMPONENT_NAME, entity.getComponentName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_ENABLED, Boolean.valueOf(entity.getEnabled()), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SCOPE, entity.getScope(), requestedIds);
    
    if (!isCollection && null != resource.getPropertyValue(ALERT_DEF_SOURCE_TYPE)) {
      Gson gson = new Gson();
      
      if (entity.getSourceType().equals("metric")) {
        try {
          MetricAlert ma = gson.fromJson(entity.getSource(), MetricAlert.class);
          resource.setProperty("AlertDefinition/metric", ma);
        } catch (Exception e) {
          LOG.error("Could not coerce alert source into a type");
        }
      }
    }
    
    return resource;
  }
  
}
