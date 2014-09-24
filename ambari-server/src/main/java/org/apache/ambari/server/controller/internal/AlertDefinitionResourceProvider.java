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
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * ResourceProvider for Alert Definitions
 */
public class AlertDefinitionResourceProvider extends AbstractControllerResourceProvider {

  protected static final String ALERT_DEF = "AlertDefinition";

  protected static final String ALERT_DEF_CLUSTER_NAME = "AlertDefinition/cluster_name";
  protected static final String ALERT_DEF_ID = "AlertDefinition/id";
  protected static final String ALERT_DEF_NAME = "AlertDefinition/name";
  protected static final String ALERT_DEF_LABEL = "AlertDefinition/label";
  protected static final String ALERT_DEF_INTERVAL = "AlertDefinition/interval";
  protected static final String ALERT_DEF_SERVICE_NAME = "AlertDefinition/service_name";
  protected static final String ALERT_DEF_COMPONENT_NAME = "AlertDefinition/component_name";
  protected static final String ALERT_DEF_ENABLED = "AlertDefinition/enabled";
  protected static final String ALERT_DEF_SCOPE = "AlertDefinition/scope";

  protected static final String ALERT_DEF_SOURCE = "AlertDefinition/source";
  protected static final String ALERT_DEF_SOURCE_TYPE = "AlertDefinition/source/type";
  protected static final String ALERT_DEF_SOURCE_REPORTING = "AlertDefinition/source/reporting";
  protected static final String ALERT_DEF_SOURCE_REPORTING_OK = "AlertDefinition/source/reporting/ok";
  protected static final String ALERT_DEF_SOURCE_REPORTING_WARNING = "AlertDefinition/source/reporting/warning";
  protected static final String ALERT_DEF_SOURCE_REPORTING_CRITICAL = "AlertDefinition/source/reporting/critical";


  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(ALERT_DEF_ID, ALERT_DEF_NAME));

  private static AlertDefinitionDAO alertDefinitionDAO = null;

  private static Gson gson = new Gson();

  private static AlertDefinitionHash alertDefinitionHash;

  /**
   * @param instance
   */
  @Inject
  public static void init(Injector injector) {
    alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    alertDefinitionHash = injector.getInstance(AlertDefinitionHash.class);
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

    String clusterName = null;
    for (Map<String, Object> requestMap : requestMaps) {
      AlertDefinitionEntity entity = new AlertDefinitionEntity();
      populateEntity(entity, requestMap);
      entities.add(entity);

      if (null == clusterName) {
        clusterName = (String) requestMap.get(ALERT_DEF_CLUSTER_NAME);
      }
    }

    Set<String> invalidatedHosts = new HashSet<String>();

    // !!! TODO multi-create in a transaction
    for (AlertDefinitionEntity entity : entities) {
      alertDefinitionDAO.create(entity);
      invalidatedHosts.addAll(alertDefinitionHash.invalidateHosts(entity));
    }

    // build alert definition commands for all agent hosts affected
    alertDefinitionHash.enqueueAgentCommands(clusterName, invalidatedHosts);
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

    String clusterName = null;
    Set<String> invalidatedHosts = new HashSet<String>();
    Clusters clusters = getManagementController().getClusters();

    for (Map<String, Object> requestPropMap : request.getProperties()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(requestPropMap, predicate)) {
        String stringId = (String) propertyMap.get(ALERT_DEF_ID);
        long id = Long.parseLong(stringId);

        AlertDefinitionEntity entity = alertDefinitionDAO.findById(id);
        if (null == entity) {
          continue;
        }

        if (null == clusterName) {
          try {
            Cluster cluster = clusters.getClusterById(entity.getClusterId());
            if (null != cluster) {
              clusterName = cluster.getClusterName();
            }
          } catch (AmbariException ae) {
            throw new IllegalArgumentException("Invalid cluster ID", ae);
          }
        }

        try{
          populateEntity(entity, propertyMap);
          alertDefinitionDAO.merge(entity);
          invalidatedHosts.addAll(alertDefinitionHash.invalidateHosts(entity));
        }
        catch( AmbariException ae ){
          LOG.error("Unable to find cluster when updating alert definition", ae);
        }
      }
    }

    // build alert definition commands for all agent hosts affected
    alertDefinitionHash.enqueueAgentCommands(clusterName, invalidatedHosts);

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

    String clusterName = null;
    for (final Resource resource : resources) {
      Long id = (Long) resource.getPropertyValue(ALERT_DEF_ID);
      definitionIds.add(id);

      if (null == clusterName) {
        clusterName = (String) resource.getPropertyValue(ALERT_DEF_CLUSTER_NAME);
      }
    }

    final Set<String> invalidatedHosts = new HashSet<String>();
    for (Long definitionId : definitionIds) {
      LOG.info("Deleting alert definition {}", definitionId);

      final AlertDefinitionEntity entity = alertDefinitionDAO.findById(definitionId.longValue());

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          alertDefinitionDAO.remove(entity);
          invalidatedHosts.addAll(alertDefinitionHash.invalidateHosts(entity));
          return null;
        }
      });
    }

    // build alert definition commands for all agent hosts affected
    alertDefinitionHash.enqueueAgentCommands(clusterName, invalidatedHosts);

    notifyDelete(Resource.Type.AlertDefinition, predicate);
    return getRequestStatus(null);
  }

  /**
   * Merges the map of properties into the specified entity. If the entity is
   * being created, an {@link IllegalArgumentException} is thrown when a
   * required property is absent. When updating, missing properties are assume
   * to not have changed.
   *
   * @param entity
   *          the entity to merge the properties into (not {@code null}).
   * @param requestMap
   *          the map of properties (not {@code null}).
   * @throws AmbariException
   */
  private void populateEntity(AlertDefinitionEntity entity,
      Map<String, Object> requestMap) throws AmbariException {

    // some fields are required on creation; on update we keep what's there
    boolean bCreate = true;
    if (null != entity.getDefinitionId()) {
      bCreate = false;
    }

    String clusterName = (String) requestMap.get(ALERT_DEF_CLUSTER_NAME);
    String definitionName = (String) requestMap.get(ALERT_DEF_NAME);
    String serviceName = (String) requestMap.get(ALERT_DEF_SERVICE_NAME);
    String componentName = (String) requestMap.get(ALERT_DEF_COMPONENT_NAME);
    String type = (String) requestMap.get(ALERT_DEF_SOURCE_TYPE);
    String label = (String) requestMap.get(ALERT_DEF_LABEL);
    String desiredScope = (String) requestMap.get(ALERT_DEF_SCOPE);

    Integer interval = null;
    if (requestMap.containsKey(ALERT_DEF_INTERVAL)) {
      interval = Integer.valueOf((String) requestMap.get(ALERT_DEF_INTERVAL));
    }

    Boolean enabled = null;
    if (requestMap.containsKey(ALERT_DEF_ENABLED)) {
      enabled = Boolean.parseBoolean((String) requestMap.get(ALERT_DEF_ENABLED));
    } else if (bCreate) {
      enabled = Boolean.TRUE;
    }

    Scope scope = null;
    if (null != desiredScope && desiredScope.length() > 0) {
      scope = Scope.valueOf(desiredScope);
    }

    SourceType sourceType = null;
    if (null != type && type.length() > 0) {
      sourceType = SourceType.valueOf(type);
    }

    // if not specified when creating an alert definition, the scope is
    // assumed to be ANY
    if (null == scope && bCreate) {
      scope = Scope.ANY;
    }

    if (StringUtils.isEmpty(clusterName)) {
      throw new IllegalArgumentException(
          "Invalid argument, cluster name is required");
    }

    if (bCreate && !requestMap.containsKey(ALERT_DEF_INTERVAL)) {
      throw new IllegalArgumentException("Check interval must be specified");
    }

    if (bCreate && StringUtils.isEmpty(definitionName)) {
      throw new IllegalArgumentException("Definition name must be specified");
    }

    if (bCreate && StringUtils.isEmpty(serviceName)) {
      throw new IllegalArgumentException("Service name must be specified");
    }

    if (bCreate && null == sourceType) {
      throw new IllegalArgumentException(String.format(
          "Source type must be specified and one of %s",
          EnumSet.allOf(SourceType.class)));
    }

    // !!! Alert structures contain nested objects; reconstruct a valid
    // JSON from the flat, exploded properties so that a Source instance can
    // be properly persisted
    JsonObject source = new JsonObject();
    JsonObject reporting = new JsonObject();
    JsonObject reportingOk = new JsonObject();
    JsonObject reportingWarning = new JsonObject();
    JsonObject reportingCritical = new JsonObject();

    for (Entry<String, Object> entry : requestMap.entrySet()) {
      String propCat = PropertyHelper.getPropertyCategory(entry.getKey());
      String propName = PropertyHelper.getPropertyName(entry.getKey());

      if (propCat.equals(ALERT_DEF) && "source".equals(propName)) {
        source.addProperty(propName, entry.getValue().toString());
      }

      if (propCat.equals(ALERT_DEF_SOURCE)) {
        source.addProperty(propName, entry.getValue().toString());
      }

      if (propCat.equals(ALERT_DEF_SOURCE_REPORTING)) {
        reporting.addProperty(propName, entry.getValue().toString());
      }

      if (propCat.equals(ALERT_DEF_SOURCE_REPORTING_OK)) {
        reportingOk.addProperty(propName, entry.getValue().toString());
      }

      if (propCat.equals(ALERT_DEF_SOURCE_REPORTING_WARNING)) {
        reportingWarning.addProperty(propName, entry.getValue().toString());
      }

      if (propCat.equals(ALERT_DEF_SOURCE_REPORTING_CRITICAL)) {
        reportingCritical.addProperty(propName, entry.getValue().toString());
      }
    }

    if (reportingOk.entrySet().size() > 0) {
      reporting.add("ok", reportingOk);
    }

    if (reportingWarning.entrySet().size() > 0) {
      reporting.add("warning", reportingWarning);
    }

    if (reportingCritical.entrySet().size() > 0) {
      reporting.add("critical", reportingCritical);
    }

    if (reporting.entrySet().size() > 0) {
      source.add("reporting", reporting);
    }

    if (bCreate && 0 == source.entrySet().size()) {
      throw new IllegalArgumentException("Source must be specified");
    }

    Cluster cluster = getManagementController().getClusters().getCluster(
        clusterName);

    // at this point, we have either validated all required properties or
    // we are using the exiting entity properties where not defined, so we
    // can do simply null checks
    entity.setClusterId(Long.valueOf(cluster.getClusterId()));

    if (null != componentName) {
      entity.setComponentName(componentName);
    }

    if (null != definitionName) {
      entity.setDefinitionName(definitionName);
    }

    if (null != label) {
      entity.setLabel(label);
    }

    if (null != enabled) {
      entity.setEnabled(enabled.booleanValue());
    }

    if (null != interval) {
      entity.setScheduleInterval(interval);
    }

    if (null != serviceName) {
      entity.setServiceName(serviceName);
    }

    if (null != sourceType) {
      entity.setSourceType(sourceType);
    }

    if (null != source) {
      entity.setSource(source.toString());
    }

    if (null != scope) {
      entity.setScope(scope);
    }

    entity.setHash(UUID.randomUUID().toString());
  }

  private Resource toResource(boolean isCollection, String clusterName,
      AlertDefinitionEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.AlertDefinition);
    resource.setProperty(ALERT_DEF_ID, entity.getDefinitionId());
    resource.setProperty(ALERT_DEF_CLUSTER_NAME, clusterName);
    resource.setProperty(ALERT_DEF_NAME, entity.getDefinitionName());
    resource.setProperty(ALERT_DEF_LABEL, entity.getLabel());

    setResourceProperty(resource, ALERT_DEF_INTERVAL, entity.getScheduleInterval(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SERVICE_NAME, entity.getServiceName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_COMPONENT_NAME, entity.getComponentName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_ENABLED, Boolean.valueOf(entity.getEnabled()), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SCOPE, entity.getScope(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SOURCE_TYPE, entity.getSourceType(), requestedIds);

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
