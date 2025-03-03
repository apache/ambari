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

package org.apache.ambari.server.metrics.system.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.ambari.server.metrics.system.SingleMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

/**
 * Gets the metrics about stomp stats connections and publishes to configured
 * Metric Sink.
 */
public class StompStatsMetricsSource extends AbstractMetricsSource {
    public static final String[] metricsTypes = { "stomp.api", "stomp.agent" };
    public static final String[] poolMetrics = { "pool_size", "active_threads", "queued_tasks", "completed_tasks" };
    public static final String[] webSocketMetrics = { "current_client_session", "current_web_socket_session",
            "current_http_stream", "current_http_polling",
            "total_sessions_established", "abnormally_closed_session", "connect_failure_session",
            "send_time_limit_exceeded", "transport_errors_sessions" };
    public static final String[] stompSubProtocolMetrics = { "connect", "connected", "disconnect" };
    private WebSocketMessageBrokerStats apiStompStats;
    private WebSocketMessageBrokerStats agentStompStats;
    private static final Logger LOG = LoggerFactory.getLogger(StompEventsMetricsSource.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Override
    public void start() {
        int interval = 60;
        LOG.info("Starting stomp stat source.");
        try {
            executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    List<SingleMetric> events = getStompStatMetrics();
                    if (!events.isEmpty()) {
                        sink.publish(events);
                        LOG.debug("********* Published stomp stat metrics to sink **********");
                    }
                }
            }, interval, interval, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.info("Failed to start stomp stat source", e);
        }
    }

    protected List<SingleMetric> getStompStatMetrics() {
        List<SingleMetric> metrics = new ArrayList<>();
        populateStompStatMetrics(metrics, metricsTypes[0], apiStompStats);
        populateStompStatMetrics(metrics, metricsTypes[1], agentStompStats);
        return metrics;
    }

    private void populateStompStatMetrics(List<SingleMetric> metricsList, String metricsPrefix,
            WebSocketMessageBrokerStats stompStats) {
        parseStats(metricsList, metricsPrefix + ".websocket.", webSocketMetrics,
                stompStats.getWebSocketSessionStatsInfo());
        parseStats(metricsList, metricsPrefix + ".stomp_sub_protocol.", stompSubProtocolMetrics,
                stompStats.getStompSubProtocolStatsInfo());
        parseStats(metricsList, metricsPrefix + ".inbound_channel.", poolMetrics,
                stompStats.getClientInboundExecutorStatsInfo());
        parseStats(metricsList, metricsPrefix + ".outbound_channel.", poolMetrics,
                stompStats.getClientOutboundExecutorStatsInfo());
        parseStats(metricsList, metricsPrefix + ".sockJsScheduler.", poolMetrics,
                stompStats.getSockJsTaskSchedulerStatsInfo());
    }

    private void parseStats(List<SingleMetric> metricsList, String metricsPrefix, String[] metricsNamesList,
            String stats) {
        if (stats.equals("null")) {
            LOG.warn("stats for " + metricsPrefix + " is null");
            return;
        }
        List<Long> statsArray = getValuesFromString(stats);

        // sanity check to make sure we have the same number of metrics and values.
        if (statsArray.size() != metricsNamesList.length) {
            LOG.error("Number of metrics and stats do not match for " + metricsPrefix);
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < metricsNamesList.length; i++) {
            metricsList
                    .add(new SingleMetric(metricsPrefix + metricsNamesList[i], statsArray.get(i), currentTimeMillis));
        }
    }

    private List<Long> getValuesFromString(String stats) {
        // '\\D+' matches one or more non-digit characters. After that it split the
        // string and filter out empty strings. Then we convert the string to long.
        return Arrays.stream(stats.split("\\D+"))
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    public void setApiStompStats(WebSocketMessageBrokerStats apiStompStats) {
        this.apiStompStats = apiStompStats;
    }

    public void setAgentStompStats(WebSocketMessageBrokerStats agentStompStats) {
        this.agentStompStats = agentStompStats;
    }
}
