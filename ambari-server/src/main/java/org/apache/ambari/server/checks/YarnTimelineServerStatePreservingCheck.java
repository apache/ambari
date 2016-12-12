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
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.BooleanUtils;

import com.google.inject.Singleton;

/**
 * The {@link YarnTimelineServerStatePreservingCheck} is used to check that the
 * YARN Timeline server has state preserving mode enabled. This value is only
 * present in HDP 2.2.4.2+.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT, order = 17.3f)
public class YarnTimelineServerStatePreservingCheck extends AbstractCheckDescriptor {

  private final static String YARN_TIMELINE_STATE_RECOVERY_ENABLED_KEY = "yarn.timeline-service.recovery.enabled";
  private final static String MIN_APPLICABLE_STACK_VERSION_PROPERTY_NAME = "min-applicable-stack-version";

  /**
   * Constructor.
   */
  public YarnTimelineServerStatePreservingCheck() {
    super(CheckDescription.SERVICES_YARN_TIMELINE_ST);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    if (!super.isApplicable(request, Arrays.asList("YARN"), true)) {
      return false;
    }

    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());

    String minApplicableStackVersion = null;
    PrerequisiteCheckConfig prerequisiteCheckConfig = request.getPrerequisiteCheckConfig();
    Map<String, String> checkProperties = null;
    if(prerequisiteCheckConfig != null) {
      checkProperties = prerequisiteCheckConfig.getCheckProperties(this.getClass().getName());
    }
    if(checkProperties != null && checkProperties.containsKey(MIN_APPLICABLE_STACK_VERSION_PROPERTY_NAME)) {
      minApplicableStackVersion = checkProperties.get(MIN_APPLICABLE_STACK_VERSION_PROPERTY_NAME);
    }

    // Due to the introduction of YARN Timeline state recovery only from certain
    // stack-versions onwards, this check is not applicable to earlier versions
    // of the stack.
    // Applicable only if min-applicable-stack-version config property is not defined, or
    // version equals or exceeds the configured version.
    if(minApplicableStackVersion != null && !minApplicableStackVersion.isEmpty()) {
      String[] minStack = minApplicableStackVersion.split("-");
      if(minStack.length == 2) {
        String minStackName = minStack[0];
        String minStackVersion = minStack[1];
        String stackName = cluster.getCurrentStackVersion().getStackName();
        if (minStackName.equals(stackName)) {
          String currentClusterRepositoryVersion = cluster.getCurrentClusterVersion().getRepositoryVersion().getVersion();
          return VersionUtils.compareVersions(currentClusterRepositoryVersion, minStackVersion) >= 0;
        }
      }
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    String propertyValue = getProperty(request, "yarn-site",
        YARN_TIMELINE_STATE_RECOVERY_ENABLED_KEY);

    if (null == propertyValue || !BooleanUtils.toBoolean(propertyValue)) {
      prerequisiteCheck.getFailedOn().add("YARN");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
    }
  }
}
