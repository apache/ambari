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
package org.apache.ambari.server.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.UpgradeContext.UpgradeSummary;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;


/**
 * Execution commands are scheduled by action manager, and these are
 * persisted in the database for recovery.
 */
public class ExecutionCommand extends AgentCommand {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutionCommand.class);

  public ExecutionCommand() {
    super(AgentCommandType.EXECUTION_COMMAND);
  }

  @SerializedName("clusterName")
  private String clusterName;

  @SerializedName("clusterSettings")
  private Map<String, String> clusterSettings;

  @SerializedName("stackSettings")
  private Map<String, String> stackSettings;

  @SerializedName("requestId")
  private long requestId;

  @SerializedName("stageId")
  private long stageId;

  @SerializedName("taskId")
  private long taskId;

  @SerializedName("commandId")
  private String commandId;

  @SerializedName("hostname")
  private String hostname;

  @SerializedName("role")
  private String role;

  @SerializedName("hostLevelParams")
  private Map<String, String> hostLevelParams = new HashMap<>();

  @SerializedName("roleParams")
  private Map<String, String> roleParams = null;

  @SerializedName("roleCommand")
  private RoleCommand roleCommand;

  @SerializedName("clusterHostInfo")
  private Map<String, Set<String>> clusterHostInfo = new HashMap<>();

  @SerializedName("configurations")
  private Map<String, Map<String, String>> configurations;

  @SerializedName("configuration_attributes")
  private Map<String, Map<String, Map<String, String>>> configurationAttributes;

  @SerializedName("configurationTags")
  private Map<String, Map<String, String>> configurationTags;

  @SerializedName("forceRefreshConfigTagsBeforeExecution")
  private boolean forceRefreshConfigTagsBeforeExecution = false;

  @SerializedName("commandParams")
  private Map<String, String> commandParams = new HashMap<>();

  @SerializedName("serviceGroupName")
  private String serviceGroupName;

  @SerializedName("serviceName")
  private String serviceName;

  @SerializedName("serviceType")
  private String serviceType;

  @SerializedName("componentName")
  private String componentName;

  @SerializedName("kerberosCommandParams")
  private List<Map<String, String>> kerberosCommandParams = new ArrayList<>();

  @SerializedName("localComponents")
  private Set<String> localComponents = new HashSet<>();

  /**
   * "true" or "false" indicating whether this
   * service is enabled for credential store use.
   */
  @SerializedName("credentialStoreEnabled")
  private String credentialStoreEnabled;

  /**
   * Map of config type to list of password properties
   *   <pre>
   *     {@code
   *       {
   *         "config_type1" :
   *           {
   *             "password_alias_name1:type1":"password_value_name1",
   *             "password_alias_name2:type2":"password_value_name2",
   *                 :
   *           },
   *         "config_type2" :
   *           {
   *             "password_alias_name1:type1":"password_value_name1",
   *             "password_alias_name2:type2":"password_value_name2",
   *                 :
   *           },
   *                 :
   *       }
   *     }
   *   </pre>
   */
  @SerializedName("configuration_credentials")
  private Map<String, Map<String, String>> configurationCredentials;


  /**
   * Provides information regarding the content of repositories.  This structure replaces
   * the deprecated use of {@link KeyNames#REPO_INFO}
   */
  @SerializedName("repositoryFile")
  private CommandRepository commandRepository;

  @SerializedName("componentVersionMap")
  private Map<String, Map<String, String>> componentVersionMap = new HashMap<>();

  @SerializedName("upgradeSummary")
  private UpgradeSummary upgradeSummary;

  @SerializedName("roleParameters")
  private Map<String, Object> roleParameters;

  /**
   * If set, the management pack associated with this command.
   */
  @SerializedName("mpackId")
  private Long mpackId;

  public void setConfigurationCredentials(Map<String, Map<String, String>> configurationCredentials) {
    this.configurationCredentials = configurationCredentials;
  }

  public Map<String, Map<String, String>> getConfigurationCredentials() {
    return configurationCredentials;
  }

  public String getCommandId() {
    return commandId;
  }

  /**
   * Sets the request and stage on this command. The {@code commandId} field is
   * automatically constructed from these as requestId-stageId.
   *
   * @param requestId
   *          the ID of the execution request.
   * @param stageId
   *          the ID of the stage request.
   */
  public void setRequestAndStage(long requestId, long stageId) {
    this.requestId = requestId;
    this.stageId = stageId;

    commandId = StageUtils.getActionId(requestId, stageId);
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

  /**
   * Sets the roleParams for the command.  Consider instead using {@link #setRoleParameters}
   * @param roleParams
   */
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

  public Map<String, String> getClusterSettings() {
    return clusterSettings;
  }

  public void setClusterSettings(Map<String, String> clusterSettings) {
    this.clusterSettings = clusterSettings;
  }


  public Map<String, String> getStackSettings() {
    return stackSettings;
  }

  public void setStackSettings(Map<String, String> stackSettings) {
    this.stackSettings = stackSettings;
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
   * Gets whether configuration tags shoudl be refreshed right before the
   * command is scheduled.
   */
  public boolean getForceRefreshConfigTagsBeforeExecution() {
    return forceRefreshConfigTagsBeforeExecution;
  }

  public void setForceRefreshConfigTagsBeforeExecution(boolean forceRefreshConfigTagsBeforeExecution) {
    this.forceRefreshConfigTagsBeforeExecution = forceRefreshConfigTagsBeforeExecution;
  }

  public Set<String> getLocalComponents() {
    return localComponents;
  }

  public void setLocalComponents(Set<String> localComponents) {
    this.localComponents = localComponents;
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

  public String getServiceGroupName() {
    return serviceGroupName;
  }

  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  /**
   * Get a value indicating whether this service is enabled
   * for credential store use.
   *
   * @return "true" or "false", any other value is
   * considered as "false"
   */
  public String getCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }

  /**
   * Set a value indicating whether this service is enabled
   * for credential store use.
   *
   * @param credentialStoreEnabled
   */
  public void setCredentialStoreEnabled(String credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
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
   * Returns  parameters for kerberos commands
   * @return  parameters for kerberos commands
   */
  public List<Map<String, String>> getKerberosCommandParams() {
    return kerberosCommandParams;
  }

  /**
   * Sets parameters for kerberos commands
   * @param  params for kerberos commands
   */
  public void setKerberosCommandParams(List<Map<String, String>> params) {
    kerberosCommandParams =  params;
  }

  /**
   * @return the repository file that is to be written.
   */
  public CommandRepository getRepositoryFile() {
    return commandRepository;
  }

  /**
   * @param repository  the command repository instance.
   */
  public void setRepositoryFile(CommandRepository repository) {
    commandRepository = repository;
  }

  /**
   * Gets the object-based role parameters for the command.
   */
  public Map<String, Object> getRoleParameters() {
    return roleParameters;
  }

  /**
   * Sets the role parameters for the command.  This is preferred over {@link #setRoleParams(Map)},
   * as this form will pass values as structured data, as opposed to unstructured, escaped json.
   *
   * @param params
   */
  public void setRoleParameters(Map<String, Object> params) {
    roleParameters = params;
  }

  /**
   * Contains key name strings. These strings are used inside maps
   * incapsulated inside command.
   */
  public interface KeyNames {
    String COMMAND_TIMEOUT = "command_timeout";
    String SCRIPT = "script";
    String SCRIPT_TYPE = "script_type";
    String SERVICE_PACKAGE_FOLDER = "service_package_folder";
    String HOOKS_FOLDER = "hooks_folder";
    String CUSTOM_FOLDER = "custom_folder";
    String STACK_NAME = "stack_name";
    String SERVICE_TYPE = "service_type";
    String STACK_VERSION = "stack_version";
    String PACKAGE_LIST = "package_list";
    String JDK_LOCATION = "jdk_location";
    String JAVA_HOME = "java_home";
    String GPL_LICENSE_ACCEPTED = "gpl_license_accepted";
    String AMBARI_JAVA_HOME = "ambari_java_home";
    String AMBARI_JDK_NAME = "ambari_jdk_name";
    String AMBARI_JCE_NAME = "ambari_jce_name";
    String AMBARI_JAVA_VERSION = "ambari_java_version";
    String JAVA_VERSION = "java_version";
    String JDK_NAME = "jdk_name";
    String JCE_NAME = "jce_name";
    String UNLIMITED_KEY_JCE_REQUIRED = "unlimited_key_jce_required";
    String MYSQL_JDBC_URL = "mysql_jdbc_url";
    String ORACLE_JDBC_URL = "oracle_jdbc_url";
    String DB_DRIVER_FILENAME = "db_driver_filename";
    String CLIENTS_TO_UPDATE_CONFIGS = "clientsToUpdateConfigs";

    String DB_NAME = "db_name";
    String GLOBAL = "global";
    String AMBARI_DB_RCA_URL = "ambari_db_rca_url";
    String AMBARI_DB_RCA_DRIVER = "ambari_db_rca_driver";
    String AMBARI_DB_RCA_USERNAME = "ambari_db_rca_username";
    String AMBARI_DB_RCA_PASSWORD = "ambari_db_rca_password";
    String COMPONENT_CATEGORY = "component_category";
    String USER_LIST = "user_list";
    String GROUP_LIST = "group_list";
    String USER_GROUPS = "user_groups";
    String NOT_MANAGED_HDFS_PATH_LIST = "not_managed_hdfs_path_list";
    String REFRESH_TOPOLOGY = "refresh_topology";
    String HOST_SYS_PREPPED = "host_sys_prepped";
    String MAX_DURATION_OF_RETRIES = "max_duration_for_retries";
    String COMMAND_RETRY_ENABLED = "command_retry_enabled";
    String AGENT_STACK_RETRY_ON_UNAVAILABILITY = "agent_stack_retry_on_unavailability";
    String AGENT_STACK_RETRY_COUNT = "agent_stack_retry_count";
    String LOG_OUTPUT = "log_output";

    /**
     * A boolean indicating whether configuration tags should be refreshed
     * before sending the command.
     */
    String REFRESH_CONFIG_TAGS_BEFORE_EXECUTION = "forceRefreshConfigTagsBeforeExecution";

    String SERVICE_CHECK = "SERVICE_CHECK"; // TODO: is it standard command? maybe add it to RoleCommand enum?
    String CUSTOM_COMMAND = "custom_command";

    /**
     * The key indicating that there is an un-finalized upgrade which is suspended.
     */
    String UPGRADE_SUSPENDED = "upgrade_suspended";

    /**
     * The version of the component to send down with the command. Normally,
     * this is simply the repository version of the component. However, during
     * upgrades, this value may change depending on the progress of the upgrade
     * and the type/direction.
     */
    @Experimental(
        feature = ExperimentalFeature.PATCH_UPGRADES,
        comment = "Change this to reflect the component version")
    String VERSION = "version";

    /**
     * When installing packages, includes what services will be included in the upgrade
     */
    String CLUSTER_VERSION_SUMMARY = "cluster_version_summary";

    /**
     * The ID of the mpack associated with this command.
     */
    String MPACK_ID = "mpack_id";
  }

  /**
   * @return
   */
  public Map<String, Map<String, String>> getComponentVersionMap() {
    return componentVersionMap;
  }

  /**
   * Used to set a map of {service -> { component -> version}}. This is
   * necessary when performing an upgrade to correct build paths of required
   * binaries. This method will only set the version information for a component
   * if:
   * <ul>
   * <li>The component advertises a version</li>
   * <li>The repository for the component has been resolved and the version can
   * be trusted</li>
   * </ul>
   *
   * @param cluster
   *          the cluster from which to build the map
   */
  public void setComponentVersions(Cluster cluster) throws AmbariException {
    componentVersionMap = cluster.getComponentVersionMap();
  }

  /**
   * Sets the upgrade summary if there is an active upgrade in the cluster.
   *
   * @param upgradeSummary
   *          the upgrade or {@code null} for none.
   */
  public void setUpgradeSummary(UpgradeSummary upgradeSummary) {
    this.upgradeSummary = upgradeSummary;
  }

  /**
   * Gets the upgrade summary if there is an active upgrade in the cluster.
   *
   * @return the upgrade or {@code null} for none.
   */
  public UpgradeSummary getUpgradeSummary() {
    return upgradeSummary;
  }

  /**
   * Gets the ID of the management pack associated with this command, or
   * {@code null} if none.
   *
   * @return the mpackId the ID of the mpack or {@code null}.
   */
  public Long getMpackId() {
    return mpackId;
  }

  /**
   * Sets the ID of the management pack associated with this command, or
   * {@code null} if none.
   *
   * @param mpackId
   *          the mpackId to set
   */
  public void setMpackId(Long mpackId) {
    this.mpackId = mpackId;
  }
}
