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
package org.apache.ambari.eventdb.db;

import org.apache.ambari.eventdb.model.Workflows;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OracleConnector extends PostgresConnector {

  public OracleConnector(String connectionURL, String driverName, String username, String password) throws IOException {
    super(connectionURL, driverName, username, password);
  }

  @Override
  protected PreparedStatement getQualifiedPS(Statements statement, String searchClause, Workflows.WorkflowDBEntry.WorkflowFields field, boolean sortAscending, int offset, int limit) throws IOException {
    if (db == null)
      throw new IOException("db not initialized");

    String order = " ORDER BY " + field.toString() + " " + (sortAscending ? SORT_ASC : SORT_DESC);

    String query = "select * \n" +
        "  from ( select " +
//        "/*+ FIRST_ROWS(n) */ \n" +
        "  a.*, ROWNUM rnum \n" +
        "      from ("
        + statement.getStatementString() + searchClause + order +
        ") a \n" +
        "      where ROWNUM <= " + (offset + limit) + ") \n" +
        "where rnum  >= " + offset;

    try {
      return db.prepareStatement(query);
    } catch (SQLException e) {
      throw new IOException(e);
    }

  }


}
