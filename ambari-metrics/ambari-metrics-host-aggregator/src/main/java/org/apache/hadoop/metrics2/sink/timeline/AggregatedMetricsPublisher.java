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



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.host.aggregator.TimelineMetricsHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Thread that aggregates and publishes metrics to collector on specified interval.
 */
public class AggregatedMetricsPublisher extends AbstractMetricPublisher {
    private static String AGGREGATED_POST_PREFIX = "/aggregated";
    private Log LOG;

    public AggregatedMetricsPublisher(TimelineMetricsHolder timelineMetricsHolder, Configuration configuration, int interval) {
        super(timelineMetricsHolder, configuration, interval);
        LOG = LogFactory.getLog(this.getClass());
    }

    /**
     * get metrics map form @TimelineMetricsHolder
     * @return
     */
    @Override
    protected Map<String, TimelineMetrics> getMetricsFromCache() {
        return timelineMetricsHolder.extractMetricsForAggregationPublishing();
    }

    /**
     * Aggregates given metrics and converts them into json string that will be send to collector
     * @param metricForAggregationValues
     * @return
     */
    @Override
    protected String processMetrics(Map<String, TimelineMetrics> metricForAggregationValues) {
        HashMap<String, TimelineMetrics> nameToMetricMap = new HashMap<>();
        for (TimelineMetrics timelineMetrics : metricForAggregationValues.values()) {
            for (TimelineMetric timelineMetric : timelineMetrics.getMetrics()) {
                if (!nameToMetricMap.containsKey(timelineMetric.getMetricName())) {
                    nameToMetricMap.put(timelineMetric.getMetricName(), new TimelineMetrics());
                }
                nameToMetricMap.get(timelineMetric.getMetricName()).addOrMergeTimelineMetric(timelineMetric);
            }
        }
        Set<TimelineMetricWithAggregatedValues> metricAggregateMap = new HashSet<>();
        for (TimelineMetrics metrics : nameToMetricMap.values()) {
            double sum = 0;
            double max = Integer.MIN_VALUE;
            double min = Integer.MAX_VALUE;
            int count = 0;
            for (TimelineMetric metric : metrics.getMetrics()) {
                for (Double value : metric.getMetricValues().values()) {
                    sum+=value;
                    max = Math.max(max, value);
                    min = Math.min(min, value);
                    count++;
                }
            }
            TimelineMetric tmpMetric = new TimelineMetric(metrics.getMetrics().get(0));
            tmpMetric.setMetricValues(new TreeMap<Long, Double>());
            metricAggregateMap.add(new TimelineMetricWithAggregatedValues(tmpMetric, new MetricHostAggregate(sum, count, 0d, max, min)));
        }
        String json = null;
        try {
            json = mapper.writeValueAsString(new AggregationResult(metricAggregateMap, System.currentTimeMillis()));
            LOG.debug(json);
        } catch (Exception e) {
            LOG.error("Failed to convert result into json", e);
        }

        return json;
    }

    @Override
    protected String getPostUrl() {
        return BASE_POST_URL + AGGREGATED_POST_PREFIX;
    }
}
