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
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixTransactSQL.Condition;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixTransactSQL.LOG;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixTransactSQL.NATIVE_TIME_RANGE_DELTA;

public class ITClusterAggregator extends AbstractMiniHBaseClusterTest {
  private Connection conn;
  private PhoenixHBaseAccessor hdb;

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
    Condition condition = new Condition(null, null, null, null, startTime,
      endTime, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA)));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt
      (conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric =
        PhoenixHBaseAccessor.getTimelineMetricClusterKeyFromResultSet(rs);
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
    Condition condition = new Condition(null, null, null, null, startTime,
      endTime, null, true);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA)));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt
      (conn, condition);
    ResultSet rs = pstmt.executeQuery();

    int recordCount = 0;
    while (rs.next()) {
      TimelineClusterMetric currentMetric =
        PhoenixHBaseAccessor.getTimelineMetricClusterKeyFromResultSet(rs);
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

    records.put(createEmptyTimelineMetric(ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineMetric(ctime += minute),
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

    records.put(createEmptyTimelineMetric("disk_used", ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineMetric("disk_free", ctime),
      new MetricClusterAggregate(1.0, 2, 0.0, 1.0, 1.0));

    records.put(createEmptyTimelineMetric("disk_used", ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineMetric("disk_free", ctime),
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

  private TimelineClusterMetric createEmptyTimelineMetric(String name,
                                                          long startTime) {
    TimelineClusterMetric metric = new TimelineClusterMetric(name,
      "test_app", null, startTime, null);

    return metric;
  }

  private TimelineClusterMetric createEmptyTimelineMetric(long startTime) {
    return createEmptyTimelineMetric("disk_used", startTime);
  }

  private MetricHostAggregate
  createMetricHostAggregate(double max, double min, int numberOfSamples,
                            double sum) {
    MetricHostAggregate expectedAggregate =
      new MetricHostAggregate();
    expectedAggregate.setMax(max);
    expectedAggregate.setMin(min);
    expectedAggregate.setNumberOfSamples(numberOfSamples);
    expectedAggregate.setSum(sum);

    return expectedAggregate;
  }

  private PhoenixHBaseAccessor createTestableHBaseAccessor() {
    Configuration metricsConf = new Configuration();
    metricsConf.set(
      TimelineMetricConfiguration.HBASE_COMPRESSION_SCHEME, "NONE");

    return
      new PhoenixHBaseAccessor(
        new Configuration(),
        metricsConf,
        new ConnectionProvider() {
          @Override
          public Connection getConnection() {
            Connection connection = null;
            try {
              connection = DriverManager.getConnection(getUrl());
            } catch (SQLException e) {
              LOG.warn("Unable to connect to HBase store using Phoenix.", e);
            }
            return connection;
          }
        });
  }

  private TimelineMetrics prepareSingleTimelineMetric(long startTime,
                                                      String host,
                                                      String metricName,
                                                      double val) {
    TimelineMetrics m = new TimelineMetrics();
    m.setMetrics(Arrays.asList(
      createTimelineMetric(startTime, metricName, host, val)));

    return m;
  }

  private TimelineMetric createTimelineMetric(long startTime,
                                              String metricName,
                                              String host,
                                              double val) {
    TimelineMetric m = new TimelineMetric();
    m.setAppId("host");
    m.setHostName(host);
    m.setMetricName(metricName);
    m.setStartTime(startTime);
    Map<Long, Double> vals = new HashMap<Long, Double>();
    vals.put(startTime + 15000l, val);
    vals.put(startTime + 30000l, val);
    vals.put(startTime + 45000l, val);
    vals.put(startTime + 60000l, val);

    m.setMetricValues(vals);

    return m;
  }
}