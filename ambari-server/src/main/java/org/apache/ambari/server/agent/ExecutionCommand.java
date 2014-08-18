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
package org.apache.ambari.server.agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.annotations.SerializedName;


/**
 * Execution commands are scheduled by action manager, and these are
 * persisted in the database for recovery.
 */
public class ExecutionCommand extends AgentCommand {

  private static Log LOG = LogFactory.getLog(ExecutionCommand.class);

  public ExecutionCommand() {
    super(AgentCommandType.EXECUTION_COMMAND);
  }

  @SerializedName("clusterName")
  private String clusterName;

  @SerializedName("taskId")
  private long taskId;

  @SerializedName("commandId")
  private String commandId;

  @SerializedName("hostname")
  private String hostname;

  @SerializedName("role")
  private String role;

  @SerializedName("hostLevelParams")
  private Map<String, String> hostLevelParams = new HashMap<String, String>();

  @SerializedName("roleParams")
  private Map<String, String> roleParams = null;

  @SerializedName("roleCommand")
  private RoleCommand roleCommand;

  @SerializedName("clusterHostInfo")
  private Map<String, Set<String>> clusterHostInfo =
      new HashMap<String, Set<String>>();

  @SerializedName("configurations")
  private Map<String, Map<String, String>> configurations;

  @SerializedName("configuration_attributes")
  private Map<String, Map<String, Map<String, String>>> configurationAttributes;

  @SerializedName("configurationTags")
  private Map<String, Map<String, String>> configurationTags;

  @SerializedName("forceRefreshConfigTags")
  private Set<String> forceRefreshConfigTags = new HashSet<String>();

  @SerializedName("commandParams")
  private Map<String, String> commandParams;

  @SerializedName("serviceName")
  private String serviceName;

  @SerializedName("componentName")
  private String componentName;

  /**
   * Used for ignoring nagios alerts at agent
   */
  @SerializedName("passiveInfo")
  private Set<Map<String,String>> passiveInfo;

  public String getCommandId() {
    return commandId;
  }

  public void setCommandId(String commandId) {
    this.commandId = commandId;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ExecutionCommand)) {
      return false;
    }
    ExecutionCommand o = (ExecutionCommand) other;
    return (commandId.equals(o.commandId) &&
            hostname.equals(o.hostname) &&
            role.equals(o.role) &&
            roleCommand.equals(o.roleCommand));
  }

  @Override
  public String toString() {
    try {
      return StageUtils.jaxbToString(this);
    } catch (Exception ex) {
      LOG.warn("Exception in json conversion", ex);
      return "Exception in json conversion";
    }
  }

  @Override
  public int hashCode() {
    return (hostname + commandId + role).hashCode();
  }

  public long getTaskId() {
    return taskId;
  }

  public void setTaskId(long taskId) {
    this.taskId = taskId;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Map<String, String> getRoleParams() {
    return roleParams;
  }

  public void setRoleParams(Map<String, String> roleParams) {
    this.roleParams = roleParams;
  }

  public RoleCommand getRoleCommand() {
    return roleCommand;
  }

  public void setRoleCommand(RoleCommand cmd) {
    roleCommand = cmd;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public Map<String, String> getHostLevelParams() {
    return hostLevelParams;
  }

  public void setHostLevelParams(Map<String, String> params) {
    hostLevelParams = params;
  }

  public Map<String, Set<String>> getClusterHostInfo() {
    return clusterHostInfo;
  }

  public void setClusterHostInfo(Map<String, Set<String>> clusterHostInfo) {
    this.clusterHostInfo = clusterHostInfo;
  }

  public Map<String, Map<String, String>> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(Map<String, Map<String, String>> configurations) {
    this.configurations = configurations;
  }
  /**
   * @return Returns the set of config-types that have to be propagated to actual-config of component of given custom command, if command is successfully finished.
   */
  public Set<String> getForceRefreshConfigTags() {
    return forceRefreshConfigTags;
  }

  public void setForceRefreshConfigTags(Set<String> forceRefreshConfigTags) {
    this.forceRefreshConfigTags = forceRefreshConfigTags;
  }

  public Map<String, Map<String, Map<String, String>>> getConfigurationAttributes() {
    return configurationAttributes;
  }

  public void setConfigurationAttributes(Map<String, Map<String, Map<String, String>>> configurationAttributes) {
    this.configurationAttributes = configurationAttributes;
  }

  public Map<String, String> getCommandParams() {
    return commandParams;
  }

  public void setCommandParams(Map<String, String> commandParams) {
    this.commandParams = commandParams;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * @param configTags the config tag map
   */
  public void setConfigurationTags(Map<String, Map<String, String>> configTags) {
    configurationTags = configTags;
  }

  /**
   * @return the configuration tags
   */
  public Map<String, Map<String, String>> getConfigurationTags() {
    return configurationTags;
  }

  /**
   * @return the passive info for the cluster
   */
  public Set<Map<String, String>> getPassiveInfo() {
    return passiveInfo;
  }

  /**
   * @param info the passive info for the cluster
   */
  public void setPassiveInfo(Set<Map<String,String>> info) {
    passiveInfo = info;
  }


  /**
   * Contains key name strings. These strings are used inside maps
   * incapsulated inside command.
   */
  public static interface KeyNames {
    String COMMAND_TIMEOUT = "command_timeout";
    String SCRIPT = "script";
    String SCRIPT_TYPE = "script_type";
    String SERVICE_PACKAGE_FOLDER = "service_package_folder";
    String HOOKS_FOLDER = "hooks_folder";
    String STACK_NAME = "stack_name";
    String STACK_VERSION = "stack_version";
    String SERVICE_REPO_INFO = "service_repo_info";
    String PACKAGE_LIST = "package_list";
    String JDK_LOCATION = "jdk_location";
    String JAVA_HOME = "java_home";
    String JDK_NAME = "jdk_name";
    String JCE_NAME = "jce_name";
    String MYSQL_JDBC_URL = "mysql_jdbc_url";
    String ORACLE_JDBC_URL = "oracle_jdbc_url";
    String DB_DRIVER_FILENAME = "db_driver_filename";
    String REPO_INFO = "repo_info";
    String DB_NAME = "db_name";
    String GLOBAL = "global";
    String AMBARI_DB_RCA_URL = "ambari_db_rca_url";
    String AMBARI_DB_RCA_DRIVER = "ambari_db_rca_driver";
    String AMBARI_DB_RCA_USERNAME = "ambari_db_rca_username";
    String AMBARI_DB_RCA_PASSWORD = "ambari_db_rca_password";
    String COMPONENT_CATEGORY = "component_category";
    String REFRESH_ADITIONAL_COMPONENT_TAGS = "forceRefreshConfigTags";

    String SERVICE_CHECK = "SERVICE_CHECK"; // TODO: is it standart command? maybe add it to RoleCommand enum?
    String CUSTOM_COMMAND = "custom_command";
  }

}
