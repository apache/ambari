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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement(name="AggregationResult")
public class AggregationResult {
    protected Set<TimelineMetricWithAggregatedValues> result;
    protected Long timeInMilis;

    @Override
    public String toString() {
        return "AggregationResult{" +
                "result=" + result +
                ", timeInMilis=" + timeInMilis +
                '}';
    }

    public AggregationResult() {
    }

    public AggregationResult(Set<TimelineMetricWithAggregatedValues> result, Long timeInMilis) {
        this.result = result;
        this.timeInMilis = timeInMilis;
    }
    @XmlElement
    public Set<TimelineMetricWithAggregatedValues> getResult() {
        return result;
    }

    public void setResult(Set<TimelineMetricWithAggregatedValues> result) {
        this.result = result;
    }
    @XmlElement
    public Long getTimeInMilis() {
        return timeInMilis;
    }

    public void setTimeInMilis(Long timeInMilis) {
        this.timeInMilis = timeInMilis;
    }
}
