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
package org.apache.ambari.server.events.listeners.alerts;

import java.util.List;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
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
    LOG.debug("Received event {}", event);

    List<AlertCurrentEntity> currentAlerts = m_alertsDao.findCurrent();

    MaintenanceState newMaintenanceState = MaintenanceState.OFF;
    if (event.getMaintenanceState() != MaintenanceState.OFF) {
      newMaintenanceState = MaintenanceState.ON;
    }

    for( AlertCurrentEntity currentAlert : currentAlerts ){
      AlertHistoryEntity history = currentAlert.getAlertHistory();

      String alertHostName = history.getHostName();
      String alertServiceName = history.getServiceName();
      String alertComponentName = history.getComponentName();

      try {
        Host host = event.getHost();
        Service service = event.getService();
        ServiceComponentHost serviceComponentHost = event.getServiceComponentHost();

        // host level maintenance
        if( null != host ){
          String hostName = host.getHostName();
          if( hostName.equals( alertHostName ) ){
            updateMaintenanceState(currentAlert, newMaintenanceState);
            continue;
          }
        } else if( null != service ){
          // service level maintenance
          String serviceName = service.getName();
          if( serviceName.equals(alertServiceName)){
            updateMaintenanceState(currentAlert, newMaintenanceState);
            continue;
          }
        } else if( null != serviceComponentHost ){
          // component level maintenance
          String hostName = serviceComponentHost.getHostName();
          String serviceName = serviceComponentHost.getServiceName();
          String componentName = serviceComponentHost.getServiceComponentName();

          // match on all 3 for a service component
          if (hostName.equals(alertHostName) && serviceName.equals(alertServiceName)
              && componentName.equals(alertComponentName)) {
            updateMaintenanceState(currentAlert, newMaintenanceState);
            continue;
          }
        }
      } catch (Exception exception) {
        AlertDefinitionEntity definition = history.getAlertDefinition();
        LOG.error("Unable to put alert '{}' for host {} into maintenance mode",
            definition.getDefinitionName(), alertHostName, exception);
      }
    }
  }

  /**
   * Updates the maintenance state of the specified alert if different than the
   * supplied maintenance state.
   *
   * @param currentAlert
   *          the alert to update (not {@code null}).
   * @param maintenanceState
   *          the maintenance state to change to, either
   *          {@link MaintenanceState#OFF} or {@link MaintenanceState#ON}.
   */
  private void updateMaintenanceState(AlertCurrentEntity currentAlert,
      MaintenanceState maintenanceState) {

    // alerts only care about OFF or ON
    if (maintenanceState != MaintenanceState.OFF && maintenanceState != MaintenanceState.ON) {
      LOG.warn("Unable to set invalid maintenance state of {} on the alert {}", maintenanceState,
          currentAlert.getAlertHistory().getAlertDefinition().getDefinitionName());

      return;
    }

    MaintenanceState currentState = currentAlert.getMaintenanceState();
    if (currentState == maintenanceState) {
      return;
    }

    currentAlert.setMaintenanceState(maintenanceState);
    m_alertsDao.merge(currentAlert);
  }
}
