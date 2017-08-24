/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.serveraction.upgrades;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;

/**
 * Fixes auth_to_local rules during upgrade from IOP to HDP. An example of
 * invalid rule introduced by HBASE_REST_SERVER in the IOP stack, set auth to
 * local mapping for HTTP spnego principal to local hbase user, which needs to
 * be deleted for HIVE service check to pass.
 */
public class FixAuthToLocalMappingAction  extends AbstractServerAction {

  private static final String SPNEGO_PRINC_PATTERN = "RULE:\\[2:\\$1@\\$0\\]\\(HTTP@.*\\)s/\\.\\*/.*/\\n";
  private static final String AMS_HBASE_PATTERN = "RULE:\\[2:\\$1@\\$0\\]\\(amshbase@.*\\)s/\\.\\*/%s/\\n";
  private static final String ZK_AMS_PATTERN = "RULE:\\[2:\\$1@\\$0\\]\\(zookeeper@.*\\)s/\\.\\*/%s/\\n";

  @Inject
  private Clusters clusters;

  @Inject
  private KerberosHelper kerberosHelper;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);

    KerberosDescriptor kd = kerberosHelper.getKerberosDescriptor(cluster, false);
    if (kd == null) {
      return null;
    }

    Map<String, Set<String>> configProperties = new HashMap<>();
    for (String property : kd.getAllAuthToLocalProperties()) {
      if (!StringUtils.isEmpty(property) && property.contains("/")) {
        String[] propertyParts = property.split("/");
        if (configProperties.containsKey(propertyParts[0])) {
          configProperties.get(propertyParts[0]).add(propertyParts[1]);
        } else {
          Set<String> properties = new HashSet<>();
          properties.add(propertyParts[1]);
          configProperties.put(propertyParts[0], properties);
        }
      }
    }

    String hbaseUser = null;
    Config hbaseEnv = cluster.getDesiredConfigByType("hbase-env");
    if (hbaseEnv != null) {
      Map<String, String> properties = hbaseEnv.getProperties();
      if (!MapUtils.isEmpty(properties)) {
        hbaseUser = properties.get("hbase_user");
      }
    }

    String amsUser = null;
    Config amsEnv = cluster.getDesiredConfigByType("ams-env");
    if (amsEnv != null) {
      Map<String, String> properties = amsEnv.getProperties();
      if (!MapUtils.isEmpty(properties)) {
        amsUser = properties.get("ambari_metrics_user");
      }
    }

    boolean replaced = false;
    StringBuilder message = new StringBuilder("Replaced offending auto_to_local mappings");

    for (Map.Entry<String, Set<String>> configProperty : configProperties.entrySet()) {
      String configType = configProperty.getKey();
      Config config = cluster.getDesiredConfigByType(configType);

      if (config == null) {
        continue;
      }

      for (String property : configProperty.getValue()) {
        Map<String, String> properties = config.getProperties();
        if (!MapUtils.isEmpty(properties) && properties.containsKey(property)) {
          String authToLocalRules = properties.get(property);
          if (!StringUtils.isEmpty(authToLocalRules)) {
            authToLocalRules = authToLocalRules.replaceAll(SPNEGO_PRINC_PATTERN, "");
            if (hbaseUser != null) {
              authToLocalRules = authToLocalRules.replaceAll(String.format(AMS_HBASE_PATTERN, hbaseUser), "");
            }
            if (amsUser != null) {
              authToLocalRules = authToLocalRules.replaceAll(String.format(ZK_AMS_PATTERN, amsUser), "");
            }
            // Only if something was replaced
            if (!properties.get(property).equals(authToLocalRules)) {
              properties.put(property, authToLocalRules);
              message.append(" , property => ");
              message.append(property);
              message.append(" , config => ");
              message.append(configType);

              config.setProperties(properties);
              config.save();
              replaced = true;
            }
          }
        }
      }
    }

    String finalMessage = message.toString();
    if (!replaced) {
      finalMessage = "No offending auto_to_local mappings found";
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", finalMessage, "");
  }
}
