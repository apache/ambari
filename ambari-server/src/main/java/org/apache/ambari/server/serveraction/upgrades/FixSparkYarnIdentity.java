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
import org.apache.ambari.server.state.SecurityType;

/**
 * Check if spark.yarn.kaytab/principal property exists
 * if not, then we are creating them and copy pasting
 * values from spark.history.kerberos.keytab/principal properties.
 * Works only for kerberized cluster.
 */
public class FixSparkYarnIdentity extends AbstractUpgradeServerAction {
  private static final String SPARK2_THRIFT_SPARKCONF_CONFIG_TYPE = "spark2-thrift-sparkconf";
  private static final String SPARK2_DEFAULTS_CONFIG_TYPE = "spark2-defaults";

  private static final String SPARK_YARN_KEYTAB_PROPERTY_NAME = "spark.yarn.keytab";
  private static final String SPARK_YARN_KEYTAB_PRINCIPAL_PROPERTY_NAME = "spark.yarn.principal";

  private static final String SPARK_HISTORY_KERBEROS_KEYTAB_PROPERTY_NAME = "spark.history.kerberos.keytab";
  private static final String SPARK_HISTORY_KERBEROS_PRINCIPAL_PROPERTY_NAME = "spark.history.kerberos.principal";


  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
          throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();

    Cluster cluster = getClusters().getCluster(clusterName);

    if (cluster.getSecurityType() == SecurityType.KERBEROS) {

      Config spark2ThriftSparkConfConfig = cluster.getDesiredConfigByType(SPARK2_THRIFT_SPARKCONF_CONFIG_TYPE);

      if (spark2ThriftSparkConfConfig != null) {
        Map spark2ThriftSparkConfProperties = spark2ThriftSparkConfConfig.getProperties();
        if (!spark2ThriftSparkConfProperties.containsKey(SPARK_YARN_KEYTAB_PROPERTY_NAME) &&
                !spark2ThriftSparkConfProperties.containsKey(SPARK_YARN_KEYTAB_PRINCIPAL_PROPERTY_NAME)) {

          Config spark2DefaultsConfig = cluster.getDesiredConfigByType(SPARK2_DEFAULTS_CONFIG_TYPE);
          if (spark2DefaultsConfig != null) {
            Map spark2DefultsProperties = spark2DefaultsConfig.getProperties();

            spark2ThriftSparkConfProperties.put(SPARK_YARN_KEYTAB_PROPERTY_NAME, spark2DefultsProperties.get(SPARK_HISTORY_KERBEROS_KEYTAB_PROPERTY_NAME));
            spark2ThriftSparkConfProperties.put(SPARK_YARN_KEYTAB_PRINCIPAL_PROPERTY_NAME, spark2DefultsProperties.get(SPARK_HISTORY_KERBEROS_PRINCIPAL_PROPERTY_NAME));
            spark2ThriftSparkConfConfig.setProperties(spark2ThriftSparkConfProperties);
            spark2ThriftSparkConfConfig.save();
            agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

            return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
                    "Properties spark.yarn.keytab/principal were successfully added and initialized.", "");
          }
        }
      }
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
            "Nothing was done, because kerberos security is not enabled or spark.yarn.keytab/principal already added", "");
  }
}
