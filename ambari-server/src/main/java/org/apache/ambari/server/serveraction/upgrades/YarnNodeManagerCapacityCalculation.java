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
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;

import com.google.common.collect.Sets;

/**
 * Determines the current cluster capacity in terms of NodeManagers mulitplied
 * by their total memory. If the cluster is considered to be large, then this
 * will create a new system queue if it does not exist.
 */
public class YarnNodeManagerCapacityCalculation extends AbstractUpgradeServerAction {
  private static final String YARN_SITE_CONFIG_TYPE = "yarn-site";

  private static final String YARN_ENV_CONFIG_TYPE = "yarn-env";
  private static final String YARN_HBASE_ENV_CONFIG_TYPE = "yarn-hbase-env";
  private static final String CAPACITY_SCHEDULER_CONFIG_TYPE = "capacity-scheduler";

  private static final String YARN_SYSTEM_SERVICE_USER_NAME = "yarn_ats_user";
  private static final String YARN_SYSTEM_SERVICE_QUEUE_NAME = "yarn-system";
  private static final String CAPACITY_SCHEDULER_ROOT_QUEUES = "yarn.scheduler.capacity.root.queues";
  private static final String YARN_SYSTEM_SERVICE_QUEUE_PREFIX = "yarn.scheduler.capacity.root." + YARN_SYSTEM_SERVICE_QUEUE_NAME;

  // Big cluster values in MB
  private static final float CLUSTER_CAPACITY_LIMIT_FOR_HBASE_SYSTEM_SERVICE = 51200;
  private static final float NODE_CAPACITY_LIMIT_FOR_HBASE_SYSTEM_SERVICE = 10240;
  private static final String YARN_NM_PMEM_MB_PROPERTY_NAME = "yarn.nodemanager.resource.memory-mb";
  private static final String YARN_HBASE_SYSTEM_SERVICE_QUEUE_PROPERTY_NAME = "yarn_hbase_system_service_queue_name";

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);

    Config yarnSiteConfig = cluster.getDesiredConfigByType(YARN_SITE_CONFIG_TYPE);

    if (yarnSiteConfig == null) {
      return  createCommandReport(0, HostRoleStatus.FAILED,"{}",
          String.format("Source type %s not found", YARN_SITE_CONFIG_TYPE), "");
    }

    int noOfNMHosts = cluster.getService("YARN").getServiceComponent("NODEMANAGER").getServiceComponentsHosts().size();
    String nmMemoryInString = yarnSiteConfig.getProperties().get(YARN_NM_PMEM_MB_PROPERTY_NAME);
    int nmMemory = Integer.parseInt(nmMemoryInString);
    int clusterCapacity = noOfNMHosts * nmMemory;

    String message = "";

    // determine if the cluster is considered to be big; if each NM capacity is
    // greater than 10GB and cluster capacity greater than 50GB
    if (nmMemory > NODE_CAPACITY_LIMIT_FOR_HBASE_SYSTEM_SERVICE
        && clusterCapacity > CLUSTER_CAPACITY_LIMIT_FOR_HBASE_SYSTEM_SERVICE) {

      Config yarnEnvConfig = cluster.getDesiredConfigByType(YARN_ENV_CONFIG_TYPE);
      if (yarnEnvConfig == null) {
        return createCommandReport(0, HostRoleStatus.FAILED, "{}",
            String.format("Source type %s not found", YARN_ENV_CONFIG_TYPE), "");
      }

      String yarnAtsUser = yarnEnvConfig.getProperties().get(YARN_SYSTEM_SERVICE_USER_NAME);

      Config hbaseEnvConfig = cluster.getDesiredConfigByType(YARN_HBASE_ENV_CONFIG_TYPE);
      if (hbaseEnvConfig == null) {
        return createCommandReport(0, HostRoleStatus.FAILED, "{}",
            String.format("Source type %s not found", YARN_HBASE_ENV_CONFIG_TYPE), "");
      }

      Map<String, String> hbaseEnvConfigProperties = hbaseEnvConfig.getProperties();
      String oldSystemServiceQueue = hbaseEnvConfigProperties.get(
          YARN_HBASE_SYSTEM_SERVICE_QUEUE_PROPERTY_NAME);

      Config csConfig = cluster.getDesiredConfigByType(CAPACITY_SCHEDULER_CONFIG_TYPE);
      if (csConfig == null) {
        return createCommandReport(0, HostRoleStatus.FAILED, "{}",
            String.format("Source type %s not found", CAPACITY_SCHEDULER_CONFIG_TYPE), "");
      }

      Map<String, String> csProperties = csConfig.getProperties();
      String old_root_queues = csProperties.get(CAPACITY_SCHEDULER_ROOT_QUEUES);
      Set<String> queues = Sets.newHashSet(old_root_queues.split(","));
      boolean isYarnSystemQueueExist = false;

      isYarnSystemQueueExist = queues.stream()
          .map(queue -> queue.trim())
          .filter(queueName -> YARN_SYSTEM_SERVICE_QUEUE_NAME.equals(queueName))
          .findFirst()
          .isPresent();

      String new_root_queues = old_root_queues + "," + YARN_SYSTEM_SERVICE_QUEUE_NAME;
      // create yarn-system queue if doesn't exist under root queues.
      if (!isYarnSystemQueueExist) {
        csProperties.put(CAPACITY_SCHEDULER_ROOT_QUEUES, new_root_queues);
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".capacity", "0");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".maximum-capacity", "100");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".user-limit-factor", "1");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".minimum-user-limit-percent", "100");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".state", "RUNNING");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".ordering-policy", "fifo");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".acl_submit_applications",yarnAtsUser);
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".acl_administer_queue", yarnAtsUser);
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".maximum-am-resource-percent", "0.5");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".disable_preemption", "true");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".intra-queue-preemption.disable_preemption","true");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".priority", "32768");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".maximum-application-lifetime", "-1");
        csProperties.put(YARN_SYSTEM_SERVICE_QUEUE_PREFIX + ".default-application-lifetime", "-1");

        csConfig.setProperties(csProperties);
        csConfig.save();

        hbaseEnvConfigProperties.put(YARN_HBASE_SYSTEM_SERVICE_QUEUE_PROPERTY_NAME, YARN_SYSTEM_SERVICE_QUEUE_NAME);
        hbaseEnvConfig.setProperties(hbaseEnvConfigProperties);
        hbaseEnvConfig.save();

        message = String.format("%s was set from %s to %s. %s was set from %s to %s",
            CAPACITY_SCHEDULER_ROOT_QUEUES, old_root_queues, new_root_queues,
            YARN_HBASE_SYSTEM_SERVICE_QUEUE_PROPERTY_NAME, oldSystemServiceQueue,
            YARN_SYSTEM_SERVICE_QUEUE_NAME);
      }
    }

    agentConfigsHolder.updateData(cluster.getClusterId(),
        cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", message, "");
  }
}
