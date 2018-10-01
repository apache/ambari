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

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;


public class AddSparkUserToYarnACLAdminQueue extends AbstractUpgradeServerAction {
  private static final String CAPACITY_SCHEDULER_CONFIG_TYPE = "capacity-scheduler";
  private static final String SPARK_ENV_CONFIG_TYPE = "spark2-env";

  private static final String YARN_ACL_ADMIN_QUEUE_PROPERTY_NAME = "yarn.scheduler.capacity.root.acl_administer_queue";
  private static final String SPARK_USER_PROPERTY_NAME = "spark_user";

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
          throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();

    Cluster cluster = getClusters().getCluster(clusterName);

    Config capacitySchedulerConfig = cluster.getDesiredConfigByType(CAPACITY_SCHEDULER_CONFIG_TYPE);
    Config sparkEnvConfig = cluster.getDesiredConfigByType(SPARK_ENV_CONFIG_TYPE);

    if (capacitySchedulerConfig == null) {
      return  createCommandReport(0, HostRoleStatus.FAILED,"{}",
              String.format("Source type %s not found", CAPACITY_SCHEDULER_CONFIG_TYPE), "");
    }

    if (sparkEnvConfig == null) {
      return  createCommandReport(0, HostRoleStatus.FAILED,"{}",
              String.format("Source type %s not found", SPARK_ENV_CONFIG_TYPE), "");
    }

    Map<String, String> capacitySchedulerProperties = capacitySchedulerConfig.getProperties();
    Map<String, String> sparkEnvProperties = sparkEnvConfig.getProperties();

    String yarnACLAdminQueue = capacitySchedulerProperties.get(YARN_ACL_ADMIN_QUEUE_PROPERTY_NAME);
    String sparkUser = sparkEnvProperties.get(SPARK_USER_PROPERTY_NAME);

    String message = "";
    if (yarnACLAdminQueue != null && !yarnACLAdminQueue.trim().equals("*")) {
      yarnACLAdminQueue = yarnACLAdminQueue + "," + sparkUser;
      capacitySchedulerProperties.put(YARN_ACL_ADMIN_QUEUE_PROPERTY_NAME, yarnACLAdminQueue);
      capacitySchedulerConfig.setProperties(capacitySchedulerProperties);
      capacitySchedulerConfig.save();
      agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));
      message = String.format("Spark user %s was successfully added to %s property value.", sparkUser, YARN_ACL_ADMIN_QUEUE_PROPERTY_NAME);
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", message, "");
  }
}
