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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.CommandExecutionType;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Upgrade catalog for version 2.5.0.
 */
public class UpgradeCatalog250 extends AbstractUpgradeCatalog {

  protected static final String HOST_VERSION_TABLE = "host_version";
  protected static final String GROUPS_TABLE = "groups";
  protected static final String GROUP_TYPE_COL = "group_type";
  private static final String AMS_ENV = "ams-env";
  private static final String HADOOP_ENV = "hadoop-env";
  private static final String KAFKA_BROKER = "kafka-broker";
  private static final String KAFKA_TIMELINE_METRICS_HOST = "kafka.timeline.metrics.host";

  public static final String COMPONENT_TABLE = "servicecomponentdesiredstate";
  public static final String COMPONENT_VERSION_TABLE = "servicecomponent_version";
  public static final String COMPONENT_VERSION_PK = "PK_sc_version";
  public static final String COMPONENT_VERSION_FK_COMPONENT = "FK_scv_component_id";
  public static final String COMPONENT_VERSION_FK_REPO_VERSION = "FK_scv_repo_version_id";

  protected static final String SERVICE_DESIRED_STATE_TABLE = "servicedesiredstate";
  protected static final String CREDENTIAL_STORE_SUPPORTED_COL = "credential_store_supported";
  protected static final String CREDENTIAL_STORE_ENABLED_COL = "credential_store_enabled";

