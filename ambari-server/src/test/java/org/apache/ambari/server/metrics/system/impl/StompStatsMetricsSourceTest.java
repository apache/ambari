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

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.ambari.server.metrics.system.SingleMetric;
import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

public class StompStatsMetricsSourceTest {
    @Test
    public void testStatsMetrics() {
        String webStat = "[1 current WS(2)-HttpStream(3)-HttpPoll(4), 5 total, 6 closed abnormally (7 connect failure, 8 send limit, 9 transport error)]";
        String poolStat = "[pool size = 10, active threads = 20, queued tasks = 30, completed tasks = 40";
        String stompSubProtocolStat = "[1 connect, 2 connected, 3 disconnect]";
        Double[] webStatArray = { 1D, 2D, 3D, 4D, 5D, 6D, 7D, 8D, 9D };
        Double[] poolStatArray = { 10D, 20D, 30D, 40D };
        Double[] stompSubProtocolStatArray = { 1D, 2D, 3D };

        WebSocketMessageBrokerStats apiStompStatsMock = createNiceMock(WebSocketMessageBrokerStats.class);
        WebSocketMessageBrokerStats agentStompStatsMock = createNiceMock(WebSocketMessageBrokerStats.class);

        StompStatsMetricsSource stompStatsMetricsSource = new StompStatsMetricsSource();

        stompStatsMetricsSource.setApiStompStats(apiStompStatsMock);
        stompStatsMetricsSource.setAgentStompStats(agentStompStatsMock);

        EasyMock.expect(apiStompStatsMock.getWebSocketSessionStatsInfo()).andReturn(webStat).anyTimes();
        EasyMock.expect(apiStompStatsMock.getStompSubProtocolStatsInfo()).andReturn(stompSubProtocolStat).anyTimes();
        EasyMock.expect(apiStompStatsMock.getClientInboundExecutorStatsInfo()).andReturn(poolStat).anyTimes();
        EasyMock.expect(apiStompStatsMock.getClientOutboundExecutorStatsInfo()).andReturn(poolStat).anyTimes();
        EasyMock.expect(apiStompStatsMock.getSockJsTaskSchedulerStatsInfo()).andReturn(poolStat).anyTimes();

        EasyMock.expect(agentStompStatsMock.getWebSocketSessionStatsInfo()).andReturn(webStat).anyTimes();
        EasyMock.expect(agentStompStatsMock.getStompSubProtocolStatsInfo()).andReturn(stompSubProtocolStat).anyTimes();
        EasyMock.expect(agentStompStatsMock.getClientInboundExecutorStatsInfo()).andReturn(poolStat).anyTimes();
        EasyMock.expect(agentStompStatsMock.getClientOutboundExecutorStatsInfo()).andReturn(poolStat).anyTimes();
        EasyMock.expect(agentStompStatsMock.getSockJsTaskSchedulerStatsInfo()).andReturn(poolStat).anyTimes();

        EasyMock.replay(apiStompStatsMock);
        EasyMock.replay(agentStompStatsMock);

        List<SingleMetric> metricList = stompStatsMetricsSource.getStompStatMetrics();

        for (int i = 0; i < StompStatsMetricsSource.webSocketMetrics.length; i++) {
            assertEquals(webStatArray[i], getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[0]
                    + ".websocket." + StompStatsMetricsSource.webSocketMetrics[i]), 0.00);
            assertEquals(webStatArray[i], getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[1]
                    + ".websocket." + StompStatsMetricsSource.webSocketMetrics[i]), 0.00);
        }

        for (int i = 0; i < StompStatsMetricsSource.stompSubProtocolMetrics.length; i++) {
            assertEquals(stompSubProtocolStatArray[i],
                    getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[0] + ".stomp_sub_protocol."
                            + StompStatsMetricsSource.stompSubProtocolMetrics[i]),
                    0.00);
            assertEquals(stompSubProtocolStatArray[i],
                    getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[1] + ".stomp_sub_protocol."
                            + StompStatsMetricsSource.stompSubProtocolMetrics[i]),
                    0.00);
        }
        for (int i = 0; i < StompStatsMetricsSource.poolMetrics.length; i++) {
            assertEquals(poolStatArray[i], getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[0]
                    + ".inbound_channel." + StompStatsMetricsSource.poolMetrics[i]), 0.00);
            assertEquals(poolStatArray[i], getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[1]
                    + ".inbound_channel." + StompStatsMetricsSource.poolMetrics[i]), 0.00);
        }
        for (int i = 0; i < StompStatsMetricsSource.poolMetrics.length; i++) {
            assertEquals(poolStatArray[i], getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[0]
                    + ".outbound_channel." + StompStatsMetricsSource.poolMetrics[i]), 0.00);
            assertEquals(poolStatArray[i], getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[1]
                    + ".outbound_channel." + StompStatsMetricsSource.poolMetrics[i]), 0.00);
        }

        for (int i = 0; i < StompStatsMetricsSource.poolMetrics.length; i++) {
            assertEquals(poolStatArray[i], getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[0]
                    + ".sockJsScheduler." + StompStatsMetricsSource.poolMetrics[i]), 0.00);
            assertEquals(poolStatArray[i], getMetricValues(metricList, StompStatsMetricsSource.metricsTypes[1]
                    + ".sockJsScheduler." + StompStatsMetricsSource.poolMetrics[i]), 0.00);
        }
    }

    private double getMetricValues(List<SingleMetric> metricList, String metricName) {
        for (SingleMetric metric : metricList) {
            if (metric.getMetricName().equals(metricName)) {
                return metric.getValue();
            }
        }
        throw new RuntimeException("Metric " + metricName + " not found");
    }
}