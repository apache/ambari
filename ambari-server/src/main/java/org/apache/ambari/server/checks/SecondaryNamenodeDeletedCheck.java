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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Checks that the Secondary NameNode is not present on any of the hosts.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.NAMENODE_HA, order = 16.0f)
public class SecondaryNamenodeDeletedCheck extends AbstractCheckDescriptor {
  private static final String HDFS_SERVICE_NAME = MasterHostResolver.Service.HDFS.name();

  @Inject
  HostComponentStateDAO hostComponentStateDao;
  /**
   * Constructor.
   */
  public SecondaryNamenodeDeletedCheck() {
    super(CheckDescription.SECONDARY_NAMENODE_MUST_BE_DELETED);
  }

  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    if (!super.isApplicable(request, Arrays.asList(HDFS_SERVICE_NAME), true)) {
      return false;
    }

    PrereqCheckStatus ha = request.getResult(CheckDescription.SERVICES_NAMENODE_HA);
    if (null != ha && ha == PrereqCheckStatus.FAIL) {
      return false;
    }

    return true;
  }

  // TODO AMBARI-12698, there are 2 ways to filter the prechecks.
  // 1. Explictly mention them in each upgrade pack, which is more flexible, but requires adding the name of checks
  //   to perform in each upgrade pack.
  // 2. Make each upgrade check class call a function before perform() that will determine if the check is appropriate
  //   given the type of upgrade. The PrereqCheckRequest object has a field for the type of upgrade.
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    Set<String> hosts = new HashSet<String>();
    final String SECONDARY_NAMENODE = "SECONDARY_NAMENODE";

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    try {
      ServiceComponent serviceComponent = cluster.getService(HDFS_SERVICE_NAME).getServiceComponent(SECONDARY_NAMENODE);
      if (serviceComponent != null) {
        hosts = serviceComponent.getServiceComponentHosts().keySet();
      }
    } catch (ServiceComponentNotFoundException err) {
      // This exception can be ignored if the component doesn't exist because it is a best-attempt at finding it.
      ;
    }

    // Try another method to find references to SECONDARY_NAMENODE
    if (hosts.isEmpty()) {
      List<HostComponentStateEntity> allHostComponents = hostComponentStateDao.findAll();
      for(HostComponentStateEntity hc : allHostComponents) {
        if (hc.getServiceName().equalsIgnoreCase(HDFS_SERVICE_NAME) && hc.getComponentName().equalsIgnoreCase(SECONDARY_NAMENODE)) {
          hosts.add(hc.getHostName());
        }
      }
    }

    if (!hosts.isEmpty()) {
      String foundHost = hosts.toArray(new String[hosts.size()])[0];
      prerequisiteCheck.getFailedOn().add(HDFS_SERVICE_NAME);
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      String failReason = getFailReason(prerequisiteCheck, request);
      prerequisiteCheck.setFailReason(String.format(failReason, foundHost));
    }
  }
}
