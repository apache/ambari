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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.utils.StageUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionCommandWrapper {
  @Inject
  static Injector injector;
  private final static Logger LOG = LoggerFactory.getLogger(ExecutionCommandWrapper.class);
  private static String DELETED = "DELETED_";
  String jsonExecutionCommand = null;
  ExecutionCommand executionCommand = null;

  public ExecutionCommandWrapper(String jsonExecutionCommand) {
    this.jsonExecutionCommand = jsonExecutionCommand;
  }

  public ExecutionCommandWrapper(ExecutionCommand executionCommand) {
    this.executionCommand = executionCommand;
  }

  @SuppressWarnings("serial")
  public ExecutionCommand getExecutionCommand() {
    if (executionCommand != null) {
      return executionCommand;
    } else if (jsonExecutionCommand != null) {
      executionCommand = StageUtils.getGson().fromJson(jsonExecutionCommand, ExecutionCommand.class);

      if (injector == null) {
        throw new RuntimeException("Injector not found, configuration cannot be restored");
      } else if (executionCommand.getConfigurationTags() != null &&
          !executionCommand.getConfigurationTags().isEmpty()) {

        // For a configuration type, both tag and an actual configuration can be stored
        // Configurations from the tag is always expanded and then over-written by the actual
        // global:version1:{a1:A1,b1:B1,d1:D1} + global:{a1:A2,c1:C1,DELETED_d1:x} ==>
        // global:{a1:A2,b1:B1,c1:C1}
        Clusters clusters = injector.getInstance(Clusters.class);
        HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
        Long clusterId = hostRoleCommandDAO.findByPK(
            executionCommand.getTaskId()).getStage().getClusterId();

        try {
          Cluster cluster = clusters.getClusterById(clusterId);
          ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
          Map<String, Map<String, String>> configurationTags = executionCommand.getConfigurationTags();

          // Execution commands have config-tags already set during their creation. However, these
          // tags become stale at runtime when other ExecutionCommands run and change the desired
          // configs (like ConfigureAction). Hence an ExecutionCommand can specify which config-types
          // should be refreshed at runtime. Specifying <code>*</code> will result in all config-type
          // tags to be refreshed to the latest cluster desired-configs.
          Set<String> refreshConfigTagsBeforeExecution = executionCommand.getForceRefreshConfigTagsBeforeExecution();
          if (refreshConfigTagsBeforeExecution != null && !refreshConfigTagsBeforeExecution.isEmpty()) {
            AmbariManagementController managementController = AmbariServer.getController();
            Map<String, Map<String, String>> configTags = managementController.findConfigurationTagsWithOverrides(
                cluster, executionCommand.getHostname());
            for (String refreshConfigTag : refreshConfigTagsBeforeExecution) {
              if ("*".equals(refreshConfigTag)) {
                // if forcing a refresh of *, then clear out any existing
                // configurations so that all of the new configurations are
                // forcefully applied
                LOG.debug("ExecutionCommandWrapper.getExecutionCommand: refreshConfigTag set to {}, so clearing config for full refresh.", refreshConfigTag);
                executionCommand.getConfigurations().clear();
                configurationTags = configTags;
                executionCommand.setConfigurationTags(configTags);
                break;
              } else if (configurationTags.containsKey(refreshConfigTag) && configTags.containsKey(refreshConfigTag)) {
                configurationTags.put(refreshConfigTag, configTags.get(refreshConfigTag));
              }
            }
          }

          Map<String, Map<String, String>> configProperties = configHelper
            .getEffectiveConfigProperties(cluster, configurationTags);

          // Apply the configurations saved with the Execution Cmd on top of
          // derived configs - This will take care of all the hacks
          for (Map.Entry<String, Map<String, String>> entry : configProperties.entrySet()) {
            String type = entry.getKey();
            Map<String, String> allLevelMergedConfig = entry.getValue();

            if (executionCommand.getConfigurations().containsKey(type)) {
              Map<String, String> mergedConfig =
                configHelper.getMergedConfig(allLevelMergedConfig,
                  executionCommand.getConfigurations().get(type));

              executionCommand.getConfigurations().get(type).clear();
              executionCommand.getConfigurations().get(type).putAll(mergedConfig);

            } else {
              executionCommand.getConfigurations().put(type, new HashMap<String, String>());
              executionCommand.getConfigurations().get(type).putAll(allLevelMergedConfig);
            }
          }

          Map<String, Map<String, Map<String, String>>> configAttributes = configHelper.getEffectiveConfigAttributes(cluster,
              executionCommand.getConfigurationTags());

          for (Map.Entry<String, Map<String, Map<String, String>>> attributesOccurrence : configAttributes.entrySet()) {
            String type = attributesOccurrence.getKey();
            Map<String, Map<String, String>> attributes = attributesOccurrence.getValue();

            if (executionCommand.getConfigurationAttributes() != null) {
              if (!executionCommand.getConfigurationAttributes().containsKey(type)) {
                executionCommand.getConfigurationAttributes().put(type, new TreeMap<String, Map<String, String>>());
              }
              configHelper.cloneAttributesMap(attributes, executionCommand.getConfigurationAttributes().get(type));
            }
          }

        } catch (AmbariException e) {
          throw new RuntimeException(e);
        }
      }

      return executionCommand;
    } else {
      throw new RuntimeException(
          "Invalid ExecutionCommandWrapper, both object and string"
              + " representations are null");
    }
  }

  public String getJson() {
    if (jsonExecutionCommand != null) {
      return jsonExecutionCommand;
    } else if (executionCommand != null) {
      jsonExecutionCommand = StageUtils.getGson().toJson(executionCommand);
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
