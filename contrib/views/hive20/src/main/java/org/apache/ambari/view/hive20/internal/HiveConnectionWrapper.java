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

package org.apache.ambari.view.hive20.internal;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import org.apache.ambari.view.hive20.AuthParams;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hive.jdbc.HiveConnection;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Composition over a Hive jdbc connection
 * This class only provides a connection over which
 * callers should run their own JDBC statements
 */
public class HiveConnectionWrapper implements Connectable, Supplier<HiveConnection> {

  private static String DRIVER_NAME = "org.apache.hive.jdbc.HiveDriver";
  public static final String SUFFIX = "validating the login";
  private final String jdbcUrl;
  private final String username;
  private final String password;
  private final AuthParams authParams;

  private UserGroupInformation ugi;

  private HiveConnection connection = null;
  private boolean authFailed;

  public HiveConnectionWrapper(String jdbcUrl, String username, String password, AuthParams authParams) {
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
    this.authParams = authParams;
  }

  @Override
  public void connect() throws ConnectionException {
    try {
      Class.forName(DRIVER_NAME);
    } catch (ClassNotFoundException e) {
      throw new ConnectionException(e, "Cannot load the hive JDBC driver");
    }

    try {
      ugi = UserGroupInformation.createProxyUser(username, authParams.getProxyUser());
    } catch (IOException e) {
      throw new ConnectionException(e, "Cannot set kerberos authentication for getting connection.");
    }

    try {
      Connection conn = ugi.doAs(new PrivilegedExceptionAction<Connection>() {
        @Override
        public Connection run() throws Exception {
          return DriverManager.getConnection(jdbcUrl, username, password);
        }
      });
      connection = (HiveConnection) conn;
    } catch (UndeclaredThrowableException exception) {
      // Check if the reason was an auth error
      Throwable undeclaredThrowable = exception.getUndeclaredThrowable();
      if (undeclaredThrowable instanceof SQLException) {
        SQLException sqlException = (SQLException) undeclaredThrowable;
        if (isLoginError(sqlException))
          authFailed = true;
        throw new ConnectionException(sqlException, "Cannot open a hive connection with connect string " + jdbcUrl);
      }

    } catch (IOException | InterruptedException e) {
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

  private boolean isLoginError(SQLException ce) {
    return ce.getCause().getMessage().toLowerCase().endsWith(SUFFIX);
  }

  /**
   * True when the connection is unauthorized
   *
   * @return
   */
  @Override
  public boolean isUnauthorized() {
    return authFailed;
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
