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

/**
 * The {@link HiveZKQuorumConfigAction} is used to ensure that the following
 * settings are correctly set when upgrading a Hive Server:
 * <ul>
 * <li>hive.zookeeper.quorum</li>
 * <li>hive.cluster.delegation.token.store.zookeeper.connectString</li>
 * </ul>
 * <p/>
 * This is typically only needed when upgrading from a version which does not
 * have these properties but where the Hive server is already Kerberized. The
 * upgrade merge logic can't do complex calculations, such as the ZK quorum.
 * <p/>
 * The above properties will be set regardless of whether
 * {@code cluster-env/security_enabled} is {@code true}. This is because the
 * Kerberization wizard doesn't know to set these when Kerberizing a version of
 * Hive that was upgraded previously. They are actually set (incorrectly) on a
 * non-Kerberized Hive installation by the installation wizard.
 */
public class HiveZKQuorumConfigAction extends AbstractUpgradeServerAction {
  protected static final String HIVE_SITE_CONFIG_TYPE = "hive-site";
  protected static final String HIVE_SITE_ZK_QUORUM = "hive.zookeeper.quorum";
  protected static final String HIVE_SITE_ZK_CONNECT_STRING = "hive.cluster.delegation.token.store.zookeeper.connectString";


  /**
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);

    Config hiveSite = cluster.getDesiredConfigByType(HIVE_SITE_CONFIG_TYPE);
    if (hiveSite == null) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
          String.format(
              "The %s configuration type was not found; unable to set Hive configuration properties",
              HIVE_SITE_CONFIG_TYPE),
          "");
    }

    String zookeeperQuorum = ZooKeeperQuorumCalculator.getZooKeeperQuorumString(cluster);

    Map<String, String> hiveSiteProperties = hiveSite.getProperties();
    hiveSiteProperties.put(HIVE_SITE_ZK_QUORUM, zookeeperQuorum);
    hiveSiteProperties.put(HIVE_SITE_ZK_CONNECT_STRING, zookeeperQuorum);

    hiveSite.setProperties(hiveSiteProperties);
    hiveSite.save();
    agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        String.format("Successfully set %s and %s in %s", HIVE_SITE_ZK_QUORUM,
            HIVE_SITE_ZK_CONNECT_STRING, HIVE_SITE_CONFIG_TYPE),
        "");
  }
}
