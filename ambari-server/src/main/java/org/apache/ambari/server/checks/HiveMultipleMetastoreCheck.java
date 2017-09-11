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
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.common.collect.Sets;
import com.google.inject.Singleton;

/**
 * The {@link HiveMultipleMetastoreCheck} checks that there are at least 2 Hive
 * Metastore instances in the cluster.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.MULTIPLE_COMPONENT_WARNING, order = 20.1f)
public class HiveMultipleMetastoreCheck extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public HiveMultipleMetastoreCheck() {
    super(CheckDescription.SERVICES_HIVE_MULTIPLE_METASTORES);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getApplicableServices() {
    return Sets.newHashSet("HIVE");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);

    try {
      Service hive = cluster.getService("HIVE");
      ServiceComponent metastore = hive.getServiceComponent("HIVE_METASTORE");
      Map<String, ServiceComponentHost> metastores = metastore.getServiceComponentHosts();

      if (metastores.size() < 2) {
        prerequisiteCheck.getFailedOn().add("HIVE");
        prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
        prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
      }
    } catch (ServiceComponentNotFoundException scnfe) {
      prerequisiteCheck.getFailedOn().add("HIVE");
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
    }
  }
}
