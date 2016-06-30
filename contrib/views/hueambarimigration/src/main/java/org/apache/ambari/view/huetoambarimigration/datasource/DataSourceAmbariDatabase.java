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

package org.apache.ambari.view.huetoambarimigration.datasource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceAmbariDatabase {

  private static DataSourceAmbariDatabase datasource;
  private ComboPooledDataSource cpds;

  private DataSourceAmbariDatabase(String ambaridatabasedriver, String ambarijdbcurl, String ambaridatabaseusename, String ambaridatabasepassword) throws IOException, SQLException, PropertyVetoException {

    cpds = new ComboPooledDataSource();
    cpds.setDriverClass(ambaridatabasedriver); //loads the jdbc driver
    cpds.setJdbcUrl(ambarijdbcurl);
    cpds.setUser(ambaridatabaseusename);
    cpds.setPassword(ambaridatabasepassword);

    // the settings below are optional -- c3p0 can work with defaults
    cpds.setMinPoolSize(10);
    cpds.setAcquireIncrement(10);
    cpds.setMaxPoolSize(20);
    cpds.setMaxStatements(180);

  }

  public static DataSourceAmbariDatabase getInstance(String ambariDbDriver, String ambariDBjadbcURL, String ambaridbUsername, String ambariDBPasswd) throws IOException, SQLException, PropertyVetoException {
    if (datasource == null) {
      datasource = new DataSourceAmbariDatabase(ambariDbDriver, ambariDBjadbcURL, ambaridbUsername, ambariDBPasswd);
      return datasource;
    } else {
      return datasource;
    }
  }

  public Connection getConnection() throws SQLException {
    return this.cpds.getConnection();
  }


}
