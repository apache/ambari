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
package org.apache.ambari.metrics.core.timeline.aggregators;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_APP_IDS;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_TIME_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.metrics.core.timeline.AbstractMiniHBaseClusterTest;
import org.apache.ambari.metrics.core.timeline.MetricTestHelper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.ambari.metrics.core.timeline.query.Condition;
import org.apache.ambari.metrics.core.timeline.query.DefaultCondition;
import org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL;
import org.junit.Test;

import junit.framework.Assert;

public class ITClusterAggregator extends AbstractMiniHBaseClusterTest {

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
        getConfigurationForTest(false), metadataManager, null, null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(metadataManager, false);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2), true);
    ctime += 2*minute;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 2), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 1), true);

    // WHEN
    long endTime = ctime + minute + 1;
    boolean success = agg.doWork(startTime, endTime);

    //THEN
    byte[] uuid = metadataManager.getUuid("disk_free", "host", null, null, true);

    Condition condition = new DefaultCondition(Collections.singletonList(uuid), Collections.singletonList("disk_free"), null, "host", null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = readHelper.fromResultSet(rs);
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
    assertTrue(recordCount == 5);
  }

  @Test
  public void testShouldAggregateClusterIgnoringInstance() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hdb,
        getConfigurationForTest(false), metadataManager, null, null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(metadataManager, false);

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
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime - 100, "local1",
      "i1", "disk_free", 1), true);
    // Four 2's at ctime - 100: different host
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime - 100, "local2",
      "i1", "disk_free", 2), true);
    // Avoid overwrite
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime + 100, "local1",
      "i2", "disk_free", 3), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime + 100, "local2",
      "i2", "disk_free", 4), true);

    ctime += minute;

    // Four 1's at ctime + 2 min
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime - 100, "local1",
      "i1", "disk_free", 1), true);
    // Four 1's at ctime + 2 min - different host
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime - 100, "local2",
      "i1", "disk_free", 3), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime + 100, "local1",
      "i2", "disk_free", 2), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime + 100, "local2",
      "i2", "disk_free", 4), true);
    // WHEN
    long endTime = ctime + minute;
    boolean success = agg.doWork(startTime - 1000, endTime + 1000);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime - 1000,
      endTime + 1000, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = readHelper.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        readHelper.getMetricClusterAggregateFromResultSet(rs);

      if (currentMetric == null) {
        continue;
      }
      if ("disk_free".equals(currentMetric.getMetricName())) {
        System.out.println("OUTPUT: " + currentMetric + " - " + currentHostAggregate);
        assertEquals(2, currentHostAggregate.getNumberOfHosts());
        double sum = Math.floor(currentHostAggregate.getSum());
        assertTrue(sum >= 2.0 && sum <= 8);
        recordCount++;
      } else {
        if (!currentMetric.getMetricName().equals("live_hosts")) {
          fail("Unexpected entry");
        }
      }
    }

    Assert.assertEquals(14, recordCount); //Interpolation adds 1 record.
  }

  @Test
  public void testShouldAggregateDifferentMetricsOnClusterProperly() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hdb,
        getConfigurationForTest(false), metadataManager, null, null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(metadataManager, false);

    // here we put some metrics tha will be aggregated
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_used", 1), true);

    ctime += 2*minute;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 2), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 1), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_used", 1), true);

    // WHEN
    long endTime = ctime + minute;
    boolean success = agg.doWork(startTime, endTime);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = readHelper.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        readHelper.getMetricClusterAggregateFromResultSet(rs);

      if (currentMetric == null) {
        continue;
      }
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
        if (!currentMetric.getMetricName().equals("live_hosts")) {
          fail("Unexpected entry");
        }
      }
    }
  }

  @Test
  public void testAggregateDailyClusterMetrics() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorDaily(hdb, getConfigurationForTest(
        false),
        metadataManager,
        null);

    // this time can be virtualized! or made independent from real clock
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long hour = 3600 * 1000;

    Map<TimelineClusterMetric, MetricHostAggregate> records =
      new HashMap<TimelineClusterMetric, MetricHostAggregate>();

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime),
      MetricTestHelper.createMetricHostAggregate(4.0, 0.0, 2, 4.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += hour),
      MetricTestHelper.createMetricHostAggregate(4.0, 0.0, 2, 4.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += hour),
      MetricTestHelper.createMetricHostAggregate(4.0, 0.0, 2, 4.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += hour),
      MetricTestHelper.createMetricHostAggregate(4.0, 0.0, 2, 4.0));


    hdb.saveClusterAggregateRecordsSecond(records, METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME);

    // WHEN
    agg.doWork(startTime - 1000, ctime + hour + 1000);

    // THEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_used"); }},
      null, "test_app", null);

    Condition condition = new DefaultCondition(uuids, new ArrayList<String>() {{ add("disk_used"); }},
      null, "test_app", null, startTime - 1000,
      ctime + hour + 2000, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_TIME_SQL,
      METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int count = 0;
    while (rs.next()) {
      TimelineMetric metric = metadataManager.getMetricFromUuid(rs.getBytes("UUID"));
      assertEquals("METRIC_NAME", "disk_used", metric.getMetricName());
      assertEquals("APP_ID", "test_app", metric.getAppId());
      assertEquals("METRIC_SUM", 4.0, rs.getDouble("METRIC_SUM"));
      assertEquals("METRIC_COUNT", 2, rs.getLong("METRIC_COUNT"));
      assertEquals("METRIC_MAX", 4.0, rs.getDouble("METRIC_MAX"));
      assertEquals("METRIC_MIN", 0.0, rs.getDouble("METRIC_MIN"));
      count++;
    }

    assertEquals("Day aggregated row expected ", 1, count);
  }

  @Test
  public void testShouldAggregateClusterOnMinuteProperly() throws Exception {

    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorMinute(
        hdb,
        getConfigurationForTest(false),
        metadataManager,
        null);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long second = 1000;
    long minute = 60*second;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));

    hdb.saveClusterAggregateRecords(records);
    agg.doWork(startTime, ctime + second);
    long oldCtime = ctime + second;

    //Next minute
    ctime = startTime + minute;

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += second),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));

    hdb.saveClusterAggregateRecords(records);
    agg.doWork(oldCtime, ctime + second);

    ResultSet rs = executeQuery("SELECT * FROM METRIC_AGGREGATE_MINUTE_UUID");
    int count = 0;
    long diff = 0 ;
    while (rs.next()) {
      TimelineMetric metric = metadataManager.getMetricFromUuid(rs.getBytes("UUID"));
      assertEquals("METRIC_NAME", "disk_used", metric.getMetricName());
      assertEquals("APP_ID", "test_app", metric.getAppId());
      assertEquals("METRIC_SUM", 4.0, rs.getDouble("METRIC_SUM"));
      assertEquals("METRIC_COUNT", 2, rs.getLong("METRIC_COUNT"));
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
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(
        hdb,
        getConfigurationForTest(false),
        metadataManager,
        null);

    // this time can be virtualized! or made independent from real clock
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_h", ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_h", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_h", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_h", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));

    hdb.saveClusterAggregateRecords(records);

    // WHEN
    agg.doWork(startTime, ctime + minute);

    // THEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_used_h"); }},
      null, "test_app", null);

    Condition condition = new DefaultCondition(uuids, new ArrayList<String>() {{ add("disk_used_h"); }},
      null, "test_app", null, startTime - 1000,
      ctime + minute + 2000, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_TIME_SQL,
      METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int count = 0;
    while (rs.next()) {
      TimelineMetric metric = metadataManager.getMetricFromUuid(rs.getBytes("UUID"));
      assertEquals("METRIC_NAME", "disk_used_h", metric.getMetricName());
      assertEquals("APP_ID", "test_app", metric.getAppId());
      assertEquals("METRIC_SUM", 4.0, rs.getDouble("METRIC_SUM"));
      assertEquals("METRIC_COUNT", 2, rs.getLong("METRIC_COUNT"));
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
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(
        hdb,
        getConfigurationForTest(false),
        metadataManager,
        null);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_h2", ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_free_h2", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_h2", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_free_h2", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_h2", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_free_h2", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_h2", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_free_h2", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    hdb.saveClusterAggregateRecords(records);

    // WHEN
    agg.doWork(startTime, ctime + minute);

    // THEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_used_h2"); add("disk_free_h2"); }},
      null, "test_app", null);

    Condition condition = new DefaultCondition(uuids, new ArrayList<String>() {{ add("disk_used_h"); add("disk_free_h2");}},
      null, "test_app", null, startTime - 1000,
      ctime + minute + 2000, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_TIME_SQL,
      METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int count = 0;
    while (rs.next()) {
      TimelineMetric metric = metadataManager.getMetricFromUuid(rs.getBytes("UUID"));
      if ("disk_used".equals(metric.getMetricName())) {
        assertEquals("APP_ID", "test_app", metric.getAppId());
        assertEquals("METRIC_SUM", 4.0, rs.getDouble("METRIC_SUM"));
        assertEquals("METRIC_COUNT", 2, rs.getLong("METRIC_COUNT"));
        assertEquals("METRIC_MAX", 4.0, rs.getDouble("METRIC_MAX"));
        assertEquals("METRIC_MIN", 0.0, rs.getDouble("METRIC_MIN"));
      } else if ("disk_free".equals(metric.getMetricName())) {
        assertEquals("APP_ID", "test_app", metric.getAppId());
        assertEquals("METRIC_SUM", 1.0, rs.getDouble("METRIC_SUM"));
        assertEquals("METRIC_COUNT", 2, rs.getLong("METRIC_COUNT"));
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
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(
        hdb,
        conf,
        metadataManager,
        null,
        null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(metadataManager, false);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric((ctime), "local1",
      "app1", null, "app_metric_random", 1), true);
    ctime += 10;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "cpu_user", 1), true);
    ctime += 10;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "cpu_user", 2), true);

    // WHEN
    long endTime = ctime + minute;
    boolean success = agg.doWork(startTime, endTime);

    //THEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("cpu_user"); }},
      null,
      "app1", null);

    Condition condition = new DefaultCondition(uuids,
      new ArrayList<String>() {{ add("cpu_user"); }}, null, "app1", null,
      startTime - 90000, endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    TimelineClusterMetric currentMetric = null;
    MetricClusterAggregate currentHostAggregate = null;
    while (rs.next()) {
      currentMetric = readHelper.fromResultSet(rs);
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
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(
        hdb,
        getConfigurationForTest(false),
        metadataManager,
        null,
        null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(metadataManager, false);

    long currentTime = System.currentTimeMillis();
    // Sample data
    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("yarn.ClusterMetrics.NumActiveNMs");
    metric1.setAppId("resourcemanager");
    metric1.setHostName("h1");
    metric1.setStartTime(currentTime);
    metric1.setMetricValues(new TreeMap<Long, Double>() {{
      put(currentTime + 10000, 1.0);
      put(currentTime + 20000, 1.0);
      put(currentTime + 30000, 1.0);
      put(currentTime + 40000, 1.0);
      put(currentTime + 50000, 1.0);
      put(currentTime + 60000, 1.0);
      put(currentTime + 70000, 1.0);
    }});

    TimelineMetric metric2 = new TimelineMetric();
    metric2.setMetricName("yarn.ClusterMetrics.NumActiveNMs");
    metric2.setAppId("resourcemanager");
    metric2.setHostName("h1");
    metric2.setStartTime(currentTime + 70000);
    metric2.setMetricValues(new TreeMap<Long, Double>() {{
      put(currentTime + 70000, 1.0);
      put(currentTime + 80000, 1.0);
      put(currentTime + 90000, 1.0);
      put(currentTime + 100000, 1.0);
      put(currentTime + 110000, 1.0);
      put(currentTime + 120000, 1.0);
      put(currentTime + 130000, 1.0);
    }});

    TimelineMetrics metrics = new TimelineMetrics();
    metrics.setMetrics(Collections.singletonList(metric1));
    insertMetricRecords(conn, metrics);

    metrics.setMetrics(Collections.singletonList(metric2));
    insertMetricRecords(conn, metrics);

    long startTime = currentTime - 3*60*1000;
    long endTime = currentTime + 3*60*1000;

    agg.doWork(startTime, endTime);

    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("yarn.ClusterMetrics.NumActiveNMs"); }},
      null, "resourcemanager", null);

    Condition condition = new DefaultCondition(uuids,new ArrayList<String>() {{ add("yarn.ClusterMetrics.NumActiveNMs"); }},
      null, "resourcemanager", null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = readHelper.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate = readHelper.getMetricClusterAggregateFromResultSet(rs);

      if ("yarn.ClusterMetrics.NumActiveNMs".equals(currentMetric.getMetricName())) {
        assertEquals(1, currentHostAggregate.getNumberOfHosts());
        assertEquals(1.0, currentHostAggregate.getMax());
        assertEquals(1.0, currentHostAggregate.getMin());
        assertEquals(1.0, currentHostAggregate.getSum());
        recordCount++;
      } else {
        if (!currentMetric.getMetricName().equals("live_hosts")) {
          fail("Unexpected entry");
        }
      }
    }
    Assert.assertEquals(10, recordCount); //With interpolation.
  }

  @Test
  public void testAggregationUsingGroupByQuery() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(
        hdb,
        getConfigurationForTest(true),
        metadataManager,
        null);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_gb", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_free_gb", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_gb", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_free_gb", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_gb", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_free_gb", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_used_gb", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric("disk_free_gb", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    hdb.saveClusterAggregateRecords(records);

    // WHEN
    agg.doWork(startTime, ctime + minute);

    // THEN
    ResultSet rs = executeQuery("SELECT * FROM METRIC_AGGREGATE_HOURLY_UUID");
    int count = 0;
    while (rs.next()) {
      TimelineMetric metric = metadataManager.getMetricFromUuid(rs.getBytes("UUID"));
      if (metric == null) {
        continue;
      }
      if ("disk_used_gb".equals(metric.getMetricName())) {
        assertEquals("APP_ID", "test_app", metric.getAppId());
        assertEquals("METRIC_SUM", 4.0, rs.getDouble("METRIC_SUM"));
        assertEquals("METRIC_COUNT", 2, rs.getLong("METRIC_COUNT"));
        assertEquals("METRIC_MAX", 4.0, rs.getDouble("METRIC_MAX"));
        assertEquals("METRIC_MIN", 0.0, rs.getDouble("METRIC_MIN"));
      } else if ("disk_free_gb".equals(metric.getMetricName())) {
        assertEquals("APP_ID", "test_app", metric.getAppId());
        assertEquals("METRIC_SUM", 1.0, rs.getDouble("METRIC_SUM"));
        assertEquals("METRIC_COUNT", 2, rs.getLong("METRIC_COUNT"));
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
