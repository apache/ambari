/*
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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
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
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.ParameterizedSource.AlertParameter;
import org.apache.ambari.server.state.alert.ServerSource;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.services.AmbariServerAlertService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(StaleAlertRunnable.class);

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
   * The multiplier for the interval of the definition which is being checked
   * for staleness. If this value is {@code 2}, then alerts are considered stale
   * if they haven't run in more than 2x their interval value.
   */
  private static final int INTERVAL_WAIT_FACTOR_DEFAULT = 2;

  /**
   * A parameter which exposes the interval multipler to use for calculating
   * staleness. If this does not exist, then
   * {@link #INTERVAL_WAIT_FACTOR_DEFAULT} will be used.
   */
  private static final String STALE_INTERVAL_MULTIPLIER_PARAM_KEY = "stale.interval.multiplier";

  /**
   * Used to get the current alerts and the last time they ran.
   */
  @Inject
  private AlertsDAO m_alertsDao;

  /**
   * Used for converting {@link AlertDefinitionEntity} into
   * {@link AlertDefinition} instances.
   */
  @Inject
  private AlertDefinitionFactory m_definitionFactory;

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
    // get the multiplier
    int waitFactor = getWaitFactorMultiplier(myDefinition);

    // use the uptime of the Ambari Server as a way to determine if we need to
    // give the alert more time to report in
    RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
    long uptime = rb.getUptime();

    int totalStaleAlerts = 0;
    Set<String> staleAlertGroupings = new TreeSet<>();
    Map<String, Set<String>> staleAlertsByHost = new HashMap<>();
    Set<String> hostsWithStaleAlerts = new TreeSet<>();

    // get the cluster's current alerts
    List<AlertCurrentEntity> currentAlerts = m_alertsDao.findCurrentByCluster(
        cluster.getClusterId());

    long now = System.currentTimeMillis();

    // for each current alert, check to see if the last time it ran is
    // more than INTERVAL_WAIT_FACTOR * its interval value (indicating it hasn't
    // run)
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

      // if the server hasn't been up long enough to consider this alert stale,
      // then don't mark it stale - this is to protect against cases where
      // Ambari was down for a while and after startup it hasn't received the
      // alert because it has a longer interval than this stale alert check:
      //
      // Stale alert check - every 5 minutes
      // Foo alert cehck - every 10 minutes
      // Ambari down for 35 minutes for upgrade
      if (uptime <= waitFactor * intervalInMillis) {
        continue;
      }

      // if the last time it was run is >= INTERVAL_WAIT_FACTOR * the interval,
      // it's stale
      long timeDifference = now - current.getLatestTimestamp();
      if (timeDifference >= waitFactor * intervalInMillis) {
        // increase the count
        totalStaleAlerts++;

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
          if (!staleAlertsByHost.containsKey(hostName)) {
            staleAlertsByHost.put(hostName, new TreeSet<>());
          }

          staleAlertsByHost.get(hostName).add(MessageFormat.format(TIMED_LABEL_MSG, label,
              millisToHumanReadableStr(timeDifference)));
        } else {
          // non host alerts
          staleAlertGroupings.add(label);
        }
      }
    }

    for (String host : staleAlertsByHost.keySet()) {
      staleAlertGroupings.add(MessageFormat.format(HOST_LABEL_MSG, host,
          StringUtils.join(staleAlertsByHost.get(host), ",\n  ")));
    }

    AlertState alertState = AlertState.OK;
    String alertText = ALL_ALERTS_CURRENT_MSG;

    // if there are stale alerts, mark as CRITICAL with the list of
    // alerts
    if (!staleAlertGroupings.isEmpty()) {
      alertState = AlertState.CRITICAL;
      alertText = MessageFormat.format(STALE_ALERTS_MSG, totalStaleAlerts,
          hostsWithStaleAlerts.size(), StringUtils.join(staleAlertGroupings, ",\n"));
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

  /**
   * Gets the wait factor multiplier off of the definition, returning
   * {@link #INTERVAL_WAIT_FACTOR_DEFAULT} if not specified. This will look for
   * {@link #STALE_INTERVAL_MULTIPLIER_PARAM_KEY} in the definition parameters.
   * The value returned from this method will be guaranteed to be in the range
   * of 2 to 10.
   *
   * @param entity
   *          the definition to read
   * @return the wait factor interval multiplier
   */
  private int getWaitFactorMultiplier(AlertDefinitionEntity entity) {
    // start with the default
    int waitFactor = INTERVAL_WAIT_FACTOR_DEFAULT;

    // coerce the entity into a business object so that the list of parameters
    // can be extracted and used for threshold calculation
    try {
      AlertDefinition definition = m_definitionFactory.coerce(entity);
      ServerSource serverSource = (ServerSource) definition.getSource();
      List<AlertParameter> parameters = serverSource.getParameters();
      for (AlertParameter parameter : parameters) {
        Object value = parameter.getValue();

        if (StringUtils.equals(parameter.getName(), STALE_INTERVAL_MULTIPLIER_PARAM_KEY)) {
          waitFactor = getThresholdValue(value, INTERVAL_WAIT_FACTOR_DEFAULT);
        }
      }

      if (waitFactor < 2 || waitFactor > 10) {
        LOG.warn(
            "The interval multipler of {} is outside the valid range for {} and will be set to 2",
            waitFactor, entity.getLabel());

        waitFactor = 2;
      }
    } catch (Exception exception) {
      LOG.error("Unable to read the {} parameter for {}", STALE_INTERVAL_MULTIPLIER_PARAM_KEY,
          StaleAlertRunnable.class.getSimpleName(), exception);
    }

    return waitFactor;
  }
}

