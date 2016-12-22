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
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * The {@link ClientRetryPropertyCheck} class is used to check that the
 * client retry properties for HIVE and OOZIE are set, but not for HDFS.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.CLIENT_RETRY_PROPERTY,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class ClientRetryPropertyCheck extends AbstractCheckDescriptor {

  static final String HDFS_CLIENT_RETRY_DISABLED_KEY = "hdfs.client.retry.enabled.key";
  static final String HIVE_CLIENT_RETRY_MISSING_KEY = "hive.client.retry.missing.key";
  static final String OOZIE_CLIENT_RETRY_MISSING_KEY = "oozie.client.retry.missing.key";

  /**
   * Constructor.
   */
  public ClientRetryPropertyCheck() {
    super(CheckDescription.CLIENT_RETRY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    return super.isApplicable(request, Arrays.asList("HDFS", "HIVE", "OOZIE"), false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    Map<String, Service> services = cluster.getServices();

    List<String> errorMessages = new ArrayList<String>();

    // HDFS needs to actually prevent client retry since that causes them to try too long and not failover quickly.
    if (services.containsKey("HDFS")) {
      String clientRetryPolicyEnabled = getProperty(request, "hdfs-site", "dfs.client.retry.policy.enabled");
      if (null != clientRetryPolicyEnabled && Boolean.parseBoolean(clientRetryPolicyEnabled)) {
        errorMessages.add(getFailReason(HDFS_CLIENT_RETRY_DISABLED_KEY, prerequisiteCheck, request));
        prerequisiteCheck.getFailedOn().add("HDFS");
      }
    }

    // check hive client properties
    if (services.containsKey("HIVE")) {
      String hiveClientRetryCount = getProperty(request, "hive-site", "hive.metastore.failure.retries");
      if (null != hiveClientRetryCount && Integer.parseInt(hiveClientRetryCount) <= 0) {
        errorMessages.add(getFailReason(HIVE_CLIENT_RETRY_MISSING_KEY, prerequisiteCheck, request));
        prerequisiteCheck.getFailedOn().add("HIVE");
      }
    }

    if (services.containsKey("OOZIE")) {
      String oozieClientRetry = getProperty(request, "oozie-env", "content");
      if (null == oozieClientRetry || !oozieClientRetry.contains("-Doozie.connection.retry.count")) {
        errorMessages.add(getFailReason(OOZIE_CLIENT_RETRY_MISSING_KEY, prerequisiteCheck, request));
        prerequisiteCheck.getFailedOn().add("OOZIE");
      }
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
    }
  }
}
