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
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;


/**
 * Upgrade catalog for version 2.1.0.
 */
public class UpgradeCatalog210 extends AbstractUpgradeCatalog {

  private static final String CLUSTERS_TABLE = "clusters";
  private static final String HOSTS_TABLE = "hosts";
  private static final String HOST_COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
  private static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  private static final String HOST_STATE_TABLE = "hoststate";
  private static final String HOST_VERSION_TABLE = "host_version";
  private static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  private static final String HOST_CONFIG_MAPPING_TABLE = "hostconfigmapping";
  private static final String CONFIG_GROUP_HOST_MAPPING_TABLE = "configgrouphostmapping";
  private static final String KERBEROS_PRINCIPAL_HOST_TABLE = "kerberos_principal_host";
  private static final String CLUSTER_HOST_MAPPING_TABLE = "ClusterHostMapping";
  private static final String USER_WIDGET_TABLE = "user_widget";
  private static final String WIDGET_LAYOUT_TABLE = "widget_layout";
  private static final String WIDGET_LAYOUT_USER_WIDGET_TABLE = "widget_layout_user_widget";
  private static final String VIEW_INSTANCE_TABLE = "viewinstance";
  private static final String VIEW_PARAMETER_TABLE = "viewparameter";
  private static final String STACK_TABLE_DEFINITION = "stack";

