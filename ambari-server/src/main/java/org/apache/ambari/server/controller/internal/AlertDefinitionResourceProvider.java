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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

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
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

/**
 * ResourceProvider for Alert Definitions
 */
public class AlertDefinitionResourceProvider extends AbstractControllerResourceProvider {

  protected static final String ALERT_DEF_CLUSTER_NAME = "AlertDefinition/cluster_name";
  protected static final String ALERT_DEF_ID = "AlertDefinition/id";
  protected static final String ALERT_DEF_NAME = "AlertDefinition/name";
  protected static final String ALERT_DEF_LABEL = "AlertDefinition/label";
  protected static final String ALERT_DEF_INTERVAL = "AlertDefinition/interval";
  protected static final String ALERT_DEF_SOURCE_TYPE = "AlertDefinition/source/type";
  protected static final String ALERT_DEF_SOURCE = "AlertDefinition/source";
  protected static final String ALERT_DEF_SERVICE_NAME = "AlertDefinition/service_name";
  protected static final String ALERT_DEF_COMPONENT_NAME = "AlertDefinition/component_name";
  protected static final String ALERT_DEF_ENABLED = "AlertDefinition/enabled";
  protected static final String ALERT_DEF_SCOPE = "AlertDefinition/scope";

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(ALERT_DEF_ID, ALERT_DEF_NAME));
  private static AlertDefinitionDAO alertDefinitionDAO = null;

  private static Gson gson = new Gson();

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
  public RequestStatus createResources(final Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createAlertDefinitions(request.getProperties());
        return null;
      }
    });
    notifyCreate(Resource.Type.AlertDefinition, request);

    return getRequestStatus(null);
  }

  private void createAlertDefinitions(Set<Map<String, Object>> requestMaps)
    throws AmbariException {
    List<AlertDefinitionEntity> entities = new ArrayList<AlertDefinitionEntity>();

    for (Map<String, Object> requestMap : requestMaps) {
      entities.add(toCreateEntity(requestMap));
    }

    // !!! TODO multi-create in a transaction
    for (AlertDefinitionEntity entity : entities) {
      alertDefinitionDAO.create(entity);
    }
  }

  private AlertDefinitionEntity toCreateEntity(Map<String, Object> requestMap)
    throws AmbariException {

    String clusterName = (String) requestMap.get(ALERT_DEF_CLUSTER_NAME);

    if (null == clusterName || clusterName.isEmpty()) {
      throw new IllegalArgumentException("Invalid argument, cluster name is required");
    }

    if (!requestMap.containsKey(ALERT_DEF_INTERVAL)) {
      throw new IllegalArgumentException("Check interval must be specified");
    }

    Integer interval = Integer.valueOf((String) requestMap.get(ALERT_DEF_INTERVAL));

    if (!requestMap.containsKey(ALERT_DEF_NAME)) {
      throw new IllegalArgumentException("Definition name must be specified");
    }

    if (!requestMap.containsKey(ALERT_DEF_SERVICE_NAME)) {
      throw new IllegalArgumentException("Service name must be specified");
    }

    if (!requestMap.containsKey(ALERT_DEF_SOURCE_TYPE)) {
      throw new IllegalArgumentException(String.format(
          "Source type must be specified and one of %s", EnumSet.allOf(
              SourceType.class)));
    }

    JsonObject jsonObj = new JsonObject();

    for (Entry<String, Object> entry : requestMap.entrySet()) {
      String propCat = PropertyHelper.getPropertyCategory(entry.getKey());
      String propName = PropertyHelper.getPropertyName(entry.getKey());

      if (propCat.equals(ALERT_DEF_SOURCE)) {
        jsonObj.addProperty(propName, entry.getValue().toString());
      }
    }

    if (0 == jsonObj.entrySet().size()) {
      throw new IllegalArgumentException("Source must be specified");
    }

    Cluster cluster = getManagementController().getClusters().getCluster(clusterName);

    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    entity.setClusterId(Long.valueOf(cluster.getClusterId()));
    entity.setComponentName((String) requestMap.get(ALERT_DEF_COMPONENT_NAME));
    entity.setDefinitionName((String) requestMap.get(ALERT_DEF_NAME));
    entity.setLabel((String) requestMap.get(ALERT_DEF_LABEL));

    boolean enabled = requestMap.containsKey(ALERT_DEF_ENABLED) ?
        Boolean.parseBoolean((String)requestMap.get(ALERT_DEF_ENABLED)) : true;

    entity.setEnabled(enabled);
    entity.setHash(UUID.randomUUID().toString());
    entity.setScheduleInterval(interval);
    entity.setServiceName((String) requestMap.get(ALERT_DEF_SERVICE_NAME));
    entity.setSourceType((String) requestMap.get(ALERT_DEF_SOURCE_TYPE));
    entity.setSource(jsonObj.toString());

    Scope scope = null;
    String desiredScope = (String) requestMap.get(ALERT_DEF_SCOPE);
    if (null != desiredScope && desiredScope.length() > 0) {
      scope = Scope.valueOf(desiredScope);
    }

    entity.setScope(scope);

    return entity;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    Set<Resource> results = new HashSet<Resource>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(ALERT_DEF_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException("Invalid argument, cluster name is required");
      }

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

    for (Map<String, Object> requestPropMap : request.getProperties()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(requestPropMap, predicate)) {
        Long id = (Long) propertyMap.get(ALERT_DEF_ID);

        AlertDefinitionEntity entity = alertDefinitionDAO.findById(id.longValue());
        if (null == entity) {
          continue;
        }

        if (propertyMap.containsKey(ALERT_DEF_NAME)) {
          entity.setDefinitionName((String) propertyMap.get(ALERT_DEF_NAME));
        }

        if (propertyMap.containsKey(ALERT_DEF_ENABLED)) {
          entity.setEnabled(Boolean.parseBoolean(
              (String) propertyMap.get(ALERT_DEF_ENABLED)));
        }

        if (propertyMap.containsKey(ALERT_DEF_INTERVAL)) {
          entity.setScheduleInterval(Integer.valueOf(
              (String) propertyMap.get(ALERT_DEF_INTERVAL)));
        }

        if (propertyMap.containsKey(ALERT_DEF_SCOPE)){
          Scope scope = null;
          String desiredScope = (String) propertyMap.get(ALERT_DEF_SCOPE);

          if (null != desiredScope && desiredScope.length() > 0) {
            scope = Scope.valueOf((desiredScope));
          }

          entity.setScope(scope);
        }


        if (propertyMap.containsKey(ALERT_DEF_SOURCE_TYPE)) {
          entity.setSourceType((String) propertyMap.get(ALERT_DEF_SOURCE_TYPE));
        }

        JsonObject jsonObj = new JsonObject();

        for (Entry<String, Object> entry : propertyMap.entrySet()) {
          String propCat = PropertyHelper.getPropertyCategory(entry.getKey());
          String propName = PropertyHelper.getPropertyName(entry.getKey());

          if (propCat.equals(ALERT_DEF_SOURCE)) {
            jsonObj.addProperty(propName, entry.getValue().toString());
          }
        }

        entity.setHash(UUID.randomUUID().toString());

        alertDefinitionDAO.merge(entity);
      }
    }

    notifyUpdate(Resource.Type.AlertDefinition, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = getResources(
        new RequestImpl(null, null, null, null), predicate);

    Set<Long> definitionIds = new HashSet<Long>();

    for (final Resource resource : resources) {
      definitionIds.add((Long) resource.getPropertyValue(ALERT_DEF_ID));
    }

    for (Long definitionId : definitionIds) {

      LOG.info("Deleting alert definition {}", definitionId);

      final AlertDefinitionEntity ad = alertDefinitionDAO.findById(definitionId.longValue());

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          alertDefinitionDAO.remove(ad);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.AlertDefinition, predicate);
    return getRequestStatus(null);

  }

  private Resource toResource(boolean isCollection, String clusterName,
      AlertDefinitionEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.AlertDefinition);

    setResourceProperty(resource, ALERT_DEF_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, ALERT_DEF_ID, entity.getDefinitionId(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_NAME, entity.getDefinitionName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_INTERVAL, entity.getScheduleInterval(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SERVICE_NAME, entity.getServiceName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_COMPONENT_NAME, entity.getComponentName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_ENABLED, Boolean.valueOf(entity.getEnabled()), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SCOPE, entity.getScope(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SOURCE_TYPE, entity.getSourceType(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_LABEL, entity.getLabel(),
        requestedIds);

    if (!isCollection && null != resource.getPropertyValue(ALERT_DEF_SOURCE_TYPE)) {

      try {
        Map<String, String> map = gson.<Map<String, String>>fromJson(entity.getSource(), Map.class);

        for (Entry<String, String> entry : map.entrySet()) {
          String subProp = PropertyHelper.getPropertyId(ALERT_DEF_SOURCE, entry.getKey());
          resource.setProperty(subProp, entry.getValue());
        }
      } catch (Exception e) {
        LOG.error("Could not coerce alert JSON into a type");
      }
    }

    return resource;
  }

}
