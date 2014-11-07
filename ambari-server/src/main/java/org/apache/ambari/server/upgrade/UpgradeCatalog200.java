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
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * Upgrade catalog for version 2.0.0.
 */
public class UpgradeCatalog200 extends AbstractUpgradeCatalog {

  private static final String ALERT_TABLE_DEFINITION = "alert_definition";

  @Inject
  private DaoUtils daoUtils;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "1.7.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.0.0";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger
      (UpgradeCatalog200.class);

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog200(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    prepareRollingUpgradesDDL();

    // add ignore_host column to alert_definition
    dbAccessor.addColumn(ALERT_TABLE_DEFINITION, new DBColumnInfo(
        "ignore_host", Short.class, 1, 0, false));

    ddlUpdateRepositoryVersion();
  }

  /**
   * Creates repoversion table and all its constraints and dependencies.
   *
   * @throws SQLException if SQL error happens
   */
  private void ddlUpdateRepositoryVersion() throws SQLException {
    final List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("repoversion_id",  Long.class,    null,  null, false));
    columns.add(new DBColumnInfo("stack",           String.class,  255,   null, false));
    columns.add(new DBColumnInfo("version",         String.class,  255,   null, false));
    columns.add(new DBColumnInfo("display_name",    String.class,  128,   null, false));
    columns.add(new DBColumnInfo("upgrade_package", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("repositories",    char[].class,  32672, null, false));
    dbAccessor.createTable("repoversion", columns, "repoversion_id");
    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, sequence_value) VALUES('repoversion_id_seq', 0)", false);
    dbAccessor.executeQuery("ALTER TABLE repoversion ADD CONSTRAINT UQ_repoversion_display_name UNIQUE (display_name)");
    dbAccessor.executeQuery("ALTER TABLE repoversion ADD CONSTRAINT UQ_repoversion_stack_version UNIQUE (stack, version)");
  }

  /**
   * Add any columns, tables, and keys needed for Rolling Upgrades.
   * @throws SQLException
   */
  private void prepareRollingUpgradesDDL() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<DBAccessor.DBColumnInfo>();

    // New columns
    dbAccessor.addColumn("hostcomponentstate", new DBAccessor.DBColumnInfo("upgrade_state",
        String.class, 32, "NONE", false));

    // New tables
    columns.add(new DBAccessor.DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("stack", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("version", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("state", String.class, 32, null, false));
    columns.add(new DBAccessor.DBColumnInfo("start_time", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("end_time", Long.class, null, null, true));
    columns.add(new DBAccessor.DBColumnInfo("user_name", String.class, 32, null, true));
    dbAccessor.createTable("cluster_version", columns, "id");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("host_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("stack", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("version", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("state", String.class, 32, null, false));
    dbAccessor.createTable("host_version", columns, "id");

    // Foreign Key Constraints
    dbAccessor.addFKConstraint("cluster_version", "FK_cluster_version_cluster_id", "cluster_id", "clusters", "cluster_id", false);
    dbAccessor.addFKConstraint("host_version", "FK_host_version_host_name", "host_name", "hosts", "host_name", false);

    // New sequences
    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, sequence_value) VALUES('cluster_version_id_seq', 0)", false);
    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, sequence_value) VALUES('host_version_id_seq', 0)", false);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {

  }
}
