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

import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The {@link MapReduce2JobHistoryStatePreservingCheck}
 * is used to check that the MR2 History server has state preserving mode enabled.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.CONFIGURATION_WARNING, order = 1.0f)
public class MapReduce2JobHistoryStatePreservingCheck extends AbstractCheckDescriptor {

  final static String MAPREDUCE2_JOBHISTORY_RECOVERY_ENABLE_KEY =
    "mapreduce.jobhistory.recovery.enable";
  final static String MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_KEY =
    "mapreduce.jobhistory.recovery.store.class";
  final static String MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_LEVELDB_PATH_KEY =
    "mapreduce.jobhistory.recovery.store.leveldb.path";
  final static String YARN_TIMELINE_SERVICE_LEVELDB_STATE_STORE_PATH_KEY =
    "yarn.timeline-service.leveldb-state-store.path";
  /**
   * Due to the introduction of MapReduce2 JobHistory state recovery only from certain
   * stack-versions onwards, this check is not applicable to earlier versions
   * of the stack.
   *
   * This enumeration lists the minimum stack-versions for which this check is applicable.
   * If a stack is not specified in this enumeration, this check will be applicable.
   */
  private enum MinimumApplicableStackVersion {
    HDP_STACK("HDP", "2.3.0.0");

    private String stackName;
    private String stackVersion;

    private MinimumApplicableStackVersion(String stackName, String stackVersion) {
      this.stackName = stackName;
      this.stackVersion = stackVersion;
    }

    public String getStackName() {
      return stackName;
    }

    public String getStackVersion() {
      return stackVersion;
    }
  }

  /**
   * Constructor.
   */
  public MapReduce2JobHistoryStatePreservingCheck() {
    super(CheckDescription.SERVICES_MR2_JOBHISTORY_ST);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    if (!super.isApplicable(request, Arrays.asList("MAPREDUCE2"), true)) {
      return false;
    }

    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());

    // Applicable only if stack not defined in MinimumApplicableStackVersion, or
    // version equals or exceeds the enumerated version.
    for (MinimumApplicableStackVersion minimumStackVersion : MinimumApplicableStackVersion.values()) {
      String stackName = cluster.getCurrentStackVersion().getStackName();
      if (minimumStackVersion.getStackName().equals(stackName)){
        String targetVersion = request.getTargetStackId().getStackVersion();
        String sourceVersion = request.getSourceStackId().getStackVersion();
        return VersionUtils.compareVersions(targetVersion, minimumStackVersion.getStackVersion()) >= 0 &&
               VersionUtils.compareVersions(sourceVersion, minimumStackVersion.getStackVersion()) >= 0;
      }
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    List<String> errorMessages = new ArrayList<String>();
    PrereqCheckStatus checkStatus = PrereqCheckStatus.FAIL;

    String enabled =
      getProperty(request, "mapred-site", MAPREDUCE2_JOBHISTORY_RECOVERY_ENABLE_KEY);
    String storeClass =
      getProperty(request, "mapred-site", MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_KEY);
    String storeLevelDbPath =
      getProperty(request, "mapred-site", MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_LEVELDB_PATH_KEY);

    if (null == enabled || !Boolean.parseBoolean(enabled)) {
      errorMessages.add(getFailReason(MAPREDUCE2_JOBHISTORY_RECOVERY_ENABLE_KEY, prerequisiteCheck, request));
    }

    if (StringUtils.isBlank(storeClass)) {
      errorMessages.add(getFailReason(MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_KEY, prerequisiteCheck,
        request));
    }

    if (StringUtils.isBlank(storeLevelDbPath)) {
      errorMessages.add(getFailReason(MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_LEVELDB_PATH_KEY, prerequisiteCheck,
        request));

    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, "\n"));
      prerequisiteCheck.getFailedOn().add("MAPREDUCE2");
      prerequisiteCheck.setStatus(checkStatus);
    }
  }
}
