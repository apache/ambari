/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20;

import com.google.common.base.Optional;
import org.apache.ambari.view.hive20.actor.message.GetColumnMetadataJob;
import org.apache.hive.jdbc.HiveConnection;
import org.apache.hive.jdbc.HiveQueryResultSet;
import org.apache.hive.jdbc.HiveStatement;
import org.junit.Test;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;


import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class HiveJdbcConnectionDelegateTest {


  @Test
  public void testCreateStatement() throws SQLException {
    HiveConnection hiveConnection = createNiceMock(HiveConnection.class);
    HiveStatement hiveStatement = createNiceMock(HiveStatement.class);
    expect(hiveConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)).andReturn(hiveStatement);
    replay(hiveConnection);
    HiveStatement statement = new HiveJdbcConnectionDelegate().createStatement(hiveConnection);
    assertEquals(hiveStatement, statement);

  }


  @Test
  public void testExecute() throws SQLException {
    HiveConnection hiveConnection = createNiceMock(HiveConnection.class);
    HiveStatement hiveStatement = createNiceMock(HiveStatement.class);
    HiveQueryResultSet resultSet = createNiceMock(HiveQueryResultSet.class);
    expect(hiveConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)).andReturn(hiveStatement);
    String query = "select * from test";
    expect(hiveStatement.execute(query)).andReturn(true);
    expect(hiveStatement.getResultSet()).andReturn(resultSet);
    replay(hiveConnection, hiveStatement, resultSet);
    HiveJdbcConnectionDelegate hiveJdbcConnectionDelegate = new HiveJdbcConnectionDelegate();
    Optional<ResultSet> execute = hiveJdbcConnectionDelegate.execute(hiveConnection, query);
    assertEquals(execute.get(), resultSet);
    verify(hiveConnection, hiveStatement, resultSet);

  }


  @Test
  public void testGetColumnMetaData() throws SQLException {

    HiveConnection hiveConnection = createNiceMock(HiveConnection.class);
    DatabaseMetaData metadata = createNiceMock(DatabaseMetaData.class);
    expect(hiveConnection.getMetaData()).andReturn(metadata);
    ResultSet resultSet = createNiceMock(ResultSet.class);
    expect(metadata.getColumns(anyString(), anyString(), anyString(), anyString())).andReturn(resultSet);
    replay(hiveConnection, metadata, resultSet);
    HiveJdbcConnectionDelegate hiveJdbcConnectionDelegate = new HiveJdbcConnectionDelegate();
    ResultSet columnMetadata = hiveJdbcConnectionDelegate.getColumnMetadata(hiveConnection, new GetColumnMetadataJob("", "", "", ""));
    assertEquals(resultSet, columnMetadata);
    verify(hiveConnection, metadata, resultSet);
  }


  @Test
  public void testCancel() throws SQLException {
    HiveConnection hiveConnection = createNiceMock(HiveConnection.class);
    HiveStatement hiveStatement = createNiceMock(HiveStatement.class);
    HiveQueryResultSet resultSet = createNiceMock(HiveQueryResultSet.class);
    expect(hiveConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)).andReturn(hiveStatement);
    String query = "select * from test";
    expect(hiveStatement.execute(query)).andReturn(true);
    expect(hiveStatement.getResultSet()).andReturn(resultSet);
    hiveStatement.cancel();
    resultSet.close();
    replay(hiveConnection, hiveStatement, resultSet);
    HiveJdbcConnectionDelegate hiveJdbcConnectionDelegate = new HiveJdbcConnectionDelegate();
    hiveJdbcConnectionDelegate.execute(hiveConnection, query);
    hiveJdbcConnectionDelegate.cancel();
    hiveJdbcConnectionDelegate.closeResultSet();
    hiveJdbcConnectionDelegate.closeStatement();
    verify(hiveConnection, hiveStatement, resultSet);
  }

}
