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

import java.util.Map;

public class RawMetricsPublisher extends AbstractMetricPublisher {
    private final Log LOG;

    public RawMetricsPublisher(TimelineMetricsHolder timelineMetricsHolder, Configuration configuration, int interval) {
        super(timelineMetricsHolder, configuration, interval);
        LOG = LogFactory.getLog(this.getClass());
    }


    @Override
    protected Map<String, TimelineMetrics> getMetricsFromCache() {
        return timelineMetricsHolder.extractMetricsForRawPublishing();
    }

    @Override
    protected String processMetrics(Map<String, TimelineMetrics> metricValues) {
        //merge everything in one TimelineMetrics object
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        for (TimelineMetrics metrics : metricValues.values()) {
            for (TimelineMetric timelineMetric : metrics.getMetrics())
                timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
        }
        //map TimelineMetrics to json string
        String json = null;
        try {
            json = mapper.writeValueAsString(timelineMetrics);
            LOG.debug(json);
        } catch (Exception e) {
            LOG.error("Failed to convert result into json", e);
        }
        return json;
    }

    @Override
    protected String getPostUrl() {
        return BASE_POST_URL;
    }
}
