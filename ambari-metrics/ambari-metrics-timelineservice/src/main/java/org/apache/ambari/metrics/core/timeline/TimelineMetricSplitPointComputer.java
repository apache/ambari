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

package org.apache.ambari.metrics.core.timeline;

import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_INITIAL_CONFIGURED_MASTER_COMPONENTS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_INITIAL_CONFIGURED_SLAVE_COMPONENTS;

public class TimelineMetricSplitPointComputer {

  private static final Log LOG = LogFactory.getLog(TimelineMetricSplitPointComputer.class);
  private Set<String> masterComponents = new HashSet<>();
  private Set<String> slaveComponents = new HashSet<>();

  private static final int MINIMUM_PRECISION_TABLE_REGIONS = 4;
  private static final int MINIMUM_AGGREGATE_TABLE_REGIONS = 2;
  private static final int OTHER_TABLE_STATIC_REGIONS = 8;
  private static final int SLAVE_EQUIDISTANT_POINTS = 50;
  private static final int MASTER_EQUIDISTANT_POINTS = 5;

  private double hbaseTotalHeapsize;
  private double hbaseMemstoreUpperLimit;
  private double hbaseMemstoreFlushSize;
  private TimelineMetricMetadataManager timelineMetricMetadataManager = null;

  private List<byte[]> precisionSplitPoints = new ArrayList<>();
  private List<byte[]> aggregateSplitPoints = new ArrayList<>();

  public TimelineMetricSplitPointComputer(Configuration metricsConf,
                                          Configuration hbaseConf,
                                          TimelineMetricMetadataManager timelineMetricMetadataManager) {

    String componentsString = metricsConf.get(TIMELINE_METRIC_INITIAL_CONFIGURED_MASTER_COMPONENTS, "");
    if (StringUtils.isNotEmpty(componentsString)) {
      masterComponents.addAll(Arrays.asList(componentsString.split(",")));
    }

   componentsString = metricsConf.get(TIMELINE_METRIC_INITIAL_CONFIGURED_SLAVE_COMPONENTS, "");
    if (StringUtils.isNotEmpty(componentsString)) {
      slaveComponents.addAll(Arrays.asList(componentsString.split(",")));
    }

    this.timelineMetricMetadataManager = timelineMetricMetadataManager;
    hbaseTotalHeapsize = metricsConf.getDouble("hbase_total_heapsize", 1024*1024*1024);
    hbaseMemstoreUpperLimit = hbaseConf.getDouble("hbase.regionserver.global.memstore.upperLimit", 0.3);
    hbaseMemstoreFlushSize = hbaseConf.getDouble("hbase.hregion.memstore.flush.size", 134217728);
  }


  protected void computeSplitPoints() {

    double memstoreMaxMemory = hbaseMemstoreUpperLimit * hbaseTotalHeapsize;
    int maxInMemoryRegions = (int) ((memstoreMaxMemory / hbaseMemstoreFlushSize) - OTHER_TABLE_STATIC_REGIONS);

    int targetPrecisionTableRegionCount = MINIMUM_PRECISION_TABLE_REGIONS;
    int targetAggregateTableRegionCount = MINIMUM_AGGREGATE_TABLE_REGIONS;

    if (maxInMemoryRegions > 2) {
      targetPrecisionTableRegionCount =  Math.max(4, (int)(0.70 * maxInMemoryRegions));
      targetAggregateTableRegionCount =  Math.max(2, (int)(0.15 * maxInMemoryRegions));
    }

    List<MetricApp> metricList = new ArrayList<>();

    for (String component : masterComponents) {
      metricList.addAll(getSortedMetricListForSplitPoint(component, false));
    }

    for (String component : slaveComponents) {
      metricList.addAll(getSortedMetricListForSplitPoint(component, true));
    }

    int totalMetricLength = metricList.size();

    if (targetPrecisionTableRegionCount > 1) {
      int idx = (int) Math.ceil(totalMetricLength / targetPrecisionTableRegionCount);
      int index = idx;
      for (int i = 0; i < targetPrecisionTableRegionCount; i++) {
        if (index < totalMetricLength - 1) {
          MetricApp metricAppService = metricList.get(index);
          byte[] uuid = timelineMetricMetadataManager.getUuid(
            new TimelineClusterMetric(metricAppService.metricName, metricAppService.appId, null, -1),
            true);
          precisionSplitPoints.add(uuid);
          index += idx;
        }
      }
    }

    if (targetAggregateTableRegionCount > 1) {
      int idx = (int) Math.ceil(totalMetricLength / targetAggregateTableRegionCount);
      int index = idx;
      for (int i = 0; i < targetAggregateTableRegionCount; i++) {
        if (index < totalMetricLength - 1) {
          MetricApp metricAppService = metricList.get(index);
          byte[] uuid = timelineMetricMetadataManager.getUuid(
            new TimelineClusterMetric(metricAppService.metricName, metricAppService.appId, null, -1),
            true);
          aggregateSplitPoints.add(uuid);
          index += idx;
        }
      }
    }
  }

