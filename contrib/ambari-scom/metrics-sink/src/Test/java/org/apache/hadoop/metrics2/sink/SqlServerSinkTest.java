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

package org.apache.hadoop.metrics2.sink;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.hadoop.metrics2.Metric;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsTag;
import org.junit.Assert;
import org.junit.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashSet;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * SqlServerSink Tests.
 */
public class SqlServerSinkTest {
  @Test
  public void testInit() throws Exception {

    SubsetConfiguration configuration = createNiceMock(SubsetConfiguration.class);

    // set expectations
    expect(configuration.getParent()).andReturn(null);
    expect(configuration.getPrefix()).andReturn("prefix");
    expect(configuration.getString("databaseUrl")).andReturn("url");

    // replay
    replay(configuration);

    SqlServerSink sink = new SqlServerSink();

    sink.init(configuration);

    verify(configuration);
  }

  @Test
  public void testEnsureConnection() throws Exception {

    SubsetConfiguration configuration = createNiceMock(SubsetConfiguration.class);
    Connection connection = createNiceMock(Connection.class);

    // set expectations
    expect(configuration.getParent()).andReturn(null);
    expect(configuration.getPrefix()).andReturn("prefix");
    expect(configuration.getString("databaseUrl")).andReturn("url");

    // replay
    replay(configuration, connection);

    SqlServerSink sink = new SqlServerSink();

    sink.init(configuration);

    Assert.assertFalse(sink.ensureConnection());

    SQLServerDriver.setConnection(connection);
    Assert.assertTrue(sink.ensureConnection());

    verify(configuration, connection);
  }

  @Test
  public void testFlush() throws Exception {

    SubsetConfiguration configuration = createNiceMock(SubsetConfiguration.class);
    Connection connection = createNiceMock(Connection.class);

    // set expectations
    expect(configuration.getParent()).andReturn(null);
    expect(configuration.getPrefix()).andReturn("prefix");
    expect(configuration.getString("databaseUrl")).andReturn("url");
    connection.close();

    // replay
    replay(configuration, connection);

    SqlServerSink sink = new SqlServerSink();

    sink.init(configuration);

    SQLServerDriver.setConnection(connection);
    sink.ensureConnection();
    sink.flush();

    verify(configuration, connection);
  }

  @Test
  public void testGetMetricRecordID() throws Exception {
    SubsetConfiguration configuration = createNiceMock(SubsetConfiguration.class);
    Connection connection = createNiceMock(Connection.class);
    CallableStatement cstmt = createNiceMock(CallableStatement.class);

    // set expectations
    expect(configuration.getParent()).andReturn(null);
    expect(configuration.getPrefix()).andReturn("prefix");
    expect(configuration.getString("databaseUrl")).andReturn("url");

    expect(connection.prepareCall("{call dbo.uspGetMetricRecord(?, ?, ?, ?, ?, ?, ?, ?, ?)}")).andReturn(cstmt);
    cstmt.setNString(1, "context");
    cstmt.setNString(2, "typeName");
    cstmt.setNString(3, "nodeName");
    cstmt.setNString(4, "ip");
    cstmt.setNString(5, "clusterName");
    cstmt.setNString(6, "serviceName");
    cstmt.setNString(7, "tagPairs");
    cstmt.setLong(8, 9999L);
    cstmt.registerOutParameter(9, java.sql.Types.BIGINT);
    expect(cstmt.execute()).andReturn(true);
    expect(cstmt.getLong(9)).andReturn(99L);
    expect(cstmt.wasNull()).andReturn(false);

    // replay
    replay(configuration, connection, cstmt);

    SqlServerSink sink = new SqlServerSink();

    sink.init(configuration);

    SQLServerDriver.setConnection(connection);

    Assert.assertEquals(99,
        sink.getMetricRecordID("context", "typeName", "nodeName", "ip", "clusterName", "serviceName", "tagPairs", 9999L));

    verify(configuration, connection, cstmt);
  }

