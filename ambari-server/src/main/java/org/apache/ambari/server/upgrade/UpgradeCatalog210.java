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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;


/**
 * Upgrade catalog for version 2.1.0.
 */
public class UpgradeCatalog210 extends AbstractUpgradeCatalog {
  private static final String CLUSTERS_TABLE = "clusters";
  private static final String CLUSTER_HOST_MAPPING_TABLE = "ClusterHostMapping";
  private static final String HOSTS_TABLE = "hosts";
  private static final String HOST_COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
  private static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  private static final String HOST_STATE_TABLE = "hoststate";
  private static final String HOST_VERSION_TABLE = "host_version";
  private static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  private static final String HOST_CONFIG_MAPPING_TABLE = "hostconfigmapping";
  private static final String CONFIG_GROUP_HOST_MAPPING_TABLE = "configgrouphostmapping";
  private static final String CONFIG_GROUP_TABLE = "configgroup";
  private static final String KERBEROS_PRINCIPAL_HOST_TABLE = "kerberos_principal_host";
  private static final String KERBEROS_PRINCIPAL_TABLE = "kerberos_principal";
  private static final String REQUEST_OPERATION_LEVEL_TABLE = "requestoperationlevel";
  private static final String SERVICE_COMPONENT_DESIRED_STATE_TABLE = "servicecomponentdesiredstate";
  private static final String SERVICE_CONFIG_TABLE = "serviceconfig";
  private static final String SERVICE_CONFIG_HOSTS_TABLE = "serviceconfighosts";
  private static final String WIDGET_TABLE = "widget";
  private static final String WIDGET_LAYOUT_TABLE = "widget_layout";
  private static final String WIDGET_LAYOUT_USER_WIDGET_TABLE = "widget_layout_user_widget";
  private static final String VIEW_TABLE = "viewmain";
  private static final String VIEW_INSTANCE_TABLE = "viewinstance";
  private static final String VIEW_PARAMETER_TABLE = "viewparameter";
  private static final String STACK_TABLE = "stack";
  private static final String REPO_VERSION_TABLE = "repo_version";
  private static final String ALERT_HISTORY_TABLE = "alert_history";
  private static final String HOST_ID_COL = "host_id";
  private static final String HOST_NAME_COL = "host_name";
  private static final String PUBLIC_HOST_NAME_COL = "public_host_name";
  private static final String TOPOLOGY_REQUEST_TABLE = "topology_request";
  private static final String TOPOLOGY_HOST_GROUP_TABLE = "topology_hostgroup";
  private static final String TOPOLOGY_HOST_INFO_TABLE = "topology_host_info";
  private static final String TOPOLOGY_LOGICAL_REQUEST_TABLE = "topology_logical_request";
  private static final String TOPOLOGY_HOST_REQUEST_TABLE = "topology_host_request";
  private static final String TOPOLOGY_HOST_TASK_TABLE = "topology_host_task";
  private static final String TOPOLOGY_LOGICAL_TASK_TABLE = "topology_logical_task";
  private static final String HDFS_SITE_CONFIG = "hdfs-site";

  // constants for stack table changes
  private static final String STACK_ID_COLUMN_NAME = "stack_id";
  private static final String DESIRED_STACK_ID_COLUMN_NAME = "desired_stack_id";
  private static final String CURRENT_STACK_ID_COLUMN_NAME = "current_stack_id";
  private static final String DESIRED_STACK_VERSION_COLUMN_NAME = "desired_stack_version";
  private static final String CURRENT_STACK_VERSION_COLUMN_NAME = "current_stack_version";
  private static final DBColumnInfo DESIRED_STACK_ID_COLUMN = new DBColumnInfo(DESIRED_STACK_ID_COLUMN_NAME, Long.class, null, null, true);
  private static final DBColumnInfo CURRENT_STACK_ID_COLUMN = new DBColumnInfo(CURRENT_STACK_ID_COLUMN_NAME, Long.class, null, null, true);
  private static final DBColumnInfo STACK_ID_COLUMN = new DBColumnInfo(STACK_ID_COLUMN_NAME, Long.class, null, null, true);

  @Inject
  DaoUtils daoUtils;

  @Inject
  private OsFamily osFamily;

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

