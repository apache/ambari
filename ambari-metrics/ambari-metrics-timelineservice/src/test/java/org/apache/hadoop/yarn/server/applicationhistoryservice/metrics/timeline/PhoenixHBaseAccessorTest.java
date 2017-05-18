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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.DoNotRetryIOException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixConnectionProvider;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.phoenix.exception.PhoenixIOException;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PhoenixTransactSQL.class)
public class PhoenixHBaseAccessorTest {
  private static final String ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";

  @Test
  public void testGetMetricRecords() throws SQLException, IOException {

    Configuration hbaseConf = new Configuration();
    hbaseConf.setStrings(ZOOKEEPER_QUORUM, "quorum");
    Configuration metricsConf = new Configuration();

    PhoenixConnectionProvider connectionProvider = new PhoenixConnectionProvider() {
      @Override
      public HBaseAdmin getHBaseAdmin() throws IOException {
        return null;
      }

      @Override
      public Connection getConnection() throws SQLException {
        return null;
      }
    };

    PhoenixHBaseAccessor accessor = new PhoenixHBaseAccessor(hbaseConf, metricsConf, connectionProvider);

    List<String> metricNames = new LinkedList<>();
    List<String> hostnames = new LinkedList<>();
    Multimap<String, List<Function>> metricFunctions = ArrayListMultimap.create();

    PowerMock.mockStatic(PhoenixTransactSQL.class);
    PreparedStatement preparedStatementMock = EasyMock.createNiceMock(PreparedStatement.class);
    Condition condition = new DefaultCondition(metricNames, hostnames, "appid", "instanceid", 123L, 234L, Precision.SECONDS, 10, true);
    EasyMock.expect(PhoenixTransactSQL.prepareGetMetricsSqlStmt(null, condition)).andReturn(preparedStatementMock).once();
    ResultSet rsMock = EasyMock.createNiceMock(ResultSet.class);
    EasyMock.expect(preparedStatementMock.executeQuery()).andReturn(rsMock);


    PowerMock.replayAll();
    EasyMock.replay(preparedStatementMock, rsMock);

    // Check when startTime < endTime
    TimelineMetrics tml = accessor.getMetricRecords(condition, metricFunctions);

    // Check when startTime > endTime
    Condition condition2 = new DefaultCondition(metricNames, hostnames, "appid", "instanceid", 234L, 123L, Precision.SECONDS, 10, true);
    TimelineMetrics tml2 = accessor.getMetricRecords(condition2, metricFunctions);
    assertEquals(0, tml2.getMetrics().size());

    PowerMock.verifyAll();
    EasyMock.verify(preparedStatementMock, rsMock);
  }

  @Test
  public void testGetMetricRecordsIOException()
    throws SQLException, IOException {

    Configuration hbaseConf = new Configuration();
    hbaseConf.setStrings(ZOOKEEPER_QUORUM, "quorum");
    Configuration metricsConf = new Configuration();

    PhoenixConnectionProvider connectionProvider = new PhoenixConnectionProvider() {
      @Override
      public HBaseAdmin getHBaseAdmin() throws IOException {
        return null;
      }

      @Override
      public Connection getConnection() throws SQLException {
        return null;
      }
    };

    PhoenixHBaseAccessor accessor = new PhoenixHBaseAccessor(hbaseConf, metricsConf, connectionProvider);

    List<String> metricNames = new LinkedList<>();
    List<String> hostnames = new LinkedList<>();
    Multimap<String, List<Function>> metricFunctions = ArrayListMultimap.create();

    PowerMock.mockStatic(PhoenixTransactSQL.class);
    PreparedStatement preparedStatementMock = EasyMock.createNiceMock(PreparedStatement.class);
    Condition condition = new DefaultCondition(metricNames, hostnames, "appid", "instanceid", 123L, 234L, Precision.SECONDS, 10, true);
    EasyMock.expect(PhoenixTransactSQL.prepareGetMetricsSqlStmt(null, condition)).andReturn(preparedStatementMock).once();
    ResultSet rsMock = EasyMock.createNiceMock(ResultSet.class);
    RuntimeException runtimeException = EasyMock.createNiceMock(RuntimeException.class);
    IOException io = EasyMock.createNiceMock(IOException.class);
    EasyMock.expect(preparedStatementMock.executeQuery()).andThrow(runtimeException);
    EasyMock.expect(runtimeException.getCause()).andReturn(io).atLeastOnce();
    StackTraceElement stackTrace[] = new StackTraceElement[]{new StackTraceElement("TimeRange","method","file",1)};
    EasyMock.expect(io.getStackTrace()).andReturn(stackTrace).atLeastOnce();


    PowerMock.replayAll();
    EasyMock.replay(preparedStatementMock, rsMock, io, runtimeException);

    TimelineMetrics tml = accessor.getMetricRecords(condition, metricFunctions);

    assertEquals(0, tml.getMetrics().size());

    PowerMock.verifyAll();
    EasyMock.verify(preparedStatementMock, rsMock, io, runtimeException);
  }

