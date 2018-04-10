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
package org.apache.hadoop.metrics2.sink.timeline;

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.host.aggregator.TimelineMetricsHolder;
import org.apache.hadoop.metrics2.host.aggregator.TimelineMetricsHolderTest;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class RawMetricsPublisherTest {
    @Test
    public void testProcessMetrics() throws Exception {
        Configuration configuration = new Configuration();
        TimelineMetricsHolder timelineMetricsHolder = TimelineMetricsHolder.getInstance();

        timelineMetricsHolder.extractMetricsForAggregationPublishing();
        timelineMetricsHolder.extractMetricsForRawPublishing();

        TreeMap<Long, Double> metric1App1Metrics = new TreeMap<>();
        metric1App1Metrics.put(1L, 1d);
        metric1App1Metrics.put(2L, 2d);
        metric1App1Metrics.put(3L, 3d);
        timelineMetricsHolder.putMetricsForRawPublishing(getTimelineMetricsForAppId("metricName1", "app1", metric1App1Metrics));

        TreeMap<Long, Double> metric2App2Metrics = new TreeMap<>();
        metric2App2Metrics.put(1L, 4d);
        metric2App2Metrics.put(2L, 5d);
        metric2App2Metrics.put(3L, 6d);
        timelineMetricsHolder.putMetricsForRawPublishing(getTimelineMetricsForAppId("metricName2", "app2", metric2App2Metrics));

        TreeMap<Long, Double> metric3App3Metrics = new TreeMap<>();
        metric3App3Metrics.put(1L, 7d);
        metric3App3Metrics.put(2L, 8d);
        metric3App3Metrics.put(3L, 9d);

        timelineMetricsHolder.putMetricsForRawPublishing(getTimelineMetricsForAppId("metricName3", "app3", metric3App3Metrics));


        RawMetricsPublisher rawMetricsPublisher =
                new RawMetricsPublisher(TimelineMetricsHolder.getInstance(), configuration, 60);

        String rawJson = rawMetricsPublisher.processMetrics(timelineMetricsHolder.extractMetricsForRawPublishing());
        String expectedResult = "{\"metrics\":[{\"timestamp\":0,\"metadata\":{},\"metricname\":\"metricName1\",\"appid\":\"app1\",\"starttime\":0,\"metrics\":{\"1\":1.0,\"2\":2.0,\"3\":3.0}},{\"timestamp\":0,\"metadata\":{},\"metricname\":\"metricName2\",\"appid\":\"app2\",\"starttime\":0,\"metrics\":{\"1\":4.0,\"2\":5.0,\"3\":6.0}},{\"timestamp\":0,\"metadata\":{},\"metricname\":\"metricName3\",\"appid\":\"app3\",\"starttime\":0,\"metrics\":{\"1\":7.0,\"2\":8.0,\"3\":9.0}}]}";
        Assert.assertNotNull(rawJson);
        Assert.assertEquals(expectedResult, rawJson);
    }

    @Test
    public void testGetPostUrl() {
        Configuration configuration = new Configuration();
        RawMetricsPublisher rawMetricsPublisher =
                new RawMetricsPublisher(TimelineMetricsHolder.getInstance(), configuration, 1);
        String actualURL = rawMetricsPublisher.getPostUrl();
        String expectedURL = "%s://%s:%s/ws/v1/timeline/metrics";
        Assert.assertNotNull(actualURL);
        Assert.assertEquals(expectedURL, actualURL);
    }

    @Test
    public void testGetCollectorUri() {
        //default configuration
        Configuration configuration = new Configuration();
        AbstractMetricPublisher rawMetricsPublisher =
                new RawMetricsPublisher(TimelineMetricsHolder.getInstance(), configuration, 1);
        String actualURL = rawMetricsPublisher.getCollectorUri("c6401.ambari.apache.org");
        String expectedURL = "http://c6401.ambari.apache.org:6188/ws/v1/timeline/metrics";
        Assert.assertNotNull(actualURL);
        Assert.assertEquals(expectedURL, actualURL);

        //https configuration
        configuration = new Configuration();
        configuration.set("timeline.metrics.service.http.policy", "HTTPS_ONLY");
        rawMetricsPublisher =
                new RawMetricsPublisher(TimelineMetricsHolder.getInstance(), configuration, 1);
        actualURL = rawMetricsPublisher.getCollectorUri("c6402.ambari.apache.org");
        expectedURL = "https://c6402.ambari.apache.org:6188/ws/v1/timeline/metrics";
        Assert.assertNotNull(actualURL);
        Assert.assertEquals(expectedURL, actualURL);

        //custom port configuration
        configuration = new Configuration();
        configuration.set("timeline.metrics.service.webapp.address", "0.0.0.0:8888");
        rawMetricsPublisher =
                new RawMetricsPublisher(TimelineMetricsHolder.getInstance(), configuration, 1);
        actualURL = rawMetricsPublisher.getCollectorUri("c6403.ambari.apache.org");
        expectedURL = "http://c6403.ambari.apache.org:8888/ws/v1/timeline/metrics";
        Assert.assertNotNull(actualURL);
        Assert.assertEquals(expectedURL, actualURL);
    }

    @Test
    public void testGetMetricsFromCache() throws InterruptedException {

        TimelineMetricsHolder timelineMetricsHolder = TimelineMetricsHolder.getInstance(4,4);
        timelineMetricsHolder.extractMetricsForAggregationPublishing();
        timelineMetricsHolder.extractMetricsForRawPublishing();

        timelineMetricsHolder.putMetricsForRawPublishing(TimelineMetricsHolderTest.getTimelineMetricsWithAppID("raw1"));
        timelineMetricsHolder.putMetricsForAggregationPublishing(TimelineMetricsHolderTest.getTimelineMetricsWithAppID("aggr"));
        timelineMetricsHolder.putMetricsForRawPublishing(TimelineMetricsHolderTest.getTimelineMetricsWithAppID("raw2"));

        Configuration configuration = new Configuration();
        RawMetricsPublisher rawMetricsPublisher =
                new RawMetricsPublisher(TimelineMetricsHolder.getInstance(), configuration, 1);

        Map<String, TimelineMetrics> metricsFromCache = rawMetricsPublisher.getMetricsFromCache();
        Assert.assertNotNull(metricsFromCache);
        Collection<TimelineMetrics> actualTimelineMetrics = metricsFromCache.values();
        Assert.assertNotNull(actualTimelineMetrics);
        Assert.assertEquals(2, actualTimelineMetrics.size());

        for (TimelineMetrics timelineMetrics : actualTimelineMetrics) {
            List<TimelineMetric> metrics = timelineMetrics.getMetrics();
            Assert.assertEquals(1, metrics.size());
            Assert.assertTrue(metrics.get(0).getAppId().contains("raw"));
        }

    }

    TimelineMetrics getTimelineMetricsForAppId(String metricName, String appId, TreeMap<Long, Double> metricValues) {
        TimelineMetric timelineMetric = new TimelineMetric();
        timelineMetric.setMetricName(metricName);
        timelineMetric.setAppId(appId);
        timelineMetric.setMetricValues(metricValues);
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
        return timelineMetrics;
    }
}
