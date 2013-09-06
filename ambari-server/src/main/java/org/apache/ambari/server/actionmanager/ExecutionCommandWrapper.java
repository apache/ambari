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
  private static Log LOG = LogFactory.getLog(ExecutionCommandWrapper.class);
  @Inject
  static Injector injector;

  String jsonExecutionCommand = null;
  ExecutionCommand executionCommand = null;

  public ExecutionCommandWrapper(String jsonExecutionCommand) {
    this.jsonExecutionCommand = jsonExecutionCommand;
  }

  public ExecutionCommandWrapper(ExecutionCommand executionCommand) {
    this.executionCommand = executionCommand;
  }

  public ExecutionCommand getExecutionCommand() {
    if (executionCommand != null) {
      return executionCommand;
    } else if (jsonExecutionCommand != null) {
//      try {
      executionCommand = StageUtils.getGson().fromJson(jsonExecutionCommand, ExecutionCommand.class);

      if (injector == null) {
        throw new RuntimeException("Injector not found, configuration cannot be restored");
      } else if ((executionCommand.getConfigurations() == null || executionCommand.getConfigurations().isEmpty()) &&
          executionCommand.getConfigurationTags() != null &&
          !executionCommand.getConfigurationTags().isEmpty()) {

        Clusters clusters = injector.getInstance(Clusters.class);
        HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
        Long clusterId = hostRoleCommandDAO.findByPK(executionCommand.getTaskId()).getStage().getCluster().getClusterId();

        try {
          Cluster cluster = clusters.getClusterById(clusterId);
          Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();

          for (Map.Entry<String, Map<String, String>> entry : executionCommand.getConfigurationTags().entrySet()) {
            String type = entry.getKey();
            Map<String, String> tags = entry.getValue();

            if (!configurations.containsKey(type)) {
              configurations.put(type, new HashMap<String, String>());
            }

            String tag;

            //perform override
            //TODO align with configs override logic
            tag = tags.get("host_override_tag");
            tag = tag == null ? tags.get("service_override_tag") : tag;
            tag = tag == null ? tags.get("tag") : tag;

            if (tag != null) {
              Config config = cluster.getConfig(type, tag);
              configurations.get(type).putAll(config.getProperties());
            }
          }

          executionCommand.setConfigurations(configurations);
        } catch (AmbariException e) {
          throw new RuntimeException(e);
        }
      }

      return executionCommand;
//      } catch (IOException e) {
//        throw new RuntimeException(e);
//      }
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
//      try {
        jsonExecutionCommand = StageUtils.getGson().toJson(executionCommand);
        return jsonExecutionCommand;
//      } catch (JAXBException e) {
//        throw new RuntimeException(e);
//      } catch (IOException e) {
//        throw new RuntimeException(e);
//      }
    } else {
      throw new RuntimeException(
          "Invalid ExecutionCommandWrapper, both object and string"
              + " representations are null");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

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
