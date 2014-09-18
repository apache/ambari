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

import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertReceivedListener} class handles {@link AlertReceivedEvent}
 * and updates the appropriate DAOs. It may also fire new
 * {@link AlertStateChangeEvent} when an {@link AlertState} change is detected.
 */
@Singleton
public class AlertReceivedListener {
  /**
   * Logger.
   */
  private static Log LOG = LogFactory.getLog(AlertReceivedListener.class);

  @Inject
  private AlertsDAO m_alertsDao;

  @Inject
  private AlertDefinitionDAO m_definitionDao;

  /**
   * Receives and publishes {@link AlertEvent} instances.
   */
  private AlertEventPublisher m_alertEventPublisher;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertReceivedListener(AlertEventPublisher publisher) {
    m_alertEventPublisher = publisher;
    m_alertEventPublisher.register(this);
  }


  /**
   * Adds an alert.  Checks for a new state before creating a new history record.
   *
   * @param clusterId the id for the cluster
   * @param alert the alert to add
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAlertEvent(AlertReceivedEvent event) {
    LOG.debug(event);

    long clusterId = event.getClusterId();
    Alert alert = event.getAlert();

    AlertCurrentEntity current = m_alertsDao.findCurrentByHostAndName(clusterId,
        alert.getHost(), alert.getName());

    if (null == current) {
      AlertDefinitionEntity definition = m_definitionDao.findByName(clusterId,
          alert.getName());

      AlertHistoryEntity history = createHistory(clusterId, definition, alert);

      current = new AlertCurrentEntity();
      current.setAlertHistory(history);
      current.setLatestTimestamp(Long.valueOf(alert.getTimestamp()));
      current.setOriginalTimestamp(Long.valueOf(alert.getTimestamp()));

      m_alertsDao.create(current);

    } else if (alert.getState() == current.getAlertHistory().getAlertState()) {
      current.setLatestTimestamp(Long.valueOf(alert.getTimestamp()));
      current.setLatestText(alert.getText());

      m_alertsDao.merge(current);
    } else {
      AlertState oldState = current.getAlertHistory().getAlertState();

      // insert history, update current
      AlertHistoryEntity history = createHistory(clusterId,
          current.getAlertHistory().getAlertDefinition(), alert);

      // manually create the new history entity since we are merging into
      // an existing current entity
      m_alertsDao.create(history);

      current.setAlertHistory(history);
      current.setLatestTimestamp(Long.valueOf(alert.getTimestamp()));
      current.setOriginalTimestamp(Long.valueOf(alert.getTimestamp()));

      m_alertsDao.merge(current);

      // broadcast the alert changed event for other subscribers
      AlertStateChangeEvent alertChangedEvent = new AlertStateChangeEvent(
          event.getClusterId(), event.getAlert(), current.getAlertHistory(),
          oldState);

      m_alertEventPublisher.publish(alertChangedEvent);
    }
  }

  /**
   * Convenience to create a new alert.
   * @param clusterId the cluster id
   * @param definition the definition
   * @param alert the alert data
   * @return the new history record
   */
  private AlertHistoryEntity createHistory(long clusterId, AlertDefinitionEntity definition, Alert alert) {
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertDefinition(definition);
    history.setAlertInstance(alert.getInstance());
    history.setAlertLabel(alert.getLabel());
    history.setAlertState(alert.getState());
    history.setAlertText(alert.getText());
    history.setAlertTimestamp(Long.valueOf(alert.getTimestamp()));
    history.setClusterId(Long.valueOf(clusterId));
    history.setComponentName(alert.getComponent());
    history.setHostName(alert.getHost());
    history.setServiceName(alert.getService());

    return history;
  }
}
