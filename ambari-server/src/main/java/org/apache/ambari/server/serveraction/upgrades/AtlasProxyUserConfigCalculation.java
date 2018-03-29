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
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;

public class AtlasProxyUserConfigCalculation extends AbstractUpgradeServerAction {

  private static final String ATLAS_APPLICATION_PROPERTIES_CONFIG_TYPE = "application-properties";
  private static final String KNOX_ENV_CONFIG_TYPE = "knox-env";
  private static final String KNOX_USER_CONFIG = "knox_user";

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);
    String outputMessage = "";

    Config atlasApplicationProperties = cluster.getDesiredConfigByType(ATLAS_APPLICATION_PROPERTIES_CONFIG_TYPE);
    if (null == atlasApplicationProperties) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        MessageFormat.format("Config type {0} not found, skipping updating property in same.", ATLAS_APPLICATION_PROPERTIES_CONFIG_TYPE), "");
    }

    Config knoxEnvConfig = cluster.getDesiredConfigByType(KNOX_ENV_CONFIG_TYPE);
    String atlasProxyUsers = "knox";
    if (null != knoxEnvConfig && knoxEnvConfig.getProperties().containsKey(KNOX_USER_CONFIG)) {
      atlasProxyUsers = knoxEnvConfig.getProperties().get(KNOX_USER_CONFIG);
    }

    Map<String, String> currentAtlasApplicationProperties = atlasApplicationProperties.getProperties();
    currentAtlasApplicationProperties.put("atlas.proxyusers", atlasProxyUsers);
    atlasApplicationProperties.setProperties(currentAtlasApplicationProperties);
    atlasApplicationProperties.save();
    agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

    outputMessage = outputMessage + MessageFormat.format("Successfully updated {0} config type.\n", ATLAS_APPLICATION_PROPERTIES_CONFIG_TYPE);
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outputMessage, "");
  }
}
