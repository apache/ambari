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

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.UpgradePlanDetailEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigMergeHelper;
import org.apache.ambari.server.state.ConfigMergeHelper.ThreeWayValue;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Checks for configuration merge conflicts.
 */
@Singleton
@UpgradeCheck(
    order = 99.0f,
    required = { UpgradeType.ROLLING, UpgradeType.EXPRESS, UpgradeType.HOST_ORDERED })
public class ConfigurationMergeCheck extends ClusterCheck {

  @Inject
  ConfigMergeHelper m_mergeHelper;

  public ConfigurationMergeCheck() {
    super(CheckDescription.CONFIG_MERGE);
  }

  /**
   * The following logic determines if a warning is generated for config merge
   * issues:
   * <ul>
   *   <li>A value that has been customized from HDP 2.2.x.x no longer exists in HDP 2.3.x.x</li>
   *   <li>A value that has been customized from HDP 2.2.x.x has changed its default value between HDP 2.2.x.x and HDP 2.3.x.x</li>
   * </ul>
   */
  @Override
  public UpgradeCheckResult perform(PrereqCheckRequest request)
      throws AmbariException {

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    UpgradeCheckResult result = new UpgradeCheckResult(this);
    Set<String> failedTypes = new HashSet<>();

    for (UpgradePlanDetailEntity upgradeDetail : request.getUpgradePlan().getDetails()) {
      ServiceGroup serviceGroup = cluster.getServiceGroup(upgradeDetail.getServiceGroupId());
      Mpack targetMpack = ambariMetaInfo.get().getMpack(upgradeDetail.getMpackTargetId());

      StackId sourceStack = serviceGroup.getStackId();
      StackId targetStack = targetMpack.getStackId();

      Map<String, Map<String, ThreeWayValue>> changes =
          m_mergeHelper.getConflicts(request.getClusterName(), targetStack);

      for (Entry<String, Map<String, ThreeWayValue>> entry : changes.entrySet()) {
        for (Entry<String, ThreeWayValue> configEntry : entry.getValue().entrySet()) {

          ThreeWayValue twv = configEntry.getValue();
          if (null == twv.oldStackValue) { // !!! did not exist and in the map means changed
            failedTypes.add(entry.getKey());

            result.getFailedOn().add(entry.getKey() + "/" + configEntry.getKey());

            MergeDetail md = new MergeDetail();
            md.type = entry.getKey();
            md.property = configEntry.getKey();
            md.current = twv.savedValue;
            md.new_stack_value = twv.newStackValue;
            md.result_value = md.current;
            result.getFailedDetail().add(md);

          } else if (!twv.oldStackValue.equals(twv.savedValue)) {  // !!! value customized
            if (null == twv.newStackValue || // !!! not in new stack
                !twv.oldStackValue.equals(twv.newStackValue)) { // !!! or the default value changed
              failedTypes.add(entry.getKey());

              result.getFailedOn().add(entry.getKey() + "/" + configEntry.getKey());

              MergeDetail md = new MergeDetail();
              md.type = entry.getKey();
              md.property = configEntry.getKey();
              md.current = twv.savedValue;
              md.new_stack_value = twv.newStackValue;
              md.result_value = md.current;
              result.getFailedDetail().add(md);
            }
          }
        }
      }
    }

    if (result.getFailedOn().size() > 0) {
      result.setStatus(PrereqCheckStatus.WARNING);
      String failReason = getFailReason(result, request);

      result.setFailReason(String.format(failReason, StringUtils.join(failedTypes, ", ")));
    } else {
      result.setStatus(PrereqCheckStatus.PASS);
    }

    return result;
  }

  /**
   * Used to represent specific detail about merge failures.
   */
  public static class MergeDetail {
    public String type = null;
    public String property = null;
    public String current = null;
    public String new_stack_value = null;
    public String result_value = null;
  }

}
