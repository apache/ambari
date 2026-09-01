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

package org.apache.ambari.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

public class DBConnectionVerificationTest {
  private static final String URL = "jdbc:ambari-verification:test";

  @BeforeClass
  public static void registerDriver() throws SQLException {
    DriverManager.registerDriver(new VerificationDriver());
  }

  @Test
  public void connectsWithFramedPasswordAndClosesConnection() throws Exception {
    VerificationDriver.reset();

    int result = DBConnectionVerification.run(
        new String[] {URL, "db user", VerificationDriver.class.getName()},
        frame("line one\nline two\0\u4e2d\u6587"),
        new PrintStream(new ByteArrayOutputStream()));

    assertEquals(0, result);
    assertEquals("db user", VerificationDriver.properties.getProperty("user"));
    assertEquals(
        "line one\nline two\0\u4e2d\u6587",
        VerificationDriver.properties.getProperty("password"));
    assertTrue(VerificationDriver.closed);
  }

  @Test
  public void integratedSecurityDoesNotPassCredentials() throws Exception {
    VerificationDriver.reset();

    int result = DBConnectionVerification.run(
        new String[] {
          URL + ";integratedSecurity=true",
          "ignored-user",
          VerificationDriver.class.getName()
        },
        frame("ignored-password"),
        new PrintStream(new ByteArrayOutputStream()));

    assertEquals(0, result);
    assertTrue(VerificationDriver.properties.isEmpty());
    assertTrue(VerificationDriver.closed);
  }

  @Test
  public void rejectsLegacyPasswordArgument() throws Exception {
    VerificationDriver.reset();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    int result = DBConnectionVerification.run(
        new String[] {
          URL, "user", "password-on-argv", VerificationDriver.class.getName()
        },
        frame("ignored"),
        new PrintStream(output));

    assertEquals(1, result);
    assertTrue(output.toString(StandardCharsets.UTF_8.name()).contains("Usage"));
    assertTrue(VerificationDriver.properties.isEmpty());
  }

  @Test
  public void rejectsTruncatedTrailingAndMalformedFrames() throws Exception {
    byte[][] invalidFrames = {
      new byte[] {0, 0, 0, 5, 'x'},
      new byte[] {0, 0, 0, 1, 'x', 'y'},
      new byte[] {0, 0, 0, 1, (byte) 0xff},
      new byte[] {0x00, 0x10, 0x00, 0x01}
    };

    for (byte[] invalidFrame : invalidFrames) {
      VerificationDriver.reset();
      int result = DBConnectionVerification.run(
          new String[] {URL, "user", VerificationDriver.class.getName()},
          new ByteArrayInputStream(invalidFrame),
          new PrintStream(new ByteArrayOutputStream()));
      assertEquals(1, result);
      assertTrue(VerificationDriver.properties.isEmpty());
    }
  }

  private static ByteArrayInputStream frame(String password) throws Exception {
    byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (DataOutputStream data = new DataOutputStream(output)) {
      data.writeInt(passwordBytes.length);
      data.write(passwordBytes);
    }
    return new ByteArrayInputStream(output.toByteArray());
  }

  public static final class VerificationDriver implements Driver {
    private static Properties properties = new Properties();
    private static boolean closed;

    static void reset() {
      properties = new Properties();
      closed = false;
    }

    @Override
    public Connection connect(String url, Properties info) {
      if (!acceptsURL(url)) {
        return null;
      }
      properties = new Properties();
      properties.putAll(info);
      return (Connection) Proxy.newProxyInstance(
          Connection.class.getClassLoader(),
          new Class<?>[] {Connection.class},
          (proxy, method, args) -> {
            if ("close".equals(method.getName())) {
              closed = true;
              return null;
            }
            if ("isClosed".equals(method.getName())) {
              return closed;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
              return false;
            }
            if (returnType == int.class) {
              return 0;
            }
            if (returnType == long.class) {
              return 0L;
            }
            return null;
          });
    }

    @Override
    public boolean acceptsURL(String url) {
      return url != null && url.startsWith("jdbc:ambari-verification:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
      return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
      return 1;
    }

    @Override
    public int getMinorVersion() {
      return 0;
    }

    @Override
    public boolean jdbcCompliant() {
      return false;
    }

    @Override
    public Logger getParentLogger() {
      return Logger.getGlobal();
    }
  }
}
