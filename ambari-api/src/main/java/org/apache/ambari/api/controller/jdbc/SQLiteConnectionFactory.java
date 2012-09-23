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

package org.apache.ambari.api.controller.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 */
public class SQLiteConnectionFactory implements ConnectionFactory {

  private static String DB_FILE_NAME = System.getProperty("ambariapi.dbfile", "src/test/resources/data.db");

  public static final String CONNECTION_URL = "jdbc:sqlite:" + DB_FILE_NAME;

  public SQLiteConnectionFactory() {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Can't load SQLite.", e);
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(CONNECTION_URL);
  }
}
