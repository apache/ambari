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

package org.apache.ambari.server.controller.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connection factory implementation for SQLite.
 */
public class SQLiteConnectionFactory implements ConnectionFactory {

  /**
   * The connection URL minus the db file.
   */
  private static final String CONNECTION_URL = "jdbc:sqlite:";

  /**
   * The filename of the SQLite db file.
   */
  private final String dbFile;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a connection factory.
   *
   * @param dbFile  the SQLite DB filename
   */
  public SQLiteConnectionFactory(String dbFile) {
    this.dbFile = dbFile;
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Can't load SQLite.", e);
    }
  }


  // ----- ConnectionFactory -------------------------------------------------

  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(CONNECTION_URL + dbFile);
  }
}
