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

import java.text.MessageFormat;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.AlertReceivedEvent;
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
 * {@link AlertReceivedEvent} instances and determine if there exists a
 * {@link SourceType#AGGREGATE} alert that needs to run.
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
   * Consume an alert that was received.
   */
  @Subscribe
  public void onAlertEvent(AlertReceivedEvent event) {
    AlertDefinition def = m_aggregateMapping.getAggregateDefinition(
        event.getClusterId(), event.getAlert().getName());

    if (null == def || null == m_alertsDao) {
      return;
    }

    AggregateSource as = (AggregateSource) def.getSource();

    AlertSummaryDTO summary = m_alertsDao.findAggregateCounts(
        event.getClusterId(), as.getAlertName());

    Alert alert = new Alert(def.getName(), null, def.getServiceName(),
        null, null, AlertState.UNKNOWN);

    alert.setLabel(def.getLabel());
    alert.setTimestamp(System.currentTimeMillis());

    if (0 == summary.getOkCount()) {
      alert.setText("Cannot determine, there are no records");
    } else if (summary.getUnknownCount() > 0) {
      alert.setText("There are alerts with status UNKNOWN.");
    } else {
      Reporting reporting = as.getReporting();

      int numerator = summary.getCriticalCount() + summary.getWarningCount();
      int denominator = summary.getOkCount();
      double value = (double)(numerator) / denominator;

      if (value > reporting.getCritical().getValue().doubleValue()) {
        alert.setState(AlertState.CRITICAL);
        alert.setText(MessageFormat.format(reporting.getCritical().getText(),
            Integer.valueOf(denominator), Integer.valueOf(numerator)));

      } else if (value > reporting.getWarning().getValue().doubleValue()) {
        alert.setState(AlertState.WARNING);
        alert.setText(MessageFormat.format(reporting.getWarning().getText(),
            Integer.valueOf(denominator), Integer.valueOf(numerator)));

      } else {
        alert.setState(AlertState.OK);
        alert.setText(MessageFormat.format(reporting.getOk().getText(),
            Integer.valueOf(denominator), Integer.valueOf(numerator)));
      }

    }

    // make a new event and allow others to consume it
    AlertReceivedEvent aggEvent = new AlertReceivedEvent(event.getClusterId(), alert);

    m_publisher.publish(aggEvent);
  }
}