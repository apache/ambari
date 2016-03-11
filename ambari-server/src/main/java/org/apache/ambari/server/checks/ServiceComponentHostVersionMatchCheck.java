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
package org.apache.ambari.server.checks;

import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Warns about host components whose upgrade state is VERSION_MISMATCH. Never triggers
 * fail.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.COMPONENT_VERSION, order = 7.0f, required = true)
public class ServiceComponentHostVersionMatchCheck extends AbstractCheckDescriptor {
  public ServiceComponentHostVersionMatchCheck() {
    super(CheckDescription.VERSION_MISMATCH);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    RepositoryVersionEntity clusterVersion = cluster.getCurrentClusterVersion().getRepositoryVersion();
    List<String> errorMessages = new ArrayList<String>();
    Map<String, Service> services = cluster.getServices();

    // If CURRENT cluster version is already computed
    if (clusterVersion != null) {
      String desiredVersion = clusterVersion.getVersion();
      for (Service service : services.values()) {
        validateService(service, desiredVersion, prerequisiteCheck, errorMessages);
      }
    } else {
      listAllComponentsWithHostVersions(services.values(), prerequisiteCheck, errorMessages);
    }

    if (!prerequisiteCheck.getFailedOn().isEmpty()) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      String failReason = getFailReason(prerequisiteCheck, request);
      prerequisiteCheck.setFailReason(String.format(failReason, StringUtils.join(errorMessages, "\n")));
    }
  }

  /**
   * Iterates over all service components belonging to a service and validates them.
   * @param service
   * @param desiredVersion current version for cluster. All service component versions
   *                       should match it
   * @param prerequisiteCheck
   * @param errorMessages
   */
  private void validateService(Service service,
                               String desiredVersion,
                               PrerequisiteCheck prerequisiteCheck,
                               List<String> errorMessages) {
    Map<String, ServiceComponent> serviceComponents = service.getServiceComponents();
    for (ServiceComponent serviceComponent : serviceComponents.values()) {
      validateServiceComponent(serviceComponent, desiredVersion, prerequisiteCheck, errorMessages);
    }
  }

  /**
   * Iterates over all host components belonging to a service component and validates them.
   * @param serviceComponent
   * @param desiredVersion current version for cluster. All host component versions
   *                       should match it
   * @param prerequisiteCheck
   * @param errorMessages
   */
  private void validateServiceComponent(ServiceComponent serviceComponent,
                                        String desiredVersion,
                                        PrerequisiteCheck prerequisiteCheck,
                                        List<String> errorMessages) {
    if (!serviceComponent.isVersionAdvertised())
      return;

    Map<String, ServiceComponentHost> serviceComponentHosts = serviceComponent.getServiceComponentHosts();
    for (ServiceComponentHost serviceComponentHost : serviceComponentHosts.values()) {
      validateServiceComponentHost(serviceComponentHost, desiredVersion, prerequisiteCheck, errorMessages);
    }
  }

  /**
   * Validates host component. If upgrade state of host component is VERSION_MISMATCH,
   * adds hostname to a Failed On map of prerequisite check, and adds all other
   * host component version details to errorMessages
   * @param serviceComponentHost
   * @param desiredVersion current version for cluster. Component host version
   *                       should match it
   * @param prerequisiteCheck
   * @param errorMessages
   */
  private void validateServiceComponentHost(ServiceComponentHost serviceComponentHost,
                                            String desiredVersion,
                                            PrerequisiteCheck prerequisiteCheck,
                                            List<String> errorMessages) {
    String actualVersion = serviceComponentHost.getVersion();
    if (StringUtils.equals(desiredVersion, actualVersion))
      return;

    String hostName = serviceComponentHost.getHostName();
    String serviceComponentName = serviceComponentHost.getServiceComponentName();

    String message = hostName + "/" + serviceComponentName
        + " desired version: " + desiredVersion
        + ", actual version: " + actualVersion;
    prerequisiteCheck.getFailedOn().add(hostName);
    errorMessages.add(message);
  }

  /**
   * Validates component version in a cluster that has no CURRENT version defined.
   * If there is more than one actual host component version in cluster, collects
   * details for warning.
   * @param services
   * @param prerequisiteCheck
   * @param errorMessages
   */
  private void listAllComponentsWithHostVersions(Collection<Service> services,
                                                 PrerequisiteCheck prerequisiteCheck,
                                                 List<String> errorMessages) {
    if (countDistinctVersionsOnHosts(services) <= 1)
      return;

    for (Service service : services) {
      for (ServiceComponent serviceComponent : service.getServiceComponents().values()) {
        if (!serviceComponent.isVersionAdvertised())
          continue;

        ArrayList<Object> hostVersions = new ArrayList<>();
        for (ServiceComponentHost serviceComponentHost : serviceComponent.getServiceComponentHosts().values()) {
          hostVersions.add(serviceComponentHost.getVersion());
          prerequisiteCheck.getFailedOn().add(serviceComponentHost.getHostName());
        }
        String message = serviceComponent.getName() + " host versions: " + hostVersions;
        errorMessages.add(message);
      }
    }
  }

  /**
   * Iterates over services, and enumerates reported versions of host components.
   * @param services collection of services
   * @return number of distinct actual versions found
   */
  private int countDistinctVersionsOnHosts(Collection<Service> services) {
    HashSet<Object> versions = new HashSet<>();
    for (Service service : services) {
      for (ServiceComponent serviceComponent : service.getServiceComponents().values()) {
        if (!serviceComponent.isVersionAdvertised())
          continue;
        for (ServiceComponentHost serviceComponentHost : serviceComponent.getServiceComponentHosts().values()) {
          versions.add(serviceComponentHost.getVersion());
        }
      }
    }
    return versions.size();
  }
}
