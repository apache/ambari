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

package org.apache.ambari.server.controller.sql;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.jdbc.ConnectionFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Factory for the sink database connection.
 */
public class SinkConnectionFactory implements ConnectionFactory {

  /**
   * The database URL.
   */
  private String databaseUrl;

  /**
   * The database driver.
   */
  private String databaseDriver;

  private String databaseUser;

  private String databasePassword;

  private boolean useIntegratedAuth;

  /**
   * Indicates whether or not the driver has been initialized
   */
  private boolean connectionInitialized = false;

  private ComboPooledDataSource cpds;
  /**
   * The singleton.
   */
  private static SinkConnectionFactory singleton = new SinkConnectionFactory();

  // ----- Constructor -------------------------------------------------------

  protected SinkConnectionFactory() {
    Configuration config = new Configuration();
    this.databaseUrl    = config.getSinkDatabaseUrl();
    this.databaseDriver = config.getSinkDatabaseDriver();
    this.useIntegratedAuth = config.getSinkUseIntegratedAuth();
    this.databaseUser = config.getSinkDatabaseUser();
    this.databasePassword =  config.getSinkDatabasePassword();
  }


  // ----- SinkConnectionFactory ---------------------------------------------

  /**
   * Initialize.
   */
  public void init() {
    this.cpds = new ComboPooledDataSource();
    this.cpds.setJdbcUrl(this.databaseUrl);
    if(!useIntegratedAuth) {
      this.cpds.setUser(this.databaseUser);
      this.cpds.setPassword(this.databasePassword);
    }
    this.cpds.setMaxPoolSize(5);
  }

  /**
   * Get the singleton instance.
   *
   * @return the singleton instance
   */
  public static SinkConnectionFactory instance() {
    return singleton;
  }

  /**
   * Get the database URL.
   *
   * @return the database URL
   */
  public String getDatabaseUrl() {
    return databaseUrl;
  }

  /**
   * Get the database driver.
   *
   * @return the database driver
   */
  public String getDatabaseDriver() {
    return databaseDriver;
  }

// ----- ConnectionFactory -----------------------------------------------

  @Override
  public Connection getConnection() throws SQLException {
    synchronized (this) {
      if (!connectionInitialized) {
        try {
          Class.forName(databaseDriver);
        } catch (Exception e) {
          throw new SQLException("Can't load the driver class.", e);
        }
        init();
        connectionInitialized = true;
      }
    }
    return this.cpds.getConnection();
  }
}
