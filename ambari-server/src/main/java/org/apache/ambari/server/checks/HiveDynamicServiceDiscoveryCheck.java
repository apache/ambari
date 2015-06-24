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
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * The {@link HiveDynamicServiceDiscoveryCheck} class is used to check that HIVE
 * is properly configured for dynamic discovery.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT, order = 1.0f)
public class HiveDynamicServiceDiscoveryCheck extends AbstractCheckDescriptor {

  static final String HIVE_DYNAMIC_SERVICE_DISCOVERY_ENABLED_KEY = "hive.dynamic-service.discovery.enabled.key";
  static final String HIVE_DYNAMIC_SERVICE_ZK_QUORUM_KEY = "hive.dynamic-service.discovery.zk-quorum.key";
  static final String HIVE_DYNAMIC_SERVICE_ZK_NAMESPACE_KEY = "hive.dynamic-service.zk-namespace.key";

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
    if (!super.isApplicable(request)) {
      return false;
    }

    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    Map<String, Service> services = cluster.getServices();
    if (services.containsKey("HIVE")) {
      return true;
    }

    return false;
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

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
      prerequisiteCheck.getFailedOn().add("HIVE");
      PrereqCheckStatus checkStatus = PrereqCheckStatus.FAIL;
      if ("HDP".equals(request.getSourceStackId().getStackName())) {
        if (VersionUtils.compareVersions(request.getSourceStackId().getStackVersion(), "2.3.0.0") < 0
            && VersionUtils.compareVersions(request.getTargetStackId().getStackVersion(), "2.3.0.0") < 0
            && VersionUtils.compareVersions(request.getSourceStackId().getStackVersion(), request.getTargetStackId().getStackVersion()) < 0) {
          checkStatus = PrereqCheckStatus.WARNING;
        }
      }
      prerequisiteCheck.setStatus(checkStatus);
    }
  }
}