  @Test
  public void testGetMetricRecordsPhoenixIOExceptionDoNotRetryException()
    throws SQLException, IOException {

    Configuration hbaseConf = new Configuration();
    hbaseConf.setStrings(ZOOKEEPER_QUORUM, "quorum");
    Configuration metricsConf = new Configuration();

    PhoenixConnectionProvider connectionProvider = new PhoenixConnectionProvider() {
      @Override
      public HBaseAdmin getHBaseAdmin() throws IOException {
        return null;
      }

      @Override
      public Connection getConnection() throws SQLException {
        return null;
      }
    };

    PhoenixHBaseAccessor accessor = new PhoenixHBaseAccessor(hbaseConf, metricsConf, connectionProvider);

    List<String> metricNames = new LinkedList<>();
    List<String> hostnames = new LinkedList<>();
    Multimap<String, List<Function>> metricFunctions = ArrayListMultimap.create();

    PowerMock.mockStatic(PhoenixTransactSQL.class);
    PreparedStatement preparedStatementMock = EasyMock.createNiceMock(PreparedStatement.class);
    Condition condition = new DefaultCondition(metricNames, hostnames, "appid", "instanceid", null, null, Precision.SECONDS, 10, true);
    EasyMock.expect(PhoenixTransactSQL.prepareGetLatestMetricSqlStmt(null, condition)).andReturn(preparedStatementMock).once();
    PhoenixTransactSQL.setSortMergeJoinEnabled(true);
    EasyMock.expectLastCall();
    ResultSet rsMock = EasyMock.createNiceMock(ResultSet.class);
    PhoenixIOException pioe1 = EasyMock.createNiceMock(PhoenixIOException.class);
    PhoenixIOException pioe2 = EasyMock.createNiceMock(PhoenixIOException.class);
    DoNotRetryIOException dnrioe = EasyMock.createNiceMock(DoNotRetryIOException.class);
    EasyMock.expect(preparedStatementMock.executeQuery()).andThrow(pioe1);
    EasyMock.expect(pioe1.getCause()).andReturn(pioe2).atLeastOnce();
    EasyMock.expect(pioe2.getCause()).andReturn(dnrioe).atLeastOnce();
    StackTraceElement stackTrace[] = new StackTraceElement[]{new StackTraceElement("HashJoinRegionScanner","method","file",1)};
    EasyMock.expect(dnrioe.getStackTrace()).andReturn(stackTrace).atLeastOnce();


    PowerMock.replayAll();
    EasyMock.replay(preparedStatementMock, rsMock, pioe1, pioe2, dnrioe);
    try {
      accessor.getMetricRecords(condition, metricFunctions);
      fail();
    } catch (Exception e) {
      //NOP
    }
    PowerMock.verifyAll();
  }

