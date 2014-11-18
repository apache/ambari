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
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.AlertExecutionCommand;
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
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

/**
 * ResourceProvider for Alert Definitions
 */
@StaticallyInject
public class AlertDefinitionResourceProvider extends AbstractControllerResourceProvider {

  protected static final String ALERT_DEF = "AlertDefinition";

  protected static final String ALERT_DEF_CLUSTER_NAME = "AlertDefinition/cluster_name";
  protected static final String ALERT_DEF_ID = "AlertDefinition/id";
  protected static final String ALERT_DEF_NAME = "AlertDefinition/name";
  protected static final String ALERT_DEF_LABEL = "AlertDefinition/label";
  protected static final String ALERT_DEF_DESCRIPTION = "AlertDefinition/description";
  protected static final String ALERT_DEF_INTERVAL = "AlertDefinition/interval";
  protected static final String ALERT_DEF_SERVICE_NAME = "AlertDefinition/service_name";
  protected static final String ALERT_DEF_COMPONENT_NAME = "AlertDefinition/component_name";
  protected static final String ALERT_DEF_ENABLED = "AlertDefinition/enabled";
  protected static final String ALERT_DEF_SCOPE = "AlertDefinition/scope";
  protected static final String ALERT_DEF_IGNORE_HOST = "AlertDefinition/ignore_host";

  protected static final String ALERT_DEF_SOURCE = "AlertDefinition/source";
  protected static final String ALERT_DEF_SOURCE_TYPE = "AlertDefinition/source/type";

