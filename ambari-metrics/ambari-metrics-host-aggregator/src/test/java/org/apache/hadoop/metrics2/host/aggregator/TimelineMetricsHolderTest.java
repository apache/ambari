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

import junit.framework.Assert;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;


public class TimelineMetricsHolderTest {
    private TimelineMetricsHolder timelineMetricsHolderInstance;

    public void clearHolderSingleton() throws NoSuchFieldException, IllegalAccessException {
        Class timelineMetricHolderClass = TimelineMetricsHolder.class;
        Field field = timelineMetricHolderClass.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(field, null);
    }

    @Test
    public void testGetInstanceDefaultValues() throws Exception {
        clearHolderSingleton();
        Assert.assertNotNull(TimelineMetricsHolder.getInstance());
    }

    @Test
    public void testGetInstanceWithParameters() throws Exception {
        clearHolderSingleton();
        Assert.assertNotNull(TimelineMetricsHolder.getInstance(1,2));
    }

    @Test
    public void testCache() throws Exception {
        clearHolderSingleton();
        timelineMetricsHolderInstance = TimelineMetricsHolder.getInstance(4,4);
        timelineMetricsHolderInstance.putMetricsForAggregationPublishing(getTimelineMetricsWithAppID("aggr"));
        timelineMetricsHolderInstance.putMetricsForRawPublishing(getTimelineMetricsWithAppID("raw"));

        Map<String, TimelineMetrics> aggregationMap =  timelineMetricsHolderInstance.extractMetricsForAggregationPublishing();
        Map<String, TimelineMetrics> rawMap =  timelineMetricsHolderInstance.extractMetricsForRawPublishing();

        Assert.assertEquals(1, aggregationMap.size());
        Assert.assertEquals(1, rawMap.size());

        Collection<TimelineMetrics> aggregationCollection = aggregationMap.values();
        Collection<TimelineMetrics> rawCollection = rawMap.values();

        Collection<String> aggregationCollectionKeys = aggregationMap.keySet();
        Collection<String> rawCollectionKeys = rawMap.keySet();

        for (String key : aggregationCollectionKeys) {
            Assert.assertTrue(key.contains("aggr"));
        }

        for (String key : rawCollectionKeys) {
            Assert.assertTrue(key.contains("raw"));
        }

        Assert.assertEquals(1, aggregationCollection.size());
        Assert.assertEquals(1, rawCollection.size());

        TimelineMetrics aggregationTimelineMetrics = (TimelineMetrics) aggregationCollection.toArray()[0];
        TimelineMetrics rawTimelineMetrics = (TimelineMetrics) rawCollection.toArray()[0];


        Assert.assertEquals(1, aggregationTimelineMetrics.getMetrics().size());
        Assert.assertEquals(1, rawTimelineMetrics.getMetrics().size());

        Assert.assertEquals("aggr", aggregationTimelineMetrics.getMetrics().get(0).getAppId());
        Assert.assertEquals("raw", rawTimelineMetrics.getMetrics().get(0).getAppId());

        aggregationMap =  timelineMetricsHolderInstance.extractMetricsForAggregationPublishing();
        rawMap =  timelineMetricsHolderInstance.extractMetricsForRawPublishing();

        //Cache should be empty after extraction
        Assert.assertEquals(0, aggregationMap.size());
        Assert.assertEquals(0, rawMap.size());
    }

    public static TimelineMetrics getTimelineMetricsWithAppID(String appId) {
        TimelineMetric timelineMetric = new TimelineMetric();
        timelineMetric.setAppId(appId);
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
        return timelineMetrics;
    }
}