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

import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.PrecisionLimitExceededException;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.SplitByMetricNamesCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.TopNCondition;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import org.easymock.EasyMock;

public class TestPhoenixTransactSQL {
  @Test
  public void testConditionClause() throws Exception {
    Condition condition = new DefaultCondition(Arrays.asList(new byte[8], new byte[8]),
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, null, null, false);

    String preparedClause = condition.getConditionClause().toString();
    String expectedClause = "(UUID IN (?, ?)) AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);
  }

  @Test
  public void testSplitByMetricNamesCondition() throws Exception {
    Condition c = new DefaultCondition(Arrays.asList(new byte[8], new byte[8]),
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, null, null, false);

    SplitByMetricNamesCondition condition = new SplitByMetricNamesCondition(c);
    condition.setCurrentUuid(new byte[8]);

    String preparedClause = condition.getConditionClause().toString();
    String expectedClause = "UUID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);
  }

  @Ignore
  @Test
  public void testLikeConditionClause() throws Exception {
    Condition condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "some=%.metric")),
      Collections.singletonList("h1"), "a1", "i1", 1407959718L, 1407959918L,
      null, null, false);

    String preparedClause = condition.getConditionClause().toString();
    String expectedClause = "(METRIC_NAME IN (?) OR METRIC_NAME LIKE ?) AND HOSTNAME = ? AND " +
        "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);


    condition = new DefaultCondition(
        Collections.<String>emptyList(), Collections.singletonList("h1"), "a1", "i1",
        1407959718L, 1407959918L, null, null, false);

    preparedClause = condition.getConditionClause().toString();
    expectedClause = " HOSTNAME = ? AND " +
        "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);


    condition = new DefaultCondition(
        null, Collections.singletonList("h1"), "a1", "i1",
        1407959718L, 1407959918L, null, null, false);

    preparedClause = condition.getConditionClause().toString();
    expectedClause = " HOSTNAME = ? AND " +
        "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);


    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("some=%.metric")), Collections.singletonList("h1"), "a1", "i1",
        1407959718L, 1407959918L, null, null, false);

    preparedClause = condition.getConditionClause().toString();
    expectedClause = "(METRIC_NAME LIKE ?) AND HOSTNAME = ? AND " +
        "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);


    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("some=%.metric1", "some=%.metric2", "some=%.metric3")),
      Collections.singletonList("h1"), "a1", "i1",
      1407959718L, 1407959918L, null, null, false);

    preparedClause = condition.getConditionClause().toString();
    expectedClause = "(METRIC_NAME LIKE ? OR METRIC_NAME LIKE ? OR METRIC_NAME LIKE ?) AND HOSTNAME = ? AND " +
        "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);
  }

  @Test
  public void testPrepareGetAggregatePrecisionMINUTES() throws SQLException {
    Condition condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, Precision.MINUTES, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetAggregateSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_AGGREGATE"));
    verify(connection, preparedStatement);
  }

  @Test
  public void testPrepareGetAggregateNoPrecision() throws SQLException {
    Long endTime = 1407959918L;
    Long startTime = 1407959718L;
    //SECONDS precision
    Condition condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetAggregateSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_AGGREGATE"));
    Assert.assertEquals(Precision.SECONDS, condition.getPrecision());
    verify(connection, preparedStatement);

    // MINUTES precision
    startTime = endTime-PhoenixTransactSQL.DAY/1000;
    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetAggregateSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_AGGREGATE"));
    Assert.assertEquals(Precision.MINUTES, condition.getPrecision());
    verify(connection, preparedStatement);

    // HOURS precision
    startTime = endTime-PhoenixTransactSQL.DAY*30/1000;
    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetAggregateSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_AGGREGATE_HOURLY"));
    Assert.assertEquals(Precision.HOURS, condition.getPrecision());
    verify(connection, preparedStatement);

    // DAYS precision
    startTime = endTime-PhoenixTransactSQL.DAY*30*2/1000;
    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetAggregateSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_AGGREGATE_DAILY"));
    Assert.assertEquals(Precision.DAYS, condition.getPrecision());
    verify(connection, preparedStatement);
  }

  @Test
  public void testPrepareGetAggregatePrecisionHours() throws SQLException {
    Condition condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, Precision.HOURS, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetAggregateSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_AGGREGATE_HOURLY"));
    verify(connection, preparedStatement);
  }

  @Test
  public void testPrepareGetMetricsPrecisionMinutes() throws SQLException {
    Condition condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, Precision.MINUTES, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD_MINUTE"));
    verify(connection, preparedStatement);
  }

  @Test
  public void testPrepareGetMetricsNoPrecision() throws SQLException {
    Long endTime = 1407959918L;
    Long startTime = endTime - 200;
    // SECONDS precision
    Condition condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD"));
    Assert.assertEquals(Precision.SECONDS, condition.getPrecision());
    verify(connection, preparedStatement);
    reset(connection, preparedStatement);

    // SECONDS precision
    startTime = endTime-PhoenixTransactSQL.HOUR*2/1000;
    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
      .andReturn(preparedStatement);
    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD"));
    Assert.assertEquals(Precision.SECONDS, condition.getPrecision());
    verify(connection, preparedStatement);

    // MINUTES precision
    startTime = endTime-PhoenixTransactSQL.DAY/1000;
    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
      .andReturn(preparedStatement);
    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD_MINUTE"));
    Assert.assertEquals(Precision.MINUTES, condition.getPrecision());
    verify(connection, preparedStatement);

    // HOURS precision
    startTime = endTime-PhoenixTransactSQL.DAY*30/1000;
    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
      .andReturn(preparedStatement);
    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD_HOURLY"));
    Assert.assertEquals(Precision.HOURS, condition.getPrecision());
    verify(connection, preparedStatement);

    // DAYS precision
    startTime = endTime-PhoenixTransactSQL.DAY*30*2/1000;
    condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", startTime, endTime, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
      .andReturn(preparedStatement);
    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD_DAILY"));
    Assert.assertEquals(Precision.DAYS, condition.getPrecision());
    verify(connection, preparedStatement);

  }

  @Test
  public void testPrepareGetLatestMetricSqlStmtMultipleHostNames() throws SQLException {
    Condition condition = new DefaultCondition(Arrays.asList(new byte[16], new byte[16], new byte[16], new byte[16]),
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Arrays.asList("h1", "h2"),
      "a1", "i1", null, null, null, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    ParameterMetaData parameterMetaData = createNiceMock(ParameterMetaData.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);
    expect(preparedStatement.getParameterMetaData())
      .andReturn(parameterMetaData).once();
    // 6 = 1 instance_id + 1 appd_id + 2 hostnames + 2 metric names
    expect(parameterMetaData.getParameterCount())
      .andReturn(4).once();

    replay(connection, preparedStatement, parameterMetaData);
    PhoenixTransactSQL.prepareGetLatestMetricSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD"));
    Assert.assertTrue(stmt.contains("JOIN"));
    verify(connection, preparedStatement, parameterMetaData);
  }

  @Test
  public void testPrepareGetLatestMetricSqlStmtSortMergeJoinAlgorithm()
    throws SQLException {
    Condition condition = new DefaultCondition(Arrays.asList(new byte[16], new byte[16]),
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Arrays.asList("h1"),
      "a1", "i1", null, null, null, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    ParameterMetaData parameterMetaData = createNiceMock(ParameterMetaData.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);
    expect(preparedStatement.getParameterMetaData())
      .andReturn(parameterMetaData).anyTimes();
    expect(parameterMetaData.getParameterCount())
      .andReturn(2).anyTimes();

    replay(connection, preparedStatement, parameterMetaData);
    PhoenixTransactSQL.setSortMergeJoinEnabled(true);
    PhoenixTransactSQL.prepareGetLatestMetricSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("/*+ USE_SORT_MERGE_JOIN NO_CACHE */"));
  }

  @Test
  public void testPrepareGetMetricsPrecisionHours() throws SQLException {
    Condition condition = new DefaultCondition(
      new ArrayList<>(Arrays.asList("cpu_user", "mem_free")), Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, Precision.HOURS, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
        .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD_HOURLY"));
    verify(connection, preparedStatement);
  }

  @Test
  public void testResultSetLimitCheck() throws SQLException {

    List<String> metrics = new ArrayList<String>();
    List<String> hosts = new ArrayList<String>();
    int numMetrics = 0;
    int numHosts = 0;
    int limit = PhoenixHBaseAccessor.RESULTSET_LIMIT;

    // 22 Metrics x 2 Hosts x 1 hour with requested SECONDS precision = 15840 points. Should be OK!
    numMetrics = 22;
    numHosts = 2;
    for (int i = 0; i < numMetrics; i++) {
      metrics.add("TestMetric"+i);
    }
    for (int i = 0; i < numHosts; i++) {
      hosts.add("TestHost"+i);
    }

    Condition condition = new DefaultCondition(
      metrics, hosts,
      "a1", "i1", 1407950000L, 1407953600L, Precision.SECONDS, null, false);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement preparedStatement = createNiceMock(PreparedStatement.class);
    Capture<String> stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
      .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD"));
    verify(connection, preparedStatement);

    //Check without passing precision. Should be OK!
    condition = new DefaultCondition(
      metrics, hosts,
      "a1", "i1", 1407950000L, 1407953600L, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
      .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD"));
    verify(connection, preparedStatement);

    //Check with more hosts and lesser metrics for 1 day data = 11520 points Should be OK!
    metrics.clear();
    hosts.clear();
    numMetrics = 2;
    numHosts = 20;
    for (int i = 0; i < numMetrics; i++) {
      metrics.add("TestMetric"+i);
    }
    for (int i = 0; i < numHosts; i++) {
      hosts.add("TestHost"+i);
    }
    condition = new DefaultCondition(
      metrics, hosts,
      "a1", "i1", 1407867200L, 1407953600L, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
      .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_RECORD_MINUTE"));
    verify(connection, preparedStatement);

    //Check with 20 metrics, NO hosts and 1 day data = 5760 points. Should be OK!
    metrics.clear();
    hosts.clear();
    numMetrics = 20;
    for (int i = 0; i < numMetrics; i++) {
      metrics.add("TestMetric"+i);
    }
    condition = new DefaultCondition(
      metrics, hosts,
      "a1", "i1", 1407867200L, 1407953600L, null, null, false);
    connection = createNiceMock(Connection.class);
    preparedStatement = createNiceMock(PreparedStatement.class);
    stmtCapture = new Capture<String>();
    expect(connection.prepareStatement(EasyMock.and(EasyMock.anyString(), EasyMock.capture(stmtCapture))))
      .andReturn(preparedStatement);

    replay(connection, preparedStatement);
    PhoenixTransactSQL.prepareGetAggregateSqlStmt(connection, condition);
    stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("FROM METRIC_AGGREGATE_MINUTE"));
    verify(connection, preparedStatement);

    //Check with 5 hosts and 10 metrics for 1 hour data = 18000 points. Should throw out Exception!
    metrics.clear();
    hosts.clear();
    numMetrics = 10;
    numHosts = 5;
    for (int i = 0; i < numMetrics; i++) {
      metrics.add("TestMetric"+i);
    }
    for (int i = 0; i < numHosts; i++) {
      hosts.add("TestHost"+i);
    }
    condition = new DefaultCondition(
      metrics, hosts,
      "a1", "i1", 1407950000L, 1407953600L, null, null, false);
    boolean exceptionThrown = false;
    boolean requestedSizeFoundInMessage = false;

    try {
      PhoenixTransactSQL.prepareGetMetricsSqlStmt(connection, condition);
    } catch (PrecisionLimitExceededException pe) {
      exceptionThrown = true;
      String message = pe.getMessage();
      if (message !=null && message.contains("18000")) {
        requestedSizeFoundInMessage = true;
      }
    }
    Assert.assertTrue(exceptionThrown);
    Assert.assertTrue(requestedSizeFoundInMessage);
  }

  @Test
  public void testTopNHostsConditionClause() throws Exception {
    List<String> hosts = Arrays.asList("h1", "h2");
    List<byte[]> uuids = Arrays.asList(new byte[16], new byte[16]);

    Condition condition = new TopNCondition(uuids, new ArrayList<>(Collections.singletonList("cpu_user")), hosts,
      "a1", "i1", 1407959718L, 1407959918L, null, null, false, 2, null, false);

    String conditionClause = condition.getConditionClause().toString();
    String expectedClause = " UUID IN (" +
      "SELECT UUID FROM METRIC_RECORD WHERE " +
          "(UUID LIKE ? OR UUID LIKE ?) AND " +
          "SERVER_TIME >= ? AND SERVER_TIME < ? " +
          "GROUP BY UUID ORDER BY MAX(METRIC_MAX) DESC LIMIT 2) AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertEquals(expectedClause, conditionClause);
  }

  @Test
  public void testTopNMetricsConditionClause() throws Exception {
    List<String> metricNames = new ArrayList<>(Arrays.asList("m1", "m2", "m3"));
    List<byte[]> uuids = Arrays.asList(new byte[16], new byte[16], new byte[16]);

    Condition condition = new TopNCondition(uuids, metricNames, Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, null, null, false, 2, null, false);

    String conditionClause = condition.getConditionClause().toString();
    String expectedClause = " UUID IN (" +
      "SELECT UUID FROM METRIC_RECORD WHERE " +
      "(UUID LIKE ? OR UUID LIKE ? OR UUID LIKE ?) AND " +
      "SERVER_TIME >= ? AND SERVER_TIME < ? " +
      "GROUP BY UUID ORDER BY MAX(METRIC_MAX) DESC LIMIT 2) AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertEquals(expectedClause, conditionClause);
  }

  @Test
  public void testTopNMetricsIllegalConditionClause() throws Exception {
    List<String> metricNames = new ArrayList<>(Arrays.asList("m1", "m2"));

    List<String> hosts = Arrays.asList("h1", "h2");
    List<byte[]> uuids = Arrays.asList(new byte[16], new byte[16], new byte[16], new byte[16]);

    Condition condition = new TopNCondition(uuids, metricNames, hosts,
      "a1", "i1", 1407959718L, 1407959918L, null, null, false, 2, null, false);

    Assert.assertEquals(condition.getConditionClause(), null);
  }

}
