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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AlertHistoryRequest;
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
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Cluster;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * ResourceProvider for Alert History
 */
public class AlertHistoryResourceProvider extends
    AbstractControllerResourceProvider {

  public static final String ALERT_HISTORY = "AlertHistory";
  public static final String ALERT_HISTORY_DEFINITION_ID = "AlertHistory/definition_id";
  public static final String ALERT_HISTORY_DEFINITION_NAME = "AlertHistory/definition_name";
  public static final String ALERT_HISTORY_ID = "AlertHistory/id";
  public static final String ALERT_HISTORY_CLUSTER_NAME = "AlertHistory/cluster_name";
  public static final String ALERT_HISTORY_SERVICE_NAME = "AlertHistory/service_name";
  public static final String ALERT_HISTORY_COMPONENT_NAME = "AlertHistory/component_name";
  public static final String ALERT_HISTORY_HOSTNAME = "AlertHistory/host_name";
  public static final String ALERT_HISTORY_LABEL = "AlertHistory/label";
  public static final String ALERT_HISTORY_STATE = "AlertHistory/state";
  public static final String ALERT_HISTORY_TEXT = "AlertHistory/text";
  public static final String ALERT_HISTORY_TIMESTAMP = "AlertHistory/timestamp";
  public static final String ALERT_HISTORY_INSTANCE = "AlertHistory/instance";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(ALERT_HISTORY_ID));

  /**
   * Used for querying alert history.
   */
  private static AlertsDAO s_dao = null;

  /**
   * The property ids for an alert history resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  /**
   * The key property ids for an alert history resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS =
      new HashMap<Resource.Type, String>();

  static {
    // properties
    PROPERTY_IDS.add(ALERT_HISTORY_DEFINITION_ID);
    PROPERTY_IDS.add(ALERT_HISTORY_DEFINITION_NAME);
    PROPERTY_IDS.add(ALERT_HISTORY_ID);
    PROPERTY_IDS.add(ALERT_HISTORY_CLUSTER_NAME);
    PROPERTY_IDS.add(ALERT_HISTORY_SERVICE_NAME);
    PROPERTY_IDS.add(ALERT_HISTORY_COMPONENT_NAME);
    PROPERTY_IDS.add(ALERT_HISTORY_HOSTNAME);
    PROPERTY_IDS.add(ALERT_HISTORY_LABEL);
    PROPERTY_IDS.add(ALERT_HISTORY_STATE);
    PROPERTY_IDS.add(ALERT_HISTORY_TEXT);
    PROPERTY_IDS.add(ALERT_HISTORY_TIMESTAMP);
    PROPERTY_IDS.add(ALERT_HISTORY_INSTANCE);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.AlertHistory, ALERT_HISTORY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, ALERT_HISTORY_CLUSTER_NAME);
  }

  /**
   * Static intializer for Guice.
   *
   * @param instance
   */
  @Inject
  public static void init(Injector injector) {
    s_dao = injector.getInstance(AlertsDAO.class);
  }

  /**
   * Constructor.
   *
   * @param controller
   */
  AlertHistoryResourceProvider(AmbariManagementController controller) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Cluster cluster = null;
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);
    Set<Resource> results = new LinkedHashSet<Resource>();

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    for (Map<String, Object> propertyMap : propertyMaps) {
      String clusterName = (String) propertyMap.get(ALERT_HISTORY_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException(
            "Invalid argument, cluster name is required");
      }

      if (null == cluster) {
        try {
          cluster = getManagementController().getClusters().getCluster(
              clusterName);
        } catch (AmbariException e) {
          throw new NoSuchResourceException(
              "Parent Cluster resource doesn't exist", e);
        }
      }

      // if asking for a single ID, get that historical entry and continue
      // the for-loop
      String id = (String) propertyMap.get(ALERT_HISTORY_ID);
      if (null != id) {
        AlertHistoryEntity entity = s_dao.findById(Long.parseLong(id));
        if (null != entity) {
          results.add(toResource(clusterName, entity, requestPropertyIds));
          continue;
        }
      }

      AlertHistoryRequest historyRequest = new AlertHistoryRequest();
      historyRequest.Predicate = predicate;

      List<AlertHistoryEntity> entities = s_dao.findAll(historyRequest);
      for (AlertHistoryEntity entity : entities) {
        results.add(toResource(cluster.getClusterName(), entity,
            requestPropertyIds));
      }
    }

    return results;
  }

  /**
   * Converts the {@link AlertHistoryEntity} to a {@link Resource}.
   *
   * @param clusterName
   *          the name of the cluster (not {@code null}).
   * @param entity
   *          the entity to convert (not {@code null}).
   * @param requestedIds
   *          the properties requested (not {@code null}).
   * @return
   */
  private Resource toResource(String clusterName, AlertHistoryEntity entity,
      Set<String> requestedIds) {
    AlertDefinitionEntity definition = entity.getAlertDefinition();

    Resource resource = new ResourceImpl(Resource.Type.AlertHistory);
    resource.setProperty(ALERT_HISTORY_ID, entity.getAlertId());

    setResourceProperty(resource, ALERT_HISTORY_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_DEFINITION_ID, definition.getDefinitionId(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_DEFINITION_NAME, definition.getDefinitionName(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_SERVICE_NAME, entity.getServiceName(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_COMPONENT_NAME, entity.getComponentName(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_HOSTNAME, entity.getHostName(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_LABEL, entity.getAlertLabel(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_STATE, entity.getAlertState(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_TEXT, entity.getAlertText(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_TIMESTAMP, entity.getAlertTimestamp(), requestedIds);
    setResourceProperty(resource, ALERT_HISTORY_INSTANCE, entity.getAlertInstance(), requestedIds);

    return resource;
  }
}
