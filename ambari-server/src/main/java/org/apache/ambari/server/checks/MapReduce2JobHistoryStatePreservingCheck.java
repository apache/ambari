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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * The {@link MapReduce2JobHistoryStatePreservingCheck}
 * is used to check that the MR2 History server has state preserving mode enabled.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.CONFIGURATION_WARNING, order = 17.0f)
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
    return super.isApplicable(request, Arrays.asList("MAPREDUCE2"), true);
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
