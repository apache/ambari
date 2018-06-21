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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.checks.PreUpgradeCheck;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class CheckHelper {
  /**
   * Log.
   */
  private static final Logger LOG = LoggerFactory.getLogger(CheckHelper.class);


  /**
   * Get the list of applicable prechecks. This function exists because it had to be mocked during a test in
   * {@see CheckHelperTest }
   * @param request Pre Check Request
   * @param checksRegistry Registry with all PreChecks that may be applied.
   * @return List of applicable PreChecks.
   */
  public List<PreUpgradeCheck> getApplicablePrerequisiteChecks(PrereqCheckRequest request,
      List<PreUpgradeCheck> checksRegistry) {
    List<PreUpgradeCheck> applicablePreChecks = new LinkedList<>();

    for (PreUpgradeCheck preUpgradeCheck : checksRegistry) {
      try {
        if (preUpgradeCheck.isApplicable(request)) {
          applicablePreChecks.add(preUpgradeCheck);
        }
      } catch (Exception ex) {
        LOG.error(
            "Unable to determine whether the pre-upgrade check {} is applicable to this upgrade",
            preUpgradeCheck.getCheckDescrption().name(), ex);
      }
    }
    return applicablePreChecks;
  }

  /**
   * Executes all registered pre-requisite checks.
   *
   * @param request
   *          pre-requisite check request
   * @return list of pre-requisite check results
   */
  public List<UpgradeCheckResult> performChecks(PrereqCheckRequest request,
      List<PreUpgradeCheck> checksRegistry, Configuration config) {

    final List<UpgradeCheckResult> prerequisiteCheckResults = new ArrayList<>();
    final boolean canBypassPreChecks = config.isUpgradePrecheckBypass();

    List<PreUpgradeCheck> applicablePreChecks = getApplicablePrerequisiteChecks(request, checksRegistry);

    for (PreUpgradeCheck preUpgradeCheck : applicablePreChecks) {
      CheckDescription checkDescription = preUpgradeCheck.getCheckDescrption();
      UpgradeCheckResult result;
      try {
        result = preUpgradeCheck.perform(request);
      } catch (ClusterNotFoundException ex) {
        result = new UpgradeCheckResult(preUpgradeCheck, PrereqCheckStatus.FAIL);
        result.setFailReason("The cluster could not be found.");
      } catch (Exception ex) {
        LOG.error("Check " + checkDescription.name() + " failed", ex);
        result = new UpgradeCheckResult(preUpgradeCheck, PrereqCheckStatus.FAIL);
        result.setFailReason("Unexpected server error happened");
      }

      if (result.getStatus() == PrereqCheckStatus.FAIL && canBypassPreChecks) {
        LOG.error(
            "Check {} failed but stack upgrade is allowed to bypass failures. Error to bypass: {}. Failed on: {}",
            checkDescription.name(), result.getFailReason(),
            StringUtils.join(result.getFailedOn(), ", "));

        result.setStatus(PrereqCheckStatus.BYPASS);
      }

      prerequisiteCheckResults.add(result);
      request.addResult(checkDescription, result.getStatus());
    }

    return prerequisiteCheckResults;
  }
}
