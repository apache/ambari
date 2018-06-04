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


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.RequestResourceProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This pre upgrade check verifies if the Ambari Metrics Hadoop Sink package version on all hosts is the expected one
 * corresponding to the stack version. For example, in HDP 3.0, the corresponding ambari-metrics-hadoop-sink version should
 * be 2.7.0.0.
 */
@Singleton
@UpgradeCheck(
  group = UpgradeCheckGroup.REPOSITORY_VERSION)
public class AmbariMetricsHadoopSinkVersionCompatibilityCheck extends AbstractCheckDescriptor  {

  @Inject
  private RequestDAO requestDAO;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  private static final Logger LOG = LoggerFactory.getLogger(AmbariMetricsHadoopSinkVersionCompatibilityCheck.class);

  private enum PreUpgradeCheckStatus {SUCCESS, FAILED, RUNNING}

  static final String HADOOP_SINK_VERSION_NOT_SPECIFIED = "hadoop-sink-version-not-specified";

  static final String MIN_HADOOP_SINK_VERSION_PROPERTY_NAME = "min-hadoop-sink-version";
  static final String RETRY_INTERVAL_PROPERTY_NAME = "request-status-check-retry-interval";
  static final String NUM_TRIES_PROPERTY_NAME = "request-status-check-num-retries";

  /**
   * Total wait time for Ambari Server request time to finish => 2 mins.
   */
  private long retryInterval = 6000l; // 6 seconds sleep interval per retry.
  private int numTries = 20; // 20 times the check will try to see if request finished.

