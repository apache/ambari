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

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_APP_IDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_APP_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricsFilter;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricHostMetadata;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;

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
  private final Map<String, TimelineMetricHostMetadata> hostMetadata;
  Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics = new HashMap<>();
  TimelineMetricMetadataManager metadataManagerInstance;

  public TimelineMetricAppAggregator(TimelineMetricMetadataManager metadataManager,
                                     Configuration metricsConf) {
    appIdsToAggregate = getAppIdsForHostAggregation(metricsConf);
    hostMetadata = metadataManager.getHostedAppsCache();
    metadataManagerInstance = metadataManager;
    LOG.info("AppIds configured for aggregation: " + appIdsToAggregate);
  }

  /**
   * Lifecycle method to initialize aggregation cycle.
   */
  public void init() {
    LOG.debug("Initializing aggregation cycle.");
    aggregateClusterMetrics = new HashMap<>();
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
      if (hostMetadata.containsKey(hostname)) {
        updateAppAggregatesFromHostMetric(clusterMetric, hostname, metricValue);
      }
    } else {
      // Build the hostedapps map if not a host metric
      // Check app candidacy for host aggregation
      if (appIdsToAggregate.contains(appId)) {
        TimelineMetricHostMetadata timelineMetricHostMetadata = hostMetadata.get(hostname);
        ConcurrentHashMap<String, String> appIds;
        if (timelineMetricHostMetadata == null) {
          appIds = new ConcurrentHashMap<>();
          hostMetadata.put(hostname, new TimelineMetricHostMetadata(appIds));
        } else {
          appIds = timelineMetricHostMetadata.getHostedApps();
        }
        if (!appIds.containsKey(appId)) {
          appIds.put(appId, appId);
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

    TimelineMetricMetadataKey appKey =  new TimelineMetricMetadataKey(clusterMetric.getMetricName(), HOST_APP_ID, clusterMetric.getInstanceId());
    ConcurrentHashMap<String, String> apps = hostMetadata.get(hostname).getHostedApps();
    for (String appId : apps.keySet()) {
      if (appIdsToAggregate.contains(appId)) {

        appKey.setAppId(appId);
        TimelineMetricMetadata appMetadata = metadataManagerInstance.getMetadataCacheValue(appKey);
        if (appMetadata == null) {
          TimelineMetricMetadataKey key = new TimelineMetricMetadataKey(clusterMetric.getMetricName(), HOST_APP_ID, clusterMetric.getInstanceId());
          TimelineMetricMetadata hostMetricMetadata = metadataManagerInstance.getMetadataCacheValue(key);

          if (hostMetricMetadata != null) {
            TimelineMetricMetadata timelineMetricMetadata = new TimelineMetricMetadata(clusterMetric.getMetricName(),
              appId, clusterMetric.getInstanceId(), hostMetricMetadata.getUnits(), hostMetricMetadata.getType(), hostMetricMetadata.getSeriesStartTime(),
              hostMetricMetadata.isSupportsAggregates(), TimelineMetricsFilter.acceptMetric(clusterMetric.getMetricName(), appId));
            metadataManagerInstance.putIfModifiedTimelineMetricMetadata(timelineMetricMetadata);
          }
        }

        // Add a new cluster aggregate metric if none exists
        TimelineClusterMetric appTimelineClusterMetric =
          new TimelineClusterMetric(clusterMetric.getMetricName(),
            appId,
            clusterMetric.getInstanceId(),
            clusterMetric.getTimestamp());

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
