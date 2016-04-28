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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.controller.internal.PageRequestImpl;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
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
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT, required = true)
public class ServiceCheckValidityCheck extends AbstractCheckDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceCheckValidityCheck.class);

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
  private static final PageRequestImpl PAGE_REQUEST = new PageRequestImpl(PageRequest.StartingPoint.End, 1000, 0, null, null);
  private static final RequestImpl REQUEST = new RequestImpl(null, null, null, null, null, PAGE_REQUEST);


  @Inject
  Provider<ServiceConfigDAO> serviceConfigDAOProvider;

  @Inject
  Provider<HostRoleCommandDAO> hostRoleCommandDAOProvider;

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

    Map<String, Long> lastServiceConfigUpdates = new HashMap<>();

    for (Service service : cluster.getServices().values()) {
      if (service.getMaintenanceState() != MaintenanceState.OFF || !hasAtLeastOneComponentVersionAdvertised(service)) {
        continue;
      }
      ServiceConfigEntity lastServiceConfig = serviceConfigDAO.getLastServiceConfig(clusterId, service.getName());
      lastServiceConfigUpdates.put(service.getName(), lastServiceConfig.getCreateTimestamp());
    }

    List<HostRoleCommandEntity> commands = hostRoleCommandDAO.findAll(REQUEST, null);
    Collections.reverse(commands);

    LinkedHashSet<String> failedServiceNames = new LinkedHashSet<>();
    for (Map.Entry<String, Long> serviceEntry : lastServiceConfigUpdates.entrySet()) {
      String serviceName = serviceEntry.getKey();
      Long configTimestamp = serviceEntry.getValue();
      for (HostRoleCommandEntity command : commands) {
        if (RoleCommand.SERVICE_CHECK.equals(command.getRoleCommand()) && command.getCommandDetail().contains(serviceName)) {
          Long serviceCheckTimestamp = command.getStartTime();
          if (serviceCheckTimestamp < configTimestamp) {
            failedServiceNames.add(serviceName);
            LOG.info("Service {} latest config change is {}, latest service check executed at {}",
                serviceName,
                DATE_FORMAT.format(new Date(configTimestamp)),
                DATE_FORMAT.format(new Date(serviceCheckTimestamp))
            );
          }
        }
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