  /**
   * Constructor.
   */
  public AmbariMetricsHadoopSinkVersionCompatibilityCheck() {
    super(CheckDescription.AMS_HADOOP_SINK_VERSION_COMPATIBILITY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getApplicableServices() {
    return Sets.newHashSet("AMBARI_METRICS", "HDFS");
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest prereqCheckRequest) throws AmbariException {

    String minHadoopSinkVersion = null;

    UpgradePack.PrerequisiteCheckConfig prerequisiteCheckConfig = prereqCheckRequest.getPrerequisiteCheckConfig();
    Map<String, String> checkProperties = null;
    if(prerequisiteCheckConfig != null) {
      checkProperties = prerequisiteCheckConfig.getCheckProperties(this.getClass().getName());
    }

    if(checkProperties != null) {
      minHadoopSinkVersion = checkProperties.get(MIN_HADOOP_SINK_VERSION_PROPERTY_NAME);
      retryInterval = Long.valueOf(checkProperties.getOrDefault(RETRY_INTERVAL_PROPERTY_NAME, "6000"));
      numTries = Integer.valueOf(checkProperties.getOrDefault(NUM_TRIES_PROPERTY_NAME, "20"));
    }

    if (StringUtils.isEmpty(minHadoopSinkVersion)) {
      LOG.debug("Hadoop Sink version for pre-check not specified.");
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(getFailReason(HADOOP_SINK_VERSION_NOT_SPECIFIED, prerequisiteCheck, prereqCheckRequest));
      return;
    }

    LOG.debug("Properties : Hadoop Sink Version = {} , retryInterval = {}, numTries = {}", minHadoopSinkVersion, retryInterval, numTries);

    AmbariManagementController ambariManagementController = AmbariServer.getController();

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      Resource.Type.Request,
      ambariManagementController
      );

    String clusterName = prereqCheckRequest.getClusterName();

    Set<String> hosts = ambariManagementController.getClusters()
      .getCluster(clusterName).getHosts("AMBARI_METRICS", "METRICS_MONITOR");

    if (CollectionUtils.isEmpty(hosts)) {
      LOG.warn("No hosts have the component METRICS_MONITOR.");
      prerequisiteCheck.setStatus(PrereqCheckStatus.PASS);
      return;
    }

    Set<Map<String, Object>> propertiesSet = new HashSet<>();
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName);

    Set<Map<String, Object>> filterSet = new HashSet<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(RequestResourceProvider.SERVICE_ID, "AMBARI_METRICS");
    filterMap.put(RequestResourceProvider.COMPONENT_ID, "METRICS_MONITOR");
    filterMap.put(RequestResourceProvider.HOSTS_ID, StringUtils.join(hosts,","));
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);
    propertiesSet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, "CHECK_HADOOP_SINK_VERSION");
    requestInfoProperties.put(RequestResourceProvider.CONTEXT, "Pre Upgrade check for Hadoop Metric Sink version");

    Request request = PropertyHelper.getCreateRequest(propertiesSet, requestInfoProperties);
    try {
      org.apache.ambari.server.controller.spi.RequestStatus response = provider.createResources(request);
      Resource responseResource = response.getRequestResource();
      String requestIdProp = PropertyHelper.getPropertyId("Requests", "id");
      long requestId = (long) responseResource.getPropertyValue(requestIdProp);
      LOG.debug("RequestId for AMS Hadoop Sink version compatibility pre check : " + requestId);

      Thread.sleep(retryInterval);
      PreUpgradeCheckStatus status;
      int retry = 0;
      LinkedHashSet<String> failedHosts = new LinkedHashSet<>();
      while ((status = pollRequestStatus(requestId, failedHosts)).equals(PreUpgradeCheckStatus.RUNNING)
        && retry++ < numTries) {
        if (retry != numTries) {
          Thread.sleep(retryInterval);
        }
      }

      if (status.equals(PreUpgradeCheckStatus.SUCCESS)) {
        prerequisiteCheck.setStatus(PrereqCheckStatus.PASS);
      } else {
        prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
        prerequisiteCheck.setFailReason(String.format(getFailReason(prerequisiteCheck, prereqCheckRequest), minHadoopSinkVersion));
        prerequisiteCheck.setFailedOn(failedHosts);
      }
    } catch (Exception e) {
      LOG.error("Error running Pre Upgrade check for AMS Hadoop Sink compatibility. " + e);
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
    }
  }

  /**
   * Get the status of the requestId and also the set of failed hosts if any.
   * @param requestId RequestId to track.
   * @param failedHosts populate this argument for failed hosts.
   * @return Status of the request.
   * @throws Exception
   */
  private PreUpgradeCheckStatus pollRequestStatus(long requestId, Set<String> failedHosts) throws Exception {

    List<RequestEntity> requestEntities = requestDAO.findByPks(Collections.singleton(requestId), true);
    if (requestEntities != null && requestEntities.size() > 0) {

      RequestEntity requestEntity = requestEntities.iterator().next();
      HostRoleStatus requestStatus = requestEntity.getStatus();

      if (HostRoleStatus.COMPLETED.equals(requestStatus)) {
        return PreUpgradeCheckStatus.SUCCESS;
      }

      else if (requestStatus.isFailedState()) {
        failedHosts.addAll(getPreUpgradeCheckFailedHosts(requestEntity));
        LOG.debug("Hadoop Sink version check failed on the following hosts : " + failedHosts.stream().collect(Collectors.joining(",")));
        return PreUpgradeCheckStatus.FAILED;
      } else {
        return PreUpgradeCheckStatus.RUNNING;
      }
    } else {
      LOG.error("Unable to find RequestEntity for created request.");
    }
    return PreUpgradeCheckStatus.FAILED;
  }

  /**
   * Get the set of hosts (tasks) that failed the check.
   * @param requestEntity request info to get the task level info.
   * @return Set of hosts which failed the check along with type of failed state.
   * @throws Exception
   */
  private Set<String> getPreUpgradeCheckFailedHosts(RequestEntity requestEntity) throws Exception {

    List<HostRoleCommandEntity> hostRoleCommandEntities = hostRoleCommandDAO.findByRequest(requestEntity.getRequestId(), true);

    Set<String> failedHosts = new LinkedHashSet<>();
    for (HostRoleCommandEntity hostRoleCommandEntity : hostRoleCommandEntities) {
      HostRoleStatus status = hostRoleCommandEntity.getStatus();
      if (status.isFailedState()) {
        failedHosts.add(hostRoleCommandEntity.getHostName() + "(" + status + ")");
      }
    }
    return failedHosts;
  }
}