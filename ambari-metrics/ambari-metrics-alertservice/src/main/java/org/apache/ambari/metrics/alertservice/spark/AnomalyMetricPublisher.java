package org.apache.ambari.metrics.alertservice.spark;

import org.apache.ambari.metrics.alertservice.common.MetricAnomaly;
import org.apache.ambari.metrics.alertservice.common.TimelineMetric;
import org.apache.ambari.metrics.alertservice.common.TimelineMetrics;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

public class AnomalyMetricPublisher implements Serializable {

    private String hostName = "UNKNOWN.example.com";
    private String instanceId = null;
    private String serviceName = "anomaly-engine";
    private String collectorHost;
    private String protocol;
    private String port;
    private static final String WS_V1_TIMELINE_METRICS = "/ws/v1/timeline/metrics";
    private static final Log LOG = LogFactory.getLog(AnomalyMetricPublisher.class);
    private static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        mapper.setAnnotationIntrospector(introspector);
        mapper.getSerializationConfig()
                .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    public AnomalyMetricPublisher(String collectorHost, String protocol, String port) {
        this.collectorHost = collectorHost;
        this.protocol = protocol;
        this.port = port;
        this.hostName = getDefaultLocalHostName();
    }

    private String getDefaultLocalHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            LOG.info("Error getting host address");
        }
        return null;
    }

    public void publish(List<MetricAnomaly> metricAnomalies) {
        LOG.info("Sending metric anomalies of size : " + metricAnomalies.size());
        List<TimelineMetric> metricList = getTimelineMetricList(metricAnomalies);
        LOG.info("Sending TimelineMetric list of size : " + metricList.size());
        if (!metricList.isEmpty()) {
            TimelineMetrics timelineMetrics = new TimelineMetrics();
            timelineMetrics.setMetrics(metricList);
            emitMetrics(timelineMetrics);
        }
    }

    private List<TimelineMetric> getTimelineMetricList(List<MetricAnomaly> metricAnomalies) {
        List<TimelineMetric> metrics = new ArrayList<>();

        if (metricAnomalies.isEmpty()) {
            return metrics;
        }

        long currentTime = System.currentTimeMillis();
        MetricAnomaly prevAnomaly = metricAnomalies.get(0);

        TimelineMetric timelineMetric = new TimelineMetric();
        timelineMetric.setMetricName(prevAnomaly.getMetricKey() + "_" + prevAnomaly.getMethodResult().getMethodType());
        timelineMetric.setAppId(serviceName);
        timelineMetric.setInstanceId(instanceId);
        timelineMetric.setHostName(hostName);
        timelineMetric.setStartTime(currentTime);

        TreeMap<Long,Double> metricValues = new TreeMap<>();
        metricValues.put(prevAnomaly.getTimestamp(), prevAnomaly.getMetricValue());
        MetricAnomaly currentAnomaly;

        for (int i = 1; i < metricAnomalies.size(); i++) {
            currentAnomaly = metricAnomalies.get(i);
            if (currentAnomaly.getMetricKey().equals(prevAnomaly.getMetricKey())) {
                metricValues.put(currentAnomaly.getTimestamp(), currentAnomaly.getMetricValue());
            } else {
                timelineMetric.setMetricValues(metricValues);
                metrics.add(timelineMetric);

                timelineMetric = new TimelineMetric();
                timelineMetric.setMetricName(currentAnomaly.getMetricKey() + "_" + currentAnomaly.getMethodResult().getMethodType());
                timelineMetric.setAppId(serviceName);
                timelineMetric.setInstanceId(instanceId);
                timelineMetric.setHostName(hostName);
                timelineMetric.setStartTime(currentTime);
                metricValues = new TreeMap<>();
                metricValues.put(currentAnomaly.getTimestamp(), currentAnomaly.getMetricValue());
                prevAnomaly = currentAnomaly;
            }
        }

        timelineMetric.setMetricValues(metricValues);
        metrics.add(timelineMetric);
        return metrics;
    }

    private boolean emitMetrics(TimelineMetrics metrics) {
        String connectUrl = constructTimelineMetricUri();
        String jsonData = null;
        LOG.info("EmitMetrics connectUrl = "  + connectUrl);
        try {
            jsonData = mapper.writeValueAsString(metrics);
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
        LOG.info("Metrics Data : " + jsonData);
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
}
