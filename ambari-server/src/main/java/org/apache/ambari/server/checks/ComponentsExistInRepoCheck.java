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

import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.UpgradePlanDetailEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentSupport;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeHelper;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link ComponentsExistInRepoCheck} is used to determine if any of the
 * components scheduled for upgrade do not exist in the target repository or
 * stack.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.INFORMATIONAL_WARNING,
    required = { UpgradeType.ROLLING, UpgradeType.EXPRESS, UpgradeType.HOST_ORDERED })
public class ComponentsExistInRepoCheck extends ClusterCheck {
  public static final String AUTO_REMOVE = "auto_remove";
  public static final String MANUAL_REMOVE = "manual_remove";
  @Inject
  ServiceComponentSupport serviceComponentSupport;
  @Inject
  UpgradeHelper upgradeHelper;

  public ComponentsExistInRepoCheck() {
    super(CheckDescription.COMPONENTS_EXIST_IN_TARGET_REPO);
  }

  @Override
  public UpgradeCheckResult perform(PrereqCheckRequest request)
      throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);

    UpgradeCheckResult result = new UpgradeCheckResult(this);

    for (UpgradePlanDetailEntity upgradeDetail : request.getUpgradePlan().getDetails()) {
      ServiceGroup serviceGroup = cluster.getServiceGroup(upgradeDetail.getServiceGroupId());
      Mpack targetMpack = ambariMetaInfo.get().getMpack(upgradeDetail.getMpackTargetId());

      StackId sourceStack = serviceGroup.getStackId();
      StackId targetStack = targetMpack.getStackId();

      Set<ServiceDetail> failedServices = new TreeSet<>();
      Set<ServiceComponentDetail> failedComponents = new TreeSet<>();

      for (Service service : serviceGroup.getServices()) {
        String serviceName = service.getName();
        String serviceType = service.getServiceType();

        try {
          ServiceInfo serviceInfo = ambariMetaInfo.get().getService(targetStack.getStackName(),
              targetStack.getStackVersion(), serviceType);

          if (serviceInfo.isDeleted() || !serviceInfo.isValid()) {
            failedServices.add(new ServiceDetail(serviceName));
            continue;
          }

          Map<String, ServiceComponent> componentsInUpgrade = service.getServiceComponents();
          for (String componentName : componentsInUpgrade.keySet()) {
            try {
              ComponentInfo componentInfo = ambariMetaInfo.get().getComponent(
                  targetStack.getStackName(), targetStack.getStackVersion(), serviceType,
                  componentName);

              // if this component isn't included in the upgrade, then skip it
              if (!componentInfo.isVersionAdvertised()) {
                continue;
              }

              if (componentInfo.isDeleted()) {
                failedComponents.add(new ServiceComponentDetail(serviceName, componentName));
              }

            } catch (StackAccessException stackAccessException) {
              failedComponents.add(new ServiceComponentDetail(serviceName, componentName));
            }
          }
        } catch (StackAccessException stackAccessException) {
          failedServices.add(new ServiceDetail(serviceName));
        }
      }

      if (failedServices.isEmpty() && failedComponents.isEmpty()) {
        continue;
      }

      Set<String> failedServiceNames = failedServices.stream().map(
          failureDetail -> failureDetail.serviceName).collect(
              Collectors.toCollection(LinkedHashSet::new));

      Set<String> failedComponentNames = failedComponents.stream().map(
          failureDetail -> failureDetail.componentName).collect(
              Collectors.toCollection(LinkedHashSet::new));

      LinkedHashSet<String> failures = new LinkedHashSet<>();
      failures.addAll(failedServiceNames);
      failures.addAll(failedComponentNames);

      result.setFailedOn(failures);
      result.setStatus(PrereqCheckStatus.FAIL);

      result.getFailedDetail().addAll(failedServices);
      result.getFailedDetail().addAll(failedComponents);

      String message = "In the {0} service group the following {1} exist in {2} but are not included in {3}. They must be removed before upgrading.";
      String messageFragment = "";
      if (!failedServices.isEmpty()) {
        messageFragment = "services";
      }

      if (!failedComponents.isEmpty()) {
        if (!StringUtils.isEmpty(messageFragment)) {
          messageFragment += " and ";
        }

        messageFragment += "components";
      }

      message = MessageFormat.format(message, serviceGroup.getServiceGroupName(), messageFragment,
          sourceStack, targetStack);

      result.setFailReason(message);
      return result;
    }

    return result;
  }
}
