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
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.PassiveState;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
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
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CUSTOM_COMMAND;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JAVA_HOME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JCE_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.MYSQL_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.ORACLE_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.PACKAGE_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCHEMA_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_VERSION;


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
  @Inject
  private ActionMetadata actionMetadata;
  @Inject
  private Clusters clusters;
  @Inject
  private AmbariManagementController amc;
  @Inject
  private Gson gson;
  @Inject
  private Configuration configs;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  @Inject
  private ConfigHelper configHelper;
  ;

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

  private Boolean isValidCustomCommand(ExecuteActionRequest actionRequest) throws AmbariException {
    String clustername = actionRequest.getClusterName();
    Cluster cluster = clusters.getCluster(clustername);
    StackId stackId = cluster.getDesiredStackVersion();
    String serviceName = actionRequest.getServiceName();
    String componentName = actionRequest.getComponentName();
    String commandName = actionRequest.getCommandName();

    if (componentName == null) {
      return false;
    }
    ComponentInfo componentInfo = ambariMetaInfo.getComponent(
        stackId.getStackName(), stackId.getStackVersion(),
        serviceName, componentName);

    if (!componentInfo.isCustomCommand(commandName) &&
        !actionMetadata.isDefaultHostComponentCommand(commandName)) {
      return false;
    }
    return true;
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

    if (!isServiceCheckCommand(actionRequest.getCommandName(), actionRequest.getServiceName())
        && !isValidCustomCommand(actionRequest)) {
      throw new AmbariException(
          "Unsupported action " + actionRequest.getCommandName() + " for Service: " + actionRequest.getServiceName()
              + " and Component: " + actionRequest.getComponentName());
    }
  }

  /**
   * Other than Service_Check and Decommission all other commands are pass-through
   *
   * @param actionRequest   received request to execute a command
   * @param stage           the initial stage for task creation
   * @param hostLevelParams specific parameters for the hosts
   * @throws AmbariException
   */
  public void addAction(ExecuteActionRequest actionRequest, Stage stage,
                        Map<String, String> hostLevelParams)
      throws AmbariException {
    if (actionRequest.getCommandName().contains("SERVICE_CHECK")) {
      findHostAndAddServiceCheckAction(actionRequest, stage, hostLevelParams);
    } else if (actionRequest.getCommandName().equals("DECOMMISSION")) {
      addDecommissionAction(actionRequest, stage, hostLevelParams);
    } else if (isValidCustomCommand(actionRequest)) {
      String commandDetail = getReadableCustomCommandDetail(actionRequest);
      addCustomCommandAction(actionRequest, stage, hostLevelParams, null, commandDetail);
    } else {
      throw new AmbariException("Unsupported action " + actionRequest.getCommandName());
    }
  }

  private String getReadableCustomCommandDetail(ExecuteActionRequest actionRequest) {
    StringBuffer sb = new StringBuffer();
    sb.append(actionRequest.getCommandName());
    if (actionRequest.getServiceName() != null && !actionRequest.getServiceName().equals("")) {
      sb.append(" for " + actionRequest.getServiceName());
    }
    if (actionRequest.getComponentName() != null && !actionRequest.getComponentName().equals("")) {
      sb.append("/" + actionRequest.getComponentName());
    }
    return sb.toString();
  }

  private void addCustomCommandAction(ExecuteActionRequest actionRequest,
                                      Stage stage, Map<String, String> hostLevelParams,
                                      Map<String, String> additionalCommandParams,
                                      String commandDetail)
      throws AmbariException {

    if (actionRequest.getHosts().isEmpty()) {
      throw new AmbariException("Invalid request : No hosts specified.");
    }

    String serviceName = actionRequest.getServiceName();
    String componentName = actionRequest.getComponentName();
    String commandName = actionRequest.getCommandName();

    String clusterName = stage.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = cluster.getDesiredStackVersion();
    AmbariMetaInfo ambariMetaInfo = amc.getAmbariMetaInfo();
    ServiceInfo serviceInfo =
        ambariMetaInfo.getServiceInfo(stackId.getStackName(),
            stackId.getStackVersion(), serviceName);
    StackInfo stackInfo = ambariMetaInfo.getStackInfo(stackId.getStackName(),
        stackId.getStackVersion());

    long nowTimestamp = System.currentTimeMillis();

    for (String hostName : actionRequest.getHosts()) {

      stage.addHostRoleExecutionCommand(hostName, Role.valueOf(componentName),
          RoleCommand.CUSTOM_COMMAND,
          new ServiceComponentHostOpInProgressEvent(componentName,
              hostName, nowTimestamp), cluster.getClusterName(), serviceName);

      Map<String, Map<String, String>> configurations =
          new TreeMap<String, Map<String, String>>();
      Map<String, Map<String, String>> configTags =
          amc.findConfigurationTagsWithOverrides(cluster, hostName);

      HostRoleCommand cmd = stage.getHostRoleCommand(hostName, componentName);
      if (cmd != null) {
        cmd.setCommandDetail(commandDetail);
        cmd.setCustomCommandName(actionRequest.getCommandName());
      }

      ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
          componentName).getExecutionCommand();

      execCmd.setConfigurations(configurations);
      execCmd.setConfigurationTags(configTags);

      execCmd.setClusterHostInfo(
          StageUtils.getClusterHostInfo(clusters.getHostsForCluster(clusterName), cluster));

      hostLevelParams.put(CUSTOM_COMMAND, commandName);
      execCmd.setHostLevelParams(hostLevelParams);

      Map<String, String> commandParams = new TreeMap<String, String>();
      commandParams.put(SCHEMA_VERSION, serviceInfo.getSchemaVersion());
      if (additionalCommandParams != null) {
        for (String key : additionalCommandParams.keySet()) {
          commandParams.put(key, additionalCommandParams.get(key));
        }
      }

      String commandTimeout = configs.getDefaultAgentTaskTimeout();

      if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
        // Service check command is not custom command
        ComponentInfo componentInfo = ambariMetaInfo.getComponent(
            stackId.getStackName(), stackId.getStackVersion(),
            serviceName, componentName);
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
    }
  }

  private void findHostAndAddServiceCheckAction(ExecuteActionRequest actionRequest, Stage stage,
                                                Map<String, String> hostLevelParams)
      throws AmbariException {
    String clusterName = actionRequest.getClusterName();
    String componentName = actionMetadata.getClient(actionRequest
        .getServiceName());
    String serviceName = actionRequest.getServiceName();
    String smokeTestRole = actionRequest.getCommandName();
    long nowTimestamp = System.currentTimeMillis();
    Map<String, String> actionParameters = actionRequest.getParameters();

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
      hostName = amc.getHealthyHost(components.keySet());
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
    AmbariMetaInfo ambariMetaInfo = amc.getAmbariMetaInfo();
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
        amc.findConfigurationTagsWithOverrides(cluster, hostname);

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
  private void addDecommissionAction(ExecuteActionRequest request, Stage stage,
                                     Map<String, String> hostLevelParams)
      throws AmbariException {

    String clusterName = request.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    String serviceName = request.getServiceName();

    if (request.getHosts() != null && request.getHosts().size() != 0) {
      throw new AmbariException("Decommission command cannot be issued with target host(s) specified.");
    }

    //Get all hosts to be added and removed
    Set<String> excludedHosts = getHostList(request.getParameters(), DECOM_EXCLUDED_HOSTS);
    Set<String> includedHosts = getHostList(request.getParameters(), DECOM_INCLUDED_HOSTS);
    String slaveCompType = request.getParameters().get(DECOM_SLAVE_COMPONENT);

    Set<String> cloneSet = new HashSet<String>(excludedHosts);
    cloneSet.retainAll(includedHosts);
    if (cloneSet.size() > 0) {
      throw new AmbariException("Same host cannot be specified for inclusion as well as exclusion. Hosts: "
          + cloneSet.toString());
    }

    Service service = cluster.getService(serviceName);
    if (service == null) {
      throw new AmbariException("Specified service " + serviceName + " is not a valid/deployed service.");
    }

    String masterCompType = request.getComponentName();
    Map<String, ServiceComponent> svcComponents = service.getServiceComponents();
    if (!svcComponents.containsKey(masterCompType)) {
      throw new AmbariException("Specified component " + masterCompType + " does not belong to service "
          + serviceName + ".");
    }

    ServiceComponent masterComponent = svcComponents.get(masterCompType);
    if (!masterComponent.isMasterComponent()) {
      throw new AmbariException("Specified component " + masterCompType + " is not a MASTER for service "
          + serviceName + ".");
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

    String isDrainOnlyRequest = request.getParameters().get(HBASE_MARK_DRAINING_ONLY);
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

    // Set/reset decommissioned flag on all components
    List<String> listOfExcludedHosts = new ArrayList<String>();
    for (ServiceComponentHost sch : svcComponents.get(slaveCompType).getServiceComponentHosts().values()) {
      if (excludedHosts.contains(sch.getHostName())) {
        sch.setComponentAdminState(HostComponentAdminState.DECOMMISSIONED);
        listOfExcludedHosts.add(sch.getHostName());
        sch.setPassiveState(PassiveState.PASSIVE);
        LOG.info("Decommissioning " + slaveCompType + " and marking it PASSIVE on " + sch.getHostName());
      }
      if (includedHosts.contains(sch.getHostName())) {
        sch.setComponentAdminState(HostComponentAdminState.INSERVICE);
        sch.setPassiveState(PassiveState.ACTIVE);
        LOG.info("Recommissioning " + slaveCompType + " and marking it ACTIVE on " + sch.getHostName());
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

    StringBuilder commandDetail = getReadableDecommissionCommandDetail(request, includedHosts, listOfExcludedHosts);

    for (String hostName : masterSchs.keySet()) {
      ExecuteActionRequest commandRequest = new ExecuteActionRequest(
          request.getClusterName(), request.getCommandName(), request.getActionName(), request.getServiceName(),
          masterComponent.getName(), Collections.singletonList(hostName), null);

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
        addCustomCommandAction(commandRequest, stage, hostLevelParams, commandParams, commandDetail.toString());
      }
    }
  }

  private StringBuilder getReadableDecommissionCommandDetail(ExecuteActionRequest request,
                                                             Set<String> includedHosts,
                                                             List<String> listOfExcludedHosts) {
    StringBuilder commandDetail = new StringBuilder();
    commandDetail.append(request.getCommandName());
    if (listOfExcludedHosts.size() > 0) {
      commandDetail.append(", Excluded: ").append(StringUtils.join(listOfExcludedHosts, ','));
    }
    if (includedHosts.size() > 0) {
      commandDetail.append(", Included: ").append(StringUtils.join(includedHosts, ','));
    }
    return commandDetail;
  }

  /**
   * Creates and populates an EXECUTION_COMMAND for host
   */
  public void createHostAction(Cluster cluster,
                               Stage stage, ServiceComponentHost scHost,
                               Map<String, Map<String, String>> configurations,
                               Map<String, Map<String, String>> configTags,
                               RoleCommand roleCommand,
                               Map<String, String> commandParams,
                               ServiceComponentHostEvent event)
      throws AmbariException {

    stage.addHostRoleExecutionCommand(scHost.getHostName(), Role.valueOf(scHost
        .getServiceComponentName()), roleCommand,
        event, scHost.getClusterName(),
        scHost.getServiceName());
    String serviceName = scHost.getServiceName();
    String componentName = event.getServiceComponentName();
    String hostname = scHost.getHostName();
    String osType = clusters.getHost(hostname).getOsType();
    StackId stackId = cluster.getDesiredStackVersion();
    ServiceInfo serviceInfo = ambariMetaInfo.getServiceInfo(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);
    ComponentInfo componentInfo = ambariMetaInfo.getComponent(
        stackId.getStackName(), stackId.getStackVersion(),
        serviceName, componentName);
    StackInfo stackInfo = ambariMetaInfo.getStackInfo(stackId.getStackName(),
        stackId.getStackVersion());

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(scHost.getHostName(),
        scHost.getServiceComponentName()).getExecutionCommand();

    Host host = clusters.getHost(scHost.getHostName());

    // Hack - Remove passwords from configs
    if (event.getServiceComponentName().equals(Role.HIVE_CLIENT.toString())) {
      configHelper.applyCustomConfig(configurations, Configuration.HIVE_CONFIG_TAG,
          Configuration.HIVE_METASTORE_PASSWORD_PROPERTY, "", true);
    }

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationTags(configTags);
    if (commandParams == null) { // if not defined
      commandParams = new TreeMap<String, String>();
    }
    commandParams.put(SCHEMA_VERSION, serviceInfo.getSchemaVersion());


    // Get command script info for custom command/custom action
    /*
     * TODO: Custom actions are not supported yet, that's why we just pass
     * component main commandScript to agent. This script is only used for
     * default commads like INSTALL/STOP/START/CONFIGURE
     */
    String commandTimeout = configs.getDefaultAgentTaskTimeout();
    CommandScriptDefinition script = componentInfo.getCommandScript();
    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
        if (script.getTimeout() > 0) {
          commandTimeout = String.valueOf(script.getTimeout());
        }
      } else {
        String message = String.format("Component %s of service %s has no " +
            "command script defined", componentName, serviceName);
        throw new AmbariException(message);
      }
    }
    commandParams.put(COMMAND_TIMEOUT, commandTimeout);
    commandParams.put(SERVICE_PACKAGE_FOLDER,
        serviceInfo.getServicePackageFolder());
    commandParams.put(HOOKS_FOLDER, stackInfo.getStackHooksFolder());

    execCmd.setCommandParams(commandParams);

    Map<String, List<RepositoryInfo>> repos = ambariMetaInfo.getRepository(
        stackId.getStackName(), stackId.getStackVersion());
    String repoInfo = "";
    if (!repos.containsKey(host.getOsType())) {
      // FIXME should this be an error?
      LOG.warn("Could not retrieve repo information for host"
          + ", hostname=" + scHost.getHostName()
          + ", clusterName=" + cluster.getClusterName()
          + ", stackInfo=" + stackId.getStackId());
    } else {
      repoInfo = gson.toJson(repos.get(host.getOsType()));
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Sending repo information to agent"
          + ", hostname=" + scHost.getHostName()
          + ", clusterName=" + cluster.getClusterName()
          + ", stackInfo=" + stackId.getStackId()
          + ", repoInfo=" + repoInfo);
    }

    Map<String, String> hostParams = new TreeMap<String, String>();
    // TODO: Move parameter population to org.apache.ambari.server.controller.AmbariManagementControllerImpl.createAction()
    hostParams.put(REPO_INFO, repoInfo);
    hostParams.put(JDK_LOCATION, amc.getJdkResourceUrl());
    hostParams.put(JAVA_HOME, amc.getJavaHome());
    hostParams.put(JDK_NAME, amc.getJDKName());
    hostParams.put(JCE_NAME, amc.getJCEName());
    hostParams.put(STACK_NAME, stackId.getStackName());
    hostParams.put(STACK_VERSION, stackId.getStackVersion());
    hostParams.put(DB_NAME, amc.getServerDB());
    hostParams.put(MYSQL_JDBC_URL, amc.getMysqljdbcUrl());
    hostParams.put(ORACLE_JDBC_URL, amc.getOjdbcUrl());
    hostParams.putAll(amc.getRcaParameters());

    // Write down os specific info for the service
    ServiceOsSpecific anyOs = null;
    if (serviceInfo.getOsSpecifics().containsKey(AmbariMetaInfo.ANY_OS)) {
      anyOs = serviceInfo.getOsSpecifics().get(AmbariMetaInfo.ANY_OS);
    }
    ServiceOsSpecific hostOs = null;
    if (serviceInfo.getOsSpecifics().containsKey(osType)) {
      hostOs = serviceInfo.getOsSpecifics().get(osType);
      // Choose repo that is relevant for host
      ServiceOsSpecific.Repo serviceRepo = hostOs.getRepo();
      if (serviceRepo != null) {
        String serviceRepoInfo = gson.toJson(serviceRepo);
        hostParams.put(SERVICE_REPO_INFO, serviceRepoInfo);
      }
    }
    // Build package list that is relevant for host
    List<ServiceOsSpecific.Package> packages =
        new ArrayList<ServiceOsSpecific.Package>();
    if (anyOs != null) {
      packages.addAll(anyOs.getPackages());
    }

    if (hostOs != null) {
      packages.addAll(hostOs.getPackages());
    }
    String packageList = gson.toJson(packages);
    hostParams.put(PACKAGE_LIST, packageList);

    if (configs.getServerDBName().equalsIgnoreCase(Configuration
        .ORACLE_DB_NAME)) {
      hostParams.put(DB_DRIVER_FILENAME, configs.getOjdbcJarName());
    } else if (configs.getServerDBName().equalsIgnoreCase(Configuration
        .MYSQL_DB_NAME)) {
      hostParams.put(DB_DRIVER_FILENAME, configs.getMySQLJarName());
    }
    execCmd.setHostLevelParams(hostParams);

    Map<String, String> roleParams = new TreeMap<String, String>();
    execCmd.setRoleParams(roleParams);
  }
}
