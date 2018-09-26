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
package org.apache.ambari.server.agent.stomp;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DFS_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GROUP_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.NOT_MANAGED_HDFS_PATH_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_GROUPS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_LIST;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.RecoveryConfig;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.agent.stomp.dto.HostLevelParamsCluster;
import org.apache.ambari.server.agent.stomp.dto.HostRepositories;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.HostLevelParamsUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceComponentRecoveryChangedEvent;
import org.apache.ambari.server.events.ServiceGroupMpackChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.mpack.MpackManager;
import org.apache.ambari.server.orm.entities.MpackHostStateEntity;
import org.apache.ambari.server.state.BlueprintProvisioningState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.collections.MapUtils;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class HostLevelParamsHolder extends AgentHostDataHolder<HostLevelParamsUpdateEvent> {

  @Inject
  private RecoveryConfigHelper recoveryConfigHelper;

  @Inject
  private Clusters clusters;

  @Inject
  private Provider<AmbariManagementController> m_ambariManagementController;

  @Inject
  private Gson gson;

  @Inject
  private Provider<ConfigHelper> configHelperProvider;

  @Inject
  private Provider<AmbariMetaInfo> ambariMetaInfoProvider;

  @Inject
  public HostLevelParamsHolder(AmbariEventPublisher ambariEventPublisher) {
    ambariEventPublisher.register(this);
  }

  @Override
  public HostLevelParamsUpdateEvent getCurrentData(Long hostId) throws AmbariException {
    return getCurrentDataExcludeCluster(hostId, null);
  }

  public HostLevelParamsUpdateEvent getCurrentDataExcludeCluster(Long hostId, Long clusterId) throws AmbariException {
    TreeMap<String, HostLevelParamsCluster> hostLevelParamsClusters = new TreeMap<>();
    Host host = clusters.getHostById(hostId);
    for (Cluster cl : clusters.getClustersForHost(host.getHostName())) {
      if (clusterId != null && cl.getClusterId() == clusterId) {
        continue;
      }
      HostLevelParamsCluster hostLevelParamsCluster = new HostLevelParamsCluster(
          m_ambariManagementController.get().retrieveHostRepositories(cl, host),
          recoveryConfigHelper.getRecoveryConfig(cl.getClusterName(), host.getHostName()),
          m_ambariManagementController.get().getBlueprintProvisioningStates(cl.getClusterId(), host.getHostId()),
          getHostStacksSettings(cl, host));

      hostLevelParamsClusters.put(Long.toString(cl.getClusterId()),
          hostLevelParamsCluster);
    }
    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(hostId, hostLevelParamsClusters);
    return hostLevelParamsUpdateEvent;
  }

  public void updateAllHosts() throws AmbariException {
    for (Host host : clusters.getHosts()) {
      updateData(getCurrentData(host.getHostId()));
    }
  }

  @Override
  protected HostLevelParamsUpdateEvent handleUpdate(HostLevelParamsUpdateEvent current, HostLevelParamsUpdateEvent update) {
    HostLevelParamsUpdateEvent result = null;
    boolean changed = false;
    Map<String, HostLevelParamsCluster> mergedClusters = new HashMap<>();
    if (MapUtils.isNotEmpty(update.getHostLevelParamsClusters())) {
      // put from current all clusters absent in update
      for (Map.Entry<String, HostLevelParamsCluster> hostLevelParamsClusterEntry : current.getHostLevelParamsClusters().entrySet()) {
        String clusterId = hostLevelParamsClusterEntry.getKey();
        if (!update.getHostLevelParamsClusters().containsKey(clusterId)) {
          mergedClusters.put(clusterId, hostLevelParamsClusterEntry.getValue());
        }
      }
      // process clusters from update
      for (Map.Entry<String, HostLevelParamsCluster> hostLevelParamsClusterEntry : update.getHostLevelParamsClusters().entrySet()) {
        String clusterId = hostLevelParamsClusterEntry.getKey();
        if (current.getHostLevelParamsClusters().containsKey(clusterId)) {
          boolean clusterChanged = false;
          HostLevelParamsCluster updatedCluster = hostLevelParamsClusterEntry.getValue();
          HostLevelParamsCluster currentCluster = current.getHostLevelParamsClusters().get(clusterId);
          RecoveryConfig mergedRecoveryConfig;
          SortedMap<Long, CommandRepository> mergedRepositories;
          Map<String, BlueprintProvisioningState> mergedBlueprintProvisioningStates;
          SortedMap<String, Long> mergedComponentRepos;
          SortedMap<Long, SortedMap<String, String>> mergedStacksSettings;
          if (!currentCluster.getRecoveryConfig().equals(updatedCluster.getRecoveryConfig())) {
            mergedRecoveryConfig = updatedCluster.getRecoveryConfig();
            clusterChanged = true;
          } else {
            mergedRecoveryConfig = currentCluster.getRecoveryConfig();
          }
          if (!currentCluster.getHostRepositories().getRepositories()
              .equals(updatedCluster.getHostRepositories().getRepositories())) {
            mergedRepositories = updatedCluster.getHostRepositories().getRepositories();
            clusterChanged = true;
          } else {
            mergedRepositories = currentCluster.getHostRepositories().getRepositories();
          }
          if (!currentCluster.getBlueprintProvisioningState()
              .equals(updatedCluster.getBlueprintProvisioningState())) {
            mergedBlueprintProvisioningStates = updatedCluster.getBlueprintProvisioningState();
            clusterChanged = true;
          } else {
            mergedBlueprintProvisioningStates = currentCluster.getBlueprintProvisioningState();
          }
          if (!currentCluster.getHostRepositories().getComponentRepos()
              .equals(updatedCluster.getHostRepositories().getComponentRepos())) {
            mergedComponentRepos = updatedCluster.getHostRepositories().getComponentRepos();
            clusterChanged = true;
          } else {
            mergedComponentRepos = currentCluster.getHostRepositories().getComponentRepos();
          }
          if (!currentCluster.getStacksSettings()
              .equals(updatedCluster.getStacksSettings())) {
            mergedStacksSettings = updatedCluster.getStacksSettings();
            clusterChanged = true;
          } else {
            mergedStacksSettings = currentCluster.getStacksSettings();
          }
          if (clusterChanged) {
            HostLevelParamsCluster mergedCluster = new HostLevelParamsCluster(
                new HostRepositories(mergedRepositories, mergedComponentRepos),
                mergedRecoveryConfig,
                mergedBlueprintProvisioningStates,
                mergedStacksSettings);
            mergedClusters.put(clusterId, mergedCluster);
            changed = true;
          } else {
            mergedClusters.put(clusterId, hostLevelParamsClusterEntry.getValue());
          }
        } else {
          mergedClusters.put(clusterId, hostLevelParamsClusterEntry.getValue());
          changed = true;
        }
      }
    }
    if (changed) {
      result = new HostLevelParamsUpdateEvent(current.getHostId(), mergedClusters);
    }
    return result;
  }

  @Override
  protected HostLevelParamsUpdateEvent getEmptyData() {
    return HostLevelParamsUpdateEvent.emptyUpdate();
  }

  @Subscribe
  public void onUpgradeDesiredMpackChange(ServiceGroupMpackChangedEvent clusterComponentsRepoChangedEvent) throws AmbariException {
    Long clusterId = clusterComponentsRepoChangedEvent.getClusterId();

    Cluster cluster = clusters.getCluster(clusterComponentsRepoChangedEvent.getClusterId());
    for (Host host : cluster.getHosts()) {
      updateDataOfHost(clusterComponentsRepoChangedEvent.getClusterId(), cluster, host);
    }
  }

  @Subscribe
  public void onServiceComponentRecoveryChanged(ServiceComponentRecoveryChangedEvent event) throws AmbariException {
    long clusterId = event.getClusterId();
    Cluster cluster = clusters.getCluster(clusterId);
    for (ServiceComponentHost host : cluster.getServiceComponentHosts(event.getServiceName(), event.getComponentName())) {
      updateDataOfHost(clusterId, cluster, host.getHost());
    }
  }

  private void updateDataOfHost(long clusterId, Cluster cluster, Host host) throws AmbariException {
    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(host.getHostId(),
        Long.toString(clusterId),
            new HostLevelParamsCluster(
                    m_ambariManagementController.get().retrieveHostRepositories(cluster, host),
                    recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), host.getHostName()),
                    m_ambariManagementController.get().getBlueprintProvisioningStates(clusterId, host.getHostId()),
                    getHostStacksSettings(cluster, host)));
    updateData(hostLevelParamsUpdateEvent);
  }

  @Subscribe
  public void onMaintenanceModeChanged(MaintenanceModeEvent event) throws AmbariException {
    long clusterId = event.getClusterId();
    Cluster cluster = clusters.getCluster(clusterId);
    if (event.getHost() != null || event.getServiceComponentHost() != null) {
      Host host = event.getHost() != null ? event.getHost() : event.getServiceComponentHost().getHost();
      updateDataOfHost(clusterId, cluster, host);
    }
    else if (event.getService() != null) {
      for (String hostName : event.getService().getServiceHosts()) {
        updateDataOfHost(clusterId, cluster, cluster.getHost(hostName));
      }
    }
  }

  public SortedMap<Long, SortedMap<String, String>> getHostStacksSettings(Cluster cl, Host host) throws AmbariException {
    SortedMap<Long, SortedMap<String, String>> stacksSettings = new TreeMap<>();
    MpackManager mpackManager = ambariMetaInfoProvider.get().getMpackManager();
    for (MpackHostStateEntity mpackHostStateEntity : host.getMPackInstallStates()) {
      Long mpackId = mpackHostStateEntity.getMpack().getId();
      Mpack mpack = mpackManager.getMpackMap().get(mpackId);
      if (mpack == null) {
        throw new AmbariException(String.format("No mpack with id %s found", mpackId));
      }
      stacksSettings.put(mpackId, new TreeMap<>(getStackSettings(cl, mpack.getStackId())));
    }
    return stacksSettings;
  }


  private SortedMap<String, String> getStackSettings(Cluster cluster, StackId stackId) throws AmbariException {
    AmbariMetaInfo ambariMetaInfo = ambariMetaInfoProvider.get();
    SortedMap<String, String> stackLevelParams = new TreeMap<>(ambariMetaInfo.getStackSettingsNameValueMap(stackId));

    // STACK_NAME is part of stack settings, but STACK_VERSION is not
    stackLevelParams.put(STACK_VERSION, stackId.getStackVersion());

    Map<String, DesiredConfig> clusterDesiredConfigs = cluster.getDesiredConfigs(false);
    Set<PropertyInfo> stackProperties = ambariMetaInfo.getStackProperties(stackId.getStackName(), stackId.getStackVersion());
    Map<String, ServiceInfo> servicesMap = ambariMetaInfo.getServices(stackId.getStackName(), stackId.getStackVersion());
    Set<PropertyInfo> clusterProperties = ambariMetaInfo.getClusterProperties();

    ConfigHelper configHelper = configHelperProvider.get();
    Map<PropertyInfo, String> users = configHelper.getPropertiesWithPropertyType(PropertyInfo.PropertyType.USER, cluster, clusterDesiredConfigs, servicesMap, stackProperties, clusterProperties);
    Set<String> userSet = new TreeSet<>(users.values());
    String userList = gson.toJson(userSet);
    stackLevelParams.put(USER_LIST, userList);

    Map<PropertyInfo, String> groups = configHelper.getPropertiesWithPropertyType(PropertyInfo.PropertyType.GROUP, cluster, clusterDesiredConfigs, servicesMap, stackProperties, clusterProperties);
    Set<String> groupSet = new TreeSet<>(groups.values());
    String groupList = gson.toJson(groupSet);
    stackLevelParams.put(GROUP_LIST, groupList);

    Map<String, Set<String>> userGroupsMap = configHelper.createUserGroupsMap(users, groups);
    String userGroups = gson.toJson(userGroupsMap);
    stackLevelParams.put(USER_GROUPS, userGroups);

    Map<PropertyInfo, String> notManagedHdfsPathMap = configHelper.getPropertiesWithPropertyType(PropertyInfo.PropertyType.NOT_MANAGED_HDFS_PATH, cluster, clusterDesiredConfigs, servicesMap, stackProperties, clusterProperties);
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
}
