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
package org.apache.hadoop.metrics2.host.aggregator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Abstract class that runs a thread that publishes metrics data to AMS collector in specified intervals.
 */
public abstract class AbstractMetricPublisherThread extends Thread {
    protected int publishIntervalInSeconds;
    protected String publishURL;
    protected ObjectMapper objectMapper;
    private Log LOG;
    protected TimelineMetricsHolder timelineMetricsHolder;

    public AbstractMetricPublisherThread(TimelineMetricsHolder timelineMetricsHolder, String publishURL, int publishIntervalInSeconds) {
        LOG = LogFactory.getLog(this.getClass());
        this.publishURL = publishURL;
        this.publishIntervalInSeconds = publishIntervalInSeconds;
        this.timelineMetricsHolder = timelineMetricsHolder;
        objectMapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        objectMapper.setAnnotationIntrospector(introspector);
        objectMapper.getSerializationConfig()
                .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    /**
     * Publishes metrics to collector in specified intervals while not interrupted.
     */
    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                sleep(this.publishIntervalInSeconds * 1000);
            } catch (InterruptedException e) {
                //Ignore
            }
            try {
                processAndPublishMetrics(getMetricsFromCache());
            } catch (Exception e) {
                LOG.error("Couldn't process and send metrics : ",e);
            }
        }
    }

    /**
     * Processes and sends metrics to collector.
     * @param metricsFromCache
     * @throws Exception
     */
    protected void processAndPublishMetrics(Map<Long, TimelineMetrics> metricsFromCache) throws Exception {
        if (metricsFromCache.size()==0) return;

        LOG.info(String.format("Preparing %s timeline metrics for publishing", metricsFromCache.size()));
        publishMetricsJson(processMetrics(metricsFromCache));
    }

    /**
     * Returns metrics map. Source is based on implementation.
     * @return
     */
    protected abstract Map<Long,TimelineMetrics> getMetricsFromCache();

    /**
     * Processes given metrics (aggregates or merges them) and converts them into json string that will be send to collector
     * @param metricValues
     * @return
     */
    protected abstract String processMetrics(Map<Long, TimelineMetrics> metricValues);

    protected void publishMetricsJson(String jsonData) throws Exception {
        int timeout = 5 * 1000;
        HttpURLConnection connection = null;
        if (this.publishURL == null) {
            throw new IOException("Unknown URL. Unable to connect to metrics collector.");
        }
        LOG.info("Collector URL : " + publishURL);
        connection = (HttpURLConnection) new URL(this.publishURL).openConnection();

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
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("responseCode is " + responseCode);
        }
        LOG.info("Successfully sent metrics.");
    }

    /**
     * Interrupts the thread.
     */
    protected void stopPublisher() {
        this.interrupt();
    }
}
