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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;

import com.google.inject.Inject;

/**
 * Computes Yarn properties.  This class is only used when moving from
 * HDP-2.1 to HDP-2.3 in that upgrade pack.
 */
public class YarnConfigCalculation extends AbstractServerAction {
  private static final String YARN_SITE_CONFIG_TYPE = "yarn-site";

  private static final String YARN_RM_ZK_ADDRESS_PROPERTY_NAME = "yarn.resourcemanager.zk-address";
  private static final String HADOOP_REGISTRY_ZK_QUORUM_PROPERTY_NAME = "hadoop.registry.zk.quorum";

  @Inject
  private Clusters clusters;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();

    Cluster cluster = clusters.getCluster(clusterName);

    Config yarnSiteConfig = cluster.getDesiredConfigByType(YARN_SITE_CONFIG_TYPE);

    if (yarnSiteConfig == null) {
      return  createCommandReport(0, HostRoleStatus.FAILED,"{}",
          String.format("Source type %s not found", YARN_SITE_CONFIG_TYPE), "");
    }

    Map<String, String> yarnSiteProperties = yarnSiteConfig.getProperties();
    String oldRmZkAddress = yarnSiteProperties.get(YARN_RM_ZK_ADDRESS_PROPERTY_NAME);
    String oldHadoopRegistryZKQuorum = yarnSiteProperties.get(HADOOP_REGISTRY_ZK_QUORUM_PROPERTY_NAME);

    String zkServersStr = ZooKeeperQuorumCalculator.getZooKeeperQuorumString(cluster);
    yarnSiteProperties.put(YARN_RM_ZK_ADDRESS_PROPERTY_NAME, zkServersStr);
    yarnSiteProperties.put(HADOOP_REGISTRY_ZK_QUORUM_PROPERTY_NAME, zkServersStr);
    yarnSiteConfig.setProperties(yarnSiteProperties);
    yarnSiteConfig.persist(false);

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        String.format("%s was set from %s to %s. %s was set from %s to %s",
            YARN_RM_ZK_ADDRESS_PROPERTY_NAME, oldRmZkAddress, zkServersStr,
            HADOOP_REGISTRY_ZK_QUORUM_PROPERTY_NAME, oldHadoopRegistryZKQuorum, zkServersStr), "");
  }
}