  @Test
  public void testMetricsCacheCommittingWhenFull() throws IOException, SQLException {
    Configuration hbaseConf = new Configuration();
    hbaseConf.setStrings(ZOOKEEPER_QUORUM, "quorum");
    Configuration metricsConf = new Configuration();
    metricsConf.setStrings(TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_SIZE, "1");
    metricsConf.setStrings(TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_COMMIT_INTERVAL, "100");
    final Connection connection = EasyMock.createNiceMock(Connection.class);


    PhoenixHBaseAccessor accessor = new PhoenixHBaseAccessor(hbaseConf, metricsConf) {
      @Override
      public void commitMetrics(Collection<TimelineMetrics> timelineMetricsCollection) {
        try {
          connection.commit();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    };

    TimelineMetrics timelineMetrics = EasyMock.createNiceMock(TimelineMetrics.class);
    EasyMock.expect(timelineMetrics.getMetrics()).andReturn(Collections.singletonList(new TimelineMetric())).anyTimes();
    connection.commit();
    EasyMock.expectLastCall().once();

    EasyMock.replay(timelineMetrics, connection);

    accessor.insertMetricRecords(timelineMetrics);
    accessor.insertMetricRecords(timelineMetrics);
    accessor.insertMetricRecords(timelineMetrics);

    EasyMock.verify(timelineMetrics, connection);
  }

  @Test
  public void testMetricsAggregatorSink() throws IOException, SQLException {
    Configuration hbaseConf = new Configuration();
    hbaseConf.setStrings(ZOOKEEPER_QUORUM, "quorum");
    Configuration metricsConf = new Configuration();
    Map<TimelineClusterMetric, MetricClusterAggregate> clusterAggregateMap =
        new HashMap<>();
    Map<TimelineClusterMetric, MetricHostAggregate> clusterTimeAggregateMap =
        new HashMap<>();
    Map<TimelineMetric, MetricHostAggregate> hostAggregateMap = new HashMap<>();

    metricsConf.setStrings(
        TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_SIZE, "1");
    metricsConf.setStrings(
        TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_COMMIT_INTERVAL,
        "100");
    metricsConf.setStrings(
        TimelineMetricConfiguration.TIMELINE_METRIC_AGGREGATOR_SINK_CLASS,
        "org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricsAggregatorMemorySink");

    final Connection connection = EasyMock.createNiceMock(Connection.class);
    final PreparedStatement statement =
        EasyMock.createNiceMock(PreparedStatement.class);
    EasyMock.expect(connection.prepareStatement(EasyMock.anyString()))
        .andReturn(statement).anyTimes();
    EasyMock.replay(statement);
    EasyMock.replay(connection);

    PhoenixConnectionProvider connectionProvider =
        new PhoenixConnectionProvider() {
          @Override
          public HBaseAdmin getHBaseAdmin() throws IOException {
            return null;
          }

          @Override
          public Connection getConnection() throws SQLException {
            return connection;
          }
        };

    TimelineClusterMetric clusterMetric =
        new TimelineClusterMetric("metricName", "appId", "instanceId",
            System.currentTimeMillis(), "type");
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("Metric1");
    timelineMetric.setType("type1");
    timelineMetric.setAppId("App1");
    timelineMetric.setInstanceId("instance1");
    timelineMetric.setHostName("host1");

    clusterAggregateMap.put(clusterMetric, new MetricClusterAggregate());
    clusterTimeAggregateMap.put(clusterMetric, new MetricHostAggregate());
    hostAggregateMap.put(timelineMetric, new MetricHostAggregate());

    PhoenixHBaseAccessor accessor =
        new PhoenixHBaseAccessor(hbaseConf, metricsConf, connectionProvider);
    accessor.saveClusterAggregateRecords(clusterAggregateMap);
    accessor.saveHostAggregateRecords(hostAggregateMap,
        PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME);
    accessor.saveClusterTimeAggregateRecords(clusterTimeAggregateMap,
        PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME);

    TimelineMetricsAggregatorMemorySink memorySink =
        new TimelineMetricsAggregatorMemorySink();
    assertEquals(1, memorySink.getClusterAggregateRecords().size());
    assertEquals(1, memorySink.getClusterTimeAggregateRecords().size());
    assertEquals(1, memorySink.getHostAggregateRecords().size());
  }

}
