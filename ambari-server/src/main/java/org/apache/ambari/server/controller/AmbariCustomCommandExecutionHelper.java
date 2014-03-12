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

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMPONENT_CATEGORY;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CUSTOM_COMMAND;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCHEMA_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;

/**
 * Helper class containing logic to process custom command execution requests .
 * This class has special support needed for SERVICE_CHECK and DECOMMISSION.
 * These commands are not pass through as Ambari has specific persistence requirements.
 */
@Singleton
public class AmbariCustomCommandExecutionHelper {
  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariCustomCommandExecutionHelper.class);
  // TODO: Remove the hard-coded mapping when stack definition indicates which slave types can be decommissioned
  private static final Map<String, String> masterToSlaveMappingForDecom = new HashMap<String, String>();

  static {
    masterToSlaveMappingForDecom.put("NAMENODE", "DATANODE");
    masterToSlaveMappingForDecom.put("RESOURCEMANAGER", "NODEMANAGER");
    masterToSlaveMappingForDecom.put("HBASE_MASTER", "HBASE_REGIONSERVER");
    masterToSlaveMappingForDecom.put("JOBTRACKER", "TASKTRACKER");
  }

  private static String DECOM_INCLUDED_HOSTS = "included_hosts";
  private static String DECOM_EXCLUDED_HOSTS = "excluded_hosts";
  private static String DECOM_SLAVE_COMPONENT = "slave_type";
  private static String HBASE_MARK_DRAINING_ONLY = "mark_draining_only";
  private static String UPDATE_EXCLUDE_FILE_ONLY = "update_exclude_file_only";
  private static String ALIGN_MAINTENANCE_STATE = "align_maintenance_state";
  @Inject
  private ActionMetadata actionMetadata;
  @Inject
  private Clusters clusters;
  @Inject
  private AmbariManagementController managementController;
  @Inject
  private Gson gson;
  @Inject
  private Configuration configs;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  @Inject
  private ConfigHelper configHelper;

  protected static final String SERVICE_CHECK_COMMAND_NAME = "SERVICE_CHECK";
  protected static final String DECOMMISSION_COMMAND_NAME = "DECOMMISSION";


  private Boolean isServiceCheckCommand(String command, String service) {
    List<String> actions = actionMetadata.getActions(service);
    if (actions == null || actions.size() == 0) {
      return false;
    }

    if (!actions.contains(command)) {
      return false;
    }

    return true;
  }

  private Boolean isValidCustomCommand(String clusterName,
      String serviceName, String componentName, String commandName)
      throws AmbariException {

    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = cluster.getDesiredStackVersion();

    if (componentName == null) {
      return false;
    }
    ComponentInfo componentInfo = ambariMetaInfo.getComponent(
      stackId.getStackName(), stackId.getStackVersion(),
      serviceName, componentName);

    return !(!componentInfo.isCustomCommand(commandName) &&
      !actionMetadata.isDefaultHostComponentCommand(commandName));
  }

  private Boolean isValidCustomCommand(ActionExecutionContext
      actionExecutionContext, RequestResourceFilter resourceFilter)
      throws AmbariException {
    String clusterName = actionExecutionContext.getClusterName();
    String serviceName = resourceFilter.getServiceName();
    String componentName = resourceFilter.getComponentName();
    String commandName = actionExecutionContext.getActionName();

    if (componentName == null) {
      return false;
    }

    return isValidCustomCommand(clusterName, serviceName, componentName, commandName);
  }

  private Boolean isValidCustomCommand(ExecuteActionRequest actionRequest,
      RequestResourceFilter resourceFilter) throws AmbariException {
    String clusterName = actionRequest.getClusterName();
    String serviceName = resourceFilter.getServiceName();
    String componentName = resourceFilter.getComponentName();
    String commandName = actionRequest.getCommandName();

    if (componentName == null) {
      return false;
    }

    return isValidCustomCommand(clusterName, serviceName, componentName, commandName);
  }

  private String getReadableCustomCommandDetail(ActionExecutionContext
        actionRequest, RequestResourceFilter resourceFilter) {
    StringBuilder sb = new StringBuilder();
    sb.append(actionRequest.getActionName());
    if (resourceFilter.getServiceName() != null
        && !resourceFilter.getServiceName().equals("")) {
      sb.append(" ");
      sb.append(resourceFilter.getServiceName());
    }
    if (resourceFilter.getComponentName() != null
        && !resourceFilter.getComponentName().equals("")) {
      sb.append("/");
      sb.append(resourceFilter.getComponentName());
    }
    return sb.toString();
  }

  private void addCustomCommandAction(ActionExecutionContext actionExecutionContext,
                                      RequestResourceFilter resourceFilter,
                                      Stage stage, Map<String, String> hostLevelParams,
                                      Map<String, String> additionalCommandParams,
                                      String commandDetail)
                                      throws AmbariException {

    List<String> hosts = resourceFilter.getHostNames();
    if (hosts.isEmpty()) {
      throw new AmbariException("Invalid request : No hosts specified.");
    }

    String serviceName = resourceFilter.getServiceName();
    String componentName = resourceFilter.getComponentName();
    String commandName = actionExecutionContext.getActionName();

    String clusterName = stage.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = cluster.getDesiredStackVersion();
    AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();
    ServiceInfo serviceInfo = ambariMetaInfo.getServiceInfo
      (stackId.getStackName(), stackId.getStackVersion(), serviceName);
    StackInfo stackInfo = ambariMetaInfo.getStackInfo
      (stackId.getStackName(), stackId.getStackVersion());

    long nowTimestamp = System.currentTimeMillis();

    for (String hostName : hosts) {

      Host host = clusters.getHost(hostName);

      stage.addHostRoleExecutionCommand(hostName, Role.valueOf(componentName),
          RoleCommand.CUSTOM_COMMAND,
          new ServiceComponentHostOpInProgressEvent(componentName,
              hostName, nowTimestamp), cluster.getClusterName(), serviceName);

      Map<String, Map<String, String>> configurations =
          new TreeMap<String, Map<String, String>>();
      Map<String, Map<String, String>> configTags =
          managementController.findConfigurationTagsWithOverrides(cluster, hostName);

      HostRoleCommand cmd = stage.getHostRoleCommand(hostName, componentName);
      if (cmd != null) {
        cmd.setCommandDetail(commandDetail);
        cmd.setCustomCommandName(commandName);
      }

      ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
          componentName).getExecutionCommand();

      execCmd.setConfigurations(configurations);
      execCmd.setConfigurationTags(configTags);

      execCmd.setClusterHostInfo(
          StageUtils.getClusterHostInfo(clusters.getHostsForCluster(clusterName), cluster));

      hostLevelParams.put(CUSTOM_COMMAND, commandName);
      // Set parameters required for re-installing clients on restart
      hostLevelParams.put(REPO_INFO, getRepoInfo
        (cluster, host));
      execCmd.setHostLevelParams(hostLevelParams);

      Map<String, String> commandParams = new TreeMap<String, String>();
      commandParams.put(SCHEMA_VERSION, serviceInfo.getSchemaVersion());
      if (additionalCommandParams != null) {
        for (String key : additionalCommandParams.keySet()) {
          commandParams.put(key, additionalCommandParams.get(key));
        }
      }

      String commandTimeout = configs.getDefaultAgentTaskTimeout();

      ComponentInfo componentInfo = ambariMetaInfo.getComponent(
          stackId.getStackName(), stackId.getStackVersion(),
          serviceName, componentName);

      if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
        // Service check command is not custom command
        CommandScriptDefinition script = componentInfo.getCommandScript();

        if (script != null) {
          commandParams.put(SCRIPT, script.getScript());
          commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
          if (script.getTimeout() > 0) {
            commandTimeout = String.valueOf(script.getTimeout());
          }
        } else {
          String message = String.format("Component %s has not command script " +
              "defined. It is not possible to send command for " +
              "this service", componentName);
          throw new AmbariException(message);
        }
        // We don't need package/repo information to perform service check
      }
      commandParams.put(COMMAND_TIMEOUT, commandTimeout);

      commandParams.put(SERVICE_PACKAGE_FOLDER,
          serviceInfo.getServicePackageFolder());
      commandParams.put(HOOKS_FOLDER, stackInfo.getStackHooksFolder());

      execCmd.setCommandParams(commandParams);

      Map<String, String> roleParams = execCmd.getRoleParams();
      if (roleParams == null) {
        roleParams = new TreeMap<String, String>();
      }
      roleParams.put(COMPONENT_CATEGORY, componentInfo.getCategory());
      execCmd.setRoleParams(roleParams);
      
      // if the target is NAGIOS (for example: restart command), make passive info always available
      if (execCmd.getRole().equals(Role.NAGIOS_SERVER.name())) {
        execCmd.setPassiveInfo(
          MaintenanceStateHelper.getMaintenanceHostComponents(clusters, cluster));
      }
      
    }
  }

  private void findHostAndAddServiceCheckAction(ActionExecutionContext
      actionExecutionContext, RequestResourceFilter resourceFilter,
      Stage stage, Map<String, String> hostLevelParams)
      throws AmbariException {

    String clusterName = actionExecutionContext.getClusterName();
    String componentName = actionMetadata.getClient(resourceFilter.getServiceName());
    String serviceName = resourceFilter.getServiceName();
    String smokeTestRole = actionExecutionContext.getActionName();
    long nowTimestamp = System.currentTimeMillis();
    Map<String, String> actionParameters = actionExecutionContext.getParameters();

    String hostName;
    if (componentName != null) {
      Map<String, ServiceComponentHost> components =
        clusters.getCluster(clusterName).getService(serviceName)
          .getServiceComponent(componentName).getServiceComponentHosts();

      if (components.isEmpty()) {
        throw new AmbariException("Hosts not found, component="
            + componentName + ", service = " + serviceName
            + ", cluster = " + clusterName);
      }

      List<String> candidateHosts = resourceFilter.getHostNames();
      if (candidateHosts != null && !candidateHosts.isEmpty()) {
        hostName = managementController.getHealthyHost
          (new HashSet<String>(candidateHosts));

        if (hostName == null) {
          LOG.info("Unable to find a healthy host amongst the provided set of " +
            "hosts. " + candidateHosts);
        }
      } else {
        hostName = managementController.getHealthyHost(components.keySet());
      }

    } else {
      Map<String, ServiceComponent> components = clusters
        .getCluster(clusterName).getService(serviceName).getServiceComponents();

      if (components.isEmpty()) {
        throw new AmbariException("Components not found, service = "
            + serviceName + ", cluster = " + clusterName);
      }

      ServiceComponent serviceComponent = components.values().iterator()
          .next();

      if (serviceComponent.getServiceComponentHosts().isEmpty()) {
        throw new AmbariException("Hosts not found, component="
            + serviceComponent.getName() + ", service = "
            + serviceName + ", cluster = " + clusterName);
      }

      hostName = serviceComponent.getServiceComponentHosts().keySet().iterator().next();
    }

    addServiceCheckAction(stage, hostName, smokeTestRole, nowTimestamp,
        serviceName, componentName, actionParameters,
        hostLevelParams);
  }

  /**
   * Creates and populates service check EXECUTION_COMMAND for host.
   * Not all EXECUTION_COMMAND parameters are populated here because they
   * are not needed by service check.
   */
  public void addServiceCheckAction(Stage stage,
                                    String hostname, String smokeTestRole,
                                    long nowTimestamp,
                                    String serviceName,
                                    String componentName,
                                    Map<String, String> actionParameters,
                                    Map<String, String> hostLevelParams)
                                    throws AmbariException {

    String clusterName = stage.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = cluster.getDesiredStackVersion();
    AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();
    ServiceInfo serviceInfo =
        ambariMetaInfo.getServiceInfo(stackId.getStackName(),
            stackId.getStackVersion(), serviceName);
    StackInfo stackInfo = ambariMetaInfo.getStackInfo(stackId.getStackName(),
      stackId.getStackVersion());


    stage.addHostRoleExecutionCommand(hostname,
        Role.valueOf(smokeTestRole),
        RoleCommand.SERVICE_CHECK,
        new ServiceComponentHostOpInProgressEvent(componentName, hostname,
            nowTimestamp), cluster.getClusterName(), serviceName);

    HostRoleCommand hrc = stage.getHostRoleCommand(hostname, smokeTestRole);
    if (hrc != null) {
      hrc.setCommandDetail(String.format("%s %s", RoleCommand.SERVICE_CHECK.toString(), serviceName));
    }
    // [ type -> [ key, value ] ]
    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();
    Map<String, Map<String, String>> configTags =
        managementController.findConfigurationTagsWithOverrides(cluster, hostname);

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostname,
        smokeTestRole).getExecutionCommand();

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationTags(configTags);

    // Generate cluster host info
    execCmd.setClusterHostInfo(
        StageUtils.getClusterHostInfo(clusters.getHostsForCluster(clusterName), cluster));

    if (hostLevelParams == null) {
      hostLevelParams = new TreeMap<String, String>();
    }
    execCmd.setHostLevelParams(hostLevelParams);

    Map<String, String> commandParams = new TreeMap<String, String>();
    commandParams.put(SCHEMA_VERSION, serviceInfo.getSchemaVersion());

    String commandTimeout = configs.getDefaultAgentTaskTimeout();


    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      // Service check command is not custom command
      CommandScriptDefinition script = serviceInfo.getCommandScript();
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
        if (script.getTimeout() > 0) {
          commandTimeout = String.valueOf(script.getTimeout());
        }
      } else {
        String message = String.format("Service %s has no command script " +
            "defined. It is not possible to run service check" +
            " for this service", serviceName);
        throw new AmbariException(message);
      }
      // We don't need package/repo information to perform service check
    }
    commandParams.put(COMMAND_TIMEOUT, commandTimeout);

    commandParams.put(SERVICE_PACKAGE_FOLDER,
        serviceInfo.getServicePackageFolder());
    commandParams.put(HOOKS_FOLDER, stackInfo.getStackHooksFolder());

    execCmd.setCommandParams(commandParams);

    if (actionParameters != null) { // If defined
      execCmd.setRoleParams(actionParameters);
    }
  }

  private Set<String> getHostList(Map<String, String> cmdParameters, String key) {
    Set<String> hosts = new HashSet<String>();
    if (cmdParameters.containsKey(key)) {
      String allHosts = cmdParameters.get(key);
      if (allHosts != null) {
        for (String hostName : allHosts.trim().split(",")) {
          hosts.add(hostName.trim());
        }
      }
    }
    return hosts;
  }

  /**
   * Processes decommission command. Modifies the host components as needed and then
   * calls into the implementation of a custom command
   */
  private void addDecommissionAction(ActionExecutionContext actionExecutionContext,
                                     RequestResourceFilter resourceFilter,
                                     Stage stage, Map<String, String> hostLevelParams)
                                     throws AmbariException {

    String clusterName = actionExecutionContext.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    String serviceName = resourceFilter.getServiceName();
    String componentName = resourceFilter.getComponentName();
    List<String> hosts = resourceFilter.getHostNames();

    if (hosts != null && !hosts.isEmpty()) {
      throw new AmbariException("Decommission command cannot be issued with " +
        "target host(s) specified.");
    }

    //Get all hosts to be added and removed
    Set<String> excludedHosts = getHostList(actionExecutionContext.getParameters(),
                                            DECOM_EXCLUDED_HOSTS);
    Set<String> includedHosts = getHostList(actionExecutionContext.getParameters(),
                                            DECOM_INCLUDED_HOSTS);
    String slaveCompType = actionExecutionContext.getParameters().get(DECOM_SLAVE_COMPONENT);

    Set<String> cloneSet = new HashSet<String>(excludedHosts);
    cloneSet.retainAll(includedHosts);
    if (cloneSet.size() > 0) {
      throw new AmbariException("Same host cannot be specified for inclusion " +
        "as well as exclusion. Hosts: " + cloneSet.toString());
    }

    Service service = cluster.getService(serviceName);
    if (service == null) {
      throw new AmbariException("Specified service " + serviceName +
        " is not a valid/deployed service.");
    }

    String masterCompType = componentName;
    Map<String, ServiceComponent> svcComponents = service.getServiceComponents();
    if (!svcComponents.containsKey(masterCompType)) {
      throw new AmbariException("Specified component " + masterCompType +
        " does not belong to service " + serviceName + ".");
    }

    ServiceComponent masterComponent = svcComponents.get(masterCompType);
    if (!masterComponent.isMasterComponent()) {
      throw new AmbariException("Specified component " + masterCompType +
        " is not a MASTER for service " + serviceName + ".");
    }

    if (!masterToSlaveMappingForDecom.containsKey(masterCompType)) {
      throw new AmbariException("Decommissioning is not supported for " + masterCompType);
    }

    // Find the slave component
    if (slaveCompType == null || slaveCompType.equals("")) {
      slaveCompType = masterToSlaveMappingForDecom.get(masterCompType);
    } else if (!masterToSlaveMappingForDecom.get(masterCompType).equals(slaveCompType)) {
      throw new AmbariException("Component " + slaveCompType + " is not supported for decommissioning.");
    }

    String isDrainOnlyRequest = actionExecutionContext.getParameters().get(HBASE_MARK_DRAINING_ONLY);
    if (isDrainOnlyRequest != null && !slaveCompType.equals(Role.HBASE_REGIONSERVER.name())) {
      throw new AmbariException(HBASE_MARK_DRAINING_ONLY + " is not a valid parameter for " + masterCompType);
    }

    // Decommission only if the sch is in state STARTED or INSTALLED
    for (ServiceComponentHost sch : svcComponents.get(slaveCompType).getServiceComponentHosts().values()) {
      if (excludedHosts.contains(sch.getHostName())
          && !"true".equals(isDrainOnlyRequest)
          && sch.getState() != State.STARTED) {
        throw new AmbariException("Component " + slaveCompType + " on host " + sch.getHostName() + " cannot be " +
            "decommissioned as its not in STARTED state. Aborting the whole request.");
      }
    }

    String alignMtnStateStr = actionExecutionContext.getParameters().get(ALIGN_MAINTENANCE_STATE);
    boolean alignMtnState = "true".equals(alignMtnStateStr);
    // Set/reset decommissioned flag on all components
    List<String> listOfExcludedHosts = new ArrayList<String>();
    for (ServiceComponentHost sch : svcComponents.get(slaveCompType).getServiceComponentHosts().values()) {
      if (excludedHosts.contains(sch.getHostName())) {
        sch.setComponentAdminState(HostComponentAdminState.DECOMMISSIONED);
        listOfExcludedHosts.add(sch.getHostName());
        if (alignMtnState) {
          sch.setMaintenanceState(MaintenanceState.ON);
        }
        LOG.info("Decommissioning " + slaveCompType + " and marking Maintenance=ON on " + sch.getHostName());
      }
      if (includedHosts.contains(sch.getHostName())) {
        sch.setComponentAdminState(HostComponentAdminState.INSERVICE);
        if (alignMtnState) {
          sch.setMaintenanceState(MaintenanceState.OFF);
        }
        LOG.info("Recommissioning " + slaveCompType + " and marking Maintenance=OFF on " + sch.getHostName());
      }
    }

    // In the event there are more than one master host the following logic is applied
    // -- HDFS/DN, MR1/TT, YARN/NM call refresh node on both
    // -- HBASE/RS call only on one host

    // Ensure host is active
    Map<String, ServiceComponentHost> masterSchs = masterComponent.getServiceComponentHosts();
    String primaryCandidate = null;
    for (String hostName : masterSchs.keySet()) {
      if (primaryCandidate == null) {
        primaryCandidate = hostName;
      } else {
        ServiceComponentHost sch = masterSchs.get(hostName);
        if (sch.getState() == State.STARTED) {
          primaryCandidate = hostName;
        }
      }
    }

    StringBuilder commandDetail = getReadableDecommissionCommandDetail
      (actionExecutionContext, includedHosts, listOfExcludedHosts);

    for (String hostName : masterSchs.keySet()) {
      RequestResourceFilter commandFilter = new RequestResourceFilter(serviceName,
        masterComponent.getName(), Collections.singletonList(hostName));
      List<RequestResourceFilter> resourceFilters = new ArrayList<RequestResourceFilter>();
      resourceFilters.add(commandFilter);

      ActionExecutionContext commandContext = new ActionExecutionContext(
        clusterName, actionExecutionContext.getActionName(), resourceFilters
      );

      String clusterHostInfoJson = StageUtils.getGson().toJson(
          StageUtils.getClusterHostInfo(clusters.getHostsForCluster(cluster.getClusterName()), cluster));

      // Reset cluster host info as it has changed
      stage.setClusterHostInfo(clusterHostInfoJson);

      Map<String, String> commandParams = new HashMap<String, String>();
      if (serviceName.equals(Service.Type.HBASE.name())) {
        commandParams.put(DECOM_EXCLUDED_HOSTS, StringUtils.join(listOfExcludedHosts, ','));
        if ((isDrainOnlyRequest != null) && isDrainOnlyRequest.equals("true")) {
          commandParams.put(HBASE_MARK_DRAINING_ONLY, isDrainOnlyRequest);
        } else {
          commandParams.put(HBASE_MARK_DRAINING_ONLY, "false");
        }
      }

      if (!serviceName.equals(Service.Type.HBASE.name()) || hostName.equals(primaryCandidate)) {
        commandParams.put(UPDATE_EXCLUDE_FILE_ONLY, "false");
        addCustomCommandAction(commandContext, commandFilter, stage,
          hostLevelParams, commandParams, commandDetail.toString());
      }
    }
  }

  private StringBuilder getReadableDecommissionCommandDetail(
      ActionExecutionContext actionExecutionContext, Set<String> includedHosts,
      List<String> listOfExcludedHosts) {
    StringBuilder commandDetail = new StringBuilder();
    commandDetail.append(actionExecutionContext.getActionName());
    if (listOfExcludedHosts.size() > 0) {
      commandDetail.append(", Excluded: ").append(StringUtils.join(listOfExcludedHosts, ','));
    }
    if (includedHosts.size() > 0) {
      commandDetail.append(", Included: ").append(StringUtils.join(includedHosts, ','));
    }
    return commandDetail;
  }

  /**
   * Validate custom command and throw exception is invalid request.
   * @param actionRequest
   * @throws AmbariException
   */
  public void validateAction(ExecuteActionRequest actionRequest) throws AmbariException {

    List<RequestResourceFilter> resourceFilters = actionRequest.getResourceFilters();

    if (resourceFilters == null || resourceFilters.isEmpty()) {
      throw new AmbariException("Command execution cannot proceed without a " +
        "resource filter.");
    }

    for (RequestResourceFilter resourceFilter : resourceFilters) {
      if (resourceFilter.getServiceName() == null
        || resourceFilter.getServiceName().isEmpty()
        || actionRequest.getCommandName() == null
        || actionRequest.getCommandName().isEmpty()) {
        throw new AmbariException("Invalid resource filter : " + "cluster = "
          + actionRequest.getClusterName() + ", service = "
          + resourceFilter.getServiceName() + ", command = "
          + actionRequest.getCommandName());
      }

      if (!isServiceCheckCommand(actionRequest.getCommandName(), resourceFilter.getServiceName())
        && !isValidCustomCommand(actionRequest, resourceFilter)) {
        throw new AmbariException(
          "Unsupported action " + actionRequest.getCommandName() +
            " for Service: " + resourceFilter.getServiceName()
            + " and Component: " + resourceFilter.getComponentName());
      }
    }
  }

  /**
   * Other than Service_Check and Decommission all other commands are pass-through
   * @param actionExecutionContext received request to execute a command
   * @param stage the initial stage for task creation
   * @param hostLevelParams specific parameters for the hosts
   * @throws AmbariException
   */
  public void addExecutionCommandsToStage(ActionExecutionContext actionExecutionContext,
                                          Stage stage, Map<String, String> hostLevelParams)
                                          throws AmbariException {

    List<RequestResourceFilter> resourceFilters = actionExecutionContext.getResourceFilters();

    for (RequestResourceFilter resourceFilter : resourceFilters) {
      LOG.debug("Received a command execution request"
        + ", clusterName=" + actionExecutionContext.getClusterName()
        + ", serviceName=" + resourceFilter.getServiceName()
        + ", request=" + actionExecutionContext.toString());

      if (actionExecutionContext.getActionName().contains(SERVICE_CHECK_COMMAND_NAME)) {
        findHostAndAddServiceCheckAction(actionExecutionContext,
          resourceFilter, stage, hostLevelParams);
      } else if (actionExecutionContext.getActionName().equals(DECOMMISSION_COMMAND_NAME)) {
        addDecommissionAction(actionExecutionContext, resourceFilter, stage, hostLevelParams);
      } else if (isValidCustomCommand(actionExecutionContext, resourceFilter)) {
        String commandDetail = getReadableCustomCommandDetail(actionExecutionContext, resourceFilter);
        addCustomCommandAction(actionExecutionContext, resourceFilter, stage,
          hostLevelParams, null, commandDetail);
      } else {
        throw new AmbariException("Unsupported action " +
          actionExecutionContext.getActionName());
      }
    }
  }

  /**
   * Get repository info given a cluster and host.
   * @param cluster
   * @param host
   * @return
   * @throws AmbariException
   */
  public String getRepoInfo(Cluster cluster, Host host) throws AmbariException {
    StackId stackId = cluster.getDesiredStackVersion();

    Map<String, List<RepositoryInfo>> repos = ambariMetaInfo.getRepository(
        stackId.getStackName(), stackId.getStackVersion());
    String repoInfo = "";
    if (!repos.containsKey(host.getOsType())) {
      // FIXME should this be an error?
      LOG.warn("Could not retrieve repo information for host"
          + ", hostname=" + host.getHostName()
          + ", clusterName=" + cluster.getClusterName()
          + ", stackInfo=" + stackId.getStackId());
    } else {
      repoInfo = gson.toJson(repos.get(host.getOsType()));
    }

    return repoInfo;
  }
}
