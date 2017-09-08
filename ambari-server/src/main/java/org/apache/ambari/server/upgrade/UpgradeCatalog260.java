/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.upgrade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link org.apache.ambari.server.upgrade.UpgradeCatalog260} upgrades Ambari from 2.5.2 to 2.6.0.
 */
public class UpgradeCatalog260 extends AbstractUpgradeCatalog {

  public static final String CLUSTER_CONFIG_MAPPING_TABLE = "clusterconfigmapping";
  public static final String CLUSTER_VERSION_TABLE = "cluster_version";
  public static final String CLUSTER_ID_COLUMN = "cluster_id";
  public static final String STATE_COLUMN = "state";
  public static final String CREATE_TIMESTAMP_COLUMN = "create_timestamp";
  public static final String VERSION_TAG_COLUMN = "version_tag";
  public static final String TYPE_NAME_COLUMN = "type_name";

  public static final String CLUSTER_CONFIG_TABLE = "clusterconfig";
  public static final String SELECTED_COLUMN = "selected";
  public static final String SERVICE_DELETED_COLUMN = "service_deleted";
  public static final String UNMAPPED_COLUMN = "unmapped";
  public static final String SELECTED_TIMESTAMP_COLUMN = "selected_timestamp";

  public static final String SERVICE_COMPONENT_DESIRED_STATE_TABLE = "servicecomponentdesiredstate";
  public static final String DESIRED_STACK_ID_COLUMN = "desired_stack_id";
  public static final String DESIRED_VERSION_COLUMN = "desired_version";
  public static final String DESIRED_REPO_VERSION_ID_COLUMN = "desired_repo_version_id";
  public static final String REPO_STATE_COLUMN = "repo_state";
  public static final String FK_SCDS_DESIRED_STACK_ID = "FK_scds_desired_stack_id";
  public static final String FK_SCDS_DESIRED_REPO_ID = "FK_scds_desired_repo_id";

  public static final String REPO_VERSION_TABLE = "repo_version";
  public static final String REPO_VERSION_ID_COLUMN = "repo_version_id";
  public static final String REPO_VERSION_HIDDEN_COLUMN = "hidden";

  public static final String HOST_COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
  public static final String FK_HCDS_DESIRED_STACK_ID = "FK_hcds_desired_stack_id";

  public static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  public static final String CURRENT_STACK_ID_COLUMN = "current_stack_id";
  public static final String FK_HCS_CURRENT_STACK_ID = "FK_hcs_current_stack_id";

  public static final String HOST_VERSION_TABLE = "host_version";
  public static final String UQ_HOST_REPO = "UQ_host_repo";
  public static final String HOST_ID_COLUMN = "host_id";

  public static final String SERVICE_DESIRED_STATE_TABLE = "servicedesiredstate";
  public static final String FK_SDS_DESIRED_STACK_ID = "FK_sds_desired_stack_id";
  public static final String FK_REPO_VERSION_ID = "FK_repo_version_id";

  public static final String UPGRADE_TABLE = "upgrade";
  public static final String UPGRADE_GROUP_TABLE = "upgrade_group";
  public static final String UPGRADE_ITEM_TABLE = "upgrade_item";
  public static final String FROM_REPO_VERSION_ID_COLUMN = "from_repo_version_id";
  public static final String TO_REPO_VERSION_ID_COLUMN = "to_repo_version_id";
  public static final String ORCHESTRATION_COLUMN = "orchestration";
  public static final String ALLOW_REVERT_COLUMN = "revert_allowed";
  public static final String FK_UPGRADE_FROM_REPO_ID = "FK_upgrade_from_repo_id";
  public static final String FK_UPGRADE_TO_REPO_ID = "FK_upgrade_to_repo_id";
  public static final String FK_UPGRADE_REPO_VERSION_ID = "FK_upgrade_repo_version_id";

