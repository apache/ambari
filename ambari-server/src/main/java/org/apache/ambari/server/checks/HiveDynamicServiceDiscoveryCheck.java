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
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * The {@link HiveDynamicServiceDiscoveryCheck} class is used to check that HIVE
 * is properly configured for dynamic discovery.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT, order = 20.0f, required = true)
public class HiveDynamicServiceDiscoveryCheck extends AbstractCheckDescriptor {

  static final String HIVE_DYNAMIC_SERVICE_DISCOVERY_ENABLED_KEY = "hive.dynamic-service.discovery.enabled.key";
  static final String HIVE_DYNAMIC_SERVICE_ZK_QUORUM_KEY = "hive.dynamic-service.discovery.zk-quorum.key";
  static final String HIVE_DYNAMIC_SERVICE_ZK_NAMESPACE_KEY = "hive.dynamic-service.zk-namespace.key";
  static final String MIN_FAILURE_STACK_VERSION_PROPERTY_NAME = "min-failure-stack-version";

  /**
   * Constructor.
   */
  public HiveDynamicServiceDiscoveryCheck() {
    super(CheckDescription.SERVICES_HIVE_DYNAMIC_SERVICE_DISCOVERY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    return super.isApplicable(request, Arrays.asList("HIVE"), true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    List<String> errorMessages = new ArrayList<String>();

    String dynamicServiceDiscoveryEnabled = getProperty(request, "hive-site", "hive.server2.support.dynamic.service.discovery");
    String zookeeperQuorum = getProperty(request, "hive-site", "hive.zookeeper.quorum");
    String zookeeperNamespace = getProperty(request, "hive-site", "hive.server2.zookeeper.namespace");

    if (null == dynamicServiceDiscoveryEnabled || !Boolean.parseBoolean(dynamicServiceDiscoveryEnabled)) {
      errorMessages.add(getFailReason(HIVE_DYNAMIC_SERVICE_DISCOVERY_ENABLED_KEY, prerequisiteCheck, request));
    }

    if (StringUtils.isBlank(zookeeperQuorum)) {
      errorMessages.add(getFailReason(HIVE_DYNAMIC_SERVICE_ZK_QUORUM_KEY, prerequisiteCheck,
          request));
    }

    if (StringUtils.isBlank(zookeeperNamespace)) {
      errorMessages.add(getFailReason(HIVE_DYNAMIC_SERVICE_ZK_NAMESPACE_KEY, prerequisiteCheck,
          request));
    }

    String minFailureStackVersion = null;
    PrerequisiteCheckConfig prerequisiteCheckConfig = request.getPrerequisiteCheckConfig();
    Map<String, String> checkProperties = null;
    if(prerequisiteCheckConfig != null) {
      checkProperties = prerequisiteCheckConfig.getCheckProperties(this.getClass().getName());
    }
    if(checkProperties != null && checkProperties.containsKey(MIN_FAILURE_STACK_VERSION_PROPERTY_NAME)) {
      minFailureStackVersion = checkProperties.get(MIN_FAILURE_STACK_VERSION_PROPERTY_NAME);
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
      prerequisiteCheck.getFailedOn().add("HIVE");
      PrereqCheckStatus checkStatus = PrereqCheckStatus.FAIL;
      if(minFailureStackVersion != null && !minFailureStackVersion.isEmpty()) {
        String[] minStack = minFailureStackVersion.split("-");
        if (minStack.length == 2) {
          String minStackName = minStack[0];
          String minStackVersion = minStack[1];
          if (minStackName.equals(request.getSourceStackId().getStackName())) {
            if (VersionUtils.compareVersions(request.getSourceStackId().getStackVersion(), minStackVersion) < 0
                && VersionUtils.compareVersions(request.getTargetStackId().getStackVersion(), minStackVersion) < 0
                && VersionUtils.compareVersions(request.getSourceStackId().getStackVersion(), request.getTargetStackId().getStackVersion()) < 0) {
              checkStatus = PrereqCheckStatus.WARNING;
            }
          }
        }
      }
      prerequisiteCheck.setStatus(checkStatus);
    }
  }
}
