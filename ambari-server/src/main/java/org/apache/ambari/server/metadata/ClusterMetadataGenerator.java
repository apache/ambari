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
package org.apache.ambari.server.metadata;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AGENT_STACK_RETRY_COUNT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AGENT_STACK_RETRY_ON_UNAVAILABILITY;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_SERVER_HOST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_SERVER_PORT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_SERVER_USE_SSL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CLUSTER_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DFS_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GPL_LICENSE_ACCEPTED;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GROUP_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOST_SYS_PREPPED;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JAVA_HOME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JAVA_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JCE_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.MYSQL_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.NOT_MANAGED_HDFS_PATH_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.ORACLE_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_GROUPS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_LIST;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.MetadataCluster;
import org.apache.ambari.server.agent.stomp.dto.MetadataServiceInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.AmbariConfig;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.StageUtils;

import com.google.gson.Gson;

public class ClusterMetadataGenerator {

  private final Configuration configs;
  private final ConfigHelper configHelper;
  private final AmbariMetaInfo ambariMetaInfo;
  private final Gson gson;
  private final AmbariConfig ambariConfig;

  @Inject
  public ClusterMetadataGenerator(AmbariMetaInfo metaInfo, Configuration configs, ConfigHelper configHelper, Gson gson) throws UnknownHostException {
    this.ambariMetaInfo = metaInfo;
    this.configs = configs;
    this.configHelper = configHelper;
    this.gson = gson;

    ambariConfig = new AmbariConfig(configs);
  }

  public AmbariConfig getAmbariConfig() {
    return ambariConfig;
  }

  public SortedMap<String, String> getMetadataStackLevelParams(Cluster cluster, StackId stackId) throws AmbariException {
    SortedMap<String, String> stackLevelParams = new TreeMap<>(ambariMetaInfo.getStackSettingsNameValueMap(stackId));

    // STACK_NAME is part of stack settings, but STACK_VERSION is not
    stackLevelParams.put(STACK_VERSION, stackId.getStackVersion());

    Map<String, DesiredConfig> clusterDesiredConfigs = cluster.getDesiredConfigs(false);
    Set<PropertyInfo> stackProperties = ambariMetaInfo.getStackProperties(stackId.getStackName(), stackId.getStackVersion());
    Map<String, ServiceInfo> servicesMap = ambariMetaInfo.getServices(stackId.getStackName(), stackId.getStackVersion());
    Set<PropertyInfo> clusterProperties = ambariMetaInfo.getClusterProperties();

    Map<PropertyInfo, String> users = configHelper.getPropertiesWithPropertyType(PropertyType.USER, cluster, clusterDesiredConfigs, servicesMap, stackProperties, clusterProperties);
    Set<String> userSet = new TreeSet<>(users.values());
    String userList = gson.toJson(userSet);
    stackLevelParams.put(USER_LIST, userList);

    Map<PropertyInfo, String> groups = configHelper.getPropertiesWithPropertyType(PropertyType.GROUP, cluster, clusterDesiredConfigs, servicesMap, stackProperties, clusterProperties);
    Set<String> groupSet = new TreeSet<>(groups.values());
    String groupList = gson.toJson(groupSet);
    stackLevelParams.put(GROUP_LIST, groupList);

    Map<String, Set<String>> userGroupsMap = configHelper.createUserGroupsMap(users, groups);
    String userGroups = gson.toJson(userGroupsMap);
    stackLevelParams.put(USER_GROUPS, userGroups);

    Map<PropertyInfo, String> notManagedHdfsPathMap = configHelper.getPropertiesWithPropertyType(PropertyType.NOT_MANAGED_HDFS_PATH, cluster, clusterDesiredConfigs, servicesMap, stackProperties, clusterProperties);
    Set<String> notManagedHdfsPathSet = configHelper.filterInvalidPropertyValues(notManagedHdfsPathMap, NOT_MANAGED_HDFS_PATH_LIST);
    String notManagedHdfsPathList = gson.toJson(notManagedHdfsPathSet);
    stackLevelParams.put(NOT_MANAGED_HDFS_PATH_LIST, notManagedHdfsPathList);

    Map<String, ServiceInfo> serviceInfos = ambariMetaInfo.getServices(stackId.getStackName(), stackId.getStackVersion());
    for (ServiceInfo serviceInfoInstance : serviceInfos.values()) {
      if (serviceInfoInstance.getServiceType() != null) {
        stackLevelParams.put(DFS_TYPE, serviceInfoInstance.getServiceType());
        break;
      }
    }

    return stackLevelParams;
  }

