/*
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

package org.apache.ambari.view.hive20;

import com.google.common.base.Optional;
import org.apache.ambari.view.hive20.actor.message.GetColumnMetadataJob;
import org.apache.hive.jdbc.HiveConnection;
import org.apache.hive.jdbc.HiveStatement;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HiveJdbcConnectionDelegate implements ConnectionDelegate {

  private ResultSet currentResultSet;
  private HiveStatement currentStatement;

  @Override
  public HiveStatement createStatement(HiveConnection connection) throws SQLException {
    Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    currentStatement = (HiveStatement) statement;
    return currentStatement;
  }

  @Override
  public Optional<ResultSet> execute(String statement) throws SQLException {
    if (currentStatement == null) {
      throw new SQLException("Statement not created. Cannot execute Hive queries");
    }

    boolean hasResultSet = currentStatement.execute(statement);

    if (hasResultSet) {
      ResultSet resultSet = currentStatement.getResultSet();
      currentResultSet = resultSet;
      return Optional.of(resultSet);
    } else {
      return Optional.absent();
    }
  }

  @Override
  public Optional<ResultSet> execute(HiveConnection connection, String sqlStatement) throws SQLException {
    createStatement(connection);
    return execute(sqlStatement);
  }


  @Override
  public ResultSet getColumnMetadata(HiveConnection connection, GetColumnMetadataJob job) throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();
    ResultSet resultSet = metaData.getColumns("", job.getSchemaPattern(), job.getTablePattern(), job.getColumnPattern());
    currentResultSet = resultSet;
    return resultSet;
  }

  @Override
  public DatabaseMetaData getDatabaseMetadata(HiveConnection connection) throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();
    return metaData;
  }

  @Override
  public void cancel() throws SQLException {
    if (currentStatement != null) {
      currentStatement.cancel();
    }
  }

  @Override
  public void closeResultSet() {

    try {
      if (currentResultSet != null) {
        currentResultSet.close();
      }
    } catch (SQLException e) {
      // Cannot do anything here
    }
  }

  @Override
  public void closeStatement() {
    try {
      if (currentStatement != null) {
        currentStatement.close();
      }
    } catch (SQLException e) {
      // cannot do anything here
    }
  }


}
