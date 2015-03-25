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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Upgrade catalog for version 2.1.0.
 */
public class UpgradeCatalog210 extends AbstractUpgradeCatalog {

  @Inject
  HostDAO hostDAO;

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
  }

  /**
   * Execute all of the hosts DDL updates.
   *
   * @throws org.apache.ambari.server.AmbariException
   * @throws java.sql.SQLException
   */
  private void executeHostsDDLUpdates() throws AmbariException, SQLException {
    Configuration.DatabaseType databaseType = configuration.getDatabaseType();

    dbAccessor.addColumn(HOSTS_TABLE, new DBColumnInfo("id", Long.class, null, null, true));

    Long hostId = 0L;
    ResultSet resultSet = null;
    try {
      resultSet = dbAccessor.executeSelect("SELECT host_name FROM hosts");
      hostId = populateHostsId(resultSet);
    } finally {
      if (resultSet != null) {
        resultSet.close();
      }
    }

    // Insert host id number into ambari_sequences
    dbAccessor.executeQuery("INSERT INTO ambari_sequences (sequence_name, sequence_value) VALUES ('host_id_seq', " + hostId + ")");
    //dbAccessor.insertRow("ambari_sequences", new String[]{"sequence_name", "sequence_value"}, new String[]{"host_id_seq", hostId.toString()}, false);

    // Make the hosts id non-null after all the values are populated
    if (databaseType == Configuration.DatabaseType.DERBY) {
      // This is a workaround for UpgradeTest.java unit test
      dbAccessor.executeQuery("ALTER TABLE hosts ALTER column id NOT NULL");
    } else {
      dbAccessor.alterColumn("hosts", new DBColumnInfo("id", Long.class, null, null, false));
      //dbAccessor.executeQuery("ALTER TABLE hosts ALTER column id SET NOT NULL");
    }


    // Drop the 8 FK constraints in the host-related tables. They will be recreated later after the PK is changed.
    // The only host-related table not being included is alert_history.
    if (databaseType == Configuration.DatabaseType.DERBY) {
      dbAccessor.executeQuery("ALTER TABLE hostcomponentdesiredstate DROP CONSTRAINT hstcmponentdesiredstatehstname");
      dbAccessor.executeQuery("ALTER TABLE hostcomponentstate DROP CONSTRAINT hostcomponentstate_host_name");
      dbAccessor.executeQuery("ALTER TABLE hoststate DROP CONSTRAINT FK_hoststate_host_name");
      dbAccessor.executeQuery("ALTER TABLE host_version DROP CONSTRAINT FK_host_version_host_name");
      dbAccessor.executeQuery("ALTER TABLE host_role_command DROP CONSTRAINT FK_host_role_command_host_name");
      // This FK name is actually different on Derby.
      dbAccessor.executeQuery("ALTER TABLE hostconfigmapping DROP CONSTRAINT FK_hostconfigmapping_host_name");
      dbAccessor.executeQuery("ALTER TABLE configgrouphostmapping DROP CONSTRAINT FK_cghm_hname");
      dbAccessor.executeQuery("ALTER TABLE kerberos_principal_host DROP CONSTRAINT FK_krb_pr_host_hostname");
    } else {
      dbAccessor.dropConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hstcmponentdesiredstatehstname");
      dbAccessor.dropConstraint(HOST_COMPONENT_STATE_TABLE, "hostcomponentstate_host_name");
      dbAccessor.dropConstraint(HOST_STATE_TABLE, "FK_hoststate_host_name");
      dbAccessor.dropConstraint(HOST_VERSION_TABLE, "FK_host_version_host_name");
      dbAccessor.dropConstraint(HOST_ROLE_COMMAND_TABLE, "FK_host_role_command_host_name");
      dbAccessor.dropConstraint(HOST_CONFIG_MAPPING_TABLE, "FK_hostconfmapping_host_name");
      dbAccessor.dropConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_cghm_hname");
      dbAccessor.dropConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_hostname");
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

    // Readd the FK to the cluster_id; will add the host_id at the end.
    dbAccessor.addFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "FK_clusterhostmapping_cluster_id",
        "cluster_id", CLUSTERS_TABLE, "cluster_id", false);

    // Drop the PK, and recreate it on the id instead
    if (databaseType == Configuration.DatabaseType.DERBY) {
      String constraintName = getDerbyTableConstraintName("p", HOSTS_TABLE);
      if (null != constraintName) {
        dbAccessor.executeQuery("ALTER TABLE hosts DROP CONSTRAINT " + constraintName);
      }
    } else {
      dbAccessor.dropConstraint(HOSTS_TABLE, "hosts_pkey");
    }
    dbAccessor.executeQuery("ALTER TABLE hosts ADD CONSTRAINT PK_hosts_id PRIMARY KEY (id)");

    dbAccessor.executeQuery("ALTER TABLE hosts ADD CONSTRAINT UQ_hosts_host_name UNIQUE (host_name)");

    // TODO, for now, these still point to the host_name and will be fixed one table at a time to point to the host id.
    // Re-add the FKs
    dbAccessor.addFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hstcmponentdesiredstatehstname",
        "host_name", HOSTS_TABLE, "host_name", false);
    dbAccessor.addFKConstraint(HOST_COMPONENT_STATE_TABLE, "hostcomponentstate_host_name",
        "host_name", HOSTS_TABLE, "host_name", false);
    dbAccessor.addFKConstraint(HOST_STATE_TABLE, "FK_hoststate_host_name",
        "host_name", HOSTS_TABLE, "host_name", false);
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
    dbAccessor.addColumn(CLUSTER_HOST_MAPPING_TABLE, new DBColumnInfo("host_id", Long.class, null, null, true));
    dbAccessor.executeQuery("UPDATE clusterhostmapping chm SET host_id = (SELECT id FROM hosts h WHERE h.host_name = chm.host_name) WHERE chm.host_id IS NULL AND chm.host_name IS NOT NULL");

    if (databaseType == Configuration.DatabaseType.DERBY) {
      // This is a workaround for UpgradeTest.java unit test
      dbAccessor.executeQuery("ALTER TABLE clusterhostmapping ALTER column host_id NOT NULL");
    } else {
      dbAccessor.executeQuery("ALTER TABLE clusterhostmapping ALTER column host_id SET NOT NULL");
    }

    // These are the FKs that have already been corrected.
    dbAccessor.addFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "FK_clusterhostmapping_host_id",
        "host_id", HOSTS_TABLE, "id", false);

    dbAccessor.dropColumn(CLUSTER_HOST_MAPPING_TABLE, "host_name");
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * Populate the id of the hosts table with an auto-increment int.
   * @param resultSet Rows from the hosts table
   * @return Returns an integer with the id for the next host record to be inserted.
   * @throws SQLException
   */
  @Transactional
  private Long populateHostsId(ResultSet resultSet) throws SQLException {
    Long hostId = 0L;
    if (resultSet != null) {
      try {
        while (resultSet.next()) {
          final String hostName = resultSet.getString(1);
          HostEntity host = hostDAO.findByName(hostName);
          host.setId(++hostId);
          hostDAO.merge(host);
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
  }
}
