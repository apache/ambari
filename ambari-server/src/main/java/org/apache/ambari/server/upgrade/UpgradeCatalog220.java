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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * Upgrade catalog for version 2.2.0.
 */
public class UpgradeCatalog220 extends AbstractUpgradeCatalog {
  private static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  private static final String HOST_ID_COL = "host_id";
  private static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  private static final String HOST_COMPONENT_STATE_ID_COLUMN = "id";
  private static final String HOST_COMPONENT_STATE_INDEX = "idx_host_component_state";

  @Inject
  DaoUtils daoUtils;


  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.1.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.2.0";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog220.class);

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog220(Injector injector) {
    super(injector);
    this.injector = injector;

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE,
        new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));

    // change out the PK on hostcomponentstate
    executeHostComponentStateDDLUpdates();
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
  }

  /**
   * Perform the DDL updates required to add a new Primary Key ID column to the
   * {@code hostcomponentstate} table. This will perform the following actions:
   * <ul>
   * <li>Add a new column to hostcomponentstate named id</li>
   * <li>Populated id with an incrementing long, then make it non-NULL</li>
   * <li>Drop the existing PK on hostcomponentstate</li>
   * <li>Add a new surrogate PK on hostcomponentstate on the id column</li>
   * <li>Add an index on hostcomponentstate for host_id, component_name, service_name, cluster_id</li>
   * </ul>
   *
   * @throws AmbariException
   * @throws SQLException
   */
  private void executeHostComponentStateDDLUpdates() throws AmbariException, SQLException {
    // add the new column, nullable for now until we insert unique IDs
    dbAccessor.addColumn(HOST_COMPONENT_STATE_TABLE,
        new DBColumnInfo(HOST_COMPONENT_STATE_ID_COLUMN, Long.class, null, null, true));

    // insert sequence values
    AtomicLong id = new AtomicLong(1);
    Statement statement = null;
    ResultSet resultSet = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        String selectSQL = MessageFormat.format(
            "SELECT cluster_id, service_name, component_name, host_id FROM {0}",
            HOST_COMPONENT_STATE_TABLE);

        resultSet = statement.executeQuery(selectSQL);
        while (resultSet.next()) {
          final Long clusterId = resultSet.getLong("cluster_id");
          final String serviceName = resultSet.getString("service_name");
          final String componentName = resultSet.getString("component_name");
          final Long hostId = resultSet.getLong("host_id");

          String updateSQL = MessageFormat.format(
              "UPDATE {0} SET {1} = {2} WHERE cluster_id = {3} AND service_name = ''{4}'' AND component_name = ''{5}'' and host_id = {6}",
              HOST_COMPONENT_STATE_TABLE, HOST_COMPONENT_STATE_ID_COLUMN, id.getAndIncrement(),
              clusterId, serviceName,
              componentName, hostId);

          dbAccessor.executeQuery(updateSQL);
        }
      }
    } finally {
      JdbcUtils.closeResultSet(resultSet);
      JdbcUtils.closeStatement(statement);
    }

    // make the column NON NULL now
    dbAccessor.alterColumn(HOST_COMPONENT_STATE_TABLE,
        new DBColumnInfo(HOST_COMPONENT_STATE_ID_COLUMN, Long.class, null, null, false));

    // drop the current PK
    String primaryKeyConstraintName = null;
    Configuration.DatabaseType databaseType = configuration.getDatabaseType();
    switch (databaseType) {
      case POSTGRES: {
        primaryKeyConstraintName = "hostcomponentstate_pkey";
        break;
      }
      case ORACLE:
      case SQL_SERVER: {
        // Oracle and SQL Server require us to lookup the PK name
        primaryKeyConstraintName = dbAccessor.getPrimaryKeyConstraintName(
            HOST_COMPONENT_STATE_TABLE);

        break;
      }
      default:
        break;
    }

    if (databaseType == DatabaseType.MYSQL) {
      String mysqlDropQuery = MessageFormat.format("ALTER TABLE {0} DROP PRIMARY KEY",
          HOST_COMPONENT_STATE_TABLE);

      dbAccessor.executeQuery(mysqlDropQuery, true);
    } else {
      // warn if we can't find it
      if (null == primaryKeyConstraintName) {
        LOG.warn("Unable to determine the primary key constraint name for {}",
            HOST_COMPONENT_STATE_TABLE);
      } else {
        dbAccessor.dropPKConstraint(HOST_COMPONENT_STATE_TABLE, primaryKeyConstraintName, true);
      }
    }

    // create a new PK, matching the name of the constraint found in the SQL files
    dbAccessor.addPKConstraint(HOST_COMPONENT_STATE_TABLE, "pk_hostcomponentstate", "id");

    // create index, ensuring column order matches that of the SQL files
    dbAccessor.createIndex(HOST_COMPONENT_STATE_INDEX, HOST_COMPONENT_STATE_TABLE, "host_id",
        "component_name", "service_name", "cluster_id");
  }

  // ----- UpgradeCatalog ----------------------------------------------------

}
