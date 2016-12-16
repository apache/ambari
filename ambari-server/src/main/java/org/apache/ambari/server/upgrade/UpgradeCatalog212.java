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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * Upgrade catalog for version 2.1.2.
 */
public class UpgradeCatalog212 extends AbstractUpgradeCatalog {
  private static final String HIVE_SITE = "hive-site";
  private static final String HIVE_ENV = "hive-env";
  private static final String HBASE_ENV = "hbase-env";
  private static final String HBASE_SITE = "hbase-site";
  private static final String CLUSTER_ENV = "cluster-env";
  private static final String OOZIE_ENV = "oozie-env";

  private static final String TOPOLOGY_REQUEST_TABLE = "topology_request";
  private static final String CLUSTERS_TABLE = "clusters";
  private static final String CLUSTERS_TABLE_CLUSTER_ID_COLUMN = "cluster_id";
  private static final String TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN = "cluster_name";
  private static final String TOPOLOGY_REQUEST_CLUSTER_ID_COLUMN = "cluster_id";
  private static final String TOPOLOGY_REQUEST_CLUSTER_ID_FK_CONSTRAINT_NAME = "FK_topology_request_cluster_id";

  private static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  private static final String HOST_ROLE_COMMAND_SKIP_COLUMN = "auto_skip_on_failure";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog212.class);

  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog212(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  protected UpgradeCatalog212() {
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.1.2";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.1.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    executeTopologyDDLUpdates();
    executeHostRoleCommandDDLUpdates();
  }

  private void executeTopologyDDLUpdates() throws AmbariException, SQLException {
    dbAccessor.addColumn(TOPOLOGY_REQUEST_TABLE, new DBColumnInfo(TOPOLOGY_REQUEST_CLUSTER_ID_COLUMN,
      Long.class, null, null, true));
    // TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN will be deleted in PreDML. We need a cluster name to set cluster id.
    // dbAccessor.dropColumn(TOPOLOGY_REQUEST_TABLE, TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN);
    // dbAccessor.setColumnNullable(TOPOLOGY_REQUEST_TABLE, TOPOLOGY_REQUEST_CLUSTER_ID_COLUMN, false);
    // dbAccessor.addFKConstraint(TOPOLOGY_REQUEST_TABLE, TOPOLOGY_REQUEST_CLUSTER_ID_FK_CONSTRAINT_NAME,
    //     TOPOLOGY_REQUEST_CLUSTER_ID_COLUMN, CLUSTERS_TABLE, CLUSTERS_TABLE_CLUSTER_ID_COLUMN, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    if (dbAccessor.tableHasColumn(TOPOLOGY_REQUEST_TABLE, TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN)) {
      addClusterIdToTopology();
      finilizeTopologyDDL();
    } else {
      LOG.debug("The column: [ {} ] has already been dropped from table: [ {} ]. Skipping preDMLUpdate logic.",
          TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN, TOPOLOGY_REQUEST_TABLE);
    }
  }

  protected void finilizeTopologyDDL() throws AmbariException, SQLException {
    dbAccessor.dropColumn(TOPOLOGY_REQUEST_TABLE, TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN);
    dbAccessor.setColumnNullable(TOPOLOGY_REQUEST_TABLE, TOPOLOGY_REQUEST_CLUSTER_ID_COLUMN, false);
    dbAccessor.addFKConstraint(TOPOLOGY_REQUEST_TABLE, TOPOLOGY_REQUEST_CLUSTER_ID_FK_CONSTRAINT_NAME,
      TOPOLOGY_REQUEST_CLUSTER_ID_COLUMN, CLUSTERS_TABLE, CLUSTERS_TABLE_CLUSTER_ID_COLUMN, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    addMissingConfigs();
  }

  protected void addClusterIdToTopology() throws AmbariException, SQLException {
    Map<String, Long> clusterNameIdMap = new HashMap<String, Long>();
    try (Statement statement = dbAccessor.getConnection().createStatement();
         ResultSet rs = statement.executeQuery("SELECT DISTINCT cluster_name, cluster_id FROM clusters");
    ) {
      while (rs.next()) {
        long clusterId = rs.getLong("cluster_id");
        String clusterName = rs.getString("cluster_name");
        clusterNameIdMap.put(clusterName, clusterId);
      }
    }

    for (String clusterName : clusterNameIdMap.keySet()) {
      try (PreparedStatement preparedStatement = dbAccessor.getConnection().prepareStatement("UPDATE topology_request " +
          "SET cluster_id=? WHERE cluster_name=?");
      ) {
        preparedStatement.setLong(1, clusterNameIdMap.get(clusterName));
        preparedStatement.setString(2, clusterName);
        preparedStatement.executeUpdate();
      }
    }

    // Set cluster id for all null values.
    // Useful if cluster was renamed and cluster name does not match.
    if (clusterNameIdMap.entrySet().size() >= 1) {
      try (PreparedStatement preparedStatement = dbAccessor.getConnection().prepareStatement("UPDATE topology_request " +
          "SET cluster_id=? WHERE cluster_id IS NULL");
      ) {
        preparedStatement.setLong(1, clusterNameIdMap.entrySet().iterator().next().getValue());
        preparedStatement.executeUpdate();
      }
    }
    if (clusterNameIdMap.entrySet().size() == 0) {
      LOG.warn("Cluster not found. topology_request.cluster_id is not set");
    }
    if (clusterNameIdMap.entrySet().size() > 1) {
      LOG.warn("Found more than one cluster. topology_request.cluster_id can be incorrect if you have renamed the cluster.");
    }
  }

  protected void addMissingConfigs() throws AmbariException {
    updateHiveConfigs();
    updateOozieConfigs();
    updateHbaseAndClusterConfigurations();
    updateKafkaConfigurations();
    updateStormConfigs();
    removeDataDirMountConfig();
  }

  protected void updateStormConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if ((clusterMap != null) && !clusterMap.isEmpty()) {
        // Iterate through the clusters and perform any configuration updates
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> removes = new HashSet<String>();
          removes.add("topology.metrics.consumer.register");
          updateConfigurationPropertiesForCluster(cluster, "storm-site",
            new HashMap<String, String>(), removes, false, false);
        }
      }
    }
  }

  protected void updateKafkaConfigurations() throws AmbariException {
    Map<String, String> properties = new HashMap<>();
    properties.put("external.kafka.metrics.exclude.prefix",
      "kafka.network.RequestMetrics,kafka.server.DelayedOperationPurgatory," +
        "kafka.server.BrokerTopicMetrics.BytesRejectedPerSec");
    properties.put("external.kafka.metrics.include.prefix",
      "kafka.network.RequestMetrics.ResponseQueueTimeMs.request.OffsetCommit.98percentile," +
        "kafka.network.RequestMetrics.ResponseQueueTimeMs.request.Offsets.95percentile," +
        "kafka.network.RequestMetrics.ResponseSendTimeMs.request.Fetch.95percentile," +
        "kafka.network.RequestMetrics.RequestsPerSec.request");

    updateConfigurationProperties("kafka-broker", properties, false, false);
  }

  protected void updateHbaseAndClusterConfigurations() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if ((clusterMap != null) && !clusterMap.isEmpty()) {
        // Iterate through the clusters and perform any configuration updates
        for (final Cluster cluster : clusterMap.values()) {
          Config hbaseEnvProps = cluster.getDesiredConfigByType(HBASE_ENV);
          Config hbaseSiteProps = cluster.getDesiredConfigByType(HBASE_SITE);

          if (hbaseEnvProps != null) {
            // Remove override_hbase_uid from hbase-env and add override_uid to cluster-env
            String value = hbaseEnvProps.getProperties().get("override_hbase_uid");
            if (value != null) {
              Map<String, String> updates = new HashMap<String, String>();
              Set<String> removes = new HashSet<String>();
              updates.put("override_uid", value);
              removes.add("override_hbase_uid");
              updateConfigurationPropertiesForCluster(cluster, HBASE_ENV, new HashMap<String, String>(), removes, false, true);
              updateConfigurationPropertiesForCluster(cluster, CLUSTER_ENV, updates, true, false);
            }
          }

          if (hbaseSiteProps != null) {
            String value = hbaseSiteProps.getProperties().get("hbase.bucketcache.size");
            if (value != null) {
              if (value.endsWith("m")) {
                value = value.substring(0, value.length() - 1);
                Map<String, String> updates = new HashMap<String, String>();
                updates.put("hbase.bucketcache.size", value);
                updateConfigurationPropertiesForCluster(cluster, HBASE_SITE, updates, true, false);
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
          Boolean isHiveSitePresent = cluster.getDesiredConfigByType(HIVE_SITE) != null;
          StackId stackId = cluster.getCurrentStackVersion();
          Boolean isStackNotLess22 = (stackId != null && stackId.getStackName().equals("HDP") &&
                  VersionUtils.compareVersions(stackId.getStackVersion(), "2.2") >= 0);

          if (cluster.getDesiredConfigByType(HIVE_ENV) != null && isStackNotLess22) {
            Map<String, String> hiveEnvProps = new HashMap<String, String>();
            content = cluster.getDesiredConfigByType(HIVE_ENV).getProperties().get("content");
            if(content != null) {
              content = updateHiveEnvContent(content);
              hiveEnvProps.put("content", content);
            }
            updateConfigurationPropertiesForCluster(cluster, HIVE_ENV, hiveEnvProps, true, true);
          }

          if (isHiveSitePresent && isStackNotLess22) {
            Set<String> hiveSiteRemoveProps = new HashSet<String>();
            hiveSiteRemoveProps.add("hive.heapsize");
            hiveSiteRemoveProps.add("hive.optimize.mapjoin.mapreduce");
            hiveSiteRemoveProps.add("hive.server2.enable.impersonation");
            hiveSiteRemoveProps.add("hive.auto.convert.sortmerge.join.noconditionaltask");

            updateConfigurationPropertiesForCluster(cluster, HIVE_SITE, new HashMap<String, String>(), hiveSiteRemoveProps, false, true);
          }
        }
      }
    }
  }

  protected void updateOozieConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Config oozieEnv = cluster.getDesiredConfigByType(OOZIE_ENV);
          if (oozieEnv != null) {
            Map<String, String> oozieEnvProperties = oozieEnv.getProperties();

            String hostname = oozieEnvProperties.get("oozie_hostname");
            String db_type = oozieEnvProperties.get("oozie_database");
            String final_db_host = null;
            // fix for empty hostname after 1.7 -> 2.1.x+ upgrade
            if (hostname != null && db_type != null && hostname.equals("")) {
              switch (db_type.toUpperCase()) {
                case "EXISTING MYSQL DATABASE":
                  final_db_host = oozieEnvProperties.get("oozie_existing_mysql_host");
                  break;
                case "EXISTING POSTGRESQL DATABASE":
                  final_db_host = oozieEnvProperties.get("oozie_existing_postgresql_host");
                  break;
                case "EXISTING ORACLE DATABASE":
                  final_db_host = oozieEnvProperties.get("oozie_existing_oracle_host");
                  break;
                default:
                  final_db_host = null;
                  break;
              }
              if (final_db_host != null) {
                Map<String, String> newProperties = new HashMap<>();
                newProperties.put("oozie_hostname", final_db_host);
                updateConfigurationPropertiesForCluster(cluster, OOZIE_ENV, newProperties, true, true);
              }
            }
          }
        }
      }
    }
  }

  protected String updateHiveEnvContent(String hiveEnvContent) {
    if(hiveEnvContent == null) {
      return null;
    }
    String oldHeapSizeRegex = "export HADOOP_HEAPSIZE=\"\\{\\{hive_heapsize\\}\\}\"\\s*\\n" +
            "export HADOOP_CLIENT_OPTS=\"-Xmx\\$\\{HADOOP_HEAPSIZE\\}m \\$HADOOP_CLIENT_OPTS\"";
    String newAuxJarPath = "";
    return hiveEnvContent.replaceAll(oldHeapSizeRegex, Matcher.quoteReplacement(newAuxJarPath));
  }

  /**
   * DDL changes for {@link #HOST_ROLE_COMMAND_TABLE}.
   *
   * @throws AmbariException
   * @throws SQLException
   */
  private void executeHostRoleCommandDDLUpdates() throws AmbariException, SQLException {
    dbAccessor.addColumn(HOST_ROLE_COMMAND_TABLE,
        new DBColumnInfo(HOST_ROLE_COMMAND_SKIP_COLUMN, Integer.class, 1, 0, false));
  }

  protected void removeDataDirMountConfig() throws AmbariException {
    Set<String> properties = new HashSet<>();
    properties.add("dfs.datanode.data.dir.mount.file");

    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          removeConfigurationPropertiesFromCluster(cluster, "hadoop-env", properties);
        }
      }
    }
  }
}
