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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 2.1.3.
 */
public class UpgradeCatalog213 extends AbstractUpgradeCatalog {

  private static final String UPGRADE_TABLE = "upgrade";
  private static final String STORM_SITE = "storm-site";
  private static final String HDFS_SITE_CONFIG = "hdfs-site";
  private static final String KAFKA_BROKER = "kafka-broker";
  private static final String AMS_ENV = "ams-env";
  private static final String AMS_HBASE_ENV = "ams-hbase-env";
  private static final String HBASE_ENV_CONFIG = "hbase-env";
  private static final String ZOOKEEPER_LOG4J_CONFIG = "zookeeper-log4j";
  private static final String NIMBS_MONITOR_FREQ_SECS_PROPERTY = "nimbus.monitor.freq.secs";
  private static final String HADOOP_ENV_CONFIG = "hadoop-env";
  private static final String CONTENT_PROPERTY = "content";
  private static final String HADOOP_ENV_CONTENT_TO_APPEND = "\n{% if is_datanode_max_locked_memory_set %}\n" +
                                    "# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n" +
                                    "# Makes sense to fix only when runing DN as root \n" +
                                    "if [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n" +
                                    "  ulimit -l {{datanode_max_locked_memory}}\n" +
                                    "fi\n" +
                                    "{% endif %};\n";

  private static final String DOWNGRADE_ALLOWED_COLUMN = "downgrade_allowed";
  private static final String UPGRADE_SKIP_FAILURE_COLUMN = "skip_failures";
  private static final String UPGRADE_SKIP_SC_FAILURE_COLUMN = "skip_sc_failures";

