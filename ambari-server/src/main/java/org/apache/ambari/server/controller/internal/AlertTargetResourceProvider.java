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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
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
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.alert.AlertTarget;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link AlertTargetResourceProvider} class deals with managing the CRUD
 * operations for {@link AlertTarget}, including property coercion to and from
 * {@link AlertTargetEntity}.
 */
public class AlertTargetResourceProvider extends
 AbstractResourceProvider {

  protected static final String ALERT_TARGET = "AlertTarget";
  protected static final String ALERT_TARGET_ID = "AlertTarget/id";
  protected static final String ALERT_TARGET_NAME = "AlertTarget/name";
  protected static final String ALERT_TARGET_DESCRIPTION = "AlertTarget/description";
  protected static final String ALERT_TARGET_NOTIFICATION_TYPE = "AlertTarget/notification_type";
  protected static final String ALERT_TARGET_PROPERTIES = "AlertTarget/properties";
  protected static final String ALERT_TARGET_GROUPS = "AlertTarget/groups";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(ALERT_TARGET_ID, ALERT_TARGET_NAME));

  /**
   * The property ids for an alert target resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  /**
   * The key property ids for an alert target resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();

  static {
    // properties
    PROPERTY_IDS.add(ALERT_TARGET_ID);
    PROPERTY_IDS.add(ALERT_TARGET_NAME);
    PROPERTY_IDS.add(ALERT_TARGET_DESCRIPTION);
    PROPERTY_IDS.add(ALERT_TARGET_NOTIFICATION_TYPE);
    PROPERTY_IDS.add(ALERT_TARGET_PROPERTIES);
    PROPERTY_IDS.add(ALERT_TARGET_GROUPS);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.AlertTarget, ALERT_TARGET_ID);
  }

  /**
   * Target DAO
   */
  @Inject
  private static AlertDispatchDAO s_dao;

  /**
   * Used for serializationa and deserialization of some fields.
   */
  private static final Gson s_gson = new Gson();

  /**
   * Initializes the injectable members of this class with the specified
   * injector.
   *
   * @param injector
   *          the injector (not {@code null}).
   */
  @Inject
  public static void init(Injector injector) {
    s_dao = injector.getInstance(AlertDispatchDAO.class);
  }

  /**
   * Constructor.
   */
  AlertTargetResourceProvider() {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createAlertTargets(request.getProperties());
        return null;
      }
    });

    notifyCreate(Resource.Type.AlertTarget, request);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<Resource>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    if( null == predicate ){
      List<AlertTargetEntity> entities = s_dao.findAllTargets();
      for (AlertTargetEntity entity : entities) {
        results.add(toResource(true, entity, requestPropertyIds));
      }
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        String id = (String) propertyMap.get(ALERT_TARGET_ID);
        if (null == id) {
          continue;
        }

        AlertTargetEntity entity = s_dao.findTargetById(Long.parseLong(id));
        if (null != entity) {
          results.add(toResource(false, entity, requestPropertyIds));
        }
      }
    }

    return results;
  }

  @Override
  public RequestStatus updateResources(final Request request,
      Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        updateAlertTargets(request.getProperties());
        return null;
      }
    });

    notifyUpdate(Resource.Type.AlertTarget, request, predicate);
    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = getResources(new RequestImpl(null, null, null,
        null), predicate);

    Set<Long> targetIds = new HashSet<Long>();

    for (final Resource resource : resources) {
      Long id = (Long) resource.getPropertyValue(ALERT_TARGET_ID);
      targetIds.add(id);
    }

    for (Long targetId : targetIds) {
      LOG.info("Deleting alert target {}", targetId);

      final AlertTargetEntity entity = s_dao.findTargetById(targetId.longValue());

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          s_dao.remove(entity);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.AlertTarget, predicate);
    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * Create and persist {@link AlertTargetEntity} from the map of properties.
   *
   * @param requestMaps
   * @throws AmbariException
   */
  private void createAlertTargets(Set<Map<String, Object>> requestMaps)
      throws AmbariException {
    List<AlertTargetEntity> entities = new ArrayList<AlertTargetEntity>();
    for (Map<String, Object> requestMap : requestMaps) {
      AlertTargetEntity entity = new AlertTargetEntity();

      String name = (String) requestMap.get(ALERT_TARGET_NAME);
      String description = (String) requestMap.get(ALERT_TARGET_DESCRIPTION);
      String notificationType = (String) requestMap.get(ALERT_TARGET_NOTIFICATION_TYPE);

      if (StringUtils.isEmpty(name)) {
        throw new IllegalArgumentException(
            "The name of the alert target is required.");
      }

      if (StringUtils.isEmpty(notificationType)) {
        throw new IllegalArgumentException(
            "The type of the alert target is required.");
      }

      String properties = extractProperties(requestMap);
      if (StringUtils.isEmpty(properties)) {
        throw new IllegalArgumentException(
            "Alert targets must be created with their connection properties");
      }

      entity.setDescription(description);
      entity.setNotificationType(notificationType);
      entity.setProperties(properties);
      entity.setTargetName(name);

      entities.add(entity);
    }

    s_dao.createTargets(entities);
  }

  /**
   * Updates existing {@link AlertTargetEntity}s with the specified properties.
   *
   * @param requestMaps
   *          a set of property maps, one map for each entity.
   * @throws AmbariException
   *           if the entity could not be found.
   */
  private void updateAlertTargets(Set<Map<String, Object>> requestMaps)
      throws AmbariException {

    for (Map<String, Object> requestMap : requestMaps) {
      String stringId = (String) requestMap.get(ALERT_TARGET_ID);

      if (StringUtils.isEmpty(stringId)) {
        throw new IllegalArgumentException(
            "The ID of the alert target is required when updating an existing target");
      }

      long id = Long.parseLong(stringId);
      AlertTargetEntity entity = s_dao.findTargetById(id);

      if (null == entity) {
        String message = MessageFormat.format(
            "The alert target with ID {0} could not be found", id);
        throw new AmbariException(message);
      }

      String name = (String) requestMap.get(ALERT_TARGET_NAME);
      String description = (String) requestMap.get(ALERT_TARGET_DESCRIPTION);
      String notificationType = (String) requestMap.get(ALERT_TARGET_NOTIFICATION_TYPE);

      if (!StringUtils.isBlank(name)) {
        entity.setTargetName(name);
      }

      if (null != description) {
        entity.setDescription(description);
      }

      if (!StringUtils.isBlank(notificationType)) {
        entity.setNotificationType(notificationType);
      }

      String properties = extractProperties(requestMap);
      if (!StringUtils.isEmpty(properties)) {
        entity.setProperties(properties);
      }

      s_dao.merge(entity);
    }
  }

  /**
   * Convert the given {@link AlertTargetEntity} to a {@link Resource}.
   *
   * @param isCollection
   *          {@code true} if the resource is part of a collection. Some
   *          properties are not returned when a collection of targets is
   *          requested.
   * @param entity
   *          the entity to convert.
   * @param requestedIds
   *          the properties that were requested or {@code null} for all.
   * @return the resource representation of the entity (never {@code null}).
   */
  private Resource toResource(boolean isCollection, AlertTargetEntity entity,
      Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.AlertTarget);
    resource.setProperty(ALERT_TARGET_ID, entity.getTargetId());
    resource.setProperty(ALERT_TARGET_NAME, entity.getTargetName());

    resource.setProperty(ALERT_TARGET_DESCRIPTION, entity.getDescription());
    resource.setProperty(ALERT_TARGET_NOTIFICATION_TYPE,
        entity.getNotificationType());

    if (!isCollection) {
      String properties = entity.getProperties();
      Map<String, String> map = s_gson.<Map<String, String>> fromJson(
          properties,
          Map.class);

      resource.setProperty(ALERT_TARGET_PROPERTIES, map);
    }

    return resource;
  }

  /**
   * Looks through the flat list of propery keys in the supplied map and builds
   * a JSON string of key:value pairs for the {@link #ALERT_TARGET_PROPERTIES}
   * key.
   *
   * @param requestMap
   *          the map of flattened properties (not {@code null}).
   * @return the JSON representing the key/value pairs of all properties, or
   *         {@code null} if none.
   */
  private String extractProperties( Map<String, Object> requestMap ){
    Map<String, Object> normalizedMap = new HashMap<String, Object>(
        requestMap.size());

    for (Entry<String, Object> entry : requestMap.entrySet()) {
      String key = entry.getKey();
      String propCat = PropertyHelper.getPropertyCategory(key);

      if (propCat.equals(ALERT_TARGET_PROPERTIES)) {
        String propKey = PropertyHelper.getPropertyName(key);
        normalizedMap.put(propKey, entry.getValue());
      }
    }

    return s_gson.toJson(normalizedMap);
  }
}
