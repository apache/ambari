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
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.alert.AlertGroup;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link AlertGroupResourceProvider} class deals with managing the CRUD
 * operations for {@link AlertGroup}, including property coercion to and from
 * {@link AlertGroupEntity}.
 */
public class AlertGroupResourceProvider extends
    AbstractControllerResourceProvider {

  protected static final String ALERT_GROUP = "AlertGroup";
  protected static final String ALERT_GROUP_ID = "AlertGroup/id";
  protected static final String ALERT_GROUP_CLUSTER_NAME = "AlertGroup/cluster_name";
  protected static final String ALERT_GROUP_NAME = "AlertGroup/name";
  protected static final String ALERT_GROUP_DEFAULT = "AlertGroup/default";
  protected static final String ALERT_GROUP_DEFINITIONS = "AlertGroup/definitions";
  protected static final String ALERT_GROUP_TARGETS = "AlertGroup/targets";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(ALERT_GROUP_ID, ALERT_GROUP_CLUSTER_NAME));

  /**
   * Group DAO
   */
  @Inject
  private static AlertDispatchDAO s_dao;

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
   *
   * @param propertyIds
   * @param keyPropertyIds
   * @param managementController
   */
  AlertGroupResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createAlertGroups(request.getProperties());
        return null;
      }
    });

    notifyCreate(Resource.Type.AlertGroup, request);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<Resource>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(ALERT_GROUP_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException("The cluster name is required when retrieving alert groups");
      }

      String id = (String) propertyMap.get(ALERT_GROUP_ID);
      if (null != id) {
        AlertGroupEntity entity = s_dao.findGroupById(Long.parseLong(id));
        if (null != entity) {
          results.add(toResource(false, clusterName, entity, requestPropertyIds));
        }
      } else {
        Cluster cluster = null;

        try {
          cluster = getManagementController().getClusters().getCluster(clusterName);
        } catch (AmbariException ae) {
          throw new NoSuchResourceException("Parent Cluster resource doesn't exist", ae);
        }

        List<AlertGroupEntity> entities = s_dao.findAllGroups(cluster.getClusterId());

        for (AlertGroupEntity entity : entities) {
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

    throw new UnsupportedOperationException();
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = getResources(new RequestImpl(null, null, null,
        null), predicate);

    Set<Long> groupIds = new HashSet<Long>();

    for (final Resource resource : resources) {
      Long id = (Long) resource.getPropertyValue(ALERT_GROUP_ID);
      groupIds.add(id);
    }

    for (Long groupId : groupIds) {
      LOG.info("Deleting alert target {}", groupId);

      final AlertGroupEntity entity = s_dao.findGroupById(groupId.longValue());

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          s_dao.remove(entity);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.AlertGroup, predicate);
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
  private void createAlertGroups(Set<Map<String, Object>> requestMaps)
      throws AmbariException {

    List<AlertGroupEntity> entities = new ArrayList<AlertGroupEntity>();
    for (Map<String, Object> requestMap : requestMaps) {
      AlertGroupEntity entity = new AlertGroupEntity();

      String name = (String) requestMap.get(ALERT_GROUP_NAME);
      String clusterName = (String) requestMap.get(ALERT_GROUP_CLUSTER_NAME);

      if (StringUtils.isEmpty(name)) {
        throw new IllegalArgumentException(
            "The name of the alert group is required.");
      }

      if (StringUtils.isEmpty(clusterName)) {
        throw new IllegalArgumentException(
            "The name of the cluster is required when creating an alert group.");
      }

      Cluster cluster = getManagementController().getClusters().getCluster(
          clusterName);

      entity.setClusterId(cluster.getClusterId());
      entity.setDefault(false);
      entity.setGroupName(name);

      entities.add(entity);
    }

    s_dao.createGroups(entities);
  }

  /**
   * Convert the given {@link AlertGroupEntity} to a {@link Resource}.
   *
   * @param isCollection
   *          {@code true} if the resource is part of a collection.
   * @param entity
   *          the entity to convert.
   * @param requestedIds
   *          the properties that were requested or {@code null} for all.
   * @return the resource representation of the entity (never {@code null}).
   */
  private Resource toResource(boolean isCollection, String clusterName,
      AlertGroupEntity entity,
      Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.AlertGroup);
    resource.setProperty(ALERT_GROUP_ID, entity.getGroupId());
    resource.setProperty(ALERT_GROUP_NAME, entity.getGroupName());
    resource.setProperty(ALERT_GROUP_CLUSTER_NAME, clusterName);

    setResourceProperty(resource, ALERT_GROUP_DEFAULT,
        entity.isDefault(), requestedIds);

    return resource;
  }
}
