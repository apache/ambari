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
package org.apache.ambari.server.alerts;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.services.AmbariServerAlertService;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;

/**
 * The {@link StaleAlertRunnable} is used by the
 * {@link AmbariServerAlertService} to check the last time that an alert was
 * checked and determine if it seems to no longer be running. It will produce a
 * single alert with {@link AlertState#CRITICAL} along with a textual
 * description of the alerts that are stale.
 */
public class StaleAlertRunnable extends AlertRunnable {
  /**
   * The message for the alert when all services have run in their designated
   * intervals.
   */
  private static final String ALL_ALERTS_CURRENT_MSG = "All alerts have run within their time intervals.";

  /**
   * The message to use when alerts are detected as stale.
   */
  private static final String STALE_ALERTS_MSG = "There are {0} stale alerts from {1} host(s):\n{2}";

  private static final String TIMED_LABEL_MSG = "{0} ({1})";

  private static final String HOST_LABEL_MSG = "{0}\n  [{1}]";

  /**
   * Convert the minutes for the delay of an alert into milliseconds.
   */
  private static final long MINUTE_TO_MS_CONVERSION = 60L * 1000L;

  private static final long MILLISECONDS_PER_MINUTE = 1000L * 60L;
  private static final int MINUTES_PER_DAY = 24 * 60;
  private static final int MINUTES_PER_HOUR = 60;

  /**
   * Used to get the current alerts and the last time they ran.
   */
  @Inject
  private AlertsDAO m_alertsDao;

  /**
   * Constructor.
   *
   * @param definitionName
   */
  public StaleAlertRunnable(String definitionName) {
    super(definitionName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  List<Alert> execute(Cluster cluster, AlertDefinitionEntity myDefinition) {
    Set<String> staleAlerts = new TreeSet<String>();
    Map<String, Set<String>> staleHostAlerts = new HashMap<>();
    Set<String> hostsWithStaleAlerts = new TreeSet<>();

    // get the cluster's current alerts
    List<AlertCurrentEntity> currentAlerts = m_alertsDao.findCurrentByCluster(
        cluster.getClusterId());

    long now = System.currentTimeMillis();

    // for each current alert, check to see if the last time it ran is
    // more than 2x its interval value (indicating it hasn't run)
    for (AlertCurrentEntity current : currentAlerts) {
      AlertHistoryEntity history = current.getAlertHistory();
      AlertDefinitionEntity currentDefinition = history.getAlertDefinition();

      // skip aggregates as they are special
      if (currentDefinition.getSourceType() == SourceType.AGGREGATE) {
          continue;
        }

      // skip alerts in maintenance mode
      if (current.getMaintenanceState() != MaintenanceState.OFF) {
        continue;
      }

      // skip alerts that have not run yet
      if (current.getLatestTimestamp() == 0) {
        continue;
      }

      // skip this alert (who watches the watchers)
      if (currentDefinition.getDefinitionName().equals(m_definitionName)) {
        continue;
      }

      // convert minutes to milliseconds for the definition's interval
      long intervalInMillis = currentDefinition.getScheduleInterval() * MINUTE_TO_MS_CONVERSION;

      // if the last time it was run is >= 2x the interval, it's stale
      long timeDifference = now - current.getLatestTimestamp();
      if (timeDifference >= 2 * intervalInMillis) {

        // it is technically possible to have a null/blank label; if so,
        // default to the name of the definition
        String label = currentDefinition.getLabel();
        if (StringUtils.isEmpty(label)) {
          label = currentDefinition.getDefinitionName();
          }

        if (null != history.getHostName()) {
          // keep track of the host, if not null
          String hostName = history.getHostName();
          hostsWithStaleAlerts.add(hostName);
          if (!staleHostAlerts.containsKey(hostName)) {
            staleHostAlerts.put(hostName, new TreeSet<String>());
            }

          staleHostAlerts.get(hostName).add(MessageFormat.format(TIMED_LABEL_MSG, label,
              millisToHumanReadableStr(timeDifference)));
        } else {
          // non host alerts
          staleAlerts.add(label);
          }
        }
    }

    for (String host : staleHostAlerts.keySet()) {
      staleAlerts.add(MessageFormat.format(HOST_LABEL_MSG, host,
          StringUtils.join(staleHostAlerts.get(host), ",\n  ")));
    }

    AlertState alertState = AlertState.OK;
    String alertText = ALL_ALERTS_CURRENT_MSG;

    // if there are stale alerts, mark as CRITICAL with the list of
    // alerts
    if (!staleAlerts.isEmpty()) {
      alertState = AlertState.CRITICAL;
      alertText = MessageFormat.format(STALE_ALERTS_MSG, staleAlerts.size(),
          hostsWithStaleAlerts.size(), StringUtils.join(staleAlerts, ",\n"));
    }

    Alert alert = new Alert(myDefinition.getDefinitionName(), null, myDefinition.getServiceName(),
        myDefinition.getComponentName(), null, alertState);

    alert.setLabel(myDefinition.getLabel());
    alert.setText(alertText);
    alert.setTimestamp(now);
    alert.setCluster(cluster.getClusterName());

    return Collections.singletonList(alert);
  }

  /**
   * Converts given {@code milliseconds} to human-readable {@link String} like "1d 2h 3m" or "2h 4m".
   * @param milliseconds milliseconds to convert
   * @return human-readable string
   */
  private static String millisToHumanReadableStr(long milliseconds){
    int min, hour, days;
    min = (int)(milliseconds / MILLISECONDS_PER_MINUTE);
    days = min / MINUTES_PER_DAY;
    min = min % MINUTES_PER_DAY;
    hour = min / MINUTES_PER_HOUR;
    min = min % MINUTES_PER_HOUR;
    String result = "";
    if(days > 0) {
      result += days + "d ";
    }
    if(hour > 0) {
      result += hour + "h ";
    }
    if(min > 0) {
      result += min + "m ";
    }
    return result.trim();
  }
}

