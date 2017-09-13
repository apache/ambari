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
package org.apache.ambari.server.checks;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO.LastServiceCheckDTO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Checks that all Service Checks are less recent than last
 * configuration update for given services.
 * That is a potential problem when doing stack update.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.DEFAULT,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class ServiceCheckValidityCheck extends AbstractCheckDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceCheckValidityCheck.class);

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");

  @Inject
  Provider<ServiceConfigDAO> serviceConfigDAOProvider;

  @Inject
  Provider<HostRoleCommandDAO> hostRoleCommandDAOProvider;

  @Inject
  Provider<ActionMetadata> actionMetadataProvider;

  /**
   * Constructor.
   */
  public ServiceCheckValidityCheck() {
    super(CheckDescription.SERVICE_CHECK);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
      throws AmbariException {

    ServiceConfigDAO serviceConfigDAO = serviceConfigDAOProvider.get();
    HostRoleCommandDAO hostRoleCommandDAO = hostRoleCommandDAOProvider.get();

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    long clusterId = cluster.getClusterId();

    // build a mapping of the last config changes by service
    Map<String, Long> lastServiceConfigUpdates = new HashMap<>();
    for (Service service : cluster.getServices().values()) {
      if (service.getMaintenanceState() != MaintenanceState.OFF || !hasAtLeastOneComponentVersionAdvertised(service)) {
        continue;
      }
      StackId stackId = service.getDesiredStackId();
      boolean isServiceWitNoConfigs = ambariMetaInfo.get().isServiceWithNoConfigs(stackId.getStackName(), stackId.getStackVersion(), service.getName());
      if (isServiceWitNoConfigs){
        LOG.info(String.format("%s in %s version %s does not have customizable configurations. Skip checking service configuration history.", service.getName(), stackId.getStackName(), stackId.getStackVersion()));
      } else {
        LOG.info(String.format("%s in %s version %s has customizable configurations. Check service configuration history.", service.getName(), stackId.getStackName(), stackId.getStackVersion()));
        ServiceConfigEntity lastServiceConfig = serviceConfigDAO.getLastServiceConfig(clusterId, service.getName());
        lastServiceConfigUpdates.put(service.getName(), lastServiceConfig.getCreateTimestamp());
      }
    }

    // get the latest service checks, grouped by role
    List<LastServiceCheckDTO> lastServiceChecks = hostRoleCommandDAO.getLatestServiceChecksByRole(clusterId);
    Map<String, Long> lastServiceChecksByRole = new HashMap<>();
    for( LastServiceCheckDTO lastServiceCheck : lastServiceChecks ) {
      lastServiceChecksByRole.put(lastServiceCheck.role, lastServiceCheck.endTime);
    }

    LinkedHashSet<String> failedServiceNames = new LinkedHashSet<>();

    // for every service, see if there was a service check executed and then
    for( Entry<String, Long> entry : lastServiceConfigUpdates.entrySet() ) {
      String serviceName = entry.getKey();
      long configCreationTime = entry.getValue();
      String role = actionMetadataProvider.get().getServiceCheckAction(serviceName);

      if(!lastServiceChecksByRole.containsKey(role) ) {
        LOG.info("There was no service check found for service {} matching role {}", serviceName, role);
        failedServiceNames.add(serviceName);
        continue;
      }

      long lastServiceCheckTime = lastServiceChecksByRole.get(role);
      if (lastServiceCheckTime < configCreationTime) {
        failedServiceNames.add(serviceName);
        LOG.info(
            "The {} service (role {}) had its configurations updated on {}, but the last service check was {}",
            serviceName, role, DATE_FORMAT.format(new Date(configCreationTime)),
            DATE_FORMAT.format(new Date(lastServiceCheckTime)));
      }
    }

    if (!failedServiceNames.isEmpty()) {
      prerequisiteCheck.setFailedOn(failedServiceNames);
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      String failReason = getFailReason(prerequisiteCheck, request);
      prerequisiteCheck.setFailReason(String.format(failReason, StringUtils.join(failedServiceNames, ", ")));
    }
  }

  private boolean hasAtLeastOneComponentVersionAdvertised(Service service) {
    Collection<ServiceComponent> components = service.getServiceComponents().values();
    for (ServiceComponent component : components) {
      if (component.isVersionAdvertised()) {
        return true;
      }
    }
    return false;
  }

}
