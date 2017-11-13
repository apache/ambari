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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class UpgradeCatalog300 extends AbstractUpgradeCatalog {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog300.class);

  protected static final String STAGE_TABLE = "stage";
  protected static final String STAGE_STATUS_COLUMN = "status";
  protected static final String STAGE_DISPLAY_STATUS_COLUMN = "display_status";
  protected static final String REQUEST_TABLE = "request";
  protected static final String REQUEST_DISPLAY_STATUS_COLUMN = "display_status";
  protected static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  protected static final String HRC_OPS_DISPLAY_NAME_COLUMN = "ops_display_name";
  protected static final String COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
  protected static final String COMPONENT_STATE_TABLE = "hostcomponentstate";
  protected static final String SERVICE_DESIRED_STATE_TABLE = "servicedesiredstate";
  protected static final String SECURITY_STATE_COLUMN = "security_state";

  protected static final String AMBARI_CONFIGURATION_TABLE = "ambari_configuration";
  protected static final String AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN = "category_name";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN = "property_name";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN = "property_value";

  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog300(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "3.0.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.6.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateStageTable();
    addOpsDisplayNameColumnToHostRoleCommand();
    removeSecurityState();
    addAmbariConfigurationTable();
  }

  protected void updateStageTable() throws SQLException {
    dbAccessor.addColumn(STAGE_TABLE,
        new DBAccessor.DBColumnInfo(STAGE_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
    dbAccessor.addColumn(STAGE_TABLE,
        new DBAccessor.DBColumnInfo(STAGE_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
    dbAccessor.addColumn(REQUEST_TABLE,
        new DBAccessor.DBColumnInfo(REQUEST_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
  }

  protected void addAmbariConfigurationTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, String.class, 100, null, false));
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN, String.class, 100, null, false));
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN, String.class, 255, null, true));

    dbAccessor.createTable(AMBARI_CONFIGURATION_TABLE, columns);
    dbAccessor.addPKConstraint(AMBARI_CONFIGURATION_TABLE, "PK_ambari_configuration", AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN);
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
    showHcatDeletedUserMessage();
    setStatusOfStagesAndRequests();
    updateLogSearchConfigs();
    updateKerberosConfigurations();
  }

  protected void showHcatDeletedUserMessage() {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      for (final Cluster cluster : clusterMap.values()) {
        Config hiveEnvConfig = cluster.getDesiredConfigByType("hive-env");
        if (hiveEnvConfig != null) {
          Map<String, String> hiveEnvProperties = hiveEnvConfig.getProperties();
          String webhcatUser = hiveEnvProperties.get("webhcat_user");
          String hcatUser = hiveEnvProperties.get("hcat_user");
          if (!StringUtils.equals(webhcatUser, hcatUser)) {
            System.out.print("WARNING: In hive-env config, webhcat and hcat user are different. In current ambari release (3.0.0), hcat user was removed from stack, so potentially you could have some problems.");
            LOG.warn("In hive-env config, webhcat and hcat user are different. In current ambari release (3.0.0), hcat user was removed from stack, so potentially you could have some problems.");
          }
        }
      }
    }

  }

  protected void setStatusOfStagesAndRequests() {
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        try {
          RequestDAO requestDAO = injector.getInstance(RequestDAO.class);
          StageFactory stageFactory = injector.getInstance(StageFactory.class);
          EntityManager em = getEntityManagerProvider().get();
          List<RequestEntity> requestEntities= requestDAO.findAll();
          for (RequestEntity requestEntity: requestEntities) {
            Collection<StageEntity> stageEntities= requestEntity.getStages();
            List <HostRoleStatus> stageDisplayStatuses = new ArrayList<>();
            List <HostRoleStatus> stageStatuses = new ArrayList<>();
            for (StageEntity stageEntity: stageEntities) {
              Stage stage = stageFactory.createExisting(stageEntity);
              List<HostRoleCommand> hostRoleCommands = stage.getOrderedHostRoleCommands();
              Map<HostRoleStatus, Integer> statusCount = CalculatedStatus.calculateStatusCountsForTasks(hostRoleCommands);
              HostRoleStatus stageDisplayStatus = CalculatedStatus.calculateSummaryDisplayStatus(statusCount, hostRoleCommands.size(), stage.isSkippable());
              HostRoleStatus stageStatus = CalculatedStatus.calculateStageStatus(hostRoleCommands, statusCount, stage.getSuccessFactors(), stage.isSkippable());
              stageEntity.setStatus(stageStatus);
              stageStatuses.add(stageStatus);
              stageEntity.setDisplayStatus(stageDisplayStatus);
              stageDisplayStatuses.add(stageDisplayStatus);
              em.merge(stageEntity);
            }
            HostRoleStatus requestStatus = CalculatedStatus.getOverallStatusForRequest(stageStatuses);
            requestEntity.setStatus(requestStatus);
            HostRoleStatus requestDisplayStatus = CalculatedStatus.getOverallDisplayStatusForRequest(stageDisplayStatuses);
            requestEntity.setDisplayStatus(requestDisplayStatus);
            em.merge(requestEntity);
          }
        } catch (Exception e) {
          LOG.warn("Setting status for stages and Requests threw exception. ", e);
        }
      }
    });
  }

  /**
   * Adds the {@value #HRC_OPS_DISPLAY_NAME_COLUMN} column to the
   * {@value #HOST_ROLE_COMMAND_TABLE} table.
   *
   * @throws SQLException
   */
  private void addOpsDisplayNameColumnToHostRoleCommand() throws SQLException {
    dbAccessor.addColumn(HOST_ROLE_COMMAND_TABLE,
        new DBAccessor.DBColumnInfo(HRC_OPS_DISPLAY_NAME_COLUMN, String.class, 255, null, true));
  }

  private void removeSecurityState() throws SQLException {
    dbAccessor.dropColumn(COMPONENT_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
    dbAccessor.dropColumn(COMPONENT_STATE_TABLE, SECURITY_STATE_COLUMN);
    dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
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

      ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Collection<Config> configs = cluster.getAllConfigs();
          for (Config config : configs) {
            String configType = config.getType();
            if (configType.endsWith("-logsearch-conf")) {
              configHelper.removeConfigsByType(cluster, configType);
            }
          }

          Config logSearchEnv = cluster.getDesiredConfigByType("logsearch-env");

          String oldProtocolProperty = null;
          String oldPortProperty = null;
          if (logSearchEnv != null) {
            oldProtocolProperty = logSearchEnv.getProperties().get("logsearch_ui_port");
            oldPortProperty = logSearchEnv.getProperties().get("logsearch_ui_protocol");
          }

          Config logSearchProperties = cluster.getDesiredConfigByType("logsearch-properties");
          Config logFeederProperties = cluster.getDesiredConfigByType("logfeeder-properties");
          if (logSearchProperties != null && logFeederProperties != null) {
            configHelper.createConfigType(cluster, cluster.getDesiredStackVersion(), ambariManagementController,
                "logsearch-common-properties", Collections.emptyMap(), "ambari-upgrade",
                String.format("Updated logsearch-common-properties during Ambari Upgrade from %s to %s",
                    getSourceVersion(), getTargetVersion()));

            String defaultLogLevels = logSearchProperties.getProperties().get("logsearch.logfeeder.include.default.level");

            Set<String> removeProperties = Sets.newHashSet("logsearch.logfeeder.include.default.level");
            removeConfigurationPropertiesFromCluster(cluster, "logsearch-properties", removeProperties);

            Map<String, String> newLogSearchProperties = new HashMap<>();
            if (oldProtocolProperty != null) {
              newLogSearchProperties.put("logsearch.protocol", oldProtocolProperty);
            }

            if (oldPortProperty != null) {
              newLogSearchProperties.put("logsearch.http.port", oldPortProperty);
              newLogSearchProperties.put("logsearch.https.port", oldPortProperty);
            }
            if (!newLogSearchProperties.isEmpty()) {
              updateConfigurationPropertiesForCluster(cluster, "logsearch-properties", newLogSearchProperties, true, true);
            }

            Map<String, String> newLogfeederProperties = new HashMap<>();
            newLogfeederProperties.put("logfeeder.include.default.level", defaultLogLevels);
            updateConfigurationPropertiesForCluster(cluster, "logfeeder-properties", newLogfeederProperties, true, true);
          }

          Config logFeederLog4jProperties = cluster.getDesiredConfigByType("logfeeder-log4j");
          if (logFeederLog4jProperties != null) {
            String content = logFeederLog4jProperties.getProperties().get("content");
            if (content.contains("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">")) {
              content = content.replace("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");
              updateConfigurationPropertiesForCluster(cluster, "logfeeder-log4j", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logSearchLog4jProperties = cluster.getDesiredConfigByType("logsearch-log4j");
          if (logSearchLog4jProperties != null) {
            String content = logSearchLog4jProperties.getProperties().get("content");
            if (content.contains("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">")) {
              content = content.replace("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");
              updateConfigurationPropertiesForCluster(cluster, "logsearch-log4j", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logSearchServiceLogsConfig = cluster.getDesiredConfigByType("logsearch-service_logs-solrconfig");
          if (logSearchServiceLogsConfig != null) {
            String content = logSearchServiceLogsConfig.getProperties().get("content");
            if (content.contains("class=\"solr.admin.AdminHandlers\"")) {
              content = content.replaceAll("(?s)<requestHandler name=\"/admin/\".*?class=\"solr.admin.AdminHandlers\" />", "");
              updateConfigurationPropertiesForCluster(cluster, "logsearch-service_logs-solrconfig", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logSearchAuditLogsConfig = cluster.getDesiredConfigByType("logsearch-audit_logs-solrconfig");
          if (logSearchAuditLogsConfig != null) {
            String content = logSearchAuditLogsConfig.getProperties().get("content");
            if (content.contains("class=\"solr.admin.AdminHandlers\"")) {
              content = content.replaceAll("(?s)<requestHandler name=\"/admin/\".*?class=\"solr.admin.AdminHandlers\" />", "");
              updateConfigurationPropertiesForCluster(cluster, "logsearch-audit_logs-solrconfig", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logFeederOutputConfig = cluster.getDesiredConfigByType("logfeeder-output-config");
          if (logFeederOutputConfig != null) {
            String content = logFeederOutputConfig.getProperties().get("content");
            content = content.replace(
                "      \"collection\":\"{{logsearch_solr_collection_service_logs}}\",\n" +
                "      \"number_of_shards\": \"{{logsearch_collection_service_logs_numshards}}\",\n" +
                "      \"splits_interval_mins\": \"{{logsearch_service_logs_split_interval_mins}}\",\n",
                "      \"type\": \"service\",\n");

            content = content.replace(
                "      \"collection\":\"{{logsearch_solr_collection_audit_logs}}\",\n" +
                "      \"number_of_shards\": \"{{logsearch_collection_audit_logs_numshards}}\",\n" +
                "      \"splits_interval_mins\": \"{{logsearch_audit_logs_split_interval_mins}}\",\n",
                "      \"type\": \"audit\",\n");

            updateConfigurationPropertiesForCluster(cluster, "logfeeder-output-config", Collections.singletonMap("content", content), true, true);
          }
        }
      }
    }
  }

  protected void updateKerberosConfigurations() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (!MapUtils.isEmpty(clusterMap)) {
        for (Cluster cluster : clusterMap.values()) {
          Config config = cluster.getDesiredConfigByType("kerberos-env");

          if (config != null) {
            Map<String, String> properties = config.getProperties();
            if (properties.containsKey("group")) {
              // Covert kerberos-env/group to kerberos-env/ipa_user_group
              updateConfigurationPropertiesForCluster(cluster, "kerberos-env",
                  Collections.singletonMap("ipa_user_group", properties.get("group")), Collections.singleton("group"),
                  true, false);
            }
          }
        }
      }
    }
  }
}
