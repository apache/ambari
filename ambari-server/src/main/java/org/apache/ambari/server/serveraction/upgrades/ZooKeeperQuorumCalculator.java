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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.lang.StringUtils;

/**
 * The {@link ZooKeeperQuorumCalculator} is a central location which can be used
 * to construct a comma-separated string of known ZooKeeper hosts. The returned
 * string constains FQDN of each known ZK host along with a port value for each.
 */
public class ZooKeeperQuorumCalculator {
  private static final String ZOO_CFG_CONFIG_TYPE = "zoo.cfg";
  private static final String ZOOKEEPER_CLIENT_PORT_PROPERTY_NAME = "clientPort";
  private static final String DEFAULT_ZK_CLIENT_PORT = "2181";

  /**
   * Gets a comma-separate list of the ZK servers along with their ports.
   *
   * @param cluster
   *          the cluster (not {@code null}).
   * @return the list of FQDN ZooKeeper hosts, or an empty string (never
   *         {@code null}).
   */
  static String getZooKeeperQuorumString(Cluster cluster) {

    // attempt to calculate the port
    String zkClientPort = DEFAULT_ZK_CLIENT_PORT;
    Config zooConfig = cluster.getDesiredConfigByType(ZOO_CFG_CONFIG_TYPE);
    if (zooConfig != null) {
      Map<String, String> zooProperties = zooConfig.getProperties();
      if (zooProperties.containsKey(ZOOKEEPER_CLIENT_PORT_PROPERTY_NAME)) {
        zkClientPort = zooProperties.get(ZOOKEEPER_CLIENT_PORT_PROPERTY_NAME);
      }
    }

    // get all known ZK hosts
    List<ServiceComponentHost> zkServers = cluster.getServiceComponentHosts(
        Service.Type.ZOOKEEPER.name(), Role.ZOOKEEPER_SERVER.name());

    List<String> zkAddresses = new ArrayList<>();
    for (ServiceComponentHost zkServer : zkServers) {
      String zkAddress = zkServer.getHostName() + ":" + zkClientPort;
      zkAddresses.add(zkAddress);
    }

    // join on comma without any spaces
    String zkServersStr = StringUtils.join(zkAddresses, ",");
    return zkServersStr;
  }
}
