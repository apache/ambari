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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertSummaryDTO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.AggregateSource;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.Reporting.ReportTemplate;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/**
 * Used to aggregate alerts. 
 */
public class AlertAggregateListener {

  @Inject
  private AlertsDAO m_alertsDao = null;

  private AlertEventPublisher m_publisher = null;
  private Map<Long, Map<String, AlertDefinition>> m_aggregateMap = 
      new HashMap<Long, Map<String, AlertDefinition>>();
  
  @Inject
  public AlertAggregateListener(AlertEventPublisher publisher) {
    m_publisher = publisher;

    publisher.register(this);
  }
  
  /**
   * Consume an alert that was received.
   */
  @Subscribe
  public void onAlertEvent(AlertReceivedEvent event) {
    AlertDefinition def = getAggregateDefinition(event.getClusterId(), event.getAlert().getName());
    
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
  
  private AlertDefinition getAggregateDefinition(long clusterId, String name) {
    Long id = Long.valueOf(clusterId);
    if (!m_aggregateMap.containsKey(id))
      return null;
    
    if (!m_aggregateMap.get(id).containsKey(name))
      return null;
    
    return m_aggregateMap.get(id).get(name);
  }

  /**
   * @param source the aggregate source
   */
  public void addAggregateType(long clusterId, AlertDefinition definition) {
    Long id = Long.valueOf(clusterId);
    
    if (!m_aggregateMap.containsKey(id)) {
      m_aggregateMap.put(id, new HashMap<String, AlertDefinition>());
    }
    
    Map<String, AlertDefinition> map = m_aggregateMap.get(id);
    
    AggregateSource as = (AggregateSource) definition.getSource();
    
    map.put(as.getAlertName(), definition);
  }
  
  
}