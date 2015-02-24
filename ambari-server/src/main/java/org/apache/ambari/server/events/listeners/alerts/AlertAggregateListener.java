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

import java.text.MessageFormat;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.InitialAlertEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertSummaryDTO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.AggregateDefinitionMapping;
import org.apache.ambari.server.state.alert.AggregateSource;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.SourceType;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertAggregateListener} is used to listen for all incoming
 * {@link AlertStateChangeEvent} instances and determine if there exists a
 * {@link SourceType#AGGREGATE} alert that needs to run.
 * <p/>
 * This listener is only needed on state changes as aggregation of alerts is
 * only performed against the state of an alert and not the values that
 * contributed to that state.
 */
@Singleton
@EagerSingleton
public class AlertAggregateListener {

  @Inject
  private AlertsDAO m_alertsDao = null;

  /**
   * The event publisher used to receive incoming events and publish new events
   * when an aggregate alert is run.
   */
  private final AlertEventPublisher m_publisher;

  /**
   * Used for quick lookups of aggregate alerts.
   */
  @Inject
  private AggregateDefinitionMapping m_aggregateMapping;

  @Inject
  public AlertAggregateListener(AlertEventPublisher publisher) {
    m_publisher = publisher;
    m_publisher.register(this);
  }

  /**
   * Consumes an {@link InitialAlertEvent}.
   */
  @Subscribe
  public void onInitialAlertEvent(InitialAlertEvent event) {
    onAlertEvent(event.getClusterId(), event.getAlert());
  }

  /**
   * Consumes an {@link AlertStateChangeEvent}.
   */
  @Subscribe
  public void onAlertStateChangeEvent(AlertStateChangeEvent event) {
    onAlertEvent(event.getClusterId(), event.getAlert());
  }

  /**
   * Calculates the aggregate alert state for the aggregated alert specified.
   */
  private void onAlertEvent(long clusterId, Alert alert) {
    AlertDefinition aggregateDefinition = m_aggregateMapping.getAggregateDefinition(
        clusterId, alert.getName());

    if (null == aggregateDefinition || null == m_alertsDao) {
      return;
    }

    AggregateSource aggregateSource = (AggregateSource) aggregateDefinition.getSource();

    AlertSummaryDTO summary = m_alertsDao.findAggregateCounts(clusterId,
        aggregateSource.getAlertName());

    // OK should be based off of true OKs and those in maintenance mode
    int okCount = summary.getOkCount() + summary.getMaintenanceCount();

    int warningCount = summary.getWarningCount();
    int criticalCount = summary.getCriticalCount();
    int unknownCount = summary.getUnknownCount();
    int totalCount = okCount + warningCount + criticalCount + unknownCount;

    Alert aggregateAlert = new Alert(aggregateDefinition.getName(), null,
        aggregateDefinition.getServiceName(), null, null, AlertState.UNKNOWN);

    aggregateAlert.setLabel(aggregateDefinition.getLabel());
    aggregateAlert.setTimestamp(System.currentTimeMillis());

    if (0 == totalCount) {
      aggregateAlert.setText("There are no instances of the aggregated alert.");
    } else if (summary.getUnknownCount() > 0) {
      aggregateAlert.setText("There are alerts with a state of UNKNOWN.");
    } else {
      Reporting reporting = aggregateSource.getReporting();

      int numerator = summary.getCriticalCount() + summary.getWarningCount();
      int denominator = totalCount;

      double value = (double) (numerator) / denominator;

      if (value >= reporting.getCritical().getValue()) {
        aggregateAlert.setState(AlertState.CRITICAL);
        aggregateAlert.setText(MessageFormat.format(
            reporting.getCritical().getText(),
            denominator, numerator));

      } else if (value >= reporting.getWarning().getValue()) {
        aggregateAlert.setState(AlertState.WARNING);
        aggregateAlert.setText(MessageFormat.format(
            reporting.getWarning().getText(),
            denominator, numerator));

      } else {
        aggregateAlert.setState(AlertState.OK);
        aggregateAlert.setText(MessageFormat.format(
            reporting.getOk().getText(),
            denominator, numerator));
      }
    }

    // make a new event and allow others to consume it
    AlertReceivedEvent aggEvent = new AlertReceivedEvent(clusterId,
        aggregateAlert);

    m_publisher.publish(aggEvent);
  }
}