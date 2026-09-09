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

public class ClusterCreationSchemaUpgradeTest {
  private Injector injector;
  private DBAccessor dbAccessor;
  private ClusterCreationSchemaUpgrade upgrade;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    dbAccessor = injector.getInstance(DBAccessor.class);
    upgrade = new ClusterCreationSchemaUpgrade(dbAccessor);
    dbAccessor.executeQuery("DROP INDEX uq_clusters_creation_draft");
    dbAccessor.executeQuery("SET REFERENTIAL_INTEGRITY FALSE");
  }

  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testMigrationIsRetrySafeAndAllowsMultipleTokenlessClusters() throws Exception {
    insertCluster(1, "ordinary-a", null, null);
    insertCluster(2, "ordinary-b", null, null);

    upgrade.execute();
    upgrade.execute();

    insertCluster(3, "draft-a", 7, "00000000-0000-0000-0000-000000000001");
    try {
      insertCluster(4, "draft-b", 7, "00000000-0000-0000-0000-000000000001");
      fail("Expected the database to reject reuse of a cluster creation draft");
    } catch (SQLException e) {
      Assert.assertTrue(e.getMessage().toLowerCase().contains("uq_clusters_creation_draft"));
    }
  }

  @Test
  public void testMigrationAddsIdentityColumnsToExistingSchema() throws Exception {
    dbAccessor.executeQuery("ALTER TABLE clusters DROP COLUMN creator_user_id");
    dbAccessor.executeQuery("ALTER TABLE clusters DROP COLUMN creation_draft_id");

    upgrade.execute();

    Assert.assertTrue(dbAccessor.tableHasColumn("clusters", "creator_user_id"));
    Assert.assertTrue(dbAccessor.tableHasColumn("clusters", "creation_draft_id"));
    Assert.assertTrue(dbAccessor.tableHasIndex("clusters", false, "uq_clusters_creation_draft"));
  }

  @Test
  public void testDuplicateIdentityStopsMigrationWithoutDeletingClusters() throws Exception {
    insertCluster(1, "draft-a", 7, "00000000-0000-0000-0000-000000000001");
    insertCluster(2, "draft-b", 7, "00000000-0000-0000-0000-000000000001");

    try {
      upgrade.execute();
      fail("Expected duplicate creation identities to stop the migration");
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("creator_user_id=7"));
      Assert.assertTrue(e.getMessage().contains("No clusters were removed or renamed"));
    }

    try (Statement statement = dbAccessor.getConnection().createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT COUNT(*) FROM clusters WHERE creator_user_id=7")) {
      Assert.assertTrue(resultSet.next());
      Assert.assertEquals(2, resultSet.getInt(1));
    }
  }

  private void insertCluster(long id, String name, Integer creatorUserId, String draftId)
      throws SQLException {
    String creator = creatorUserId == null ? "NULL" : creatorUserId.toString();
    String draft = draftId == null ? "NULL" : "'" + draftId + "'";
    dbAccessor.executeUpdate(String.format(
        "INSERT INTO clusters (cluster_id, resource_id, cluster_info, cluster_name, provisioning_state, "
            + "security_type, desired_cluster_state, desired_stack_id, creator_user_id, creation_draft_id) "
            + "VALUES (%d, %d, '', '%s', 'INIT', 'NONE', '', 1, %s, %s)",
        id, id, name, creator, draft));
  }
}
