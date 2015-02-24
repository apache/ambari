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
 * Checks that MR jobs reference hadoop libraries from the distributed cache.
 */
public class ServicesMapReduceDistributedCacheCheck extends AbstractCheckDescriptor {

  @Override
  public boolean isApplicable(PrereqCheckRequest request)
    throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    try {
      cluster.getService("YARN");
    } catch (ServiceNotFoundException ex) {
      return false;
    }
    return true;
  }

  /**
   * Constructor.
   */
  public ServicesMapReduceDistributedCacheCheck() {
    super("SERVICES_MR_DISTRIBUTED_CACHE", PrereqCheckType.SERVICE, "MapReduce should reference hadoop libraries from the distributed cache");
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final String mrConfigType = "mapred-site";
    final String coreSiteConfigType = "core-site";
    final Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    final DesiredConfig mrDesiredConfig = desiredConfigs.get(mrConfigType);
    final DesiredConfig coreSiteDesiredConfig = desiredConfigs.get(coreSiteConfigType);
    final Config mrConfig = cluster.getConfig(mrConfigType, mrDesiredConfig.getTag());
    final Config coreSiteConfig = cluster.getConfig(coreSiteConfigType, coreSiteDesiredConfig.getTag());
    final String applicationClasspath = mrConfig.getProperties().get("mapreduce.application.classpath");
    final String frameworkPath = mrConfig.getProperties().get("mapreduce.application.framework.path");
    final String defaultFS = coreSiteConfig.getProperties().get("fs.defaultFS");

    List<String> errorMessages = new ArrayList<String>();
    if (applicationClasspath == null || applicationClasspath.isEmpty()) {
      errorMessages.add("Property mapreduce.application.classpath is missing from mapred-site, please add it.");
    }

    if (frameworkPath == null || frameworkPath.isEmpty()) {
      errorMessages.add("Property mapreduce.application.framework.path is missing from mapred-site, please add it.");
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.getFailedOn().add("MAP_REDUCE");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
      return;
    }

    if (!frameworkPath.matches("^[^:]*dfs:.*") && (defaultFS == null || !defaultFS.matches("^[^:]*dfs:.*"))) {
      prerequisiteCheck.getFailedOn().add("MAP_REDUCE");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason("MapReduce should reference hadoop libraries from the distributed cache. Please make sure that either mapred-site's mapreduce.application.framework.path or core-site's fs.defaultFS begins with *dfs:");
    }
  }
}