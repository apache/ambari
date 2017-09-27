/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.alertservice.prototype.core;

import org.apache.ambari.metrics.alertservice.prototype.methods.MetricAnomaly;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class MetricsCollectorInterface implements Serializable {

  private static String hostName = null;
  private String instanceId = null;
  public final static String serviceName = "anomaly-engine";
  private String collectorHost;
  private String protocol;
  private String port;
  private static final String WS_V1_TIMELINE_METRICS = "/ws/v1/timeline/metrics";
  private static final Log LOG = LogFactory.getLog(MetricsCollectorInterface.class);
  private static ObjectMapper mapper;
  private final static ObjectReader timelineObjectReader;

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig()
      .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    timelineObjectReader = mapper.reader(TimelineMetrics.class);
  }

  public MetricsCollectorInterface(String collectorHost, String protocol, String port) {
    this.collectorHost = collectorHost;
    this.protocol = protocol;
    this.port = port;
    this.hostName = getDefaultLocalHostName();
  }

  public static String getDefaultLocalHostName() {

    if (hostName != null) {
      return hostName;
    }

    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      LOG.info("Error getting host address");
    }
    return null;
  }

  public void publish(List<MetricAnomaly> metricAnomalies) {
    if (CollectionUtils.isNotEmpty(metricAnomalies)) {
      LOG.info("Sending metric anomalies of size : " + metricAnomalies.size());
      List<TimelineMetric> metricList = getTimelineMetricList(metricAnomalies);
      if (!metricList.isEmpty()) {
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.setMetrics(metricList);
        emitMetrics(timelineMetrics);
      }
    } else {
      LOG.debug("No anomalies to send.");
    }
  }

  private List<TimelineMetric> getTimelineMetricList(List<MetricAnomaly> metricAnomalies) {
    List<TimelineMetric> metrics = new ArrayList<>();

    if (metricAnomalies.isEmpty()) {
      return metrics;
    }

    for (MetricAnomaly anomaly : metricAnomalies) {
      TimelineMetric timelineMetric = new TimelineMetric();
      timelineMetric.setMetricName(anomaly.getMetricKey());
      timelineMetric.setAppId(serviceName + "-" + anomaly.getMethodType());
      timelineMetric.setInstanceId(null);
      timelineMetric.setHostName(getDefaultLocalHostName());
      timelineMetric.setStartTime(anomaly.getTimestamp());
      HashMap<String, String> metadata = new HashMap<>();
      metadata.put("method", anomaly.getMethodType());
      metadata.put("anomaly-score", String.valueOf(anomaly.getAnomalyScore()));
      timelineMetric.setMetadata(metadata);
      TreeMap<Long,Double> metricValues = new TreeMap<>();
      metricValues.put(anomaly.getTimestamp(), anomaly.getMetricValue());
      timelineMetric.setMetricValues(metricValues);

      metrics.add(timelineMetric);
    }
    return metrics;
  }

  public boolean emitMetrics(TimelineMetrics metrics) {
    String connectUrl = constructTimelineMetricUri();
    String jsonData = null;
    LOG.debug("EmitMetrics connectUrl = " + connectUrl);
    try {
      jsonData = mapper.writeValueAsString(metrics);
      LOG.info(jsonData);
    } catch (IOException e) {
      LOG.error("Unable to parse metrics", e);
    }
    if (jsonData != null) {
      return emitMetricsJson(connectUrl, jsonData);
    }
    return false;
  }

  private HttpURLConnection getConnection(String spec) throws IOException {
    return (HttpURLConnection) new URL(spec).openConnection();
  }

  private boolean emitMetricsJson(String connectUrl, String jsonData) {
    int timeout = 10000;
    HttpURLConnection connection = null;
    try {
      if (connectUrl == null) {
        throw new IOException("Unknown URL. Unable to connect to metrics collector.");
      }
      connection = getConnection(connectUrl);

      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Connection", "Keep-Alive");
      connection.setConnectTimeout(timeout);
      connection.setReadTimeout(timeout);
      connection.setDoOutput(true);

      if (jsonData != null) {
        try (OutputStream os = connection.getOutputStream()) {
          os.write(jsonData.getBytes("UTF-8"));
        }
      }

      int statusCode = connection.getResponseCode();

      if (statusCode != 200) {
        LOG.info("Unable to POST metrics to collector, " + connectUrl + ", " +
          "statusCode = " + statusCode);
      } else {
        LOG.info("Metrics posted to Collector " + connectUrl);
      }
      return true;
    } catch (IOException ioe) {
      LOG.error(ioe.getMessage());
    }
    return false;
  }

  private String constructTimelineMetricUri() {
    StringBuilder sb = new StringBuilder(protocol);
    sb.append("://");
    sb.append(collectorHost);
    sb.append(":");
    sb.append(port);
    sb.append(WS_V1_TIMELINE_METRICS);
    return sb.toString();
  }

  public TimelineMetrics fetchMetrics(String metricName,
                                      String appId,
                                      String hostname,
                                      long startime,
                                      long endtime) {

    String url = constructTimelineMetricUri() + "?metricNames=" + metricName + "&appId=" + appId +
      "&hostname=" + hostname + "&startTime=" + startime + "&endTime=" + endtime;
    LOG.debug("Fetch metrics URL : " + url);

    URL obj = null;
    BufferedReader in = null;
    TimelineMetrics timelineMetrics = new TimelineMetrics();

    try {
      obj = new URL(url);
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("GET");
      int responseCode = con.getResponseCode();
      LOG.debug("Sending 'GET' request to URL : " + url);
      LOG.debug("Response Code : " + responseCode);

      in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));
      timelineMetrics = timelineObjectReader.readValue(in);
    } catch (Exception e) {
      LOG.error(e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          LOG.warn(e);
        }
      }
    }

    LOG.info("Fetched " + timelineMetrics.getMetrics().size() + " metrics.");
    return timelineMetrics;
  }
}
