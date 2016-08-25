/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.solr.metrics;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.management.MalformedObjectNameException;

import org.apache.ambari.logsearch.common.PropertiesHelper;
import org.apache.ambari.logsearch.solr.AmbariSolrCloudClient;
import org.apache.ambari.logsearch.solr.AmbariSolrCloudClientBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrMetricsLoader extends TimerTask {
  private static final Logger LOG = LoggerFactory.getLogger(SolrMetricsLoader.class);

  private static final int RETRY = 3;
  private static final int MAX_METRIC_SIZE= 1000;

  private final String solrHost;
  private final SolrJmxAdapter solrJmxAdapter;
  private final SolrAmsClient solrAmsClient;
  
  private final TimelineMetrics metrics = new TimelineMetrics();

  public SolrMetricsLoader(String solrHost, int solrJmxPort, String collectorHost) throws IOException {
    this.solrHost = solrHost;
    this.solrJmxAdapter = new SolrJmxAdapter(solrHost, solrJmxPort);
    this.solrAmsClient = new SolrAmsClient(collectorHost);

    solrJmxAdapter.connect();
  }

  @Override
  public void run() {
    LOG.info("Loading Solr Metrics for the host " + solrHost);

    addCpuUsageMetric();
    addHeapMemoryUsageMetric();
    addIndexSizeMetric();

    emitMetrics();
    removeOverTheLimitMetrics();
  }

  private void addCpuUsageMetric() {
    Exception lastException = null;
    for (int retries = 0; retries < RETRY; retries++) {
      try {
        double processCpuLoad = solrJmxAdapter.getProcessCpuLoad();
        addMetric("logsearch.solr.cpu.usage", "Float", processCpuLoad);
        return;
      } catch (MalformedObjectNameException e) {
        lastException = e;
        try {
          solrJmxAdapter.reConnect();
        } catch (IOException e1) {
        }
      }
    }

    LOG.info("Could not load solr cpu usage metric, last exception:", lastException);
  }

  private void addHeapMemoryUsageMetric() {
    Exception lastException = null;
    for (int retries = 0; retries < RETRY; retries++) {
      try {
        Map<String, Long> memoryData = solrJmxAdapter.getMemoryData();
        addMetric("jvm.JvmMetrics.MemHeapUsedM", "Long", memoryData.get("heapMemoryUsed").doubleValue() / 1024 / 1024);
        addMetric("jvm.JvmMetrics.MemHeapCommittedM", "Long", memoryData.get("heapMemoryCommitted").doubleValue() / 1024 / 1024);
        addMetric("jvm.JvmMetrics.MemHeapMaxM", "Long", memoryData.get("heapMemoryMax").doubleValue() / 1024 / 1024);
        addMetric("jvm.JvmMetrics.MemNonHeapUsedM", "Long", memoryData.get("nonHeapMemoryUsed").doubleValue() / 1024 / 1024);
        addMetric("jvm.JvmMetrics.MemNonHeapCommittedM", "Long", memoryData.get("nonHeapMemoryCommitted").doubleValue() / 1024 / 1024);
        addMetric("jvm.JvmMetrics.MemNonHeapMaxM", "Long", memoryData.get("nonHeapMemoryMax").doubleValue() / 1024 / 1024);
        return;
      } catch (MalformedObjectNameException e) {
        lastException = e;
        try {
          solrJmxAdapter.reConnect();
        } catch (IOException e1) {
        }
      }
    }

    LOG.info("Could not load solr heap memory usage metric, last exception:", lastException);
  }

  private void addIndexSizeMetric() {
    Exception lastException = null;
    for (int retries = 0; retries < RETRY; retries++) {
      try {
        double indexSize = solrJmxAdapter.getIndexSize();
        addMetric("logsearch.solr.index.size", "Long", indexSize / 1024 / 1024 / 1024);
        return;
      } catch (Exception e) {
        lastException = e;
        try {
          solrJmxAdapter.reConnect();
        } catch (IOException e1) {
        }
      }
    }

    LOG.info("Could not load solr index size metric, last exception:", lastException);
  }

  private void addMetric(String metricName, String type, Double value) {
    Long currMS = System.currentTimeMillis();

    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName(metricName);
    metric.setHostName(solrHost);
    metric.setAppId("infra-solr");
    metric.setStartTime(currMS);
    metric.setType(type);
    metric.setTimestamp(currMS);
    metric.getMetricValues().put(currMS, value);

    metrics.addOrMergeTimelineMetric(metric);
  }

  private void emitMetrics() {
    Exception lastException = null;
    for (int retries = 0; retries < RETRY; retries++) {
      try {
        if (solrAmsClient.emitMetrics(metrics)) {
          metrics.getMetrics().clear();
          return;
        }
      } catch (Exception e) {
        lastException = e;
      }
    }

    LOG.info("Could not emit metrics, last exception:", lastException);
  }

  private void removeOverTheLimitMetrics() {
    for (TimelineMetric metric : metrics.getMetrics()) {
      TreeMap<Long, Double> metricValues = metric.getMetricValues();
      while (metricValues.size() > MAX_METRIC_SIZE) {
        metricValues.remove(metricValues.firstKey());
      }
    }
  }

  public static void startSolrMetricsLoaderTasks() {
    try {
      String collectorHosts = PropertiesHelper.getProperty("logsearch.solr.metrics.collector.hosts");
      if (StringUtils.isEmpty(collectorHosts)) {
        LOG.warn("No Ambari Metrics service is available, no Solr metrics will be loaded!");
        return;
      }

      int solrJmxPort = PropertiesHelper.getIntProperty("logsearch.solr.jmx.port");

      String zkConnectString = PropertiesHelper.getProperty("logsearch.solr.zk_connect_string");
      AmbariSolrCloudClient ambariSolrCloudClient = new AmbariSolrCloudClientBuilder()
          .withZkConnectString(zkConnectString)
          .build();

      Collection<String> solrHosts = ambariSolrCloudClient.getSolrHosts();
      for (String solrHost : solrHosts) {
        SolrMetricsLoader sml = new SolrMetricsLoader(solrHost, solrJmxPort, collectorHosts);
        Timer timer = new Timer("Solr Metrics Loader - " + solrHost, true);
        timer.scheduleAtFixedRate(sml, 0, 10000);
      }
    } catch (Exception e) {
      LOG.warn("Could not start solr metric loader tasks", e);
    }
  }
}
