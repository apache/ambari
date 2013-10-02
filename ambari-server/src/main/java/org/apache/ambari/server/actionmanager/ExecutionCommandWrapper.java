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

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

public class ExecutionCommandWrapper {
  @Inject
  static Injector injector;
  private static Log LOG = LogFactory.getLog(ExecutionCommandWrapper.class);
  private static String DELETED = "DELETED_";
  String jsonExecutionCommand = null;
  ExecutionCommand executionCommand = null;

  public ExecutionCommandWrapper(String jsonExecutionCommand) {
    this.jsonExecutionCommand = jsonExecutionCommand;
  }

  public ExecutionCommandWrapper(ExecutionCommand executionCommand) {
    this.executionCommand = executionCommand;
  }

  public static void applyCustomConfig(Map<String, Map<String, String>> configurations, String type,
                                       String name, String value, Boolean deleted) {
    if (!configurations.containsKey(type)) {
      configurations.put(type, new HashMap<String, String>());
    }
    String nameToUse = deleted ? DELETED + name : name;
    Map<String, String> properties = configurations.get(type);
    if (properties.containsKey(nameToUse)) {
      properties.remove(nameToUse);
    }
    properties.put(nameToUse, value);
  }

  public static Map<String, String> getMergedConfig(Map<String, String> persistedClusterConfig,
                                                    Map<String, String> override) {
    Map<String, String> finalConfig = new HashMap<String, String>(persistedClusterConfig);
    if (override != null && override.size() > 0) {
      for (String overrideKey : override.keySet()) {
        Boolean deleted = 0 == overrideKey.indexOf(DELETED);
        String nameToUse = deleted ? overrideKey.substring(DELETED.length()) : overrideKey;
        if (finalConfig.containsKey(nameToUse)) {
          finalConfig.remove(nameToUse);
        }
        if (!deleted) {
          finalConfig.put(nameToUse, override.get(overrideKey));
        }
      }
    }

    return finalConfig;
  }

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
            executionCommand.getTaskId()).getStage().getCluster().getClusterId();

        try {
          Cluster cluster = clusters.getClusterById(clusterId);
          for (Map.Entry<String, Map<String, String>> entry : executionCommand.getConfigurationTags().entrySet()) {
            String type = entry.getKey();
            Map<String, String> tags = entry.getValue();

            String tag = tags.get("tag");
            
            if (tag != null) {
              Config config = cluster.getConfig(type, tag);
              
              //Merge cluster level configs with service overriden, with host overriden
              Map<String, String> allLevelMergedConfig = new HashMap<String, String>();
              
              allLevelMergedConfig.putAll(config.getProperties());
              
              String serviceTag =  tags.get("service_override_tag");
              if (serviceTag != null) {
                Config configService = cluster.getConfig(type, serviceTag);
                if (configService != null)
                  allLevelMergedConfig = getMergedConfig(allLevelMergedConfig, configService.getProperties());
              }
                
              String hostTag = tags.get("host_override_tag");
              if (hostTag != null) {
                Config configHost = cluster.getConfig(type, hostTag);
                if (configHost != null)
                  allLevelMergedConfig = getMergedConfig(allLevelMergedConfig, configHost.getProperties());
              }
              
              if (executionCommand.getConfigurations().containsKey(type)) {
                Map<String, String> mergedConfig =
                    getMergedConfig(allLevelMergedConfig, executionCommand.getConfigurations().get(type));
                executionCommand.getConfigurations().get(type).clear();
                executionCommand.getConfigurations().get(type).putAll(mergedConfig);

              } else {
                executionCommand.getConfigurations().put(type, new HashMap<String, String>());
                executionCommand.getConfigurations().get(type).putAll(allLevelMergedConfig);
              }
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
