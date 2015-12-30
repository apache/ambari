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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * Upgrade catalog for version 2.1.1.
 */
public class UpgradeCatalog211 extends AbstractUpgradeCatalog {
  private static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  private static final String HOST_COMPONENT_STATE_ID_COLUMN = "id";
  private static final String HOST_COMPONENT_STATE_INDEX = "idx_host_component_state";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog211.class);

  // this "id holder" is a field only for a test that verifies "big" 4 digit+
  // numbers are formatted correctly
  private AtomicLong m_hcsId = new AtomicLong(1);


  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog211(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.1.1";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

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
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    // change out the PK on hostcomponentstate
    executeHostComponentStateDDLUpdates();

    // make viewinstanceproperty.value & viewinstancedata.value nullable
    dbAccessor.setColumnNullable("viewinstanceproperty", "value", true);
    dbAccessor.setColumnNullable("viewinstancedata", "value", true);

  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    updateExistingConfigurations();
  }

  // ----- UpgradeCatalog211 --------------------------------------------

  /**
   * Iterates over the set of clusters to call service-specific configuration
   * update routines.
   *
   * @throws AmbariException
   *           if an error occurs while updating the configurations
   */
  protected void updateExistingConfigurations() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if ((clusterMap != null) && !clusterMap.isEmpty()) {
        // Iterate through the clusters and perform any configuration updates
        for (final Cluster cluster : clusterMap.values()) {
          updateKerberosConfigurations(cluster);

          /* *********************************************************
           * Add additional configuration update methods here
           * ********************************************************* */
        }
      }
    }
  }

  /**
   * Updates the Kerberos configurations for the given cluster
   * <p/>
   * Performs the following updates:
   * <ul>
   * <li>Rename <code>create_attributes_template</code> to
   * <code>ad_create_attributes_template</code></li>
   * </ul>
   *
   * @param cluster
   *          the cluster
   * @throws AmbariException
   *           if an error occurs while updating the configurations
   */
  protected void updateKerberosConfigurations(Cluster cluster) throws AmbariException {
    Config config = cluster.getDesiredConfigByType("kerberos-env");

    if (config != null) {
      // Rename create_attributes_template to ad_create_attributes_template
      String value = config.getProperties().get("create_attributes_template");
      Map<String, String> updates = Collections.singletonMap("ad_create_attributes_template", value);
      Set<String> removes = Collections.singleton("create_attributes_template");

      updateConfigurationPropertiesForCluster(cluster, "kerberos-env", updates, removes, true, false);
    }
  }

  /**
   * Perform the DDL updates required to add a new Primary Key ID column to the
   * {@code hostcomponentstate} table. This will perform the following actions:
   * <ul>
   * <li>Add a new column to hostcomponentstate named id</li>
   * <li>Populated id with an incrementing long, then make it non-NULL</li>
   * <li>Drop the existing PK on hostcomponentstate</li>
   * <li>Add a new surrogate PK on hostcomponentstate on the id column</li>
   * <li>Add an index on hostcomponentstate for host_id, component_name,
   * service_name, cluster_id</li>
   * </ul>
   *
   * @throws AmbariException
   * @throws SQLException
   */
  private void executeHostComponentStateDDLUpdates() throws AmbariException, SQLException {
    if (!dbAccessor.tableHasPrimaryKey(HOST_COMPONENT_STATE_TABLE, HOST_COMPONENT_STATE_ID_COLUMN)) {
      // add the new column, nullable for now until we insert unique IDs
      dbAccessor.addColumn(HOST_COMPONENT_STATE_TABLE,
          new DBColumnInfo(HOST_COMPONENT_STATE_ID_COLUMN, Long.class, null, null, true));

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
                "UPDATE {0} SET {1} = {2,number,#} WHERE cluster_id = {3} AND service_name = ''{4}'' AND component_name = ''{5}'' and host_id = {6,number,#}",
                HOST_COMPONENT_STATE_TABLE, HOST_COMPONENT_STATE_ID_COLUMN, m_hcsId.getAndIncrement(),
                clusterId, serviceName, componentName, hostId);

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

      // Add sequence for hostcomponentstate id
      addSequence("hostcomponentstate_id_seq", m_hcsId.get(), false);

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

      // create a new PK, matching the name of the constraint found in the SQL
      // files
      dbAccessor.addPKConstraint(HOST_COMPONENT_STATE_TABLE, "pk_hostcomponentstate", "id");

      // create index, ensuring column order matches that of the SQL files
      dbAccessor.createIndex(HOST_COMPONENT_STATE_INDEX, HOST_COMPONENT_STATE_TABLE, "host_id",
          "component_name", "service_name", "cluster_id");
    }
  }
}
