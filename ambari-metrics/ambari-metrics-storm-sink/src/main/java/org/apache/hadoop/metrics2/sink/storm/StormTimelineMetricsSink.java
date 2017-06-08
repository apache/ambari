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

package org.apache.hadoop.metrics2.sink.storm;

import org.apache.commons.lang.StringUtils;
import org.apache.storm.Constants;
import org.apache.storm.metric.api.IMetricsConsumer;
import org.apache.storm.task.IErrorReporter;
import org.apache.storm.task.TopologyContext;
import org.apache.commons.lang3.ClassUtils;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.UnableToConnectException;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.apache.hadoop.metrics2.sink.timeline.configuration.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache.MAX_EVICTION_TIME_MILLIS;
import static org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache.MAX_RECS_PER_NAME_DEFAULT;

public class StormTimelineMetricsSink extends AbstractTimelineMetricsSink implements IMetricsConsumer {
  // covers built-in metrics but still not beauty
  private static final String[] METRIC_LOWERCASE_SUBSTRINGS_AGGREGATE_AVERAGE = { "-latency", "timems", "time_ms", "rate_secs", "timesecs" };

  private static final String[] WARN_STRINGS_FOR_TOPOLOGY_OR_COMPONENT_NAME = { ".", "_" };

  // create String manually in order to not rely on Guava Joiner or having our own
  private static final String JOINED_WARN_STRINGS_FOR_MESSAGE = "\".\", \"_\"";

  // it's safe since it doesn't exceed the boundary
  public static final int SYSTEM_TASK_ID = (int) Constants.SYSTEM_TASK_ID;

  public static final String CLUSTER_REPORTER_APP_ID = "clusterReporterAppId";
  public static final String DEFAULT_CLUSTER_REPORTER_APP_ID = "storm";
  public static final String METRIC_NAME_PREFIX_KAFKA_OFFSET = "kafkaOffset.";

  private String collectorUri;
  private TimelineMetricsCache metricsCache;
  private String hostname;
  private int timeoutSeconds;
  private Collection<String> collectorHosts;
  private String zkQuorum;
  private String protocol;
  private String port;
  private String topologyName;
  private String applicationId;
  private String instanceId;
  private boolean setInstanceId;
  private boolean hostInMemoryAggregationEnabled;
  private int hostInMemoryAggregationPort;

  @Override
  protected String getCollectorUri(String host) {
    return constructTimelineMetricUri(protocol, host, port);
  }

  @Override
  protected String getCollectorProtocol() {
    return protocol;
  }

  @Override
  protected int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  @Override
  protected String getZookeeperQuorum() {
    return zkQuorum;
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return collectorHosts;
  }

  @Override
  protected String getCollectorPort() {
    return port;
  }

  @Override
  protected String getHostname() {
    return hostname;
  }

  @Override
  protected boolean isHostInMemoryAggregationEnabled() {
    return hostInMemoryAggregationEnabled;
  }

  @Override
  protected int getHostInMemoryAggregationPort() {
    return hostInMemoryAggregationPort;
  }

