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

public class TopologyProvisioningSchemaUpgradeTest {
  private Injector injector;
  private DBAccessor dbAccessor;
  private TopologyProvisioningSchemaUpgrade upgrade;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    dbAccessor = injector.getInstance(DBAccessor.class);
    upgrade = new TopologyProvisioningSchemaUpgrade(dbAccessor);
    dbAccessor.executeQuery("DROP TABLE IF EXISTS topology_request");
    dbAccessor.executeQuery("DROP TABLE IF EXISTS repo_version");
    dbAccessor.executeQuery("CREATE TABLE repo_version ("
        + "repo_version_id BIGINT NOT NULL, "
        + "CONSTRAINT PK_repo_version PRIMARY KEY (repo_version_id))");
    dbAccessor.executeQuery("CREATE TABLE topology_request ("
        + "id BIGINT NOT NULL, action VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, "
        + "bp_name VARCHAR(100) NOT NULL, cluster_properties VARCHAR(3000), "
        + "cluster_attributes VARCHAR(3000), description VARCHAR(1024), "
        + "provision_action VARCHAR(255), CONSTRAINT PK_topology_request PRIMARY KEY (id))");
  }

  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testMigrationAddsDurableIntentColumnsAndRepositoryForeignKey() throws Exception {
    upgrade.execute();
    upgrade.execute();

    Assert.assertTrue(dbAccessor.tableHasColumn("topology_request", "repository_version_id"));
    Assert.assertTrue(dbAccessor.tableHasColumn("topology_request", "specification_hash"));
    Assert.assertTrue(dbAccessor.tableHasColumn("topology_request", "provisioning_state"));
    Assert.assertTrue(dbAccessor.tableHasForeignKey(
        "topology_request", "FK_topology_request_repo_ver"));
  }
}
