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
package org.apache.ambari.server.checks;


import static java.util.Arrays.asList;
import static org.apache.ambari.server.state.AlertState.CRITICAL;
import static org.apache.ambari.server.state.AlertState.WARNING;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Checks that there are no WARNING/CRITICAL alerts on current cluster.
 * That is a potential problem when doing stack update.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.DEFAULT,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class HealthCheck extends AbstractCheckDescriptor {

  private static final List<AlertState> ALERT_STATES = asList(WARNING, CRITICAL);

  @Inject
  Provider<AlertsDAO> alertsDAOProvider;

  /**
   * Constructor.
   */
  public HealthCheck() {
    super(CheckDescription.HEALTH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
      throws AmbariException {

    AlertsDAO alertsDAO = alertsDAOProvider.get();
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    List<AlertCurrentEntity> alerts = alertsDAO.findCurrentByCluster(cluster.getClusterId());

    List<String> errorMessages = new ArrayList<>();

    for (AlertCurrentEntity alert : alerts) {
      AlertHistoryEntity alertHistory = alert.getAlertHistory();
      AlertState alertState = alertHistory.getAlertState();
      if (ALERT_STATES.contains(alertState) && !alert.getMaintenanceState().equals(MaintenanceState.ON)) {
        String state = alertState.name();
        String label = alertHistory.getAlertDefinition().getLabel();
        String hostName = alertHistory.getHostName();

        if (hostName == null) {
          errorMessages.add(state + ": " + label);
        } else {
          errorMessages.add(state + ": " + label + ": " + hostName);
        }
        prerequisiteCheck.getFailedDetail().add(new AlertDetail(state, label, hostName));
      }
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.getFailedOn().add(clusterName);
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      String failReason = getFailReason(prerequisiteCheck, request);
      prerequisiteCheck.setFailReason(
          String.format(failReason, StringUtils.join(errorMessages, System.lineSeparator())));
    }
  }

  /**
   * Used to represent specific detail about alert.
   */
  private static class AlertDetail {
    public String state;
    public String label;
    public String hostName;

    AlertDetail(String state, String label, String hostName) {
      this.state = state;
      this.label = label;
      this.hostName = hostName;
    }
  }
}
