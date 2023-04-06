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
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.collect.Sets;
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

  private static final String HDFS_CLIENT_RETRY_PROPERTY = "dfs.client.retry.policy.enabled";
  private static final String HIVE_CLIENT_RETRY_PROPERTY = "hive.metastore.failure.retries";
  private static final String OOZIE_CLIENT_RETRY_PROPERTY = "-Doozie.connection.retry.count";

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
  public Set<String> getApplicableServices() {
    return Sets.newHashSet("HDFS", "HIVE", "OOZIE");
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    Map<String, Service> services = cluster.getServices();

    List<String> errorMessages = new ArrayList<>();

    // HDFS needs to actually prevent client retry since that causes them to try too long and not failover quickly.
    if (services.containsKey("HDFS")) {
      String clientRetryPolicyEnabled = getProperty(request, "hdfs-site", HDFS_CLIENT_RETRY_PROPERTY);
      if (null != clientRetryPolicyEnabled && Boolean.parseBoolean(clientRetryPolicyEnabled)) {
        MissingClientRetryProperty missingProperty = new MissingClientRetryProperty("HDFS",
            "hdfs-site", HDFS_CLIENT_RETRY_PROPERTY);

        prerequisiteCheck.getFailedDetail().add(missingProperty);

        errorMessages.add(getFailReason(HDFS_CLIENT_RETRY_DISABLED_KEY, prerequisiteCheck, request));
        prerequisiteCheck.getFailedOn().add("HDFS");
      }
    }

    // check hive client properties
    if (services.containsKey("HIVE")) {
      String hiveClientRetryCount = getProperty(request, "hive-site", HIVE_CLIENT_RETRY_PROPERTY);
      if (null != hiveClientRetryCount && Integer.parseInt(hiveClientRetryCount) <= 0) {
        MissingClientRetryProperty missingProperty = new MissingClientRetryProperty("HIVE",
            "hive-site", HIVE_CLIENT_RETRY_PROPERTY);

        prerequisiteCheck.getFailedDetail().add(missingProperty);

        errorMessages.add(getFailReason(HIVE_CLIENT_RETRY_MISSING_KEY, prerequisiteCheck, request));
        prerequisiteCheck.getFailedOn().add("HIVE");
      }
    }

    if (services.containsKey("OOZIE")) {
      String oozieClientRetry = getProperty(request, "oozie-env", "content");
      if (null == oozieClientRetry || !oozieClientRetry.contains(OOZIE_CLIENT_RETRY_PROPERTY)) {
        MissingClientRetryProperty missingProperty = new MissingClientRetryProperty("OOZIE",
            "oozie-env", OOZIE_CLIENT_RETRY_PROPERTY);

        prerequisiteCheck.getFailedDetail().add(missingProperty);

        errorMessages.add(getFailReason(OOZIE_CLIENT_RETRY_MISSING_KEY, prerequisiteCheck, request));
        prerequisiteCheck.getFailedOn().add("OOZIE");
      }
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.setFailReason(StringUtils.join(errorMessages, " "));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
    }
  }

  /**
   * Used to represent a missing retry property.
   */
  private static class MissingClientRetryProperty {
    @JsonProperty("service_name")
    public String serviceName;

    @JsonProperty("type")
    public String propertyType;

    @JsonProperty("property_name")
    public String propertyName;

    MissingClientRetryProperty(String serviceName, String propertyType, String propertyName) {
      this.serviceName = serviceName;
      this.propertyType = propertyType;
      this.propertyName = propertyName;
    }
  }
}