    daoUtils = injector.getInstance(DaoUtils.class);
    osFamily = injector.getInstance(OsFamily.class);
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    executeAlertDDLUpdates();
    executeHostsDDLUpdates();
    executeWidgetDDLUpdates();
    executeStackDDLUpdates();
    executeTopologyDDLUpdates();
    executeViewDDLUpdates();
  }

  private void executeTopologyDDLUpdates() throws AmbariException, SQLException {
    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();

    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("action", String.class, 255, null, false));
    columns.add(new DBColumnInfo("cluster_name", String.class, 100, null, false));
    columns.add(new DBColumnInfo("bp_name", String.class, 100, null, false));
    columns.add(new DBColumnInfo("cluster_properties", char[].class, null, null, true));
    columns.add(new DBColumnInfo("cluster_attributes", char[].class, null, null, true));
    columns.add(new DBColumnInfo("description", String.class, 1024, null, true));

    dbAccessor.createTable(TOPOLOGY_REQUEST_TABLE, columns, "id");

    columns.clear();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("group_properties", char[].class, null, null, true));
    columns.add(new DBColumnInfo("group_attributes", char[].class, null, null, true));
    columns.add(new DBColumnInfo("request_id", Long.class, null, null, false));

    dbAccessor.createTable(TOPOLOGY_HOST_GROUP_TABLE, columns, "id");
    dbAccessor.addFKConstraint(TOPOLOGY_HOST_GROUP_TABLE, "FK_hostgroup_req_id", "request_id", TOPOLOGY_REQUEST_TABLE, "id", false, false);

    columns.clear();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("group_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fqdn", String.class, 255, null, true));
    columns.add(new DBColumnInfo("host_count", Integer.class, null, null, true));
    columns.add(new DBColumnInfo("predicate", String.class, 2048, null, true));

    dbAccessor.createTable(TOPOLOGY_HOST_INFO_TABLE, columns, "id");
    dbAccessor.addFKConstraint(TOPOLOGY_HOST_INFO_TABLE, "FK_hostinfo_group_id", "group_id", TOPOLOGY_HOST_GROUP_TABLE, "id", false, false);

    columns.clear();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("request_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("description", String.class, 1024, null, true));

    dbAccessor.createTable(TOPOLOGY_LOGICAL_REQUEST_TABLE, columns, "id");
    dbAccessor.addFKConstraint(TOPOLOGY_LOGICAL_REQUEST_TABLE, "FK_logicalreq_req_id", "request_id", TOPOLOGY_REQUEST_TABLE, "id", false, false);

    columns.clear();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("logical_request_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("group_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("stage_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("host_name", String.class, 255, null, true));

    dbAccessor.createTable(TOPOLOGY_HOST_REQUEST_TABLE, columns, "id");
    dbAccessor.addFKConstraint(TOPOLOGY_HOST_REQUEST_TABLE, "FK_hostreq_logicalreq_id", "logical_request_id", TOPOLOGY_LOGICAL_REQUEST_TABLE, "id", false, false);
    dbAccessor.addFKConstraint(TOPOLOGY_HOST_REQUEST_TABLE, "FK_hostreq_group_id", "group_id", TOPOLOGY_HOST_GROUP_TABLE, "id", false, false);

    columns.clear();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("host_request_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("type", String.class, 255, null, false));
    dbAccessor.createTable(TOPOLOGY_HOST_TASK_TABLE, columns, "id");
    dbAccessor.addFKConstraint(TOPOLOGY_HOST_TASK_TABLE, "FK_hosttask_req_id", "host_request_id", TOPOLOGY_HOST_REQUEST_TABLE, "id", false, false);

    columns.clear();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("host_task_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("physical_task_id", Long.class, null, null, true));
    columns.add(new DBColumnInfo("component", String.class, 255, null, false));
    dbAccessor.createTable(TOPOLOGY_LOGICAL_TASK_TABLE, columns, "id");
    dbAccessor.addFKConstraint(TOPOLOGY_LOGICAL_TASK_TABLE, "FK_ltask_hosttask_id", "host_task_id", TOPOLOGY_HOST_TASK_TABLE, "id", false, false);
    dbAccessor.addFKConstraint(TOPOLOGY_LOGICAL_TASK_TABLE, "FK_ltask_hrc_id", "physical_task_id", "host_role_command", "task_id", false, false);

    // Sequence updates
    addSequences(Arrays.asList(
      "topology_host_info_id_seq",
      "topology_host_request_id_seq",
      "topology_host_task_id_seq",
      "topology_logical_request_id_seq",
      "topology_logical_task_id_seq",
      "topology_request_id_seq",
      "topology_host_group_id_seq"
    ), 0L, false);
  }


  private void executeAlertDDLUpdates() throws AmbariException, SQLException {
    //Fix latest_text column type to match for all DBMS
    Configuration.DatabaseType databaseType = configuration.getDatabaseType();

    // MySQL columns are already TEXT, but we need to be sure in that, since LONGTEXT will really slowdown database when querying the alerts too often
    if (Configuration.DatabaseType.MYSQL == databaseType) {
      dbAccessor.alterColumn("alert_current", new DBColumnInfo("latest_text", new FieldTypeDefinition("TEXT"), null));
      dbAccessor.alterColumn("alert_history", new DBColumnInfo("alert_text", new FieldTypeDefinition("TEXT"), null));
    } else {
      dbAccessor.changeColumnType("alert_current", "latest_text", String.class, char[].class);
      dbAccessor.changeColumnType("alert_history", "alert_text", String.class, char[].class);
    }

  }

  private void executeViewDDLUpdates() throws AmbariException, SQLException {
    // cluster association
    dbAccessor.addColumn(VIEW_INSTANCE_TABLE, new DBColumnInfo("cluster_handle", String.class, 255, null, true));
    // determine whether to alter the names of the dynamic entities / attributes to
    // avoid db reserved word conflicts.  should be false for existing instances
    // for backward compatibility.
    dbAccessor.addColumn(VIEW_INSTANCE_TABLE, new DBColumnInfo("alter_names", Integer.class, 0, 0, false));
    // cluster configuration
    dbAccessor.addColumn(VIEW_PARAMETER_TABLE, new DBColumnInfo("cluster_config", String.class, 255, null, true));
    // view build number
    dbAccessor.addColumn(VIEW_TABLE, new DBColumnInfo("build", String.class, 128, null, true));
  }

  /**
   * Execute all of the hosts DDL updates.
   *
   * @throws org.apache.ambari.server.AmbariException
   * @throws java.sql.SQLException
   */
  private void executeHostsDDLUpdates() throws AmbariException, SQLException {
    Configuration.DatabaseType databaseType = configuration.getDatabaseType();

    String randomHostName = null;
    if (dbAccessor.tableHasData(HOST_ROLE_COMMAND_TABLE)) {
      randomHostName = getRandomHostName();
      if (StringUtils.isBlank(randomHostName)) {
        throw new AmbariException("UpgradeCatalog210 could not retrieve a random host_name from the hosts table while running executeHostsDDLUpdates.");
      }
    }

    dbAccessor.addColumn(HOSTS_TABLE, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));

    // Sequence value for the hosts table primary key. First record will be 1, so ambari_sequence value must be 0.
    Long hostId = 0L;
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        rs = statement.executeQuery("SELECT host_name, host_id FROM hosts ORDER BY host_id ASC, host_name ASC");
        if (rs != null) {
          hostId = populateHostsId(rs);
        }
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (statement != null) {
        statement.close();
      }
    }
    // Insert host id number into ambari_sequences
    addSequence("host_id_seq", hostId, false);

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
      dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, "hostcomponentstate_host_name");
      dbAccessor.dropFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hstcmponentdesiredstatehstname");
      dbAccessor.dropFKConstraint(HOST_ROLE_COMMAND_TABLE, "FK_host_role_command_host_name");
      dbAccessor.dropFKConstraint(HOST_STATE_TABLE, "FK_hoststate_host_name");
      dbAccessor.dropFKConstraint(HOST_VERSION_TABLE, "FK_host_version_host_name");
      dbAccessor.dropFKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_cghm_hname");
      // FK_krb_pr_host_hostname used to have a CASCADE DELETE, which is not needed.
      dbAccessor.dropFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_hostname");
      // FK_krb_pr_host_principalname used to have a CASCADE DELETE, which is not needed, so it will be recreated without it.
      dbAccessor.dropFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_principalname");

      // This FK name is actually different on Derby.
      dbAccessor.dropFKConstraint(HOST_CONFIG_MAPPING_TABLE, "FK_hostconfigmapping_host_name");
    } else {
      dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, "hostcomponentstate_host_name");
      dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, "fk_hostcomponentstate_host_name");

      dbAccessor.dropFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hstcmponentdesiredstatehstname");
      dbAccessor.dropFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "fk_hostcomponentdesiredstate_host_name");

      dbAccessor.dropFKConstraint(HOST_ROLE_COMMAND_TABLE, "FK_host_role_command_host_name");
      dbAccessor.dropFKConstraint(HOST_STATE_TABLE, "FK_hoststate_host_name");
      dbAccessor.dropFKConstraint(HOST_VERSION_TABLE, "FK_host_version_host_name");

      dbAccessor.dropFKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_cghm_hname");
      dbAccessor.dropFKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "fk_configgrouphostmapping_host_name");

      // FK_krb_pr_host_hostname used to have a CASCADE DELETE, which is not needed.
      dbAccessor.dropFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_hostname");
      dbAccessor.dropFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "fk_kerberos_principal_host_host_name");

      // FK_krb_pr_host_principalname used to have a CASCADE DELETE, which is not needed, so it will be recreated without it.
      dbAccessor.dropFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_principalname");

      dbAccessor.dropFKConstraint(HOST_CONFIG_MAPPING_TABLE, "FK_hostconfmapping_host_name");
    }

    // In Ambari 2.0.0, there were discrepancies with the FK in the ClusterHostMapping table in the Postgres databases.
    // They were either swapped, or pointing to the wrong table. Ignore failures for both of these.
    try {
      dbAccessor.dropFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "ClusterHostMapping_host_name", true);
      dbAccessor.dropFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "fk_clusterhostmapping_host_name", true);
    } catch (Exception e) {
      LOG.warn("Performed best attempt at deleting FK ClusterHostMapping_host_name. " +
          "It is possible it did not exist or the deletion failed. " +  e.getMessage());
    }
    try {
      dbAccessor.dropFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "ClusterHostMapping_cluster_id", true);
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
        // Derby doesn't support CASCADE DELETE.
        dbAccessor.executeQuery("ALTER TABLE " + HOSTS_TABLE + " DROP CONSTRAINT " + constraintName);
      }
    } else {
      dbAccessor.dropPKConstraint(HOSTS_TABLE, "hosts_pkey", "host_name", true);
    }

    dbAccessor.addPKConstraint(HOSTS_TABLE, "PK_hosts_id", "host_id");
    dbAccessor.addUniqueConstraint(HOSTS_TABLE, "UQ_hosts_host_name", "host_name");


    // Add host_id to the host-related tables, and populate the host_id, one table at a time.
    String[] tablesToAddHostID = new String[] {
        CONFIG_GROUP_HOST_MAPPING_TABLE,
        CLUSTER_HOST_MAPPING_TABLE,
        HOST_CONFIG_MAPPING_TABLE,
        HOST_COMPONENT_STATE_TABLE,
        HOST_COMPONENT_DESIRED_STATE_TABLE,
        HOST_ROLE_COMMAND_TABLE,
        HOST_STATE_TABLE,
        HOST_VERSION_TABLE,
        KERBEROS_PRINCIPAL_HOST_TABLE,
        REQUEST_OPERATION_LEVEL_TABLE,
        SERVICE_CONFIG_HOSTS_TABLE
    };

    for (String tableName : tablesToAddHostID) {
      dbAccessor.addColumn(tableName, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));

      // The column name is different for one table
      String hostNameColumnName = tableName.equals(SERVICE_CONFIG_HOSTS_TABLE) ? "hostname" : "host_name";

      if (dbAccessor.tableHasData(tableName) && dbAccessor.tableHasColumn(tableName, hostNameColumnName)) {
        dbAccessor.executeQuery("UPDATE " + tableName + " t SET host_id = (SELECT host_id FROM hosts h WHERE h.host_name = t." + hostNameColumnName + ") WHERE t.host_id IS NULL AND t." + hostNameColumnName + " IS NOT NULL");

        // For legacy reasons, the hostrolecommand table will contain "none" for some records where the host_name was not important.
        // These records were populated during Finalize in Rolling Upgrade, so they must be updated to use a valid host_name.
        if (tableName.equals(HOST_ROLE_COMMAND_TABLE) && StringUtils.isNotBlank(randomHostName)) {
          dbAccessor.executeQuery("UPDATE " + tableName + " t SET host_id = (SELECT host_id FROM hosts h WHERE h.host_name = '" + randomHostName + "') WHERE t.host_id IS NULL AND t.host_name = 'none'");
        }
      }

      // The one exception for setting NOT NULL is the requestoperationlevel table
      if (!tableName.equals(REQUEST_OPERATION_LEVEL_TABLE)) {
        dbAccessor.setColumnNullable(tableName, HOST_ID_COL, false);
      }
    }


    // For any tables where the host_name was part of the PK, need to drop the PK, and recreate it with the host_id
    String[] tablesWithHostNameInPK =  new String[] {
        CONFIG_GROUP_HOST_MAPPING_TABLE,
        CLUSTER_HOST_MAPPING_TABLE,
        HOST_CONFIG_MAPPING_TABLE,
        HOST_COMPONENT_STATE_TABLE,
        HOST_COMPONENT_DESIRED_STATE_TABLE,
        HOST_STATE_TABLE,
        KERBEROS_PRINCIPAL_HOST_TABLE,
        SERVICE_CONFIG_HOSTS_TABLE
    };

    // We can't drop PK, if a one of PK columns is a part of foreign key. We should drop FK and re-create him after dropping PK
    dbAccessor.dropFKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_cghm_cgid");
    dbAccessor.dropFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "FK_clhostmapping_cluster_id");

    dbAccessor.dropFKConstraint(HOST_CONFIG_MAPPING_TABLE, "FK_hostconfmapping_cluster_id");
    dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, "hstcomponentstatecomponentname");
    dbAccessor.dropFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hstcmpnntdesiredstatecmpnntnme");
    dbAccessor.dropFKConstraint(SERVICE_CONFIG_HOSTS_TABLE, "FK_scvhosts_scv");

    //These FK's hasn't been deleted previously due to MySQL case sensitivity
    if (databaseType == Configuration.DatabaseType.MYSQL) {
      dbAccessor.dropFKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_configgrouphostmapping_config_group_id");
      dbAccessor.dropFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "FK_ClusterHostMapping_cluster_id");
      dbAccessor.dropFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_kerberos_principal_host_principal_name");
      dbAccessor.dropFKConstraint(SERVICE_CONFIG_HOSTS_TABLE, "FK_serviceconfighosts_service_config_id");
    }

    if (databaseType == Configuration.DatabaseType.DERBY) {
      for (String tableName : tablesWithHostNameInPK) {
        String constraintName = getDerbyTableConstraintName("p", tableName);
        if (null != constraintName) {
          dbAccessor.executeQuery("ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
        }
      }
    } else {
      // drop constrain only if existed constraint contains required column
      dbAccessor.dropPKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "configgrouphostmapping_pkey", HOST_NAME_COL, true);
      dbAccessor.dropPKConstraint(CLUSTER_HOST_MAPPING_TABLE, "clusterhostmapping_pkey",HOST_NAME_COL, true);
      dbAccessor.dropPKConstraint(HOST_CONFIG_MAPPING_TABLE, "hostconfigmapping_pkey", HOST_NAME_COL, true);
      dbAccessor.dropPKConstraint(HOST_COMPONENT_STATE_TABLE, "hostcomponentstate_pkey", HOST_NAME_COL, true);
      dbAccessor.dropPKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hostcomponentdesiredstate_pkey", HOST_NAME_COL, true);
      dbAccessor.dropPKConstraint(HOST_STATE_TABLE, "hoststate_pkey", HOST_NAME_COL, true);
      dbAccessor.dropPKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "kerberos_principal_host_pkey", HOST_NAME_COL, true);
      dbAccessor.dropPKConstraint(SERVICE_CONFIG_HOSTS_TABLE, "serviceconfighosts_pkey", "hostname", true);
    }

    // Finish by deleting the unnecessary host_name columns.
    dbAccessor.dropColumn(CONFIG_GROUP_HOST_MAPPING_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(CLUSTER_HOST_MAPPING_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(HOST_CONFIG_MAPPING_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(HOST_COMPONENT_STATE_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(HOST_COMPONENT_DESIRED_STATE_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(HOST_ROLE_COMMAND_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(HOST_STATE_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(HOST_VERSION_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(KERBEROS_PRINCIPAL_HOST_TABLE, HOST_NAME_COL);
    dbAccessor.dropColumn(REQUEST_OPERATION_LEVEL_TABLE, HOST_NAME_COL);

    // Notice that the column name doesn't have an underscore here.
    dbAccessor.dropColumn(SERVICE_CONFIG_HOSTS_TABLE, "hostname");

    dbAccessor.addPKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "configgrouphostmapping_pkey", "config_group_id", "host_id");
    dbAccessor.addPKConstraint(CLUSTER_HOST_MAPPING_TABLE, "clusterhostmapping_pkey", "cluster_id", "host_id");
    dbAccessor.addPKConstraint(HOST_CONFIG_MAPPING_TABLE, "hostconfigmapping_pkey", "create_timestamp", "host_id", "cluster_id", "type_name");
    dbAccessor.addPKConstraint(HOST_COMPONENT_STATE_TABLE, "hostcomponentstate_pkey", "cluster_id", "component_name", "host_id", "service_name");
    dbAccessor.addPKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hostcomponentdesiredstate_pkey", "cluster_id", "component_name", "host_id", "service_name");
    dbAccessor.addPKConstraint(HOST_STATE_TABLE, "hoststate_pkey", "host_id");
    dbAccessor.addPKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "kerberos_principal_host_pkey", "principal_name", "host_id");
    dbAccessor.addPKConstraint(SERVICE_CONFIG_HOSTS_TABLE, "serviceconfighosts_pkey", "service_config_id", "host_id");

    // re-create FK constraints
    dbAccessor.addFKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_cghm_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "FK_clusterhostmapping_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_CONFIG_MAPPING_TABLE, "FK_hostconfmapping_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_COMPONENT_STATE_TABLE, "FK_hostcomponentstate_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "FK_hcdesiredstate_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_ROLE_COMMAND_TABLE, "FK_host_role_command_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_STATE_TABLE, "FK_hoststate_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(HOST_VERSION_TABLE, "FK_host_version_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_principalname", "principal_name", KERBEROS_PRINCIPAL_TABLE, "principal_name", false);
    dbAccessor.addFKConstraint(SERVICE_CONFIG_HOSTS_TABLE, "FK_scvhosts_host_id", "host_id", HOSTS_TABLE, "host_id", false);
    dbAccessor.addFKConstraint(CONFIG_GROUP_HOST_MAPPING_TABLE, "FK_cghm_cgid", "config_group_id", CONFIG_GROUP_TABLE, "group_id", false);
    dbAccessor.addFKConstraint(CLUSTER_HOST_MAPPING_TABLE, "FK_clhostmapping_cluster_id", "cluster_id", CLUSTERS_TABLE, "cluster_id", false);
    dbAccessor.addFKConstraint(HOST_CONFIG_MAPPING_TABLE, "FK_hostconfmapping_cluster_id", "cluster_id", CLUSTERS_TABLE, "cluster_id", false);
    dbAccessor.addFKConstraint(HOST_COMPONENT_STATE_TABLE, "hstcomponentstatecomponentname",
                                  new String[]{"component_name", "cluster_id", "service_name"}, SERVICE_COMPONENT_DESIRED_STATE_TABLE,
                                  new String[]{"component_name", "cluster_id", "service_name"}, false);
    dbAccessor.addFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, "hstcmpnntdesiredstatecmpnntnme",
                                  new String[]{"component_name", "cluster_id", "service_name"}, SERVICE_COMPONENT_DESIRED_STATE_TABLE,
                                  new String[]{"component_name", "cluster_id", "service_name"}, false);
    dbAccessor.addFKConstraint(SERVICE_CONFIG_HOSTS_TABLE, "FK_scvhosts_scv", "service_config_id", SERVICE_CONFIG_TABLE, "service_config_id", false);

    // Update host names to be case insensitive
    String UPDATE_TEMPLATE = "UPDATE {0} SET {1} = lower({1})";
    // First remove duplicate hosts
    removeDuplicateHosts();
    // Lowercase host name in hosts
    String updateHostName = MessageFormat.format(UPDATE_TEMPLATE, HOSTS_TABLE, HOST_NAME_COL);
    dbAccessor.executeQuery(updateHostName);
    // Lowercase public host name in hosts
    String updatePublicHostName = MessageFormat.format(UPDATE_TEMPLATE, HOSTS_TABLE, PUBLIC_HOST_NAME_COL);
    dbAccessor.executeQuery(updatePublicHostName);
    // Lowercase host name in alert_history
    String updateAlertHostName = MessageFormat.format(UPDATE_TEMPLATE, ALERT_HISTORY_TABLE, HOST_NAME_COL);
    dbAccessor.executeQuery(updateAlertHostName);
  }

  private void executeWidgetDDLUpdates() throws AmbariException, SQLException {
    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();

    columns.add(new DBColumnInfo("id", Long.class,    null,  null, false));
    columns.add(new DBColumnInfo("widget_name", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("widget_type", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("metrics", char[].class, null, null, true));
    columns.add(new DBColumnInfo("time_created", Long.class,  null,   null, false));
    columns.add(new DBColumnInfo("author", String.class, 255, null, true));
    columns.add(new DBColumnInfo("description", String.class, 255, null, true));
    columns.add(new DBColumnInfo("default_section_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("scope", String.class,  255,   null, true));
    columns.add(new DBColumnInfo("widget_values", char[].class, null, null, true));
    columns.add(new DBColumnInfo("properties", char[].class, null, null, true));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    dbAccessor.createTable(WIDGET_TABLE, columns, "id");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("layout_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("section_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("scope", String.class, 255, null, false));
    columns.add(new DBColumnInfo("user_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("display_name", String.class, 255, null, true));

    dbAccessor.createTable(WIDGET_LAYOUT_TABLE, columns, "id");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("widget_layout_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("widget_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("widget_order", Short.class, null, null, true));
    dbAccessor.createTable(WIDGET_LAYOUT_USER_WIDGET_TABLE, columns, "widget_layout_id", "widget_id");
    dbAccessor.addFKConstraint(WIDGET_LAYOUT_USER_WIDGET_TABLE, "FK_widget_layout_id", "widget_layout_id", "widget_layout", "id", false, false);
    dbAccessor.addFKConstraint(WIDGET_LAYOUT_USER_WIDGET_TABLE, "FK_widget_id", "widget_id", "widget", "id", false, false);

    //Alter users to store active widget layouts
    dbAccessor.addColumn("users", new DBColumnInfo("active_widget_layouts", String.class, 1024, null, true));

    // Sequence updates
      addSequences(Arrays.asList("widget_id_seq", "widget_layout_id_seq"), 0L, false);
  }

  /**
   * Adds the stack table, FKs, and constraints.
   */
  private void executeStackDDLUpdates() throws AmbariException, SQLException {
    // stack table creation
    ArrayList<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("stack_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("stack_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("stack_version", String.class, 255, null,
        false));

    dbAccessor.createTable(STACK_TABLE, columns, "stack_id");
    dbAccessor.addUniqueConstraint(STACK_TABLE, "unq_stack", "stack_name", "stack_version");

    addSequence("stack_id_seq", 0L, false);

    // create the new stack ID columns NULLABLE for now since we need to insert
    // data into them later on (we'll change them to NOT NULL after that)
    dbAccessor.addColumn(CLUSTERS_TABLE, DESIRED_STACK_ID_COLUMN);
    dbAccessor.addColumn("hostcomponentdesiredstate", DESIRED_STACK_ID_COLUMN);
    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE, DESIRED_STACK_ID_COLUMN);
    dbAccessor.addColumn("servicedesiredstate", DESIRED_STACK_ID_COLUMN);

    dbAccessor.addFKConstraint(CLUSTERS_TABLE, "fk_clusters_desired_stack_id", DESIRED_STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);
    dbAccessor.addFKConstraint("hostcomponentdesiredstate", "fk_hcds_desired_stack_id", DESIRED_STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);
    dbAccessor.addFKConstraint(SERVICE_COMPONENT_DESIRED_STATE_TABLE, "fk_scds_desired_stack_id", DESIRED_STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);
    dbAccessor.addFKConstraint("servicedesiredstate", "fk_sds_desired_stack_id", DESIRED_STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);

    dbAccessor.addColumn("clusterstate", CURRENT_STACK_ID_COLUMN);
    dbAccessor.addColumn("hostcomponentstate", CURRENT_STACK_ID_COLUMN);

    dbAccessor.addFKConstraint("clusterstate", "fk_cs_current_stack_id", CURRENT_STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);
    dbAccessor.addFKConstraint("hostcomponentstate", "fk_hcs_current_stack_id", CURRENT_STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);

    dbAccessor.addColumn("clusterconfig", STACK_ID_COLUMN);
    dbAccessor.addColumn("serviceconfig", STACK_ID_COLUMN);
    dbAccessor.addColumn("blueprint", STACK_ID_COLUMN);
    dbAccessor.addColumn(REPO_VERSION_TABLE, STACK_ID_COLUMN);

    dbAccessor.addFKConstraint("clusterconfig", "fk_clusterconfig_stack_id", STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);
    dbAccessor.addFKConstraint("serviceconfig", "fk_serviceconfig_stack_id", STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);
    dbAccessor.addFKConstraint("blueprint", "fk_blueprint_stack_id", STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);
    dbAccessor.addFKConstraint(REPO_VERSION_TABLE, "fk_repoversion_stack_id", STACK_ID_COLUMN_NAME, STACK_TABLE, STACK_ID_COLUMN_NAME, true);

    // drop the unique constraint for the old column and add the new one
    dbAccessor.dropUniqueConstraint(REPO_VERSION_TABLE, "uq_repo_version_stack_version");
    dbAccessor.addUniqueConstraint("repo_version", "uq_repo_version_stack_id", "stack_id", "version");
  }

  /**
   * Adds the stack table and constraints.
   */
  protected void executeStackPreDMLUpdates() throws AmbariException, SQLException {
    Gson gson = new Gson();

    injector.getInstance(AmbariMetaInfo.class);

    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    List<StackEntity> stacks = stackDAO.findAll();
    Map<Long,String> entityToJsonMap = new HashMap<Long, String>();

    // build a mapping of stack entity to old-school JSON
    for( StackEntity stack : stacks ){
      StackId stackId = new StackId(stack.getStackName(),
          stack.getStackVersion());
      String stackJson = gson.toJson(stackId);
      entityToJsonMap.put(stack.getStackId(), stackJson);
    }

    // use a bulk update on all tables to populate the new FK columns
    String UPDATE_TEMPLATE = "UPDATE {0} SET {1} = {2} WHERE {3} = ''{4}''";
    String UPDATE_BLUEPRINT_TEMPLATE = "UPDATE blueprint SET stack_id = {0} WHERE stack_name = ''{1}'' AND stack_version = ''{2}''";

    Set<Long> stackEntityIds = entityToJsonMap.keySet();
    for (Long stackEntityId : stackEntityIds) {
      StackEntity stackEntity = stackDAO.findById(stackEntityId);
      String outdatedJson = entityToJsonMap.get(stackEntityId);
      String outdatedRepoStack = MessageFormat.format("{0}-{1}",stackEntity.getStackName(),stackEntity.getStackVersion());

      String clustersSQL = MessageFormat.format(UPDATE_TEMPLATE, "clusters",
          DESIRED_STACK_ID_COLUMN_NAME, stackEntityId,
          DESIRED_STACK_VERSION_COLUMN_NAME, outdatedJson);

      String hostComponentDesiredStateSQL = MessageFormat.format(
          UPDATE_TEMPLATE, "hostcomponentdesiredstate",
          DESIRED_STACK_ID_COLUMN_NAME, stackEntityId,
          DESIRED_STACK_VERSION_COLUMN_NAME, outdatedJson);

      String serviceComponentDesiredStateSQL = MessageFormat.format(
          UPDATE_TEMPLATE, SERVICE_COMPONENT_DESIRED_STATE_TABLE,
          DESIRED_STACK_ID_COLUMN_NAME, stackEntityId,
          DESIRED_STACK_VERSION_COLUMN_NAME, outdatedJson);

      String serviceDesiredStateSQL = MessageFormat.format(UPDATE_TEMPLATE,
          "servicedesiredstate",
          DESIRED_STACK_ID_COLUMN_NAME, stackEntityId,
          DESIRED_STACK_VERSION_COLUMN_NAME, outdatedJson);

      String clusterStateSQL = MessageFormat.format(UPDATE_TEMPLATE,
          "clusterstate", CURRENT_STACK_ID_COLUMN_NAME, stackEntityId,
          CURRENT_STACK_VERSION_COLUMN_NAME, outdatedJson);

      String hostComponentStateSQL = MessageFormat.format(UPDATE_TEMPLATE,
          "hostcomponentstate", CURRENT_STACK_ID_COLUMN_NAME, stackEntityId,
          CURRENT_STACK_VERSION_COLUMN_NAME, outdatedJson);

      String blueprintSQL = MessageFormat.format(UPDATE_BLUEPRINT_TEMPLATE,
          stackEntityId, stackEntity.getStackName(),
          stackEntity.getStackVersion());

      String repoVersionSQL = MessageFormat.format(UPDATE_TEMPLATE,
          REPO_VERSION_TABLE, STACK_ID_COLUMN_NAME, stackEntityId, "stack",
          outdatedRepoStack);

        dbAccessor.executeQuery(clustersSQL, "clusters", DESIRED_STACK_VERSION_COLUMN_NAME);
        dbAccessor.executeQuery(hostComponentDesiredStateSQL, "hostcomponentdesiredstate", DESIRED_STACK_VERSION_COLUMN_NAME);
        dbAccessor.executeQuery(serviceComponentDesiredStateSQL, SERVICE_COMPONENT_DESIRED_STATE_TABLE, DESIRED_STACK_VERSION_COLUMN_NAME);
        dbAccessor.executeQuery(serviceDesiredStateSQL, "servicedesiredstate", DESIRED_STACK_VERSION_COLUMN_NAME);
        dbAccessor.executeQuery(clusterStateSQL, "clusterstate", CURRENT_STACK_VERSION_COLUMN_NAME);
        dbAccessor.executeQuery(hostComponentStateSQL, "hostcomponentstate", CURRENT_STACK_VERSION_COLUMN_NAME);
        dbAccessor.executeQuery(blueprintSQL, "blueprint", "stack_name");

        dbAccessor.executeQuery(repoVersionSQL, REPO_VERSION_TABLE, "stack");
    }

    // for the tables with no prior stack, set these based on the cluster's
    // stack for each cluster defined
    String INSERT_STACK_ID_TEMPLATE = "UPDATE {0} SET {1} = {2} WHERE cluster_id = {3}";
    // we should do the changes only if they are required
    if (dbAccessor.tableHasColumn(CLUSTERS_TABLE,DESIRED_STACK_VERSION_COLUMN_NAME)) {

      Statement statement = null;
      ResultSet rs = null;
      try {
        statement = dbAccessor.getConnection().createStatement();
        if (statement != null) {
          rs = statement.executeQuery("SELECT * FROM " + CLUSTERS_TABLE);
          if (rs != null) {
            while (rs.next()) {
              long clusterId = rs.getLong("cluster_id");
              String stackJson = rs.getString(DESIRED_STACK_VERSION_COLUMN_NAME);
              StackId stackId = gson.fromJson(stackJson, StackId.class);

              StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
                stackId.getStackVersion());

              String clusterConfigSQL = MessageFormat.format(
                INSERT_STACK_ID_TEMPLATE, "clusterconfig", STACK_ID_COLUMN_NAME,
                stackEntity.getStackId(), clusterId);

              String serviceConfigSQL = MessageFormat.format(
                INSERT_STACK_ID_TEMPLATE, "serviceconfig", STACK_ID_COLUMN_NAME,
                stackEntity.getStackId(), clusterId);

              dbAccessor.executeQuery(clusterConfigSQL);
              dbAccessor.executeQuery(serviceConfigSQL);
            }
          }
        }
        String UPDATE_CURRENT_STACK_ID_IF_NULL_TEMPLATE =
          "UPDATE hostcomponentstate " +
          "SET current_stack_id={0} " +
          "WHERE current_stack_id IS NULL " +
          "AND cluster_id={1} ";
        rs = statement.executeQuery("SELECT cluster_id, current_stack_id FROM clusterstate");
        if (rs != null) {
          while (rs.next()) {
            // if hostcomponentstate.current_stack_id is null,
            // set to cluster's current_stack_id
            long clusterId = rs.getLong("cluster_id");
            long currentStackId = rs.getLong("current_stack_id");
            String hostComponentStateSQL = MessageFormat.format(
              UPDATE_CURRENT_STACK_ID_IF_NULL_TEMPLATE, currentStackId, clusterId);
            dbAccessor.executeUpdate(hostComponentStateSQL, false);
          }
        }
      } finally {
        if (rs != null) {
          rs.close();
        }
        if (statement != null) {
          statement.close();
        }
      }
    }
  }

  /**
   * Copy cluster & service widgets from stack to DB.
   */
  protected void initializeClusterAndServiceWidgets() throws AmbariException {
    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = controller.getClusters();
    if (clusters == null) {
      return;
    }

    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (Cluster cluster : clusterMap.values()) {
        controller.initializeWidgetsAndLayouts(cluster, null);

        Map<String, Service> serviceMap = cluster.getServices();
        if (serviceMap != null && !serviceMap.isEmpty()) {
          for (Service service : serviceMap.values()) {
            controller.initializeWidgetsAndLayouts(cluster, service);
          }
        }
      }
    }
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * Populate the id of the hosts table with an auto-increment int.
   * @param resultSet Rows from the hosts table, sorted first by host_id
   * @return Returns an integer with the id for the next host record to be inserted.
   * @throws SQLException
   */
  Long populateHostsId(ResultSet resultSet) throws SQLException {
    Long hostId = 0L;
    if (resultSet != null) {
      try {
        while (resultSet.next()) {
          hostId++;
          final String hostName = resultSet.getString(1);

          if (StringUtils.isNotBlank(hostName)) {
            dbAccessor.executeQuery("UPDATE " + HOSTS_TABLE + " SET host_id = " + hostId +
                " WHERE " + HOST_NAME_COL + " = '" + hostName + "'");
          }
        }
      } catch (Exception e) {
        LOG.error("Unable to populate the id of the hosts. " + e.getMessage());
      }
    }
    return hostId;
  }

  private String getRandomHostName() throws SQLException {
    String randomHostName = null;

    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        rs = statement.executeQuery("SELECT " + HOST_NAME_COL + " FROM " + HOSTS_TABLE + " ORDER BY " + HOST_NAME_COL + " ASC");
        if (rs != null && rs.next()) {
          randomHostName = rs.getString(1);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to retrieve random host name. Exception: " + e.getMessage());
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (statement != null) {
        statement.close();
      }
    }
    return randomHostName;
  }

  /**
   * Remove duplicate hosts before making host name case-insensitive
   * @throws SQLException
   */
  private void removeDuplicateHosts() throws SQLException {
    // Select hosts not in the cluster
    String hostsNotInClusterQuery = MessageFormat.format(
        "SELECT * FROM {0} WHERE {1} NOT IN (SELECT {1} FROM {2})",
        HOSTS_TABLE, HOST_ID_COL, CLUSTER_HOST_MAPPING_TABLE);
    ResultSet hostsNotInCluster = null;
    Statement statement = null;
    Statement duplicatedHostsStatement = null;

    try {
      statement = dbAccessor.getConnection().createStatement();
      duplicatedHostsStatement = dbAccessor.getConnection().createStatement();
      hostsNotInCluster = statement.executeQuery(hostsNotInClusterQuery);
      if(hostsNotInCluster != null) {
        while (hostsNotInCluster.next()) {
          long hostToDeleteId = hostsNotInCluster.getLong(HOST_ID_COL);
          String hostToDeleteName = hostsNotInCluster.getString(HOST_NAME_COL);
          String duplicateHostsQuery = "SELECT count(*) FROM hosts WHERE lower(host_name) = '" + hostToDeleteName + "' AND host_id != " + hostToDeleteId;
          long count = 0;
          ResultSet duplicateHosts = null;
          try {
            duplicateHosts = duplicatedHostsStatement.executeQuery(duplicateHostsQuery);
            if (duplicateHosts != null && duplicateHosts.next()) {
              count = duplicateHosts.getLong(1);
            }
          } finally {
            if (null != duplicateHosts) {
              duplicateHosts.close();
            }
          }
          if (count > 0) {
            // Delete hosts and host_state table entries for this duplicate host entry
            dbAccessor.executeQuery(
                MessageFormat.format("DELETE from {0} WHERE {1} = {2,number,#}", HOST_STATE_TABLE, HOST_ID_COL, hostToDeleteId));
            dbAccessor.executeQuery(
                MessageFormat.format("DELETE from {0} WHERE {1} = {2,number,#}", HOSTS_TABLE, HOST_ID_COL, hostToDeleteId));
          }
        }
      }
    } finally {
      if (null != hostsNotInCluster) {
        hostsNotInCluster.close();
      }
      if (statement != null) {
        statement.close();
      }
      if (duplicatedHostsStatement != null) {
        duplicatedHostsStatement.close();
      }
    }
  }

  /**
   * Get the constraint name created by Derby if one was not specified for the table.
   * @param type Constraint-type, either, "p" (Primary), "c" (Check), "f" (Foreign), "u" (Unique)
   * @param tableName Table Name
   * @return Return the constraint name, or null if not found.
   * @throws SQLException
   */
  private String getDerbyTableConstraintName(String type, String tableName) throws SQLException {
    boolean found = false;
    String constraint = null;

    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        rs = statement.executeQuery("SELECT c.constraintname, c.type, t.tablename FROM sys.sysconstraints c, sys.systables t WHERE c.tableid = t.tableid");
        if (rs != null) {
          while(rs.next()) {
            constraint = rs.getString(1);
            String recordType = rs.getString(2);
            String recordTableName = rs.getString(3);

            if (recordType.equalsIgnoreCase(type) && recordTableName.equalsIgnoreCase(tableName)) {
              found = true;
              break;
            }
          }
        }
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (statement != null) {
        statement.close();
      }
    }
    return found ? constraint : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    executeStackPreDMLUpdates();
    cleanupStackUpdates();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();

    // Initialize all default widgets and widget layouts
    initializeClusterAndServiceWidgets();

    addMissingConfigs();
    updateAlertDefinitions();
    removeStormRestApiServiceComponent();
    updateKerberosDescriptorArtifacts();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateKerberosDescriptorArtifact(ArtifactDAO artifactDAO, ArtifactEntity artifactEntity) throws AmbariException {
    if (artifactEntity != null) {
      Map<String, Object> data = artifactEntity.getArtifactData();

      if (data != null) {
        final KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(data);

        if (kerberosDescriptor != null) {
          // Get the global "hdfs" identity (if it exists)
          KerberosIdentityDescriptor hdfsIdentity = kerberosDescriptor.getIdentity("hdfs");

          if (hdfsIdentity != null) {
            // Move the "hdfs" global identity to under HDFS service by removing it from the
            // collection of global identities and _merging_ it into the identities for the HDFS
            // service - creating a sparse HDFS service structure if necessary.
            KerberosServiceDescriptor hdfsService = kerberosDescriptor.getService("HDFS");

            if (hdfsService == null) {
              hdfsService = new KerberosServiceDescriptorFactory().createInstance("HDFS", (Map) null);
              hdfsService.putIdentity(hdfsIdentity);
              kerberosDescriptor.putService(hdfsService);
            } else {
              KerberosIdentityDescriptor hdfsReferenceIdentity = hdfsService.getIdentity("/hdfs");

              if (hdfsReferenceIdentity != null) {
                // Merge the changes from the reference identity into the global identity...
                hdfsIdentity.update(hdfsReferenceIdentity);
                // Make sure the identity's name didn't change.
                hdfsIdentity.setName("hdfs");

                hdfsService.removeIdentity("/hdfs");
              }

              hdfsService.putIdentity(hdfsIdentity);
            }

            kerberosDescriptor.removeIdentity("hdfs");
          }

          // Find all identities named "/hdfs" and update the name to "/HDFS/hdfs"
          updateKerberosDescriptorIdentityReferences(kerberosDescriptor, "/hdfs", "/HDFS/hdfs");
          updateKerberosDescriptorIdentityReferences(kerberosDescriptor.getServices(), "/hdfs", "/HDFS/hdfs");

          artifactEntity.setArtifactData(kerberosDescriptor.toMap());
          artifactDAO.merge(artifactEntity);
        }
      }
    }
  }

  /**
   * Delete STORM_REST_API component if HDP is upgraded past 2.2 and the
   * Component still exists.
   */
  protected void removeStormRestApiServiceComponent() {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      for (final Cluster cluster : clusterMap.values()) {
        StackId stackId = cluster.getCurrentStackVersion();
        if (stackId != null && stackId.getStackName().equals("HDP") &&
          VersionUtils.compareVersions(stackId.getStackVersion(), "2.2") >= 0) {

          executeInTransaction(new Runnable() {
            @Override
            public void run() {
            ServiceComponentDesiredStateDAO dao = injector.getInstance(ServiceComponentDesiredStateDAO.class);
            ServiceComponentDesiredStateEntityPK entityPK = new ServiceComponentDesiredStateEntityPK();
            entityPK.setClusterId(cluster.getClusterId());
            entityPK.setServiceName("STORM");
            entityPK.setComponentName("STORM_REST_API");
            ServiceComponentDesiredStateEntity entity = dao.findByPK(entityPK);
            if (entity != null) {
              EntityManager em = getEntityManagerProvider().get();
              CriteriaBuilder cb = em.getCriteriaBuilder();

              try {
                LOG.info("Deleting STORM_REST_API service component.");
                CriteriaDelete<HostComponentStateEntity> hcsDelete = cb.createCriteriaDelete(HostComponentStateEntity.class);
                CriteriaDelete<HostComponentDesiredStateEntity> hcdDelete = cb.createCriteriaDelete(HostComponentDesiredStateEntity.class);
                CriteriaDelete<ServiceComponentDesiredStateEntity> scdDelete = cb.createCriteriaDelete(ServiceComponentDesiredStateEntity.class);

                Root<HostComponentStateEntity> hcsRoot = hcsDelete.from(HostComponentStateEntity.class);
                Root<HostComponentDesiredStateEntity> hcdRoot = hcdDelete.from(HostComponentDesiredStateEntity.class);
                Root<ServiceComponentDesiredStateEntity> scdRoot = scdDelete.from(ServiceComponentDesiredStateEntity.class);

                hcsDelete.where(cb.equal(hcsRoot.get("componentName"), "STORM_REST_API"));
                hcdDelete.where(cb.equal(hcdRoot.get("componentName"), "STORM_REST_API"));
                scdDelete.where(cb.equal(scdRoot.get("componentName"), "STORM_REST_API"));

                em.createQuery(hcsDelete).executeUpdate();
                em.createQuery(hcdDelete).executeUpdate();
                em.createQuery(scdDelete).executeUpdate();
              } catch (Exception e) {
                LOG.warn("Error deleting STORM_REST_API service component. " +
                  "This could result in issue with ambari server start. " +
                  "Please make sure the STORM_REST_API component is deleted " +
                  "from the database by running following commands:\n" +
                  "delete from hostcomponentdesiredstate where component_name='STORM_REST_API';\n" +
                  "delete from hostcomponentstate where component_name='STORM_REST_API';\n" +
                  "delete from servicecomponentdesiredstate where component_name='STORM_REST_API';\n", e);
              }
            }
            }
          });
        }
      }
    }
  }

  /**
   * Modifies the JSON of some of the alert definitions which have changed
   * between Ambari versions.
   */
  protected void updateAlertDefinitions() {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    List<String> metricAlerts = Arrays.asList("namenode_cpu", "namenode_hdfs_blocks_health",
            "namenode_hdfs_capacity_utilization", "namenode_rpc_latency",
            "namenode_directory_status", "datanode_health_summary", "datanode_storage");

    List<String> mapredAlerts = Arrays.asList("mapreduce_history_server_cpu", "mapreduce_history_server_rpc_latency");
    List<String> rmAlerts = Arrays.asList("yarn_resourcemanager_cpu", "yarn_resourcemanager_rpc_latency");

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          // HDFS metric alerts
          for (String alertName : metricAlerts) {
            AlertDefinitionEntity alertDefinitionEntity = alertDefinitionDAO.findByName(
                cluster.getClusterId(), alertName);

            if (alertDefinitionEntity != null) {
              String source = alertDefinitionEntity.getSource();
              JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();

              rootJson.get("uri").getAsJsonObject().addProperty("kerberos_keytab",
                    "{{hdfs-site/dfs.web.authentication.kerberos.keytab}}");

              rootJson.get("uri").getAsJsonObject().addProperty("kerberos_principal",
                    "{{hdfs-site/dfs.web.authentication.kerberos.principal}}");

              updateAlertDefinitionEntitySource(alertName, rootJson.toString(), UUID.randomUUID().toString());
            }
          }

          // MapR alerts update for kerberos
          for (String alertName : mapredAlerts) {
            AlertDefinitionEntity alertDefinitionEntity = alertDefinitionDAO.findByName(
                cluster.getClusterId(), alertName);

            if (alertDefinitionEntity != null) {
              String source = alertDefinitionEntity.getSource();
              JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();
              rootJson.get("uri").getAsJsonObject().addProperty("kerberos_keytab",
                    "{{mapred-site/mapreduce.jobhistory.webapp.spnego-keytab-file}}");

              rootJson.get("uri").getAsJsonObject().addProperty("kerberos_principal",
                    "{{mapred-site/mapreduce.jobhistory.webapp.spnego-principal}}");

              updateAlertDefinitionEntitySource(alertName, rootJson.toString(), UUID.randomUUID().toString());
            }
          }

          // YARN alerts
          for (String alertName : rmAlerts) {
            AlertDefinitionEntity alertDefinitionEntity = alertDefinitionDAO.findByName(
                cluster.getClusterId(), alertName);

            if (alertDefinitionEntity != null) {
              String source = alertDefinitionEntity.getSource();
              JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();

              rootJson.get("uri").getAsJsonObject().addProperty("kerberos_keytab",
                    "{{yarn-site/yarn.resourcemanager.webapp.spnego-keytab-file}}");

              rootJson.get("uri").getAsJsonObject().addProperty("kerberos_principal",
                    "{{yarn-site/yarn.resourcemanager.webapp.spnego-principal}}");

              updateAlertDefinitionEntitySource(alertName, rootJson.toString(), UUID.randomUUID().toString());
            }
          }

          // zookeeper failover conroller alert update for default port and uri
          // to 8019 and dfs.ha.zkfc.port
          AlertDefinitionEntity zkFailoverDefinitionEntity = alertDefinitionDAO.findByName(
              cluster.getClusterId(), "hdfs_zookeeper_failover_controller_process");

          if (zkFailoverDefinitionEntity != null) {
            String source = zkFailoverDefinitionEntity.getSource();
            JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();
            rootJson.remove("uri");
            rootJson.remove("default_port");
            rootJson.addProperty("uri", "{{hdfs-site/dfs.ha.zkfc.port}}");
            rootJson.addProperty("default_port", new Integer(8019));

            // save the changes
            updateAlertDefinitionEntitySource("hdfs_zookeeper_failover_controller_process",
                rootJson.toString(), UUID.randomUUID().toString());
          }

          // update ranger admin alerts from type port(2.2) to web(2.3)
          AlertDefinitionEntity rangerAdminDefinitionEntity = alertDefinitionDAO.findByName(
            cluster.getClusterId(), "ranger_admin_process");

          if (rangerAdminDefinitionEntity != null) {
            String source = rangerAdminDefinitionEntity.getSource();
            JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();
            JsonObject uriJson = new JsonObject();
            JsonObject reporting = rootJson.getAsJsonObject("reporting");
            JsonObject ok = reporting.getAsJsonObject("ok");
            JsonObject warning = reporting.getAsJsonObject("warning");
            JsonObject critical = reporting.getAsJsonObject("critical");            

            rootJson.remove("type");
            rootJson.remove("default_port");
            rootJson.addProperty("type", "WEB");

            uriJson.addProperty("http", "{{admin-properties/policymgr_external_url}}");
            uriJson.addProperty("https", "{{admin-properties/policymgr_external_url}}");
            uriJson.addProperty("https_property", "{{ranger-site/http.enabled}}");
            uriJson.addProperty("https_property_value", "false");
            uriJson.addProperty("connection_timeout", 5.0f);

            rootJson.remove("uri");
            rootJson.add("uri", uriJson);

            ok.remove("text");
            ok.addProperty("text", "HTTP {0} response in {2:.3f}s");

            warning.remove("text");
            warning.remove("value");
            warning.addProperty("text", "HTTP {0} response from {1} in {2:.3f}s ({3})");

            critical.remove("text");
            critical.remove("value");
            critical.addProperty("text", "Connection failed to {1} ({3})");

            // save the changes
            updateAlertDefinitionEntitySource("ranger_admin_process",
              rootJson.toString(), UUID.randomUUID().toString());
          }

          // update oozie web ui alert
          AlertDefinitionEntity oozieWebUIAlertDefinitionEntity = alertDefinitionDAO.findByName(
              cluster.getClusterId(), "oozie_server_webui");

          if (oozieWebUIAlertDefinitionEntity != null) {
            String source = oozieWebUIAlertDefinitionEntity.getSource();
            JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();
            rootJson.get("uri").getAsJsonObject().remove("http");
            rootJson.get("uri").getAsJsonObject().remove("kerberos_keytab");
            rootJson.get("uri").getAsJsonObject().remove("kerberos_principal");
            rootJson.get("uri").getAsJsonObject().addProperty("http",
                    "{{oozie-site/oozie.base.url}}/?user.name={{oozie-env/oozie_user}}");
            rootJson.get("uri").getAsJsonObject().addProperty("kerberos_keytab",
                    "{{cluster-env/smokeuser_keytab}}");
            rootJson.get("uri").getAsJsonObject().addProperty("kerberos_principal",
                    "{{cluster-env/smokeuser_principal_name}}");

            // save the changes
            updateAlertDefinitionEntitySource("oozie_server_webui", rootJson.toString(),
                UUID.randomUUID().toString());
          }

          // update HDFS metric alerts that had changes to their text
          List<String> hdfsMetricAlertsFloatDivision = Arrays.asList(
              "namenode_hdfs_capacity_utilization", "datanode_storage");

          for (String metricAlertName : hdfsMetricAlertsFloatDivision) {
            AlertDefinitionEntity entity = alertDefinitionDAO.findByName(cluster.getClusterId(),
                metricAlertName);

            if (null == entity) {
              continue;
            }

            String source = entity.getSource();
            JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();
            JsonObject reporting = rootJson.getAsJsonObject("reporting");
            JsonObject ok = reporting.getAsJsonObject("ok");
            JsonObject warning = reporting.getAsJsonObject("warning");
            JsonObject critical = reporting.getAsJsonObject("critical");

            JsonElement okText = ok.remove("text");
            ok.addProperty("text", okText.getAsString().replace("{2:d}", "{2:.0f}"));

            JsonElement warningText = warning.remove("text");
            warning.addProperty("text", warningText.getAsString().replace("{2:d}", "{2:.0f}"));

            JsonElement criticalText = critical.remove("text");
            critical.addProperty("text", criticalText.getAsString().replace("{2:d}", "{2:.0f}"));

            // save the changes
            updateAlertDefinitionEntitySource(metricAlertName, rootJson.toString(),
                UUID.randomUUID().toString());
          }
        }
      }
    }
  }

  private void updateAlertDefinitionEntitySource(final String alertName, final String source, final String newHash) {
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        EntityManager em = getEntityManagerProvider().get();
        Query nativeQuery = em.createNativeQuery("UPDATE alert_definition SET alert_source=?1, hash=?2 WHERE " +
          "definition_name=?3");
        nativeQuery.setParameter(1, source);
        nativeQuery.setParameter(2, newHash);
        nativeQuery.setParameter(3, alertName);
        nativeQuery.executeUpdate();
      }
    });
  }

  protected void addMissingConfigs() throws AmbariException {
    updateHiveConfigs();
    updateHdfsConfigs();
    updateStormConfigs();
    updateRangerHiveConfigs();
    updateRangerHBaseConfigs();
    updateHBaseConfigs();
  }

  protected void updateRangerHiveConfigs() throws AmbariException{
    AmbariManagementController ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Config RangerHiveConfig = cluster.getDesiredConfigByType("ranger-hive-plugin-properties");
          if (RangerHiveConfig != null
                  && RangerHiveConfig.getProperties().containsKey("ranger-hive-plugin-enabled")
                  && cluster.getDesiredConfigByType("hive-env") != null) {
            Map<String, String> newHiveEnvProperties = new HashMap<String, String>();
            Map<String, String> newHiveServerProperties = new HashMap<String, String>();
            Set<String> removeRangerHiveProperties = new HashSet<String>();
            removeRangerHiveProperties.add("ranger-hive-plugin-enabled");

            if (RangerHiveConfig.getProperties().get("ranger-hive-plugin-enabled") != null
                    && RangerHiveConfig.getProperties().get("ranger-hive-plugin-enabled").equalsIgnoreCase("yes")) {
              newHiveEnvProperties.put("hive_security_authorization", "Ranger");
              newHiveServerProperties.put("hive.security.authorization.enabled", "true");
            }
            boolean updateProperty = cluster.getDesiredConfigByType("hive-env").getProperties().containsKey("hive_security_authorization");
            updateConfigurationPropertiesForCluster(cluster, "hive-env", newHiveEnvProperties, updateProperty, true);
            updateConfigurationPropertiesForCluster(cluster, "hiveserver2-site", newHiveServerProperties, updateProperty, true);
            removeConfigurationPropertiesFromCluster(cluster, "ranger-hive-plugin-properties", removeRangerHiveProperties);
          }
        }
      }
    }
  }

  protected void updateRangerHBaseConfigs() throws AmbariException{
    AmbariManagementController ambariManagementController = injector.getInstance(
                                                                                  AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Config RangerHBaseConfig = cluster.getDesiredConfigByType("ranger-hbase-plugin-properties");
          if (RangerHBaseConfig != null
                && RangerHBaseConfig.getProperties().containsKey("ranger-hbase-plugin-enabled")
                && cluster.getDesiredConfigByType("hbase-site") != null) {
            Map<String, String> newHBaseSiteProperties = new HashMap<String, String>();

            if (RangerHBaseConfig.getProperties().get("ranger-hbase-plugin-enabled") != null
                  && RangerHBaseConfig.getProperties().get("ranger-hbase-plugin-enabled").equalsIgnoreCase("yes")) {

              newHBaseSiteProperties.put("hbase.security.authorization", "true");
            }
            boolean updateProperty = cluster.getDesiredConfigByType("hbase-site").getProperties().containsKey("hbase.security.authorization");
            updateConfigurationPropertiesForCluster(cluster, "hbase-site", newHBaseSiteProperties, updateProperty, true);
          }
        }
      }
    }
  }

  protected void updateHdfsConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
        AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      Map<String, String> prop = new HashMap<String, String>();
      String content = null;

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          /***
           * Append -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 to HADOOP_NAMENODE_OPTS from hadoop-env.sh
           */
          content = null;
          if (cluster.getDesiredConfigByType("hadoop-env") != null) {
            content = cluster.getDesiredConfigByType(
                "hadoop-env").getProperties().get("content");
          }

          if (content != null) {
            content += "\nexport HADOOP_NAMENODE_OPTS=\"${HADOOP_NAMENODE_OPTS} -Dorg.mortbay.jetty.Request.maxFormContentSize=-1\"";

            prop.put("content", content);
            updateConfigurationPropertiesForCluster(cluster, "hadoop-env",
                prop, true, false);
          }
          /***
           * Update dfs.namenode.rpc-address set hostname instead of localhost
           */
          if (cluster.getDesiredConfigByType(HDFS_SITE_CONFIG) != null && !cluster.getHosts("HDFS","NAMENODE").isEmpty()) {

            URI nameNodeRpc = null;
            String hostName = cluster.getHosts("HDFS","NAMENODE").iterator().next();
            // Try to generate dfs.namenode.rpc-address
            if (cluster.getDesiredConfigByType("core-site").getProperties() != null &&
                      cluster.getDesiredConfigByType("core-site").getProperties().get("fs.defaultFS") != null) {
              try {
                if (isNNHAEnabled(cluster)) {
                  // NN HA enabled
                  // Remove dfs.namenode.rpc-address property
                  Set<String> removePropertiesSet = new HashSet<>();
                  removePropertiesSet.add("dfs.namenode.rpc-address");
                  removeConfigurationPropertiesFromCluster(cluster, HDFS_SITE_CONFIG, removePropertiesSet);
                } else {
                  // NN HA disabled
                  nameNodeRpc = new URI(cluster.getDesiredConfigByType("core-site").getProperties().get("fs.defaultFS"));
                  Map<String, String> hdfsProp = new HashMap<String, String>();
                  hdfsProp.put("dfs.namenode.rpc-address", hostName + ":" + nameNodeRpc.getPort());
                  updateConfigurationPropertiesForCluster(cluster, HDFS_SITE_CONFIG,
                          hdfsProp, false, false);
                }
              } catch (URISyntaxException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    }
  }

  protected void updateHiveConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          String content = null;
          String hive_server2_auth = "";
          if (cluster.getDesiredConfigByType("hive-site") != null &&
              cluster.getDesiredConfigByType("hive-site").getProperties().containsKey("hive.server2.authentication")) {

            hive_server2_auth = cluster.getDesiredConfigByType("hive-site").getProperties().get("hive.server2.authentication");
          }

          if(cluster.getDesiredConfigByType("hive-env") != null) {
            Map<String, String> hiveEnvProps = new HashMap<String, String>();
            Set<String> hiveServerSiteRemoveProps = new HashSet<String>();
            // Update logic for setting HIVE_AUX_JARS_PATH in hive-env.sh
            content = cluster.getDesiredConfigByType("hive-env").getProperties().get("content");
            if(content != null) {
              content = updateHiveEnvContent(content);
              hiveEnvProps.put("content", content);
            }
            //hive metastore and client_heapsize are added for HDP2, we should check if it exists and not add it for HDP1
            if (!cluster.getDesiredConfigByType("hive-env").getProperties().containsKey("hive.client.heapsize")) {
              hiveEnvProps.put("hive.client.heapsize", "512");
            }
            if (!cluster.getDesiredConfigByType("hive-env").getProperties().containsKey("hive.metastore.heapsize")) {
              hiveEnvProps.put("hive.metastore.heapsize", "1024");
            }

            boolean isHiveSecurityAuthPresent = cluster.getDesiredConfigByType("hive-env").getProperties().containsKey("hive_security_authorization");
            String hiveSecurityAuth="";

            if ("kerberos".equalsIgnoreCase(hive_server2_auth) && cluster.getServices().containsKey("KERBEROS")){
              hiveSecurityAuth = "SQLStdAuth";
              isHiveSecurityAuthPresent = true;
              hiveEnvProps.put("hive_security_authorization", hiveSecurityAuth);
            } else {
              if (isHiveSecurityAuthPresent) {
                hiveSecurityAuth = cluster.getDesiredConfigByType("hive-env").getProperties().get("hive_security_authorization");
              }
            }

            if (isHiveSecurityAuthPresent && "none".equalsIgnoreCase(hiveSecurityAuth)) {
              hiveServerSiteRemoveProps.add("hive.security.authorization.manager");
              hiveServerSiteRemoveProps.add("hive.security.authenticator.manager");
            }
            updateConfigurationPropertiesForCluster(cluster, "hive-env", hiveEnvProps, true, true);
            removeConfigurationPropertiesFromCluster(cluster, "hiveserver2-site", hiveServerSiteRemoveProps);
          }

          if(cluster.getDesiredConfigByType("hive-site") != null) {
            Set<String> hiveSiteRemoveProps = new HashSet<String>();
            Map<String, String> hiveSiteAddProps = new HashMap<String, String>();

            if (!"pam".equalsIgnoreCase(hive_server2_auth)) {
              hiveSiteRemoveProps.add("hive.server2.authentication.pam.services");
            } else {
              hiveSiteAddProps.put("hive.server2.authentication.pam.services", "");
            }
            if (!"custom".equalsIgnoreCase(hive_server2_auth)) {
              hiveSiteRemoveProps.add("hive.server2.custom.authentication.class");
            } else {
              hiveSiteAddProps.put("hive.server2.custom.authentication.class", "");
            }
            if (!"ldap".equalsIgnoreCase(hive_server2_auth)) {
              hiveSiteRemoveProps.add("hive.server2.authentication.ldap.url");
            } else {
              hiveSiteAddProps.put("hive.server2.authentication.ldap.url", "");
            }
            if (!"kerberos".equalsIgnoreCase(hive_server2_auth) && !cluster.getServices().containsKey("KERBEROS")) {
              hiveSiteRemoveProps.add("hive.server2.authentication.kerberos.keytab");
              hiveSiteRemoveProps.add("hive.server2.authentication.kerberos.principal");
            } else {
              hiveSiteAddProps.put("hive.server2.authentication.kerberos.keytab", "");
              hiveSiteAddProps.put("hive.server2.authentication.kerberos.principal", "");

            }
            
            
            updateConfigurationPropertiesForCluster(cluster, "hive-site", hiveSiteAddProps, hiveSiteRemoveProps, false, true);
          }
        }
      }
    }
  }

  protected void updateHBaseConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
        AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          if (cluster.getDesiredConfigByType("hbase-site") != null && cluster.getDesiredConfigByType("hbase-env") != null) {
            Map<String, String> hbaseEnvProps = new HashMap<String, String>();
            Map<String, String> hbaseSiteProps = new HashMap<String, String>();
            Set<String> hbaseEnvRemoveProps = new HashSet<String>();
            Set<String> hbaseSiteRemoveProps = new HashSet<String>();

            if (cluster.getDesiredConfigByType("hbase-site").getProperties().containsKey("hbase.region.server.rpc.scheduler.factory.class") &&
                "org.apache.phoenix.hbase.index.ipc.PhoenixIndexRpcSchedulerFactory".equals(cluster.getDesiredConfigByType("hbase-site").getProperties().get(
                        "hbase.region.server.rpc.scheduler.factory.class"))) {
              hbaseEnvProps.put("phoenix_sql_enabled", "true");
            }

            if (cluster.getDesiredConfigByType("hbase-env").getProperties().containsKey("phoenix_sql_enabled") &&
            "true".equalsIgnoreCase(cluster.getDesiredConfigByType("hbase-env").getProperties().get("phoenix_sql_enabled"))) {
              hbaseSiteProps.put("hbase.regionserver.wal.codec", "org.apache.hadoop.hbase.regionserver.wal.IndexedWALEditCodec");
              hbaseSiteProps.put("phoenix.functions.allowUserDefinedFunctions", "true");
            }
            else {
              hbaseSiteProps.put("hbase.regionserver.wal.codec", "org.apache.hadoop.hbase.regionserver.wal.WALCellCodec");
              hbaseSiteRemoveProps.add("hbase.rpc.controllerfactory.class");
              hbaseSiteRemoveProps.add("phoenix.functions.allowUserDefinedFunctions");
            }

            if (cluster.getDesiredConfigByType("hbase-site").getProperties().containsKey("hbase.security.authorization")) {
              if("true".equalsIgnoreCase(cluster.getDesiredConfigByType("hbase-site").getProperties().get("hbase.security.authorization"))) {
                hbaseSiteProps.put("hbase.coprocessor.master.classes", "org.apache.hadoop.hbase.security.access.AccessController");
                hbaseSiteProps.put("hbase.coprocessor.regionserver.classes", "org.apache.hadoop.hbase.security.access.AccessController");
              }
              else {
                hbaseSiteProps.put("hbase.coprocessor.master.classes", "");
                hbaseSiteRemoveProps.add("hbase.coprocessor.regionserver.classes");
              }
            }
            else {
              hbaseSiteRemoveProps.add("hbase.coprocessor.regionserver.classes");
            }

            updateConfigurationPropertiesForCluster(cluster, "hbase-site", hbaseSiteProps, true, false);
            updateConfigurationPropertiesForCluster(cluster, "hbase-env", hbaseEnvProps, true, false);
            updateConfigurationPropertiesForCluster(cluster, "hbase-site", new HashMap<String, String>(), hbaseSiteRemoveProps, false, true);
            updateConfigurationPropertiesForCluster(cluster, "hbase-env", new HashMap<String, String>(), hbaseEnvRemoveProps, false, true);
          }
        }
      }
    }
  }

  protected String updateHiveEnvContent(String hiveEnvContent) {
    if(hiveEnvContent == null) {
      return null;
    }

    String oldAuxJarRegex = "if\\s*\\[\\s*\"\\$\\{HIVE_AUX_JARS_PATH\\}\"\\s*!=\\s*\"\"\\s*];\\s*then\\s*\\n" +
        "\\s*export\\s+HIVE_AUX_JARS_PATH\\s*=\\s*\\$\\{HIVE_AUX_JARS_PATH\\}\\s*\\n" +
        "\\s*elif\\s*\\[\\s*-d\\s*\"/usr/hdp/current/hive-webhcat/share/hcatalog\"\\s*\\];\\s*then\\s*\\n" +
        "\\s*export\\s+HIVE_AUX_JARS_PATH\\s*=\\s*/usr/hdp/current/hive-webhcat/share/hcatalog\\s*\n" +
        "\\s*fi";
    String newAuxJarPath = "if [ \"${HIVE_AUX_JARS_PATH}\" != \"\" ]; then\n" +
        "  if [ -f \"${HIVE_AUX_JARS_PATH}\" ]; then    \n" +
        "    export HIVE_AUX_JARS_PATH=${HIVE_AUX_JARS_PATH}\n" +
        "  elif [ -d \"/usr/hdp/current/hive-webhcat/share/hcatalog\" ]; then\n" +
        "    export HIVE_AUX_JARS_PATH=/usr/hdp/current/hive-webhcat/share/hcatalog/hive-hcatalog-core.jar\n" +
        "  fi\n" +
        "elif [ -d \"/usr/hdp/current/hive-webhcat/share/hcatalog\" ]; then\n" +
        "  export HIVE_AUX_JARS_PATH=/usr/hdp/current/hive-webhcat/share/hcatalog/hive-hcatalog-core.jar\n" +
        "fi";
    return hiveEnvContent.replaceAll(oldAuxJarRegex, Matcher.quoteReplacement(newAuxJarPath));
  }

  protected  void updateStormConfigs() throws  AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          //if cluster is secured we should set additional properties
          if(cluster.getDesiredConfigByType("cluster-env") != null
                  && cluster.getDesiredConfigByType("cluster-env").getProperties().get("security_enabled").equals("true")
                  && cluster.getDesiredConfigByType("storm-site") != null ) {
            Map<String, String> newStormProps = new HashMap<String, String>();
            if (!cluster.getDesiredConfigByType("storm-site").getProperties().containsKey("java.security.auth.login.config")) {
              newStormProps.put("java.security.auth.login.config", "{{conf_dir}}/storm_jaas.conf");
            }
            if (!cluster.getDesiredConfigByType("storm-site").getProperties().containsKey("nimbus.admins")) {
              newStormProps.put("nimbus.admins", "['{{storm_user}}']");
            }
            if (!cluster.getDesiredConfigByType("storm-site").getProperties().containsKey("nimbus.supervisors.users")) {
              newStormProps.put("nimbus.supervisors.users", "['{{storm_user}}']");
            }
            if (!cluster.getDesiredConfigByType("storm-site").getProperties().containsKey("storm.zookeeper.superACL")) {
              newStormProps.put("storm.zookeeper.superACL", "sasl:{{storm_bare_jaas_principal}}");
            }
            if (!cluster.getDesiredConfigByType("storm-site").getProperties().containsKey("ui.filter.params")) {
              newStormProps.put("ui.filter.params", "{'type': 'kerberos', 'kerberos.principal': '{{storm_ui_jaas_principal}}', 'kerberos.keytab': '{{storm_ui_keytab_path}}', 'kerberos.name.rules': 'DEFAULT'}");
            }
            updateConfigurationPropertiesForCluster(cluster, "storm-site", newStormProps, false, true);
          }
        }
      }
    }
  }


  /**
   * Adds non NULL constraints and drops outdated columns no longer needed after
   * the column data migration.
   */
  private void cleanupStackUpdates() throws SQLException {
    DESIRED_STACK_ID_COLUMN.setNullable(false);
    CURRENT_STACK_ID_COLUMN.setNullable(false);
    STACK_ID_COLUMN.setNullable(false);

    // make all stack columns NOT NULL now that they are filled in
    dbAccessor.setColumnNullable(CLUSTERS_TABLE, DESIRED_STACK_ID_COLUMN_NAME, false);
    dbAccessor.setColumnNullable("hostcomponentdesiredstate", DESIRED_STACK_ID_COLUMN_NAME, false);
    dbAccessor.setColumnNullable(SERVICE_COMPONENT_DESIRED_STATE_TABLE, DESIRED_STACK_ID_COLUMN_NAME, false);
    dbAccessor.setColumnNullable("servicedesiredstate", DESIRED_STACK_ID_COLUMN_NAME, false);

    dbAccessor.setColumnNullable("clusterstate", CURRENT_STACK_ID_COLUMN_NAME, false);
    dbAccessor.setColumnNullable("hostcomponentstate", CURRENT_STACK_ID_COLUMN_NAME, false);

    dbAccessor.setColumnNullable("clusterconfig", STACK_ID_COLUMN_NAME, false);
    dbAccessor.setColumnNullable("serviceconfig", STACK_ID_COLUMN_NAME, false);
    dbAccessor.setColumnNullable("blueprint", STACK_ID_COLUMN_NAME, false);
    dbAccessor.setColumnNullable(REPO_VERSION_TABLE, STACK_ID_COLUMN_NAME, false);

    // drop unused JSON columns
    dbAccessor.dropColumn(CLUSTERS_TABLE, DESIRED_STACK_VERSION_COLUMN_NAME);
    dbAccessor.dropColumn("hostcomponentdesiredstate", DESIRED_STACK_VERSION_COLUMN_NAME);
    dbAccessor.dropColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE, DESIRED_STACK_VERSION_COLUMN_NAME);
    dbAccessor.dropColumn("servicedesiredstate", DESIRED_STACK_VERSION_COLUMN_NAME);

    dbAccessor.dropColumn("clusterstate", CURRENT_STACK_VERSION_COLUMN_NAME);
    dbAccessor.dropColumn("hostcomponentstate", CURRENT_STACK_VERSION_COLUMN_NAME);

    dbAccessor.dropColumn("blueprint", "stack_name");
    dbAccessor.dropColumn("blueprint", "stack_version");

    dbAccessor.dropColumn(REPO_VERSION_TABLE, "stack");
  }
}