  @Override
  public void prepare(Map map, Object o, TopologyContext topologyContext, IErrorReporter iErrorReporter) {
    LOG.info("Preparing Storm Metrics Sink");
    try {
      hostname = InetAddress.getLocalHost().getHostName();
      //If not FQDN , call  DNS
      if ((hostname == null) || (!hostname.contains("."))) {
        hostname = InetAddress.getLocalHost().getCanonicalHostName();
      }
    } catch (UnknownHostException e) {
      LOG.error("Could not identify hostname.");
      throw new RuntimeException("Could not identify hostname.", e);
    }
    Configuration configuration = new Configuration("/storm-metrics2.properties");
    timeoutSeconds = Integer.parseInt(configuration.getProperty(METRICS_POST_TIMEOUT_SECONDS,
        String.valueOf(DEFAULT_POST_TIMEOUT_SECONDS)));
    int maxRowCacheSize = Integer.parseInt(configuration.getProperty(MAX_METRIC_ROW_CACHE_SIZE,
        String.valueOf(MAX_RECS_PER_NAME_DEFAULT)));
    int metricsSendInterval = Integer.parseInt(configuration.getProperty(METRICS_SEND_INTERVAL,
        String.valueOf(MAX_EVICTION_TIME_MILLIS)));
    applicationId = configuration.getProperty(CLUSTER_REPORTER_APP_ID, DEFAULT_CLUSTER_REPORTER_APP_ID);
    metricsCache = new TimelineMetricsCache(maxRowCacheSize, metricsSendInterval);
    collectorHosts = parseHostsStringIntoCollection(configuration.getProperty(COLLECTOR_HOSTS_PROPERTY));

    zkQuorum = StringUtils.isEmpty(configuration.getProperty(COLLECTOR_ZOOKEEPER_QUORUM)) ?
      configuration.getProperty("zookeeper.quorum") : configuration.getProperty(COLLECTOR_ZOOKEEPER_QUORUM);

    protocol = configuration.getProperty(COLLECTOR_PROTOCOL, "http");
    port = configuration.getProperty(COLLECTOR_PORT, "6188");
    instanceId = configuration.getProperty(INSTANCE_ID_PROPERTY, null);
    setInstanceId = Boolean.valueOf(configuration.getProperty(SET_INSTANCE_ID_PROPERTY, "false"));

    hostInMemoryAggregationEnabled = Boolean.valueOf(configuration.getProperty(HOST_IN_MEMORY_AGGREGATION_ENABLED_PROPERTY));
    hostInMemoryAggregationPort = Integer.valueOf(configuration.getProperty(HOST_IN_MEMORY_AGGREGATION_PORT_PROPERTY));

    // Initialize the collector write strategy
    super.init();

    if (protocol.contains("https")) {
      String trustStorePath = configuration.getProperty(SSL_KEYSTORE_PATH_PROPERTY).trim();
      String trustStoreType = configuration.getProperty(SSL_KEYSTORE_TYPE_PROPERTY).trim();
      String trustStorePwd = configuration.getProperty(SSL_KEYSTORE_PASSWORD_PROPERTY).trim();
      loadTruststore(trustStorePath, trustStoreType, trustStorePwd);
    }
    this.topologyName = removeNonce(topologyContext.getStormId());
    warnIfTopologyNameContainsWarnString(topologyName);
  }

  @Override
  public void handleDataPoints(TaskInfo taskInfo, Collection<DataPoint> dataPoints) {
    List<TimelineMetric> metricList = new ArrayList<TimelineMetric>();

    for (DataPoint dataPoint : dataPoints) {
      LOG.debug(dataPoint.name + " = " + dataPoint.value);

      List<DataPoint> populatedDataPoints;
      if (taskInfo.srcTaskId == SYSTEM_TASK_ID && dataPoint.value instanceof Collection) {
        // worker level aggregated metrics - aggregation should be handled
        List<DataPoint> populatedBeforeAggregationDataPoints = populateAllDataPointValues(dataPoint);
        Map<String, List<Double>> metricNameKeyedValues = groupByMetricNameDataPoints(populatedBeforeAggregationDataPoints);
        populatedDataPoints = applyAggregationToMetricNameKeyedDataPoints(metricNameKeyedValues);
      } else {
        populatedDataPoints = populateDataPoints(dataPoint);
      }

      for (DataPoint populatedDataPoint : populatedDataPoints) {
        String metricName;
        if (populatedDataPoint.name.startsWith(METRIC_NAME_PREFIX_KAFKA_OFFSET)) {
          metricName = createKafkaOffsetMetricName(populatedDataPoint.name);
        } else {
          metricName = createMetricName(taskInfo.srcComponentId, taskInfo.srcWorkerHost,
              taskInfo.srcWorkerPort, taskInfo.srcTaskId, populatedDataPoint.name);
        }

        LOG.debug("populated datapoint: " + metricName + " = " + populatedDataPoint.value);

        TimelineMetric timelineMetric = createTimelineMetric(taskInfo.timestamp * 1000,
            taskInfo.srcWorkerHost, metricName, Double.valueOf(populatedDataPoint.value.toString()));

        // Put intermediate values into the cache until it is time to send
        metricsCache.putTimelineMetric(timelineMetric);

        TimelineMetric cachedMetric = metricsCache.getTimelineMetric(timelineMetric.getMetricName());

        if (cachedMetric != null) {
          metricList.add(cachedMetric);
        }
      }
    }

    if (!metricList.isEmpty()) {
      TimelineMetrics timelineMetrics = new TimelineMetrics();
      timelineMetrics.setMetrics(metricList);

      try {
        emitMetrics(timelineMetrics);
      } catch (UnableToConnectException uce) {
        LOG.warn("Unable to send metrics to collector by address:" + uce.getConnectUrl());
      }
    }
  }

