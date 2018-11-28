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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.exceptions.UpgradeActionException;
import org.apache.ambari.spi.upgrade.UpgradeAction;
import org.apache.ambari.spi.upgrade.UpgradeActionOperations;
import org.apache.ambari.spi.upgrade.UpgradeActionOperations.ChangeType;
import org.apache.ambari.spi.upgrade.UpgradeActionOperations.ConfigurationChanges;
import org.apache.ambari.spi.upgrade.UpgradeActionOperations.PropertyChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PluginUpgradeServerAction} is used to execute operations from
 * {@link UpgradeActionOperations} defined in each stack.
 */
public class PluginUpgradeServerAction extends AbstractUpgradeServerAction {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(PluginUpgradeServerAction.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = m_clusters.getCluster(clusterName);

    UpgradeContext upgradeContext = getUpgradeContext(cluster);
    UpgradePack upgradePack = upgradeContext.getUpgradePack();
    StackId stackId = upgradePack.getOwnerStackId();
    StackInfo stackInfo = m_metainfoProvider.get().getStack(stackId);

    ClassLoader pluginClassLoader = stackInfo.getLibraryClassLoader();
    if (null == pluginClassLoader) {
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
          "There is no plugin classloader defined for stack " + stackId);
    }

    final UpgradeAction upgradeAction;
    final String pluginClassName = getActionClassName();

    try {
      @SuppressWarnings("unchecked")
      Class<? extends UpgradeAction> upgradeActionClass = (Class<? extends UpgradeAction>) pluginClassLoader.loadClass(
          pluginClassName);

      upgradeAction = upgradeActionClass.newInstance();
    } catch (Exception exception) {
      LOG.error("Unable to load the upgrade action {}", pluginClassName, exception);

      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
          "Unable to load the upgrade class  " + pluginClassName);
    }

    String standardOutput;

    try {
      ClusterInformation clusterInformation = cluster.buildClusterInformation();
      UpgradeActionOperations upgradeActionOperations = upgradeAction.getOperations(
          clusterInformation);

      // update configurations
      changeConfigurations(cluster, upgradeActionOperations.getConfigurationChanges());

      standardOutput = "Successfully executed " + pluginClassName;
      if(null != upgradeActionOperations.getStandardOutput()) {
        standardOutput = upgradeActionOperations.getStandardOutput().toString();
      }
    } catch (UpgradeActionException exception) {
      LOG.error("Unable to run the upgrade action {}", pluginClassName, exception);
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "", exception.getMessage());
    } catch (Exception exception) {
      LOG.error("Unable to run the upgrade action {}", pluginClassName, exception);
      String standardError = "Unable to run " + pluginClassName;
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "", standardError);
    }

    // !!! it's stupid that we have to do this
    agentConfigsHolder.updateData(cluster.getClusterId(),
        cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", standardOutput, "");
  }

  /**
   * Updates configurations in the cluster.
   *
   * @param cluster
   *          the cluster used to retrieve the configurations.
   * @param configurationChanges
   *          the changes to make.
   * @throws AmbariException
   *           if there was a problem determining what change to make or while
   *           making changes.
   */
  private void changeConfigurations(Cluster cluster,
      List<ConfigurationChanges> configurationChanges)
      throws AmbariException {
    for (ConfigurationChanges configTypeChanges : configurationChanges) {
      String configType = configTypeChanges.getConfigType();
      Config config = cluster.getDesiredConfigByType(configType);

      List<PropertyChange> propertyChanges = configTypeChanges.getPropertyChanges();
      for (PropertyChange propertyChange : propertyChanges) {
        ChangeType changeType = propertyChange.getChangeType();
        switch (changeType) {
          case REMOVE:
            config.deleteProperties(Collections.singletonList(propertyChange.getPropertyName()));
            break;
          case SET:
            Map<String, String> propertyMap = new HashMap<>();
            propertyMap.put(propertyChange.getPropertyName(), propertyChange.getPropertyValue());
            config.updateProperties(propertyMap);
            break;
          default:
            LOG.error("Unknown configuration action type {}", changeType);
            throw new AmbariException(
                "Unable to update configurations because " + changeType + " is an unknown type");
        }
      }

      config.save();
    }
  }

  /**
   * Gets the fully qualified classname of the {@link UpgradeAction} class which
   * will be executed. This will look in the command parameters of the execution
   * command for {@link ServerAction#WRAPPED_CLASS_NAME}.
   *
   *
   * @return the name of the class.
   * @throws AmbariException
   *           if the class name could not be found.
   */
  private String getActionClassName() throws AmbariException {
    String wrappedClassName = getCommandParameterValue(ServerAction.WRAPPED_CLASS_NAME);
    if (null == wrappedClassName) {
      throw new AmbariException("The name of the upgrade action class to execute was not found.");
    }

    return wrappedClassName;
  }
}
