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
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;

import java.util.Set;

/**
 * Checks that the Secondary NameNode is not present on any of the hosts.
 */
public class SecondaryNamenodeDeletedCheck extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public SecondaryNamenodeDeletedCheck() {
    super("SECONDARY_NAMENODE_MUST_BE_DELETED", PrereqCheckType.SERVICE, "The SECONDARY_NAMENODE component must be deleted from all hosts");
  }

  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    return cluster.getService(MasterHostResolver.Service.HDFS.name()) != null;
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();

    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    ServiceComponent serviceComponent = cluster.getService(MasterHostResolver.Service.HDFS.name()).getServiceComponent("SECONDARY_NAMENODE");
    if (serviceComponent !=  null) {
      Set<String> hosts = serviceComponent.getServiceComponentHosts().keySet();

      if (!hosts.isEmpty()) {
        prerequisiteCheck.getFailedOn().add(serviceComponent.getName());
        prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
        prerequisiteCheck.setFailReason("The SECONDARY_NAMENODE component must be deleted from host(s): " + StringUtils.join(hosts, ", ") + ". Please use the REST API to delete it.");
      }
    }
  }
}
