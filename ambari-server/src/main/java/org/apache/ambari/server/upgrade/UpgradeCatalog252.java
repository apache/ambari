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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;

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
}
