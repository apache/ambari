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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.inject.Singleton;

/**
 * Check that cluster is kerberized while trying to upgrade Kafka.
 * Will show warning for kerberized cluster with Kafka service and nothing if
 * cluster is not kerberized
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.KERBEROS, order = 1.0f)
public class KafkaKerberosCheck extends AbstractCheckDescriptor {

  private final String KAFKA_SERVICE = "KAFKA";

  /**
   * Constructor.
   */
  public KafkaKerberosCheck() {
    super(CheckDescription.KAFKA_KERBEROS_CHECK);
  }

  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    return super.isApplicable(request, Arrays.asList(KAFKA_SERVICE), true);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);

    if (cluster.getSecurityType() == SecurityType.KERBEROS){
      prerequisiteCheck.getFailedOn().add(KAFKA_SERVICE);
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
    }
  }
}
