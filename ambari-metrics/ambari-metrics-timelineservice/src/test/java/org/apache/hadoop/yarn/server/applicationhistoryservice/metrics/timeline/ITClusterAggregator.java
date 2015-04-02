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


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricClusterAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetricReader;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricClusterAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricClusterAggregatorHourly;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.junit.After;
import org.junit.Assert;
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
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.createEmptyTimelineClusterMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.prepareSingleTimelineMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_APP_IDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.NATIVE_TIME_RANGE_DELTA;

public class ITClusterAggregator extends AbstractMiniHBaseClusterTest {
  private Connection conn;
  private PhoenixHBaseAccessor hdb;
  private final TimelineClusterMetricReader metricReader = new
    TimelineClusterMetricReader(false);

  @Before
  public void setUp() throws Exception {
    hdb = createTestableHBaseAccessor();
    // inits connection, starts mini cluster
    conn = getConnection(getUrl());

    hdb.initMetricSchema();
  }

  @After
  public void tearDown() throws Exception {
    Connection conn = getConnection(getUrl());
    Statement stmt = conn.createStatement();

    stmt.execute("delete from METRIC_AGGREGATE");
    stmt.execute("delete from METRIC_AGGREGATE_HOURLY");
    stmt.execute("delete from METRIC_RECORD");
    stmt.execute("delete from METRIC_RECORD_HOURLY");
    stmt.execute("delete from METRIC_RECORD_MINUTE");
    conn.commit();

    stmt.close();
    conn.close();
  }

  @Test
  public void testShouldAggregateClusterProperly() throws Exception {
    // GIVEN
    TimelineMetricClusterAggregator agg =
      new TimelineMetricClusterAggregator(hdb, new Configuration());

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2));
    ctime += minute;
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

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt
      (conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = metricReader.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        PhoenixHBaseAccessor.getMetricClusterAggregateFromResultSet(rs);

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
    TimelineMetricClusterAggregator agg =
      new TimelineMetricClusterAggregator(hdb, new Configuration());

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    /**
     * Here we have two nodes with two instances each:
     *              | local1 | local2 |
     *  instance i1 |   1    |   2    |
     *  instance i2 |   3    |   4    |
     *
     */
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "i1", "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "i1", "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "i2", "disk_free", 3));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "i2", "disk_free", 4));
    ctime += minute;

    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "i1", "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "i1", "disk_free", 3));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "i2", "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "i2", "disk_free", 4));
    // WHEN
    long endTime = ctime + minute;
    boolean success = agg.doWork(startTime, endTime);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA),
      METRICS_CLUSTER_AGGREGATE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt
      (conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = metricReader.fromResultSet(rs);
//        PhoenixHBaseAccessor.getTimelineMetricClusterKeyFromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        PhoenixHBaseAccessor.getMetricClusterAggregateFromResultSet(rs);

      if ("disk_free".equals(currentMetric.getMetricName())) {
        System.out.println("OUTPUT: " + currentMetric+" - " +
          ""+currentHostAggregate);
        assertEquals(4, currentHostAggregate.getNumberOfHosts());
        assertEquals(4.0, currentHostAggregate.getMax());
        assertEquals(1.0, currentHostAggregate.getMin());
        assertEquals(10.0, currentHostAggregate.getSum());
        recordCount++;
      } else {
        fail("Unexpected entry");
      }
    }
  }

  @Test
  public void testShouldAggregateDifferentMetricsOnClusterProperly()
    throws Exception {
    // GIVEN
    TimelineMetricClusterAggregator agg =
      new TimelineMetricClusterAggregator(hdb, new Configuration());

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

    ctime += minute;
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

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt
      (conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric = metricReader.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        PhoenixHBaseAccessor.getMetricClusterAggregateFromResultSet(rs);

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
  public void testShouldAggregateClusterOnHourProperly() throws Exception {
    // GIVEN
    TimelineMetricClusterAggregatorHourly agg =
      new TimelineMetricClusterAggregatorHourly(hdb, new Configuration());

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
  public void testShouldAggregateDifferentMetricsOnHourProperly() throws
    Exception {
    // GIVEN
    TimelineMetricClusterAggregatorHourly agg =
      new TimelineMetricClusterAggregatorHourly(hdb, new Configuration());

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
    Configuration conf = new Configuration();
    conf.set(CLUSTER_AGGREGATOR_APP_IDS, "app1");
    TimelineMetricClusterAggregator agg = new TimelineMetricClusterAggregator(hdb, conf);

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
      currentHostAggregate = PhoenixHBaseAccessor.getMetricClusterAggregateFromResultSet(rs);
      recordCount++;
    }
    assertEquals(4, recordCount);
    assertNotNull(currentMetric);
    assertEquals("cpu_user", currentMetric.getMetricName());
    assertEquals("app1", currentMetric.getAppId());
    assertNotNull(currentHostAggregate);
    assertEquals(1, currentHostAggregate.getNumberOfHosts());
    assertEquals(1.0d, currentHostAggregate.getSum());
  }

  private ResultSet executeQuery(String query) throws SQLException {
    Connection conn = getConnection(getUrl());
    Statement stmt = conn.createStatement();
    return stmt.executeQuery(query);
  }
}