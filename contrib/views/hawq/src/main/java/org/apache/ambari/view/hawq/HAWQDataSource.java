/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.apache.ambari.view.hawq;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.ambari.view.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

class HAWQDataSource {

    private final static Logger LOG = LoggerFactory.getLogger(HAWQDataSource.class);

    private volatile ComboPooledDataSource dataSource;
    private volatile String dataSourceKey;

    Connection getConnection(String host, String port, String user, String password) throws SystemException, SQLException {

        LOG.debug("Getting connection for = {}:{}:{}", host, port, user);

        // check if configuration changed from when the data source was created
        String key = composeKey(host, port, user, password);
        if (!key.equals(dataSourceKey)) {
            resetDataSource(host, port, user, password);
        }

        Connection conn = dataSource.getConnection();
        LOG.debug("Acquired connection = {}", conn);
        return conn;
    }

    private String composeKey(String host, String port, String user, String password) {
        return host + port + user + password;
    }

    private synchronized void resetDataSource(String host, String port, String user, String password) throws SystemException {
        // double check locking using volatile members
        String key = composeKey(host, port, user, password);
        if (key.equals(dataSourceKey)) {
            return;
        }

        // close old datasource
        if (dataSource != null) {
            dataSource.close();
        }

        // create new datasource
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass("org.postgresql.Driver"); //loads the jdbc driver
        } catch (Exception e) {
            throw new SystemException("Failed to load JDBC driver.", e);
        }
        String url = String.format("jdbc:postgresql://%s:%s/template1", host, port);
        cpds.setJdbcUrl(url);
        cpds.setUser(user);
        cpds.setPassword(password);
        cpds.setInitialPoolSize(0);
        cpds.setMinPoolSize(0);
        cpds.setMaxPoolSize(1);
        cpds.setMaxIdleTime(600); // 10 minutes
        // spend no more than 5 secs trying to acquire a connection
        cpds.setAcquireRetryAttempts(5);
        cpds.setAcquireRetryDelay(1000);

        LOG.info("Set data source for url = {} and user = {}", url, user);

        // update volatile members before relinquishing the lock
        dataSource = cpds;
        dataSourceKey = key;
    }

}