  private List<MetricApp> getSortedMetricListForSplitPoint(String component, boolean isSlave) {

    String appId = getAppId(component);
    List<MetricApp> metricList = new ArrayList<>();

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = getClass().getClassLoader();
    }

    String strLine;
    BufferedReader bufferedReader;

    try (InputStream inputStream = classLoader.getResourceAsStream("metrics_def/" + appId.toUpperCase() + ".dat")) {

      if (inputStream != null) {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        LOG.info("Found split point candidate metrics for : " + appId);

        while ((strLine = bufferedReader.readLine()) != null) {
          metricList.add(new MetricApp(strLine.trim(), appId));
        }
      } else {
        LOG.info("Split point candidate metrics not found for : " + appId);
      }
    } catch (Exception e) {
      LOG.info("Error reading split point candidate metrics for component : " + component);
      LOG.error(e);
    }

    if (isSlave) {
      return getEquidistantMetrics(metricList, SLAVE_EQUIDISTANT_POINTS);
    } else {
      return getEquidistantMetrics(metricList, MASTER_EQUIDISTANT_POINTS);
    }
  }

  private List<MetricApp> getEquidistantMetrics(List<MetricApp> metrics, int distance) {
    List<MetricApp> selectedMetricApps = new ArrayList<>();

    int idx = metrics.size() / distance;
    if (idx == 0) {
      return metrics;
    }

    int index = idx;
    for (int i = 0; i < distance; i++) {
      selectedMetricApps.add(metrics.get(index - 1));
      index += idx;
    }
    return selectedMetricApps;
  }


  public List<byte[]> getPrecisionSplitPoints() {
    return precisionSplitPoints;
  }

  public List<byte[]> getClusterAggregateSplitPoints() {
    return aggregateSplitPoints;
  }

  public List<byte[]> getHostAggregateSplitPoints() {
    return aggregateSplitPoints;
  }

  private String getAppId(String component) {

    if (component.equalsIgnoreCase("METRICS_COLLECTOR")) {
      return "ams-hbase";
    }

    if (component.equalsIgnoreCase("METRICS_MONITOR")) {
      return "HOST";
    }
    return component;
  }
}

class MetricApp implements Comparable{
  String metricName;
  String appId;

  MetricApp(String metricName, String appId) {
    this.metricName = metricName;
    if (appId.startsWith("hbase")) {
      this.appId = "hbase";
    } else {
      this.appId = appId;
    }
  }

  @Override
  public int compareTo(Object o) {
    MetricApp that = (MetricApp)o;

    int metricCompare = metricName.compareTo(that.metricName);
    if (metricCompare != 0) {
      return metricCompare;
    }

    return appId.compareTo(that.appId);
  }
}
