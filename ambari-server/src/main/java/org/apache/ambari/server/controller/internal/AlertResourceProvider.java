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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Cluster;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * ResourceProvider for Alert instances
 */
public class AlertResourceProvider extends ReadOnlyResourceProvider {

  public static final String ALERT_STATE = "Alert/state";
  public static final String ALERT_ORIGINAL_TIMESTAMP = "Alert/original_timestamp";
  public static final String ALERT_ID = "Alert/id";
  public static final String ALERT_NAME = "Alert/name";

  protected static final String ALERT_CLUSTER_NAME = "Alert/cluster_name";
  protected static final String ALERT_LATEST_TIMESTAMP = "Alert/latest_timestamp";
  protected static final String ALERT_MAINTENANCE_STATE = "Alert/maintenance_state";
  protected static final String ALERT_INSTANCE = "Alert/instance";
  protected static final String ALERT_LABEL = "Alert/label";
  protected static final String ALERT_TEXT = "Alert/text";
  protected static final String ALERT_COMPONENT = "Alert/component_name";
  protected static final String ALERT_HOST = "Alert/host_name";
  protected static final String ALERT_SERVICE = "Alert/service_name";
  protected static final String ALERT_SCOPE = "Alert/scope";

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(ALERT_ID, ALERT_NAME));

  private static AlertsDAO alertsDAO = null;

  /**
   * @param injector the injector
   */
  @Inject
  public static void init(Injector injector) {
    alertsDAO = injector.getInstance(AlertsDAO.class);
  }

  AlertResourceProvider(Set<String> propertyIds,
      Map<Resource.Type, String> keyPropertyIds,
      AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    Set<Resource> results = new HashSet<Resource>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {

      String clusterName = (String) propertyMap.get(ALERT_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException("Invalid argument, cluster name is required");
      }

      String id = (String) propertyMap.get(ALERT_ID);
      if (null != id) {
        AlertCurrentEntity entity = alertsDAO.findCurrentById(Long.parseLong(id));

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

        String serviceName = (String) propertyMap.get(ALERT_SERVICE);
        String hostName = (String) propertyMap.get(ALERT_HOST);

        List<AlertCurrentEntity> entities = null;

        if (null != hostName) {
          entities = alertsDAO.findCurrentByHost(cluster.getClusterId(),
              hostName);
        } else if (null != serviceName) {
          entities = alertsDAO.findCurrentByService(cluster.getClusterId(),
              serviceName);
        } else {
          entities = alertsDAO.findCurrentByCluster(cluster.getClusterId());
        }

        if (null == entities) {
          entities = Collections.emptyList();
        }

        for (AlertCurrentEntity entity : entities) {
          results.add(toResource(true, clusterName, entity, requestPropertyIds));
        }
      }
    }

    return results;
  }

  /**
   * Converts an entity to a resource.
   *
   * @param isCollection {@code true} if the response is for a collection
   * @param clusterName the cluster name
   * @param entity the entity
   * @param requestedIds the requested ids
   * @return the resource
   */
  private Resource toResource(boolean isCollection, String clusterName,
      AlertCurrentEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.Alert);

    setResourceProperty(resource, ALERT_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, ALERT_ID, entity.getAlertId(), requestedIds);
    setResourceProperty(resource, ALERT_LATEST_TIMESTAMP, entity.getLatestTimestamp(), requestedIds);
    setResourceProperty(resource, ALERT_MAINTENANCE_STATE, entity.getMaintenanceState(), requestedIds);
    setResourceProperty(resource, ALERT_ORIGINAL_TIMESTAMP, entity.getOriginalTimestamp(), requestedIds);
    setResourceProperty(resource, ALERT_TEXT, entity.getLatestText(), requestedIds);

    AlertHistoryEntity history = entity.getAlertHistory();
    setResourceProperty(resource, ALERT_INSTANCE, history.getAlertInstance(), requestedIds);
    setResourceProperty(resource, ALERT_LABEL, history.getAlertLabel(), requestedIds);
    setResourceProperty(resource, ALERT_STATE, history.getAlertState(), requestedIds);
    setResourceProperty(resource, ALERT_COMPONENT, history.getComponentName(), requestedIds);
    setResourceProperty(resource, ALERT_HOST, history.getHostName(), requestedIds);
    setResourceProperty(resource, ALERT_SERVICE, history.getServiceName(), requestedIds);

    AlertDefinitionEntity definition = history.getAlertDefinition();
    setResourceProperty(resource, ALERT_NAME, definition.getDefinitionName(), requestedIds);
    setResourceProperty(resource, ALERT_SCOPE, definition.getScope(), requestedIds);

    if (isCollection) {
      // !!! want name to be populated as if it were a PK when requesting the collection
      resource.setProperty(ALERT_NAME, definition.getDefinitionName());
    }

    return resource;
  }
}
