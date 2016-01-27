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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TestTimelineMetricStore implements TimelineMetricStore {
  @Override
  public TimelineMetrics getTimelineMetrics(List<String> metricNames,
      List<String> hostnames, String applicationId, String instanceId, Long startTime,
      Long endTime, Precision precision, Integer limit, boolean groupedByHost) throws SQLException,
    IOException {
    TimelineMetrics timelineMetrics = new TimelineMetrics();
    List<TimelineMetric> metricList = new ArrayList<TimelineMetric>();
    timelineMetrics.setMetrics(metricList);
    TimelineMetric metric1 = new TimelineMetric();
    TimelineMetric metric2 = new TimelineMetric();
    metricList.add(metric1);
    metricList.add(metric2);
    metric1.setMetricName("cpu_user");
    metric1.setAppId("1");
    metric1.setInstanceId(null);
    metric1.setHostName("c6401");
    metric1.setStartTime(1407949812L);
    metric1.setMetricValues(new TreeMap<Long, Double>() {{
      put(1407949812L, 1.0d);
      put(1407949912L, 1.8d);
      put(1407950002L, 0.7d);
    }});

    metric2.setMetricName("mem_free");
    metric2.setAppId("2");
    metric2.setInstanceId("3");
    metric2.setHostName("c6401");
    metric2.setStartTime(1407949812L);
    metric2.setMetricValues(new TreeMap<Long, Double>() {{
      put(1407949812L, 2.5d);
      put(1407949912L, 3.0d);
      put(1407950002L, 0.9d);
    }});

    return timelineMetrics;
  }

  @Override
  public TimelineMetric getTimelineMetric(String metricName, List<String> hostname,
      String applicationId, String instanceId, Long startTime, Long endTime,
      Precision precision, Integer limit) throws SQLException, IOException {

    return null;
  }

  @Override
  public TimelinePutResponse putMetrics(TimelineMetrics metrics)
      throws SQLException, IOException {

    return new TimelinePutResponse();
  }

  @Override
  public Map<String, List<TimelineMetricMetadata>> getTimelineMetricMetadata() throws SQLException, IOException {
    return null;
  }

  @Override
  public Map<String, Set<String>> getHostAppsMetadata() throws SQLException, IOException {
    return Collections.emptyMap();
  }
}
