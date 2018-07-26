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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class UpgradeCatalog271 extends AbstractUpgradeCatalog {

  /**
   * Logger
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog271.class);

  @Inject
  DaoUtils daoUtils;

  /**
   * Constructor
   *
   * @param injector
   */
  @Inject
  public UpgradeCatalog271(Injector injector) {
    super(injector);
    daoUtils = injector.getInstance(DaoUtils.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.7.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.7.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
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
    updateRangerLogDirConfigs();
    updateRangerKmsDbUrl();
  }

  /**
   * Updating Ranger Admin and Ranger Usersync log directory configs
   * @throws AmbariException
   */
  protected void updateRangerLogDirConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          if (installedServices.contains("RANGER")) {
            Config rangerEnvConfig = cluster.getDesiredConfigByType("ranger-env");
            Config rangerAdminSiteConfig = cluster.getDesiredConfigByType("ranger-admin-site");
            Config rangerUgsyncSiteConfig = cluster.getDesiredConfigByType("ranger-ugsync-site");
            if (rangerEnvConfig != null) {
              String rangerAdminLogDir = rangerEnvConfig.getProperties().get("ranger_admin_log_dir");
              String rangerUsersyncLogDir = rangerEnvConfig.getProperties().get("ranger_usersync_log_dir");
              if (rangerAdminLogDir != null && rangerAdminSiteConfig != null) {
                Map<String, String> newProperty = new HashMap<String, String>();
                newProperty.put("ranger.logs.base.dir", rangerAdminLogDir);
                updateConfigurationPropertiesForCluster(cluster, "ranger-admin-site", newProperty, true, false);
              }
              if (rangerUsersyncLogDir != null && rangerUgsyncSiteConfig != null && rangerUgsyncSiteConfig.getProperties().containsKey("ranger.usersync.logdir")) {
                Map<String, String> updateProperty = new HashMap<String, String>();
                updateProperty.put("ranger.usersync.logdir", rangerUsersyncLogDir);
                updateConfigurationPropertiesForCluster(cluster, "ranger-ugsync-site", updateProperty, true, false);
              }
              Set<String> removeProperties = Sets.newHashSet("ranger_admin_log_dir", "ranger_usersync_log_dir");
              removeConfigurationPropertiesFromCluster(cluster, "ranger-env", removeProperties);
            }
          }
        }
      }
    }

  }

  /**
   * Updating JDBC connection url in Ranger KMS for verifying communication to database
   * using database root user credentials
   * @throws AmbariException
   */
  protected void updateRangerKmsDbUrl() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          if (installedServices.contains("RANGER_KMS")) {
            Config rangerKmsPropertiesConfig = cluster.getDesiredConfigByType("kms-properties");
            Config rangerKmsEnvConfig = cluster.getDesiredConfigByType("kms-env");
            if (rangerKmsPropertiesConfig != null) {
              String dbFlavor = rangerKmsPropertiesConfig.getProperties().get("DB_FLAVOR");
              String dbHost = rangerKmsPropertiesConfig.getProperties().get("db_host");
              String rangerKmsRootDbUrl = "";
              if (dbFlavor != null && dbHost != null) {
                if ("MYSQL".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:mysql://" + dbHost + ":3306";
                } else if ("ORACLE".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:oracle:thin:@//" + dbHost + ":1521";
                } else if ("POSTGRES".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:postgresql://" + dbHost + ":5432/postgres";
                } else if ("MSSQL".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:sqlserver://" + dbHost + ":1433;";
                } else if ("SQLA".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:sqlanywhere:host=" + dbHost + ":2638;";
                }
                Map<String, String> newProperty = new HashMap<String, String>();
                newProperty.put("ranger_kms_privelege_user_jdbc_url", rangerKmsRootDbUrl);
                if (rangerKmsEnvConfig != null) {
                  updateConfigurationPropertiesForCluster(cluster, "kms-env", newProperty, true, false);
                }
              }
            }
          }
        }
      }
    }
  }

}