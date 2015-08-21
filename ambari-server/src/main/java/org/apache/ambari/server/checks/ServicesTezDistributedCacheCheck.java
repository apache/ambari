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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * Checks that Tez jobs reference hadoop libraries from the distributed cache.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.NAMENODE_HA, order = 4.0f)
public class ServicesTezDistributedCacheCheck extends AbstractCheckDescriptor {

  static final String KEY_LIB_URI_MISSING = "tez_lib_uri_missing";
  static final String KEY_USE_HADOOP_LIBS = "tez_use_hadoop_libs";
  static final String KEY_LIB_NOT_DFS = "lib_not_dfs";
  static final String KEY_LIB_NOT_TARGZ = "lib_not_targz";
  static final String KEY_USE_HADOOP_LIBS_FALSE = "tez_use_hadoop_libs_false";

  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    if (!super.isApplicable(request, Arrays.asList("TEZ"), true)) {
      return false;
    }

    PrereqCheckStatus ha = request.getResult(CheckDescription.SERVICES_NAMENODE_HA);
    if (null != ha && ha == PrereqCheckStatus.FAIL) {
      return false;
    }

    return true;
  }

  /**
   * Constructor.
   */
  public ServicesTezDistributedCacheCheck() {
    super(CheckDescription.SERVICES_TEZ_DISTRIBUTED_CACHE);
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
      errorMessages.add(getFailReason(KEY_LIB_URI_MISSING, prerequisiteCheck, request));
    }

    if (useHadoopLibs == null || useHadoopLibs.isEmpty()) {
      errorMessages.add(getFailReason(KEY_USE_HADOOP_LIBS, prerequisiteCheck, request));
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.getFailedOn().add("TEZ");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
      return;
    }

    if (!libUris.matches("^[^:]*dfs:.*") && (defaultFS == null || !defaultFS.matches("^[^:]*dfs:.*"))) {
      errorMessages.add(getFailReason(KEY_LIB_NOT_DFS, prerequisiteCheck, request));
    }

    if (!libUris.contains("tar.gz")) {
      errorMessages.add(getFailReason(KEY_LIB_NOT_TARGZ, prerequisiteCheck, request));
    }

    if (Boolean.parseBoolean(useHadoopLibs)) {
      errorMessages.add(getFailReason(KEY_USE_HADOOP_LIBS_FALSE, prerequisiteCheck, request));
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.getFailedOn().add("TEZ");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
    }
  }
}
