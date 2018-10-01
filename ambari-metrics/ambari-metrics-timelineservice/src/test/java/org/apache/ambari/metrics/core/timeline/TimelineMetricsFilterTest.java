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
package org.apache.ambari.metrics.core.timeline;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.easymock.EasyMock;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class TimelineMetricsFilterTest {

  @Test
  public void testAppBlacklisting() throws Exception{

    Configuration metricsConf = new Configuration();
    metricsConf.set("timeline.metrics.apps.blacklist", "hbase,datanode,nimbus");
    TimelineMetricConfiguration configuration = EasyMock.createNiceMock(TimelineMetricConfiguration.class);
    expect(configuration.getMetricsConf()).andReturn(metricsConf).once();
    replay(configuration);

    TimelineMetricsFilter.initializeMetricFilter(configuration);

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
    TimelineMetricConfiguration configuration = EasyMock.createNiceMock(TimelineMetricConfiguration.class);
    expect(configuration.getMetricsConf()).andReturn(metricsConf).once();
    expect(configuration.isWhitelistingEnabled()).andReturn(true).anyTimes();
    replay(configuration);

    metricsConf.set("timeline.metrics.whitelist.file", getTestWhitelistFilePath());
    TimelineMetricsFilter.initializeMetricFilter(configuration);

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
  public void testMetricBlacklisting() throws Exception {

    Configuration metricsConf = new Configuration();
    TimelineMetricConfiguration configuration = EasyMock.createNiceMock(TimelineMetricConfiguration.class);
    expect(configuration.getMetricsConf()).andReturn(metricsConf).once();
    replay(configuration);

    metricsConf.set("timeline.metrics.blacklist.file", getTestBlacklistFilePath());
    TimelineMetricsFilter.initializeMetricFilter(configuration);

    TimelineMetric timelineMetric = new TimelineMetric();

    timelineMetric.setMetricName("cpu_system");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("cpu_idle");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));
  }


  @Test
  public void testTogether() throws Exception {

    Configuration metricsConf = new Configuration();
    metricsConf.set("timeline.metrics.apps.blacklist", "hbase,datanode,nimbus");
    TimelineMetricConfiguration configuration = EasyMock.createNiceMock(TimelineMetricConfiguration.class);
    expect(configuration.getMetricsConf()).andReturn(metricsConf).once();
    replay(configuration);

    metricsConf.set("timeline.metrics.whitelist.file", getTestWhitelistFilePath());

    TimelineMetricsFilter.initializeMetricFilter(configuration);

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

  @Test
  public void testAmshbaseWhitelisting() throws Exception {

    TimelineMetricConfiguration configuration = EasyMock.createNiceMock(TimelineMetricConfiguration.class);

    Configuration metricsConf = new Configuration();
    expect(configuration.getMetricsConf()).andReturn(metricsConf).once();

    Set<String> whitelist = new HashSet();
    whitelist.add("regionserver.Server.Delete_99th_percentile");
    whitelist.add("regionserver.Server.Delete_max");
    whitelist.add("regionserver.Server.Delete_mean");
    expect(configuration.getAmshbaseWhitelist()).andReturn(whitelist).once();

    replay(configuration);

    TimelineMetricsFilter.initializeMetricFilter(configuration);

    TimelineMetric timelineMetric = new TimelineMetric();

    timelineMetric.setMetricName("regionserver.Server.Delete_max");
    timelineMetric.setAppId("ams-hbase");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("regionserver.Server.Delete_min3333");
    timelineMetric.setAppId("ams-hbase");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("jvm.JvmMetrics.MemHeapUsedM");
    timelineMetric.setAppId("hbase");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));
  }

  @Test
  public void testHybridFilter() throws Exception {

    // Whitelist Apps - namenode, nimbus
    // Blacklist Apps - datanode, kafka_broker
    // Accept ams-hbase whitelisting.
    // Reject non whitelisted metrics from non whitelisted Apps (Say hbase)

    TimelineMetricConfiguration configuration = EasyMock.createNiceMock(TimelineMetricConfiguration.class);

    Configuration metricsConf = new Configuration();
    metricsConf.set("timeline.metrics.apps.whitelist", "namenode,nimbus");
    metricsConf.set("timeline.metrics.apps.blacklist", "datanode,kafka_broker");
    metricsConf.set("timeline.metrics.whitelist.file", getTestWhitelistFilePath());
    metricsConf.set("timeline.metrics.blacklist.file", getTestBlacklistFilePath());
    expect(configuration.getMetricsConf()).andReturn(metricsConf).once();

    Set<String> whitelist = new HashSet<>();
    whitelist.add("regionserver.Server.Delete_99th_percentile");
    whitelist.add("regionserver.Server.Delete_max");
    whitelist.add("regionserver.Server.Delete_mean");
    expect(configuration.getAmshbaseWhitelist()).andReturn(whitelist).once();

    expect(configuration.isWhitelistingEnabled()).andReturn(true).anyTimes();

    replay(configuration);

    TimelineMetricsFilter.initializeMetricFilter(configuration);

    TimelineMetric timelineMetric = new TimelineMetric();


    //Test Metric Blacklisting
    timelineMetric.setMetricName("cpu_idle");
    timelineMetric.setAppId("namenode");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("jvm.HeapMetrics.m1");
    timelineMetric.setAppId("nimbus");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));

    //Test App Whitelisting
    timelineMetric.setMetricName("metric.a.b.c");
    timelineMetric.setAppId("namenode");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("metric.d.e.f");
    timelineMetric.setAppId("nimbus");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    //Test App Blacklisting
    timelineMetric.setMetricName("metric.d.e.f");
    timelineMetric.setAppId("datanode");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("metric.d.e.f");
    timelineMetric.setAppId("kafka_broker");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));


    //Test ams-hbase Whitelisting
    timelineMetric.setMetricName("regionserver.Server.Delete_max");
    timelineMetric.setAppId("ams-hbase");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("regionserver.Server.Delete_min3333");
    timelineMetric.setAppId("ams-hbase");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("regionserver.Server.Delete_mean");
    timelineMetric.setAppId("ams-hbase");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    //Test Metric Whitelisting
    timelineMetric.setMetricName("regionserver.WAL.SyncTime_max");
    timelineMetric.setAppId("hbase");
    Assert.assertTrue(TimelineMetricsFilter.acceptMetric(timelineMetric));

    timelineMetric.setMetricName("regionserver.WAL.metric.not.needed");
    timelineMetric.setAppId("hbase");
    Assert.assertFalse(TimelineMetricsFilter.acceptMetric(timelineMetric));
  }

  private static String getTestWhitelistFilePath() throws URISyntaxException {
    return ClassLoader.getSystemResource("test_data/metric_whitelist.dat").toURI().getPath();
  }

  private static String getTestBlacklistFilePath() throws URISyntaxException {
    return ClassLoader.getSystemResource("test_data/metric_blacklist.dat").toURI().getPath();
  }
}
