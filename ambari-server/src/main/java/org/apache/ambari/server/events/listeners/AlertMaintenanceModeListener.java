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
package org.apache.ambari.server.events.listeners;

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link AlertMaintenanceModeListener} handles events that relate to
 * Maintenance Mode changes.
 */
@Singleton
@EagerSingleton
public class AlertMaintenanceModeListener {
  /**
   * Logger.
   */
  private static Logger LOG = LoggerFactory.getLogger(AlertMaintenanceModeListener.class);

  /**
   * Used for updating the MM of current alerts.
   */
  @Inject
  private AlertsDAO m_alertsDao = null;

  /**
   * Used to assist in determining implied maintenance state.
   */
  @Inject
  private Provider<MaintenanceStateHelper> m_maintenanceHelper;

  /**
   * Used to lookup MM states.
   */
  @Inject
  private Provider<Clusters> m_clusters;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertMaintenanceModeListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Handles {@link MaintenanceModeEvent} by performing the following tasks:
   * <ul>
   * <li>Iterates through all {@link AlertNoticeEntity}, updating the MM state</li>
   * </ul>
   *
   * @param event
   *          the event being handled.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onEvent(MaintenanceModeEvent event) {
    List<AlertCurrentEntity> currentAlerts = m_alertsDao.findCurrent();

    for( AlertCurrentEntity currentAlert : currentAlerts ){
      MaintenanceState currentState = currentAlert.getMaintenanceState();
      AlertHistoryEntity history = currentAlert.getAlertHistory();
      AlertDefinitionEntity definition = history.getAlertDefinition();

      long clusterId = history.getClusterId();
      String hostName = history.getHostName();
      String serviceName = history.getServiceName();
      String componentName = history.getComponentName();

      try {
        Cluster cluster = m_clusters.get().getClusterById(clusterId);
        if (null == cluster) {
          LOG.warn("Unable to find cluster with ID {}", clusterId);
          continue;
        }

        Service service = cluster.getService(serviceName);
        if (null == service) {
          LOG.warn("Unable to find service named {} in cluster {}",
              serviceName, cluster.getClusterName());

          continue;
        }

        // if this is a service-level alert, then check explicitely against the
        // service for the MM state
        if (null == componentName) {
          MaintenanceState serviceState = service.getMaintenanceState();
          if (currentState != serviceState) {
            currentAlert.setMaintenanceState(serviceState);
            m_alertsDao.merge(currentAlert);
          }
        }
        // the presence of a component name means that it's a component alert
        // which require a host
        else {
          if (hostName == null) {
            LOG.warn("The alert {} for component {} must have a host",
                definition.getDefinitionName(), componentName);

            continue;
          }

          List<ServiceComponentHost> serviceComponentHosts = cluster.getServiceComponentHosts(hostName);
          if (null == serviceComponentHosts) {
            LOG.warn(
                "Unable to find service components on host {} for {} in cluster {}",
                hostName, serviceName, cluster.getClusterName());

            continue;
          }

          ServiceComponentHost serviceComponentHost = null;
          for (ServiceComponentHost sch : serviceComponentHosts) {
            if (componentName.equals(sch.getServiceComponentName())) {
              serviceComponentHost = sch;
              break;
            }
          }

          if (null == serviceComponentHost) {
            LOG.warn("Unable to find component {} of {} on host {}",
                componentName, serviceName, hostName);

            continue;
          }

          MaintenanceState effectiveState = m_maintenanceHelper.get().getEffectiveState(
              serviceComponentHost);

          switch (effectiveState) {
            case OFF:
              if (currentState != MaintenanceState.OFF) {
                currentAlert.setMaintenanceState(MaintenanceState.OFF);
                m_alertsDao.merge(currentAlert);
              }

              break;
            case ON:
            case IMPLIED_FROM_HOST:
            case IMPLIED_FROM_SERVICE:
            case IMPLIED_FROM_SERVICE_AND_HOST:
              if (currentState == MaintenanceState.OFF) {
                currentAlert.setMaintenanceState(MaintenanceState.ON);
                m_alertsDao.merge(currentAlert);
              }

              break;
            default:
              break;
          }
        }
      } catch (AmbariException ambariException) {
        LOG.error("Unable to put alert '{}' for host {} into maintenance mode",
            definition.getDefinitionName(), hostName, ambariException);
      }
    }
  }
}
