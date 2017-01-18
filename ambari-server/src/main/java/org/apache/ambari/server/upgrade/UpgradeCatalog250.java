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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 2.5.0.
 */
public class UpgradeCatalog250 extends AbstractUpgradeCatalog {

  protected static final String HOST_VERSION_TABLE = "host_version";
  protected static final String GROUPS_TABLE = "groups";
  protected static final String GROUP_TYPE_COL = "group_type";
  private static final String AMS_ENV = "ams-env";
  private static final String AMS_SITE = "ams-site";
  private static final String AMS_LOG4J = "ams-log4j";
  private static final String AMS_HBASE_LOG4J = "ams-hbase-log4j";
  private static final String AMS_MODE = "timeline.metrics.service.operation.mode";
  private static final String AMS_HBASE_SITE = "ams-hbase-site";
  private static final String HBASE_ROOTDIR = "hbase.rootdir";
  private static final String HADOOP_ENV = "hadoop-env";
  private static final String KAFKA_BROKER = "kafka-broker";
  private static final String YARN_SITE_CONFIG = "yarn-site";
  private static final String YARN_ENV_CONFIG = "yarn-env";
  private static final String YARN_LCE_CGROUPS_MOUNT_PATH = "yarn.nodemanager.linux-container-executor.cgroups.mount-path";
  private static final String YARN_CGROUPS_ENABLED = "yarn_cgroups_enabled";
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
    updateLogSearchConfigs();
    updateAmbariInfraConfigs();
    updateYarnSite();
    updateRangerUrlConfigs();
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

