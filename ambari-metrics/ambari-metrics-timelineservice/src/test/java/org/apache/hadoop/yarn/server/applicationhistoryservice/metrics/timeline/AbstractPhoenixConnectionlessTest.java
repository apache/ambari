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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import org.apache.phoenix.jdbc.PhoenixEmbeddedDriver;
import org.apache.phoenix.jdbc.PhoenixTestDriver;
import org.apache.phoenix.query.BaseTest;
import org.apache.phoenix.util.PropertiesUtil;
import org.apache.phoenix.util.ReadOnlyProps;
import org.apache.phoenix.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import static org.apache.phoenix.util.PhoenixRuntime.TENANT_ID_ATTRIB;
import static org.apache.phoenix.util.TestUtil.TEST_PROPERTIES;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractPhoenixConnectionlessTest extends BaseTest {

  protected static String getUrl() {
    return TestUtil.PHOENIX_CONNECTIONLESS_JDBC_URL;
  }

  protected static String getUrl(String tenantId) {
    return getUrl() + ';' + TENANT_ID_ATTRIB + '=' + tenantId;
  }

  protected static PhoenixTestDriver driver;

  private static void startServer(String url) throws Exception {
    assertNull(driver);
    // only load the test driver if we are testing locally - for integration tests, we want to
    // test on a wider scale
    if (PhoenixEmbeddedDriver.isTestUrl(url)) {
      driver = initDriver(ReadOnlyProps.EMPTY_PROPS);
      assertTrue(DriverManager.getDriver(url) == driver);
      driver.connect(url, PropertiesUtil.deepCopy(TEST_PROPERTIES));
    }
  }

  protected static synchronized PhoenixTestDriver initDriver(ReadOnlyProps props) throws Exception {
    if (driver == null) {
      driver = new PhoenixTestDriver(props);
      DriverManager.registerDriver(driver);
    }
    return driver;
  }

  private String connUrl;

  @Before
  public void setup() throws Exception {
    connUrl = getUrl();
    startServer(connUrl);
  }

  @Test
  public void testStorageSystemInitialized() throws Exception {
    String sampleDDL = "CREATE TABLE TEST_METRICS (TEST_COLUMN VARCHAR " +
      "CONSTRAINT pk PRIMARY KEY (TEST_COLUMN)) DATA_BLOCK_ENCODING='FAST_DIFF', " +
      "IMMUTABLE_ROWS=true, TTL=86400, COMPRESSION='SNAPPY'";

    Connection conn = null;
    PreparedStatement stmt = null;
    try {
      conn = DriverManager.getConnection(connUrl);
      stmt = conn.prepareStatement(sampleDDL);
      stmt.execute();
      conn.commit();
    } finally {
      if (stmt != null) {
        stmt.close();
      }
      if (conn != null) {
        conn.close();
      }
    }
  }

  @After
  public void tearDown() throws Exception {
    if (driver != null) {
      try {
        driver.close();
      } finally {
        PhoenixTestDriver phoenixTestDriver = driver;
        driver = null;
        DriverManager.deregisterDriver(phoenixTestDriver);
      }
    }
  }
}
