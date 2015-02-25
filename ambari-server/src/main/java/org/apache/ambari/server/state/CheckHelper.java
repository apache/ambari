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
import java.util.List;

import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class CheckHelper {
  /**
   * Log.
   */
  private static Logger LOG = LoggerFactory.getLogger(CheckHelper.class);


  /**
   * Executes all registered pre-requisite checks.
   *
   * @param request pre-requisite check request
   * @return list of pre-requisite check results
   */
  public List<PrerequisiteCheck> performChecks(PrereqCheckRequest request, List<AbstractCheckDescriptor> checksRegistry) {

    final String clusterName = request.getClusterName();
    final List<PrerequisiteCheck> prerequisiteCheckResults = new ArrayList<PrerequisiteCheck>();
    for (AbstractCheckDescriptor checkDescriptor : checksRegistry) {
      final PrerequisiteCheck prerequisiteCheck = new PrerequisiteCheck(
          checkDescriptor.getDescription(), clusterName);
      try {
        if (checkDescriptor.isApplicable(request)) {
          checkDescriptor.perform(prerequisiteCheck, request);
          prerequisiteCheckResults.add(prerequisiteCheck);

          request.addResult(checkDescriptor.getDescription(), prerequisiteCheck.getStatus());
        }
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
