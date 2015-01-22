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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.PrereqCheckType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Checks that all hosts in maintenance state do not have master components.
 */
public class HostsMasterMaintenanceCheck extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public HostsMasterMaintenanceCheck() {
    super("HOSTS_MASTER_MAINTENANCE", PrereqCheckType.HOST, "Hosts in Maintenance Mode must not have any master components");
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final MasterHostResolver masterHostResolver = new MasterHostResolver(configHelperProvider.get(), cluster);
    final Set<String> hostsWithMasterComponent = new HashSet<String>();
    for (Map.Entry<String, Service> serviceEntry: cluster.getServices().entrySet()) {
      final Service service = serviceEntry.getValue();
      for (Map.Entry<String, ServiceComponent> serviceComponentEntry: service.getServiceComponents().entrySet()) {
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
        prerequisiteCheck.getFailedOn().add(host.getHostName());
      }
    }
    if (!prerequisiteCheck.getFailedOn().isEmpty()) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason("Some hosts with master components are in Maintenance Mode");
    }
  }
}
