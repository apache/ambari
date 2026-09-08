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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.ambari.server.orm.PersistenceType;
import org.junit.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class DDLCreateIT {

  @Test
  public void mariaDb() {
    testSchemaCreate(() -> new MariaDBContainer("mariadb:10.11").withConfigurationOverride(null).withInitScript("Ambari-DDL-MySQL-CREATE.sql"));
  }

  @Test
  public void mysql() {
    testSchemaCreate(() -> new MySQLContainer("mysql:5.7").withConfigurationOverride(null).withInitScript("Ambari-DDL-MySQL-CREATE.sql"));
  }

  @Test
  public void postgres() {
    testSchemaCreate(() -> new PostgreSQLContainer("postgres:9.6").withInitScript("Ambari-DDL-Postgres-CREATE.sql"));
    testSchemaCreate(() -> new PostgreSQLContainer("postgres:10").withInitScript("Ambari-DDL-Postgres-CREATE.sql"));
  }

  @Test
  public void sqlServer() {
    String architecture = System.getProperty("os.arch");
    assumeFalse("The official SQL Server container image does not support ARM",
        "aarch64".equals(architecture) || "arm64".equals(architecture));

    testSchemaCreate(() -> new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
        .acceptLicense()
        .withInitScript("Ambari-DDL-SQLServer-CREATE.sql"));
  }

  private static void testSchemaCreate(Supplier<? extends JdbcDatabaseContainer> containerSupplier) {
    Path passwordFile = null;
    try (JdbcDatabaseContainer container = containerSupplier.get()) {
      container.start();
      passwordFile = Files.createTempFile("ambari-database-password", ".txt");
      Files.writeString(passwordFile, container.getPassword(), UTF_8);

      Properties props = new Properties();
      props.put(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), PersistenceType.REMOTE.getValue());
      props.put(Configuration.SERVER_DB_NAME.getKey(), container.getDatabaseName());
      props.put(Configuration.SERVER_JDBC_DRIVER.getKey(), container.getDriverClassName());
      props.put(Configuration.SERVER_JDBC_URL.getKey(), container.getJdbcUrl().replace("mariadb", "mysql"));
      props.put(Configuration.SERVER_JDBC_USER_NAME.getKey(), container.getUsername());
      props.put(Configuration.SERVER_JDBC_USER_PASSWD.getKey(), passwordFile.toString());
      Configuration config = new Configuration(props);
      DBAccessor db = new DBAccessorImpl(config);

      assertTrue(db.tableExists("metainfo"));
      assertEquals(new Integer(1),
        db.getIntColumnValues("users", "user_id", new String[] { "user_name" }, new String[] { "admin" }, false).get(0)
      );
    } catch (IOException | SQLException e) {
      throw new AssertionError("Could not verify the Ambari database schema", e);
    } finally {
      if (passwordFile != null) {
        try {
          Files.deleteIfExists(passwordFile);
        } catch (IOException e) {
          throw new AssertionError("Could not remove the temporary database password file", e);
        }
      }
    }
  }

}
