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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PreUpgradeCheckRequest;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheckStatus;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheckType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Manages pre-upgrade checks.
 */
@Singleton
public class UpgradeCheckHelper {

  /**
   * Log.
   */
  private static Logger LOG = LoggerFactory.getLogger(UpgradeCheckHelper.class);

  /**
   * List of all possible upgrade checks.
   */
  final List<UpgradeCheckDescriptor> registry = new ArrayList<UpgradeCheckDescriptor>();

  @Inject
  Provider<Clusters> clustersProvider;

  @Inject
  Provider<Configuration> configurationProvider;

  @Inject
  Provider<HostVersionDAO> hostVersionDaoProvider;

  @Inject
  Provider<RepositoryVersionDAO> repositoryVersionDaoProvider;

  /**
   * Constructor. Fills upgrade check registry upon creation.
   */
  public UpgradeCheckHelper() {
    registry.add(new HostsHeartbeatCheck());
    registry.add(new HostsMasterMaintenanceCheck());
    registry.add(new HostsRepositoryVersionCheck());
    registry.add(new ServicesUpCheck());
    registry.add(new ServicesMaintenanceModeCheck());
    registry.add(new ServicesNamenodeHighAvailabilityCheck());
    registry.add(new ServicesYarnWorkPreservingCheck());
    registry.add(new ServicesDecommissionCheck());
    registry.add(new ServicesJobsDistributedCacheCheck());
  }

  /**
   * Executes all registered pre upgrade checks.
   *
   * @param request pre upgrade check request
   * @return list of upgrade check results
   */
  public List<UpgradeCheck> performPreUpgradeChecks(PreUpgradeCheckRequest request) {
    final String clusterName = request.getClusterName();
    final List<UpgradeCheck> upgradeCheckResults = new ArrayList<UpgradeCheck>();
    for (UpgradeCheckDescriptor upgradeCheckDescriptor: registry) {
      final UpgradeCheck upgradeCheck = new UpgradeCheck(
          upgradeCheckDescriptor.id, upgradeCheckDescriptor.description,
          upgradeCheckDescriptor.type, clusterName);
      try {
        if (upgradeCheckDescriptor.isApplicable(request)) {
          upgradeCheckDescriptor.perform(upgradeCheck, request);
          upgradeCheckResults.add(upgradeCheck);
        }
      } catch (ClusterNotFoundException ex) {
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Cluster with name " + clusterName + " doesn't exists");
        upgradeCheckResults.add(upgradeCheck);
      } catch (Exception ex) {
        LOG.error("Pre-upgrade check " + upgradeCheckDescriptor.id + " failed", ex);
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Unexpected server error happened");
        upgradeCheckResults.add(upgradeCheck);
      }
    }
    return upgradeCheckResults;
  }

  /**
   * Describes upgrade check.
   */
  protected abstract class UpgradeCheckDescriptor {

    private final String id;
    private final String description;
    private final UpgradeCheckType type;

    /**
     * Constructor.
     *
     * @param id unique identifier
     * @param type type
     * @param description description
     */
    public UpgradeCheckDescriptor(String id, UpgradeCheckType type, String description) {
      this.id = id;
      this.type = type;
      this.description = description;
    }

    /**
     * Tests if the upgrade check is applicable to given cluster. By default returns true.
     *
     * @param request pre upgrade check request
     * @return true if check should be performed
     *
     * @throws AmbariException if server error happens
     */
    public boolean isApplicable(PreUpgradeCheckRequest request) throws AmbariException {
      return true;
    }

    /**
     * Executes check against given cluster.
     *
     * @param upgradeCheck dto for upgrade check results
     * @param request pre upgrade check request
     *
     * @throws AmbariException if server error happens
     */
    public abstract void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException;
  }

  /**
   * Checks that services are up.
   */
  protected class ServicesUpCheck extends UpgradeCheckDescriptor {

    /**
     * Constructor.
     */
    public ServicesUpCheck() {
      super("SERVICES_UP", UpgradeCheckType.SERVICE, "All services must be up");
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      for (Map.Entry<String, Service> serviceEntry : cluster.getServices().entrySet()) {
        final Service service = serviceEntry.getValue();
        if (!service.isClientOnlyService() && service.getDesiredState() != State.STARTED) {
          upgradeCheck.getFailedOn().add(service.getName());
        }
      }
      if (!upgradeCheck.getFailedOn().isEmpty()) {
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Some services are down");
      }
    }
  }

  /**
   * Checks that services are in the maintenance mode.
   */
  protected class ServicesMaintenanceModeCheck extends UpgradeCheckDescriptor {

