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

import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_INFRA_NEW_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_INFRA_OLD_NAME;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.commons.collections.MapUtils;
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

  private static final String SERVICE_CONFIG_MAPPING_TABLE = "serviceconfigmapping";
  private static final String CLUSTER_CONFIG_TABLE = "clusterconfig";

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
    renameAmbariInfraInConfigGroups();
    removeLogSearchPatternConfigs();
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
            Config rangerKmsDbksConfig = cluster.getDesiredConfigByType("dbks-site");
            if (rangerKmsPropertiesConfig != null) {
              String dbFlavor = rangerKmsPropertiesConfig.getProperties().get("DB_FLAVOR");
              String dbHost = rangerKmsPropertiesConfig.getProperties().get("db_host");
              String rangerKmsRootDbUrl = "";
              if (dbFlavor != null && dbHost != null) {
                String port = "";
                if (rangerKmsDbksConfig != null) {
                  String rangerKmsDbUrl = rangerKmsDbksConfig.getProperties().get("ranger.ks.jpa.jdbc.url");
                  if (rangerKmsDbUrl != null) {
                    Pattern pattern = Pattern.compile("(:[0-9]+)");
                    Matcher matcher = pattern.matcher(rangerKmsDbUrl);
                    if (matcher.find()) {
                      port = matcher.group();
                    }
                  }
                }
                if ("MYSQL".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:mysql://" + dbHost + (!port.equalsIgnoreCase("")?port:":3306");
                } else if ("ORACLE".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:oracle:thin:@//" + dbHost + (!port.equalsIgnoreCase("")?port:":1521");
                } else if ("POSTGRES".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:postgresql://" + dbHost + (!port.equalsIgnoreCase("")?port:":5432") + "/postgres";
                } else if ("MSSQL".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:sqlserver://" + dbHost + (!port.equalsIgnoreCase("")?port:":1433");
                } else if ("SQLA".equalsIgnoreCase(dbFlavor)) {
                  rangerKmsRootDbUrl = "jdbc:sqlanywhere:host=" + dbHost + (!port.equalsIgnoreCase("")?port:":2638") + ";";
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

  protected void renameAmbariInfraInConfigGroups() {
    LOG.info("Renaming service AMBARI_INFRA to AMBARI_INFRA_SOLR in config group records");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters == null)
      return;

    Map<String, Cluster> clusterMap = clusters.getClusters();
    if (MapUtils.isEmpty(clusterMap))
      return;

    EntityManager entityManager = getEntityManagerProvider().get();

    executeInTransaction(() -> {
      TypedQuery<ServiceConfigEntity> serviceConfigUpdate = entityManager.createQuery(
              "UPDATE ConfigGroupEntity SET serviceName = :newServiceName WHERE serviceName = :oldServiceName", ServiceConfigEntity.class);
      serviceConfigUpdate.setParameter("newServiceName", AMBARI_INFRA_NEW_NAME);
      serviceConfigUpdate.setParameter("oldServiceName", AMBARI_INFRA_OLD_NAME);
      serviceConfigUpdate.executeUpdate();
    });

    executeInTransaction(() -> {
      TypedQuery<ServiceConfigEntity> serviceConfigUpdate = entityManager.createQuery(
              "UPDATE ConfigGroupEntity SET tag = :newServiceName WHERE tag = :oldServiceName", ServiceConfigEntity.class);
      serviceConfigUpdate.setParameter("newServiceName", AMBARI_INFRA_NEW_NAME);
      serviceConfigUpdate.setParameter("oldServiceName", AMBARI_INFRA_OLD_NAME);
      serviceConfigUpdate.executeUpdate();
    });


    // Force the clusters object to reload to ensure the renamed service is accounted for
    entityManager.getEntityManagerFactory().getCache().evictAll();
    clusters.invalidateAllClusters();
  }

  /**
   * Removes config types with -logsearch-conf suffix
   */
  protected void removeLogSearchPatternConfigs() throws SQLException {
    DBAccessor dba = dbAccessor != null ? dbAccessor : injector.getInstance(DBAccessor.class); // for testing
    String configSuffix = "-logsearch-conf";
    String serviceConfigMappingRemoveSQL = String.format(
      "DELETE FROM %s WHERE config_id IN (SELECT config_id from %s where type_name like '%%%s')",
      SERVICE_CONFIG_MAPPING_TABLE, CLUSTER_CONFIG_TABLE, configSuffix);

    String clusterConfigRemoveSQL = String.format(
      "DELETE FROM %s WHERE type_name like '%%%s'",
      CLUSTER_CONFIG_TABLE, configSuffix);

    dba.executeQuery(serviceConfigMappingRemoveSQL);
    dba.executeQuery(clusterConfigRemoveSQL);
  }
}