  @Override
  public void cleanup() {
    LOG.info("Stopping Storm Metrics Sink");
  }

  // purpose just for testing
  void setTopologyName(String topologyName) {
    this.topologyName = topologyName;
  }

  private String removeNonce(String topologyId) {
    return topologyId.substring(0, topologyId.substring(0, topologyId.lastIndexOf("-")).lastIndexOf("-"));
  }

  private List<DataPoint> populateAllDataPointValues(DataPoint dataPoint) {
    List<DataPoint> populatedDataPoints = new ArrayList<>();
    Collection<Object> values = (Collection<Object>) dataPoint.value;
    for (Object value : values) {
      List<DataPoint> populated = populateDataPoints(new DataPoint(dataPoint.name, value));
      for (DataPoint point : populated) {
        if (point.value == null) {
          continue;
        }

        populatedDataPoints.add(point);
      }
    }
    return populatedDataPoints;
  }

  private List<DataPoint> populateDataPoints(DataPoint dataPoint) {
    List<DataPoint> dataPoints = new ArrayList<>();

    if (dataPoint.value == null) {
      LOG.warn("Data point with name " + dataPoint.name + " is null. Discarding." + dataPoint.name);
    } else if (dataPoint.value instanceof Map) {
      Map<String, Object> dataMap = (Map<String, Object>) dataPoint.value;

      for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
        Double value = convertValueToDouble(entry.getKey(), entry.getValue());
        if (value != null) {
          dataPoints.add(new DataPoint(dataPoint.name + "." + entry.getKey(), value));
        }
      }
    } else {
      Double value = convertValueToDouble(dataPoint.name, dataPoint.value);
      if (value != null) {
        dataPoints.add(new DataPoint(dataPoint.name, value));
      }
    }

