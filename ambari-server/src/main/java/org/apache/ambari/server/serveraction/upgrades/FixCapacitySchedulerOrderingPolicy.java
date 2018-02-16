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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;

/**
 * In HDP-2.6, the parent queue's cannot have a ordering-policy other than {@code utilization} or
 * {@code priority-utilization}.
 *
 * This class is used when moving from HDP-2.3/HDP-2.4/HDP-2.5 to HDP2.6
 */
public class FixCapacitySchedulerOrderingPolicy extends AbstractUpgradeServerAction {
  private static final String SOURCE_CONFIG_TYPE = "capacity-scheduler";
  private static final String ORDERING_POLICY_SUFFIX = "ordering-policy";

  private static final String CAPACITY_SCHEDULER_PREFIX = "yarn.scheduler.capacity";
  private static final String UTILIZATION = "utilization";
  private static final String PRIORITY_UTILIZATION = "priority-utilization";


  // queue names with any letter, ., -, or _
  private static final Pattern ROOT_QUEUE_REGEX = Pattern.compile(
      String.format("%s.([.\\-_\\w]+).queues", CAPACITY_SCHEDULER_PREFIX));

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {


    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);
    Config config = cluster.getDesiredConfigByType(SOURCE_CONFIG_TYPE);

    if (null == config) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
          String.format("The cluster does not have %s defined.", SOURCE_CONFIG_TYPE), "");
    }

    Map<String, String> properties = config.getProperties();

    Set<String> parentQueueNames = new HashSet<>();

    // first find the parent queue names
    for (String key : properties.keySet()) {
      Matcher matcher = ROOT_QUEUE_REGEX.matcher(key);
      if (matcher.matches() && 1 == matcher.groupCount()) {
        parentQueueNames.add(matcher.group(1));
      }
    }

    if (parentQueueNames.isEmpty()) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
          String.format("The %s has no root queue names.", SOURCE_CONFIG_TYPE), "");
    }

    boolean changedProperties = false;
    StringBuilder stdout = new StringBuilder();


    for (String queueName : parentQueueNames) {
      String orderingPolicyKey = String.format("%s.%s.%s", CAPACITY_SCHEDULER_PREFIX, queueName, ORDERING_POLICY_SUFFIX);

      String orderingPolicyValue = properties.get(orderingPolicyKey);
      if (null == orderingPolicyValue) {
        stdout.append("Ordering policy not found for ").append(orderingPolicyKey).append(',')
          .append(" value will not be set.").append(System.lineSeparator());

      } else if (!orderingPolicyValue.equals(UTILIZATION) || !orderingPolicyValue.equals(PRIORITY_UTILIZATION)) {
        properties.put(orderingPolicyKey, UTILIZATION);
        changedProperties = true;

        stdout.append("Changed ordering policy on ").append(orderingPolicyKey)
          .append(" from '").append(orderingPolicyValue).append("' to '").append(UTILIZATION)
          .append('\'').append(System.lineSeparator());
      }
    }

    if (!changedProperties) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
          String.format("No root queues required updating to %s.", UTILIZATION), "");
    }

    config.setProperties(properties);
    config.save();
    agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", stdout.toString(), "");
  }
}
