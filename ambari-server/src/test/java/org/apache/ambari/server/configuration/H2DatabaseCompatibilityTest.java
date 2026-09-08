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
package org.apache.ambari.server.configuration;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

import org.junit.Test;

public class H2DatabaseCompatibilityTest {

  @Test
  public void testInMemoryDatabaseSupportsAmbariSchemaIdentifiers() throws Exception {
    try (Connection connection = DriverManager.getConnection(
        Configuration.JDBC_IN_MEMORY_URL,
        Configuration.JDBC_IN_MEMORY_USER,
        Configuration.JDBC_IN_MEMORY_PASSWORD);
        Statement statement = connection.createStatement()) {
      assertEquals(Configuration.DEFAULT_H2_SCHEMA.toUpperCase(Locale.ROOT), connection.getSchema());

      statement.execute("CREATE TABLE h2_compatibility (month INTEGER, value VARCHAR(32))");
      statement.execute("INSERT INTO h2_compatibility (month, value) VALUES (9, 'compatible')");

      try (ResultSet resultSet = statement.executeQuery(
          "SELECT value FROM h2_compatibility WHERE month = 9")) {
        resultSet.next();
        assertEquals("compatible", resultSet.getString(1));
      }

      statement.execute("DROP TABLE h2_compatibility");
    }
  }
}
