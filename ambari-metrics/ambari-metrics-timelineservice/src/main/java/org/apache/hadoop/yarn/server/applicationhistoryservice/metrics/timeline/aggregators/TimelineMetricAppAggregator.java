/**
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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_APP_IDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_APP_ID;

/**
 * Aggregator responsible for providing app level host aggregates. This task
 * is accomplished without doing a round trip to storage, rather
 * TimelineMetricClusterAggregators are responsible for lifecycle of
 * @TimelineMetricAppAggregator and provide the raw data to aggregate.
 */
public class TimelineMetricAppAggregator {
  private static final Log LOG = LogFactory.getLog(TimelineMetricAppAggregator.class);
  // Lookup to check candidacy of an app
  private final List<String> appIdsToAggregate;
  private final Map<String, Set<String>> hostedAppsMap;
  Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics;

  public TimelineMetricAppAggregator(TimelineMetricMetadataManager metadataManager,
                                     Configuration metricsConf) {
    appIdsToAggregate = getAppIdsForHostAggregation(metricsConf);
    hostedAppsMap = metadataManager.getHostedAppsCache();
    LOG.info("AppIds configured for aggregation: " + appIdsToAggregate);
  }

  /**
   * Lifecycle method to initialize aggregation cycle.
   */
  public void init() {
    LOG.debug("Initializing aggregation cycle.");
    aggregateClusterMetrics = new HashMap<TimelineClusterMetric, MetricClusterAggregate>();
  }

  /**
   * Lifecycle method to indicate end of aggregation cycle.
   */
  public void cleanup() {
    LOG.debug("Cleanup aggregated data.");
    aggregateClusterMetrics = null;
  }

  /**
   * Calculate aggregates if the clusterMetric is a Host metric for recorded
   * apps that are housed by this host.
   *
   * @param clusterMetric @TimelineClusterMetric Host / App metric
   * @param hostname This is the hostname from which this clusterMetric originated.
   * @param metricValue The metric value for this metric.
   */
  public void processTimelineClusterMetric(TimelineClusterMetric clusterMetric,
                                           String hostname, Double metricValue) {

    String appId = clusterMetric.getAppId();
    if (appId == null) {
      return; // No real use case except tests
    }

    // If metric is a host metric and host has apps on it
    if (appId.equalsIgnoreCase(HOST_APP_ID)) {
      // Candidate metric, update app aggregates
      if (hostedAppsMap.containsKey(hostname)) {
        updateAppAggregatesFromHostMetric(clusterMetric, hostname, metricValue);
      }
    } else {
      // Build the hostedapps map if not a host metric
      // Check app candidacy for host aggregation
      if (appIdsToAggregate.contains(appId)) {
        Set<String> appIds = hostedAppsMap.get(hostname);
        if (appIds == null) {
          appIds = new HashSet<>();
          hostedAppsMap.put(hostname, appIds);
        }
        if (!appIds.contains(appId)) {
          appIds.add(appId);
          LOG.info("Adding appId to hosted apps: appId = " +
            clusterMetric.getAppId() + ", hostname = " + hostname);
        }
      }
    }
  }

  /**
   * Build a cluster app metric from a host metric
   */
  private void updateAppAggregatesFromHostMetric(TimelineClusterMetric clusterMetric,
                                                 String hostname, Double metricValue) {

    if (aggregateClusterMetrics == null) {
      LOG.error("Aggregation requested without init call.");
      return;
    }

    Set<String> apps = hostedAppsMap.get(hostname);
    for (String appId : apps) {
      // Add a new cluster aggregate metric if none exists
      TimelineClusterMetric appTimelineClusterMetric =
        new TimelineClusterMetric(clusterMetric.getMetricName(),
          appId,
          clusterMetric.getInstanceId(),
          clusterMetric.getTimestamp(),
          clusterMetric.getType()
        );

      MetricClusterAggregate clusterAggregate = aggregateClusterMetrics.get(appTimelineClusterMetric);

      if (clusterAggregate == null) {
        clusterAggregate = new MetricClusterAggregate(metricValue, 1, null, metricValue, metricValue);
        aggregateClusterMetrics.put(appTimelineClusterMetric, clusterAggregate);
      } else {
        clusterAggregate.updateSum(metricValue);
        clusterAggregate.updateNumberOfHosts(1);
        clusterAggregate.updateMax(metricValue);
        clusterAggregate.updateMin(metricValue);
      }

    }
  }

  /**
   * Return current copy of aggregated data.
   */
  public Map<TimelineClusterMetric, MetricClusterAggregate> getAggregateClusterMetrics() {
    return aggregateClusterMetrics;
  }

  private List<String> getAppIdsForHostAggregation(Configuration metricsConf) {
    String appIds = metricsConf.get(CLUSTER_AGGREGATOR_APP_IDS);
    if (!StringUtils.isEmpty(appIds)) {
      return Arrays.asList(StringUtils.stripAll(appIds.split(",")));
    }
    return Collections.emptyList();
  }
}