  public static final String SERVICE_COMPONENT_HISTORY_TABLE = "servicecomponent_history";
  public static final String UPGRADE_HISTORY_TABLE = "upgrade_history";
  public static final String ID_COLUMN = "id";
  public static final String UPGRADE_ID_COLUMN = "upgrade_id";
  public static final String SERVICE_NAME_COLUMN = "service_name";
  public static final String COMPONENT_NAME_COLUMN = "component_name";
  public static final String TARGET_REPO_VERSION_ID_COLUMN = "target_repo_version_id";
  public static final String PK_UPGRADE_HIST = "PK_upgrade_hist";
  public static final String FK_UPGRADE_HIST_UPGRADE_ID = "FK_upgrade_hist_upgrade_id";
  public static final String FK_UPGRADE_HIST_FROM_REPO = "FK_upgrade_hist_from_repo";
  public static final String FK_UPGRADE_HIST_TARGET_REPO = "FK_upgrade_hist_target_repo";
  public static final String UQ_UPGRADE_HIST = "UQ_upgrade_hist";
  public static final String SERVICE_CONFIG_MAPPING_TABLE = "serviceconfigmapping";
  public static final String SERVICE_COMPONENT_DESIRED_STATE = "servicecomponentdesiredstate";
  public static final String HOST_COMPONENT_DESIRED_STATE = "hostcomponentdesiredstate";
  public static final String HOST_COMPONENT_STATE = "hostcomponentstate";

