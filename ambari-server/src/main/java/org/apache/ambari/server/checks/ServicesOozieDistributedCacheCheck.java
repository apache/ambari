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
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Checks that Oozie jobs reference hadoop libraries from the distributed cache.
 */
public class ServicesOozieDistributedCacheCheck extends AbstractCheckDescriptor {

  @Inject
  Provider<ServicesMapReduceDistributedCacheCheck> mapReduceCheck;

  @Override
  public boolean isApplicable(PrereqCheckRequest request)
    throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    try {
      cluster.getService("OOZIE");
    } catch (ServiceNotFoundException ex) {
      return false;
    }
    return true;
  }

  /**
   * Constructor.
   */
  public ServicesOozieDistributedCacheCheck() {
    super("SERVICES_OOZIE_DISTRIBUTED_CACHE", PrereqCheckType.SERVICE, "Oozie should reference hadoop libraries from the distributed cache");
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    // since Oozie does talk to multiple clusters, all clusters need to have MapReduce configured for Distributed Cache
    for (String clusterName: clustersProvider.get().getClusters().keySet()) {
      final PrereqCheckRequest clusterRequest = new PrereqCheckRequest(clusterName);
      mapReduceCheck.get().perform(prerequisiteCheck, clusterRequest);
      if (prerequisiteCheck.getStatus() == PrereqCheckStatus.FAIL) {
        prerequisiteCheck.getFailedOn().clear();
        prerequisiteCheck.getFailedOn().add("OOZIE");
        prerequisiteCheck.setFailReason("MapReduce on cluster " + clusterName + " should reference hadoop libraries from the distributed cache. "
            + "Make sure that mapreduce.application.framework.path and mapreduce.application.classpath properties are present in mapred.site.xml"
            + "and point to hdfs:/... urls");
        break;
      }
    }
  }
}