  private static final String HOST_ID_COL = "host_id";

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.0.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.1.0";
  }

  /**
   * {@inheritDoc}
   */
  public String[] getCompatibleVersions() {
    return new String[] {"*"};
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger
      (UpgradeCatalog210.class);

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog210(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    executeHostsDDLUpdates();
    executeWidgetDDLUpdates();
    executeStackDDLUpdates();
  }

  /**
   * Execute all of the hosts DDL updates.
   *
   * @throws org.apache.ambari.server.AmbariException
   * @throws java.sql.SQLException
   */
  private void executeHostsDDLUpdates() throws AmbariException, SQLException {
    Configuration.DatabaseType databaseType = configuration.getDatabaseType();

    dbAccessor.addColumn(HOSTS_TABLE, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));

    // Sequence value for the hosts table primary key. First record will be 1, so ambari_sequence value must be 0.
    Long hostId = 0L;
    ResultSet resultSet = null;
    try {
      // Notice that hosts are ordered by host_id ASC, so any null values are last.
      resultSet = dbAccessor.executeSelect("SELECT host_name, host_id FROM hosts ORDER BY host_id ASC, host_name ASC");
      hostId = populateHostsId(resultSet);
    } finally {
      if (resultSet != null) {
        resultSet.close();
      }
    }

    // Insert host id number into ambari_sequences
    dbAccessor.executeQuery("INSERT INTO ambari_sequences (sequence_name, sequence_value) VALUES ('host_id_seq', " + hostId + ")");

    // Make the hosts id non-null after all the values are populated
    if (databaseType == Configuration.DatabaseType.DERBY) {
      // This is a workaround for UpgradeTest.java unit test
      dbAccessor.executeQuery("ALTER TABLE " + HOSTS_TABLE + " ALTER column " + HOST_ID_COL + " NOT NULL");
    } else {
      dbAccessor.alterColumn(HOSTS_TABLE, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, false));
    }


    // Drop the 8 FK constraints in the host-related tables. They will be recreated later after the PK is changed.
    // The only host-related table not being included is alert_history.
    if (databaseType == Configuration.DatabaseType.DERBY) {
      dbAccessor.executeQuery("ALTER TABLE " + HOST_COMPONENT_STATE_TABLE + " DROP CONSTRAINT hostcomponentstate_host_name");
      dbAccessor.executeQuery("ALTER TABLE " + HOST_COMPONENT_DESIRED_STATE_TABLE + " DROP CONSTRAINT hstcmponentdesiredstatehstname");
      dbAccessor.executeQuery("ALTER TABLE " + HOST_ROLE_COMMAND_TABLE + " DROP CONSTRAINT FK_host_role_command_host_name");
      dbAccessor.executeQuery("ALTER TABLE " + HOST_STATE_TABLE + " DROP CONSTRAINT FK_hoststate_host_name");
      dbAccessor.executeQuery("ALTER TABLE " + HOST_VERSION_TABLE + " DROP CONSTRAINT FK_host_version_host_name");
      dbAccessor.executeQuery("ALTER TABLE " + CONFIG_GROUP_HOST_MAPPING_TABLE + " DROP CONSTRAINT FK_cghm_hname");
      dbAccessor.executeQuery("ALTER TABLE " + KERBEROS_PRINCIPAL_HOST_TABLE + " DROP CONSTRAINT FK_krb_pr_host_hostname");

      // This FK name is actually different on Derby.
      dbAccessor.executeQuery("ALTER TABLE " + HOST_CONFIG_MAPPING_TABLE + " DROP CONSTRAINT FK_hostconfigmapping_host_name");
    } else {
      dbAccessor.dropConstraint(HOST_COMPONENT_STATE_TABLE, "hostcomponentstate_host_name");
      dbAccessor.dropConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hstcmponentdesiredstatehstname");
      dbAccessor.dropConstraint(HOST_ROLE_COMMAND_TABLE, "FK_host_role_command_host_name");
      dbAccessor.dropConstraint(HOST_STATE_TABLE, "FK_hoststate_host_name");
      dbAccessor.dropConstraint(HOST_VERSION_TABLE, "FK_host_version_host_name");
      dbAccessor.dropConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_cghm_hname");
      dbAccessor.dropConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_hostname");

      dbAccessor.dropConstraint(HOST_CONFIG_MAPPING_TABLE, "FK_hostconfmapping_host_name");
    }

    // In Ambari 2.0.0, there were discrepancies with the FK in the ClusterHostMapping table in the Postgres databases.
    // They were either swapped, or pointing to the wrong table. Ignore failures for both of these.
    try {
      dbAccessor.dropConstraint(CLUSTER_HOST_MAPPING_TABLE, "ClusterHostMapping_host_name", true);
    } catch (Exception e) {
      LOG.warn("Performed best attempt at deleting FK ClusterHostMapping_host_name. " +
          "It is possible it did not exist or the deletion failed. " +  e.getMessage());
    }
    try {
      dbAccessor.dropConstraint(CLUSTER_HOST_MAPPING_TABLE, "ClusterHostMapping_cluster_id", true);
    } catch (Exception e) {
      LOG.warn("Performed best attempt at deleting FK ClusterHostMapping_cluster_id. " +
          "It is possible it did not exist or the deletion failed. " +  e.getMessage());
    }

    // Re-add the FK to the cluster_id; will add the host_id at the end.
    dbAccessor.addFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "FK_clhostmapping_cluster_id",
        "cluster_id", CLUSTERS_TABLE, "cluster_id", false);

    // Drop the PK, and recreate it on the host_id instead
    if (databaseType == Configuration.DatabaseType.DERBY) {
      String constraintName = getDerbyTableConstraintName("p", HOSTS_TABLE);
      if (null != constraintName) {
        dbAccessor.executeQuery("ALTER TABLE " + HOSTS_TABLE + " DROP CONSTRAINT " + constraintName);
      }
    } else {
      dbAccessor.dropConstraint(HOSTS_TABLE, "hosts_pkey");
    }
    dbAccessor.executeQuery("ALTER TABLE " + HOSTS_TABLE + " ADD CONSTRAINT PK_hosts_id PRIMARY KEY (host_id)");
    dbAccessor.executeQuery("ALTER TABLE " + HOSTS_TABLE + " ADD CONSTRAINT UQ_hosts_host_name UNIQUE (host_name)");


    // TODO, for now, these still point to the host_name and will be fixed one table at a time to point to the host id.
    // Re-add the FKs
    dbAccessor.addFKConstraint(HOST_VERSION_TABLE, "FK_host_version_host_name",
        "host_name", HOSTS_TABLE, "host_name", false);
    dbAccessor.addFKConstraint(HOST_ROLE_COMMAND_TABLE, "FK_host_role_command_host_name",
        "host_name", HOSTS_TABLE, "host_name", false);
    dbAccessor.addFKConstraint(HOST_CONFIG_MAPPING_TABLE, "FK_hostconfmapping_host_name",
        "host_name", HOSTS_TABLE, "host_name", false);
    dbAccessor.addFKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_cghm_hname",
        "host_name", HOSTS_TABLE, "host_name", false);
    dbAccessor.addFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_host_name",
        "host_name", HOSTS_TABLE, "host_name", false);


    // Add host_id to the host-related tables, and populate the host_id, one table at a time.
    // TODO, include other tables.
    String[] tablesToAddHostID = new String[] {
        CLUSTER_HOST_MAPPING_TABLE,
        HOST_COMPONENT_STATE_TABLE,
        HOST_COMPONENT_DESIRED_STATE_TABLE,
        HOST_STATE_TABLE
    };
    for (String tableName : tablesToAddHostID) {
      dbAccessor.addColumn(tableName, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));
      dbAccessor.executeQuery("UPDATE " + tableName + " t SET host_id = (SELECT host_id FROM hosts h WHERE h.host_name = t.host_name) WHERE t.host_id IS NULL AND t.host_name IS NOT NULL");

      if (databaseType == Configuration.DatabaseType.DERBY) {
        // This is a workaround for UpgradeTest.java unit test
        dbAccessor.executeQuery("ALTER TABLE " + tableName + " ALTER column " + HOST_ID_COL + " NOT NULL");
      } else {
        dbAccessor.executeQuery("ALTER TABLE " + tableName + " ALTER column " + HOST_ID_COL + " SET NOT NULL");
      }
    }

    // These are the FKs that have already been corrected.
    // TODO, include other tables.
    dbAccessor.addFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "FK_clusterhostmapping_host_id",
        "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_COMPONENT_STATE_TABLE, "FK_hostcomponentstate_host_id",
        "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "FK_hcdesiredstate_host_id",
        "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_STATE_TABLE, "FK_hoststate_host_id",
        "host_id", HOSTS_TABLE, "host_id", false);



    // For any tables where the host_name was part of the PK, need to drop the PK, and recreate it with the host_id
    // TODO, include other tables.
    String[] tablesWithHostNameInPK =  new String[] {
        CLUSTER_HOST_MAPPING_TABLE,
        HOST_COMPONENT_STATE_TABLE,
        HOST_COMPONENT_DESIRED_STATE_TABLE,
        HOST_STATE_TABLE
    };

    if (databaseType == Configuration.DatabaseType.DERBY) {
      for (String tableName : tablesWithHostNameInPK) {
        String constraintName = getDerbyTableConstraintName("p", tableName);
        if (null != constraintName) {
          dbAccessor.executeQuery("ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
        }
      }
    } else {

      dbAccessor.dropConstraint(CLUSTER_HOST_MAPPING_TABLE, "clusterhostmapping_pkey");
      dbAccessor.dropConstraint(HOST_COMPONENT_STATE_TABLE, "hostcomponentstate_pkey");
      dbAccessor.dropConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hostcomponentdesiredstate_pkey");
      dbAccessor.dropConstraint(HOST_STATE_TABLE, "hoststate_pkey");
      // TODO, include other tables.
    }
    dbAccessor.executeQuery("ALTER TABLE " + CLUSTER_HOST_MAPPING_TABLE +
        " ADD CONSTRAINT clusterhostmapping_pkey PRIMARY KEY (cluster_id, host_id)");
    dbAccessor.executeQuery("ALTER TABLE " + HOST_COMPONENT_STATE_TABLE +
        " ADD CONSTRAINT hostcomponentstate_pkey PRIMARY KEY (cluster_id, component_name, host_id, service_name)");
    dbAccessor.executeQuery("ALTER TABLE " + HOST_COMPONENT_DESIRED_STATE_TABLE +
        " ADD CONSTRAINT hostcomponentdesiredstate_pkey PRIMARY KEY (cluster_id, component_name, host_id, service_name)");
    dbAccessor.executeQuery("ALTER TABLE " + HOST_STATE_TABLE +
        " ADD CONSTRAINT hoststate_pkey PRIMARY KEY (host_id)");
    // TODO, include other tables.

    // Finish by deleting the unnecessary host_name columns.
    dbAccessor.dropColumn(CLUSTER_HOST_MAPPING_TABLE, "host_name");
    dbAccessor.dropColumn(HOST_COMPONENT_STATE_TABLE, "host_name");
    dbAccessor.dropColumn(HOST_COMPONENT_DESIRED_STATE_TABLE, "host_name");
    dbAccessor.dropColumn(HOST_STATE_TABLE, "host_name");
    // TODO, include other tables.
    
    // view columns for cluster association
    dbAccessor.addColumn(VIEW_INSTANCE_TABLE, new DBColumnInfo("cluster_handle", String.class, 255, null, true));
    dbAccessor.addColumn(VIEW_PARAMETER_TABLE, new DBColumnInfo("cluster_config", String.class, 255, null, true));
  }

  private void executeWidgetDDLUpdates() throws AmbariException, SQLException {
    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();

    columns.add(new DBColumnInfo("id", Long.class,    null,  null, false));
    columns.add(new DBColumnInfo("user_widget_name", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("user_widget_type", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("metrics", String.class,  32672,   null, true));
    columns.add(new DBColumnInfo("time_created", Long.class,  255,   null, false));
    columns.add(new DBColumnInfo("author", String.class,  255,   null, true));
    columns.add(new DBColumnInfo("description", String.class,  255,   null, true));
    columns.add(new DBColumnInfo("display_name", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("scope", String.class,  255,   null, true));
    columns.add(new DBColumnInfo("widget_values", String.class,  255,   null, true));
    columns.add(new DBColumnInfo("properties", String.class,  255,   null, true));
    columns.add(new DBColumnInfo("cluster_id", Long.class,  255,   null, false));
    dbAccessor.createTable(USER_WIDGET_TABLE, columns, "id");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("id", Long.class,    null,  null, false));
    columns.add(new DBColumnInfo("layout_name", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("section_name", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class,  255,   null, false));
    columns.add(new DBColumnInfo("scope", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("user_name", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("display_name", String.class,  255,   null, false));

    dbAccessor.createTable(WIDGET_LAYOUT_TABLE, columns, "id");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("widget_layout_id", Long.class,    null,  null, false));
    columns.add(new DBColumnInfo("user_widget_id", Long.class,    null,  null, false));
    columns.add(new DBColumnInfo("widget_order", Integer.class,    null,  null, false));
    dbAccessor.createTable(WIDGET_LAYOUT_USER_WIDGET_TABLE, columns, "widget_layout_id", "user_widget_id");
    dbAccessor.addFKConstraint(WIDGET_LAYOUT_USER_WIDGET_TABLE, "FK_widget_layout_id", "widget_layout_id", "widget_layout", "id", true, false);
    dbAccessor.addFKConstraint(WIDGET_LAYOUT_USER_WIDGET_TABLE, "FK_user_widget_id", "user_widget_id", "user_widget", "id", true, false);
  }

  /**
   * Adds the stack table and constraints.
   */
  private void executeStackDDLUpdates() throws AmbariException, SQLException {
    // alert_definition
    ArrayList<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("stack_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("stack_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("stack_version", String.class, 255, null,
        false));

    dbAccessor.createTable(STACK_TABLE_DEFINITION, columns, "stack_id");

    dbAccessor.executeQuery("ALTER TABLE " + STACK_TABLE_DEFINITION
        + " ADD CONSTRAINT unq_stack UNIQUE (stack_name,stack_version)", false);

    dbAccessor.executeQuery(
        "INSERT INTO ambari_sequences(sequence_name, sequence_value) VALUES('stack_id_seq', 0)",
        false);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * Populate the id of the hosts table with an auto-increment int.
   * @param resultSet Rows from the hosts table, sorted first by host_id
   * @return Returns an integer with the id for the next host record to be inserted.
   * @throws SQLException
   */
  @Transactional
  private Long populateHostsId(ResultSet resultSet) throws SQLException {
    Long hostId = 0L;
    if (resultSet != null) {
      try {
        while (resultSet.next()) {
          hostId++;
          final String hostName = resultSet.getString(1);
          Long currHostID = resultSet.getLong(2); // in case of a retry, may not be null

          if (currHostID == null && StringUtils.isNotBlank(hostName)) {
            dbAccessor.executeQuery("UPDATE " + HOSTS_TABLE + " SET host_id = " + hostId +
                " WHERE host_name = '" + hostName + "'");
          }
        }
      } catch (Exception e) {
        LOG.error("Unable to populate the id of the hosts. " + e.getMessage());
      }
    }
    return hostId;
  }

  /**
   * Get the constraint name created by Derby if one was not specified for the table.
   * @param type Constraint-type, either, "p" (Primary), "c" (Check), "f" (Foreign), "u" (Unique)
   * @param tableName Table Name
   * @return Return the constraint name, or null if not found.
   * @throws SQLException
   */
  private String getDerbyTableConstraintName(String type, String tableName) throws SQLException {
    ResultSet resultSet = null;
    boolean found = false;
    String constraint = null;

    try {
      resultSet = dbAccessor.executeSelect("SELECT c.constraintname, c.type, t.tablename FROM sys.sysconstraints c, sys.systables t WHERE c.tableid = t.tableid");
      while(resultSet.next()) {
        constraint = resultSet.getString(1);
        String recordType = resultSet.getString(2);
        String recordTableName = resultSet.getString(3);

        if (recordType.equalsIgnoreCase(type) && recordTableName.equalsIgnoreCase(tableName)) {
          found = true;
          break;
        }
      }
    } finally {
      if (resultSet != null) {
        resultSet.close();
      }
    }
    return found ? constraint : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
  }
}
