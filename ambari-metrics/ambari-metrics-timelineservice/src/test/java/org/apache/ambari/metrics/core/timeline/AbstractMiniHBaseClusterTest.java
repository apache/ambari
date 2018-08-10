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

import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.OUT_OFF_BAND_DATA_TIME_ALLOWANCE;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.PHOENIX_TABLES;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_METRICS_SQL;
import static org.apache.phoenix.end2end.ParallelStatsDisabledIT.tearDownMiniCluster;
import static org.apache.phoenix.util.TestUtil.TEST_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
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

import javax.annotation.Nonnull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.IntegrationTestingUtility;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.ambari.metrics.core.timeline.aggregators.AggregatorUtils;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.ambari.metrics.core.timeline.query.PhoenixConnectionProvider;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.phoenix.hbase.index.write.IndexWriterUtils;
import org.apache.phoenix.query.BaseTest;
import org.apache.phoenix.query.QueryServices;
import org.apache.phoenix.util.PropertiesUtil;
import org.apache.phoenix.util.ReadOnlyProps;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class AbstractMiniHBaseClusterTest extends BaseTest {

  protected static final long BATCH_SIZE = 3;
  protected Connection conn;
  protected PhoenixHBaseAccessor hdb;
  protected TimelineMetricMetadataManager metadataManager;
  private static StandaloneHBaseTestingUtility utility;

  public final Log LOG;

  public AbstractMiniHBaseClusterTest() {
    LOG = LogFactory.getLog(this.getClass());
  }


  protected static void setUpTestDriver(ReadOnlyProps props) throws Exception {
    setUpTestDriver(props, props);
  }

  protected static void setUpTestDriver(ReadOnlyProps serverProps, ReadOnlyProps clientProps) throws Exception {
    if (driver == null) {
      String url = checkClusterInitialized(serverProps);
      driver = initAndRegisterTestDriver(url, clientProps);
    }
  }

  private static String checkClusterInitialized(ReadOnlyProps serverProps) throws Exception {
    if(!clusterInitialized) {
      url = setUpTestCluster(config, serverProps);
      clusterInitialized = true;
    }

    return url;
  }

  protected static String setUpTestCluster(@Nonnull Configuration conf, ReadOnlyProps overrideProps) throws Exception {
    return initEmbeddedMiniCluster(conf, overrideProps);
  }

  private static String initEmbeddedMiniCluster(Configuration conf, ReadOnlyProps overrideProps) throws Exception {
    setUpConfigForMiniCluster(conf, overrideProps);
    utility = new StandaloneHBaseTestingUtility(conf);

    try {
      utility.startStandaloneHBaseCluster();
      return getLocalClusterUrl(utility);
    } catch (Throwable var3) {
      throw new RuntimeException(var3);
    }
  }

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
    dropNonSystemTables();
    tearDownMiniCluster();
  }

  @Before
  public void setUp() throws Exception {
    Logger.getLogger("org.apache.ambari.metrics.core.timeline").setLevel(Level.DEBUG);
    hdb = createTestableHBaseAccessor();

    //Change default precision table ttl.
    Field f = PhoenixHBaseAccessor.class.getDeclaredField("tableTTL");
    f.setAccessible(true);
    Map<String, Integer> precisionValues = (Map<String, Integer>) f.get(hdb);
    precisionValues.put(METRICS_RECORD_TABLE_NAME, 2 * 86400);
    f.set(hdb, precisionValues);

    // inits connection, starts mini cluster
    conn = getConnection(getUrl());

    Configuration metricsConf = new Configuration();
    metricsConf.set(TimelineMetricConfiguration.HBASE_COMPRESSION_SCHEME, "NONE");

    metadataManager = new TimelineMetricMetadataManager(metricsConf, hdb);
    metadataManager.initializeMetadata();
    hdb.initMetricSchema();
    hdb.setMetadataInstance(metadataManager);
  }

  private void deleteTableIgnoringExceptions(Statement stmt, String tableName) {
    try {
      stmt.execute("delete from " + tableName);
    } catch (Exception e) {
      LOG.warn("Exception on delete table " + tableName, e);
    }
  }

  @After
  public void tearDown() {
    Connection conn = null;
    Statement stmt = null;
    try {
      conn = getConnection(getUrl());
      stmt = conn.createStatement();

      deleteTableIgnoringExceptions(stmt, "METRIC_AGGREGATE_UUID");
      deleteTableIgnoringExceptions(stmt, "METRIC_AGGREGATE_MINUTE_UUID");
      deleteTableIgnoringExceptions(stmt, "METRIC_AGGREGATE_HOURLY_UUID");
      deleteTableIgnoringExceptions(stmt, "METRIC_AGGREGATE_DAILY_UUID");
      deleteTableIgnoringExceptions(stmt, "METRIC_RECORD_UUID");
      deleteTableIgnoringExceptions(stmt, "METRIC_RECORD_MINUTE_UUID");
      deleteTableIgnoringExceptions(stmt, "METRIC_RECORD_HOURLY_UUID");
      deleteTableIgnoringExceptions(stmt, "METRIC_RECORD_DAILY_UUID");
      deleteTableIgnoringExceptions(stmt, "METRICS_METADATA_UUID");
      deleteTableIgnoringExceptions(stmt, "HOSTED_APPS_METADATA_UUID");

      conn.commit();
    } catch (Exception e) {
      LOG.warn("Error on deleting HBase schema.", e);
    }  finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }

      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
    }
  }

  public static Map<String, String> getDefaultProps() {
    Map<String, String> props = new HashMap<String, String>();
    // Must update config before starting server
    props.put(QueryServices.STATS_USE_CURRENT_TIME_ATTRIB,
      Boolean.FALSE.toString());
    props.put("java.security.krb5.realm", "");
    props.put("java.security.krb5.kdc", "");
    props.put(HConstants.REGIONSERVER_PORT, String.valueOf(HBaseTestingUtility.randomFreePort()));
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
    metricsConf.set("timeline.metrics.transient.metric.patterns", "topology%");
    // Unit tests insert values into the future
    metricsConf.setLong(OUT_OFF_BAND_DATA_TIME_ALLOWANCE, 600000);
    metricsConf.set("timeline.metrics." + METRICS_RECORD_TABLE_NAME + ".durability", "SKIP_WAL");
    metricsConf.set("timeline.metrics." + METRICS_CLUSTER_AGGREGATE_TABLE_NAME + ".durability", "ASYNC_WAL");

    return
      new PhoenixHBaseAccessor(new TimelineMetricConfiguration(new Configuration(), metricsConf),
        new PhoenixConnectionProvider() {
          @Override
          public Admin getHBaseAdmin() throws IOException {
            try {
              return driver.getConnectionQueryServices(null, null).getAdmin();
            } catch (SQLException e) {
              LOG.error(e);
            }
            return null;
          }

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

  protected void insertMetricRecords(Connection conn, TimelineMetrics metrics)
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

        byte[] uuid = metadataManager.getUuid(metric, true);
        if (uuid == null) {
          LOG.error("Error computing UUID for metric. Cannot write metrics : " + metric.toString());
          continue;
        }
        metricRecordStmt.setBytes(1, uuid);
        metricRecordStmt.setLong(2, metric.getStartTime());
        metricRecordStmt.setDouble(3, aggregates[0]);
        metricRecordStmt.setDouble(4, aggregates[1]);
        metricRecordStmt.setDouble(5, aggregates[2]);
        metricRecordStmt.setInt(6, (int) aggregates[3]);
        String json = TimelineUtils.dumpTimelineRecordtoJSON(metric.getMetricValues());
        metricRecordStmt.setString(7, json);

        try {
          int row = metricRecordStmt.executeUpdate();
          LOG.info("Inserted " + row + " rows.");
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
    }
  }

  @After
  public void cleanup() throws SQLException {
    for (String table : PHOENIX_TABLES) {
      executeUpdate("DELETE FROM " + table);
    }
  }

  private void executeUpdate(String query) throws SQLException {
    Connection conn = getConnection(getUrl());
    Statement stmt = conn.createStatement();
    stmt.executeUpdate(query);
  }
}