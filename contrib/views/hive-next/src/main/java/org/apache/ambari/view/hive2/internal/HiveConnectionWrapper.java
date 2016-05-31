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

package org.apache.ambari.view.hive2.internal;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import org.apache.hive.jdbc.HiveConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Composition over a Hive jdbc connection
 * This class only provides a connection over which
 * callers should run their own JDBC statements
 */
public class HiveConnectionWrapper implements Connectable,Supplier<HiveConnection> {

  private static String DRIVER_NAME = "org.apache.hive.jdbc.HiveDriver";
  private final String jdbcUrl;
  private final String username;
  private final String password;

  private HiveConnection connection = null;

  public HiveConnectionWrapper(String jdbcUrl, String username, String password) {
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
  }


  @Override
  public void connect() throws ConnectionException {
    try {
      Class.forName(DRIVER_NAME);
    } catch (ClassNotFoundException e) {
      throw new ConnectionException(e, "Cannot load the hive JDBC driver");
    }

    try {
      Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
      connection = (HiveConnection)conn;

    } catch (SQLException e) {
      throw new ConnectionException(e, "Cannot open a hive connection with connect string " + jdbcUrl);
    }


  }

  @Override
  public void reconnect() throws ConnectionException {

  }

  @Override
  public void disconnect() throws ConnectionException {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        throw new ConnectionException(e, "Cannot close the hive connection with connect string " + jdbcUrl);
      }
    }
  }

  public Optional<HiveConnection> getConnection() {
    return Optional.fromNullable(connection);
  }

  @Override
  public boolean isOpen() {
    try {
      return connection != null && !connection.isClosed() && connection.isValid(100);
    } catch (SQLException e) {
      // in case of an SQ error just return
      return false;
    }
  }

  /**
   * Retrieves an instance of the appropriate type. The returned object may or
   * may not be a new instance, depending on the implementation.
   *
   * @return an instance of the appropriate type
   */
  @Override
  public HiveConnection get() {
    return null;
  }
}