  private static final String CORE_SITE = "core-site";
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog260.class);
  public static final String STANDARD = "STANDARD";
  public static final String NOT_REQUIRED = "NOT_REQUIRED";
  public static final String CURRENT = "CURRENT";
  public static final String SELECTED = "1";


  /**
   * Constructor.
   *
   * @param injector
   */
  @Inject
  public UpgradeCatalog260(Injector injector) {
    super(injector);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion()
  {
    return "2.5.2";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.6.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    int currentVersionID = getCurrentVersionID();
    updateServiceComponentDesiredStateTable(currentVersionID);
    updateServiceDesiredStateTable(currentVersionID);
    addSelectedCollumsToClusterconfigTable();
    updateHostComponentDesiredStateTable();
    updateHostComponentStateTable();
    dropStaleTables();
    updateUpgradeTable();
    createUpgradeHistoryTable();
    updateRepositoryVersionTable();
    renameServiceDeletedColumn();
  }

  private void renameServiceDeletedColumn() throws AmbariException, SQLException {
    if (dbAccessor.tableHasColumn(CLUSTER_CONFIG_TABLE, SERVICE_DELETED_COLUMN)) {
      dbAccessor.renameColumn(CLUSTER_CONFIG_TABLE, SERVICE_DELETED_COLUMN, new DBAccessor.DBColumnInfo(UNMAPPED_COLUMN, Short.class, null, 0, false));
    }
  }

  /*
  * This method, search for configs which are not linked with any service
  * and set "unmapped" to true. We need that because in case when
  * config is not mapped and "unmapped" flag = false, db consistency check
  * will show warning about not mapped config. (AMBARI-21795)
  * */
  private void setUnmappedForOrphanedConfigs() {
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        EntityManager entityManager = getEntityManagerProvider().get();
        Query query = entityManager.createNamedQuery("ClusterConfigEntity.findNotMappedClusterConfigsToService",ClusterConfigEntity.class);

        List<ClusterConfigEntity> notMappedConfigs =  query.getResultList();
        if (notMappedConfigs != null) {
          for (ClusterConfigEntity clusterConfigEntity : notMappedConfigs) {
            clusterConfigEntity.setUnmapped(true);
            entityManager.merge(clusterConfigEntity);
          }
        }
      }
    });
  }

  private void createUpgradeHistoryTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();

    columns.add(new DBAccessor.DBColumnInfo(ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(UPGRADE_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(SERVICE_NAME_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(COMPONENT_NAME_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(FROM_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(TARGET_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));
    dbAccessor.createTable(UPGRADE_HISTORY_TABLE, columns);

    dbAccessor.addPKConstraint(UPGRADE_HISTORY_TABLE, PK_UPGRADE_HIST, ID_COLUMN);

    dbAccessor.addFKConstraint(UPGRADE_HISTORY_TABLE, FK_UPGRADE_HIST_UPGRADE_ID, UPGRADE_ID_COLUMN, UPGRADE_TABLE, UPGRADE_ID_COLUMN, false);
    dbAccessor.addFKConstraint(UPGRADE_HISTORY_TABLE, FK_UPGRADE_HIST_FROM_REPO, FROM_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);
    dbAccessor.addFKConstraint(UPGRADE_HISTORY_TABLE, FK_UPGRADE_HIST_TARGET_REPO, TARGET_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);
    dbAccessor.addUniqueConstraint(UPGRADE_HISTORY_TABLE, UQ_UPGRADE_HIST, UPGRADE_ID_COLUMN, COMPONENT_NAME_COLUMN, SERVICE_NAME_COLUMN);

    addSequence("upgrade_history_id_seq", 0L, false);
  }

  /**
   * Updates {@value #UPGRADE_TABLE} table.
   * clear {@value #UPGRADE_TABLE} table
   * Removes {@value #FROM_REPO_VERSION_ID_COLUMN} column.
   * Removes {@value #TO_REPO_VERSION_ID_COLUMN} column.
   * Adds the {@value #ORCHESTRATION_COLUMN} column.
   * Adds the {@value #REPO_VERSION_ID_COLUMN} column.
   * Removes {@value #FK_UPGRADE_FROM_REPO_ID} foreign key.
   * Removes {@value #FK_UPGRADE_TO_REPO_ID} foreign key.
   * adds {@value #FK_REPO_VERSION_ID} foreign key.
   *
   * @throws java.sql.SQLException
   */
  private void updateUpgradeTable() throws SQLException {
    dbAccessor.clearTable(UPGRADE_ITEM_TABLE);
    dbAccessor.clearTable(UPGRADE_GROUP_TABLE);
    dbAccessor.clearTable(UPGRADE_TABLE);
    dbAccessor.dropFKConstraint(UPGRADE_TABLE, FK_UPGRADE_FROM_REPO_ID);
    dbAccessor.dropFKConstraint(UPGRADE_TABLE, FK_UPGRADE_TO_REPO_ID);
    dbAccessor.dropColumn(UPGRADE_TABLE, FROM_REPO_VERSION_ID_COLUMN);
    dbAccessor.dropColumn(UPGRADE_TABLE, TO_REPO_VERSION_ID_COLUMN);

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBAccessor.DBColumnInfo(REPO_VERSION_ID_COLUMN, Long.class, null, null, false));

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBAccessor.DBColumnInfo(ORCHESTRATION_COLUMN, String.class, 255, STANDARD, false));

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBAccessor.DBColumnInfo(ALLOW_REVERT_COLUMN, Short.class, null, 0, false));

    dbAccessor.addFKConstraint(UPGRADE_TABLE, FK_UPGRADE_REPO_VERSION_ID, REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);
  }

  /**
   * Updates {@value #SERVICE_DESIRED_STATE_TABLE} table.
   * Removes {@value #DESIRED_STACK_ID_COLUMN} column.
   * Adds the {@value #DESIRED_REPO_VERSION_ID_COLUMN} column.
   * Removes {@value #FK_SDS_DESIRED_STACK_ID} foreign key.
   * adds {@value #FK_REPO_VERSION_ID} foreign key.
   *
   * @throws java.sql.SQLException
   */
  private void updateServiceDesiredStateTable(int currentRepoID) throws SQLException {

    dbAccessor.addColumn(SERVICE_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(DESIRED_REPO_VERSION_ID_COLUMN, Long.class, null, currentRepoID, false));
    dbAccessor.alterColumn(SERVICE_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(DESIRED_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));

    dbAccessor.addFKConstraint(SERVICE_DESIRED_STATE_TABLE, FK_REPO_VERSION_ID, DESIRED_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);
    dbAccessor.dropFKConstraint(SERVICE_DESIRED_STATE_TABLE, FK_SDS_DESIRED_STACK_ID);
    dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, DESIRED_STACK_ID_COLUMN);
  }

  /**
   * drop {@value #CLUSTER_CONFIG_MAPPING_TABLE} and {@value #CLUSTER_VERSION_TABLE} tables.
   *
   * @throws java.sql.SQLException
   */
  private void dropStaleTables() throws SQLException {
    dbAccessor.dropTable(CLUSTER_CONFIG_MAPPING_TABLE);
    dbAccessor.dropTable(CLUSTER_VERSION_TABLE);
    dbAccessor.dropTable(SERVICE_COMPONENT_HISTORY_TABLE);
  }

  /**
   * Adds the {@value #SELECTED_COLUMN} and {@value #SELECTED_TIMESTAMP_COLUMN} columns to the
   * {@value #CLUSTER_CONFIG_TABLE} table.
   *
   * @throws java.sql.SQLException
   */
  private void addSelectedCollumsToClusterconfigTable() throws SQLException {
    DBAccessor.DBColumnInfo selectedColumnInfo = new DBAccessor.DBColumnInfo(SELECTED_COLUMN, Short.class, null, 0, false);
    DBAccessor.DBColumnInfo selectedmappingColumnInfo = new DBAccessor.DBColumnInfo(SELECTED_COLUMN, Integer.class, null, 0, false);
    DBAccessor.DBColumnInfo selectedTimestampColumnInfo = new DBAccessor.DBColumnInfo(SELECTED_TIMESTAMP_COLUMN, Long.class, null, 0, false);
    DBAccessor.DBColumnInfo createTimestampColumnInfo = new DBAccessor.DBColumnInfo(CREATE_TIMESTAMP_COLUMN, Long.class, null, null, false);
    dbAccessor.copyColumnToAnotherTable(CLUSTER_CONFIG_MAPPING_TABLE, selectedmappingColumnInfo,
        CLUSTER_ID_COLUMN, TYPE_NAME_COLUMN, VERSION_TAG_COLUMN, CLUSTER_CONFIG_TABLE, selectedColumnInfo,
        CLUSTER_ID_COLUMN, TYPE_NAME_COLUMN, VERSION_TAG_COLUMN, SELECTED_COLUMN, SELECTED, 0);

    dbAccessor.copyColumnToAnotherTable(CLUSTER_CONFIG_MAPPING_TABLE, createTimestampColumnInfo,
        CLUSTER_ID_COLUMN, TYPE_NAME_COLUMN, VERSION_TAG_COLUMN, CLUSTER_CONFIG_TABLE, selectedTimestampColumnInfo,
        CLUSTER_ID_COLUMN, TYPE_NAME_COLUMN, VERSION_TAG_COLUMN, SELECTED_COLUMN, SELECTED, 0);
  }


  /**
   * Updates {@value #SERVICE_COMPONENT_DESIRED_STATE_TABLE} table.
   * Removes {@value #DESIRED_VERSION_COLUMN},{@value #DESIRED_STACK_ID_COLUMN} columns.
   * Adds the {@value #DESIRED_REPO_VERSION_ID_COLUMN},{@value #REPO_STATE_COLUMN} columns.
   * Removes {@value #FK_SCDS_DESIRED_STACK_ID} foreign key.
   * adds {@value #FK_SCDS_DESIRED_REPO_ID} foreign key.
   *
   * @throws java.sql.SQLException
   */
  private void updateServiceComponentDesiredStateTable(int currentRepoID) throws SQLException {
    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(DESIRED_REPO_VERSION_ID_COLUMN, Long.class, null, currentRepoID, false));
    dbAccessor.alterColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(DESIRED_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));

    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(REPO_STATE_COLUMN, String.class, 255, CURRENT, false));
    dbAccessor.alterColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(REPO_STATE_COLUMN, String.class, 255, NOT_REQUIRED, false));

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_DESIRED_STATE_TABLE, FK_SCDS_DESIRED_REPO_ID, DESIRED_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);

    dbAccessor.dropFKConstraint(SERVICE_COMPONENT_DESIRED_STATE_TABLE, FK_SCDS_DESIRED_STACK_ID);
    dbAccessor.dropColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE, DESIRED_STACK_ID_COLUMN);
    dbAccessor.dropColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE, DESIRED_VERSION_COLUMN);
  }

  /**
   * Updates {@value #HOST_COMPONENT_DESIRED_STATE_TABLE} table.
   * Removes {@value #DESIRED_STACK_ID_COLUMN} column.
   * Removes {@value #FK_HCDS_DESIRED_STACK_ID} foreign key.
   *
   * @throws java.sql.SQLException
   */
  private void updateHostComponentDesiredStateTable() throws SQLException {
    dbAccessor.dropFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, FK_HCDS_DESIRED_STACK_ID);
    dbAccessor.dropColumn(HOST_COMPONENT_DESIRED_STATE_TABLE, DESIRED_STACK_ID_COLUMN);
  }

  /**
   * Updates {@value #HOST_COMPONENT_STATE_TABLE} table.
   * Removes {@value #CURRENT_STACK_ID_COLUMN} column.
   * Removes {@value #FK_HCS_CURRENT_STACK_ID} foreign key.
   *
   * @throws java.sql.SQLException
   */
  private void updateHostComponentStateTable() throws SQLException {
    dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, FK_HCS_CURRENT_STACK_ID);
    dbAccessor.dropColumn(HOST_COMPONENT_STATE_TABLE, CURRENT_STACK_ID_COLUMN);
  }

  /**
   * Updates {@value #REPO_VERSION_TABLE} table. Adds
   * {@value #REPO_VERSION_HIDDEN_COLUMN} column.
   *
   * @throws java.sql.SQLException
   */
  private void updateRepositoryVersionTable() throws SQLException {
    dbAccessor.addColumn(REPO_VERSION_TABLE,
        new DBAccessor.DBColumnInfo(REPO_VERSION_HIDDEN_COLUMN, Short.class, null, 0, false));
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
    setUnmappedForOrphanedConfigs();
    removeSupersetFromDruid();
    ensureZeppelinProxyUserConfigs();
    updateKerberosDescriptorArtifacts();
  }

  /**
   * get {@value #REPO_VERSION_ID_COLUMN} value from {@value #CLUSTER_VERSION_TABLE}
   * where {@value #STATE_COLUMN} = {@value #CURRENT}
   * and validate it
   *
   * @return current version ID
   * @throws AmbariException
   * @throws SQLException
   */
  public int getCurrentVersionID() throws AmbariException, SQLException {
    List<Integer> currentVersionList = dbAccessor.getIntColumnValues(CLUSTER_VERSION_TABLE, REPO_VERSION_ID_COLUMN,
        new String[]{STATE_COLUMN}, new String[]{CURRENT}, false);
    if (currentVersionList.isEmpty()) {
      throw new AmbariException("Unable to find any CURRENT repositories.");
    } else if (currentVersionList.size() != 1) {
      throw new AmbariException("The following repositories were found to be CURRENT: ".concat(StringUtils.join(currentVersionList, ",")));
    }
    return currentVersionList.get(0);
  }

  // Superset is moved as an independent service in Ambari-2.6.
  // This will remove superset component if installed under Druid Service
  protected void removeSupersetFromDruid() throws SQLException {
    removeComponent("DRUID_SUPERSET", "druid-superset");
  }

  private void removeComponent(String componentName, String configPrefix) throws SQLException {

    String serviceConfigMappingRemoveSQL = String.format(
        "DELETE FROM %s WHERE config_id IN (SELECT config_id from %s where type_name like '%s%%')",
        SERVICE_CONFIG_MAPPING_TABLE, CLUSTER_CONFIG_TABLE, configPrefix);

    String supersetConfigRemoveSQL = String.format(
        "DELETE FROM %s WHERE type_name like '%s%%'",
        CLUSTER_CONFIG_TABLE, configPrefix);

    String hostComponentDesiredStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        HOST_COMPONENT_DESIRED_STATE, componentName);

    String hostComponentStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        HOST_COMPONENT_STATE, componentName);

    String serviceComponentDesiredStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        SERVICE_COMPONENT_DESIRED_STATE, componentName);

    dbAccessor.executeQuery(serviceConfigMappingRemoveSQL);
    dbAccessor.executeQuery(supersetConfigRemoveSQL);
    dbAccessor.executeQuery(hostComponentDesiredStateRemoveSQL);
    dbAccessor.executeQuery(hostComponentStateRemoveSQL);
    dbAccessor.executeQuery(serviceComponentDesiredStateRemoveSQL);
  }

  /**
   * If Zeppelin is installed, ensure that the proxyuser configurations are set in <code>core-site</code>.
   * <p>
   * The following configurations will be added, if core-site exists and the properties are not in the
   * set of core-site properties:
   * <ul>
   * <li><code>"hadoop.proxyuser.{zeppelin-env/zeppelin_user}.groups": "*"</code></li>
   * <li><code>"hadoop.proxyuser.{zeppelin-env/zeppelin_user}.hosts": "*"</code></li>
   * </ul>
   */
  void ensureZeppelinProxyUserConfigs() throws AmbariException {
    Clusters clusters = injector.getInstance(Clusters.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);

    if ((clusterMap != null) && !clusterMap.isEmpty()) {
      for (final Cluster cluster : clusterMap.values()) {
        Config zeppelinEnvConfig = cluster.getDesiredConfigByType("zeppelin-env");

        if (zeppelinEnvConfig != null) {
          // If zeppelin-env exists, than it is assumed that Zeppelin is installed
          Map<String, String> zeppelinEnvProperties = zeppelinEnvConfig.getProperties();

          String zeppelinUser = null;
          if (zeppelinEnvProperties != null) {
            zeppelinUser = zeppelinEnvProperties.get("zeppelin_user");
          }

          if (!StringUtils.isEmpty(zeppelinUser)) {
            // If the zeppelin user is set, see if the proxyuser configs need to be set

            Config coreSiteConfig = cluster.getDesiredConfigByType(CORE_SITE);
            if (coreSiteConfig != null) {
              // If core-site exists, ensure the proxyuser configurations for Zeppelin are set.
              // If they are not already set, set them to their default value.
              String proxyUserHostsName = String.format("hadoop.proxyuser.%s.hosts", zeppelinUser);
              String proxyUserGroupsName = String.format("hadoop.proxyuser.%s.groups", zeppelinUser);

              Map<String, String> proxyUserProperties = new HashMap<>();
              proxyUserProperties.put(proxyUserHostsName, "*");
              proxyUserProperties.put(proxyUserGroupsName, "*");

              Map<String, String> coreSiteConfigProperties = coreSiteConfig.getProperties();

              if (coreSiteConfigProperties != null) {
                if (coreSiteConfigProperties.containsKey(proxyUserHostsName)) {
                  proxyUserProperties.remove(proxyUserHostsName);
                }

                if (coreSiteConfigProperties.containsKey(proxyUserGroupsName)) {
                  proxyUserProperties.remove(proxyUserGroupsName);
                }
              }

              if (!proxyUserProperties.isEmpty()) {
                updateConfigurationPropertiesForCluster(cluster, CORE_SITE, proxyUserProperties, true, false);
              }
            }
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateKerberosDescriptorArtifact(ArtifactDAO artifactDAO, ArtifactEntity artifactEntity) throws AmbariException {
    if (artifactEntity != null) {
      Map<String, Object> data = artifactEntity.getArtifactData();
      if (data != null) {
        final KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(data);
        if (kerberosDescriptor != null) {
          KerberosServiceDescriptor rangerKmsServiceDescriptor = kerberosDescriptor.getService("RANGER_KMS");
          if (rangerKmsServiceDescriptor != null) {

            KerberosIdentityDescriptor rangerKmsServiceIdentity = rangerKmsServiceDescriptor.getIdentity("/smokeuser");
            if (rangerKmsServiceIdentity != null) {
              rangerKmsServiceDescriptor.removeIdentity("/smokeuser");
            }
            KerberosComponentDescriptor rangerKmscomponentDescriptor = rangerKmsServiceDescriptor.getComponent("RANGER_KMS_SERVER");
            if (rangerKmscomponentDescriptor != null) {
              KerberosIdentityDescriptor rangerKmsComponentIdentity = rangerKmscomponentDescriptor.getIdentity("/smokeuser");
              if (rangerKmsComponentIdentity != null) {
                rangerKmscomponentDescriptor.removeIdentity("/smokeuser");
              }
            }
          }
          artifactEntity.setArtifactData(kerberosDescriptor.toMap());
          artifactDAO.merge(artifactEntity);
        }
      }
    }
  }
}
