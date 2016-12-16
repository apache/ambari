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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 2.2.1.
 */
public class UpgradeCatalog221 extends AbstractUpgradeCatalog {

  private static final String AMS_HBASE_SITE = "ams-hbase-site";
  private static final String AMS_SITE = "ams-site";
  private static final String AMS_HBASE_SECURITY_SITE = "ams-hbase-security-site";
  private static final String AMS_ENV = "ams-env";
  private static final String AMS_HBASE_ENV = "ams-hbase-env";
  private static final String AMS_MODE = "timeline.metrics.service.operation.mode";
  private static final String ZK_ZNODE_PARENT = "zookeeper.znode.parent";
  private static final String ZK_CLIENT_PORT = "hbase.zookeeper.property.clientPort";
  private static final String ZK_TICK_TIME = "hbase.zookeeper.property.tickTime";
  private static final String CLUSTER_ENV = "cluster-env";
  private static final String SECURITY_ENABLED = "security_enabled";
  private static final String TOPOLOGY_HOST_INFO_TABLE = "topology_host_info";
  private static final String TOPOLOGY_HOST_INFO_RACK_INFO_COLUMN = "rack_info";
  private static final String TEZ_SITE = "tez-site";

  @Inject
  DaoUtils daoUtils;

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog221.class);

  private static final String OOZIE_SITE_CONFIG = "oozie-site";
  private static final String OOZIE_SERVICE_HADOOP_CONFIGURATIONS_PROPERTY_NAME = "oozie.service.HadoopAccessorService.hadoop.configurations";
  private static final String OLD_DEFAULT_HADOOP_CONFIG_PATH = "/etc/hadoop/conf";
  private static final String NEW_DEFAULT_HADOOP_CONFIG_PATH = "{{hadoop_conf_dir}}";

  private static final String BLUEPRINT_HOSTGROUP_COMPONENT_TABLE_NAME = "hostgroup_component";
  private static final String BLUEPRINT_PROVISION_ACTION_COLUMN_NAME = "provision_action";

  private static final String RANGER_KMS_DBKS_CONFIG = "dbks-site";
  private static final String RANGER_KMS_DB_FLAVOR = "DB_FLAVOR";
  private static final String RANGER_KMS_DB_HOST = "db_host";
  private static final String RANGER_KMS_DB_NAME = "db_name";
  private static final String RANGER_KMS_JDBC_URL = "ranger.ks.jpa.jdbc.url";
  private static final String RANGER_KMS_JDBC_DRIVER = "ranger.ks.jpa.jdbc.driver";
  private static final String RANGER_KMS_PROPERTIES = "kms-properties";

  private static final String TEZ_COUNTERS_MAX = "tez.counters.max";
  private static final String TEZ_COUNTERS_MAX_GROUPS = "tez.counters.max.groups";

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog221(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.2.1";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.2.0";
  }


  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    // indices to improve request status calc performance
    dbAccessor.createIndex("idx_stage_request_id", "stage", "request_id");
    dbAccessor.createIndex("idx_hrc_request_id", "host_role_command", "request_id");
    dbAccessor.createIndex("idx_rsc_request_id", "role_success_criteria", "request_id");

    executeBlueprintProvisionActionDDLUpdates();

    dbAccessor.addColumn(TOPOLOGY_HOST_INFO_TABLE,
        new DBAccessor.DBColumnInfo(TOPOLOGY_HOST_INFO_RACK_INFO_COLUMN, String.class, 255));

  }

  private void executeBlueprintProvisionActionDDLUpdates() throws AmbariException, SQLException {
    // add provision_action column to the hostgroup_component table for Blueprints
    dbAccessor.addColumn(BLUEPRINT_HOSTGROUP_COMPONENT_TABLE_NAME, new DBAccessor.DBColumnInfo(BLUEPRINT_PROVISION_ACTION_COLUMN_NAME,
      String.class, 255, null, true));
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    updateAlerts();
    updateOozieConfigs();
    updateTezConfigs();
    updateRangerKmsDbksConfigs();
    updateAMSConfigs();
  }

  protected void updateAlerts() {
    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterID = cluster.getClusterId();
      final AlertDefinitionEntity hiveMetastoreProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_metastore_process");
      final AlertDefinitionEntity hiveServerProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_server_process");

      List<AlertDefinitionEntity> hiveAlertDefinitions = new ArrayList<>();
      if(hiveMetastoreProcessAlertDefinitionEntity != null) {
        hiveAlertDefinitions.add(hiveMetastoreProcessAlertDefinitionEntity);
      }
      if(hiveServerProcessAlertDefinitionEntity != null) {
        hiveAlertDefinitions.add(hiveServerProcessAlertDefinitionEntity);
      }

      for(AlertDefinitionEntity alertDefinition : hiveAlertDefinitions){
        String source = alertDefinition.getSource();

        alertDefinition.setScheduleInterval(3);
        alertDefinition.setSource(addCheckCommandTimeoutParam(source));
        alertDefinition.setHash(UUID.randomUUID().toString());

        alertDefinitionDAO.merge(alertDefinition);
      }

      final AlertDefinitionEntity amsZookeeperProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
        clusterID, "ams_metrics_collector_zookeeper_server_process");

      if (amsZookeeperProcessAlertDefinitionEntity != null) {
        LOG.info("Removing alert : ams_metrics_collector_zookeeper_server_process");
        alertDefinitionDAO.remove(amsZookeeperProcessAlertDefinitionEntity);
      }
    }
  }

  protected String addCheckCommandTimeoutParam(String source) {
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    JsonArray parametersJson = sourceJson.getAsJsonArray("parameters");

    boolean parameterExists = parametersJson != null && !parametersJson.isJsonNull();

    if (parameterExists) {
      Iterator<JsonElement> jsonElementIterator = parametersJson.iterator();
      while(jsonElementIterator.hasNext()) {
        JsonElement element = jsonElementIterator.next();
        JsonElement name = element.getAsJsonObject().get("name");
        if (name != null && !name.isJsonNull() && name.getAsString().equals("check.command.timeout")) {
          return sourceJson.toString();
        }
      }
    }

    JsonObject checkCommandTimeoutParamJson = new JsonObject();
    checkCommandTimeoutParamJson.add("name", new JsonPrimitive("check.command.timeout"));
    checkCommandTimeoutParamJson.add("display_name", new JsonPrimitive("Check command timeout"));
    checkCommandTimeoutParamJson.add("value", new JsonPrimitive(60.0));
    checkCommandTimeoutParamJson.add("type", new JsonPrimitive("NUMERIC"));
    checkCommandTimeoutParamJson.add("description", new JsonPrimitive("The maximum time before check command will be killed by timeout"));
    checkCommandTimeoutParamJson.add("units", new JsonPrimitive("seconds"));

    if (!parameterExists) {
      parametersJson = new JsonArray();
      parametersJson.add(checkCommandTimeoutParamJson);
      sourceJson.add("parameters", parametersJson);
    } else {
      parametersJson.add(checkCommandTimeoutParamJson);
      sourceJson.remove("parameters");
      sourceJson.add("parameters", parametersJson);
    }

    return sourceJson.toString();
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

          String znodeParent = null;
          Config amsHbaseSecuritySite = cluster.getDesiredConfigByType(AMS_HBASE_SECURITY_SITE);
          if (amsHbaseSecuritySite != null) {
            Map<String, String> amsHbaseSecuritySiteProperties = amsHbaseSecuritySite.getProperties();
            znodeParent = amsHbaseSecuritySiteProperties.get(ZK_ZNODE_PARENT);
            LOG.info("Removing config zookeeper.znode.parent from ams-hbase-security-site");
            removeConfigurationPropertiesFromCluster(cluster, AMS_HBASE_SECURITY_SITE, Collections.singleton(ZK_ZNODE_PARENT));
          }

          Config amsHbaseSite = cluster.getDesiredConfigByType(AMS_HBASE_SITE);
          if (amsHbaseSite != null) {
            Map<String, String> amsHbaseSiteProperties = amsHbaseSite.getProperties();
            Map<String, String> newProperties = new HashMap<>();

            if (!amsHbaseSiteProperties.containsKey(ZK_ZNODE_PARENT)) {
              if (StringUtils.isEmpty(znodeParent) || "/hbase".equals(znodeParent)) {
                boolean isSecurityEnabled = false;
                Config clusterEnv = cluster.getDesiredConfigByType(CLUSTER_ENV);
                if (clusterEnv != null) {
                  Map<String,String> clusterEnvProperties = clusterEnv.getProperties();
                  if (clusterEnvProperties.containsKey(SECURITY_ENABLED)) {
                    isSecurityEnabled = Boolean.valueOf(clusterEnvProperties.get(SECURITY_ENABLED));
                  }
                }
                znodeParent = "/ams-hbase-" + (isSecurityEnabled ? "secure" : "unsecure");
              }

              LOG.info("Adding config zookeeper.znode.parent=" + znodeParent + " to ams-hbase-site");
              newProperties.put(ZK_ZNODE_PARENT, znodeParent);

            }

            boolean isDistributed = false;
            Config amsSite = cluster.getDesiredConfigByType(AMS_SITE);
            if (amsSite != null) {
              if ("distributed".equals(amsSite.getProperties().get(AMS_MODE))) {
                isDistributed = true;
              }
            }

            // Skip override if custom port found in embedded mode.
            if (amsHbaseSiteProperties.containsKey(ZK_CLIENT_PORT) &&
               (isDistributed || amsHbaseSiteProperties.get(ZK_CLIENT_PORT).equals("61181"))) {
              String newValue = "{{zookeeper_clientPort}}";
              LOG.info("Replacing value of " + ZK_CLIENT_PORT + " from " +
                amsHbaseSiteProperties.get(ZK_CLIENT_PORT) + " to " +
                newValue + " in ams-hbase-site");

              newProperties.put(ZK_CLIENT_PORT, newValue);
            }

            if (!amsHbaseSiteProperties.containsKey(ZK_TICK_TIME)) {
              LOG.info("Adding config " + ZK_TICK_TIME + " to ams-hbase-site");
              newProperties.put(ZK_TICK_TIME, "6000");
            }

            updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_SITE, newProperties, true, true);
          }

          Config amsHbaseEnv = cluster.getDesiredConfigByType(AMS_HBASE_ENV);
          if (amsHbaseEnv != null) {
            Map<String, String> amsHbaseEnvProperties = amsHbaseEnv.getProperties();
            String content = amsHbaseEnvProperties.get("content");
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("content", updateAmsHbaseEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_ENV, newProperties, true, true);
          }
        }
      }
    }
  }

  protected String updateAmsHbaseEnvContent(String content) {
    if (content == null) {
      return null;
    }
    String regSearch = "_jaas_config_file\\}\\} -Dzookeeper.sasl.client.username=\\{\\{zk_servicename\\}\\}";
    String replacement = "_jaas_config_file}}";
    content = content.replaceAll(regSearch, replacement);
    return content;
  }

  protected String updateAmsEnvContent(String content) {

    if (content == null) {
      return null;
    }
    String regSearch = "-Djava.security.auth.login.config=\\{\\{ams_collector_jaas_config_file\\}\\} " +
      "-Dzookeeper.sasl.client.username=\\{\\{zk_servicename\\}\\}";
    String replacement = "-Djava.security.auth.login.config={{ams_collector_jaas_config_file}}";
    content = content.replaceAll(regSearch, replacement);

    return content;
  }

  protected void updateOozieConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config oozieSiteProps = cluster.getDesiredConfigByType(OOZIE_SITE_CONFIG);
      if (oozieSiteProps != null) {
        // Update oozie.service.HadoopAccessorService.hadoop.configurations
        Map<String, String> updateProperties = new HashMap<>();
        String oozieHadoopConfigProperty = oozieSiteProps.getProperties().get(OOZIE_SERVICE_HADOOP_CONFIGURATIONS_PROPERTY_NAME);
        if(oozieHadoopConfigProperty != null && oozieHadoopConfigProperty.contains(OLD_DEFAULT_HADOOP_CONFIG_PATH)) {
          String updatedOozieHadoopConfigProperty = oozieHadoopConfigProperty.replaceAll(
              OLD_DEFAULT_HADOOP_CONFIG_PATH, NEW_DEFAULT_HADOOP_CONFIG_PATH);
          updateProperties.put(OOZIE_SERVICE_HADOOP_CONFIGURATIONS_PROPERTY_NAME, updatedOozieHadoopConfigProperty);
          updateConfigurationPropertiesForCluster(cluster, OOZIE_SITE_CONFIG, updateProperties, true, false);
        }
      }
    }
  }

  protected void updateTezConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config tezSiteProps = cluster.getDesiredConfigByType(TEZ_SITE);
      if (tezSiteProps != null) {

        // Update tez.counters.max and tez.counters.max.groups configurations
        String tezCountersMaxProperty = tezSiteProps.getProperties().get(TEZ_COUNTERS_MAX);
        String tezCountersMaxGroupesProperty = tezSiteProps.getProperties().get(TEZ_COUNTERS_MAX_GROUPS);

        StackId stackId = cluster.getCurrentStackVersion();
        boolean isStackNotLess23 = (stackId != null && stackId.getStackName().equals("HDP") &&
            VersionUtils.compareVersions(stackId.getStackVersion(), "2.3") >= 0);

        if (isStackNotLess23) {
          Map<String, String> updates = new HashMap<String, String>();
          if (tezCountersMaxProperty != null && tezCountersMaxProperty.equals("2000")) {
            updates.put(TEZ_COUNTERS_MAX, "10000");
          }
          if (tezCountersMaxGroupesProperty != null && tezCountersMaxGroupesProperty.equals("1000")) {
            updates.put(TEZ_COUNTERS_MAX_GROUPS, "3000");
          }
          if (!updates.isEmpty()) {
            updateConfigurationPropertiesForCluster(cluster, TEZ_SITE, updates, true, false);
          }
        }
      }
    }
  }

  protected void updateRangerKmsDbksConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Map<String, String> newRangerKmsProps = new HashMap<>();
      Config rangerKmsDbConfigs = cluster.getDesiredConfigByType(RANGER_KMS_PROPERTIES);
      if (rangerKmsDbConfigs != null) {
        String dbFlavor = rangerKmsDbConfigs.getProperties().get(RANGER_KMS_DB_FLAVOR);
        String dbHost = rangerKmsDbConfigs.getProperties().get(RANGER_KMS_DB_HOST);
        String dbName = rangerKmsDbConfigs.getProperties().get(RANGER_KMS_DB_NAME);
        String dbConnectionString = null;
        String dbDriver = null;

        if (dbFlavor != null && dbHost != null && dbName != null) {
          if ("MYSQL".equalsIgnoreCase(dbFlavor)) {
            dbConnectionString = "jdbc:mysql://"+dbHost+"/"+dbName;
            dbDriver = "com.mysql.jdbc.Driver";
          } else if ("ORACLE".equalsIgnoreCase(dbFlavor)) {
            dbConnectionString = "jdbc:oracle:thin:@//"+dbHost;
            dbDriver = "oracle.jdbc.driver.OracleDriver";
          } else if ("POSTGRES".equalsIgnoreCase(dbFlavor)) {
            dbConnectionString = "jdbc:postgresql://"+dbHost+"/"+dbName;
            dbDriver = "org.postgresql.Driver";
          } else if ("MSSQL".equalsIgnoreCase(dbFlavor)) {
            dbConnectionString = "jdbc:sqlserver://"+dbHost+";databaseName="+dbName;
            dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
          } else if ("SQLA".equalsIgnoreCase(dbFlavor)) {
            dbConnectionString = "jdbc:sqlanywhere:database="+dbName+";host="+dbHost;
            dbDriver = "sap.jdbc4.sqlanywhere.IDriver";
          }
          newRangerKmsProps.put(RANGER_KMS_JDBC_URL, dbConnectionString);
          newRangerKmsProps.put(RANGER_KMS_JDBC_DRIVER, dbDriver);
          updateConfigurationPropertiesForCluster(cluster, RANGER_KMS_DBKS_CONFIG, newRangerKmsProps, true, false);
        }
      }
    }
  }

}
