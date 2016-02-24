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
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;


class DescriptorPreCheck {

  public AbstractCheckDescriptor descriptor;
  public PrerequisiteCheck check;

  public DescriptorPreCheck(AbstractCheckDescriptor descriptor, PrerequisiteCheck check) {
    this.descriptor = descriptor;
    this.check = check;
  }
}

@Singleton
public class CheckHelper {
  /**
   * Log.
   */
  private static Logger LOG = LoggerFactory.getLogger(CheckHelper.class);


  /**
   * Get the list of applicable prechecks. This function exists because it had to be mocked during a test in
   * {@see CheckHelperTest }
   * @param request Pre Check Request
   * @param checksRegistry Registry with all PreChecks that may be applied.
   * @return List of applicable PreChecks.
   */
  public List<DescriptorPreCheck> getApplicablePrerequisiteChecks(PrereqCheckRequest request,
                                                       List<AbstractCheckDescriptor> checksRegistry) {
    List<DescriptorPreCheck> applicablePreChecks = new LinkedList<>();

    final String clusterName = request.getClusterName();
    for (AbstractCheckDescriptor checkDescriptor : checksRegistry) {
      final PrerequisiteCheck prerequisiteCheck = new PrerequisiteCheck(checkDescriptor.getDescription(), clusterName);

      try {
        if (checkDescriptor.isApplicable(request)) {
          applicablePreChecks.add(new DescriptorPreCheck(checkDescriptor, prerequisiteCheck));
        }
      } catch (Exception ex) {
        LOG.error("Check " + checkDescriptor.getDescription().name() + " failed", ex);
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
  public List<PrerequisiteCheck> performChecks(PrereqCheckRequest request,
      List<AbstractCheckDescriptor> checksRegistry) {

    final String clusterName = request.getClusterName();
    final List<PrerequisiteCheck> prerequisiteCheckResults = new ArrayList<PrerequisiteCheck>();

    List<DescriptorPreCheck> applicablePreChecks = getApplicablePrerequisiteChecks(request, checksRegistry);
    for (DescriptorPreCheck descriptorPreCheck : applicablePreChecks) {
      AbstractCheckDescriptor checkDescriptor = descriptorPreCheck.descriptor;
      PrerequisiteCheck prerequisiteCheck = descriptorPreCheck.check;
      try {
        checkDescriptor.perform(prerequisiteCheck, request);

        boolean canBypassPreChecks = checkDescriptor.isStackUpgradeAllowedToBypassPreChecks();

        if (prerequisiteCheck.getStatus() == PrereqCheckStatus.FAIL && canBypassPreChecks) {
          LOG.error("Check {} failed but stack upgrade is allowed to bypass failures. Error to bypass: {}. Failed on: {}",
              checkDescriptor.getDescription().name(),
              prerequisiteCheck.getFailReason(),
              StringUtils.join(prerequisiteCheck.getFailedOn(), ", "));
          prerequisiteCheck.setStatus(PrereqCheckStatus.BYPASS);
        }
        prerequisiteCheckResults.add(prerequisiteCheck);

        request.addResult(checkDescriptor.getDescription(), prerequisiteCheck.getStatus());
      } catch (ClusterNotFoundException ex) {
        prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
        prerequisiteCheck.setFailReason("Cluster with name " + clusterName + " doesn't exists");
        prerequisiteCheckResults.add(prerequisiteCheck);

        request.addResult(checkDescriptor.getDescription(), prerequisiteCheck.getStatus());
      } catch (Exception ex) {
        LOG.error("Check " + checkDescriptor.getDescription().name() + " failed", ex);
        prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
        prerequisiteCheck.setFailReason("Unexpected server error happened");
        prerequisiteCheckResults.add(prerequisiteCheck);

        request.addResult(checkDescriptor.getDescription(), prerequisiteCheck.getStatus());
      }
    }

    return prerequisiteCheckResults;
  }
}
