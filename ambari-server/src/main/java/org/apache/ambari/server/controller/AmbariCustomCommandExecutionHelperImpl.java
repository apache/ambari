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
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.*;


/**
 * Helper class containing logic to process custom command execution requests
 */
@Singleton
public class AmbariCustomCommandExecutionHelperImpl implements AmbariCustomCommandExecutionHelper {
  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariCustomCommandExecutionHelperImpl.class);

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


  @Override
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

  @Override
  public void addAction(ExecuteActionRequest actionRequest, Stage stage,
                        HostsMap hostsMap, Map<String, String> hostLevelParams)
      throws AmbariException {
    if (actionRequest.getCommandName().contains("SERVICE_CHECK")) {
      addServiceCheckAction(actionRequest, stage, hostsMap, hostLevelParams);
    } else if (actionRequest.getCommandName().equals("DECOMMISSION_DATANODE")) {
      addDecommissionDatanodeAction(actionRequest, stage, hostLevelParams);
    } else {
      throw new AmbariException("Unsupported action " + actionRequest.getCommandName());
    }
  }

  private void addServiceCheckAction(ExecuteActionRequest actionRequest, Stage stage,
                                     HostsMap hostsMap,
                                     Map<String, String> hostLevelParams)
      throws AmbariException {
    String clusterName = actionRequest.getClusterName();
    String componentName = actionMetadata.getClient(actionRequest
        .getServiceName());
    String serviceName = actionRequest.getServiceName();
    String smokeTestRole = actionRequest.getCommandName();
    long nowTimestamp = System.currentTimeMillis();
    Map<String, String> roleParameters = actionRequest.getParameters();

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


    addServiceCheckActionImpl(stage, hostName, smokeTestRole, nowTimestamp,
            serviceName, componentName, roleParameters, hostsMap,
            hostLevelParams);
  }



  /**
   * Creates and populates service check EXECUTION_COMMAND for host.
   * Not all EXECUTION_COMMAND parameters are populated here because they
   * are not needed by service check.
   */
  @Override
  public void addServiceCheckActionImpl(Stage stage,
                                        String hostname, String smokeTestRole,
                                        long nowTimestamp,
                                        String serviceName,
                                        String componentName,
                                        Map<String, String> roleParameters,
                                        HostsMap hostsMap,
                                        Map<String, String> hostLevelParams)
          throws AmbariException{

    String clusterName = stage.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = cluster.getDesiredStackVersion();
    AmbariMetaInfo ambariMetaInfo = amc.getAmbariMetaInfo();
    ServiceInfo serviceInfo =
            ambariMetaInfo.getServiceInfo(stackId.getStackName(),
              stackId.getStackVersion(), serviceName);


    stage.addHostRoleExecutionCommand(hostname,
            Role.valueOf(smokeTestRole),
            RoleCommand.SERVICE_CHECK,
            new ServiceComponentHostOpInProgressEvent(componentName, hostname,
                    nowTimestamp), cluster.getClusterName(), serviceName);

    // [ type -> [ key, value ] ]
    Map<String, Map<String, String>> configurations =
            new TreeMap<String, Map<String, String>>();
    Map<String, Map<String, String>> configTags =
            amc.findConfigurationTagsWithOverrides(cluster, hostname);

    ExecutionCommand execCmd =  stage.getExecutionCommandWrapper(hostname,
            smokeTestRole).getExecutionCommand();

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationTags(configTags);

    // Generate cluster host info
    execCmd.setClusterHostInfo(
            StageUtils.getClusterHostInfo(clusters.getHostsForCluster(clusterName), cluster, hostsMap, configs));

    if (hostLevelParams == null) {
      hostLevelParams = new TreeMap<String, String>();
    }
    hostLevelParams.put(JDK_LOCATION, amc.getJdkResourceUrl());
    hostLevelParams.put(STACK_NAME, stackId.getStackName());
    hostLevelParams.put(STACK_VERSION,stackId.getStackVersion());
    execCmd.setHostLevelParams(hostLevelParams);

    Map<String,String> commandParams = new TreeMap<String, String>();
    commandParams.put(SCHEMA_VERSION, serviceInfo.getSchemaVersion());

    String commandTimeout = COMMAND_TIMEOUT_DEFAULT;


    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      // Service check command is not custom command
      CommandScriptDefinition script = serviceInfo.getCommandScript();
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
        commandTimeout = String.valueOf(script.getTimeout());
      } else {
        String message = String.format("Service %s has not command script " +
                "defined. It is not possible to run service check" +
                " for this service", serviceName);
        throw new AmbariException(message);
      }
      // We don't need package/repo infomation to perform service check
    }
    commandParams.put(COMMAND_TIMEOUT, commandTimeout);

    commandParams.put(SERVICE_METADATA_FOLDER,
            serviceInfo.getServiceMetadataFolder());

    execCmd.setCommandParams(commandParams);

    if (roleParameters != null) { // If defined
      execCmd.setRoleParams(roleParameters);
    }

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


    Map<String, Map<String, String>> configTags = amc.findConfigurationTagsWithOverrides(cluster, namenodeHost);

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
    /*
     TODO: When migrating to custom services, datanode decommision
     probably will be implemented as a custom action; that's why
     we have no schema version 2 command parameters here
    */
    execCmd.setHostLevelParams(hostLevelParams);
  }


  /**
   * Creates and populates an EXECUTION_COMMAND for host
   */
  @Override
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
    String commandTimeout = ExecutionCommand.KeyNames.COMMAND_TIMEOUT_DEFAULT;
    CommandScriptDefinition script = componentInfo.getCommandScript();
    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
        commandTimeout = String.valueOf(script.getTimeout());
      } else {
        String message = String.format("Component %s of service %s has not " +
                "command script defined", componentName, serviceName);
        throw new AmbariException(message);
      }
    }
    commandParams.put(COMMAND_TIMEOUT, commandTimeout);
    commandParams.put(SERVICE_METADATA_FOLDER,
            serviceInfo.getServiceMetadataFolder());

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
    hostParams.put(STACK_NAME, stackId.getStackName());
    hostParams.put(STACK_VERSION, stackId.getStackVersion());
    hostParams.put(DB_NAME, amc.getServerDB());
    hostParams.put(MYSQL_JDBC_URL, amc.getMysqljdbcUrl());
    hostParams.put(ORACLE_JDBC_URL, amc.getOjdbcUrl());

    // Write down os specific info for the service
    ServiceOsSpecific anyOs = null;
    if (serviceInfo.getOsSpecifics().containsKey(AmbariMetaInfo.ANY_OS)) {
      anyOs = serviceInfo.getOsSpecifics().get(AmbariMetaInfo.ANY_OS);
    }
    ServiceOsSpecific hostOs = null;
    if (serviceInfo.getOsSpecifics().containsKey(osType)) {
      hostOs = serviceInfo.getOsSpecifics().get(osType);
      // Choose repo that is relevant for host
      ServiceOsSpecific.Repo serviceRepo= hostOs.getRepo();
      if (serviceRepo != null) {
        String serviceRepoInfo = gson.toJson(serviceInfo);
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
