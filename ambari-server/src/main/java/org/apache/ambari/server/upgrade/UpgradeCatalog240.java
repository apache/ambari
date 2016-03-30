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

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

/**
 * Upgrade catalog for version 2.4.0.
 */
public class UpgradeCatalog240 extends AbstractUpgradeCatalog {

  protected static final String ADMIN_PERMISSION_TABLE = "adminpermission";
  protected static final String ALERT_DEFINITION_TABLE = "alert_definition";
  protected static final String HELP_URL_COLUMN = "help_url";
  protected static final String REPEAT_TOLERANCE_COLUMN = "repeat_tolerance";
  protected static final String REPEAT_TOLERANCE_ENABLED_COLUMN = "repeat_tolerance_enabled";
  protected static final String PERMISSION_ID_COL = "permission_name";
  protected static final String SORT_ORDER_COL = "sort_order";
  protected static final String REPO_VERSION_TABLE = "repo_version";
  protected static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  protected static final String SERVICE_COMPONENT_DS_TABLE = "servicecomponentdesiredstate";
  protected static final String HOST_COMPONENT_DS_TABLE = "hostcomponentdesiredstate";
  protected static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  protected static final String SERVICE_COMPONENT_HISTORY_TABLE = "servicecomponent_history";
  protected static final String UPGRADE_TABLE = "upgrade";
  protected static final String STACK_TABLE = "stack";
  protected static final String CLUSTER_TABLE = "clusters";
  protected static final String CLUSTER_UPGRADE_ID_COLUMN = "upgrade_id";
  public static final String DESIRED_VERSION_COLUMN_NAME = "desired_version";


  @Inject
  PermissionDAO permissionDAO;

