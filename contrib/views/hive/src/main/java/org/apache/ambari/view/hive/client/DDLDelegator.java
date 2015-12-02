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

package org.apache.ambari.view.hive.client;

import org.apache.hive.service.cli.thrift.*;
import org.apache.thrift.TException;

import java.util.LinkedList;
import java.util.List;

public class DDLDelegator {
  private Connection connection;

  public DDLDelegator(Connection connection) {
    this.connection = connection;
  }

  /**
   * Retrieve list of tables in DB
   * @param db db name
   * @return list of table names
   * @throws HiveClientException
   */
  public List<String> getTableList(TSessionHandle session, String db, String like) throws HiveClientException {
    Cursor cursor = getTableListCursor(session, db, like);
    return cursor.getValuesInColumn(0);
  }

  /**
   * Retrieve list of tables in DB results set
   * @param db db name
   * @return list of table names
   * @throws HiveClientException
   */
   public Cursor getTableListCursor(TSessionHandle session, String db, String like) throws HiveClientException {
    connection.executeSync(session, String.format("use %s", db));
    TOperationHandle handle = connection.executeSync(session, String.format("show tables like '%s'", like));

    return new Cursor(connection, handle);
  }

  /**
   * Retrieve databases
   * @param like '*' for all
   * @return list of databases
   * @throws HiveClientException
   */
  public List<String> getDBList(TSessionHandle session, String like) throws HiveClientException {
    Cursor cursor = getDBListCursor(session, like);
    return cursor.getValuesInColumn(0);
  }

  /**
   * Retrieve databases results set
   * @param like '*' for all
   * @return list of databases
   * @throws HiveClientException
   */
  public Cursor getDBListCursor(TSessionHandle session, String like) throws HiveClientException {
    TOperationHandle handle = connection.executeSync(session, String.format("show databases like '%s'", like));
    return new Cursor(connection, handle);
  }

  /**
   * Retrieve table schema
   * @param db database name
   * @param table table name
   * @return schema
   * @throws HiveClientException
   */
  public List<ColumnDescription> getTableDescription(TSessionHandle session, final String db, final String table, String like, boolean extended) throws HiveClientException {
    List<ColumnDescription> columnDescriptions = new LinkedList<ColumnDescription>();
    Cursor cursor = getTableDescriptionCursor(session, db, table, like);
    for(Row row : cursor) {
      Object[] rowObjects = row.getRow();

      ColumnDescription columnDescription;
      if (extended) {
        //TODO: retrieve sortedBy, clusteredBy, partitioned
        columnDescription = ColumnDescriptionExtended.createExtendedColumnDescription(
            (String) rowObjects[3], (String) rowObjects[5], (String) rowObjects[11],
            false, false, false, (Integer) rowObjects[16]);
      } else {
        columnDescription = ColumnDescriptionShort.createShortColumnDescription(
            (String) rowObjects[3], (String) rowObjects[5], (Integer) rowObjects[16]);
      }
      columnDescriptions.add(columnDescription);
    }
    return columnDescriptions;
  }

  /**
   * Retrieve table schema results set
   * @param db database name
   * @param table table name
   * @return schema
   * @throws HiveClientException
   */
  public Cursor getTableDescriptionCursor(final TSessionHandle session, final String db, final String table, String like) throws HiveClientException {
    if (like == null)
      like = ".*";
    else
      like = ".*" + like + ".*";
    final String finalLike = like;
    TGetColumnsResp resp = new HiveCall<TGetColumnsResp>(connection,session) {
      @Override
      public TGetColumnsResp body() throws HiveClientException {

        TGetColumnsReq req = new TGetColumnsReq(session);
        req.setSchemaName(db);
        req.setTableName(table);
        req.setColumnName(finalLike);
        try {
          return connection.getClient().GetColumns(req);
        } catch (TException e) {
          throw new HiveClientException("H200 Unable to get table columns", e);
        }
      }

    }.call();

    return new Cursor(connection, resp.getOperationHandle());
  }
}
