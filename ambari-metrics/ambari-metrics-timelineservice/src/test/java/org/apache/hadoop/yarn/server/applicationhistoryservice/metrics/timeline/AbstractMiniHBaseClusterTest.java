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
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.IntegrationTestingUtility;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.ConnectionProvider;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;
import org.apache.phoenix.hbase.index.write.IndexWriterUtils;
import org.apache.phoenix.query.BaseTest;
import org.apache.phoenix.query.QueryServices;
import org.apache.phoenix.util.PropertiesUtil;
import org.apache.phoenix.util.ReadOnlyProps;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.LOG;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_METRICS_SQL;
import static org.apache.phoenix.util.TestUtil.TEST_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractMiniHBaseClusterTest extends BaseTest {

  protected static final long BATCH_SIZE = 3;

  @BeforeClass
  public static void doSetup() throws Exception {
    Map<String, String> props = getDefaultProps();
    props.put(IntegrationTestingUtility.IS_DISTRIBUTED_CLUSTER, "false");
    props.put(QueryServices.QUEUE_SIZE_ATTRIB, Integer.toString(5000));
    props.put(IndexWriterUtils.HTABLE_THREAD_KEY, Integer.toString(100));
    // Make a small batch size to test multiple calls to reserve sequences
    props.put(QueryServices.SEQUENCE_CACHE_SIZE_ATTRIB, Long.toString(BATCH_SIZE));
    // Must update config before starting server
    setUpTestDriver(new ReadOnlyProps(props.entrySet().iterator()));
  }

  @AfterClass
  public static void doTeardown() throws Exception {
    dropAllTables();
  }

  @After
  public void cleanUpAfterTest() throws Exception {
    deletePriorTables(HConstants.LATEST_TIMESTAMP, getUrl());
  }

  public static Map<String, String> getDefaultProps() {
    Map<String, String> props = new HashMap<String, String>();
    // Must update config before starting server
    props.put(QueryServices.STATS_USE_CURRENT_TIME_ATTRIB,
      Boolean.FALSE.toString());
    props.put("java.security.krb5.realm", "");
    props.put("java.security.krb5.kdc", "");
    return props;
  }

  protected Connection getConnection(String url) throws SQLException {
    Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
    Connection conn = DriverManager.getConnection(getUrl(), props);
    return conn;
  }

  /**
   * A canary test. Will show if the infrastructure is set-up correctly.
   */
  @Test
  public void testClusterOK() throws Exception {
    Connection conn = getConnection(getUrl());
    conn.setAutoCommit(true);

    String sampleDDL = "CREATE TABLE TEST_METRICS " +
      "(TEST_COLUMN VARCHAR " +
      "CONSTRAINT pk PRIMARY KEY (TEST_COLUMN)) " +
      "DATA_BLOCK_ENCODING='FAST_DIFF', IMMUTABLE_ROWS=true, " +
      "TTL=86400, COMPRESSION='NONE' ";

    Statement stmt = conn.createStatement();
    stmt.executeUpdate(sampleDDL);
    conn.commit();

    ResultSet rs = stmt.executeQuery(
      "SELECT COUNT(TEST_COLUMN) FROM TEST_METRICS");

    rs.next();
    long l = rs.getLong(1);
    assertThat(l).isGreaterThanOrEqualTo(0);

    stmt.execute("DROP TABLE TEST_METRICS");
    conn.close();
  }

  protected PhoenixHBaseAccessor createTestableHBaseAccessor() {
    Configuration metricsConf = new Configuration();
    metricsConf.set(TimelineMetricConfiguration.HBASE_COMPRESSION_SCHEME, "NONE");

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

  protected void insertMetricRecords(Connection conn, TimelineMetrics metrics, long currentTime)
                                    throws SQLException, IOException {

    List<TimelineMetric> timelineMetrics = metrics.getMetrics();
    if (timelineMetrics == null || timelineMetrics.isEmpty()) {
      LOG.debug("Empty metrics insert request.");
      return;
    }

    PreparedStatement metricRecordStmt = null;

    try {
      metricRecordStmt = conn.prepareStatement(String.format(
        UPSERT_METRICS_SQL, METRICS_RECORD_TABLE_NAME));

      for (TimelineMetric metric : timelineMetrics) {
        metricRecordStmt.clearParameters();

        if (LOG.isTraceEnabled()) {
          LOG.trace("host: " + metric.getHostName() + ", " +
            "metricName = " + metric.getMetricName() + ", " +
            "values: " + metric.getMetricValues());
        }
        double[] aggregates =  AggregatorUtils.calculateAggregates(
          metric.getMetricValues());

        metricRecordStmt.setString(1, metric.getMetricName());
        metricRecordStmt.setString(2, metric.getHostName());
        metricRecordStmt.setString(3, metric.getAppId());
        metricRecordStmt.setString(4, metric.getInstanceId());
        metricRecordStmt.setLong(5, currentTime);
        metricRecordStmt.setLong(6, metric.getStartTime());
        metricRecordStmt.setString(7, metric.getType());
        metricRecordStmt.setDouble(8, aggregates[0]);
        metricRecordStmt.setDouble(9, aggregates[1]);
        metricRecordStmt.setDouble(10, aggregates[2]);
        metricRecordStmt.setLong(11, (long) aggregates[3]);
        String json = TimelineUtils.dumpTimelineRecordtoJSON(metric.getMetricValues());
        metricRecordStmt.setString(12, json);

        try {
          metricRecordStmt.executeUpdate();
        } catch (SQLException sql) {
          LOG.error(sql);
        }
      }

      conn.commit();

    } finally {
      if (metricRecordStmt != null) {
        try {
          metricRecordStmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException sql) {
          // Ignore
        }
      }
    }
  }
}