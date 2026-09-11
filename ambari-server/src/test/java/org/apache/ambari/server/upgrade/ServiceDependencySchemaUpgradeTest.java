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

import java.sql.SQLException;

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

public class ServiceDependencySchemaUpgradeTest {
  private Injector injector;
  private DBAccessor dbAccessor;
  private ServiceDependencySchemaUpgrade upgrade;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(org.apache.ambari.server.orm.OrmTestHelper.class).createCluster("schema-consumer");
    dbAccessor = injector.getInstance(DBAccessor.class);
    upgrade = new ServiceDependencySchemaUpgrade(dbAccessor);
    dropDependencyTables();
  }

  @After
  public void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testCreatesPortableSchemaAndRerunRepairsMissingForeignKey() throws Exception {
    upgrade.execute();
    dbAccessor.dropFKConstraint(ServiceDependencySchemaUpgrade.OPERATION,
        "fk_svc_dep_operation_binding");

    upgrade.execute();

    Assert.assertTrue(dbAccessor.tableHasForeignKey(ServiceDependencySchemaUpgrade.BINDING,
        "fk_svc_dep_consumer"));
    Assert.assertTrue(dbAccessor.tableHasForeignKey(ServiceDependencySchemaUpgrade.BINDING,
        "fk_svc_dep_provider"));
    Assert.assertTrue(dbAccessor.tableHasForeignKey(ServiceDependencySchemaUpgrade.OPERATION,
        "fk_svc_dep_operation_binding"));
    Assert.assertTrue(dbAccessor.tableHasForeignKey(ServiceDependencySchemaUpgrade.HOST_RESULT,
        "fk_svc_dep_host_snapshot"));
    Assert.assertTrue(dbAccessor.tableHasPrimaryKey(ServiceDependencySchemaUpgrade.HOST_RESULT,
        "operation_epoch"));
  }

  @Test
  public void testReplacesPrototypeHostResultPrimaryKeyWithEpochInclusiveHistoryKey()
      throws Exception {
    upgrade.execute();
    dbAccessor.dropPKConstraint(ServiceDependencySchemaUpgrade.HOST_RESULT,
        "pk_" + ServiceDependencySchemaUpgrade.HOST_RESULT);
    dbAccessor.addPKConstraint(ServiceDependencySchemaUpgrade.HOST_RESULT,
        "pk_" + ServiceDependencySchemaUpgrade.HOST_RESULT,
        "binding_id", "snapshot_version", "host_id", "dependency_type", "check_kind");

    Assert.assertFalse(dbAccessor.tableHasPrimaryKey(ServiceDependencySchemaUpgrade.HOST_RESULT,
        "operation_epoch"));
    upgrade.execute();

    Assert.assertTrue(dbAccessor.tableHasPrimaryKey(ServiceDependencySchemaUpgrade.HOST_RESULT,
        "operation_epoch"));
  }

  @Test
  public void testAddsDetachReplayIdentityToPrototypeFenceWithoutDroppingHistory()
      throws Exception {
    upgrade.execute();
    dbAccessor.dropColumn(ServiceDependencySchemaUpgrade.FENCE, "detach_operation_id");
    dbAccessor.dropColumn(ServiceDependencySchemaUpgrade.FENCE, "detach_request_hash");

    upgrade.execute();

    Assert.assertTrue(dbAccessor.tableHasColumn(
        ServiceDependencySchemaUpgrade.FENCE, "detach_operation_id"));
    Assert.assertTrue(dbAccessor.tableHasColumn(
        ServiceDependencySchemaUpgrade.FENCE, "detach_request_hash"));
  }

  @Test
  public void testActiveReferencesRejectMissingServicesButFenceRetainsHistoricalIdentity() throws Exception {
    upgrade.execute();

    dbAccessor.executeUpdate("INSERT INTO service_dependency_fence "
        + "(binding_id, final_epoch, immutable_spec_hash, dependency_type, "
        + "consumer_cluster_id, consumer_service_name, provider_cluster_id, provider_service_name, "
        + "namespace_hash, detach_operation_id, detach_request_hash, "
        + "detached_by_user_id, detach_timestamp) VALUES "
        + "('00000000-0000-0000-0000-000000000001', 4, 'sha256:history', 'HDFS', "
        + "9001, 'HBASE', 9002, 'HDFS', 'sha256:namespace', "
        + "'00000000-0000-0000-0000-000000000004', 'sha256:detach', 1, 10)");

    try {
      dbAccessor.executeUpdate("INSERT INTO service_dependency_binding "
          + "(binding_id, consumer_cluster_id, consumer_service_name, provider_cluster_id, "
          + "provider_service_name, dependency_type, state, row_version, operation_epoch, "
          + "desired_snapshot_version, snapshot_approval, provider_fingerprint, active_operation_id, "
          + "failure_retryable, created_by_user_id, updated_by_user_id, create_timestamp, update_timestamp) "
          + "VALUES ('00000000-0000-0000-0000-000000000002', 9001, 'HBASE', 9002, 'HDFS', "
          + "'HDFS', 'PROVISIONING', 0, 1, 1, 'APPROVED', 'sha256:provider', "
          + "'00000000-0000-0000-0000-000000000003', 1, 1, 1, 10, 10)");
      Assert.fail("Active binding must retain restrictive service references");
    } catch (SQLException expected) {
      Assert.assertTrue(expected.getMessage().toLowerCase().contains("referential")
          || expected.getMessage().toLowerCase().contains("constraint"));
    }
  }

  private void dropDependencyTables() throws SQLException {
    for (String table : new String[] {
        ServiceDependencySchemaUpgrade.HOST_RESULT,
        ServiceDependencySchemaUpgrade.OPERATION,
        ServiceDependencySchemaUpgrade.SNAPSHOT,
        ServiceDependencySchemaUpgrade.BINDING,
        ServiceDependencySchemaUpgrade.FENCE}) {
      if (dbAccessor.tableExists(table)) {
        dbAccessor.executeQuery("DROP TABLE " + table);
      }
    }
  }
}
