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
package org.apache.ambari.server.actionmanager;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.VERSION;

import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ModuleComponent;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.UpgradeContext.UpgradeSummary;
import org.apache.ambari.server.state.UpgradeContextFactory;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ExecutionCommandWrapper {

  private final static Logger LOG = LoggerFactory.getLogger(ExecutionCommandWrapper.class);
  String jsonExecutionCommand = null;
  ExecutionCommand executionCommand = null;

  @Inject
  Clusters clusters;

  @Inject
  HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  ConfigHelper configHelper;

  @Inject
  private Gson gson;

  @Inject
  private UpgradeContextFactory upgradeContextFactory;

  @Inject
  private RepositoryVersionHelper repoVersionHelper;

  /**
   * Used for injecting hooks and common-services into the command.
   */
  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private Configuration configuration;

  /**
   * Used to get service groups (and from them mpacks) so that we can set the
   * right information on the command.
   */
  @Inject
  private ServiceGroupDAO serviceGroupDAO;

  /**
   * Used for retrieving mpack entities by their ID.
   */
  @Inject
  private MpackDAO mpackDAO;

  @AssistedInject
  public ExecutionCommandWrapper(@Assisted String jsonExecutionCommand) {
    this.jsonExecutionCommand = jsonExecutionCommand;
  }

  @AssistedInject
  public ExecutionCommandWrapper(@Assisted ExecutionCommand executionCommand) {
    this.executionCommand = executionCommand;
  }

  /**
   * Gets the execution command by either de-serializing the backing JSON
   * command or returning the encapsulated instance which has already been
   * de-serialized.
   * <p/>
   * If the {@link ExecutionCommand} has configuration tags which need to be
   * refreshed, then this method will lookup the appropriate configuration tags
   * before building the final configurations to set ont he command. Therefore,
   * the {@link ExecutionCommand} is allowed to have no configuration tags as
   * long as it has been instructed to set updated ones at execution time.
   *
   * @return
   */
  public ExecutionCommand getExecutionCommand() {
    if (executionCommand != null) {
      return executionCommand;
    }

    if (null == jsonExecutionCommand) {
      throw new RuntimeException(
          "Invalid ExecutionCommandWrapper, both object and string representations are null");
    }

    try {
      executionCommand = gson.fromJson(jsonExecutionCommand, ExecutionCommand.class);

      // sanity; if no configurations, just initialize to prevent NPEs
      if (null == executionCommand.getConfigurations()) {
        executionCommand.setConfigurations(new TreeMap<>());
      }

      Map<String, Map<String, String>> configurations = executionCommand.getConfigurations();

      // For a configuration type, both tag and an actual configuration can be stored
      // Configurations from the tag is always expanded and then over-written by the actual
      // global:version1:{a1:A1,b1:B1,d1:D1} + global:{a1:A2,c1:C1,DELETED_d1:x} ==>
      // global:{a1:A2,b1:B1,c1:C1}
      Long clusterId = hostRoleCommandDAO.findByPK(
          executionCommand.getTaskId()).getStage().getClusterId();

      Cluster cluster = clusters.getClusterById(clusterId);

      // Execution commands may have config-tags already set during their creation.
      // However, these tags become stale at runtime when other
      // ExecutionCommands run and change the desired configs (like
      // ConfigureAction). Hence an ExecutionCommand can specify which
      // config-types should be refreshed at runtime. Specifying <code>*</code>
      // will result in all config-type tags to be refreshed to the latest
      // cluster desired-configs. Additionally, there may be no configuration
      // tags set but refresh might be set to *. In this case, they should still
      // be refreshed with the latest.
      boolean refreshConfigTagsBeforeExecution = executionCommand.getForceRefreshConfigTagsBeforeExecution();
      if (refreshConfigTagsBeforeExecution) {
        Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();

        Map<String, Map<String, String>> configurationTags = configHelper.getEffectiveDesiredTags(
            cluster, executionCommand.getHostname(), desiredConfigs);

        LOG.debug(
            "While scheduling task {} on cluster {}, configurations are being refreshed using desired configurations of {}",
            executionCommand.getTaskId(), cluster.getClusterName(), desiredConfigs);

        // then clear out any existing configurations so that all of the new
        // configurations are forcefully applied
        configurations.clear();
        executionCommand.setConfigurationTags(configurationTags);
      }

      // now that the tags have been updated (if necessary), fetch the
      // configurations
      Map<String, Map<String, String>> configurationTags = executionCommand.getConfigurationTags();
      configHelper.getAndMergeHostConfigs(configurations, configurationTags, cluster);
      configHelper.getAndMergeHostConfigAttributes(executionCommand.getConfigurationAttributes(),
          configurationTags, cluster);

      setVersions(cluster);

      // provide some basic information about a cluster upgrade if there is one
      // in progress
      UpgradeEntity upgrade = cluster.getUpgradeInProgress();
      if (null != upgrade) {
        UpgradeContext upgradeContext = upgradeContextFactory.create(cluster, upgrade);
        UpgradeSummary upgradeSummary = upgradeContext.getUpgradeSummary();

        executionCommand.setUpgradeSummary(upgradeSummary);
      }

      ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.find(clusterId,
          executionCommand.getServiceGroupName());

      StackEntity stack = serviceGroupEntity.getStack();
      Long mpackId = stack.getMpackId();
      Mpack mpack = ambariMetaInfo.getMpack(mpackId);

      if (null == executionCommand.getMpackId()) {
        executionCommand.setMpackId(mpackId);
      }

      // setting repositoryFile
      final Host host = cluster.getHost(executionCommand.getHostname());  // can be null on internal commands
      if (null == executionCommand.getRepositoryFile() && null != host && mpackId != null) {
        MpackEntity mpackEntity = mpackDAO.findById(mpackId);
        RepoOsEntity osEntity = repoVersionHelper.getOSEntityForHost(mpackEntity, host);
        final CommandRepository commandRepository = repoVersionHelper.getCommandRepository(mpack, osEntity);
        executionCommand.setRepositoryFile(commandRepository);
      }

    } catch (ClusterNotFoundException cnfe) {
      // it's possible that there are commands without clusters; in such cases,
      // just return the de-serialized command and don't try to read configs
      LOG.warn(
          "Unable to lookup the cluster by ID; assuming that there is no cluster and therefore no configs for this execution command: {}",
          cnfe.getMessage());

      return executionCommand;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return executionCommand;
  }

  public void setVersions(Cluster cluster) {
    String serviceGroupName = executionCommand.getServiceGroupName();
    String serviceName = executionCommand.getServiceName();
    String componentName = executionCommand.getComponentName();

    String serviceType = null;

    try {
      Mpack mpack = null;
      StackEntity stackEntity = null;

      if (StringUtils.isNotBlank(serviceGroupName)) {
        ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.find(cluster.getClusterId(), serviceGroupName);
        stackEntity = serviceGroupEntity.getStack();
        mpack = ambariMetaInfo.getMpack(stackEntity.getMpackId());
      }

      Service service = cluster.getService(serviceGroupName, serviceName);
      ServiceComponent serviceComponent = null;
      if (null != service) {
        serviceType = service.getServiceType();

        if (StringUtils.isNotBlank(componentName)) {
          serviceComponent = service.getServiceComponent(componentName);
        }
      }

      ModuleComponent moduleComponent = null;
      Map<String, String> commandParams = executionCommand.getCommandParams();
      if (null != mpack && StringUtils.isNotBlank(serviceName) && StringUtils.isNotBlank(componentName)) {
        // only set the version if it's not set and this is NOT an install
        // command

        moduleComponent = mpack.getModuleComponent(serviceType, serviceComponent.getType());
      }

      if (null != moduleComponent) {
        if (!commandParams.containsKey(VERSION)
            && executionCommand.getRoleCommand() != RoleCommand.INSTALL) {
          commandParams.put(VERSION, moduleComponent.getVersion());
        }
      }

      if (null != stackEntity) {
        StackId stackId = new StackId(stackEntity);
        ambariMetaInfo.getStack(stackId.getStackName(),
          stackId.getStackVersion());

        if (!commandParams.containsKey(HOOKS_FOLDER)) {
          commandParams.put(HOOKS_FOLDER,configuration.getProperty(Configuration.HOOKS_FOLDER));
        }

        if (!commandParams.containsKey(SERVICE_PACKAGE_FOLDER)) {
          if (!StringUtils.isEmpty(serviceType)) {
            ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
              stackId.getStackVersion(), serviceType);

            commandParams.put(SERVICE_PACKAGE_FOLDER, serviceInfo.getServicePackageFolder());
          }
        }
      }


      // set the desired versions of versionable components.  This is safe even during an upgrade because
      // we are "loading-late": components that have not yet upgraded in an EU will have the correct versions.
      executionCommand.setComponentVersions(cluster);
    } catch (ServiceNotFoundException serviceNotFoundException) {
      // it's possible that there are commands specified for a service where
      // the service doesn't exist yet.  Ignore nulls to avoid flooding the log.
      if (null != serviceName) {
        LOG.warn(
          "The service {} is not installed in the cluster. No repository version will be sent for this command.",
          serviceName);
      }
    } catch (AmbariException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the type of command by deserializing the JSON and invoking
   * {@link ExecutionCommand#getCommandType()}.
   *
   * @return
   */
  public AgentCommandType getCommandType() {
    if (executionCommand != null) {
      return executionCommand.getCommandType();
    }

    if (null == jsonExecutionCommand) {
      throw new RuntimeException(
          "Invalid ExecutionCommandWrapper, both object and string" + " representations are null");
    }

    return gson.fromJson(jsonExecutionCommand,
        ExecutionCommand.class).getCommandType();
  }

  public String getJson() {
    if (jsonExecutionCommand != null) {
      return jsonExecutionCommand;
    } else if (executionCommand != null) {
      jsonExecutionCommand = gson.toJson(executionCommand);
      return jsonExecutionCommand;
    } else {
      throw new RuntimeException(
          "Invalid ExecutionCommandWrapper, both object and string"
              + " representations are null");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ExecutionCommandWrapper wrapper = (ExecutionCommandWrapper) o;

    if (executionCommand != null && wrapper.executionCommand != null) {
      return executionCommand.equals(wrapper.executionCommand);
    } else {
      return getJson().equals(wrapper.getJson());
    }
  }

  @Override
  public int hashCode() {
    if (executionCommand != null) {
      return executionCommand.hashCode();
    } else if (jsonExecutionCommand != null) {
      return jsonExecutionCommand.hashCode();
    }
    throw new RuntimeException("Invalid Wrapper object");
  }

  void invalidateJson() {
    if (executionCommand == null) {
      throw new RuntimeException("Invalid Wrapper object");
    }
    jsonExecutionCommand = null;
  }
}
