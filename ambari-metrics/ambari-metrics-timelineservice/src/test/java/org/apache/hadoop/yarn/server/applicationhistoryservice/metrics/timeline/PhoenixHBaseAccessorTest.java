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
import org.apache.hadoop.hbase.util.RetryCounterFactory;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixConnectionProvider;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.phoenix.exception.PhoenixIOException;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PhoenixTransactSQL.class, TimelineMetricConfiguration.class})
public class PhoenixHBaseAccessorTest {
  private static final String ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";

  PhoenixConnectionProvider connectionProvider;
  PhoenixHBaseAccessor accessor;

  @Before
  public void setupConf() throws Exception {
    Configuration hbaseConf = new Configuration();
    hbaseConf.setStrings(ZOOKEEPER_QUORUM, "quorum");
    Configuration metricsConf = new Configuration();
    metricsConf.setStrings(TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_SIZE, "1");
    metricsConf.setStrings(TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_COMMIT_INTERVAL, "100");
    metricsConf.setStrings(
      TimelineMetricConfiguration.TIMELINE_METRIC_AGGREGATOR_SINK_CLASS,
      "org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricsAggregatorMemorySink");

    TimelineMetricConfiguration conf = new TimelineMetricConfiguration(hbaseConf, metricsConf);
    mockStatic(TimelineMetricConfiguration.class);
    expect(TimelineMetricConfiguration.getInstance()).andReturn(conf).anyTimes();
    replayAll();

    connectionProvider = new PhoenixConnectionProvider() {
      @Override
      public HBaseAdmin getHBaseAdmin() throws IOException {
        return null;
      }

      @Override
      public Connection getConnection() throws SQLException {
        return null;
      }

      };

    accessor = new PhoenixHBaseAccessor(connectionProvider);
  }

  @Test
  public void testGetMetricRecords() throws SQLException, IOException {
    List<String> metricNames = new LinkedList<>();
    List<String> hostnames = new LinkedList<>();
    Multimap<String, List<Function>> metricFunctions = ArrayListMultimap.create();

    mockStatic(PhoenixTransactSQL.class);
    PreparedStatement preparedStatementMock = EasyMock.createNiceMock(PreparedStatement.class);
    Condition condition = new DefaultCondition(metricNames, hostnames, "appid", "instanceid", 123L, 234L, Precision.SECONDS, 10, true);
    expect(PhoenixTransactSQL.prepareGetMetricsSqlStmt(null, condition)).andReturn(preparedStatementMock).once();
    ResultSet rsMock = EasyMock.createNiceMock(ResultSet.class);
    expect(preparedStatementMock.executeQuery()).andReturn(rsMock);


    replayAll();
    EasyMock.replay(preparedStatementMock, rsMock);

    // Check when startTime < endTime
    TimelineMetrics tml = accessor.getMetricRecords(condition, metricFunctions);

    // Check when startTime > endTime
    Condition condition2 = new DefaultCondition(metricNames, hostnames, "appid", "instanceid", 234L, 123L, Precision.SECONDS, 10, true);
    TimelineMetrics tml2 = accessor.getMetricRecords(condition2, metricFunctions);
    assertEquals(0, tml2.getMetrics().size());

    verifyAll();
    EasyMock.verify(preparedStatementMock, rsMock);
  }

  @Test
  public void testGetMetricRecordsIOException() throws SQLException, IOException {
    List<String> metricNames = new LinkedList<>();
    List<String> hostnames = new LinkedList<>();
    Multimap<String, List<Function>> metricFunctions = ArrayListMultimap.create();

    mockStatic(PhoenixTransactSQL.class);
    PreparedStatement preparedStatementMock = EasyMock.createNiceMock(PreparedStatement.class);
    Condition condition = new DefaultCondition(metricNames, hostnames, "appid", "instanceid", 123L, 234L, Precision.SECONDS, 10, true);
    expect(PhoenixTransactSQL.prepareGetMetricsSqlStmt(null, condition)).andReturn(preparedStatementMock).once();
    ResultSet rsMock = EasyMock.createNiceMock(ResultSet.class);
    RuntimeException runtimeException = EasyMock.createNiceMock(RuntimeException.class);
    IOException io = EasyMock.createNiceMock(IOException.class);
    expect(preparedStatementMock.executeQuery()).andThrow(runtimeException);
    expect(runtimeException.getCause()).andReturn(io).atLeastOnce();
    StackTraceElement stackTrace[] = new StackTraceElement[]{new StackTraceElement("TimeRange","method","file",1)};
    expect(io.getStackTrace()).andReturn(stackTrace).atLeastOnce();


    replayAll();
    EasyMock.replay(preparedStatementMock, rsMock, io, runtimeException);

    TimelineMetrics tml = accessor.getMetricRecords(condition, metricFunctions);

    assertEquals(0, tml.getMetrics().size());

    verifyAll();
    EasyMock.verify(preparedStatementMock, rsMock, io, runtimeException);
  }