  private static final String KERBEROS_DESCRIPTOR_TABLE = "kerberos_descriptor";
  private static final String KERBEROS_DESCRIPTOR_NAME_COLUMN = "kerberos_descriptor_name";
  private static final String KERBEROS_DESCRIPTOR_COLUMN = "kerberos_descriptor";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog213.class);

  @Inject
  DaoUtils daoUtils;


  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog213(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.1.3";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.1.2";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    executeUpgradeDDLUpdates();
    addKerberosDescriptorTable();
  }

  protected void executeUpgradeDDLUpdates() throws AmbariException, SQLException {
    updateUpgradesDDL();
  }

  private void addKerberosDescriptorTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo(KERBEROS_DESCRIPTOR_NAME_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(KERBEROS_DESCRIPTOR_COLUMN, char[].class, null, null, false));

    LOG.debug("Creating table [ {} ] with columns [ {} ] and primary key: [ {} ]", KERBEROS_DESCRIPTOR_TABLE, columns, KERBEROS_DESCRIPTOR_NAME_COLUMN);
    dbAccessor.createTable(KERBEROS_DESCRIPTOR_TABLE, columns, KERBEROS_DESCRIPTOR_NAME_COLUMN);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    executeUpgradePreDMLUpdates();
  }

  /**
   * Updates the following columns on the {@value #UPGRADE_TABLE} table to
   * default values:
   * <ul>
   * <li>{value {@link #DOWNGRADE_ALLOWED_COLUMN}}</li>
   * <li>{value {@link #UPGRADE_SKIP_FAILURE_COLUMN}}</li>
   * <li>{value {@link #UPGRADE_SKIP_SC_FAILURE_COLUMN}}</li>
   * </ul>
   *
   * @throws AmbariException
   * @throws SQLException
   */
  protected void executeUpgradePreDMLUpdates() throws AmbariException, SQLException {
    UpgradeDAO upgradeDAO = injector.getInstance(UpgradeDAO.class);
    List<UpgradeEntity> upgrades = upgradeDAO.findAll();
    for (UpgradeEntity upgrade: upgrades){
      if (upgrade.isDowngradeAllowed() == null) {
        upgrade.setDowngradeAllowed(true);
        upgradeDAO.merge(upgrade);
      }

      // ensure that these are set to false for existing upgrades
      upgrade.setAutoSkipComponentFailures(false);
      upgrade.setAutoSkipServiceCheckFailures(false);

      LOG.info(String.format("Updated upgrade id %s, upgrade pack %s from version %s to %s",
          upgrade.getId(), upgrade.getUpgradePackage(), upgrade.getFromVersion(),
          upgrade.getToVersion()));
    }

    // make the columns nullable now that they have defaults
    dbAccessor.setColumnNullable(UPGRADE_TABLE, DOWNGRADE_ALLOWED_COLUMN, false);
    dbAccessor.setColumnNullable(UPGRADE_TABLE, UPGRADE_SKIP_FAILURE_COLUMN, false);
    dbAccessor.setColumnNullable(UPGRADE_TABLE, UPGRADE_SKIP_SC_FAILURE_COLUMN, false);
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    updateAlertDefinitions();
    updateStormConfigs();
    updateAMSConfigs();
    updateHDFSConfigs();
    updateHbaseEnvConfig();
    updateHadoopEnv();
    updateKafkaConfigs();
    updateZookeeperLog4j();
  }

  /**
   * Adds the following columns to the {@value #UPGRADE_TABLE} table:
   * <ul>
   * <li>{@value #DOWNGRADE_ALLOWED_COLUMN}</li>
   * <li>{@value #UPGRADE_SKIP_FAILURE_COLUMN}</li>
   * <li>{@value #UPGRADE_SKIP_SC_FAILURE_COLUMN}</li>
   * </ul>
   *
   * @throws SQLException
   */
  protected void updateUpgradesDDL() throws SQLException{
    dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(DOWNGRADE_ALLOWED_COLUMN, Short.class, 1, null, true));
    dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_SKIP_FAILURE_COLUMN, Short.class, 1, null, true));
    dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_SKIP_SC_FAILURE_COLUMN, Short.class, 1, null, true));
  }

  /**
   * Modifies the JSON of some of the alert definitions which have changed
   * between Ambari versions.
   */
  protected void updateAlertDefinitions() {
    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      final AlertDefinitionEntity alertDefinitionEntity = alertDefinitionDAO.findByName(
          cluster.getClusterId(), "journalnode_process");

      if (alertDefinitionEntity != null) {
        String source = alertDefinitionEntity.getSource();

        alertDefinitionEntity.setSource(modifyJournalnodeProcessAlertSource(source));
        alertDefinitionEntity.setSourceType(SourceType.WEB);
        alertDefinitionEntity.setHash(UUID.randomUUID().toString());

        alertDefinitionDAO.merge(alertDefinitionEntity);
        LOG.info("journalnode_process alert definition was updated.");
      }
    }
  }

  /**
   * Modifies type of the journalnode_process alert to WEB.
   * Changes reporting text and uri according to the WEB type.
   * Removes default_port property.
   */
  String modifyJournalnodeProcessAlertSource(String source) {
    JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();

    rootJson.remove("type");
    rootJson.addProperty("type", "WEB");

    rootJson.remove("default_port");

    rootJson.remove("uri");
    JsonObject uriJson = new JsonObject();
    uriJson.addProperty("http", "{{hdfs-site/dfs.journalnode.http-address}}");
    uriJson.addProperty("https", "{{hdfs-site/dfs.journalnode.https-address}}");
    uriJson.addProperty("kerberos_keytab", "{{hdfs-site/dfs.web.authentication.kerberos.keytab}}");
    uriJson.addProperty("kerberos_principal", "{{hdfs-site/dfs.web.authentication.kerberos.principal}}");
    uriJson.addProperty("https_property", "{{hdfs-site/dfs.http.policy}}");
    uriJson.addProperty("https_property_value", "HTTPS_ONLY");
    uriJson.addProperty("connection_timeout", 5.0);
    rootJson.add("uri", uriJson);

    rootJson.getAsJsonObject("reporting").getAsJsonObject("ok").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("ok").addProperty(
            "text", "HTTP {0} response in {2:.3f}s");

    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").addProperty(
            "text", "HTTP {0} response from {1} in {2:.3f}s ({3})");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").remove("value");

    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").addProperty("text",
            "Connection failed to {1} ({3})");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").remove("value");

    return rootJson.toString();
  }

  protected void updateHadoopEnv() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config hadoopEnvConfig = cluster.getDesiredConfigByType(HADOOP_ENV_CONFIG);
      if (hadoopEnvConfig != null) {
        String content = hadoopEnvConfig.getProperties().get(CONTENT_PROPERTY);
        if (content != null) {
          content += HADOOP_ENV_CONTENT_TO_APPEND;
          Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
          updateConfigurationPropertiesForCluster(cluster, HADOOP_ENV_CONFIG, updates, true, false);
        }
      }
    }
  }

  protected void updateHDFSConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(ambariManagementController.getClusters());

    for (final Cluster cluster : clusterMap.values()) {
      // Remove dfs.namenode.rpc-address property when NN HA is enabled
      if (cluster.getDesiredConfigByType(HDFS_SITE_CONFIG) != null && isNNHAEnabled(cluster)) {
        Set<String> removePropertiesSet = new HashSet<>();
        removePropertiesSet.add("dfs.namenode.rpc-address");
        removeConfigurationPropertiesFromCluster(cluster, HDFS_SITE_CONFIG, removePropertiesSet);
      }
    }
  }

  protected void updateZookeeperLog4j() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config zookeeperLog4jConfig = cluster.getDesiredConfigByType(ZOOKEEPER_LOG4J_CONFIG);
      if (zookeeperLog4jConfig != null) {
        String content = zookeeperLog4jConfig.getProperties().get(CONTENT_PROPERTY);
        if (content != null) {
          content = content.replaceAll("[\n^]\\s*log4j\\.rootLogger\\s*=\\s*INFO\\s*,\\s*CONSOLE", "\nlog4j.rootLogger=INFO, ROLLINGFILE");
          Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
          updateConfigurationPropertiesForCluster(cluster, ZOOKEEPER_LOG4J_CONFIG, updates, true, false);
        }
      }
    }
  }

  protected void updateStormConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config stormSiteProps = cluster.getDesiredConfigByType(STORM_SITE);
      if (stormSiteProps != null) {
        String nimbusMonitorFreqSecs = stormSiteProps.getProperties().get(NIMBS_MONITOR_FREQ_SECS_PROPERTY);
        if (nimbusMonitorFreqSecs != null && nimbusMonitorFreqSecs.equals("10")) {
          Map<String, String> updates = Collections.singletonMap(NIMBS_MONITOR_FREQ_SECS_PROPERTY, "120");
          updateConfigurationPropertiesForCluster(cluster, STORM_SITE, updates, true, false);
        }
      }
    }
  }

  protected void updateHbaseEnvConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      StackId stackId = cluster.getCurrentStackVersion();
      if (stackId != null && stackId.getStackName().equals("HDP") &&
               VersionUtils.compareVersions(stackId.getStackVersion(), "2.2") >= 0) {
        Config hbaseEnvConfig = cluster.getDesiredConfigByType(HBASE_ENV_CONFIG);
        if (hbaseEnvConfig != null) {
          String content = hbaseEnvConfig.getProperties().get(CONTENT_PROPERTY);
          if (content != null && content.indexOf("MaxDirectMemorySize={{hbase_max_direct_memory_size}}m") < 0) {
            String newPartOfContent = "\n\nexport HBASE_REGIONSERVER_OPTS=\"$HBASE_REGIONSERVER_OPTS {% if hbase_max_direct_memory_size %} -XX:MaxDirectMemorySize={{hbase_max_direct_memory_size}}m {% endif %}\"\n\n";
            content += newPartOfContent;
            Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
            updateConfigurationPropertiesForCluster(cluster, HBASE_ENV_CONFIG, updates, true, false);
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

            String metrics_collector_heapsize = amsEnvProperties.get("metrics_collector_heapsize");
            String content = amsEnvProperties.get("content");
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("metrics_collector_heapsize", memoryToIntMb(metrics_collector_heapsize));
            newProperties.put("content", updateAmsEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_ENV, newProperties, true, true);
          }
          Config amsHbaseEnv = cluster.getDesiredConfigByType(AMS_HBASE_ENV);
          if (amsHbaseEnv != null) {
            Map<String, String> amsHbaseEnvProperties = amsHbaseEnv.getProperties();
            String hbase_regionserver_heapsize = amsHbaseEnvProperties.get("hbase_regionserver_heapsize");
            String regionserver_xmn_size = amsHbaseEnvProperties.get("regionserver_xmn_size");
            String hbase_master_xmn_size = amsHbaseEnvProperties.get("hbase_master_xmn_size");
            String hbase_master_maxperm_size = amsHbaseEnvProperties.get("hbase_master_maxperm_size");
            String hbase_master_heapsize = amsHbaseEnvProperties.get("hbase_master_heapsize");
            String content = amsHbaseEnvProperties.get("content");

            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("hbase_regionserver_heapsize", memoryToIntMb(hbase_regionserver_heapsize));
            newProperties.put("regionserver_xmn_size", memoryToIntMb(regionserver_xmn_size));
            newProperties.put("hbase_master_xmn_size", memoryToIntMb(hbase_master_xmn_size));
            newProperties.put("hbase_master_maxperm_size", memoryToIntMb(hbase_master_maxperm_size));
            newProperties.put("hbase_master_heapsize", memoryToIntMb(hbase_master_heapsize));
            newProperties.put("content", updateAmsHbaseEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_ENV, newProperties, true, true);
          }
        }
      }
    }

  }

  protected void updateKafkaConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices =cluster.getServices().keySet();
          Config kafkaBroker = cluster.getDesiredConfigByType(KAFKA_BROKER);
          if (kafkaBroker != null) {
            Map<String, String> newProperties = new HashMap<>();
            Map<String, String> kafkaBrokerProperties = kafkaBroker.getProperties();
            String kafkaMetricsReporters = kafkaBrokerProperties.get("kafka.metrics.reporters");
            if (kafkaMetricsReporters == null ||
              "{{kafka_metrics_reporters}}".equals(kafkaMetricsReporters)) {

              if (installedServices.contains("AMBARI_METRICS")) {
                newProperties.put("kafka.metrics.reporters", "org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter");
              } else if (installedServices.contains("GANGLIA")) {
                newProperties.put("kafka.metrics.reporters", "kafka.ganglia.KafkaGangliaMetricsReporter");
              } else {
                newProperties.put("kafka.metrics.reporters", " ");
              }

            }
            if (!newProperties.isEmpty()) {
              updateConfigurationPropertiesForCluster(cluster, KAFKA_BROKER, newProperties, true, true);
            }
          }
        }
      }
    }
  }

  protected String updateAmsEnvContent(String oldContent) {
    if (oldContent == null) {
      return null;
    }
    String regSearch = "export\\s*AMS_COLLECTOR_HEAPSIZE\\s*=\\s*\\{\\{metrics_collector_heapsize\\}\\}";
    String replacement = "export AMS_COLLECTOR_HEAPSIZE={{metrics_collector_heapsize}}m";
    return oldContent.replaceAll(regSearch, replacement);
  }

  protected String updateAmsHbaseEnvContent(String content) {
    if (content == null) {
      return null;
    }

    String regSearch = "\\{\\{hbase_heapsize\\}\\}";
    String replacement = "{{hbase_heapsize}}m";
    content = content.replaceAll(regSearch, replacement);
    regSearch = "\\{\\{hbase_master_maxperm_size\\}\\}";
    replacement = "{{hbase_master_maxperm_size}}m";
    content = content.replaceAll(regSearch, replacement);
    regSearch = "\\{\\{hbase_master_xmn_size\\}\\}";
    replacement = "{{hbase_master_xmn_size}}m";
    content = content.replaceAll(regSearch, replacement);
    regSearch = "\\{\\{regionserver_xmn_size\\}\\}";
    replacement = "{{regionserver_xmn_size}}m";
    content = content.replaceAll(regSearch, replacement);
    regSearch = "\\{\\{regionserver_heapsize\\}\\}";
    replacement = "{{regionserver_heapsize}}m";
    content = content.replaceAll(regSearch, replacement);
    return content;
  }

  private String memoryToIntMb(String memorySize) {
    if (memorySize == null) {
      return "0";
    }
    Integer value = 0;
    try {
      value = Integer.parseInt(memorySize.replaceAll("\\D+", ""));
    } catch (NumberFormatException ex) {
      LOG.error(ex.getMessage());
    }
    char unit = memorySize.toUpperCase().charAt(memorySize.length() - 1);
    // Recalculate memory size to Mb
    switch (unit) {
      case 'K':
        value /= 1024;
        break;
      case 'B':
        value /= (1024*1024);
        break;
      case 'G':
        value *= 1024;
        break;
      case 'T':
        value *= 1024*1024;
        break;
    }
    return value.toString();
  }
}
