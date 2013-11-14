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
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.actionmanager.ActionDefinition;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Helper class containing logic to process custom action execution requests
 */
public class AmbariActionExecutionHelper {
  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariCustomCommandExecutionHelper.class);
  private ActionMetadata actionMetadata;
  private Clusters clusters;
  private AmbariManagementControllerImpl amcImpl;
  private ActionManager actionManager;
  private AmbariMetaInfo ambariMetaInfo;

  public AmbariActionExecutionHelper(ActionMetadata actionMetadata, Clusters clusters,
                                     AmbariManagementControllerImpl amcImpl) {
    this.amcImpl = amcImpl;
    this.actionMetadata = actionMetadata;
    this.clusters = clusters;
    this.actionManager = amcImpl.getActionManager();
    this.ambariMetaInfo = amcImpl.getAmbariMetaInfo();
  }

  /**
   * Validates the request to execute an action
   *
   * @param actionRequest
   * @param cluster
   * @return
   * @throws AmbariException
   */
  public ActionExecutionContext validateCustomAction(ExecuteActionRequest actionRequest, Cluster cluster)
      throws AmbariException {
    if (actionRequest.getActionName() == null || actionRequest.getActionName().isEmpty()) {
      throw new AmbariException("Action name must be specified");
    }

    ActionDefinition actionDef = actionManager.getActionDefinition(actionRequest.getActionName());
    if (actionDef == null) {
      throw new AmbariException("Action " + actionRequest.getActionName() + " does not exist");
    }

    StackId stackId = cluster.getCurrentStackVersion();
    String expectedService = actionDef.getTargetService() == null ? "" : actionDef.getTargetService();
    String actualService = actionRequest.getServiceName() == null ? "" : actionRequest.getServiceName();
    if (!expectedService.isEmpty() && !actualService.isEmpty() && !expectedService.equals(actualService)) {
      throw new AmbariException("Action " + actionRequest.getActionName() + " targets service " + actualService +
          " that does not match with expected " + expectedService);
    }

    String targetService = expectedService;
    if (targetService == null || targetService.isEmpty()) {
      targetService = actualService;
    }

    if (targetService != null && !targetService.isEmpty()) {
      ServiceInfo serviceInfo;
      try {
        serviceInfo = ambariMetaInfo.getService(stackId.getStackName(), stackId.getStackVersion(),
            targetService);
      } catch (StackAccessException se) {
        serviceInfo = null;
      }

      if (serviceInfo == null) {
        throw new AmbariException("Action " + actionRequest.getActionName() + " targets service " + targetService +
            " that does not exist.");
      }
    }

    String expectedComponent = actionDef.getTargetComponent() == null ? "" : actionDef.getTargetComponent();
    String actualComponent = actionRequest.getComponentName() == null ? "" : actionRequest.getComponentName();
    if (!expectedComponent.isEmpty() && !actualComponent.isEmpty() && !expectedComponent.equals(actualComponent)) {
      throw new AmbariException("Action " + actionRequest.getActionName() + " targets component " + actualComponent +
          " that does not match with expected " + expectedComponent);
    }

    String targetComponent = expectedComponent;
    if (targetComponent == null || targetComponent.isEmpty()) {
      targetComponent = actualComponent;
    }

    if (!targetComponent.isEmpty() && targetService.isEmpty()) {
      throw new AmbariException("Action " + actionRequest.getActionName() + " targets component " + targetComponent +
          " without specifying the target service.");
    }

    if (targetComponent != null && !targetComponent.isEmpty()) {
      ComponentInfo compInfo;
      try {
        compInfo = ambariMetaInfo.getComponent(stackId.getStackName(), stackId.getStackVersion(),
            targetService, targetComponent);
      } catch (StackAccessException se) {
        compInfo = null;
      }

      if (compInfo == null) {
        throw new AmbariException("Action " + actionRequest.getActionName() + " targets component " + targetComponent +
            " that does not exist.");
      }
    }

    if (actionDef.getInputs() != null) {
      String[] inputs = actionDef.getInputs().split(",");
      for (String input : inputs) {
        if (!input.trim().isEmpty() && !actionRequest.getParameters().containsKey(input.trim())) {
          throw new AmbariException("Action " + actionRequest.getActionName() + " requires input '" +
              input.trim() + "' that is not provided.");
        }
      }
    }

    if (actionDef.getTargetType() == TargetHostType.SPECIFIC
        || (targetService.isEmpty() && targetService.isEmpty())) {
      if (actionRequest.getHosts().size() == 0) {
        throw new AmbariException("Action " + actionRequest.getActionName() + " requires explicit target host(s)" +
            " that is not provided.");
      }
    }

    LOG.info("Received action execution request"
        + ", clusterName=" + actionRequest.getClusterName()
        + ", request=" + actionRequest.toString());

    ActionExecutionContext actionExecutionContext = new ActionExecutionContext(
        actionRequest.getClusterName(), actionRequest.getActionName(), targetService, targetComponent,
        actionRequest.getHosts(), actionRequest.getParameters(), actionDef.getTargetType(),
        actionDef.getDefaultTimeout());

    return actionExecutionContext;
  }

  /**
   * Add tasks to the stage based on the requested action execution
   *
   * @param actionContext   the context associated with the action
   * @param stage           stage into which tasks must be inserted
   * @param configuration
   * @param hostsMap
   * @param hostLevelParams
   * @throws AmbariException
   */
  public void addAction(ActionExecutionContext actionContext, Stage stage,
                        Configuration configuration, HostsMap hostsMap, Map<String, String> hostLevelParams)
      throws AmbariException {
    String actionName = actionContext.getActionName();
    String clusterName = actionContext.getClusterName();
    String serviceName = actionContext.getServiceName();
    String componentName = actionContext.getComponentName();

    // List of host to select from
    Set<String> candidateHosts = new HashSet<String>();
    if (!serviceName.isEmpty()) {
      if (!componentName.isEmpty()) {
        Map<String, ServiceComponentHost> componentHosts =
            clusters.getCluster(clusterName).getService(serviceName)
                .getServiceComponent(componentName).getServiceComponentHosts();
        candidateHosts.addAll(componentHosts.keySet());
      } else {
        for (String component : clusters.getCluster(clusterName).getService(serviceName)
            .getServiceComponents().keySet()) {
          Map<String, ServiceComponentHost> componentHosts =
              clusters.getCluster(clusterName).getService(serviceName)
                  .getServiceComponent(component).getServiceComponentHosts();
          candidateHosts.addAll(componentHosts.keySet());
        }
      }
    } else {
      // All hosts are valid target host
      candidateHosts.addAll(amcImpl.getClusters().getHostsForCluster(clusterName).keySet());
    }

    // If request did not specify hosts and there exists no host
    if (actionContext.getHosts().isEmpty() && candidateHosts.isEmpty()) {
      throw new AmbariException("Suitable hosts not found, component="
          + componentName + ", service=" + serviceName
          + ", cluster=" + clusterName + ", actionName=" + actionName);
    }

    // Compare specified hosts to available hosts
    if (!actionContext.getHosts().isEmpty() && !candidateHosts.isEmpty()) {
      for (String hostname : actionContext.getHosts()) {
        if (!candidateHosts.contains(hostname)) {
          throw new AmbariException("Request specifies host " + hostname + " but its not a valid host based on the " +
              "target service=" + serviceName + " and component=" + componentName);
        }
      }
    }

    //Find target hosts to execute
    if (actionContext.getHosts().isEmpty()) {
      TargetHostType hostType = actionContext.getTargetType();
      switch (hostType) {
        case ALL:
          actionContext.getHosts().addAll(candidateHosts);
          break;
        case ANY:
          actionContext.getHosts().add(amcImpl.getHealthyHost(candidateHosts));
          break;
        case MAJORITY:
          for (int i = 0; i < (candidateHosts.size() / 2) + 1; i++) {
            String hostname = amcImpl.getHealthyHost(candidateHosts);
            actionContext.getHosts().add(hostname);
            candidateHosts.remove(hostname);
          }
          break;
        default:
          throw new AmbariException("Unsupported target type=" + hostType);
      }
    }

    //create tasks for each host
    for (String hostName : actionContext.getHosts()) {
      stage.addHostRoleExecutionCommand(hostName, Role.valueOf(actionContext.getActionName()), RoleCommand.ACTIONEXECUTE,
          new ServiceComponentHostOpInProgressEvent(actionContext.getActionName(), hostName,
              System.currentTimeMillis()), clusterName, actionContext.getServiceName());

      stage.getExecutionCommandWrapper(hostName, actionContext.getActionName()).getExecutionCommand()
          .setRoleParams(actionContext.getParameters());

      Cluster cluster = clusters.getCluster(clusterName);

      Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String, String>>();
      Map<String, Map<String, String>> configTags = null;
      if (!actionContext.getServiceName().isEmpty()) {
        configTags = amcImpl.findConfigurationTagsWithOverrides(cluster, hostName);
      }

      ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
          actionContext.getActionName()).getExecutionCommand();

      execCmd.setConfigurations(configurations);
      execCmd.setConfigurationTags(configTags);
      execCmd.setHostLevelParams(hostLevelParams);
      execCmd.setCommandParams(actionContext.getParameters());
      execCmd.setServiceName(serviceName);
      execCmd.setComponentName(componentName);

      // Generate cluster host info
      execCmd.setClusterHostInfo(
          StageUtils.getClusterHostInfo(clusters.getHostsForCluster(clusterName), cluster, hostsMap, configuration));
    }
  }
}
