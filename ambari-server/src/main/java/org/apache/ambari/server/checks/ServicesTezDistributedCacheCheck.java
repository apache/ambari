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
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks that Tez jobs reference hadoop libraries from the distributed cache.
 */
public class ServicesTezDistributedCacheCheck extends AbstractCheckDescriptor {

  @Override
  public boolean isApplicable(PrereqCheckRequest request)
    throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    try {
      cluster.getService("TEZ");
    } catch (ServiceNotFoundException ex) {
      return false;
    }
    return true;
  }

  /**
   * Constructor.
   */
  public ServicesTezDistributedCacheCheck() {
    super("SERVICES_TEZ_DISTRIBUTED_CACHE", PrereqCheckType.SERVICE, "TEZ should reference hadoop libraries from the distributed cache");
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final String tezConfigType = "tez-site";
    final String coreSiteConfigType = "core-site";
    final Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    final DesiredConfig tezDesiredConfig = desiredConfigs.get(tezConfigType);
    final Config tezConfig = cluster.getConfig(tezConfigType, tezDesiredConfig.getTag());
    final DesiredConfig coreSiteDesiredConfig = desiredConfigs.get(coreSiteConfigType);
    final Config coreSiteConfig = cluster.getConfig(coreSiteConfigType, coreSiteDesiredConfig.getTag());
    final String libUris = tezConfig.getProperties().get("tez.lib.uris");
    final String useHadoopLibs = tezConfig.getProperties().get("tez.use.cluster.hadoop-libs");
    final String defaultFS = coreSiteConfig.getProperties().get("fs.defaultFS");

    List<String> errorMessages = new ArrayList<String>();
    if (libUris == null || libUris.isEmpty()) {
      errorMessages.add("Property tez.lib.uris is missing from tez-site, please add it.");
    }

    if (useHadoopLibs == null || useHadoopLibs.isEmpty()) {
      errorMessages.add("Property tez.use.cluster.hadoop-libs is missing from tez-site, please add it.");
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.getFailedOn().add("TEZ");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
      return;
    }

    if (!libUris.matches("^[^:]*dfs:.*") && (defaultFS == null || !defaultFS.matches("^[^:]*dfs:.*"))) {
      errorMessages.add("Property tez.lib.uris in tez-site should use a distributed file system. Please make sure that either tez-site's tez.lib.uris or core-site's fs.defaultFS begins with *dfs:");
    }
    if (!libUris.contains("tar.gz")) {
      errorMessages.add("Property tez.lib.uris in tez-site should end in tar.gz");
    }
    if (Boolean.parseBoolean(useHadoopLibs)) {
      errorMessages.add("Property tez.use.cluster.hadoop-libs in tez-site should be set to false");
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.getFailedOn().add("TEZ");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
    }
  }
}