  @Test
  public void testGetMetricRecordID_nullReturn() throws Exception {
    SubsetConfiguration configuration = createNiceMock(SubsetConfiguration.class);
    Connection connection = createNiceMock(Connection.class);
    CallableStatement cstmt = createNiceMock(CallableStatement.class);

    // set expectations
    expect(configuration.getParent()).andReturn(null);
    expect(configuration.getPrefix()).andReturn("prefix");
    expect(configuration.getString("databaseUrl")).andReturn("url");

    expect(connection.prepareCall("{call dbo.uspGetMetricRecord(?, ?, ?, ?, ?, ?, ?, ?, ?)}")).andReturn(cstmt);
    cstmt.setNString(1, "context");
    cstmt.setNString(2, "typeName");
    cstmt.setNString(3, "nodeName");
    cstmt.setNString(4, "ip");
    cstmt.setNString(5, "clusterName");
    cstmt.setNString(6, "serviceName");
    cstmt.setNString(7, "tagPairs");
    cstmt.setLong(8, 9999L);
    cstmt.registerOutParameter(9, java.sql.Types.BIGINT);
    expect(cstmt.execute()).andReturn(true);
    expect(cstmt.getLong(9)).andReturn(99L);
    expect(cstmt.wasNull()).andReturn(true);

    // replay
    replay(configuration, connection, cstmt);

    SqlServerSink sink = new SqlServerSink();

    sink.init(configuration);

    SQLServerDriver.setConnection(connection);

    Assert.assertEquals(-1,
        sink.getMetricRecordID("context", "typeName", "nodeName", "ip", "clusterName", "serviceName", "tagPairs", 9999L));

    verify(configuration, connection, cstmt);
  }

  @Test
  public void testInsertMetricValue() throws Exception {
    SubsetConfiguration configuration = createNiceMock(SubsetConfiguration.class);
    Connection connection = createNiceMock(Connection.class);
    CallableStatement cstmt = createNiceMock(CallableStatement.class);

    // set expectations
    expect(configuration.getParent()).andReturn(null);
    expect(configuration.getPrefix()).andReturn("prefix");
    expect(configuration.getString("databaseUrl")).andReturn("url");

    expect(connection.prepareCall("{call dbo.uspInsertMetricValue(?, ?, ?)}")).andReturn(cstmt);
    cstmt.setLong(1, 9999L);
    cstmt.setNString(2, "metricName");
    cstmt.setNString(3, "metricValue");
    expect(cstmt.execute()).andReturn(true);

    // replay
    replay(configuration, connection, cstmt);

    SqlServerSink sink = new SqlServerSink();

    sink.init(configuration);

    SQLServerDriver.setConnection(connection);

    sink.insertMetricValue(9999L, "metricName", "metricValue");

    verify(configuration, connection, cstmt);
  }

  @Test
  public void testPutMetrics() throws Exception {
    SubsetConfiguration configuration = createNiceMock(SubsetConfiguration.class);
    Connection connection = createNiceMock(Connection.class);
    CallableStatement cstmt = createNiceMock(CallableStatement.class);
    MetricsRecord record = createNiceMock(MetricsRecord.class);
    Metric metric = createNiceMock(Metric.class);

    // set expectations
    expect(configuration.getParent()).andReturn(null);
    expect(configuration.getPrefix()).andReturn("prefix");
    expect(configuration.getString("databaseUrl")).andReturn("url");

    expect(record.context()).andReturn("context");
    expect(record.name()).andReturn("typeName");
    expect(record.tags()).andReturn(new HashSet<MetricsTag>());
    expect(record.timestamp()).andReturn(9999L);

    expect(record.metrics()).andReturn(Collections.singleton(metric));

    expect(metric.name()).andReturn("name").anyTimes();
    expect(metric.value()).andReturn(1234);

    expect(connection.prepareCall("{call dbo.uspGetMetricRecord(?, ?, ?, ?, ?, ?, ?, ?, ?)}")).andReturn(cstmt);
    cstmt.setNString(1, "context");
    cstmt.setNString(2, "typeName");
    cstmt.setNString(eq(3), (String) anyObject());
    cstmt.setNString(eq(4), (String) anyObject());
    cstmt.setNString(eq(5), (String) anyObject());
    cstmt.setNString(6, "prefix");
    cstmt.setNString(7, "sourceName:prefix");
    cstmt.setLong(8, 9999L);
    cstmt.registerOutParameter(9, java.sql.Types.BIGINT);
    expect(cstmt.execute()).andReturn(true);
    expect(cstmt.getLong(9)).andReturn(99L);
    expect(cstmt.wasNull()).andReturn(false);

    expect(connection.prepareCall("{call dbo.uspInsertMetricValue(?, ?, ?)}")).andReturn(cstmt);
    cstmt.setLong(1, 99L);
    cstmt.setNString(2, "name");
    cstmt.setNString(3, "1234");
    expect(cstmt.execute()).andReturn(true);

    // replay
    replay(configuration, connection, cstmt, record, metric);

    SqlServerSink sink = new SqlServerSink();

    sink.init(configuration);

    SQLServerDriver.setConnection(connection);

    sink.putMetrics(record);

    verify(configuration, connection, cstmt, record, metric);
  }
}