  /**
   * Collects metadata info about clusters for agent.
   */
  public MetadataUpdateEvent getClustersMetadata(Clusters clusters) throws AmbariException {
    SortedMap<String, MetadataCluster> metadataClusters = new TreeMap<>();

    for (Cluster cl : clusters.getClusters().values()) {
      SecurityType securityType = cl.getSecurityType();

      MetadataCluster metadataCluster = new MetadataCluster(securityType,
        getMetadataServiceLevelParams(cl),
        true,
        getMetadataClusterLevelParams(cl),
        null);
      metadataClusters.put(Long.toString(cl.getClusterId()), metadataCluster);
    }

    return new MetadataUpdateEvent(metadataClusters, getMetadataAmbariLevelParams(), getMetadataAgentConfigs(), UpdateEventType.CREATE);
  }

  public MetadataUpdateEvent getClusterMetadata(Cluster cl) throws AmbariException {
    SortedMap<String, MetadataCluster> metadataClusters = new TreeMap<>();
    MetadataCluster metadataCluster = new MetadataCluster(cl.getSecurityType(), getMetadataServiceLevelParams(cl), true, getMetadataClusterLevelParams(cl), null);
    metadataClusters.put(Long.toString(cl.getClusterId()), metadataCluster);
    return new MetadataUpdateEvent(metadataClusters, null, getMetadataAgentConfigs(), UpdateEventType.UPDATE);
  }

  public MetadataUpdateEvent getClusterMetadataOnConfigsUpdate(Cluster cl) {
    SortedMap<String, MetadataCluster> metadataClusters = new TreeMap<>();
    metadataClusters.put(Long.toString(cl.getClusterId()), MetadataCluster.clusterLevelParamsMetadataCluster(null, getMetadataClusterLevelParams(cl)));
    return new MetadataUpdateEvent(metadataClusters, null, getMetadataAgentConfigs(), UpdateEventType.UPDATE);
  }

  public MetadataUpdateEvent getClusterMetadataOnRepoUpdate(Cluster cl) throws AmbariException {
    SortedMap<String, MetadataCluster> metadataClusters = new TreeMap<>();
    metadataClusters.put(Long.toString(cl.getClusterId()), MetadataCluster.serviceLevelParamsMetadataCluster(null, getMetadataServiceLevelParams(cl), true));
    return new MetadataUpdateEvent(metadataClusters, null, getMetadataAgentConfigs(), UpdateEventType.UPDATE);
  }

  public MetadataUpdateEvent getClusterMetadataOnServiceInstall(Cluster cl, String serviceName) throws AmbariException {
    return getClusterMetadataOnServiceCredentialStoreUpdate(cl, serviceName);
  }

  public MetadataUpdateEvent getClusterMetadataOnServiceCredentialStoreUpdate(Cluster cl, String serviceName) throws AmbariException {
    final SortedMap<String, MetadataCluster> metadataClusters = new TreeMap<>();
    metadataClusters.put(Long.toString(cl.getClusterId()), MetadataCluster.serviceLevelParamsMetadataCluster(null, getMetadataServiceLevelParams(cl), false));
    return new MetadataUpdateEvent(metadataClusters, null, getMetadataAgentConfigs(), UpdateEventType.UPDATE);
  }

  private SortedMap<String, String> getMetadataClusterLevelParams(Cluster cluster) {
    TreeMap<String, String> clusterLevelParams = new TreeMap<>();
    clusterLevelParams.put(CLUSTER_NAME, cluster.getClusterName());
    clusterLevelParams.put(HOOKS_FOLDER, configs.getProperty(Configuration.HOOKS_FOLDER));
    return clusterLevelParams;
  }

  public SortedMap<String, MetadataServiceInfo> getMetadataServiceLevelParams(Cluster cluster) throws AmbariException {
    SortedMap<String, MetadataServiceInfo> serviceLevelParams = new TreeMap<>();
    for (Service service : cluster.getServices()) {
      serviceLevelParams.putAll(getMetadataServiceLevelParams(service));
    }
    return serviceLevelParams;
  }