    /**
     * Constructor.
     */
    public ServicesMaintenanceModeCheck() {
      super("SERVICES_MAINTENANCE_MODE", UpgradeCheckType.SERVICE, "No service can be in maintenance mode");
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      for (Map.Entry<String, Service> serviceEntry : cluster.getServices().entrySet()) {
        final Service service = serviceEntry.getValue();
        if (service.getMaintenanceState() == MaintenanceState.ON) {
          upgradeCheck.getFailedOn().add(service.getName());
        }
      }
      if (!upgradeCheck.getFailedOn().isEmpty()) {
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Some services are in Maintenance Mode");
      }
    }
  }

  /**
   * Checks that all hosts are either in maintenance mode or heartbeating with the server.
   */
  protected class HostsHeartbeatCheck extends UpgradeCheckDescriptor {

    /**
     * Constructor.
     */
    public HostsHeartbeatCheck() {
      super("HOSTS_HEARTBEAT", UpgradeCheckType.HOST, "All hosts must be heartbeating with the server unless they are in Maintenance Mode");
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
      for (Map.Entry<String, Host> hostEntry : clusterHosts.entrySet()) {
        final Host host = hostEntry.getValue();
        if (host.getHealthStatus().getHealthStatus() == HealthStatus.UNKNOWN && host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.OFF) {
          upgradeCheck.getFailedOn().add(host.getHostName());
        }
      }
      if (!upgradeCheck.getFailedOn().isEmpty()) {
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Some hosts are not heartbeating with the server");
      }
    }
  }

  /**
   * Checks that all hosts in maintenance state do not have master components.
   */
  protected class HostsMasterMaintenanceCheck extends UpgradeCheckDescriptor {

