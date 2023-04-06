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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;

/**
 * Computes HBase Env content property.
 * This class is only used when moving from HDP-2.3 to HDP-2.4 and HDP-2.3 to HDP-2.5
 */
public class HBaseEnvMaxDirectMemorySizeAction extends AbstractUpgradeServerAction {
  private static final String SOURCE_CONFIG_TYPE = "hbase-env";
  private static final String CONTENT_NAME = "content";
  private static final String APPEND_CONTENT_LINE = "export HBASE_MASTER_OPTS=\"$HBASE_MASTER_OPTS {% if hbase_max_direct_memory_size %} -XX:MaxDirectMemorySize={{hbase_max_direct_memory_size}}m {% endif %}\"";
  private static final String CHECK_REGEX = "^.*\\s*(HBASE_MASTER_OPTS)\\s*=.*(XX:MaxDirectMemorySize).*$";
  private static final Pattern REGEX = Pattern.compile(CHECK_REGEX, Pattern.MULTILINE);

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
    String content = properties.get(CONTENT_NAME);


    if (content == null) {
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        String.format("The %s/%s property was not found. The -XX:MaxDirectMemorySize will not be added", SOURCE_CONFIG_TYPE, CONTENT_NAME), "");
    }

    Matcher m = REGEX.matcher(content);

    if (m.find()){
      return  createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        String.format("The %s/%s property already contains an entry for %s and will not be modified", SOURCE_CONFIG_TYPE, CONTENT_NAME, APPEND_CONTENT_LINE), "");
    }

    String appendedContent = content + "\n" + APPEND_CONTENT_LINE;
    properties.put(CONTENT_NAME, appendedContent);

    config.setProperties(properties);
    config.save();
    agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
      String.format("The %s/%s property was appended with %s", SOURCE_CONFIG_TYPE, CONTENT_NAME, APPEND_CONTENT_LINE),"");
  }
}