  @Inject
  ResourceTypeDAO resourceTypeDAO;

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog240.class);

  private static final String ID = "id";
  private static final String SETTING_TABLE = "setting";

  protected static final String SERVICE_COMPONENT_DESIRED_STATE_TABLE = "servicecomponentdesiredstate";
  protected static final String RECOVERY_ENABLED_COL = "recovery_enabled";

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog240(Injector injector) {
    super(injector);
    injector.injectMembers(this);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.4.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.3.0";
  }


  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateAdminPermissionTable();
    updateServiceComponentDesiredStateTable();
    createSettingTable();
    updateRepoVersionTableDDL();
    updateServiceComponentDesiredStateTableDDL();
    createServiceComponentHistoryTable();
    updateClusterTableDDL();
    updateAlertDefinitionTable();
  }

  private void updateClusterTableDDL() throws SQLException {
    dbAccessor.addColumn(CLUSTER_TABLE, new DBColumnInfo(CLUSTER_UPGRADE_ID_COLUMN, Long.class, null, null, true));

    dbAccessor.addFKConstraint(CLUSTER_TABLE, "FK_clusters_upgrade_id",
      CLUSTER_UPGRADE_ID_COLUMN, UPGRADE_TABLE, "upgrade_id", false);
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    updateAlerts();
    setRoleSortOrder();
    addSettingPermission();
    addManageUserPersistedDataPermission();
    updateAMSConfigs();
  }

  private void createSettingTable() throws SQLException {
    List<DBColumnInfo> columns = new ArrayList<>();

    //  Add setting table
    LOG.info("Creating " + SETTING_TABLE + " table");

    columns.add(new DBColumnInfo(ID, Long.class, null, null, false));
    columns.add(new DBColumnInfo("name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("setting_type", String.class, 255, null, false));
    columns.add(new DBColumnInfo("content", String.class, 3000, null, false));
    columns.add(new DBColumnInfo("updated_by", String.class, 255, "_db", false));
    columns.add(new DBColumnInfo("update_timestamp", Long.class, null, null, false));
    dbAccessor.createTable(SETTING_TABLE, columns, ID);
    addSequence("setting_id_seq", 0L, false);
  }

  protected void addSettingPermission() throws SQLException {
    RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);

    if (roleAuthorizationDAO.findById("AMBARI.MANAGE_SETTINGS") == null) {
      RoleAuthorizationEntity roleAuthorizationEntity = new RoleAuthorizationEntity();
      roleAuthorizationEntity.setAuthorizationId("AMBARI.MANAGE_SETTINGS");
      roleAuthorizationEntity.setAuthorizationName("Manage settings");
      roleAuthorizationDAO.create(roleAuthorizationEntity);
    }

    String administratorPermissionId = permissionDAO.findPermissionByNameAndType("AMBARI.ADMINISTRATOR",
        resourceTypeDAO.findByName("AMBARI")).getId().toString();
    dbAccessor.insertRowIfMissing("permission_roleauthorization", new String[]{"permission_id", "authorization_id"},
      new String[]{"'" + administratorPermissionId + "'", "'AMBARI.MANAGE_SETTINGS'"}, false);
  }

  /**
   * Add 'MANAGE_USER_PERSISTED_DATA' permissions for CLUSTER.ADMINISTRATOR, SERVICE.OPERATOR, SERVICE.ADMINISTRATOR,
   * CLUSTER.OPERATOR, AMBARI.ADMINISTRATOR.
   *
   */
  protected void addManageUserPersistedDataPermission() throws SQLException {

    RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);

    // Add to 'roleauthorization' table
    if (roleAuthorizationDAO.findById("CLUSTER.MANAGE_USER_PERSISTED_DATA") == null) {
      RoleAuthorizationEntity roleAuthorizationEntity = new RoleAuthorizationEntity();
      roleAuthorizationEntity.setAuthorizationId("CLUSTER.MANAGE_USER_PERSISTED_DATA");
      roleAuthorizationEntity.setAuthorizationName("Manage cluster-level user persisted data");
      roleAuthorizationDAO.create(roleAuthorizationEntity);
    }

    // Adds to 'permission_roleauthorization' table
    String permissionId = permissionDAO.findPermissionByNameAndType("CLUSTER.ADMINISTRATOR",
      resourceTypeDAO.findByName("CLUSTER")).getId().toString();
    dbAccessor.insertRowIfMissing("permission_roleauthorization", new String[]{"permission_id", "authorization_id"},
      new String[]{"'" + permissionId + "'", "'CLUSTER.MANAGE_USER_PERSISTED_DATA'"}, false);

    permissionId = permissionDAO.findPermissionByNameAndType("SERVICE.OPERATOR",
      resourceTypeDAO.findByName("CLUSTER")).getId().toString();
    dbAccessor.insertRowIfMissing("permission_roleauthorization", new String[]{"permission_id", "authorization_id"},
      new String[]{"'" + permissionId + "'", "'CLUSTER.MANAGE_USER_PERSISTED_DATA'"}, false);

    permissionId = permissionDAO.findPermissionByNameAndType("SERVICE.ADMINISTRATOR",
      resourceTypeDAO.findByName("CLUSTER")).getId().toString();
    dbAccessor.insertRowIfMissing("permission_roleauthorization", new String[]{"permission_id", "authorization_id"},
      new String[]{"'" + permissionId + "'", "'CLUSTER.MANAGE_USER_PERSISTED_DATA'"}, false);

    permissionId = permissionDAO.findPermissionByNameAndType("CLUSTER.OPERATOR",
      resourceTypeDAO.findByName("CLUSTER")).getId().toString();
    dbAccessor.insertRowIfMissing("permission_roleauthorization", new String[]{"permission_id", "authorization_id"},
      new String[]{"'" + permissionId + "'", "'CLUSTER.MANAGE_USER_PERSISTED_DATA'"}, false);

    permissionId = permissionDAO.findPermissionByNameAndType("AMBARI.ADMINISTRATOR",
      resourceTypeDAO.findByName("AMBARI")).getId().toString();
    dbAccessor.insertRowIfMissing("permission_roleauthorization", new String[]{"permission_id", "authorization_id"},
      new String[]{"'" + permissionId + "'", "'CLUSTER.MANAGE_USER_PERSISTED_DATA'"}, false);

    permissionId = permissionDAO.findPermissionByNameAndType("CLUSTER.USER",
      resourceTypeDAO.findByName("CLUSTER")).getId().toString();
    dbAccessor.insertRowIfMissing("permission_roleauthorization", new String[]{"permission_id", "authorization_id"},
      new String[]{"'" + permissionId + "'", "'CLUSTER.MANAGE_USER_PERSISTED_DATA'"}, false);

  }

  protected void updateAlerts() {
    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterID = cluster.getClusterId();

      final AlertDefinitionEntity namenodeLastCheckpointAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "namenode_last_checkpoint");
      final AlertDefinitionEntity namenodeHAHealthAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "namenode_ha_health");
      final AlertDefinitionEntity nodemanagerHealthAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "yarn_nodemanager_health");
      final AlertDefinitionEntity nodemanagerHealthSummaryAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "nodemanager_health_summary");
      final AlertDefinitionEntity hiveMetastoreProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_metastore_process");
      final AlertDefinitionEntity hiveServerProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_server_process");
      final AlertDefinitionEntity hiveWebhcatServerStatusAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_webhcat_server_status");
      final AlertDefinitionEntity flumeAgentStatusAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "flume_agent_status");

      Map<AlertDefinitionEntity, List<String>> alertDefinitionParams = new HashMap<>();
      checkedPutToMap(alertDefinitionParams, namenodeLastCheckpointAlertDefinitionEntity,
              Lists.newArrayList("connection.timeout", "checkpoint.time.warning.threshold", "checkpoint.time.critical.threshold"));
      checkedPutToMap(alertDefinitionParams, namenodeHAHealthAlertDefinitionEntity,
              Lists.newArrayList("connection.timeout"));
      checkedPutToMap(alertDefinitionParams, nodemanagerHealthAlertDefinitionEntity,
              Lists.newArrayList("connection.timeout"));
      checkedPutToMap(alertDefinitionParams, nodemanagerHealthSummaryAlertDefinitionEntity,
              Lists.newArrayList("connection.timeout"));
      checkedPutToMap(alertDefinitionParams, hiveMetastoreProcessAlertDefinitionEntity,
              Lists.newArrayList("default.smoke.user", "default.smoke.principal", "default.smoke.keytab"));
      checkedPutToMap(alertDefinitionParams, hiveServerProcessAlertDefinitionEntity,
              Lists.newArrayList("default.smoke.user", "default.smoke.principal", "default.smoke.keytab"));
      checkedPutToMap(alertDefinitionParams, hiveWebhcatServerStatusAlertDefinitionEntity,
              Lists.newArrayList("default.smoke.user", "connection.timeout"));
      checkedPutToMap(alertDefinitionParams, flumeAgentStatusAlertDefinitionEntity,
              Lists.newArrayList("run.directory"));

      for(Map.Entry<AlertDefinitionEntity, List<String>> entry : alertDefinitionParams.entrySet()){
        AlertDefinitionEntity alertDefinition = entry.getKey();
        String source = alertDefinition.getSource();

        alertDefinition.setSource(addParam(source, entry.getValue()));
        alertDefinition.setHash(UUID.randomUUID().toString());

        alertDefinitionDAO.merge(alertDefinition);
      }

    }
  }

  /*
  * Simple put method with check for key is not null
  * */
  private void checkedPutToMap(Map<AlertDefinitionEntity, List<String>> alertDefinitionParams, AlertDefinitionEntity alertDefinitionEntity,
                               List<String> params) {
    if (alertDefinitionEntity != null) {
      alertDefinitionParams.put(alertDefinitionEntity, params);
    }
  }

  protected String addParam(String source, List<String> params) {
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    JsonArray parametersJson = sourceJson.getAsJsonArray("parameters");

    boolean parameterExists = parametersJson != null && !parametersJson.isJsonNull();

    if (parameterExists) {
      Iterator<JsonElement> jsonElementIterator = parametersJson.iterator();
      while(jsonElementIterator.hasNext()) {
        JsonElement element = jsonElementIterator.next();
        JsonElement name = element.getAsJsonObject().get("name");
        if (name != null && !name.isJsonNull() && params.contains(name.getAsString())) {
          params.remove(name.getAsString());
        }
      }
      if (params.size() == 0) {
        return sourceJson.toString();
      }
    }

    List<JsonObject> paramsToAdd = new ArrayList<>();

    if (params.contains("connection.timeout")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("connection.timeout"));
      param.add("display_name", new JsonPrimitive("Connection Timeout"));
      param.add("value", new JsonPrimitive(5.0));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The maximum time before this alert is considered to be CRITICAL"));
      param.add("units", new JsonPrimitive("seconds"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));

      paramsToAdd.add(param);

    }
    if (params.contains("checkpoint.time.warning.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("checkpoint.time.warning.threshold"));
      param.add("display_name", new JsonPrimitive("Checkpoint Warning"));
      param.add("value", new JsonPrimitive(2.0));
      param.add("type", new JsonPrimitive("PERCENT"));
      param.add("description", new JsonPrimitive("The percentage of the last checkpoint time greater than the interval in order to trigger a warning alert."));
      param.add("units", new JsonPrimitive("%"));
      param.add("threshold", new JsonPrimitive("WARNING"));

      paramsToAdd.add(param);

    }
    if (params.contains("checkpoint.time.critical.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("checkpoint.time.critical.threshold"));
      param.add("display_name", new JsonPrimitive("Checkpoint Critical"));
      param.add("value", new JsonPrimitive(2.0));
      param.add("type", new JsonPrimitive("PERCENT"));
      param.add("description", new JsonPrimitive("The percentage of the last checkpoint time greater than the interval in order to trigger a critical alert."));
      param.add("units", new JsonPrimitive("%"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));

      paramsToAdd.add(param);

    }
    if (params.contains("default.smoke.user")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.user"));
      param.add("display_name", new JsonPrimitive("Default Smoke User"));
      param.add("value", new JsonPrimitive("ambari-qa"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The user that will run the Hive commands if not specified in cluster-env/smokeuser"));

      paramsToAdd.add(param);

    }
    if (params.contains("default.smoke.principal")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.principal"));
      param.add("display_name", new JsonPrimitive("Default Smoke Principal"));
      param.add("value", new JsonPrimitive("ambari-qa@EXAMPLE.COM"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The principal to use when retrieving the kerberos ticket if not specified in cluster-env/smokeuser_principal_name"));

      paramsToAdd.add(param);

    }
    if (params.contains("default.smoke.keytab")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.keytab"));
      param.add("display_name", new JsonPrimitive("Default Smoke Keytab"));
      param.add("value", new JsonPrimitive("/etc/security/keytabs/smokeuser.headless.keytab"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The keytab to use when retrieving the kerberos ticket if not specified in cluster-env/smokeuser_keytab"));

      paramsToAdd.add(param);

    }
    if (params.contains("run.directory")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("run.directory"));
      param.add("display_name", new JsonPrimitive("Run Directory"));
      param.add("value", new JsonPrimitive("/var/run/flume"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The directory where flume agent processes will place their PID files."));

      paramsToAdd.add(param);

    }


    if (!parameterExists) {
      parametersJson = new JsonArray();
      for (JsonObject param : paramsToAdd) {
        parametersJson.add(param);
      }
      sourceJson.add("parameters", parametersJson);
    } else {
      for (JsonObject param : paramsToAdd) {
        parametersJson.add(param);
      }
      sourceJson.remove("parameters");
      sourceJson.add("parameters", parametersJson);
    }

    return sourceJson.toString();
  }

  protected void updateAdminPermissionTable() throws SQLException {
    // Add the sort_order column to the adminpermission table
    dbAccessor.addColumn(ADMIN_PERMISSION_TABLE,
        new DBColumnInfo(SORT_ORDER_COL, Short.class, null, 1, false));
  }

  /**
   * Updates the {@value #ALERT_DEFINITION_TABLE} in the following ways:
   * <ul>
   * <li>Craetes the {@value #HELP_URL_COLUMN} column</li>
   * <li>Craetes the {@value #REPEAT_TOLERANCE_COLUMN} column</li>
   * <li>Craetes the {@value #REPEAT_TOLERANCE_ENABLED_COLUMN} column</li>
   * </ul>
   *
   * @throws SQLException
   */
  protected void updateAlertDefinitionTable() throws SQLException {
    dbAccessor.addColumn(ALERT_DEFINITION_TABLE,
        new DBColumnInfo(HELP_URL_COLUMN, String.class, 512, null, true));

    dbAccessor.addColumn(ALERT_DEFINITION_TABLE,
        new DBColumnInfo(REPEAT_TOLERANCE_COLUMN, Integer.class, null, 1, false));

    dbAccessor.addColumn(ALERT_DEFINITION_TABLE,
        new DBColumnInfo(REPEAT_TOLERANCE_ENABLED_COLUMN, Short.class, null, 0, false));
  }

  protected void setRoleSortOrder() throws SQLException {
    String updateStatement = "UPDATE " + ADMIN_PERMISSION_TABLE + " SET " + SORT_ORDER_COL + "=%d WHERE " + PERMISSION_ID_COL + "='%s'";

    LOG.info("Setting permission labels");
    dbAccessor.executeUpdate(String.format(updateStatement,
        1, PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
        2, PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
        3, PermissionEntity.CLUSTER_OPERATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
        4, PermissionEntity.SERVICE_ADMINISTRATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
        5, PermissionEntity.SERVICE_OPERATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
        6, PermissionEntity.CLUSTER_USER_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
      7, PermissionEntity.VIEW_USER_PERMISSION_NAME));
  }

  /**
   * Makes the following changes to the {@value #REPO_VERSION_TABLE} table:
   * <ul>
   * <li>repo_type VARCHAR(255) DEFAULT 'STANDARD' NOT NULL</li>
   * <li>version_url VARCHAR(1024)</li>
   * <li>version_xml MEDIUMTEXT</li>
   * <li>version_xsd VARCHAR(512)</li>
   * <li>parent_id BIGINT</li>
   * </ul>
   *
   * @throws SQLException
   */
  private void updateRepoVersionTableDDL() throws SQLException {
    DBColumnInfo repoTypeColumn = new DBColumnInfo("repo_type", String.class, 255, RepositoryType.STANDARD.name(), false);
    DBColumnInfo versionUrlColumn = new DBColumnInfo("version_url", String.class, 1024, null, true);
    DBColumnInfo versionXmlColumn = new DBColumnInfo("version_xml", Clob.class, null, null, true);
    DBColumnInfo versionXsdColumn = new DBColumnInfo("version_xsd", String.class, 512, null, true);
    DBColumnInfo parentIdColumn = new DBColumnInfo("parent_id", Long.class, null, null, true);

    dbAccessor.addColumn(REPO_VERSION_TABLE, repoTypeColumn);
    dbAccessor.addColumn(REPO_VERSION_TABLE, versionUrlColumn);
    dbAccessor.addColumn(REPO_VERSION_TABLE, versionXmlColumn);
    dbAccessor.addColumn(REPO_VERSION_TABLE, versionXsdColumn);
    dbAccessor.addColumn(REPO_VERSION_TABLE, parentIdColumn);
  }

  /**
   * Makes the following changes to the {@value #SERVICE_COMPONENT_DS_TABLE} table,
   * but only if the table doesn't have it's new PK set.
   * <ul>
   * <li>id BIGINT NOT NULL</li>
   * <li>Drops FKs on {@value #HOST_COMPONENT_DS_TABLE} and {@value #HOST_COMPONENT_STATE_TABLE}</li>
   * <li>Populates {@value #SQLException#ID} in {@value #SERVICE_COMPONENT_DS_TABLE}</li>
   * <li>Creates {@code UNIQUE} constraint on {@value #HOST_COMPONENT_DS_TABLE}</li>
   * <li>Adds FKs on {@value #HOST_COMPONENT_DS_TABLE} and {@value #HOST_COMPONENT_STATE_TABLE}</li>
   * <li>Adds new sequence value of {@code servicecomponentdesiredstate_id_seq}</li>
   * </ul>
   *
   * @throws SQLException
   */
  @Transactional
  private void updateServiceComponentDesiredStateTableDDL() throws SQLException {
    if (dbAccessor.tableHasPrimaryKey(SERVICE_COMPONENT_DS_TABLE, ID)) {
      LOG.info("Skipping {} table Primary Key modifications since the new {} column already exists",
          SERVICE_COMPONENT_DS_TABLE, ID);

      return;
    }

    // drop FKs to SCDS in both HCDS and HCS tables
    dbAccessor.dropFKConstraint(HOST_COMPONENT_DS_TABLE, "hstcmpnntdesiredstatecmpnntnme");
    dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, "hstcomponentstatecomponentname");

    // remove existing compound PK
    dbAccessor.dropPKConstraint(SERVICE_COMPONENT_DS_TABLE, "servicecomponentdesiredstate_pkey");

    // add new PK column to SCDS, making it nullable for now
    DBColumnInfo idColumn = new DBColumnInfo(ID, Long.class, null, null, true);
    dbAccessor.addColumn(SERVICE_COMPONENT_DS_TABLE, idColumn);

    // populate SCDS id column
    AtomicLong scdsIdCounter = new AtomicLong(1);
    Statement statement = null;
    ResultSet resultSet = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        String selectSQL = String.format("SELECT cluster_id, service_name, component_name FROM %s",
            SERVICE_COMPONENT_DS_TABLE);

        resultSet = statement.executeQuery(selectSQL);
        while (null != resultSet && resultSet.next()) {
          final Long clusterId = resultSet.getLong("cluster_id");
          final String serviceName = resultSet.getString("service_name");
          final String componentName = resultSet.getString("component_name");

          String updateSQL = String.format(
              "UPDATE %s SET %s = %d WHERE cluster_id = %d AND service_name = '%s' AND component_name = '%s'",
              SERVICE_COMPONENT_DS_TABLE, ID, scdsIdCounter.getAndIncrement(), clusterId,
              serviceName, componentName);

          dbAccessor.executeQuery(updateSQL);
        }
      }
    } finally {
      JdbcUtils.closeResultSet(resultSet);
      JdbcUtils.closeStatement(statement);
    }

    // make the column NON NULL now
    dbAccessor.alterColumn(SERVICE_COMPONENT_DS_TABLE,
        new DBColumnInfo(ID, Long.class, null, null, false));

    // create a new PK, matching the name of the constraint found in SQL
    dbAccessor.addPKConstraint(SERVICE_COMPONENT_DS_TABLE, "pk_sc_desiredstate", ID);

    // create UNIQUE constraint, ensuring column order matches SQL files
    String[] uniqueColumns = new String[] { "component_name", "service_name", "cluster_id" };
    dbAccessor.addUniqueConstraint(SERVICE_COMPONENT_DS_TABLE, "unq_scdesiredstate_name",
        uniqueColumns);

    // add FKs back to SCDS in both HCDS and HCS tables
    dbAccessor.addFKConstraint(HOST_COMPONENT_DS_TABLE, "hstcmpnntdesiredstatecmpnntnme",
        uniqueColumns, SERVICE_COMPONENT_DS_TABLE, uniqueColumns, false);

    dbAccessor.addFKConstraint(HOST_COMPONENT_STATE_TABLE, "hstcomponentstatecomponentname",
        uniqueColumns, SERVICE_COMPONENT_DS_TABLE, uniqueColumns, false);

    // Add sequence for SCDS id
    addSequence("servicecomponentdesiredstate_id_seq", scdsIdCounter.get(), false);
  }

  /**
   * Makes the following changes to the {@value #SERVICE_COMPONENT_HISTORY_TABLE} table:
   * <ul>
   * <li>id BIGINT NOT NULL</li>
   * <li>component_id BIGINT NOT NULL</li>
   * <li>upgrade_id BIGINT NOT NULL</li>
   * <li>from_stack_id BIGINT NOT NULL</li>
   * <li>to_stack_id BIGINT NOT NULL</li>
   * <li>CONSTRAINT PK_sc_history PRIMARY KEY (id)</li>
   * <li>CONSTRAINT FK_sc_history_component_id FOREIGN KEY (component_id) REFERENCES servicecomponentdesiredstate (id)</li>
   * <li>CONSTRAINT FK_sc_history_upgrade_id FOREIGN KEY (upgrade_id) REFERENCES upgrade (upgrade_id)</li>
   * <li>CONSTRAINT FK_sc_history_from_stack_id FOREIGN KEY (from_stack_id) REFERENCES stack (stack_id)</li>
   * <li>CONSTRAINT FK_sc_history_to_stack_id FOREIGN KEY (to_stack_id) REFERENCES stack (stack_id)</li>
   * <li>Creates the {@code servicecomponent_history_id_seq}</li>
   * </ul>
   *
   * @throws SQLException
   */
  private void createServiceComponentHistoryTable() throws SQLException {
    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo(ID, Long.class, null, null, false));
    columns.add(new DBColumnInfo("component_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("upgrade_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("from_stack_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("to_stack_id", Long.class, null, null, false));
    dbAccessor.createTable(SERVICE_COMPONENT_HISTORY_TABLE, columns, (String[]) null);

    dbAccessor.addPKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "PK_sc_history", ID);

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "FK_sc_history_component_id",
        "component_id", SERVICE_COMPONENT_DS_TABLE, "id", false);

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "FK_sc_history_upgrade_id",
        "upgrade_id", UPGRADE_TABLE, "upgrade_id", false);

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "FK_sc_history_from_stack_id",
        "from_stack_id", STACK_TABLE, "stack_id", false);

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "FK_sc_history_to_stack_id",
        "to_stack_id", STACK_TABLE, "stack_id", false);

    addSequence("servicecomponent_history_id_seq", 0L, false);
  }

  /**
   * Alter servicecomponentdesiredstate table to add recovery_enabled column.
   * @throws SQLException
   */
  private void updateServiceComponentDesiredStateTable() throws SQLException {
    // ALTER TABLE servicecomponentdesiredstate ADD COLUMN
    // recovery_enabled SMALLINT DEFAULT 0 NOT NULL
    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
            new DBColumnInfo(RECOVERY_ENABLED_COL, Short.class, null, 0, false));

    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
      new DBColumnInfo(DESIRED_VERSION_COLUMN_NAME, String.class, 255, State.UNKNOWN.toString(), false));
  }

  /**
   * Alter host_role_command table to add original_start_time, which is needed because the start_time column now
   * allows overriding the value in ActionScheduler.java
   * @throws SQLException
   */
  private void updateHostRoleCommandTable() throws SQLException {
    final String columnName = "original_start_time";
    DBColumnInfo originalStartTimeColumn = new DBColumnInfo(columnName, Long.class, null, -1L, true);
    dbAccessor.addColumn(HOST_ROLE_COMMAND_TABLE, originalStartTimeColumn);
    dbAccessor.executeQuery("UPDATE " + HOST_ROLE_COMMAND_TABLE + " SET original_start_time = start_time", false);
    dbAccessor.executeQuery("UPDATE " + HOST_ROLE_COMMAND_TABLE + " SET original_start_time=-1 WHERE original_start_time IS NULL");
    dbAccessor.setColumnNullable(HOST_ROLE_COMMAND_TABLE, columnName, false);
  }

  protected void updateAMSConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {

          Config amsEnv = cluster.getDesiredConfigByType("ams-env");

          if (amsEnv != null) {
            String content = amsEnv.getProperties().get("content");
            if (content != null && !content.contains("AMS_INSTANCE_NAME")) {
              String newContent = content + "\n # AMS instance name\n" +
                "export AMS_INSTANCE_NAME={{hostname}}\n";

              updateConfigurationProperties("ams-env", Collections.singletonMap("content", newContent), true, true);
            }
          }
        }
      }
    }
  }
}