  protected static final String ALERT_DEF_ACTION_RUN_NOW = "AlertDefinition/run_now";

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(ALERT_DEF_ID, ALERT_DEF_NAME));

  private static Gson gson = new Gson();

  @Inject
  private static AlertDefinitionHash alertDefinitionHash;

  @Inject
  private static AlertDefinitionDAO alertDefinitionDAO = null;

  /**
   * Used for coercing an {@link AlertDefinitionEntity} to an
   * {@link AlertDefinition}.
   */
  @Inject
  private static AlertDefinitionFactory definitionFactory;

  /**
   * Used to enqueue commands to send to the agents when running an alert
   * on-demand.
   */
  @Inject
  private static ActionQueue actionQueue;

  /**
   * The property ids for an alert defintion resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  /**
   * The key property ids for an alert definition resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();

  static {
    // properties
    PROPERTY_IDS.add(ALERT_DEF_CLUSTER_NAME);
    PROPERTY_IDS.add(ALERT_DEF_SERVICE_NAME);
    PROPERTY_IDS.add(ALERT_DEF_COMPONENT_NAME);
    PROPERTY_IDS.add(ALERT_DEF_ID);
    PROPERTY_IDS.add(ALERT_DEF_NAME);
    PROPERTY_IDS.add(ALERT_DEF_LABEL);
    PROPERTY_IDS.add(ALERT_DEF_DESCRIPTION);
    PROPERTY_IDS.add(ALERT_DEF_INTERVAL);
    PROPERTY_IDS.add(ALERT_DEF_ENABLED);
    PROPERTY_IDS.add(ALERT_DEF_SCOPE);
    PROPERTY_IDS.add(ALERT_DEF_IGNORE_HOST);
    PROPERTY_IDS.add(ALERT_DEF_SOURCE);
    PROPERTY_IDS.add(ALERT_DEF_ACTION_RUN_NOW);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.AlertDefinition, ALERT_DEF_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, ALERT_DEF_CLUSTER_NAME);
  }

  /**
   * Constructor.
   *
   * @param controller
   */
  AlertDefinitionResourceProvider(AmbariManagementController controller) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
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
    Clusters clusters = getManagementController().getClusters();

    // check the predicate to see if there is a reques to run
    // the alert definition immediately
    if( null != predicate ){
      Set<Map<String,Object>> predicateMaps = getPropertyMaps(predicate);
      for (Map<String, Object> propertyMap : predicateMaps) {
        String runNow = (String) propertyMap.get(ALERT_DEF_ACTION_RUN_NOW);
        if (null != runNow) {
          if (Boolean.valueOf(runNow) == Boolean.TRUE) {
            scheduleImmediateAlert(propertyMap);
          }
        }
      }
    }

    // if an AlertDefinition property body was specified, perform the update\
    Set<String> invalidatedHosts = new HashSet<String>();
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

    // build alert definition commands for all agent hosts affected; only
    // update agents and broadcast update event if there are actual invalidated
    // hosts
    if (invalidatedHosts.size() > 0) {
      alertDefinitionHash.enqueueAgentCommands(clusterName, invalidatedHosts);
      notifyUpdate(Resource.Type.AlertDefinition, request, predicate);
    }

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
    String description = (String) requestMap.get(ALERT_DEF_DESCRIPTION);
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

    Boolean ignoreHost = null;
    if (requestMap.containsKey(ALERT_DEF_IGNORE_HOST)) {
      ignoreHost = Boolean.parseBoolean((String) requestMap.get(ALERT_DEF_IGNORE_HOST));
    } else if (bCreate) {
      ignoreHost = Boolean.FALSE;
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

    // on creation, source type is required
    if (bCreate && null == sourceType) {
      throw new IllegalArgumentException(String.format(
          "Source type must be specified and one of %s",
          EnumSet.allOf(SourceType.class)));
    }

    // !!! The AlertDefinition "Source" field is a nested JSON object;
    // build a JSON representation from the flat properties and then
    // serialize that JSON object as a string
    Map<String,JsonObject> jsonObjectMapping = new HashMap<String, JsonObject>();

    // for every property in the request, if it's a source property, then
    // add it to the JSON model
    for (Entry<String, Object> entry : requestMap.entrySet()) {
      String propertyKey = entry.getKey();

      // only handle "source" subproperties
      if (!propertyKey.startsWith(ALERT_DEF_SOURCE)) {
        continue;
      }

      // gets a JSON object to add the property to; this will create the
      // property and all of its parent properties recursively if necessary
      JsonObject jsonObject = getJsonObjectMapping(ALERT_DEF_SOURCE,
          jsonObjectMapping, propertyKey);

      String propertyName = PropertyHelper.getPropertyName(propertyKey);
      Object entryValue = entry.getValue();

      if( entryValue instanceof Collection<?> ){
        JsonElement jsonElement = gson.toJsonTree(entryValue);
        jsonObject.add(propertyName, jsonElement);
      } else {
        if (entryValue instanceof Number) {
          jsonObject.addProperty(propertyName, (Number) entryValue);
        } else {
          jsonObject.addProperty(propertyName, entryValue.toString());
        }
      }
    }

    // "source" must be filled in when creating
    JsonObject source = jsonObjectMapping.get(ALERT_DEF_SOURCE);
    if (bCreate && (null == source || 0 == source.entrySet().size())) {
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

    if (null != description) {
      entity.setDescription(description);
    }

    if (null != enabled) {
      entity.setEnabled(enabled.booleanValue());
    }

    if (null != ignoreHost) {
      entity.setHostIgnored(ignoreHost.booleanValue());
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

  /**
   * Gets a {@link JsonObject} that the specified flattened property should be
   * set on, creating all parent {@link JsonObject} instances as needed. This
   * will turn {code AlertDefinition/source/reporting/critical/text} into
   *
   * <pre>
   * AlertDefinition/source: {
   *   reporting: {
   *     critical: {
   *       text
   *     }
   *   }
   * }
   * </pre>
   *
   * Where the the following {@link JsonObject} instances are created:
   *
   * <pre>
   * AlertDefinition/source
   *   Reporting
   *     Critical
   *       property 'text',
   *       property 'value',
   *       etc
   * </pre>
   *
   * @param root
   *          the flattened property which will be considered the root.
   * @param jsonObjectMapping
   *          a mapping of flattened property to {@link JsonObject}.
   * @param propertyKey
   *          the key to get the {@link JsonObject} for.
   * @return the {@link JsonObject} that cooresponds to the specified key.
   */
  private JsonObject getJsonObjectMapping(String root,
      Map<String, JsonObject> jsonObjectMapping, String propertyKey) {

    // if there is already a mapping for the key, return the mapping
    if (jsonObjectMapping.containsKey(propertyKey)) {
      return jsonObjectMapping.get(propertyKey);
    }

    if (root.equals(propertyKey)) {
      JsonObject jsonRoot = jsonObjectMapping.get(root);
      if (null == jsonRoot) {
        jsonRoot = new JsonObject();
        jsonObjectMapping.put(root, jsonRoot);
      }

      return jsonRoot;
    }

    String propertyCategory = PropertyHelper.getPropertyCategory(propertyKey);
    JsonObject categoryJson = jsonObjectMapping.get(propertyCategory);

    if (null == categoryJson) {
      JsonObject parent = getJsonObjectMapping(root, jsonObjectMapping,
          propertyCategory);

      categoryJson = new JsonObject();
      jsonObjectMapping.put(propertyCategory, categoryJson);

      String categoryName = PropertyHelper.getPropertyName(propertyCategory);
      parent.add(categoryName, categoryJson);
    }

    return categoryJson;
  }

  private Resource toResource(boolean isCollection, String clusterName,
      AlertDefinitionEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.AlertDefinition);
    resource.setProperty(ALERT_DEF_ID, entity.getDefinitionId());
    resource.setProperty(ALERT_DEF_CLUSTER_NAME, clusterName);
    resource.setProperty(ALERT_DEF_NAME, entity.getDefinitionName());
    resource.setProperty(ALERT_DEF_LABEL, entity.getLabel());

    setResourceProperty(resource, ALERT_DEF_DESCRIPTION, entity.getDescription(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_INTERVAL, entity.getScheduleInterval(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SERVICE_NAME, entity.getServiceName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_COMPONENT_NAME, entity.getComponentName(), requestedIds);
    setResourceProperty(resource, ALERT_DEF_ENABLED, Boolean.valueOf(entity.getEnabled()), requestedIds);
    setResourceProperty(resource, ALERT_DEF_IGNORE_HOST, Boolean.valueOf(entity.isHostIgnored()), requestedIds);
    setResourceProperty(resource, ALERT_DEF_SCOPE, entity.getScope(), requestedIds);

    boolean sourceTypeRequested = setResourceProperty(resource,
        ALERT_DEF_SOURCE_TYPE, entity.getSourceType(), requestedIds);

    if (sourceTypeRequested
        && null != resource.getPropertyValue(ALERT_DEF_SOURCE_TYPE)) {
      try {
        Map<String, String> map = gson.<Map<String, String>> fromJson(
            entity.getSource(), Map.class);

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

  /**
   * Extracts an {@link AlertDefinitionEntity} from the property map and
   * enqueues an {@link AlertExecutionCommand} for every host that should re-run
   * the specified definition.
   *
   * @param propertyMap
   */
  private void scheduleImmediateAlert(Map<String, Object> propertyMap) {
    Clusters clusters = getManagementController().getClusters();
    String stringId = (String) propertyMap.get(ALERT_DEF_ID);
    long id = Long.parseLong(stringId);

    AlertDefinitionEntity entity = alertDefinitionDAO.findById(id);
    if (null == entity) {
      LOG.error("Unable to lookup alert definition with ID {}", id);
      return;
    }

    Cluster cluster = null;
    try {
      cluster = clusters.getClusterById(entity.getClusterId());
    } catch (AmbariException ambariException) {
      LOG.error("Unable to lookup cluster with ID {}", entity.getClusterId(),
          ambariException);
      return;
    }

    Set<String> hostNames = alertDefinitionHash.getAssociatedHosts(cluster,
        entity.getSourceType(),
        entity.getDefinitionName(), entity.getServiceName(),
        entity.getComponentName());

    for (String hostName : hostNames) {
      AlertDefinition definition = definitionFactory.coerce(entity);
      AlertExecutionCommand command = new AlertExecutionCommand(
          cluster.getClusterName(), hostName, definition);

      actionQueue.enqueue(hostName, command);
    }
  }
}