    /**
     * Constructor.
     */
    public HostsMasterMaintenanceCheck() {
      super("HOSTS_MASTER_MAINTENANCE", UpgradeCheckType.HOST, "Hosts in Maintenance Mode must not have any master components");
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      final MasterHostResolver masterHostResolver = new MasterHostResolver(cluster);
      final Set<String> hostsWithMasterComponent = new HashSet<String>();
      for (Entry<String, Service> serviceEntry: cluster.getServices().entrySet()) {
        final Service service = serviceEntry.getValue();
        for (Entry<String, ServiceComponent> serviceComponentEntry: service.getServiceComponents().entrySet()) {
          final ServiceComponent serviceComponent = serviceComponentEntry.getValue();
          final HostsType hostsType = masterHostResolver.getMasterAndHosts(service.getName(), serviceComponent.getName());
          if (hostsType != null && hostsType.master != null) {
            hostsWithMasterComponent.add(hostsType.master);
          }
        }
      }
      final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
      for (Map.Entry<String, Host> hostEntry : clusterHosts.entrySet()) {
        final Host host = hostEntry.getValue();
        if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.ON && hostsWithMasterComponent.contains(host.getHostName())) {
          upgradeCheck.getFailedOn().add(host.getHostName());
        }
      }
      if (!upgradeCheck.getFailedOn().isEmpty()) {
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Some hosts with master components are in Maintenance Mode");
      }
    }
  }

  /**
   * Checks that all hosts have particular repository version.
   */
  protected class HostsRepositoryVersionCheck extends UpgradeCheckDescriptor {

    /**
     * Constructor.
     */
    public HostsRepositoryVersionCheck() {
      super("HOSTS_REPOSITORY_VERSION", UpgradeCheckType.HOST, "Hosts should have the new repository version installed");
    }

    @Override
    public boolean isApplicable(PreUpgradeCheckRequest request) throws AmbariException {
      return request.getRepositoryVersion() != null;
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
      final StackId stackId = cluster.getDesiredStackVersion();
      final RepositoryVersionEntity repositoryVersion = repositoryVersionDaoProvider.get().findByStackAndVersion(stackId.getStackId(), request.getRepositoryVersion());
      if (repositoryVersion == null) {
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Repository version " + request.getRepositoryVersion() + " doesn't exist");
        upgradeCheck.getFailedOn().addAll(clusterHosts.keySet());
        return;
      }
      for (Map.Entry<String, Host> hostEntry : clusterHosts.entrySet()) {
        final Host host = hostEntry.getValue();
        if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.OFF) {
          final HostVersionEntity hostVersion = hostVersionDaoProvider.get().findByClusterStackVersionAndHost(clusterName, repositoryVersion.getStack(), repositoryVersion.getVersion(), host.getHostName());
          if (hostVersion == null || hostVersion.getState() != RepositoryVersionState.INSTALLED) {
            upgradeCheck.getFailedOn().add(host.getHostName());
          }
        }
      }
      if (!upgradeCheck.getFailedOn().isEmpty()) {
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Some hosts do not have repository version " + request.getRepositoryVersion() + " installed");
      }
    }
  }

  /**
   * Checks that namenode high availability is enabled.
   */
  protected class ServicesNamenodeHighAvailabilityCheck extends UpgradeCheckDescriptor {

    /**
     * Constructor.
     */
    public ServicesNamenodeHighAvailabilityCheck() {
      super("SERVICES_NAMENODE_HA", UpgradeCheckType.SERVICE, "Namenode high availability should be enabled");
    }

    @Override
    public boolean isApplicable(PreUpgradeCheckRequest request) throws AmbariException {
      final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
      return cluster.getService("HDFS") != null;
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      final String configType = "hdfs-site";
      final Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
      final DesiredConfig desiredConfig = desiredConfigs.get(configType);
      final Config config = cluster.getConfig(configType, desiredConfig.getTag());
      if (!config.getProperties().containsKey("dfs.nameservices")) {
        upgradeCheck.getFailedOn().add("HDFS");
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("Namenode high availability is disabled");
      }
    }
  }

  /**
   * Checks that YARN has work-preserving restart enabled.
   */
  protected class ServicesYarnWorkPreservingCheck extends UpgradeCheckDescriptor {

    /**
     * Constructor.
     */
    public ServicesYarnWorkPreservingCheck() {
      super("SERVICES_YARN_WP", UpgradeCheckType.SERVICE, "YARN work preserving restart should be enabled");
    }

    @Override
    public boolean isApplicable(PreUpgradeCheckRequest request) throws AmbariException {
      final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
      try {
        cluster.getService("YARN");
      } catch (ServiceNotFoundException ex) {
        return false;
      }
      return true;
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      final String configType = "yarn-site";
      final Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
      final DesiredConfig desiredConfig = desiredConfigs.get(configType);
      final Config config = cluster.getConfig(configType, desiredConfig.getTag());
      if (!config.getProperties().containsKey("yarn.resourcemanager.work-preserving-recovery.enabled") ||
          !Boolean.getBoolean(config.getProperties().get("yarn.resourcemanager.work-preserving-recovery.enabled"))) {
        upgradeCheck.getFailedOn().add("YARN");
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("YARN doesn't have work preserving restart, yarn.resourcemanager.work-preserving-recovery.enabled property is missing");
      }
    }
  }

  /**
   * Checks that there are no services in decommission state.
   */
  protected class ServicesDecommissionCheck extends UpgradeCheckDescriptor {

    /**
     * Constructor.
     */
    public ServicesDecommissionCheck() {
      super("SERVICES_DECOMMISSION", UpgradeCheckType.SERVICE, "Services should not be in Decommission state");
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      for (Entry<String, Service> serviceEntry: cluster.getServices().entrySet()) {
        final Service service = serviceEntry.getValue();
        for (Entry<String, ServiceComponent> serviceComponentEntry: service.getServiceComponents().entrySet()) {
          final ServiceComponent serviceComponent = serviceComponentEntry.getValue();
          for (String hostName : serviceComponent.getServiceComponentHosts().keySet()) {
            final ServiceComponentHost scHost = serviceComponent.getServiceComponentHost(hostName);
            if (scHost.getComponentAdminState() == HostComponentAdminState.DECOMMISSIONED || scHost.getComponentAdminState() == HostComponentAdminState.DECOMMISSIONING) {
              upgradeCheck.getFailedOn().add(serviceComponent.getName());
            }
          }
        }
      }
      if (!upgradeCheck.getFailedOn().isEmpty()) {
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("There are services in decommission or decommissioning state");
      }
    }
  }

  /**
   * Checks that MR, Oozie and Tez jobs reference hadoop libraries from the distributed cache.
   */
  protected class ServicesJobsDistributedCacheCheck extends UpgradeCheckDescriptor {

    @Override
    public boolean isApplicable(PreUpgradeCheckRequest request)
        throws AmbariException {
      final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
      try {
        cluster.getService("YARN");
      } catch (ServiceNotFoundException ex) {
        return false;
      }
      return true;
    }

    /**
     * Constructor.
     */
    public ServicesJobsDistributedCacheCheck() {
      super("SERVICES_JOBS_DISTRIBUTED_CACHE", UpgradeCheckType.SERVICE, "Jobs should reference hadoop libraries from the distributed cache");
    }

    @Override
    public void perform(UpgradeCheck upgradeCheck, PreUpgradeCheckRequest request) throws AmbariException {
      final String clusterName = request.getClusterName();
      final Cluster cluster = clustersProvider.get().getCluster(clusterName);
      final String configType = "mapred-site";
      final Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
      final DesiredConfig desiredConfig = desiredConfigs.get(configType);
      final Config config = cluster.getConfig(configType, desiredConfig.getTag());
      if (!config.getProperties().containsKey("mapreduce.application.framework.path") || !config.getProperties().containsKey("mapreduce.application.classpath")) {
        // TODO actually it is needed to validate that these properties contain proper values but the tickets for these changes are still open, so it will cause
        // preupgrade checks to fail
        upgradeCheck.getFailedOn().add("MR");
        upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
        upgradeCheck.setFailReason("mapreduce.application.framework.path and mapreduce.application.classpath should reference distributed cache");
      }
    }
  }
}
