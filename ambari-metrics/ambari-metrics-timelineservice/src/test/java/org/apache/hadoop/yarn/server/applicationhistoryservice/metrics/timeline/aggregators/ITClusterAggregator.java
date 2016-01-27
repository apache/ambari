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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;


import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.AbstractMiniHBaseClusterTest;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricClusterAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricHostAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregatorFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricReadHelper;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.createEmptyTimelineClusterMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.prepareSingleTimelineMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_APP_IDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.NATIVE_TIME_RANGE_DELTA;

public class ITClusterAggregator extends AbstractMiniHBaseClusterTest {
  private final TimelineMetricReadHelper metricReader = new TimelineMetricReadHelper(false);

  private Configuration getConfigurationForTest(boolean useGroupByAggregators) {
    Configuration configuration = new Configuration();
    configuration.set("timeline.metrics.service.use.groupBy.aggregators", String.valueOf(useGroupByAggregators));
    return configuration;
  }

  @Test
  public void testShouldAggregateClusterProperly() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hdb,
        getConfigurationForTest(false), new TimelineMetricMetadataManager(hdb, new Configuration()));
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2));
    ctime += 2*minute;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 1));

    // WHEN
    long endTime = ctime + minute;
    boolean success = agg.doWork(startTime, endTime);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA),
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = metricReader.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        readHelper.getMetricClusterAggregateFromResultSet(rs);

      if ("disk_free".equals(currentMetric.getMetricName())) {
        assertEquals(2, currentHostAggregate.getNumberOfHosts());
        assertEquals(2.0, currentHostAggregate.getMax());
        assertEquals(1.0, currentHostAggregate.getMin());
        assertEquals(3.0, currentHostAggregate.getSum());
        recordCount++;
      } else {
        fail("Unexpected entry");
      }
    }
  }

  @Test
  public void testShouldAggregateClusterIgnoringInstance() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hdb,
        getConfigurationForTest(false), new TimelineMetricMetadataManager(hdb, new Configuration()));
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000 * 2;

    /**
     * Here we have two nodes with two instances each:
     *              | local1 | local2 |
     *  instance i1 |   1    |   2    |
     *  instance i2 |   3    |   4    |
     *
     */
    // Four 1's at ctime - 100
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime - 100, "local1",
      "i1", "disk_free", 1));
    // Four 2's at ctime - 100: different host
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime - 100, "local2",
      "i1", "disk_free", 2));
    // Avoid overwrite
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime + 100, "local1",
      "i2", "disk_free", 3));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime + 100, "local2",
      "i2", "disk_free", 4));

    ctime += minute;

    // Four 1's at ctime + 2 min
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime - 100, "local1",
      "i1", "disk_free", 1));
    // Four 1's at ctime + 2 min - different host
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime - 100, "local2",
      "i1", "disk_free", 3));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime + 100, "local1",
      "i2", "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime + 100, "local2",
      "i2", "disk_free", 4));
    // WHEN
    long endTime = ctime + minute;
    boolean success = agg.doWork(startTime - 1000, endTime + 1000);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA),
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = metricReader.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        readHelper.getMetricClusterAggregateFromResultSet(rs);

      if ("disk_free".equals(currentMetric.getMetricName())) {
        System.out.println("OUTPUT: " + currentMetric + " - " + currentHostAggregate);
        assertEquals(2, currentHostAggregate.getNumberOfHosts());
        assertEquals(5.0, Math.floor(currentHostAggregate.getSum()));
        recordCount++;
      } else {
        fail("Unexpected entry");
      }
    }

    Assert.assertEquals(5, recordCount);
  }

  @Test
  public void testShouldAggregateDifferentMetricsOnClusterProperly() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hdb,
        getConfigurationForTest(false), new TimelineMetricMetadataManager(hdb, new Configuration()));
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);

    // here we put some metrics tha will be aggregated
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_used", 1));

    ctime += 2*minute;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_used", 1));

    // WHEN
    long endTime = ctime + minute;
    boolean success = agg.doWork(startTime, endTime);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA),
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = metricReader.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        readHelper.getMetricClusterAggregateFromResultSet(rs);

      if ("disk_free".equals(currentMetric.getMetricName())) {
        assertEquals(2, currentHostAggregate.getNumberOfHosts());
        assertEquals(2.0, currentHostAggregate.getMax());
        assertEquals(1.0, currentHostAggregate.getMin());
        assertEquals(3.0, currentHostAggregate.getSum());
        recordCount++;
      } else if ("disk_used".equals(currentMetric.getMetricName())) {
        assertEquals(1, currentHostAggregate.getNumberOfHosts());
        assertEquals(1.0, currentHostAggregate.getMax());
        assertEquals(1.0, currentHostAggregate.getMin());
        assertEquals(1.0, currentHostAggregate.getSum());
        recordCount++;
      } else {
        fail("Unexpected entry");
      }
    }
  }

  @Test
  public void testAggregateDailyClusterMetrics() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorDaily(hdb, getConfigurationForTest(false));

    // this time can be virtualized! or made independent from real clock
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long hour = 3600 * 1000;

    Map<TimelineClusterMetric, MetricHostAggregate> records =
      new HashMap<TimelineClusterMetric, MetricHostAggregate>();

    records.put(createEmptyTimelineClusterMetric(ctime),
      MetricTestHelper.createMetricHostAggregate(4.0, 0.0, 2, 4.0));
    records.put(createEmptyTimelineClusterMetric(ctime += hour),
      MetricTestHelper.createMetricHostAggregate(4.0, 0.0, 2, 4.0));
    records.put(createEmptyTimelineClusterMetric(ctime += hour),
      MetricTestHelper.createMetricHostAggregate(4.0, 0.0, 2, 4.0));
    records.put(createEmptyTimelineClusterMetric(ctime += hour),
      MetricTestHelper.createMetricHostAggregate(4.0, 0.0, 2, 4.0));


    hdb.saveClusterTimeAggregateRecords(records, METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME);

    // WHEN
    agg.doWork(startTime, ctime + hour + 1000);

    // THEN
    ResultSet rs = executeQuery("SELECT * FROM METRIC_AGGREGATE_DAILY");
    int count = 0;
    while (rs.next()) {
      assertEquals("METRIC_NAME", "disk_used", rs.getString("METRIC_NAME"));
      assertEquals("APP_ID", "test_app", rs.getString("APP_ID"));
      assertEquals("METRIC_SUM", 16.0, rs.getDouble("METRIC_SUM"));
      assertEquals("METRIC_COUNT", 8, rs.getLong("METRIC_COUNT"));
      assertEquals("METRIC_MAX", 4.0, rs.getDouble("METRIC_MAX"));
      assertEquals("METRIC_MIN", 0.0, rs.getDouble("METRIC_MIN"));
      count++;
    }

    assertEquals("Day aggregated row expected ", 1, count);
  }

  @Test
  public void testShouldAggregateClusterOnMinuteProperly() throws Exception {

    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorMinute(hdb, getConfigurationForTest(false));

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long second = 1000;
    long minute = 60*second;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(createEmptyTimelineClusterMetric(ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));

    hdb.saveClusterAggregateRecords(records);
    agg.doWork(startTime, ctime + second);
    long oldCtime = ctime + second;

    //Next minute
    ctime = startTime + minute;

    records.put(createEmptyTimelineClusterMetric(ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));

    hdb.saveClusterAggregateRecords(records);
    agg.doWork(oldCtime, ctime + second);

    ResultSet rs = executeQuery("SELECT * FROM METRIC_AGGREGATE_MINUTE");
    int count = 0;
    long diff = 0 ;
    while (rs.next()) {
      assertEquals("METRIC_NAME", "disk_used", rs.getString("METRIC_NAME"));
      assertEquals("APP_ID", "test_app", rs.getString("APP_ID"));
      assertEquals("METRIC_SUM", 16.0, rs.getDouble("METRIC_SUM"));
      assertEquals("METRIC_COUNT", 8, rs.getLong("METRIC_COUNT"));
      assertEquals("METRIC_MAX", 4.0, rs.getDouble("METRIC_MAX"));
      assertEquals("METRIC_MIN", 0.0, rs.getDouble("METRIC_MIN"));
      if (count == 0) {
        diff+=rs.getLong("SERVER_TIME");
      } else {
        diff-=rs.getLong("SERVER_TIME");
        if (diff < 0) {
          diff*=-1;
        }
        assertTrue(diff == minute);
      }
      count++;
    }

    assertEquals("One hourly aggregated row expected ", 2, count);
  }

  @Test
  public void testShouldAggregateClusterOnHourProperly() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(hdb, getConfigurationForTest(false));

    // this time can be virtualized! or made independent from real clock
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(createEmptyTimelineClusterMetric(ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));

    hdb.saveClusterAggregateRecords(records);

    // WHEN
    agg.doWork(startTime, ctime + minute);

    // THEN
    ResultSet rs = executeQuery("SELECT * FROM METRIC_AGGREGATE_HOURLY");
    int count = 0;
    while (rs.next()) {
      assertEquals("METRIC_NAME", "disk_used", rs.getString("METRIC_NAME"));
      assertEquals("APP_ID", "test_app", rs.getString("APP_ID"));
      assertEquals("METRIC_SUM", 16.0, rs.getDouble("METRIC_SUM"));
      assertEquals("METRIC_COUNT", 8, rs.getLong("METRIC_COUNT"));
      assertEquals("METRIC_MAX", 4.0, rs.getDouble("METRIC_MAX"));
      assertEquals("METRIC_MIN", 0.0, rs.getDouble("METRIC_MIN"));
      count++;
    }

    assertEquals("One hourly aggregated row expected ", 1, count);
  }

  @Test
  public void testShouldAggregateDifferentMetricsOnHourProperly() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(hdb, getConfigurationForTest(false));

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(createEmptyTimelineClusterMetric("disk_used", ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineClusterMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineClusterMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineClusterMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    hdb.saveClusterAggregateRecords(records);

    // WHEN
    agg.doWork(startTime, ctime + minute);

    // THEN
    ResultSet rs = executeQuery("SELECT * FROM METRIC_AGGREGATE_HOURLY");
    int count = 0;
    while (rs.next()) {
      if ("disk_used".equals(rs.getString("METRIC_NAME"))) {
        assertEquals("APP_ID", "test_app", rs.getString("APP_ID"));
        assertEquals("METRIC_SUM", 16.0, rs.getDouble("METRIC_SUM"));
        assertEquals("METRIC_COUNT", 8, rs.getLong("METRIC_COUNT"));
        assertEquals("METRIC_MAX", 4.0, rs.getDouble("METRIC_MAX"));
        assertEquals("METRIC_MIN", 0.0, rs.getDouble("METRIC_MIN"));
      } else if ("disk_free".equals(rs.getString("METRIC_NAME"))) {
        assertEquals("APP_ID", "test_app", rs.getString("APP_ID"));
        assertEquals("METRIC_SUM", 4.0, rs.getDouble("METRIC_SUM"));
        assertEquals("METRIC_COUNT", 8, rs.getLong("METRIC_COUNT"));
        assertEquals("METRIC_MAX", 1.0, rs.getDouble("METRIC_MAX"));
        assertEquals("METRIC_MIN", 1.0, rs.getDouble("METRIC_MIN"));
      }

      count++;
    }

    assertEquals("Two hourly aggregated row expected ", 2, count);
  }

  @Test
  public void testAppLevelHostMetricAggregates() throws Exception {
    Configuration conf = getConfigurationForTest(false);
    conf.set(CLUSTER_AGGREGATOR_APP_IDS, "app1");
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hdb,
        conf, new TimelineMetricMetadataManager(hdb, new Configuration()));
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareSingleTimelineMetric((ctime), "local1",
      "app1", null, "app_metric_random", 1));
    ctime += 10;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "cpu_user", 1));
    ctime += 10;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "cpu_user", 2));

    // WHEN
    long endTime = ctime + minute;
    boolean success = agg.doWork(startTime, endTime);

    //THEN
    Condition condition = new DefaultCondition(
      Collections.singletonList("cpu_user"), null, "app1", null,
      startTime, endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA),
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt
      (conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    TimelineClusterMetric currentMetric = null;
    MetricClusterAggregate currentHostAggregate = null;
    while (rs.next()) {
      currentMetric = metricReader.fromResultSet(rs);
      currentHostAggregate = readHelper.getMetricClusterAggregateFromResultSet(rs);
      recordCount++;
    }
    assertEquals(3, recordCount);
    assertNotNull(currentMetric);
    assertEquals("cpu_user", currentMetric.getMetricName());
    assertEquals("app1", currentMetric.getAppId());
    assertNotNull(currentHostAggregate);
    assertEquals(1, currentHostAggregate.getNumberOfHosts());
    assertEquals(1.0d, currentHostAggregate.getSum());
  }

  @Test
  public void testClusterAggregateMetricNormalization() throws Exception {
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hdb,
        getConfigurationForTest(false), new TimelineMetricMetadataManager(hdb, new Configuration()));
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);

    // Sample data
    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("yarn.ClusterMetrics.NumActiveNMs");
    metric1.setAppId("resourcemanager");
    metric1.setHostName("h1");
    metric1.setStartTime(1431372311811l);
    metric1.setMetricValues(new TreeMap<Long, Double>() {{
      put(1431372311811l, 1.0);
      put(1431372321811l, 1.0);
      put(1431372331811l, 1.0);
      put(1431372341811l, 1.0);
      put(1431372351811l, 1.0);
      put(1431372361811l, 1.0);
      put(1431372371810l, 1.0);
    }});

    TimelineMetric metric2 = new TimelineMetric();
    metric2.setMetricName("yarn.ClusterMetrics.NumActiveNMs");
    metric2.setAppId("resourcemanager");
    metric2.setHostName("h1");
    metric2.setStartTime(1431372381810l);
    metric2.setMetricValues(new TreeMap<Long, Double>() {{
      put(1431372381810l, 1.0);
      put(1431372391811l, 1.0);
      put(1431372401811l, 1.0);
      put(1431372411811l, 1.0);
      put(1431372421811l, 1.0);
      put(1431372431811l, 1.0);
      put(1431372441810l, 1.0);
    }});

    TimelineMetrics metrics = new TimelineMetrics();
    metrics.setMetrics(Collections.singletonList(metric1));
    insertMetricRecords(conn, metrics, 1431372371810l);

    metrics.setMetrics(Collections.singletonList(metric2));
    insertMetricRecords(conn, metrics, 1431372441810l);

    long startTime = 1431372055000l;
    long endTime = 1431372655000l;

    agg.doWork(startTime, endTime);

    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA),
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = metricReader.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate = readHelper.getMetricClusterAggregateFromResultSet(rs);

      if ("yarn.ClusterMetrics.NumActiveNMs".equals(currentMetric.getMetricName())) {
        assertEquals(1, currentHostAggregate.getNumberOfHosts());
        assertEquals(1.0, currentHostAggregate.getMax());
        assertEquals(1.0, currentHostAggregate.getMin());
        assertEquals(1.0, currentHostAggregate.getSum());
        recordCount++;
      } else {
        fail("Unexpected entry");
      }
    }
    Assert.assertEquals(5, recordCount);
  }

  @Test
  public void testAggregationUsingGroupByQuery() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(hdb, getConfigurationForTest(true));

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(createEmptyTimelineClusterMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineClusterMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineClusterMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineClusterMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    hdb.saveClusterAggregateRecords(records);

    // WHEN
    agg.doWork(startTime, ctime + minute);

    // THEN
    ResultSet rs = executeQuery("SELECT * FROM METRIC_AGGREGATE_HOURLY");
    int count = 0;
    while (rs.next()) {
      if ("disk_used".equals(rs.getString("METRIC_NAME"))) {
        assertEquals("APP_ID", "test_app", rs.getString("APP_ID"));
        assertEquals("METRIC_SUM", 16.0, rs.getDouble("METRIC_SUM"));
        assertEquals("METRIC_COUNT", 8, rs.getLong("METRIC_COUNT"));
        assertEquals("METRIC_MAX", 4.0, rs.getDouble("METRIC_MAX"));
        assertEquals("METRIC_MIN", 0.0, rs.getDouble("METRIC_MIN"));
      } else if ("disk_free".equals(rs.getString("METRIC_NAME"))) {
        assertEquals("APP_ID", "test_app", rs.getString("APP_ID"));
        assertEquals("METRIC_SUM", 4.0, rs.getDouble("METRIC_SUM"));
        assertEquals("METRIC_COUNT", 8, rs.getLong("METRIC_COUNT"));
        assertEquals("METRIC_MAX", 1.0, rs.getDouble("METRIC_MAX"));
        assertEquals("METRIC_MIN", 1.0, rs.getDouble("METRIC_MIN"));
      }
      count++;
    }
    assertEquals("Two hourly aggregated row expected ", 2, count);
  }

  private ResultSet executeQuery(String query) throws SQLException {
    Connection conn = getConnection(getUrl());
    Statement stmt = conn.createStatement();
    return stmt.executeQuery(query);
  }
}
