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

import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class HBaseConfigCalculation extends AbstractServerAction {

  @Inject
  Clusters clusters;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    StringBuilder stdout = new StringBuilder();
    String clusterName = getExecutionCommand().getClusterName();

    Cluster cluster = clusters.getCluster(clusterName);

    Config config = cluster.getDesiredConfigByType("hbase-site");

    Map<String, String> properties = config.getProperties();
    String upperLimitStr = properties.get("hbase.regionserver.global.memstore.upperLimit");
    String lowerLimitStr = properties.get("hbase.regionserver.global.memstore.lowerLimit");

    BigDecimal upperLimit = new BigDecimal(upperLimitStr);
    BigDecimal lowerLimit = new BigDecimal(lowerLimitStr);
    if (lowerLimit.scale() < 2) //make sure result will have at least 2 digits after decimal point
      lowerLimit = lowerLimit.setScale(2, BigDecimal.ROUND_HALF_UP);
    BigDecimal lowerLimitNew = lowerLimit.divide(upperLimit, BigDecimal.ROUND_HALF_UP);

    properties.put("hbase.regionserver.global.memstore.size.lower.limit", lowerLimitNew.toString());
    stdout.append("hbase.regionserver.global.memstore.size.lower.limit was set to ").append(lowerLimitNew);

    config.setProperties(properties);
    config.persist(false);

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", stdout.toString(), "");

  }
}
