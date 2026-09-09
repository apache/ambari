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

import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

public class ScopedWorkflowSchemaUpgradeTest {
  private Injector injector;
  private DBAccessor dbAccessor;
  private ScopedWorkflowSchemaUpgrade upgrade;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    dbAccessor = injector.getInstance(DBAccessor.class);
    upgrade = new ScopedWorkflowSchemaUpgrade(dbAccessor);
    dbAccessor.executeQuery("DROP TABLE scoped_workflow_state");
  }

  @After
  public void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testCreatesPortableLobTableAndIsRetrySafe() throws Exception {
    upgrade.execute();
    upgrade.execute();

    Assert.assertTrue(dbAccessor.tableHasColumn(
        ScopedWorkflowSchemaUpgrade.TABLE_NAME, ScopedWorkflowSchemaUpgrade.CREATED_CLUSTER_ID_COLUMN));

    String payload = "x".repeat(100_000);
    try (PreparedStatement statement = dbAccessor.getConnection().prepareStatement(
        "INSERT INTO scoped_workflow_state "
            + "(scope_key, revision, workflow, phase, payload) VALUES (?, ?, ?, ?, ?)")) {
      statement.setString(1, "clusters:1");
      statement.setLong(2, 1L);
      statement.setString(3, "ADD_HOST");
      statement.setString(4, "HOSTS");
      statement.setString(5, payload);
      statement.executeUpdate();
    }

    try (ResultSet result = dbAccessor.getConnection().createStatement().executeQuery(
        "SELECT payload FROM scoped_workflow_state WHERE scope_key = 'clusters:1'")) {
      Assert.assertTrue(result.next());
      Assert.assertEquals(payload, result.getString(1));
    }
  }

  @Test
  public void testExistingWorkflowTableReceivesCreationMarkerWithoutDataReset() throws Exception {
    upgrade.execute();
    dbAccessor.executeUpdate("INSERT INTO scoped_workflow_state "
        + "(scope_key, revision, workflow, phase, payload) VALUES "
        + "('scoped:workflow:drafts:7:00000000-0000-0000-0000-000000000001', 4, "
        + "'CLUSTER_CREATE', 'REVIEW', '{}')");
    dbAccessor.executeQuery("ALTER TABLE scoped_workflow_state DROP COLUMN created_cluster_id");

    upgrade.execute();

    try (ResultSet result = dbAccessor.getConnection().createStatement().executeQuery(
        "SELECT revision, phase, payload, created_cluster_id FROM scoped_workflow_state")) {
      Assert.assertTrue(result.next());
      Assert.assertEquals(4, result.getLong(1));
      Assert.assertEquals("REVIEW", result.getString(2));
      Assert.assertEquals("{}", result.getString(3));
      Assert.assertNull(result.getObject(4));
    }
  }
}
