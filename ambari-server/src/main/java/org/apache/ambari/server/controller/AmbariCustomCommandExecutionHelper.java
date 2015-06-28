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

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CLIENTS_TO_UPDATE_CONFIGS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMPONENT_CATEGORY;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CUSTOM_COMMAND;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GROUP_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JAVA_HOME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JAVA_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JCE_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.MYSQL_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.ORACLE_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOST_SYS_PREPPED;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_LIST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
  public static final Map<String, String> masterToSlaveMappingForDecom = new HashMap<String, String>();

  static {
    masterToSlaveMappingForDecom.put("NAMENODE", "DATANODE");
    masterToSlaveMappingForDecom.put("RESOURCEMANAGER", "NODEMANAGER");
    masterToSlaveMappingForDecom.put("HBASE_MASTER", "HBASE_REGIONSERVER");
    masterToSlaveMappingForDecom.put("JOBTRACKER", "TASKTRACKER");
  }

  public final static String DECOM_INCLUDED_HOSTS = "included_hosts";
  public final static String DECOM_EXCLUDED_HOSTS = "excluded_hosts";
  public final static String DECOM_SLAVE_COMPONENT = "slave_type";
  public final static String HBASE_MARK_DRAINING_ONLY = "mark_draining_only";
  public final static String UPDATE_EXCLUDE_FILE_ONLY = "update_exclude_file_only";

  private final static String ALIGN_MAINTENANCE_STATE = "align_maintenance_state";

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
  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;
  @Inject
  private OsFamily os_family;
  @Inject
  private ClusterVersionDAO clusterVersionDAO;

  protected static final String SERVICE_CHECK_COMMAND_NAME = "SERVICE_CHECK";
  protected static final String START_COMMAND_NAME = "START";
  protected static final String RESTART_COMMAND_NAME = "RESTART";
  protected static final String INSTALL_COMMAND_NAME = "INSTALL";
  public static final String DECOMMISSION_COMMAND_NAME = "DECOMMISSION";


  private Boolean isServiceCheckCommand(String command, String service) {
    List<String> actions = actionMetadata.getActions(service);

    return !(actions == null || actions.size() == 0) && actions.contains(command);
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

  private void addCustomCommandAction(final ActionExecutionContext actionExecutionContext,
                                      final RequestResourceFilter resourceFilter,
                                      Stage stage,
                                      Map<String, String> additionalCommandParams,
                                      String commandDetail,
                                      boolean retryAllowed)
                                      throws AmbariException {
    final String serviceName = resourceFilter.getServiceName();
    final String componentName = resourceFilter.getComponentName();
    final String commandName = actionExecutionContext.getActionName();

    String clusterName = stage.getClusterName();
    final Cluster cluster = clusters.getCluster(clusterName);

    Set<String> candidateHosts = new HashSet<String>(resourceFilter.getHostNames());
    // Filter hosts that are in MS
    Set<String> ignoredHosts = maintenanceStateHelper.filterHostsInMaintenanceState(
        candidateHosts, new MaintenanceStateHelper.HostPredicate() {
          @Override
          public boolean shouldHostBeRemoved(final String hostname)
              throws AmbariException {
            return !maintenanceStateHelper.isOperationAllowed(
                cluster, actionExecutionContext.getOperationLevel(),
                resourceFilter, serviceName, componentName, hostname);
          }
        }
    );

    // Filter unhealthy hosts
    Set<String> filteredHosts = filterUnhealthHosts(candidateHosts, actionExecutionContext, resourceFilter);

    if (!ignoredHosts.isEmpty()) {
      String message = String.format("Some hosts (%s) have been ignored " +
                      "because components on them are in Maintenance state.",
              ignoredHosts);
      LOG.debug(message);
    } else if (!filteredHosts.isEmpty()) {
      String message = String.format("Some hosts (%s) have been ignored " +
            "because they are in unknown state",
         filteredHosts);
      LOG.warn(message);
    } else if (candidateHosts.isEmpty()) {
      String message = "Invalid request : No hosts specified.";
      throw new AmbariException(message);
    }

    StackId stackId = cluster.getDesiredStackVersion();
    AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();
    ServiceInfo serviceInfo = ambariMetaInfo.getService(
        stackId.getStackName(), stackId.getStackVersion(), serviceName);
    StackInfo stackInfo = ambariMetaInfo.getStack
       (stackId.getStackName(), stackId.getStackVersion());

    CustomCommandDefinition customCommandDefinition = null;
    ComponentInfo ci = serviceInfo.getComponentByName(componentName);
    if(ci != null){
      customCommandDefinition = ci.getCustomCommandByName(commandName);
    }

    long nowTimestamp = System.currentTimeMillis();

    for (String hostName : candidateHosts) {

      Host host = clusters.getHost(hostName);

      stage.addHostRoleExecutionCommand(hostName, Role.valueOf(componentName),
          RoleCommand.CUSTOM_COMMAND,
          new ServiceComponentHostOpInProgressEvent(componentName,
              hostName, nowTimestamp), cluster.getClusterName(), serviceName, retryAllowed);

      Map<String, Map<String, String>> configurations =
          new TreeMap<String, Map<String, String>>();
      Map<String, Map<String, Map<String, String>>> configurationAttributes =
          new TreeMap<String, Map<String, Map<String, String>>>();
      Map<String, Map<String, String>> configTags =
          managementController.findConfigurationTagsWithOverrides(cluster, hostName);

      HostRoleCommand cmd = stage.getHostRoleCommand(hostName, componentName);
      if (cmd != null) {
        cmd.setCommandDetail(commandDetail);
        cmd.setCustomCommandName(commandName);
      }

      ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
          componentName).getExecutionCommand();

      //set type background
      if(customCommandDefinition != null && customCommandDefinition.isBackground()){
        execCmd.setCommandType(AgentCommandType.BACKGROUND_EXECUTION_COMMAND);
      }

      execCmd.setConfigurations(configurations);
      execCmd.setConfigurationAttributes(configurationAttributes);
      execCmd.setConfigurationTags(configTags);

      if(actionExecutionContext.getParameters() != null && actionExecutionContext.getParameters().containsKey(KeyNames.REFRESH_ADITIONAL_COMPONENT_TAGS)){
        execCmd.setForceRefreshConfigTags(parseAndValidateComponentsMapping(actionExecutionContext.getParameters().get(KeyNames.REFRESH_ADITIONAL_COMPONENT_TAGS)));
      }

      if(actionExecutionContext.getParameters() != null && actionExecutionContext.getParameters().containsKey(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION)){
        execCmd.setForceRefreshConfigTagsBeforeExecution(parseAndValidateComponentsMapping(actionExecutionContext.getParameters().get(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION)));
      }

      Map<String, String> hostLevelParams = new TreeMap<String, String>();

      hostLevelParams.put(CUSTOM_COMMAND, commandName);
      // Set parameters required for re-installing clients on restart
      hostLevelParams.put(REPO_INFO, getRepoInfo
        (cluster, host));

      Set<String> userSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.USER, cluster);
      String userList = gson.toJson(userSet);
      hostLevelParams.put(USER_LIST, userList);

      Set<String> groupSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.GROUP, cluster);
      String groupList = gson.toJson(groupSet);
      hostLevelParams.put(GROUP_LIST, groupList);

      execCmd.setHostLevelParams(hostLevelParams);

      Map<String, String> commandParams = new TreeMap<String, String>();
      if (additionalCommandParams != null) {
        for (String key : additionalCommandParams.keySet()) {
          commandParams.put(key, additionalCommandParams.get(key));
        }
      }

      boolean isInstallCommand = commandName.equals(RoleCommand.INSTALL.toString());
      String commandTimeout = configs.getDefaultAgentTaskTimeout(isInstallCommand);

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

      ClusterVersionEntity currentClusterVersion = cluster.getCurrentClusterVersion();
      if (currentClusterVersion != null) {
       commandParams.put(KeyNames.VERSION, currentClusterVersion.getRepositoryVersion().getVersion());
      }

      execCmd.setCommandParams(commandParams);

      Map<String, String> roleParams = execCmd.getRoleParams();
      if (roleParams == null) {
        roleParams = new TreeMap<String, String>();
      }

      roleParams.put(COMPONENT_CATEGORY, componentInfo.getCategory());
      execCmd.setRoleParams(roleParams);
    }
  }

  /**
   * Splits the passed comma separated value and returns it as set.
   *
   * @param commaSeparatedTags  separated list
   *
   * @return set of items or null
   */
  private Set<String> parseAndValidateComponentsMapping(String commaSeparatedTags) {
    Set<String> retVal = null;
    if(commaSeparatedTags != null && !commaSeparatedTags.trim().isEmpty()){
      Collections.addAll(retVal = new HashSet<String>(), commaSeparatedTags.split(","));
    }
    return retVal;
  }

  private void findHostAndAddServiceCheckAction(
      final ActionExecutionContext actionExecutionContext,
      final RequestResourceFilter resourceFilter,
      Stage stage,
      boolean retryAllowed)
          throws AmbariException {

    String clusterName = actionExecutionContext.getClusterName();
    final Cluster cluster = clusters.getCluster(clusterName);
    final String componentName = actionMetadata.getClient(resourceFilter.getServiceName());
    final String serviceName = resourceFilter.getServiceName();
    String smokeTestRole = actionMetadata.getServiceCheckAction(serviceName);
    if (null == smokeTestRole) {
      smokeTestRole = actionExecutionContext.getActionName();
    }
    long nowTimestamp = System.currentTimeMillis();
    Map<String, String> actionParameters = actionExecutionContext.getParameters();
    final Set<String> candidateHosts;
    if (componentName != null) {
      Map<String, ServiceComponentHost> components =
        cluster.getService(serviceName)
          .getServiceComponent(componentName).getServiceComponentHosts();
      if (components.isEmpty()) {
        throw new AmbariException("Hosts not found, component="
            + componentName + ", service = " + serviceName
            + ", cluster = " + clusterName);
      }
      List<String> candidateHostsList = resourceFilter.getHostNames();
      if (candidateHostsList != null && !candidateHostsList.isEmpty()) {
        candidateHosts = new HashSet<String>(candidateHostsList);
      } else {
        candidateHosts = components.keySet();
      }
    } else { // TODO: this code branch looks unreliable(taking random component)
      Map<String, ServiceComponent> components =
              cluster.getService(serviceName).getServiceComponents();
      if (components.isEmpty()) {
        throw new AmbariException("Components not found, service = "
            + serviceName + ", cluster = " + clusterName);
      }
      ServiceComponent serviceComponent = components.values().iterator().next();
      if (serviceComponent.getServiceComponentHosts().isEmpty()) {
        throw new AmbariException("Hosts not found, component="
            + serviceComponent.getName() + ", service = "
            + serviceName + ", cluster = " + clusterName);
      }
      candidateHosts = serviceComponent.getServiceComponentHosts().keySet();
    }

    // Filter hosts that are in MS
    Set<String> ignoredHosts = new HashSet<String>();

    if (!actionExecutionContext.isIgnoreMaintenance()) {
      ignoredHosts.addAll(maintenanceStateHelper.filterHostsInMaintenanceState(
        candidateHosts, new MaintenanceStateHelper.HostPredicate() {
          @Override
          public boolean shouldHostBeRemoved(final String hostname)
                  throws AmbariException {
            return !maintenanceStateHelper.isOperationAllowed(
                    cluster, actionExecutionContext.getOperationLevel(),
                    resourceFilter, serviceName, componentName, hostname);
          }
        }
      ));
    }

    String hostName = managementController.getHealthyHost(candidateHosts);
    if (hostName == null) {
      String msg = String.format("Unable to find a healthy host " +
              "amongst the provided set of " +
              "hosts: %s. You may also see this message if " +
              "all healthy hosts are not appropriate for service check " +
              "due to maintenance state (these hosts are %s). ",
              candidateHosts, ignoredHosts);
      LOG.info(msg);
    }

    addServiceCheckAction(stage, hostName, smokeTestRole, nowTimestamp,
        serviceName, componentName, actionParameters, retryAllowed);
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
                                    boolean retryAllowed)
                                    throws AmbariException {

    String clusterName = stage.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = cluster.getDesiredStackVersion();
    AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();
    ServiceInfo serviceInfo =
        ambariMetaInfo.getService(stackId.getStackName(),
            stackId.getStackVersion(), serviceName);
    StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(),
        stackId.getStackVersion());


    stage.addHostRoleExecutionCommand(hostname,
        Role.valueOf(smokeTestRole),
        RoleCommand.SERVICE_CHECK,
        new ServiceComponentHostOpInProgressEvent(componentName, hostname,
            nowTimestamp), cluster.getClusterName(), serviceName, retryAllowed);

    HostRoleCommand hrc = stage.getHostRoleCommand(hostname, smokeTestRole);
    if (hrc != null) {
      hrc.setCommandDetail(String.format("%s %s", RoleCommand.SERVICE_CHECK.toString(), serviceName));
    }
    // [ type -> [ key, value ] ]
    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();
    Map<String, Map<String, Map<String, String>>> configurationAttributes =
        new TreeMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> configTags =
        managementController.findConfigurationTagsWithOverrides(cluster, hostname);

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostname,
        smokeTestRole).getExecutionCommand();

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationAttributes(configurationAttributes);
    execCmd.setConfigurationTags(configTags);
    if(actionParameters != null && actionParameters.containsKey(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION)){
      execCmd.setForceRefreshConfigTagsBeforeExecution(parseAndValidateComponentsMapping(actionParameters.get(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION)));
    }

    // Generate cluster host info
    execCmd.setClusterHostInfo(
        StageUtils.getClusterHostInfo(cluster));

    Map<String, String> commandParams = new TreeMap<String, String>();

    String commandTimeout = configs.getDefaultAgentTaskTimeout(false);


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
  private void addDecommissionAction(final ActionExecutionContext actionExecutionContext,
                                     final RequestResourceFilter resourceFilter,
                                     Stage stage,
                                     boolean retryAllowed)
                                     throws AmbariException {

    String clusterName = actionExecutionContext.getClusterName();
    final Cluster cluster = clusters.getCluster(clusterName);
    final String serviceName = resourceFilter.getServiceName();
    String masterCompType = resourceFilter.getComponentName();
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
    String slaveCompStr = actionExecutionContext.getParameters().get(DECOM_SLAVE_COMPONENT);
    final String slaveCompType;
    if (slaveCompStr == null || slaveCompStr.equals("")) {
      slaveCompType = masterToSlaveMappingForDecom.get(masterCompType);
    } else {
      slaveCompType = slaveCompStr;
      if (!masterToSlaveMappingForDecom.get(masterCompType).equals(slaveCompType)) {
        throw new AmbariException("Component " + slaveCompType + " is not supported for decommissioning.");
      }
    }

    String isDrainOnlyRequest = actionExecutionContext.getParameters().get(HBASE_MARK_DRAINING_ONLY);
    if (isDrainOnlyRequest != null && !slaveCompType.equals(Role.HBASE_REGIONSERVER.name())) {
      throw new AmbariException(HBASE_MARK_DRAINING_ONLY + " is not a valid parameter for " + masterCompType);
    }

    // Filtering hosts based on Maintenance State
    MaintenanceStateHelper.HostPredicate hostPredicate
            = new MaintenanceStateHelper.HostPredicate() {
              @Override
              public boolean shouldHostBeRemoved(final String hostname)
              throws AmbariException {
                //Get UPDATE_EXCLUDE_FILE_ONLY parameter as string
                String upd_excl_file_only_str = actionExecutionContext.getParameters()
                .get(UPDATE_EXCLUDE_FILE_ONLY);

                String decom_incl_hosts_str = actionExecutionContext.getParameters()
                .get(DECOM_INCLUDED_HOSTS);
                if ((upd_excl_file_only_str != null &&
                        !upd_excl_file_only_str.trim().equals(""))){
                  upd_excl_file_only_str = upd_excl_file_only_str.trim();
                }

                boolean upd_excl_file_only = false;
                //Parse of possible forms of value
                if (upd_excl_file_only_str != null &&
                        !upd_excl_file_only_str.equals("") &&
                        (upd_excl_file_only_str.equals("\"true\"")
                        || upd_excl_file_only_str.equals("'true'")
                        || upd_excl_file_only_str.equals("true"))){
                  upd_excl_file_only = true;
                }

                // If we just clear *.exclude and component have been already removed we will skip check
                if (upd_excl_file_only && decom_incl_hosts_str != null
                        && !decom_incl_hosts_str.trim().equals("")) {
                  return upd_excl_file_only;
                } else {
                  return !maintenanceStateHelper.isOperationAllowed(
                          cluster, actionExecutionContext.getOperationLevel(),
                          resourceFilter, serviceName, slaveCompType, hostname);
                }
              }
            };
    // Filter excluded hosts
    Set<String> filteredExcludedHosts = new HashSet<String>(excludedHosts);
    Set<String> ignoredHosts = maintenanceStateHelper.filterHostsInMaintenanceState(
            filteredExcludedHosts, hostPredicate);
    if (! ignoredHosts.isEmpty()) {
      String message = String.format("Some hosts (%s) from host exclude list " +
                      "have been ignored " +
                      "because components on them are in Maintenance state.",
              ignoredHosts);
      LOG.debug(message);
    }

    // Filter included hosts
    Set<String> filteredIncludedHosts = new HashSet<String>(includedHosts);
    ignoredHosts = maintenanceStateHelper.filterHostsInMaintenanceState(
            filteredIncludedHosts, hostPredicate);
    if (! ignoredHosts.isEmpty()) {
      String message = String.format("Some hosts (%s) from host include list " +
                      "have been ignored " +
                      "because components on them are in Maintenance state.",
              ignoredHosts);
      LOG.debug(message);
    }

    // Decommission only if the sch is in state STARTED or INSTALLED
    for (ServiceComponentHost sch : svcComponents.get(slaveCompType).getServiceComponentHosts().values()) {
      if (filteredExcludedHosts.contains(sch.getHostName())
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
      if (filteredExcludedHosts.contains(sch.getHostName())) {
        sch.setComponentAdminState(HostComponentAdminState.DECOMMISSIONED);
        listOfExcludedHosts.add(sch.getHostName());
        if (alignMtnState) {
          sch.setMaintenanceState(MaintenanceState.ON);
        }
        LOG.info("Decommissioning " + slaveCompType + " and marking Maintenance=ON on " + sch.getHostName());
      }
      if (filteredIncludedHosts.contains(sch.getHostName())) {
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
      (actionExecutionContext, filteredIncludedHosts, listOfExcludedHosts);

    for (String hostName : masterSchs.keySet()) {
      RequestResourceFilter commandFilter = new RequestResourceFilter(serviceName,
        masterComponent.getName(), Collections.singletonList(hostName));
      List<RequestResourceFilter> resourceFilters = new ArrayList<RequestResourceFilter>();
      resourceFilters.add(commandFilter);

      ActionExecutionContext commandContext = new ActionExecutionContext(
        clusterName, actionExecutionContext.getActionName(), resourceFilters
      );

      String clusterHostInfoJson = StageUtils.getGson().toJson(
          StageUtils.getClusterHostInfo(cluster));

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
          commandParams, commandDetail.toString(), retryAllowed);
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
   *
   * @param actionRequest  the action request
   *
   * @throws AmbariException if the action can not be validated
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
   *
   * @param actionExecutionContext  received request to execute a command
   * @param stage                   the initial stage for task creation
   * @param retryAllowed            indicates whether the the command allows retry
   *
   * @throws AmbariException if the commands can not be added
   */
  public void addExecutionCommandsToStage(ActionExecutionContext actionExecutionContext,
                                          Stage stage,
                                          Map<String, String> requestParams,
                                          boolean retryAllowed)
                                          throws AmbariException {

    List<RequestResourceFilter> resourceFilters = actionExecutionContext.getResourceFilters();

    for (RequestResourceFilter resourceFilter : resourceFilters) {
      LOG.debug("Received a command execution request"
        + ", clusterName=" + actionExecutionContext.getClusterName()
        + ", serviceName=" + resourceFilter.getServiceName()
        + ", request=" + actionExecutionContext.toString());

      String actionName = actionExecutionContext.getActionName();

      if (actionName.contains(SERVICE_CHECK_COMMAND_NAME)) {
        findHostAndAddServiceCheckAction(actionExecutionContext,
            resourceFilter, stage, retryAllowed);
      } else if (actionName.equals(DECOMMISSION_COMMAND_NAME)) {
        addDecommissionAction(actionExecutionContext, resourceFilter, stage, retryAllowed);
      } else if (isValidCustomCommand(actionExecutionContext, resourceFilter)) {

        String commandDetail = getReadableCustomCommandDetail(actionExecutionContext, resourceFilter);

        Map<String, String> extraParams = new HashMap<String, String>();;
        String componentName = (null == resourceFilter.getComponentName()) ? null :
            resourceFilter.getComponentName().toLowerCase();

        if (null != componentName && requestParams.containsKey(componentName)) {
          extraParams.put(componentName, requestParams.get(componentName));
        }

        if(requestParams.containsKey(KeyNames.REFRESH_ADITIONAL_COMPONENT_TAGS)){
          actionExecutionContext.getParameters().put(KeyNames.REFRESH_ADITIONAL_COMPONENT_TAGS, requestParams.get(KeyNames.REFRESH_ADITIONAL_COMPONENT_TAGS));
        }

        if(requestParams.containsKey(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION)){
          actionExecutionContext.getParameters().put(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION, requestParams.get(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION));
        }

        RequestOperationLevel operationLevel = actionExecutionContext.getOperationLevel();
        if (operationLevel != null) {
          String clusterName = operationLevel.getClusterName();
          String serviceName = operationLevel.getServiceName();

          if (isTopologyRefreshRequired(actionName, clusterName, serviceName)) {
            extraParams.put(KeyNames.REFRESH_TOPOLOGY, "True");
          }
        }

        addCustomCommandAction(actionExecutionContext, resourceFilter, stage,
          extraParams, commandDetail, retryAllowed);
      } else {
        throw new AmbariException("Unsupported action " + actionName);
      }
    }
  }

  /**
   * Get repository info given a cluster and host.
   *
   * @param cluster  the cluster
   * @param host     the host
   *
   * @return the repo info
   *
   * @throws AmbariException if the repository information can not be obtained
   */
  public String getRepoInfo(Cluster cluster, Host host) throws AmbariException {

    StackId stackId = cluster.getDesiredStackVersion();

    Map<String, List<RepositoryInfo>> repos = ambariMetaInfo.getRepository(
        stackId.getStackName(), stackId.getStackVersion());

    String family = os_family.find(host.getOsType());
    if (null == family) {
      family = host.getOsFamily();
    }

    JsonElement gsonList = null;

    // !!! check for the most specific first
    if (repos.containsKey(host.getOsType())) {
      gsonList = gson.toJsonTree(repos.get(host.getOsType()));
    } else if (null != family && repos.containsKey(family)) {
      gsonList = gson.toJsonTree(repos.get(family));
    } else {
      LOG.warn("Could not retrieve repo information for host"
          + ", hostname=" + host.getHostName()
          + ", clusterName=" + cluster.getClusterName()
          + ", stackInfo=" + stackId.getStackId());
    }

    if (null != gsonList) {
      updateBaseUrls(cluster, JsonArray.class.cast(gsonList));
      return gsonList.toString();
    } else {
      return "";
    }
  }

  /**
   * Checks repo URLs against the current version for the cluster and makes
   * adjustments to the Base URL when the current is different.
   * @param cluster   the cluster to load the current version
   * @param jsonArray the array containing stack repo data
   */
  private void updateBaseUrls(Cluster cluster, JsonArray jsonArray) {
    ClusterVersionEntity cve = cluster.getCurrentClusterVersion();
    if (null == cve || null == cve.getRepositoryVersion()) {
      return;
    }

    RepositoryVersionEntity rve = cve.getRepositoryVersion();

    for (JsonElement e : jsonArray) {
      JsonObject obj = e.getAsJsonObject();

      String repoId = obj.has("repoId") ? obj.get("repoId").getAsString() : null;
      String repoName = obj.has("repoName") ? obj.get("repoName").getAsString() : null;
      String baseUrl = obj.has("baseUrl") ? obj.get("baseUrl").getAsString() : null;
      String osType = obj.has("osType") ? obj.get("osType").getAsString() : null;

      if (null == repoId || null == baseUrl || null == osType || null == repoName) {
        continue;
      }

      for (OperatingSystemEntity ose : rve.getOperatingSystems()) {
        if (ose.getOsType().equals(osType)) {
          for (RepositoryEntity re : ose.getRepositories()) {
            if (re.getName().equals(repoName) &&
                re.getRepositoryId().equals(repoId) &&
                !re.getBaseUrl().equals(baseUrl)) {
              obj.addProperty("baseUrl", re.getBaseUrl());
            }
          }
        }
      }
    }
  }


  /**
   * Helper method to fill execution command information.
   *
   * @param actionExecContext  the context
   * @param cluster            the cluster for the command
   *
   * @return a wrapper of the imporant JSON structures to add to a stage
   */
  public ExecuteCommandJson getCommandJson(ActionExecutionContext actionExecContext,
      Cluster cluster) throws AmbariException {

    Map<String, String> commandParamsStage = StageUtils.getCommandParamsStage(actionExecContext);
    Map<String, String> hostParamsStage = new HashMap<String, String>();
    Map<String, Set<String>> clusterHostInfo;
    String clusterHostInfoJson = "{}";

    if (null != cluster) {
      clusterHostInfo = StageUtils.getClusterHostInfo(
          cluster);
      hostParamsStage = createDefaultHostParams(cluster);
      StackId stackId = cluster.getDesiredStackVersion();
      String componentName = null;
      String serviceName = null;
      if (actionExecContext.getOperationLevel() != null) {
        componentName = actionExecContext.getOperationLevel().getHostComponentName();
        serviceName = actionExecContext.getOperationLevel().getServiceName();
      }
      if (serviceName != null && componentName != null) {
        ComponentInfo componentInfo = ambariMetaInfo.getComponent(
                stackId.getStackName(), stackId.getStackVersion(),
                serviceName, componentName);
        List<String> clientsToUpdateConfigsList = componentInfo.getClientsToUpdateConfigs();
        if (clientsToUpdateConfigsList == null) {
          clientsToUpdateConfigsList = new ArrayList<String>();
          clientsToUpdateConfigsList.add("*");
        }
        String clientsToUpdateConfigs = gson.toJson(clientsToUpdateConfigsList);
        hostParamsStage.put(CLIENTS_TO_UPDATE_CONFIGS, clientsToUpdateConfigs);
      }
      clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);
    }

    String hostParamsStageJson = StageUtils.getGson().toJson(hostParamsStage);
    String commandParamsStageJson = StageUtils.getGson().toJson(commandParamsStage);

    return new ExecuteCommandJson(clusterHostInfoJson, commandParamsStageJson,
        hostParamsStageJson);
  }

  Map<String, String> createDefaultHostParams(Cluster cluster) {
    StackId stackId = cluster.getDesiredStackVersion();
    TreeMap<String, String> hostLevelParams = new TreeMap<String, String>();
    hostLevelParams.put(JDK_LOCATION, managementController.getJdkResourceUrl());
    hostLevelParams.put(JAVA_HOME, managementController.getJavaHome());
    hostLevelParams.put(JAVA_VERSION, String.valueOf(configs.getJavaVersion()));
    hostLevelParams.put(JDK_NAME, managementController.getJDKName());
    hostLevelParams.put(JCE_NAME, managementController.getJCEName());
    hostLevelParams.put(STACK_NAME, stackId.getStackName());
    hostLevelParams.put(STACK_VERSION, stackId.getStackVersion());
    hostLevelParams.put(DB_NAME, managementController.getServerDB());
    hostLevelParams.put(MYSQL_JDBC_URL, managementController.getMysqljdbcUrl());
    hostLevelParams.put(ORACLE_JDBC_URL, managementController.getOjdbcUrl());
    hostLevelParams.put(DB_DRIVER_FILENAME, configs.getMySQLJarName());
    hostLevelParams.putAll(managementController.getRcaParameters());
    hostLevelParams.put(HOST_SYS_PREPPED, configs.areHostsSysPrepped());
    ClusterVersionEntity clusterVersionEntity = clusterVersionDAO.findByClusterAndStateCurrent(cluster.getClusterName());
    if (clusterVersionEntity == null) {
      List<ClusterVersionEntity> clusterVersionEntityList = clusterVersionDAO
              .findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.UPGRADING);
      if (!clusterVersionEntityList.isEmpty()) {
        clusterVersionEntity = clusterVersionEntityList.iterator().next();
      }
    }
    if (clusterVersionEntity != null) {
      hostLevelParams.put("current_version", clusterVersionEntity.getRepositoryVersion().getVersion());
    }

    return hostLevelParams;
  }

  /**
   * Determine whether or not the action should trigger a topology refresh.
   *
   * @param actionName   the action name (i.e. START, RESTART)
   * @param clusterName  the cluster name
   * @param serviceName  the service name
   *
   * @return true if a topology refresh is required for the action
   */
  public boolean isTopologyRefreshRequired(String actionName, String clusterName, String serviceName)
      throws AmbariException {
    if (actionName.equals(START_COMMAND_NAME) || actionName.equals(RESTART_COMMAND_NAME)) {
      Cluster cluster = clusters.getCluster(clusterName);
      StackId stackId = cluster.getDesiredStackVersion();

      AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();

      StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
      if (stack != null) {
        ServiceInfo serviceInfo = stack.getService(serviceName);

        if (serviceInfo != null) {
          // if there is a chance that this action was triggered by a change in rack info then we want to
          // force a topology refresh
          // TODO : we may be able to be smarter about this and only refresh when the rack info has definitely changed
          Boolean restartRequiredAfterRackChange = serviceInfo.isRestartRequiredAfterRackChange();
          if (restartRequiredAfterRackChange != null && restartRequiredAfterRackChange) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private ServiceComponent getServiceComponent ( ActionExecutionContext actionExecutionContext,
                                                RequestResourceFilter resourceFilter){
    try {
      Cluster cluster = clusters.getCluster(actionExecutionContext.getClusterName());
      Service service = cluster.getService(resourceFilter.getServiceName());

      return service.getServiceComponent(resourceFilter.getComponentName());
    } catch (Exception e) {
      LOG.debug(String.format( "Unknown error appears during getting service component: %s", e.getMessage()));
    }
    return null;
  }

  /**
   * Filter host according to status of host/host components
   * @param hostname Host name to check
   * @param actionExecutionContext Received request to execute a command
   * @param resourceFilter Resource filter
   * @return True if host need to be filtered, False if Not
   * @throws AmbariException
   */
  private boolean filterUnhealthHostItem(String hostname,
                                         ActionExecutionContext actionExecutionContext,
                                         RequestResourceFilter resourceFilter) throws AmbariException {

    RequestOperationLevel operationLevel = actionExecutionContext.getOperationLevel();
    ServiceComponent serviceComponent = getServiceComponent(actionExecutionContext, resourceFilter);
    if (serviceComponent != null && operationLevel != null
                                && operationLevel.getLevel() == Resource.Type.Service // compare operation is allowed only for Service operation level
                                && actionExecutionContext.getResourceFilters().size() > 1  // Check if operation was started in a chain
                                && !serviceComponent.isMasterComponent()
       ){

      return !(clusters.getHost(hostname).getState() == HostState.HEALTHY);
    } else if (serviceComponent != null && operationLevel != null
                                        && operationLevel.getLevel() == Resource.Type.Host  // compare operation is allowed only for host component operation level
                                        && actionExecutionContext.getResourceFilters().size() > 1  // Check if operation was started in a chain
                                        && serviceComponent.getServiceComponentHosts().containsKey(hostname)  // Check if host is assigned to host component
                                        && !serviceComponent.isMasterComponent()
       ){

      State hostState = serviceComponent.getServiceComponentHosts().get(hostname).getState();

      return hostState == State.UNKNOWN;
    }
    return false;
  }


  /**
   * Filter hosts according to status of host/host components
   * @param hosts Host name set to filter
   * @param actionExecutionContext Received request to execute a command
   * @param resourceFilter Resource filter
   * @return Set of excluded hosts
   * @throws AmbariException
   */
  private Set<String> filterUnhealthHosts(Set<String> hosts,
                                          ActionExecutionContext actionExecutionContext,
                                          RequestResourceFilter resourceFilter) throws AmbariException {
    Set<String> removedHosts = new HashSet<String>();
    for (String hostname : hosts) {
      if (filterUnhealthHostItem(hostname, actionExecutionContext, resourceFilter)){
        removedHosts.add(hostname);
      }
    }
    hosts.removeAll(removedHosts);
    return removedHosts;
  }
}