  protected static final String HOST_COMPONENT_DESIREDSTATE_TABLE = "hostcomponentdesiredstate";
  protected static final String HOST_COMPONENT_DESIREDSTATE_ID_COL = "id";
  protected static final String HOST_COMPONENT_DESIREDSTATE_INDEX = "UQ_hcdesiredstate_name";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog250.class);

  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog250(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.5.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.4.2";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateHostVersionTable();
    createComponentVersionTable();
    updateGroupsTable();
    dbAccessor.addColumn("stage",
      new DBAccessor.DBColumnInfo("command_execution_type", String.class, 32, CommandExecutionType.STAGE.toString(),
        false));
    updateServiceDesiredStateTable();
    updateHostComponentDesiredStateTable();
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
    updateAMSConfigs();
    updateHadoopEnvConfigs();
    updateKafkaConfigs();
    updateHIVEInteractiveConfigs();
    updateTEZInteractiveConfigs();
    updateHiveLlapConfigs();
    updateTablesForZeppelinViewRemoval();
    updateAtlasConfigs();
    addManageServiceAutoStartPermissions();
  }

  protected void updateHostVersionTable() throws SQLException {
    LOG.info("Updating the {} table", HOST_VERSION_TABLE);

    // Add the unique constraint to the host_version table
    dbAccessor.addUniqueConstraint(HOST_VERSION_TABLE, "UQ_host_repo", "repo_version_id", "host_id");
  }

  protected void updateGroupsTable() throws SQLException {
    LOG.info("Updating the {} table", GROUPS_TABLE);

    dbAccessor.addColumn(GROUPS_TABLE, new DBColumnInfo(GROUP_TYPE_COL, String.class, null, "LOCAL", false));
    dbAccessor.executeQuery("UPDATE groups SET group_type='LDAP' WHERE ldap_group=1");
    dbAccessor.addUniqueConstraint(GROUPS_TABLE, "UNQ_groups_0", "group_name", "group_type");
  }

  protected void updateAMSConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {

          Config amsEnv = cluster.getDesiredConfigByType(AMS_ENV);
          if (amsEnv != null) {
            Map<String, String> amsEnvProperties = amsEnv.getProperties();
            String content = amsEnvProperties.get("content");
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("content", updateAmsEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_ENV, newProperties, true, true);
          }

        }
      }
    }
  }

  protected void updateTablesForZeppelinViewRemoval() throws SQLException {
    dbAccessor.executeQuery("DELETE from viewinstance WHERE view_name='ZEPPELIN{1.0.0}'", true);
    dbAccessor.executeQuery("DELETE from viewmain WHERE view_name='ZEPPELIN{1.0.0}'", true);
    dbAccessor.executeQuery("DELETE from viewparameter WHERE view_name='ZEPPELIN{1.0.0}'", true);
  }

  protected String updateAmsEnvContent(String content) {
    if (content == null) {
      return null;
    }

    List<String> toReplaceList = new ArrayList<>();
    toReplaceList.add("\n# HBase normalizer enabled\n");
    toReplaceList.add("\n# HBase compaction policy enabled\n");
    toReplaceList.add("export AMS_HBASE_NORMALIZER_ENABLED={{ams_hbase_normalizer_enabled}}\n");
    toReplaceList.add("export AMS_HBASE_FIFO_COMPACTION_ENABLED={{ams_hbase_fifo_compaction_enabled}}\n");

    //Because of AMBARI-15331 : AMS HBase FIFO compaction policy and Normalizer settings are not handled correctly
    toReplaceList.add("export HBASE_NORMALIZATION_ENABLED={{ams_hbase_normalizer_enabled}}\n");
    toReplaceList.add("export HBASE_FIFO_COMPACTION_POLICY_ENABLED={{ams_hbase_fifo_compaction_policy_enabled}}\n");


    for (String toReplace : toReplaceList) {
      if (content.contains(toReplace)) {
        content = content.replace(toReplace, StringUtils.EMPTY);
      }
    }

    return content;
  }

  protected void updateHiveLlapConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();

          if (installedServices.contains("HIVE")) {
            Config hiveSite = cluster.getDesiredConfigByType("hive-interactive-site");
            if (hiveSite != null) {
              Map<String, String> hiveSiteProperties = hiveSite.getProperties();
              String schedulerDelay = hiveSiteProperties.get("hive.llap.task.scheduler.locality.delay");
              if (schedulerDelay != null) {
                // Property exists. Change to new default if set to -1.
                if (schedulerDelay.length() != 0) {
                  try {
                    int schedulerDelayInt = Integer.parseInt(schedulerDelay);
                    if (schedulerDelayInt == -1) {
                      // Old default. Set to new default.
                      updateConfigurationProperties("hive-interactive-site", Collections
                              .singletonMap("hive.llap.task.scheduler.locality.delay", "8000"), true,
                          false);
                    }
                  } catch (NumberFormatException e) {
                    // Invalid existing value. Set to new default.
                    updateConfigurationProperties("hive-interactive-site", Collections
                            .singletonMap("hive.llap.task.scheduler.locality.delay", "8000"), true,
                        false);
                  }
                }
              }
              updateConfigurationProperties("hive-interactive-site",
                  Collections.singletonMap("hive.mapjoin.hybridgrace.hashtable", "true"), true,
                  false);
              updateConfigurationProperties("tez-interactive-site",
                  Collections.singletonMap("tez.session.am.dag.submit.timeout.secs", "1209600"), true,
                  false);
              // Explicitly skipping hive.llap.allow.permanent.fns during upgrades, since it's related to security,
              // and we don't know if the value is set by the user or as a result of the previous default.
            }
          }
        }
      }
    }
  }


  protected void updateHadoopEnvConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
        AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      Map<String, String> prop = new HashMap<String, String>();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          /***
           * Append "ulimit -l" from hadoop-env.sh
           */
          String content = null;
          if (cluster.getDesiredConfigByType(HADOOP_ENV) != null) {
            content = cluster.getDesiredConfigByType(HADOOP_ENV).getProperties().get("content");
          }

          if (content != null && !content.contains("ulimit")) {
            content += "\n" +
                "{% if is_datanode_max_locked_memory_set %}\n" +
                "# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n" +
                "# Makes sense to fix only when runing DN as root \n" +
                "if [ \"$command\" == \"datanode\" ] &amp;&amp; [ \"$EUID\" -eq 0 ] &amp;&amp; [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n" +
                "  ulimit -l {{datanode_max_locked_memory}}\n" +
                "fi\n" +
                "{% endif %}";

            prop.put("content", content);
            updateConfigurationPropertiesForCluster(cluster, "hadoop-env",
                prop, true, false);
          }
        }
      }
    }
  }

  /**
   * Creates the servicecomponent_version table
   *
   * @throws SQLException
   */
  private void createComponentVersionTable() throws SQLException {

    List<DBColumnInfo> columns = new ArrayList<>();

    // Add extension link table
    LOG.info("Creating {} table", COMPONENT_VERSION_TABLE);

    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("component_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("repo_version_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("state", String.class, 32, null, false));
    columns.add(new DBColumnInfo("user_name", String.class, 255, null, false));
    dbAccessor.createTable(COMPONENT_VERSION_TABLE, columns, (String[]) null);

    dbAccessor.addPKConstraint(COMPONENT_VERSION_TABLE, COMPONENT_VERSION_PK, "id");

    dbAccessor.addFKConstraint(COMPONENT_VERSION_TABLE, COMPONENT_VERSION_FK_COMPONENT, "component_id",
      COMPONENT_TABLE, "id", false);

    dbAccessor.addFKConstraint(COMPONENT_VERSION_TABLE, COMPONENT_VERSION_FK_REPO_VERSION, "repo_version_id",
      "repo_version", "repo_version_id", false);

    addSequence("servicecomponent_version_id_seq", 0L, false);
  }

  protected void updateKafkaConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {

          Config kafkaBrokerConfig = cluster.getDesiredConfigByType(KAFKA_BROKER);
          if (kafkaBrokerConfig != null) {
            Map<String, String> kafkaBrokerProperties = kafkaBrokerConfig.getProperties();

            if (kafkaBrokerProperties != null && kafkaBrokerProperties.containsKey(KAFKA_TIMELINE_METRICS_HOST)) {
              LOG.info("Removing kafka.timeline.metrics.host from kafka-broker");
              removeConfigurationPropertiesFromCluster(cluster, KAFKA_BROKER, Collections.singleton("kafka.timeline.metrics.host"));
            }
          }
        }
      }
    }
  }

  /**
   * Alter servicedesiredstate table.
   * @throws SQLException
   */
  private void updateServiceDesiredStateTable() throws SQLException {
    // ALTER TABLE servicedesiredstate ADD COLUMN
    // credential_store_supported SMALLINT DEFAULT 0 NOT NULL
    // credential_store_enabled SMALLINT DEFAULT 0 NOT NULL
    dbAccessor.addColumn(SERVICE_DESIRED_STATE_TABLE,
      new DBColumnInfo(CREDENTIAL_STORE_SUPPORTED_COL, Short.class, null, 0, false));

    dbAccessor.addColumn(SERVICE_DESIRED_STATE_TABLE,
      new DBColumnInfo(CREDENTIAL_STORE_ENABLED_COL, Short.class, null, 0, false));
  }


  /**
   * Removes the compound PK from hostcomponentdesiredstate table
   * and replaces it with a surrogate PK, but only if the table doesn't have it's new PK set.
   * Create index and unqiue constraint on the columns that originally formed the compound PK.
   *
   * @throws SQLException
   */
  private void updateHostComponentDesiredStateTable() throws SQLException {
    if (dbAccessor.tableHasPrimaryKey(HOST_COMPONENT_DESIREDSTATE_TABLE, HOST_COMPONENT_DESIREDSTATE_ID_COL)) {
      LOG.info("Skipping {} table Primary Key modifications since the new {} column already exists",
        HOST_COMPONENT_DESIREDSTATE_TABLE, HOST_COMPONENT_DESIREDSTATE_ID_COL);

      return;
    }
    // add the new ID column as nullable until we populate
    dbAccessor.addColumn(HOST_COMPONENT_DESIREDSTATE_TABLE,
      new DBColumnInfo(HOST_COMPONENT_DESIREDSTATE_ID_COL, Long.class, null, null, true));

    // insert sequence values
    AtomicLong id = new AtomicLong(1);
    Statement statement = null;
    ResultSet resultSet = null;

    try {
      statement = dbAccessor.getConnection().createStatement();

      if (statement != null) {
        // Select records by old PK
        String selectSQL = String.format(
          "SELECT cluster_id, component_name, host_id, service_name FROM %s", HOST_COMPONENT_DESIREDSTATE_TABLE);

        resultSet = statement.executeQuery(selectSQL);

        while (resultSet.next()) {
          final Long clusterId = resultSet.getLong("cluster_id");
          final String componentName = resultSet.getString("component_name");
          final Long hostId = resultSet.getLong("host_id");
          final String serviceName = resultSet.getString("service_name");

          String updateSQL = String.format(
            "UPDATE %s SET %s = %s WHERE cluster_id = %d AND component_name = '%s' AND service_name = '%s' AND host_id = %d",
            HOST_COMPONENT_DESIREDSTATE_TABLE, HOST_COMPONENT_DESIREDSTATE_ID_COL, id.getAndIncrement(),
            clusterId, componentName, serviceName, hostId);

          dbAccessor.executeQuery(updateSQL);
        }

        // Add sequence for hostcomponentdesiredstate table ids
        addSequence("hostcomponentdesiredstate_id_seq", id.get(), false);
      }

    }
    finally {
      JdbcUtils.closeResultSet(resultSet);
      JdbcUtils.closeStatement(statement);
    }

    // make the ID column NON NULL now
    dbAccessor.alterColumn(HOST_COMPONENT_DESIREDSTATE_TABLE,
      new DBColumnInfo(HOST_COMPONENT_DESIREDSTATE_ID_COL, Long.class, null, null, false));

    // drop existing PK and create new one on ID column
    String primaryKeyConstraintName = null;
    Configuration.DatabaseType databaseType = configuration.getDatabaseType();

    switch (databaseType) {
      case POSTGRES:
      case MYSQL:
      case ORACLE:
      case SQL_SERVER:
        primaryKeyConstraintName = dbAccessor.getPrimaryKeyConstraintName(HOST_COMPONENT_DESIREDSTATE_TABLE);
        break;

      default:
        throw new UnsupportedOperationException(String.format("Invalid database type '%s'", databaseType));

    }

    // warn if we can't find it
    if (null == primaryKeyConstraintName) {
      LOG.warn("Unable to determine the primary key constraint name for {}", HOST_COMPONENT_DESIREDSTATE_TABLE);
    }
    else {
      dbAccessor.dropPKConstraint(HOST_COMPONENT_DESIREDSTATE_TABLE, primaryKeyConstraintName, true);
    }

    // create a new PK, matching the name of the constraint found in the SQL files
    dbAccessor.addPKConstraint(HOST_COMPONENT_DESIREDSTATE_TABLE, "PK_hostcomponentdesiredstate", "id");

    // create index, ensuring column order matches that of the SQL files
    dbAccessor.addUniqueConstraint(HOST_COMPONENT_DESIREDSTATE_TABLE, HOST_COMPONENT_DESIREDSTATE_INDEX,
      "component_name", "service_name", "host_id", "cluster_id");
  }

  protected void updateAtlasConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          updateAtlasHookConfig(cluster, "HIVE", "hive-env", "hive.atlas.hook");
          updateAtlasHookConfig(cluster, "STORM", "storm-env", "storm.atlas.hook");
          updateAtlasHookConfig(cluster, "FALCON", "falcon-env", "falcon.atlas.hook");
          updateAtlasHookConfig(cluster, "SQOOP", "sqoop-env", "sqoop.atlas.hook");
        }
      }
    }
  }

  protected void updateAtlasHookConfig(Cluster cluster, String serviceName, String configType, String propertyName) throws AmbariException {
    Set<String> installedServices = cluster.getServices().keySet();
    if (installedServices.contains("ATLAS") && installedServices.contains(serviceName)) {
      Config configEnv = cluster.getDesiredConfigByType(configType);
      if (configEnv != null) {
        Map<String, String> newProperties = new HashMap<>();
        newProperties.put(propertyName, "true");
        boolean updateProperty = configEnv.getProperties().containsKey(propertyName);
        updateConfigurationPropertiesForCluster(cluster, configType, newProperties, updateProperty, true);
      }
    }
  }

  /**
   * Updates Hive Interactive's config in hive-interactive-site.
   *
   * @throws AmbariException
   */
  protected void updateHIVEInteractiveConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Config hiveInteractiveSite = cluster.getDesiredConfigByType("hive-interactive-site");
          if (hiveInteractiveSite != null) {
            updateConfigurationProperties("hive-interactive-site", Collections.singletonMap("hive.tez.container.size",
                "SET_ON_FIRST_INVOCATION"), true, true);

            updateConfigurationProperties("hive-interactive-site", Collections.singletonMap("hive.auto.convert.join.noconditionaltask.size",
                "1000000000"), true, true);
          }
        }
      }
    }
  }

  /**
   * Updates Tez for Hive2 Interactive's config in tez-interactive-site.
   *
   * @throws AmbariException
   */
  protected void updateTEZInteractiveConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Config tezInteractiveSite = cluster.getDesiredConfigByType("tez-interactive-site");
          if (tezInteractiveSite != null) {

            updateConfigurationProperties("tez-interactive-site", Collections.singletonMap("tez.runtime.io.sort.mb", "512"), true, true);

            updateConfigurationProperties("tez-interactive-site", Collections.singletonMap("tez.runtime.unordered.output.buffer.size-mb",
                "100"), true, true);
          }
        }
      }
    }
  }

  /**
   * Add permissions for managing service auto-start.
   * <p>
   * <ul>
   * <li>SERVICE.MANAGE_AUTO_START permissions for SERVICE.ADMINISTRATOR, CLUSTER.OPERATOR, CLUSTER.ADMINISTRATOR, AMBARI.ADMINISTRATOR</li>
   * <li>CLUSTER.MANAGE_AUTO_START permissions for CLUSTER.OPERATOR, CLUSTER.ADMINISTRATOR, AMBARI.ADMINISTRATOR</li>
   * </ul>
   */
  protected void addManageServiceAutoStartPermissions() throws SQLException {
    Collection<String> roles;

    // Add service-level auto-start permission
    roles = Arrays.asList(
        "AMBARI.ADMINISTRATOR:AMBARI",
        "CLUSTER.ADMINISTRATOR:CLUSTER",
        "CLUSTER.OPERATOR:CLUSTER",
        "SERVICE.ADMINISTRATOR:CLUSTER");
    addRoleAuthorization("SERVICE.MANAGE_AUTO_START", "Manage service auto-start", roles);

    // Add cluster-level auto start-permission
    roles = Arrays.asList(
        "AMBARI.ADMINISTRATOR:AMBARI",
        "CLUSTER.ADMINISTRATOR:CLUSTER",
        "CLUSTER.OPERATOR:CLUSTER");
    addRoleAuthorization("CLUSTER.MANAGE_AUTO_START", "Manage service auto-start configuration", roles);
  }
}
