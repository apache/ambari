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

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;

/**
 * Computes HBase properties.  This class is only used when moving from
 * HDP-2.2 to HDP-2.3 in that upgrade pack.
 */
public class HBaseConfigCalculation extends AbstractUpgradeServerAction {
  private static final String SOURCE_CONFIG_TYPE = "hbase-site";
  private static final String OLD_UPPER_LIMIT_PROPERTY_NAME = "hbase.regionserver.global.memstore.upperLimit";
  private static final String OLD_LOWER_LIMIT_PROPERTY_NAME = "hbase.regionserver.global.memstore.lowerLimit";
  private static final String NEW_LOWER_LIMIT_PROPERTY_NAME = "hbase.regionserver.global.memstore.size.lower.limit";


  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
    throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();

    Cluster cluster = getClusters().getCluster(clusterName);

    Config config = cluster.getDesiredConfigByType(SOURCE_CONFIG_TYPE);

    if (config == null) {
      return  createCommandReport(0, HostRoleStatus.FAILED,"{}",
                                   String.format("Source type %s not found", SOURCE_CONFIG_TYPE), "");
    }

    Map<String, String> properties = config.getProperties();
    String upperLimitStr = properties.get(OLD_UPPER_LIMIT_PROPERTY_NAME);
    String lowerLimitStr = properties.get(OLD_LOWER_LIMIT_PROPERTY_NAME);

    if (upperLimitStr == null || lowerLimitStr == null) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                                   "Upper or lower memstore limit setting is not set, skipping", "");
    }

    BigDecimal upperLimit;
    BigDecimal lowerLimit;

    try {
      upperLimit = new BigDecimal(upperLimitStr);
      lowerLimit = new BigDecimal(lowerLimitStr);
    } catch (NumberFormatException e) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                                   "Upper or lower memstore limit setting value is malformed, skipping", "");
    }

    if (lowerLimit.scale() < 2) {
      lowerLimit = lowerLimit.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    BigDecimal lowerLimitNew = lowerLimit.divide(upperLimit, BigDecimal.ROUND_HALF_UP);

    properties.put(NEW_LOWER_LIMIT_PROPERTY_NAME, lowerLimitNew.toString());

    // remove old properties
    properties.remove(OLD_UPPER_LIMIT_PROPERTY_NAME);
    properties.remove(OLD_LOWER_LIMIT_PROPERTY_NAME);

    config.setProperties(properties);
    config.save();

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                  String.format("%s was set to %s", NEW_LOWER_LIMIT_PROPERTY_NAME, lowerLimitNew.toString()), "");

  }
}
