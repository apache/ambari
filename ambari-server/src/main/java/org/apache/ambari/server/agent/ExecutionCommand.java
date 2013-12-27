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
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonProperty;


/**
 * Execution commands are scheduled by action manager, and these are
 * persisted in the database for recovery.
 */
public class ExecutionCommand extends AgentCommand {
  
  private static Log LOG = LogFactory.getLog(ExecutionCommand.class);
  
  public ExecutionCommand() {
    super(AgentCommandType.EXECUTION_COMMAND);
  }


  private String clusterName;
  private long taskId;
  private String commandId;
  private String hostname;
  private String role;
  private Map<String, String> hostLevelParams = new HashMap<String, String>();
  private Map<String, String> roleParams = null;
  private RoleCommand roleCommand;
  private Map<String, Set<String>> clusterHostInfo = 
      new HashMap<String, Set<String>>();
  private Map<String, Map<String, String>> configurations;
  private Map<String, Map<String, String>> configurationTags;
  private Map<String, String> commandParams;
  private String serviceName;
  private String componentName;

  @JsonProperty("commandId")
  public String getCommandId() {
    return this.commandId;
  }
  
  @JsonProperty("commandId")
  public void setCommandId(String commandId) {
    this.commandId = commandId;
  }
  
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ExecutionCommand)) {
      return false;
    }
    ExecutionCommand o = (ExecutionCommand) other;
    return (this.commandId.equals(o.commandId) &&
            this.hostname.equals(o.hostname) &&
            this.role.equals(o.role) &&
            this.roleCommand.equals(o.roleCommand));
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

  @JsonProperty("taskId")
  public long getTaskId() {
    return taskId;
  }

  @JsonProperty("taskId")
  public void setTaskId(long taskId) {
    this.taskId = taskId;
  }

  @JsonProperty("role")
  public String getRole() {
    return role;
  }

  @JsonProperty("role")
  public void setRole(String role) {
    this.role = role;
  }

  @JsonProperty("roleParams")
  public Map<String, String> getRoleParams() {
    return roleParams;
  }

  @JsonProperty("roleParams")
  public void setRoleParams(Map<String, String> roleParams) {
    this.roleParams = roleParams;
  }

  @JsonProperty("roleCommand")
  public RoleCommand getRoleCommand() {
    return roleCommand;
  }

  @JsonProperty("roleCommand")
  public void setRoleCommand(RoleCommand cmd) {
    this.roleCommand = cmd;
  }
  
  @JsonProperty("clusterName")
  public String getClusterName() {
    return clusterName;
  }
  
  @JsonProperty("clusterName")
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @JsonProperty("hostname")
  public String getHostname() {
    return hostname;
  }

  @JsonProperty("hostname")
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  @JsonProperty("hostLevelParams")
  public Map<String, String> getHostLevelParams() {
    return hostLevelParams;
  }

  @JsonProperty("hostLevelParams")
  public void setHostLevelParams(Map<String, String> params) {
    this.hostLevelParams = params;
  }

  @JsonProperty("clusterHostInfo")
  public Map<String, Set<String>> getClusterHostInfo() {
    return clusterHostInfo;
  }

  @JsonProperty("clusterHostInfo")
  public void setClusterHostInfo(Map<String, Set<String>> clusterHostInfo) {
    this.clusterHostInfo = clusterHostInfo;
  }
  
  @JsonProperty("configurations")
  public Map<String, Map<String, String>> getConfigurations() {
    return configurations;
  }

  @JsonProperty("configurations")
  public void setConfigurations(Map<String, Map<String, String>> configurations) {
    this.configurations = configurations;
  }

  @JsonProperty("commandParams")
  public Map<String, String> getCommandParams() {
    return commandParams;
  }

  @JsonProperty("commandParams")
  public void setCommandParams(Map<String, String> commandParams) {
    this.commandParams = commandParams;
  }

  @JsonProperty("serviceName")
  public String getServiceName() {
    return serviceName;
  }

  @JsonProperty("serviceName")
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
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
   * Contains key name strings. These strings are used inside maps
   * incapsulated inside command.
   */
  public static interface KeyNames {

    String SCHEMA_VERSION = "schema_version";
    String COMMAND_TIMEOUT = "command_timeout";
    String SCRIPT = "script";
    String SCRIPT_TYPE = "script_type";
    String SERVICE_METADATA_FOLDER = "service_metadata_folder";
    String STACK_NAME = "stack_name";
    String STACK_VERSION = "stack_version";
    String SERVICE_REPO_INFO = "service_repo_info";
    String PACKAGE_LIST = "package_list";
    String JDK_LOCATION = "jdk_location";
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
    String SERVICE_CHECK = "SERVICE_CHECK"; // TODO: is it standart command? maybe add it to RoleCommand enum?

    String COMMAND_TIMEOUT_DEFAULT = "600"; // TODO: Will be replaced by proper initialization in another jira
    String CUSTOM_COMMAND = "custom_command";

  }

}
