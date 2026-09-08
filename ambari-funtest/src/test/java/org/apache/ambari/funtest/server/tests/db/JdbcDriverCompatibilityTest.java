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
package org.apache.ambari.funtest.server.tests.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Properties;

import org.junit.Test;

public class JdbcDriverCompatibilityTest {

  @Test
  public void testMariaDbDriverContract() throws Exception {
    Driver driver = new org.mariadb.jdbc.Driver();

    assertTrue(driver.acceptsURL("jdbc:mariadb://localhost:3306/ambari"));
    assertTrue(driver.acceptsURL("jdbc:mysql://localhost:3306/ambari"));
  }

  @Test
  public void testSqlServerDriverContract() throws Exception {
    Driver driver = new com.microsoft.sqlserver.jdbc.SQLServerDriver();
    DriverPropertyInfo[] properties = driver.getPropertyInfo(
        "jdbc:sqlserver://localhost:1433;databaseName=ambari;"
            + "encrypt=false;trustServerCertificate=true",
        new Properties());

    assertTrue(driver.acceptsURL("jdbc:sqlserver://localhost:1433;databaseName=ambari"));
    assertEquals("ambari", propertyValue(properties, "databaseName"));
    assertEquals("false", propertyValue(properties, "encrypt"));
    assertEquals("true", propertyValue(properties, "trustServerCertificate"));
  }

  private static String propertyValue(DriverPropertyInfo[] properties, String name) {
    for (DriverPropertyInfo property : properties) {
      if (name.equals(property.name)) {
        return property.value;
      }
    }
    throw new AssertionError("Missing JDBC driver property " + name);
  }
}
