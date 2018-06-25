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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

import com.google.inject.Singleton;

/**
 * Checks to ensure that services are not in maintenance mode.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.MAINTENANCE_MODE,
    order = 6.0f,
    required = { UpgradeType.ROLLING, UpgradeType.EXPRESS, UpgradeType.HOST_ORDERED })
public class ServicesMaintenanceModeCheck extends ClusterCheck {

  /**
   * Constructor.
   */
  public ServicesMaintenanceModeCheck() {
    super(CheckDescription.SERVICES_MAINTENANCE_MODE);
  }

  @Override
  public UpgradeCheckResult perform(PrereqCheckRequest request) throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    Map<ServiceGroup, Set<String>> serviceGroupsInUpgrade = getServicesInUpgrade(request);

    UpgradeCheckResult result = new UpgradeCheckResult(this);

    for (ServiceGroup serviceGroup : serviceGroupsInUpgrade.keySet()) {
      for( String serviceName : serviceGroupsInUpgrade.get(serviceGroup) ) {  
        final Service service = cluster.getService(serviceName);
        if (!service.isClientOnlyService() && service.getMaintenanceState() == MaintenanceState.ON) {
          result.getFailedOn().add(service.getName());
        }
      }
    }
  
    if (!result.getFailedOn().isEmpty()) {
      result.setStatus(PrereqCheckStatus.FAIL);
      result.setFailReason(getFailReason(result, request));
    }

    return result;
  }
}
