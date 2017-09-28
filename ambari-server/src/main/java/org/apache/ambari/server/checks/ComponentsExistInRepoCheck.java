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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * The {@link ComponentsExistInRepoCheck} is used to determine if any of the
 * components scheduled for upgrade do not exist in the target repository or
 * stack.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.TOPOLOGY,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class ComponentsExistInRepoCheck extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public ComponentsExistInRepoCheck() {
    super(CheckDescription.COMPONENTS_EXIST_IN_TARGET_REPO);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
      throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    RepositoryVersionEntity repositoryVersion = request.getTargetRepositoryVersion();

    StackId sourceStack = request.getSourceStackId();
    StackId targetStack = repositoryVersion.getStackId();

    Set<String> failedServices = new TreeSet<>();
    Set<String> failedComponents = new TreeSet<>();

    Set<String> servicesInUpgrade = getServicesInUpgrade(request);
    for (String serviceName : servicesInUpgrade) {
      try {
        ServiceInfo serviceInfo = ambariMetaInfo.get().getService(targetStack.getStackName(),
            targetStack.getStackVersion(), serviceName);

        if (serviceInfo.isDeleted() || !serviceInfo.isValid()) {
          failedServices.add(serviceName);
          continue;
        }

        Service service = cluster.getService(serviceName);
        Map<String, ServiceComponent> componentsInUpgrade = service.getServiceComponents();
        for (String componentName : componentsInUpgrade.keySet()) {
          try {
            ComponentInfo componentInfo = ambariMetaInfo.get().getComponent(
                targetStack.getStackName(), targetStack.getStackVersion(), serviceName,
                componentName);

            // if this component isn't included in the upgrade, then skip it
            if (!componentInfo.isVersionAdvertised()) {
              continue;
            }

            if (componentInfo.isDeleted()) {
              failedComponents.add(componentName);
            }

          } catch (StackAccessException stackAccessException) {
            failedComponents.add(componentName);
          }
        }
      } catch (StackAccessException stackAccessException) {
        failedServices.add(serviceName);
      }
    }

    if( failedServices.isEmpty() && failedComponents.isEmpty() ){
      prerequisiteCheck.setStatus(PrereqCheckStatus.PASS);
      return;
    }

    LinkedHashSet<String> failedOn = new LinkedHashSet<>();
    failedOn.addAll(failedServices);
    failedOn.addAll(failedComponents);

    prerequisiteCheck.setFailedOn(failedOn);
    prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);

    String message = "The following {0} exist in {1} but are not included in {2}. They must be removed before upgrading.";
    String messageFragment = "";
    if (!failedServices.isEmpty()) {
      messageFragment = "services";
    }

    if( !failedComponents.isEmpty() ){
      if(!StringUtils.isEmpty(messageFragment)){
        messageFragment += " and ";
      }

      messageFragment += "components";
    }

    message = MessageFormat.format(message, messageFragment, sourceStack, targetStack);
    prerequisiteCheck.setFailReason(message);
  }
}