  /**
   * Updates {@code yarn-site} in the following ways:
   *
   * Remove {@code YARN_LCE_CGROUPS_MOUNT_PATH} if  {@code YARN_CGROUPS_ENABLED} is {@code false} and
   * {@code YARN_LCE_CGROUPS_MOUNT_PATH} is empty string
   *
   * @throws AmbariException
   */
  protected void updateYarnSite() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);

    for (final Cluster cluster : clusterMap.values()) {
      Config yarnEnvConfig = cluster.getDesiredConfigByType(YARN_ENV_CONFIG);
      Config yarnSiteConfig = cluster.getDesiredConfigByType(YARN_SITE_CONFIG);

      if (yarnEnvConfig != null && yarnSiteConfig != null) {
        String cgroupEnabled = yarnEnvConfig.getProperties().get(YARN_CGROUPS_ENABLED);
        String mountPath = yarnSiteConfig.getProperties().get(YARN_LCE_CGROUPS_MOUNT_PATH);

        if (StringUtils.isEmpty(mountPath) && cgroupEnabled != null
          && cgroupEnabled.trim().equalsIgnoreCase("false")){

          removeConfigurationPropertiesFromCluster(cluster, YARN_SITE_CONFIG, new HashSet<String>(){{
            add(YARN_LCE_CGROUPS_MOUNT_PATH);
          }});

        }
      }

    }
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


          boolean isDistributed = false;
          Config amsSite = cluster.getDesiredConfigByType(AMS_SITE);
          if (amsSite != null) {
            if ("distributed".equals(amsSite.getProperties().get(AMS_MODE))) {
              isDistributed = true;
            }
          }

          if (isDistributed) {
            Config amsHbaseSite = cluster.getDesiredConfigByType(AMS_HBASE_SITE);
            if (amsHbaseSite != null) {
              Map<String, String> amsHbaseSiteProperties = amsHbaseSite.getProperties();
              String rootDir = amsHbaseSiteProperties.get(HBASE_ROOTDIR);
              if (StringUtils.isNotEmpty(rootDir) && rootDir.startsWith("hdfs://")) {
                int indexOfSlash = rootDir.indexOf("/", 7);
                Map<String, String> newProperties = new HashMap<>();
                String newRootdir = rootDir.substring(indexOfSlash);
                newProperties.put(HBASE_ROOTDIR, newRootdir);
                LOG.info("Changing ams-hbase-site rootdir to " + newRootdir);
                updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_SITE, newProperties, true, true);
              }
            }
          }

          //Update AMS log4j to make rolling properties configurable as separate fields.
          Config amsLog4jProperties = cluster.getDesiredConfigByType(AMS_LOG4J);
          if(amsLog4jProperties != null){
            Map<String, String> newProperties = new HashMap<>();

            String content = amsLog4jProperties.getProperties().get("content");
            content = SchemaUpgradeUtil.extractProperty(content,"ams_log_max_backup_size","ams_log_max_backup_size","log4j.appender.file.MaxFileSize=(\\w+)MB","80",newProperties);
            content = SchemaUpgradeUtil.extractProperty(content,"ams_log_number_of_backup_files","ams_log_number_of_backup_files","log4j.appender.file.MaxBackupIndex=(\\w+)","60",newProperties);
            newProperties.put("content",content);
            updateConfigurationPropertiesForCluster(cluster,AMS_LOG4J,newProperties,true,true);
          }

          Config amsHbaseLog4jProperties = cluster.getDesiredConfigByType(AMS_HBASE_LOG4J);
          if(amsHbaseLog4jProperties != null){
            Map<String, String> newProperties = new HashMap<>();

            String content = amsHbaseLog4jProperties.getProperties().get("content");
            content = SchemaUpgradeUtil.extractProperty(content,"ams_hbase_log_maxfilesize","ams_hbase_log_maxfilesize","hbase.log.maxfilesize=(\\w+)MB","256",newProperties);
            content = SchemaUpgradeUtil.extractProperty(content,"ams_hbase_log_maxbackupindex","ams_hbase_log_maxbackupindex","hbase.log.maxbackupindex=(\\w+)","20",newProperties);
            content = SchemaUpgradeUtil.extractProperty(content,"ams_hbase_security_log_maxfilesize","ams_hbase_security_log_maxfilesize","hbase.security.log.maxfilesize=(\\w+)MB","256",newProperties);
            content = SchemaUpgradeUtil.extractProperty(content,"ams_hbase_security_log_maxbackupindex","ams_hbase_security_log_maxbackupindex","hbase.security.log.maxbackupindex=(\\w+)","20",newProperties);
            newProperties.put("content",content);
            updateConfigurationPropertiesForCluster(cluster,AMS_HBASE_LOG4J,newProperties,true,true);
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
            updateConfigurationProperties("hive-interactive-site",
                Collections.singletonMap("hive.llap.execution.mode", "only"),
                true, true);
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
   * Updates Log Search configs.
   *
   * @throws AmbariException
   */
  protected void updateLogSearchConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Config logSearchProperties = cluster.getDesiredConfigByType("logsearch-properties");
          if (logSearchProperties != null) {
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("logsearch.auth.external_auth.enabled", logSearchProperties.getProperties().get("logsearch.external.auth.enabled"));
            newProperties.put("logsearch.auth.external_auth.host_url", logSearchProperties.getProperties().get("logsearch.external.auth.host_url"));
            newProperties.put("logsearch.auth.external_auth.login_url", logSearchProperties.getProperties().get("logsearch.external.auth.login_url"));
            
            Set<String> removeProperties = new HashSet<>();
            removeProperties.add("logsearch.external.auth.enabled");
            removeProperties.add("logsearch.external.auth.host_url");
            removeProperties.add("logsearch.external.auth.login_url");
            
            updateConfigurationPropertiesForCluster(cluster, "logsearch-properties", newProperties, removeProperties, true, true);
          }
          
          Config logfeederEnvProperties = cluster.getDesiredConfigByType("logfeeder-env");
          if (logfeederEnvProperties != null) {
            String content = logfeederEnvProperties.getProperties().get("content");
            if (content.contains("infra_solr_ssl_enabled")) {
              content = content.replace("infra_solr_ssl_enabled", "logfeeder_use_ssl");
              updateConfigurationPropertiesForCluster(cluster, "logfeeder-env", Collections.singletonMap("content", content), true, true);
            }
          }
          
          Config logsearchEnvProperties = cluster.getDesiredConfigByType("logsearch-env");
          if (logsearchEnvProperties != null) {
            Map<String, String> newProperties = new HashMap<>();
            String content = logsearchEnvProperties.getProperties().get("content");
            if (content.contains("infra_solr_ssl_enabled or logsearch_ui_protocol == 'https'")) {
              content = content.replace("infra_solr_ssl_enabled or logsearch_ui_protocol == 'https'", "logsearch_use_ssl");
            }
            if (!content.equals(logsearchEnvProperties.getProperties().get("content"))) {
              newProperties.put("content", content);
            }
            
            Set<String> removeProperties = new HashSet<>();
            removeProperties.add("logsearch_solr_audit_logs_use_ranger");
            removeProperties.add("logsearch_solr_audit_logs_zk_node");
            removeProperties.add("logsearch_solr_audit_logs_zk_quorum");
            
            updateConfigurationPropertiesForCluster(cluster, "logsearch-env", newProperties, removeProperties, true, true);
          }
          
          Config logfeederLog4jProperties = cluster.getDesiredConfigByType("logfeeder-log4j");
          if (logfeederLog4jProperties != null) {
            Map<String, String> newProperties = new HashMap<>();
            
            String content = logfeederLog4jProperties.getProperties().get("content");
            content = SchemaUpgradeUtil.extractProperty(content, "logfeeder_log_maxfilesize", "logfeeder_log_maxfilesize",
                "    <param name=\"file\" value=\"\\{\\{logfeeder_log_dir}}/logfeeder.log\"/>\n" +
                "    <param name=\"append\" value=\"true\"/>\n" +
                "    <param name=\"maxFileSize\" value=\"(\\w+)MB\"/>", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logfeeder_log_maxbackupindex", "logfeeder_log_maxbackupindex",
                "    <param name=\"file\" value=\"\\{\\{logfeeder_log_dir}}/logfeeder.log\"/>\n" +
                "    <param name=\"append\" value=\"true\"/>\n" +
                "    <param name=\"maxFileSize\" value=\"\\{\\{logfeeder_log_maxfilesize}}MB\"/>\n" +
                "    <param name=\"maxBackupIndex\" value=\"(\\w+)\"/>", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logfeeder_json_log_maxfilesize", "logfeeder_json_log_maxfilesize",
                "    <param name=\"file\" value=\"\\{\\{logfeeder_log_dir}}/logsearch-logfeeder.json\" />\n" +
                "    <param name=\"append\" value=\"true\" />\n" +
                "    <param name=\"maxFileSize\" value=\"(\\w+)MB\" />", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logfeeder_json_log_maxbackupindex", "logfeeder_json_log_maxbackupindex",
                "    <param name=\"file\" value=\"\\{\\{logfeeder_log_dir}}/logsearch-logfeeder.json\" />\n" +
                "    <param name=\"append\" value=\"true\" />\n" +
                "    <param name=\"maxFileSize\" value=\"\\{\\{logfeeder_json_log_maxfilesize}}MB\" />\n" +
                "    <param name=\"maxBackupIndex\" value=\"(\\w+)\" />", "10", newProperties);
            
            newProperties.put("content", content);
            updateConfigurationPropertiesForCluster(cluster, "logfeeder-log4j", newProperties, true, true);
          }
          
          Config logsearchLog4jProperties = cluster.getDesiredConfigByType("logsearch-log4j");
          if (logsearchLog4jProperties != null) {
            Map<String, String> newProperties = new HashMap<>();

            String content = logsearchLog4jProperties.getProperties().get("content");
            if (content.contains("{{logsearch_log_dir}}/logsearch.err")) {
              content = content.replace("{{logsearch_log_dir}}/logsearch.err", "{{logsearch_log_dir}}/logsearch.log");
            }
            if (content.contains("<priority value=\"warn\"/>")) {
              content = content.replace("<priority value=\"warn\"/>", "<priority value=\"info\"/>");
            }
            
            
            content = SchemaUpgradeUtil.extractProperty(content, "logsearch_log_maxfilesize", "logsearch_log_maxfilesize",
                "    <param name=\"file\" value=\"\\{\\{logsearch_log_dir}}/logsearch.log\" />\n" +
                "    <param name=\"Threshold\" value=\"info\" />\n" +
                "    <param name=\"append\" value=\"true\" />\n" +
                "    <param name=\"maxFileSize\" value=\"(\\w+)MB\" />\n", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logsearch_log_maxbackupindex", "logsearch_log_maxbackupindex",
                "    <param name=\"file\" value=\"\\{\\{logsearch_log_dir}}/logsearch.log\" />\n" +
                "    <param name=\"Threshold\" value=\"info\" />\n" +
                "    <param name=\"append\" value=\"true\" />\n" +
                "    <param name=\"maxFileSize\" value=\"\\{\\{logsearch_log_maxfilesize}}MB\" />\n" +
                "    <param name=\"maxBackupIndex\" value=\"(\\w+)\" />\n", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logsearch_json_log_maxfilesize", "logsearch_json_log_maxfilesize",
                "    <param name=\"file\" value=\"\\{\\{logsearch_log_dir}}/logsearch.json\"/>\n" +
                "    <param name=\"append\" value=\"true\"/>\n" +
                "    <param name=\"maxFileSize\" value=\"(\\w+)MB\"/>\n", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logsearch_json_log_maxbackupindex", "logsearch_json_log_maxbackupindex",
                "    <param name=\"file\" value=\"\\{\\{logsearch_log_dir}}/logsearch.json\"/>\n" +
                "    <param name=\"append\" value=\"true\"/>\n" +
                "    <param name=\"maxFileSize\" value=\"\\{\\{logsearch_json_log_maxfilesize}}MB\"/>\n" +
                "    <param name=\"maxBackupIndex\" value=\"(\\w+)\"/>\n", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logsearch_audit_log_maxfilesize", "logsearch_audit_log_maxfilesize",
                "    <param name=\"file\" value=\"\\{\\{logsearch_log_dir}}/logsearch-audit.json\"/>\n" +
                "    <param name=\"append\" value=\"true\"/>\n" +
                "    <param name=\"maxFileSize\" value=\"(\\w+)MB\"/>\n", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logsearch_audit_log_maxbackupindex", "logsearch_audit_log_maxbackupindex",
                "    <param name=\"file\" value=\"\\{\\{logsearch_log_dir}}/logsearch-audit.json\"/>\n" +
                "    <param name=\"append\" value=\"true\"/>\n" +
                "    <param name=\"maxFileSize\" value=\"\\{\\{logsearch_audit_log_maxfilesize}}MB\"/>\n" +
                "    <param name=\"maxBackupIndex\" value=\"(\\w+)\"/>\n", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logsearch_perf_log_maxfilesize", "logsearch_perf_log_maxfilesize",
                "    <param name=\"file\" value=\"\\{\\{logsearch_log_dir}}/logsearch-performance.json\"/>\n" +
                "    <param name=\"Threshold\" value=\"info\"/>\n" +
                "    <param name=\"append\" value=\"true\"/>\n" +
                "    <param name=\"maxFileSize\" value=\"(\\w+)MB\"/>\n", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "logsearch_perf_log_maxbackupindex", "logsearch_perf_log_maxbackupindex",
                "    <param name=\"file\" value=\"\\{\\{logsearch_log_dir}}/logsearch-performance.json\"/>\n" +
                "    <param name=\"Threshold\" value=\"info\"/>\n" +
                "    <param name=\"append\" value=\"true\"/>\n" +
                "    <param name=\"maxFileSize\" value=\"\\{\\{logsearch_perf_log_maxfilesize}}MB\"/>\n" +
                "    <param name=\"maxBackupIndex\" value=\"(\\w+)\"/>\n", "10", newProperties);
            
            newProperties.put("content", content);
            if (!content.equals(logsearchLog4jProperties.getProperties().get("content"))) {
              updateConfigurationPropertiesForCluster(cluster, "logsearch-log4j", newProperties, true, true);
            }
          }
        }
      }
    }
  }
  
  /**
   * Updates Ambari Infra configs.
   *
   * @throws AmbariException
   */
  protected void updateAmbariInfraConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Config infraSolrEnvProperties = cluster.getDesiredConfigByType("infra-solr-env");
          if (infraSolrEnvProperties != null) {
            String content = infraSolrEnvProperties.getProperties().get("content");
            if (content.contains("SOLR_SSL_TRUST_STORE={{infra_solr_keystore_location}}")) {
              content = content.replace("SOLR_SSL_TRUST_STORE={{infra_solr_keystore_location}}", "SOLR_SSL_TRUST_STORE={{infra_solr_truststore_location}}");
            }
            if (content.contains("SOLR_SSL_TRUST_STORE_PASSWORD={{infra_solr_keystore_password}}")) {
              content = content.replace("SOLR_SSL_TRUST_STORE_PASSWORD={{infra_solr_keystore_password}}", "SOLR_SSL_TRUST_STORE_PASSWORD={{infra_solr_truststore_password}}");
            }
            if (content.contains("SOLR_KERB_NAME_RULES={{infra_solr_kerberos_name_rules}}")) {
              content = content.replace("SOLR_KERB_NAME_RULES={{infra_solr_kerberos_name_rules}}", "SOLR_KERB_NAME_RULES=\"{{infra_solr_kerberos_name_rules}}\"");
            }
            if (content.contains(" -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}")) {
              content = content.replace(" -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}", "");
            }
            if (!content.equals(infraSolrEnvProperties.getProperties().get("content"))) {
              updateConfigurationPropertiesForCluster(cluster, "infra-solr-env", Collections.singletonMap("content", content), true, true);
            }
          }
          
          Config infraSolrLog4jProperties = cluster.getDesiredConfigByType("infra-solr-log4j");
          if (infraSolrLog4jProperties != null) {
            Map<String, String> newProperties = new HashMap<>();
            
            String content = infraSolrLog4jProperties.getProperties().get("content");
            content = SchemaUpgradeUtil.extractProperty(content, "infra_log_maxfilesize", "infra_log_maxfilesize",
                "log4j.appender.file.MaxFileSize=(\\w+)MB", "10", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "infra_log_maxbackupindex", "infra_log_maxbackupindex",
                "log4j.appender.file.MaxBackupIndex=(\\w+)\n", "9", newProperties);
            
            newProperties.put("content", content);
            updateConfigurationPropertiesForCluster(cluster, "infra-solr-log4j", newProperties, true, true);
          }
          
          Config infraSolrClientLog4jProperties = cluster.getDesiredConfigByType("infra-solr-client-log4j");
          if (infraSolrClientLog4jProperties != null) {
            Map<String, String> newProperties = new HashMap<>();
            
            String content = infraSolrClientLog4jProperties.getProperties().get("content");
            if (content.contains("infra_client_log")) {
              content = content.replace("infra_client_log", "solr_client_log");
            }
            
            content = SchemaUpgradeUtil.extractProperty(content, "infra_client_log_maxfilesize", "solr_client_log_maxfilesize",
                "log4j.appender.file.MaxFileSize=(\\w+)MB", "80", newProperties);
            content = SchemaUpgradeUtil.extractProperty(content, "infra_client_log_maxbackupindex", "solr_client_log_maxbackupindex",
                "log4j.appender.file.MaxBackupIndex=(\\w+)\n", "60", newProperties);
            
            newProperties.put("content", content);
            updateConfigurationPropertiesForCluster(cluster, "infra-solr-client-log4j", newProperties, true, true);
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

  /**
   * Updates Ranger admin url for Ranger plugin supported configs.
   *
   * @throws AmbariException
   */
  protected void updateRangerUrlConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {

      Config ranger_admin_properties = cluster.getDesiredConfigByType("admin-properties");
      if(null != ranger_admin_properties) {
        String policyUrl = ranger_admin_properties.getProperties().get("policymgr_external_url");
        if (null != policyUrl) {
          updateRangerUrl(cluster, "ranger-hdfs-security", "ranger.plugin.hdfs.policy.rest.url", policyUrl);
          updateRangerUrl(cluster, "ranger-hive-security", "ranger.plugin.hive.policy.rest.url", policyUrl);
          updateRangerUrl(cluster, "ranger-hbase-security", "ranger.plugin.hbase.policy.rest.url", policyUrl);
          updateRangerUrl(cluster, "ranger-knox-security", "ranger.plugin.knox.policy.rest.url", policyUrl);
          updateRangerUrl(cluster, "ranger-storm-security", "ranger.plugin.storm.policy.rest.url", policyUrl);
          updateRangerUrl(cluster, "ranger-yarn-security", "ranger.plugin.yarn.policy.rest.url", policyUrl);
          updateRangerUrl(cluster, "ranger-kafka-security", "ranger.plugin.kafka.policy.rest.url", policyUrl);
          updateRangerUrl(cluster, "ranger-atlas-security", "ranger.plugin.atlas.policy.rest.url", policyUrl);
          updateRangerUrl(cluster, "ranger-kms-security", "ranger.plugin.kms.policy.rest.url", policyUrl);
        }
      }
    }
  }

  protected void updateRangerUrl(Cluster cluster, String configType, String configProperty, String policyUrl) throws AmbariException {
    Config componentSecurity = cluster.getDesiredConfigByType(configType);
    if(componentSecurity != null && componentSecurity.getProperties().containsKey(configProperty)) {
      Map<String, String> updateProperty = new HashMap<>();
      updateProperty.put(configProperty, policyUrl);
      updateConfigurationPropertiesForCluster(cluster, configType, updateProperty, true, false);
    }
  }
}
