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

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;

/**
* Computes Ranger KMS Proxy properties in kms-site
*/

public class RangerKmsProxyConfig extends AbstractUpgradeServerAction {
  private static final String RANGER_ENV_CONFIG_TYPE = "ranger-env";
  private static final String RANGER_KMS_SITE_CONFIG_TYPE = "kms-site";

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
    throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);
    String outputMsg = "";

    Config rangerEnv = cluster.getDesiredConfigByType(RANGER_ENV_CONFIG_TYPE);

    if (null == rangerEnv) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        MessageFormat.format("Config source type {0} not found, skipping adding properties to {1}.", RANGER_ENV_CONFIG_TYPE, RANGER_KMS_SITE_CONFIG_TYPE), "");
    }

    String rangerUserProp = "ranger_user";
    String rangerUser = rangerEnv.getProperties().get(rangerUserProp);

    if (null == rangerUser) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        MessageFormat.format("Required user service user value from {0}/{1} not found, skipping adding properties to {2}.", RANGER_ENV_CONFIG_TYPE, rangerUserProp, RANGER_KMS_SITE_CONFIG_TYPE), "");
    }

    Config kmsSite = cluster.getDesiredConfigByType(RANGER_KMS_SITE_CONFIG_TYPE);

    if (null == kmsSite) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        MessageFormat.format("Config type {0} not found, skipping adding properties to it.", RANGER_KMS_SITE_CONFIG_TYPE), "");
    }

    Map<String, String> targetValues = kmsSite.getProperties();
    if (cluster.getSecurityType() == SecurityType.KERBEROS) {
      String userProp = "hadoop.kms.proxyuser." + rangerUser + ".users";
      String groupProp = "hadoop.kms.proxyuser." + rangerUser + ".groups";
      String hostProp = "hadoop.kms.proxyuser." + rangerUser + ".hosts";
      targetValues.put(userProp, "*");
      targetValues.put(groupProp, "*");
      targetValues.put(hostProp, "*");
      kmsSite.setProperties(targetValues);
      kmsSite.save();
      outputMsg = outputMsg + MessageFormat.format("Successfully added properties to {0}", RANGER_KMS_SITE_CONFIG_TYPE);
    } else {
      outputMsg = outputMsg +  MessageFormat.format("Kerberos not enable, not setting proxy properties to {0}", RANGER_KMS_SITE_CONFIG_TYPE);
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outputMsg, "");

  }
}
