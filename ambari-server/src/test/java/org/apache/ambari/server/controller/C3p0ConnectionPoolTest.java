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
package org.apache.ambari.server.controller;

import static org.eclipse.persistence.config.PersistenceUnitProperties.NON_JTA_DATASOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.ConnectionPoolType;
import org.apache.ambari.server.orm.PersistenceType;
import org.junit.jupiter.api.Test;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class C3p0ConnectionPoolTest {

  private static final String H2_DRIVER = "org.h2.Driver";
  private static final String JDBC_URL = "jdbc:h2:mem:c3p0_pool;MODE=LEGACY";

  @Test
  public void testConfiguredDataSourceConnects() throws Exception {
    ComboPooledDataSource dataSource = getDataSource(databaseProperties(H2_DRIVER));
    try {
      assertEquals(1, dataSource.getMinPoolSize());
      assertEquals(2, dataSource.getMaxPoolSize());
      assertEquals("SELECT 1", dataSource.getPreferredTestQuery());

      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT 1")) {
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt(1));
      }
    } finally {
      dataSource.close();
    }
  }

  @Test
  public void testPoolCanBeCreatedAfterDatabaseConfigurationIsCorrected() throws Exception {
    Properties invalidProperties = databaseProperties(H2_DRIVER);
    invalidProperties.setProperty(Configuration.SERVER_JDBC_URL.getKey(),
        "jdbc:h2:tcp://127.0.0.1:1/mem:unavailable");
    ComboPooledDataSource failedDataSource = getDataSource(invalidProperties);
    try {
      assertThrows(SQLException.class, failedDataSource::getConnection);
    } finally {
      failedDataSource.close();
    }

    ComboPooledDataSource dataSource = getDataSource(databaseProperties(H2_DRIVER));
    try (Connection connection = dataSource.getConnection()) {
      assertFalse(connection.isClosed());
    } finally {
      dataSource.close();
    }
  }

  @Test
  public void testLegacyUserOverridesPayloadIsNotDeserialized() throws Exception {
    ComboPooledDataSource dataSource = new ComboPooledDataSource();
    try {
      Map<String, Map<String, String>> overrides = new HashMap<>();
      Map<String, String> userProperties = new HashMap<>();
      userProperties.put("preferredTestQuery", "SELECT 1");
      overrides.put("ambari", userProperties);

      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
        output.writeObject(overrides);
      }

      String legacyPayload = "HexAsciiSerializedMap["
          + HexFormat.of().formatHex(bytes.toByteArray()) + "]";
      dataSource.setUserOverridesAsString(legacyPayload);

      assertFalse(dataSource.getUserOverrides().containsKey("ambari"));
    } finally {
      dataSource.close();
    }
  }

  private static ComboPooledDataSource getDataSource(Properties configurationProperties) {
    Object dataSource = ControllerModule.getPersistenceProperties(
        new Configuration(configurationProperties)).get(NON_JTA_DATASOURCE);
    return assertInstanceOf(ComboPooledDataSource.class, dataSource);
  }

  private static Properties databaseProperties(String driver) {
    Properties properties = new Properties();
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(),
        PersistenceType.REMOTE.getValue());
    properties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), JDBC_URL);
    properties.setProperty(Configuration.SERVER_JDBC_DRIVER.getKey(), driver);
    properties.setProperty(Configuration.SERVER_JDBC_USER_NAME.getKey(), "sa");
    properties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL.getKey(),
        ConnectionPoolType.C3P0.getName());
    properties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_MIN_SIZE.getKey(), "1");
    properties.setProperty(Configuration.SERVER_JDBC_CONNECTION_POOL_MAX_SIZE.getKey(), "2");
    properties.setProperty(
        Configuration.SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_ATTEMPTS.getKey(), "1");
    properties.setProperty(
        Configuration.SERVER_JDBC_CONNECTION_POOL_ACQUISITION_RETRY_DELAY.getKey(), "0");
    return properties;
  }
}