  public SortedMap<String, MetadataServiceInfo> getMetadataServiceLevelParams(Service service) throws AmbariException {
    SortedMap<String, MetadataServiceInfo> serviceLevelParams = new TreeMap<>();

    StackId serviceStackId = service.getStackId();
    ServiceInfo serviceInfo = ambariMetaInfo.getService(serviceStackId.getStackName(),
      serviceStackId.getStackVersion(), service.getName());
    Long statusCommandTimeout = null;
    if (serviceInfo.getCommandScript() != null) {
      statusCommandTimeout = new Long(getStatusCommandTimeout(serviceInfo));
    }

    String servicePackageFolder = serviceInfo.getServicePackageFolder();
    Map<String, Map<String, String>> configCredentials = configHelper.getCredentialStoreEnabledProperties(serviceStackId, service);

    serviceLevelParams.put(serviceInfo.getName(), new MetadataServiceInfo(serviceInfo.getVersion(),
      service.isCredentialStoreEnabled(), configCredentials, statusCommandTimeout, servicePackageFolder));

    return serviceLevelParams;
  }

  public TreeMap<String, String> getMetadataAmbariLevelParams() {
    TreeMap<String, String> ambariLevelParams = new TreeMap<>();
    ambariLevelParams.put(JDK_LOCATION, ambariConfig.getJdkResourceUrl());
    ambariLevelParams.put(JAVA_HOME, ambariConfig.getJavaHome());
    ambariLevelParams.put(JAVA_VERSION, String.valueOf(configs.getJavaVersion()));
    ambariLevelParams.put(JDK_NAME, ambariConfig.getJDKName());
    ambariLevelParams.put(JCE_NAME, ambariConfig.getJCEName());
    ambariLevelParams.put(DB_NAME, ambariConfig.getServerDB());
    ambariLevelParams.put(MYSQL_JDBC_URL, ambariConfig.getMysqljdbcUrl());
    ambariLevelParams.put(ORACLE_JDBC_URL, ambariConfig.getOjdbcUrl());
    ambariLevelParams.put(DB_DRIVER_FILENAME, configs.getMySQLJarName());
    ambariLevelParams.put(HOST_SYS_PREPPED, configs.areHostsSysPrepped());
    ambariLevelParams.put(AGENT_STACK_RETRY_ON_UNAVAILABILITY, configs.isAgentStackRetryOnInstallEnabled());
    ambariLevelParams.put(AGENT_STACK_RETRY_COUNT, configs.getAgentStackRetryOnInstallCount());

    boolean serverUseSsl = configs.getApiSSLAuthentication();
    int port = serverUseSsl ? configs.getClientSSLApiPort() : configs.getClientApiPort();
    ambariLevelParams.put(AMBARI_SERVER_HOST, StageUtils.getHostName());
    ambariLevelParams.put(AMBARI_SERVER_PORT, Integer.toString(port));
    ambariLevelParams.put(AMBARI_SERVER_USE_SSL, Boolean.toString(serverUseSsl));

    for (Map.Entry<String, String> dbConnectorName : configs.getDatabaseConnectorNames().entrySet()) {
      ambariLevelParams.put(dbConnectorName.getKey(), dbConnectorName.getValue());
    }
    for (Map.Entry<String, String> previousDBConnectorName : configs.getPreviousDatabaseConnectorNames().entrySet()) {
      ambariLevelParams.put(previousDBConnectorName.getKey(), previousDBConnectorName.getValue());
    }
    ambariLevelParams.put(GPL_LICENSE_ACCEPTED, configs.getGplLicenseAccepted().toString());

    return ambariLevelParams;
  }

  public String getStatusCommandTimeout(ServiceInfo serviceInfo) throws AmbariException {
    String commandTimeout = configs.getDefaultAgentTaskTimeout(false);

    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      // Service check command is not custom command
      CommandScriptDefinition script = serviceInfo.getCommandScript();
      if (script != null) {
        if (script.getTimeout() > 0) {
          commandTimeout = String.valueOf(script.getTimeout());
        }
      } else {
        String message = String.format("Service %s has no command script " +
          "defined. It is not possible to run service check" +
          " for this service", serviceInfo.getName());
        throw new AmbariException(message);
      }
    }

    // Try to apply overridden service check timeout value if available
    Long overriddenTimeout = configs.getAgentServiceCheckTaskTimeout();
    if (!overriddenTimeout.equals(Configuration.AGENT_SERVICE_CHECK_TASK_TIMEOUT.getDefaultValue())) {
      commandTimeout = String.valueOf(overriddenTimeout);
    }
    return commandTimeout;
  }

  public SortedMap<String, SortedMap<String,String>> getMetadataAgentConfigs() {
    SortedMap<String, SortedMap<String,String>> agentConfigs = new TreeMap<>();
    Map<String, Map<String,String>> agentConfigsMap = configs.getAgentConfigsMap();

    for (String key : agentConfigsMap.keySet()) {
      agentConfigs.put(key, new TreeMap<>(agentConfigsMap.get(key)));
    }

    return agentConfigs;
  }

}
