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
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.SplitByMetricNamesCondition;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import org.easymock.EasyMock;

public class TestPhoenixTransactSQL {
  @Test
  public void testConditionClause() throws Exception {
    Condition condition = new DefaultCondition(
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, null, null, false);

    String preparedClause = condition.getConditionClause().toString();
    String expectedClause = "(METRIC_NAME IN (?, ?)) AND HOSTNAME = ? AND " +
      "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);
  }

  @Test
  public void testSplitByMetricNamesCondition() throws Exception {
    Condition c = new DefaultCondition(
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
      "a1", "i1", 1407959718L, 1407959918L, null, null, false);

    SplitByMetricNamesCondition condition = new SplitByMetricNamesCondition(c);
    condition.setCurrentMetric(c.getMetricNames().get(0));

    String preparedClause = condition.getConditionClause().toString();
    String expectedClause = "METRIC_NAME = ? AND HOSTNAME = ? AND " +
      "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);
  }

  @Test
  public void testLikeConditionClause() throws Exception {
    Condition condition = new DefaultCondition(
      Arrays.asList("cpu_user", "some=%.metric"),
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
        Arrays.asList("some=%.metric"), Collections.singletonList("h1"), "a1", "i1",
        1407959718L, 1407959918L, null, null, false);

    preparedClause = condition.getConditionClause().toString();
    expectedClause = "(METRIC_NAME LIKE ?) AND HOSTNAME = ? AND " +
        "APP_ID = ? AND INSTANCE_ID = ? AND SERVER_TIME >= ? AND SERVER_TIME < ?";

    Assert.assertNotNull(preparedClause);
    Assert.assertEquals(expectedClause, preparedClause);


    condition = new DefaultCondition(
      Arrays.asList("some=%.metric1", "some=%.metric2", "some=%.metric3"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
    Condition condition = new DefaultCondition(
      Arrays.asList("cpu_user", "mem_free"), Arrays.asList("h1", "h2"),
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
      .andReturn(6).once();

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
    Condition condition = new DefaultCondition(
      Arrays.asList("cpu_user", "mem_free"), Arrays.asList("h1"),
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
      .andReturn(6).anyTimes();

    replay(connection, preparedStatement, parameterMetaData);
    PhoenixTransactSQL.setSortMergeJoinEnabled(true);
    PhoenixTransactSQL.prepareGetLatestMetricSqlStmt(connection, condition);
    String stmt = stmtCapture.getValue();
    Assert.assertTrue(stmt.contains("/*+ USE_SORT_MERGE_JOIN NO_CACHE */"));
  }

  @Test
  public void testPrepareGetMetricsPrecisionHours() throws SQLException {
    Condition condition = new DefaultCondition(
      Arrays.asList("cpu_user", "mem_free"), Collections.singletonList("h1"),
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
}
