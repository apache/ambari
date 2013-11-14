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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class containing logic to process custom command execution requests
 */
public class AmbariCustomCommandExecutionHelper {
  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariCustomCommandExecutionHelper.class);
  private ActionMetadata actionMetadata;
  private Clusters clusters;
  private AmbariManagementControllerImpl amcImpl;

  public AmbariCustomCommandExecutionHelper(ActionMetadata actionMetadata, Clusters clusters,
                                            AmbariManagementControllerImpl amcImpl) {
    this.amcImpl = amcImpl;
    this.actionMetadata = actionMetadata;
    this.clusters = clusters;
  }

  public void validateCustomCommand(ExecuteActionRequest actionRequest) throws AmbariException {
    if (actionRequest.getServiceName() == null
        || actionRequest.getServiceName().isEmpty()
        || actionRequest.getCommandName() == null
        || actionRequest.getCommandName().isEmpty()) {
      throw new AmbariException("Invalid request : " + "cluster="
          + actionRequest.getClusterName() + ", service="
          + actionRequest.getServiceName() + ", command="
          + actionRequest.getCommandName());
    }

    LOG.info("Received a command execution request"
        + ", clusterName=" + actionRequest.getClusterName()
        + ", serviceName=" + actionRequest.getServiceName()
        + ", request=" + actionRequest.toString());

    if (!isValidCommand(actionRequest.getCommandName(), actionRequest.getServiceName())) {
      throw new AmbariException(
          "Unsupported action " + actionRequest.getCommandName() + " for " + actionRequest.getServiceName());
    }
  }

  private Boolean isValidCommand(String command, String service) {
    List<String> actions = actionMetadata.getActions(service);
    if (actions == null || actions.size() == 0) {
      return false;
    }

    if (!actions.contains(command)) {
      return false;
    }

    return true;
  }

  public void addAction(ExecuteActionRequest actionRequest, Stage stage,
                        Configuration configuration, HostsMap hostsMap, Map<String, String> hostLevelParams)
      throws AmbariException {
    if (actionRequest.getCommandName().contains("SERVICE_CHECK")) {
      addServiceCheckAction(actionRequest, stage, configuration, hostsMap, hostLevelParams);
    } else if (actionRequest.getCommandName().equals("DECOMMISSION_DATANODE")) {
      addDecommissionDatanodeAction(actionRequest, stage, hostLevelParams);
    } else {
      throw new AmbariException("Unsupported action " + actionRequest.getCommandName());
    }
  }

  private void addServiceCheckAction(ExecuteActionRequest actionRequest, Stage stage,
                                     Configuration configuration, HostsMap hostsMap,
                                     Map<String, String> hostLevelParams)
      throws AmbariException {
    String clusterName = actionRequest.getClusterName();
    String componentName = actionMetadata.getClient(actionRequest
        .getServiceName());

    String hostName;
    if (componentName != null) {
      Map<String, ServiceComponentHost> components = clusters
          .getCluster(clusterName).getService(actionRequest.getServiceName())
          .getServiceComponent(componentName).getServiceComponentHosts();

      if (components.isEmpty()) {
        throw new AmbariException("Hosts not found, component="
            + componentName + ", service=" + actionRequest.getServiceName()
            + ", cluster=" + clusterName);
      }
      hostName = amcImpl.getHealthyHost(components.keySet());
    } else {
      Map<String, ServiceComponent> components = clusters
          .getCluster(clusterName).getService(actionRequest.getServiceName())
          .getServiceComponents();

      if (components.isEmpty()) {
        throw new AmbariException("Components not found, service="
            + actionRequest.getServiceName() + ", cluster=" + clusterName);
      }

      ServiceComponent serviceComponent = components.values().iterator()
          .next();

      if (serviceComponent.getServiceComponentHosts().isEmpty()) {
        throw new AmbariException("Hosts not found, component="
            + serviceComponent.getName() + ", service="
            + actionRequest.getServiceName() + ", cluster=" + clusterName);
      }

      hostName = serviceComponent.getServiceComponentHosts().keySet()
          .iterator().next();
    }

    stage.addHostRoleExecutionCommand(hostName, Role.valueOf(actionRequest
        .getCommandName()), RoleCommand.EXECUTE,
        new ServiceComponentHostOpInProgressEvent(componentName, hostName,
            System.currentTimeMillis()), clusterName, actionRequest
        .getServiceName());

    stage.getExecutionCommandWrapper(hostName, actionRequest.getCommandName()).getExecutionCommand()
        .setRoleParams(actionRequest.getParameters());

    Cluster cluster = clusters.getCluster(clusterName);

    // [ type -> [ key, value ] ]
    Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String, String>>();
    Map<String, Map<String, String>> configTags = amcImpl.findConfigurationTagsWithOverrides(cluster, hostName);

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
        actionRequest.getCommandName()).getExecutionCommand();

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationTags(configTags);
    execCmd.setHostLevelParams(hostLevelParams);

    // Generate cluster host info
    execCmd.setClusterHostInfo(
        StageUtils.getClusterHostInfo(clusters.getHostsForCluster(clusterName), cluster, hostsMap, configuration));
  }

  private void addDecommissionDatanodeAction(ExecuteActionRequest decommissionRequest, Stage stage,
                                             Map<String, String> hostLevelParams)
      throws AmbariException {
    String hdfsExcludeFileType = "hdfs-exclude-file";
    // Find hdfs admin host, just decommission from namenode.
    String clusterName = decommissionRequest.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    String serviceName = decommissionRequest.getServiceName();
    String namenodeHost = clusters.getCluster(clusterName)
        .getService(serviceName).getServiceComponent(Role.NAMENODE.toString())
        .getServiceComponentHosts().keySet().iterator().next();

    String excludeFileTag = null;
    if (decommissionRequest.getParameters() != null
        && (decommissionRequest.getParameters().get("excludeFileTag") != null)) {
      excludeFileTag = decommissionRequest.getParameters()
          .get("excludeFileTag");
    }

    if (excludeFileTag == null) {
      throw new AmbariException("No exclude file specified"
          + " when decommissioning datanodes. Provide parameter excludeFileTag with the tag for config type "
          + hdfsExcludeFileType);
    }

    Config config = clusters.getCluster(clusterName).getConfig(
        hdfsExcludeFileType, excludeFileTag);
    if (config == null) {
      throw new AmbariException("Decommissioning datanodes requires the cluster to be associated with config type " +
          hdfsExcludeFileType + " with a list of datanodes to be decommissioned (\"datanodes\" : list).");
    }

    LOG.info("Decommissioning data nodes: " + config.getProperties().get("datanodes") +
        " " + hdfsExcludeFileType + " tag: " + excludeFileTag);

    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();


    Map<String, Map<String, String>> configTags = amcImpl.findConfigurationTagsWithOverrides(cluster, namenodeHost);

    // Add the tag for hdfs-exclude-file
    Map<String, String> excludeTags = new HashMap<String, String>();
    excludeTags.put(ConfigHelper.CLUSTER_DEFAULT_TAG, config.getVersionTag());
    configTags.put(hdfsExcludeFileType, excludeTags);

    stage.addHostRoleExecutionCommand(
        namenodeHost,
        Role.DECOMMISSION_DATANODE,
        RoleCommand.EXECUTE,
        new ServiceComponentHostOpInProgressEvent(Role.DECOMMISSION_DATANODE
            .toString(), namenodeHost, System.currentTimeMillis()),
        clusterName, serviceName);

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(namenodeHost,
        Role.DECOMMISSION_DATANODE.toString()).getExecutionCommand();

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationTags(configTags);
    execCmd.setHostLevelParams(hostLevelParams);
  }
}