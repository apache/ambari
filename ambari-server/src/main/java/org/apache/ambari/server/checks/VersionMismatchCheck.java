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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * Warns about host components whose upgrade state is VERSION_MISMATCH. Never triggers
 * fail. In failure description, lists actual and expected component versions.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.COMPONENT_VERSION,
    order = 7.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class VersionMismatchCheck extends AbstractCheckDescriptor {

  public VersionMismatchCheck() {
    super(CheckDescription.VERSION_MISMATCH);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    Map<String, Service> services = cluster.getServices();
    List<String> errorMessages = new ArrayList<>();
    for (Service service : services.values()) {
      validateService(service, prerequisiteCheck, errorMessages);
    }

    if (!prerequisiteCheck.getFailedOn().isEmpty()) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      String failReason = getFailReason(prerequisiteCheck, request);
      prerequisiteCheck.setFailReason(String.format(failReason, StringUtils.join(errorMessages, "\n")));
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, "\n"));
    }
  }

  /**
   * Iterates over all service components belonging to a service and validates them.
   * @param service
   * @param prerequisiteCheck
   * @param errorMessages
   * @throws AmbariException
   */
  private void validateService(Service service, PrerequisiteCheck prerequisiteCheck,
                               List<String> errorMessages) throws AmbariException {
    Map<String, ServiceComponent> serviceComponents = service.getServiceComponents();
    for (ServiceComponent serviceComponent : serviceComponents.values()) {
      validateServiceComponent(serviceComponent, prerequisiteCheck, errorMessages);
    }
  }

  /**
   * Iterates over all host components belonging to a service component and validates them.
   * @param serviceComponent
   * @param prerequisiteCheck
   * @param errorMessages
   */
  private void validateServiceComponent(ServiceComponent serviceComponent,
                                        PrerequisiteCheck prerequisiteCheck, List<String> errorMessages) {
    Map<String, ServiceComponentHost> serviceComponentHosts = serviceComponent.getServiceComponentHosts();
    for (ServiceComponentHost serviceComponentHost : serviceComponentHosts.values()) {
      validateServiceComponentHost(serviceComponent, serviceComponentHost,
        prerequisiteCheck, errorMessages);
    }
  }

  /**
   * Validates host component. If upgrade state of host component is VERSION_MISMATCH,
   * adds hostname to a Failed On map of prerequisite check, and adds all other
   * host component version details to errorMessages
   * @param serviceComponent
   * @param serviceComponentHost
   * @param prerequisiteCheck
   * @param errorMessages
   */
  private void validateServiceComponentHost(ServiceComponent serviceComponent,
                                            ServiceComponentHost serviceComponentHost,
                                            PrerequisiteCheck prerequisiteCheck,
                                            List<String> errorMessages) {
    if (serviceComponentHost.getUpgradeState().equals(UpgradeState.VERSION_MISMATCH)) {
      String hostName = serviceComponentHost.getHostName();
      String serviceComponentName = serviceComponentHost.getServiceComponentName();
      String desiredVersion = serviceComponent.getDesiredVersion();
      String actualVersion = serviceComponentHost.getVersion();

      String message = hostName + "/" + serviceComponentName
          + " desired version: " + desiredVersion
          + ", actual version: " + actualVersion;
      prerequisiteCheck.getFailedOn().add(hostName);
      errorMessages.add(message);
    }
  }
}
