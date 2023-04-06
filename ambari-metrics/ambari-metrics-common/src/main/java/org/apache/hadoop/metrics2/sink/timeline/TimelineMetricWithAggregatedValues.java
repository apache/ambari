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


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "TimelineMetricWithAggregatedValues")
@XmlAccessorType(XmlAccessType.NONE)
public class TimelineMetricWithAggregatedValues {
    private TimelineMetric timelineMetric;
    private MetricHostAggregate metricAggregate;

    public TimelineMetricWithAggregatedValues() {
    }

    public TimelineMetricWithAggregatedValues(TimelineMetric metric, MetricHostAggregate metricAggregate) {
        timelineMetric = metric;
        this.metricAggregate = metricAggregate;
    }

    @XmlElement
    public MetricHostAggregate getMetricAggregate() {
        return metricAggregate;
    }
    @XmlElement
    public TimelineMetric getTimelineMetric() {
        return timelineMetric;
    }

    public void setTimelineMetric(TimelineMetric timelineMetric) {
        this.timelineMetric = timelineMetric;
    }

    public void setMetricAggregate(MetricHostAggregate metricAggregate) {
        this.metricAggregate = metricAggregate;
    }

    @Override
    public String toString() {
        return "TimelineMetricWithAggregatedValues{" +
                "timelineMetric=" + timelineMetric +
                ", metricAggregate=" + metricAggregate +
                '}';
    }
}
