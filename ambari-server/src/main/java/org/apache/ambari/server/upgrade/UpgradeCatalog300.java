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
import java.util.Collection;
import java.util.HashSet;
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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

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
  protected static final String CLUSTER_CONFIG_TABLE = "clusterconfig";
  protected static final String CLUSTER_CONFIG_SELECTED_COLUMN = "selected";
  protected static final String CLUSTER_CONFIG_SELECTED_TIMESTAMP_COLUMN = "selected_timestamp";
  protected static final String CLUSTER_CONFIG_MAPPING_TABLE = "clusterconfigmapping";

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
    return "2.5.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateStageTable();
    updateClusterConfigurationTable();
  }

  protected void updateStageTable() throws SQLException {
    dbAccessor.addColumn(STAGE_TABLE,
        new DBAccessor.DBColumnInfo(STAGE_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
    dbAccessor.addColumn(STAGE_TABLE,
        new DBAccessor.DBColumnInfo(STAGE_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
    dbAccessor.addColumn(REQUEST_TABLE,
        new DBAccessor.DBColumnInfo(REQUEST_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    setSelectedConfigurationsAndRemoveMappingTable();
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
   * Performs the following operations on {@code clusterconfig}:
   * <ul>
   * <li>Adds the {@link #CLUSTER_CONFIG_SELECTED_COLUMN} to
   * {@link #CLUSTER_CONFIG_TABLE}.
   * <li>Adds the {@link #CLUSTER_CONFIG_SELECTED_TIMESTAMP} to
   * {@link #CLUSTER_CONFIG_TABLE}.
   * </ul>
   */
  protected void updateClusterConfigurationTable() throws SQLException {
    dbAccessor.addColumn(CLUSTER_CONFIG_TABLE,
        new DBAccessor.DBColumnInfo(CLUSTER_CONFIG_SELECTED_COLUMN, Short.class, null, 0, false));

    dbAccessor.addColumn(CLUSTER_CONFIG_TABLE,
        new DBAccessor.DBColumnInfo(CLUSTER_CONFIG_SELECTED_TIMESTAMP_COLUMN, Long.class, null, 0,
            false));
  }

  /**
   * Performs the following operations on {@code clusterconfig} and
   * {@code clusterconfigmapping}:
   * <ul>
   * <li>Sets both selected columns to the current config by querying
   * {@link #CLUSTER_CONFIG_MAPPING_TABLE}.
   * <li>Removes {@link #CLUSTER_CONFIG_MAPPING_TABLE}.
   * </ul>
   */
  protected void setSelectedConfigurationsAndRemoveMappingTable() throws SQLException {
    // update the new selected columns
    executeInTransaction(new Runnable() {
      /**
       * {@inheritDoc}
       */
      @Override
      public void run() {
        String selectSQL = String.format(
            "SELECT cluster_id, type_name, version_tag FROM %s WHERE selected = 1 ORDER BY cluster_id ASC, type_name ASC, version_tag ASC",
            CLUSTER_CONFIG_MAPPING_TABLE);

        Statement statement = null;
        ResultSet resultSet = null;

        long now = System.currentTimeMillis();

        try {
          statement = dbAccessor.getConnection().createStatement();
          resultSet = statement.executeQuery(selectSQL);

          while (resultSet.next()) {
            final Long clusterId = resultSet.getLong("cluster_id");
            final String typeName = resultSet.getString("type_name");
            final String versionTag = resultSet.getString("version_tag");

            // inefficient since this can be done with a single nested SELECT,
            // but this way we can log what's happening which is more useful
            String updateSQL = String.format(
                "UPDATE %s SET selected = 1, selected_timestamp = %d WHERE cluster_id = %d AND type_name = '%s' AND version_tag = '%s'",
                CLUSTER_CONFIG_TABLE, now, clusterId, typeName, versionTag);

            dbAccessor.executeQuery(updateSQL);
          }
        } catch (SQLException sqlException) {
          throw new RuntimeException(sqlException);
        } finally {
          JdbcUtils.closeResultSet(resultSet);
          JdbcUtils.closeStatement(statement);
        }
      }
    });

    // if the above execution and committed the transaction, then we can remove
    // the cluster configuration mapping table
    dbAccessor.dropTable(CLUSTER_CONFIG_MAPPING_TABLE);
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
          Collection<Config> configs = cluster.getAllConfigs();
          for (Config config : configs) {
            String configType = config.getType();
            if (!configType.endsWith("-logsearch-conf")) {
              continue;
            }
            
            Set<String> removeProperties = new HashSet<>();
            removeProperties.add("service_name");
            removeProperties.add("component_mappings");
            removeProperties.add("content");
            
            removeConfigurationPropertiesFromCluster(cluster, configType, removeProperties);
          }
        }
      }
    }
  }
}
