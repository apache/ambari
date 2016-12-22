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

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.junit.Test;
import java.net.URL;

public class TimelineMetricsFilterTest {

  @Test
  public void testAppBlacklisting() {

    Configuration metricsConf = new Configuration();
    metricsConf.set("timeline.metrics.apps.blacklist", "hbase,datanode,nimbus");
    TimelineMetricsFilter.initializeMetricFilter(metricsConf);

    TimelineMetric timelineMetric = new TimelineMetric();

    timelineMetric.setAppId("hbase");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setAppId("namenode");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setAppId("nimbus");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));
  }

  @Test
  public void testMetricWhitelisting() throws Exception {

    Configuration metricsConf = new Configuration();
    URL fileUrl = ClassLoader.getSystemResource("test_data/metric_whitelist.dat");

    metricsConf.set("timeline.metrics.whitelist.file", fileUrl.getPath());
    TimelineMetricsFilter.initializeMetricFilter(metricsConf);

    TimelineMetric timelineMetric = new TimelineMetric();

    timelineMetric.setMetricName("cpu_system");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("cpu_system1");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("jvm.JvmMetrics.MemHeapUsedM");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("dfs.FSNamesystem.TotalFiles");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));
  }

  @Test
  public void testTogether() throws Exception {

    Configuration metricsConf = new Configuration();
    metricsConf.set("timeline.metrics.apps.blacklist", "hbase,datanode,nimbus");

    URL fileUrl = ClassLoader.getSystemResource("test_data/metric_whitelist.dat");
    metricsConf.set("timeline.metrics.whitelist.file", fileUrl.getPath());

    TimelineMetricsFilter.initializeMetricFilter(metricsConf);

    TimelineMetric timelineMetric = new TimelineMetric();

    timelineMetric.setMetricName("cpu_system");
    timelineMetric.setAppId("hbase");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("cpu_system");
    timelineMetric.setAppId("HOST");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("jvm.JvmMetrics.MemHeapUsedM");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("dfs.FSNamesystem.TotalFiles");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));
  }

}
