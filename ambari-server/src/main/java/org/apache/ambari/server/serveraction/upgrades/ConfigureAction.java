/**
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;

import com.google.inject.Inject;

/**
 * Action that represents a manual stage.
 */
public class ConfigureAction extends AbstractServerAction {

  /**
   * Used to lookup the cluster.
   */
  @Inject
  private Clusters m_clusters;

  /**
   * Used to update the configuration properties.
   */
  @Inject
  private AmbariManagementController m_controller;

  /**
   * Used to assist in the creation of a {@link ConfigurationRequest} to update
   * configuration values.
   */
  @Inject
  private ConfigHelper m_configHelper;

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(
      ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    Map<String,String> commandParameters = getCommandParameters();
    if( null == commandParameters || commandParameters.isEmpty() ){
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
          "Unable to change configuration values without command parameters");
    }

    String clusterName = commandParameters.get("clusterName");
    String key = commandParameters.get(ConfigureTask.PARAMETER_KEY);
    String value = commandParameters.get(ConfigureTask.PARAMETER_VALUE);

    // such as hdfs-site or hbase-env
    String configType = commandParameters.get(ConfigureTask.PARAMETER_CONFIG_TYPE);

    // if the two required properties are null, then assume that no
    // conditions were met and let the action complete
    if (null == configType && null == key) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", "",
          "Skipping configuration task");
    }

    // if only 1 of the required properties was null, then something went
    // wrong
    if (null == clusterName || null == configType || null == key) {
      String message = "cluster={0}, type={1}, key={2}";
      message = MessageFormat.format(message, clusterName, configType, key);
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "", message);
    }

    Cluster cluster = m_clusters.getCluster(clusterName);
    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get(configType);
    Config config = cluster.getConfig(configType, desiredConfig.getTag());

    Map<String, String> propertiesToChange = new HashMap<String, String>();
    propertiesToChange.put(key, value);
    config.updateProperties(propertiesToChange);

    String serviceVersionNote = "Stack Upgrade";

    m_configHelper.createConfigType(cluster, m_controller, configType,
        config.getProperties(), m_controller.getAuthName(), serviceVersionNote);

    String message = "Updated ''{0}'' with ''{1}={2}''";
    message = MessageFormat.format(message, configType, key, value);

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", message, "");
  }
}