    return dataPoints;
  }

  private Double convertValueToDouble(String metricName, Object value) {
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else if (value instanceof String) {
      try {
        return Double.parseDouble((String) value);
      } catch (NumberFormatException e) {
        LOG.warn("Data point with name " + metricName + " doesn't have number format value " +
            value + ". Discarding.");
      }

      return null;
    } else {
      LOG.warn("Data point with name " + metricName + " has value " + value +
          " which is not supported. Discarding.");

      return null;
    }
  }

  private Map<String, List<Double>> groupByMetricNameDataPoints(List<DataPoint> populatedDataPoints) {
    Map<String, List<Double>> metricNameKeyedValues = new HashMap<>();
    for (DataPoint point : populatedDataPoints) {
      List<Double> valuesOnMetric = metricNameKeyedValues.get(point.name);

      if (valuesOnMetric == null) {
        valuesOnMetric = new ArrayList<>();
        metricNameKeyedValues.put(point.name, valuesOnMetric);
      }

      valuesOnMetric.add(Double.valueOf(point.value.toString()));
    }
    return metricNameKeyedValues;
  }

  private List<DataPoint> applyAggregationToMetricNameKeyedDataPoints(Map<String, List<Double>> metricNameKeyedValues) {
    List<DataPoint> populatedDataPoints = new ArrayList<>();
    for (Map.Entry<String, List<Double>> metricNameToValues : metricNameKeyedValues.entrySet()) {
      String key = metricNameToValues.getKey();
      List<Double> values = metricNameToValues.getValue();
      populatedDataPoints.add(new DataPoint(key, applyAggregateFunction(key, values)));
    }
    return populatedDataPoints;
  }

  private Double applyAggregateFunction(String metricName, List<Double> values) {
    String lowerCaseMetricName = metricName.toLowerCase();
    for (String aggregateMetricSubstring : METRIC_LOWERCASE_SUBSTRINGS_AGGREGATE_AVERAGE) {
      if (lowerCaseMetricName.contains(aggregateMetricSubstring)) {
        return calculateAverage(values);
      }
    }

    return calculateSummation(values);
  }

  private Double calculateSummation(List<Double> values) {
    Double sum = 0.0;
    for (Double value : values) {
      sum += value;
    }
    return sum;
  }

  private Double calculateAverage(List<Double> values) {
    if (values.isEmpty()) {
      return 0.0d;
    }

    return calculateSummation(values) / values.size();
  }

  private TimelineMetric createTimelineMetric(long currentTimeMillis, String hostName,
                                              String attributeName, Double attributeValue) {
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName(attributeName);
    timelineMetric.setHostName(hostName);
    if (setInstanceId) {
      timelineMetric.setInstanceId(instanceId);
    }
    timelineMetric.setAppId(applicationId);
    timelineMetric.setStartTime(currentTimeMillis);
    timelineMetric.setType(ClassUtils.getShortCanonicalName(
        attributeValue, "Number"));
    timelineMetric.getMetricValues().put(currentTimeMillis, attributeValue);
    return timelineMetric;
  }

  private String createMetricName(String componentId, String workerHost, int workerPort, int taskId,
      String attributeName) {
    // <topology name>.<component name>.<worker host>.<worker port>.<task id>.<metric name>
    String metricName = "topology." + topologyName + "." + componentId + "." + workerHost + "." + workerPort +
        "." + taskId + "." + attributeName;

    // since '._' is treat as special character (separator) so it should be replaced
    return metricName.replace('_', '-');
  }

  private String createKafkaOffsetMetricName(String kafkaOffsetMetricName) {
    // get rid of "kafkaOffset."
    // <topic>/<metric name (starts with total)> or <topic>/partition_<partition_num>/<metricName>
    String tempMetricName = kafkaOffsetMetricName.substring(METRIC_NAME_PREFIX_KAFKA_OFFSET.length());

    String[] slashSplittedNames = tempMetricName.split("/");

    if (slashSplittedNames.length == 1) {
      // unknown metrics
      throw new IllegalArgumentException("Unknown metrics for kafka offset metric: " + kafkaOffsetMetricName);
    }

    String topic = slashSplittedNames[0];
    String metricName = "topology." + topologyName + ".kafka-topic." + topic;
    if (slashSplittedNames.length > 2) {
      // partition level
      metricName = metricName + "." + slashSplittedNames[1] + "." + slashSplittedNames[2];
    } else {
      // topic level
      metricName = metricName + "." + slashSplittedNames[1];
    }

    // since '._' is treat as special character (separator) so it should be replaced
    return metricName.replace('_', '-');
  }

  private void warnIfTopologyNameContainsWarnString(String name) {
    for (String warn : WARN_STRINGS_FOR_TOPOLOGY_OR_COMPONENT_NAME) {
      if (name.contains(warn)) {
        LOG.warn("Topology name \"" + name + "\" contains \"" + warn + "\" which can be problematic for AMS.");
        LOG.warn("Encouraged to not using any of these strings: " + JOINED_WARN_STRINGS_FOR_MESSAGE);
        LOG.warn("Same suggestion applies to component name.");
      }
    }
  }

  public void setMetricsCache(TimelineMetricsCache metricsCache) {
    this.metricsCache = metricsCache;
  }

}