  @Test
  public void testGetMetricRecordsPhoenixIOExceptionDoNotRetryException() throws SQLException, IOException {
    List<String> metricNames = new LinkedList<>();
    List<String> hostnames = new LinkedList<>();
    Multimap<String, List<Function>> metricFunctions = ArrayListMultimap.create();

    mockStatic(PhoenixTransactSQL.class);
    PreparedStatement preparedStatementMock = EasyMock.createNiceMock(PreparedStatement.class);
    Condition condition = new DefaultCondition(metricNames, hostnames, "appid", "instanceid", null, null, Precision.SECONDS, 10, true);
    expect(PhoenixTransactSQL.prepareGetLatestMetricSqlStmt(null, condition)).andReturn(preparedStatementMock).once();
    PhoenixTransactSQL.setSortMergeJoinEnabled(true);
    EasyMock.expectLastCall();
    ResultSet rsMock = EasyMock.createNiceMock(ResultSet.class);
    PhoenixIOException pioe1 = EasyMock.createNiceMock(PhoenixIOException.class);
    PhoenixIOException pioe2 = EasyMock.createNiceMock(PhoenixIOException.class);
    DoNotRetryIOException dnrioe = EasyMock.createNiceMock(DoNotRetryIOException.class);
    expect(preparedStatementMock.executeQuery()).andThrow(pioe1);
    expect(pioe1.getCause()).andReturn(pioe2).atLeastOnce();
    expect(pioe2.getCause()).andReturn(dnrioe).atLeastOnce();
    StackTraceElement stackTrace[] = new StackTraceElement[]{new StackTraceElement("HashJoinRegionScanner","method","file",1)};
    expect(dnrioe.getStackTrace()).andReturn(stackTrace).atLeastOnce();


    replayAll();
    EasyMock.replay(preparedStatementMock, rsMock, pioe1, pioe2, dnrioe);
    try {
      accessor.getMetricRecords(condition, metricFunctions);
      fail();
    } catch (Exception e) {
      //NOP
    }
    verifyAll();
  }

  @Test
  public void testMetricsCacheCommittingWhenFull() throws IOException, SQLException {
    Configuration hbaseConf = new Configuration();
    hbaseConf.setStrings(ZOOKEEPER_QUORUM, "quorum");

    final Connection connection = EasyMock.createNiceMock(Connection.class);

    accessor = new PhoenixHBaseAccessor(connectionProvider) {
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
    expect(timelineMetrics.getMetrics()).andReturn(Collections.singletonList(new TimelineMetric())).anyTimes();
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
    Map<TimelineClusterMetric, MetricClusterAggregate> clusterAggregateMap =
        new HashMap<>();
    Map<TimelineClusterMetric, MetricHostAggregate> clusterTimeAggregateMap =
        new HashMap<>();
    Map<TimelineMetric, MetricHostAggregate> hostAggregateMap = new HashMap<>();


    final Connection connection = EasyMock.createNiceMock(Connection.class);
    final PreparedStatement statement = EasyMock.createNiceMock(PreparedStatement.class);
    expect(connection.prepareStatement(EasyMock.anyString())).andReturn(statement).anyTimes();
    EasyMock.replay(statement);
    EasyMock.replay(connection);

    connectionProvider = new PhoenixConnectionProvider() {

      @Override
      public HBaseAdmin getHBaseAdmin() throws IOException {
        return null;
      }

      @Override
      public Connection getConnection() throws SQLException {
        return connection;
      }
    };

    accessor = new PhoenixHBaseAccessor(connectionProvider);

    TimelineClusterMetric clusterMetric =
        new TimelineClusterMetric("metricName", "appId", "instanceId",
            System.currentTimeMillis());
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("Metric1");
    timelineMetric.setType("type1");
    timelineMetric.setAppId("App1");
    timelineMetric.setInstanceId("instance1");
    timelineMetric.setHostName("host1");

    clusterAggregateMap.put(clusterMetric, new MetricClusterAggregate());
    clusterTimeAggregateMap.put(clusterMetric, new MetricHostAggregate());
    hostAggregateMap.put(timelineMetric, new MetricHostAggregate());

    TimelineMetricMetadataManager metricMetadataManagerMock = EasyMock.createMock(TimelineMetricMetadataManager.class);
    expect(metricMetadataManagerMock.getUuid(anyObject(TimelineClusterMetric.class))).andReturn(new byte[16]).times(2);
    expect(metricMetadataManagerMock.getUuid(anyObject(TimelineMetric.class))).andReturn(new byte[20]).once();
    replay(metricMetadataManagerMock);

    accessor.setMetadataInstance(metricMetadataManagerMock);
    accessor.saveClusterAggregateRecords(clusterAggregateMap);
    accessor.saveHostAggregateRecords(hostAggregateMap,
        PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME);
    accessor.saveClusterAggregateRecordsSecond(clusterTimeAggregateMap,
        PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME);

    TimelineMetricsAggregatorMemorySink memorySink =
        new TimelineMetricsAggregatorMemorySink();
    assertEquals(1, memorySink.getClusterAggregateRecords().size());
    assertEquals(1, memorySink.getClusterTimeAggregateRecords().size());
    assertEquals(1, memorySink.getHostAggregateRecords().size());
  }

}
