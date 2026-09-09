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
package org.apache.ambari.server.upgrade;

import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class HostMembershipSchemaUpgradeTest {
  private Injector injector;
  private DBAccessor dbAccessor;
  private HostMembershipSchemaUpgrade upgrade;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    dbAccessor = injector.getInstance(DBAccessor.class);
    upgrade = new HostMembershipSchemaUpgrade(dbAccessor);

    dbAccessor.executeQuery("DROP TABLE ClusterHostMapping");
    dbAccessor.executeQuery("CREATE TABLE ClusterHostMapping (" +
        "cluster_id BIGINT NOT NULL, host_id BIGINT NOT NULL, " +
        "CONSTRAINT PK_ClusterHostMapping PRIMARY KEY (cluster_id, host_id))");
  }

  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testCleanMigrationIsRetrySafeAndConstraintIsAuthoritative() throws Exception {
    upgrade.execute();
    upgrade.execute();

    dbAccessor.executeUpdate("INSERT INTO ClusterHostMapping (cluster_id, host_id) VALUES (1, 10)");
    try {
      dbAccessor.executeUpdate("INSERT INTO ClusterHostMapping (cluster_id, host_id) VALUES (2, 10)");
      fail("Expected the unique host constraint to reject a second cluster owner");
    } catch (SQLException e) {
      Assert.assertTrue(e.getMessage().contains("UQ_CLUSTERHOSTMAPPING_HOST_ID"));
    }
  }

  @Test
  public void testDuplicateMigrationFailsWithoutChangingMappings() throws Exception {
    dbAccessor.executeUpdate("INSERT INTO ClusterHostMapping (cluster_id, host_id) VALUES (1, 10)");
    dbAccessor.executeUpdate("INSERT INTO ClusterHostMapping (cluster_id, host_id) VALUES (2, 10)");

    try {
      upgrade.execute();
      fail("Expected duplicate legacy mappings to stop the migration");
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("host_id=10 (2 mappings)"));
      Assert.assertTrue(e.getMessage().contains("No mappings were changed"));
    }

    try (Statement statement = dbAccessor.getConnection().createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT COUNT(*) FROM ClusterHostMapping WHERE host_id = 10")) {
      Assert.assertTrue(resultSet.next());
      Assert.assertEquals(2, resultSet.getInt(1));
    }
  }
}
