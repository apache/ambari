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

import org.junit.Test;

public class JdbcDriverCompatibilityTest {

  @Test
  public void testMariaDbDriverContract() throws Exception {
    Driver driver = new org.mariadb.jdbc.Driver();

    assertTrue(driver.acceptsURL("jdbc:mariadb://localhost:3306/ambari"));
    assertEquals(2, driver.getMajorVersion());
    assertEquals(7, driver.getMinorVersion());
  }

  @Test
  public void testSqlServerDriverContract() throws Exception {
    Driver driver = new com.microsoft.sqlserver.jdbc.SQLServerDriver();

    assertTrue(driver.acceptsURL("jdbc:sqlserver://localhost:1433;databaseName=ambari"));
    assertEquals(13, driver.getMajorVersion());
    assertEquals(4, driver.getMinorVersion());
  }
}
