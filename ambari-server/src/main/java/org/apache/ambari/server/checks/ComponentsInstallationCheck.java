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

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.models.HostComponentSummary;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * Checks that service components are installed.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.LIVELINESS,
    order = 2.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class ComponentsInstallationCheck extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public ComponentsInstallationCheck() {
    super(CheckDescription.COMPONENTS_INSTALLATION);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    Set<String> failedServiceNames = new HashSet<String>();

    StackId stackId = cluster.getCurrentStackVersion();

    // Preq-req check should fail if any service component is in INSTALL_FAILED state
    Set<String> installFailedHostComponents = new HashSet<String>();

    for (Map.Entry<String, Service> serviceEntry : cluster.getServices().entrySet()) {
      final Service service = serviceEntry.getValue();
      // Skip service if it is in maintenance mode
      if (service.getMaintenanceState() != MaintenanceState.ON) {
        Map<String, ServiceComponent> serviceComponents = service.getServiceComponents();
        for (Map.Entry<String, ServiceComponent> component : serviceComponents.entrySet()) {
          ServiceComponent serviceComponent = component.getValue();
          if (serviceComponent.isVersionAdvertised()) {
            List<HostComponentSummary> hostComponentSummaries = HostComponentSummary.getHostComponentSummaries(
                service.getName(), serviceComponent.getName());

            for (HostComponentSummary hcs : hostComponentSummaries) {
              // Skip host if it is in maintenance mode
              Host host = clustersProvider.get().getHost(hcs.getHostName());
              if (host.getMaintenanceState(cluster.getClusterId()) != MaintenanceState.ON) {
                if (hcs.getCurrentState() == State.INSTALL_FAILED) {
                  failedServiceNames.add(service.getName());
                  installFailedHostComponents.add(MessageFormat.format(
                      "[{0}:{1} on {2}]", service.getName(), serviceComponent.getName(), hcs.getHostName()));
                }
              }
            }
          }
        }
      }
    }

    if(!installFailedHostComponents.isEmpty()) {
      String message = MessageFormat.format("Service components in INSTALL_FAILED state: {0}.",
          StringUtils.join(installFailedHostComponents, ", "));
      prerequisiteCheck.setFailedOn(new LinkedHashSet<String>(failedServiceNames));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(
          "Found service components in INSTALL_FAILED state. Please re-install these components. " + message);
    }
  }
}
