/**
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link org.apache.ambari.server.upgrade.UpgradeCatalog252} upgrades Ambari from 2.5.1 to 2.5.2.
 */
public class UpgradeCatalog252 extends AbstractUpgradeCatalog {

  static final String CLUSTERCONFIG_TABLE = "clusterconfig";
  static final String SERVICE_DELETED_COLUMN = "service_deleted";

  private static final String UPGRADE_TABLE = "upgrade";
  private static final String UPGRADE_TABLE_FROM_REPO_COLUMN = "from_repo_version_id";
  private static final String UPGRADE_TABLE_TO_REPO_COLUMN = "to_repo_version_id";
  private static final String CLUSTERS_TABLE = "clusters";
  private static final String SERVICE_COMPONENT_HISTORY_TABLE = "servicecomponent_history";
  private static final String UPGRADE_GROUP_TABLE = "upgrade_group";
  private static final String UPGRADE_ITEM_TABLE = "upgrade_item";
  private static final String UPGRADE_ID_COLUMN = "upgrade_id";

  private static final String CLUSTER_ENV = "cluster-env";

  private static final String HIVE_ENV = "hive-env";
  private static final String MARIADB_REDHAT_SUPPORT = "mariadb_redhat_support";

  private static final List<String> configTypesToEnsureSelected = Arrays.asList("spark2-javaopts-properties");
  
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog252.class);

  /**
   * Constructor.
   *
   * @param injector
   */
  @Inject
  public UpgradeCatalog252(Injector injector) {
    super(injector);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.5.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.5.2";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    addServiceDeletedColumnToClusterConfigTable();
    addRepositoryColumnsToUpgradeTable();
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
    resetStackToolsAndFeatures();
    ensureConfigTypesHaveAtLeastOneVersionSelected();
    updateMariaDBRedHatSupportHive();
  }

  /**
   * Adds the {@value #SERVICE_DELETED_COLUMN} column to the
   * {@value #CLUSTERCONFIG_TABLE} table.
   *
   * @throws java.sql.SQLException
   */
  private void addServiceDeletedColumnToClusterConfigTable() throws SQLException {
    dbAccessor.addColumn(CLUSTERCONFIG_TABLE,
        new DBColumnInfo(SERVICE_DELETED_COLUMN, Short.class, null, 0, false));
  }

  /**
   * Changes the following columns to {@value #UPGRADE_TABLE}:
   * <ul>
   * <li>{@value #UPGRADE_TABLE_FROM_REPO_COLUMN}
   * <li>{@value #UPGRADE_TABLE_TO_REPO_COLUMN}
   * <li>Removes {@code to_version}
   * <li>Removes {@code from_version}
   * </ul>
   *
   * @throws SQLException
   */
  private void addRepositoryColumnsToUpgradeTable() throws SQLException {
    dbAccessor.clearTableColumn(CLUSTERS_TABLE, UPGRADE_ID_COLUMN, null);
    dbAccessor.clearTable(SERVICE_COMPONENT_HISTORY_TABLE);
    dbAccessor.clearTable(SERVICE_COMPONENT_HISTORY_TABLE);
    dbAccessor.clearTable(UPGRADE_ITEM_TABLE);
    dbAccessor.clearTable(UPGRADE_GROUP_TABLE);
    dbAccessor.clearTable(UPGRADE_TABLE);

    dbAccessor.dropColumn(UPGRADE_TABLE, "to_version");
    dbAccessor.dropColumn(UPGRADE_TABLE, "from_version");

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBColumnInfo(UPGRADE_TABLE_FROM_REPO_COLUMN, Long.class, null, null, false));

    dbAccessor.addFKConstraint(UPGRADE_TABLE, "FK_upgrade_from_repo_id",
        UPGRADE_TABLE_FROM_REPO_COLUMN, "repo_version", "repo_version_id", false);

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBColumnInfo(UPGRADE_TABLE_TO_REPO_COLUMN, Long.class, null, null, false));

    dbAccessor.addFKConstraint(UPGRADE_TABLE, "FK_upgrade_to_repo_id",
        UPGRADE_TABLE_FROM_REPO_COLUMN, "repo_version", "repo_version_id", false);
  }

  /**
   * Resets the following properties in {@code cluster-env} to their new
   * defaults:
   * <ul>
   * <li>stack_root
   * <li>stack_tools
   * <li>stack_features
   * <ul>
   *
   * @throws AmbariException
   */
  private void resetStackToolsAndFeatures() throws AmbariException {
    Set<String> propertiesToReset = Sets.newHashSet("stack_tools", "stack_features", "stack_root");

    Clusters clusters = injector.getInstance(Clusters.class);
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);

    Map<String, Cluster> clusterMap = clusters.getClusters();
    for (Cluster cluster : clusterMap.values()) {
      Config clusterEnv = cluster.getDesiredConfigByType(CLUSTER_ENV);
      if (null == clusterEnv) {
        continue;
      }

      Map<String, String> newStackProperties = new HashMap<>();
      Set<PropertyInfo> stackProperties = configHelper.getStackProperties(cluster);
      if (null == stackProperties) {
        continue;
      }

      for (PropertyInfo propertyInfo : stackProperties) {
        String fileName = propertyInfo.getFilename();
        if (StringUtils.isEmpty(fileName)) {
          continue;
        }

        if (StringUtils.equals(ConfigHelper.fileNameToConfigType(fileName), CLUSTER_ENV)) {
          String stackPropertyName = propertyInfo.getName();
          if (propertiesToReset.contains(stackPropertyName)) {
            newStackProperties.put(stackPropertyName, propertyInfo.getValue());
          }
        }
      }

      updateConfigurationPropertiesForCluster(cluster, CLUSTER_ENV, newStackProperties, true, false);
    }
  }

  /**
   * When doing a cross-stack upgrade, we found that one config type (spark2-javaopts-properties)
   * did not have any mappings that were selected, so it caused Ambari Server start to fail on the DB Consistency Checker.
   * To fix this, iterate over all config types and ensure that at least one is selected.
   * If none are selected, then pick the one with the greatest time stamp; this should be safe since we are only adding
   * more data to use as opposed to removing.
   */
  private void ensureConfigTypesHaveAtLeastOneVersionSelected() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    List<ClusterEntity> clusters = clusterDAO.findAll();

    if (null == clusters) {
      return;
    }

    for (ClusterEntity clusterEntity : clusters) {
      LOG.info("Ensuring all config types have at least one selected config for cluster {}", clusterEntity.getClusterName());

      boolean atLeastOneChanged = false;
      Collection<ClusterConfigMappingEntity> configMappingEntities = clusterEntity.getConfigMappingEntities();

      if (configMappingEntities != null) {
        Set<String> configTypesNotSelected = new HashSet<>();
        Set<String> configTypesWithAtLeastOneSelected = new HashSet<>();

        for (ClusterConfigMappingEntity clusterConfigMappingEntity : configMappingEntities) {
          String typeName = clusterConfigMappingEntity.getType();

          if (clusterConfigMappingEntity.isSelected() == 1) {
            configTypesWithAtLeastOneSelected.add(typeName);
          } else {
            configTypesNotSelected.add(typeName);
          }
        }

        // Due to the ordering, eliminate any configs with at least one selected.
        configTypesNotSelected.removeAll(configTypesWithAtLeastOneSelected);
        if (!configTypesNotSelected.isEmpty()) {
          LOG.info("The following config types have config mappings which don't have at least one as selected. {}", StringUtils.join(configTypesNotSelected, ", "));

          LOG.info("Filtering only config types these config types: {}", StringUtils.join(configTypesToEnsureSelected, ", "));
          // Get the intersection with a subset of configs that are allowed to be selected during the migration.
          configTypesNotSelected.retainAll(configTypesToEnsureSelected);
        }

        if (!configTypesNotSelected.isEmpty()) {
          LOG.info("The following config types have config mappings which don't have at least one as selected. {}", StringUtils.join(configTypesNotSelected, ", "));

          for (String typeName : configTypesNotSelected) {
            ClusterConfigMappingEntity clusterConfigMappingWithGreatestTimeStamp = null;

            for (ClusterConfigMappingEntity clusterConfigMappingEntity : configMappingEntities) {
              if (typeName.equals(clusterConfigMappingEntity.getType())) {

                if (null == clusterConfigMappingWithGreatestTimeStamp) {
                  clusterConfigMappingWithGreatestTimeStamp = clusterConfigMappingEntity;
                } else {
                  if (clusterConfigMappingEntity.getCreateTimestamp() >= clusterConfigMappingWithGreatestTimeStamp.getCreateTimestamp()) {
                    clusterConfigMappingWithGreatestTimeStamp = clusterConfigMappingEntity;
                  }
                }
              }
            }

            if (null != clusterConfigMappingWithGreatestTimeStamp) {
              LOG.info("Saving. Config type {} has a mapping with tag {} and greatest timestamp {} that is not selected, so will mark it selected.",
                  typeName, clusterConfigMappingWithGreatestTimeStamp.getTag(), clusterConfigMappingWithGreatestTimeStamp.getCreateTimestamp());
              atLeastOneChanged = true;
              clusterConfigMappingWithGreatestTimeStamp.setSelected(1);
            }
          }
        } else {
          LOG.info("All config types have at least one mapping that is selected. Nothing to do.");
        }
      }

      if (atLeastOneChanged) {
        clusterDAO.mergeConfigMappings(configMappingEntities);
      }
    }
  }

  /**
   * Insert mariadb_redhat_support to hive-env if the current stack is BigInsights 4.2.5
   * @throws AmbariException
   * */
  private void updateMariaDBRedHatSupportHive() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          if (installedServices.contains("HIVE")) {
            StackId currentStack = cluster.getCurrentStackVersion();
            if (currentStack.getStackName().equals("BigInsights") && currentStack.getStackVersion().equals("4.2.5")) {
              Map<String, String> newProperties = new HashMap<>();
              newProperties.put(MARIADB_REDHAT_SUPPORT, "true");
              updateConfigurationPropertiesForCluster(cluster, HIVE_ENV, newProperties, true, false);
            }
          }
        }
      }
    }
  }
}
