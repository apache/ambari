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
package org.apache.ambari.server.serveraction.upgrades;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;

import com.google.common.collect.ImmutableSet;

/**
 * Removes alerts that are superseded in the target stack.
 * Eg. SPARK2_THRIFTSERVER_PROCESS by spark2_thriftserver_status.
 */
public class RemoveObsoleteAlerts extends AbstractServerAction {

  private static final Set<String> ALERTS_TO_BE_REMOVED = ImmutableSet.of("SPARK2_THRIFTSERVER_PROCESS");

  @Inject
  private Clusters clusters;

  @Inject
  private AlertsDAO alertsDAO;

  @Inject
  private AlertDefinitionDAO alertDefinitionDAO;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);

    StringBuilder sb = new StringBuilder();
    for (String alertName : ALERTS_TO_BE_REMOVED) {
      String message;
      AlertDefinitionEntity alert = alertDefinitionDAO.findByName(cluster.getClusterId(), alertName);
      if (alert != null) {
        alertsDAO.removeByDefinitionId(alert.getDefinitionId());
        alertDefinitionDAO.remove(alert);
        message = String.format("Removed alert id = %d, name = %s", alert.getDefinitionId(), alertName);
      } else {
        message = String.format("Alert with name %s not found", alertName);
      }
      sb.append(message);
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", sb.toString(), "");
  }
}
