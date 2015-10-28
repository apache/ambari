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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

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
  public static final String UPGRADE_PACKAGE_COL = "upgrade_package";
  public static final String UPGRADE_TYPE_COL = "upgrade_type";
  public static final String REPO_VERSION_TABLE = "repo_version";

  private static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  private static final String HOST_ID_COL = "host_id";

  private static final String KERBEROS_DESCRIPTOR_TABLE = "kerberos_descriptor";
  private static final String KERBEROS_DESCRIPTOR_NAME_COLUMN = "kerberos_descriptor_name";
  private static final String KERBEROS_DESCRIPTOR_COLUMN = "kerberos_descriptor";

  private static final String BLUEPRINT_TABLE = "blueprint";
  private static final String SECURITY_TYPE_COLUMN = "security_type";
  private static final String SECURITY_DESCRIPTOR_REF_COLUMN = "security_descriptor_reference";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog213.class);

  @Inject
  DaoUtils daoUtils;

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private ClusterDAO clusterDAO;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog213(Injector injector) {
    super(injector);
    this.injector = injector;
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
    return "2.1.2.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    executeUpgradeDDLUpdates();

    // Alter the host_role_command table to allow host_id to be nullable
    dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));

    addKerberosDescriptorTable();
    executeBlueprintDDLUpdates();
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

  private void executeBlueprintDDLUpdates() throws AmbariException, SQLException {
    dbAccessor.addColumn(BLUEPRINT_TABLE, new DBAccessor.DBColumnInfo(SECURITY_TYPE_COLUMN,
      String.class, 32, "NONE", false));
    dbAccessor.addColumn(BLUEPRINT_TABLE, new DBAccessor.DBColumnInfo(SECURITY_DESCRIPTOR_REF_COLUMN,
      String.class, null, null, true));
  }

    /**
     * {@inheritDoc}
     */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    // execute DDL updates
    executeStackUpgradeDDLUpdates();

    // DDL and DML mixed code, double check here
    bootstrapRepoVersionForHDP21();

    // execute DML updates, no DDL things after this line
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
      }

      // ensure that these are set to false for existing upgrades
      upgrade.setAutoSkipComponentFailures(false);
      upgrade.setAutoSkipServiceCheckFailures(false);

      // apply changes
      upgradeDAO.merge(upgrade);

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
   * Move the upgrade_package column from the repo_version table to the upgrade table as follows,
   * add column upgrade_package to upgrade table as String 255 and nullable
   * populate column in the upgrade table
   * drop the column in the repo_version table
   * make the column in the upgrade table non-nullable.
   * This has to be called as part of DML and not DDL since the persistence service has to be started.
   * @throws AmbariException
   * @throws SQLException
   */
  @Transactional
  protected void executeStackUpgradeDDLUpdates() throws SQLException, AmbariException {
    final Configuration.DatabaseType databaseType = configuration.getDatabaseType();

    // Add columns
    if (!dbAccessor.tableHasColumn(UPGRADE_TABLE, UPGRADE_PACKAGE_COL)) {
      LOG.info("Adding upgrade_package column to upgrade table.");
      dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_PACKAGE_COL, String.class, 255, null, true));
    }
    if (!dbAccessor.tableHasColumn(UPGRADE_TABLE, UPGRADE_TYPE_COL)) {
      LOG.info("Adding upgrade_type column to upgrade table.");
      dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_TYPE_COL, String.class, 32, null, true));
    }

    // Populate values in upgrade table.
    boolean success = populateUpgradeTable();

    if (!success) {
      throw new AmbariException("Errors found while populating the upgrade table with values for columns upgrade_type and upgrade_package.");
    }

    if (dbAccessor.tableHasColumn(REPO_VERSION_TABLE, UPGRADE_PACKAGE_COL)) {
      LOG.info("Dropping upgrade_package column from repo_version table.");
      dbAccessor.dropColumn(REPO_VERSION_TABLE, UPGRADE_PACKAGE_COL);

      // Now, make the added column non-nullable
      // Make the hosts id non-null after all the values are populated
      LOG.info("Making upgrade_package column in the upgrade table non-nullable.");
      if (databaseType == Configuration.DatabaseType.DERBY) {
        // This is a workaround for UpgradeTest.java unit test
        dbAccessor.executeQuery("ALTER TABLE " + UPGRADE_TABLE + " ALTER column " + UPGRADE_PACKAGE_COL + " NOT NULL");
      } else {
        dbAccessor.alterColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_PACKAGE_COL, String.class, 255, null, false));
      }
    }

    if (dbAccessor.tableHasColumn(REPO_VERSION_TABLE, UPGRADE_TYPE_COL)) {
      // Now, make the added column non-nullable
      // Make the hosts id non-null after all the values are populated
      LOG.info("Making upgrade_type column in the upgrade table non-nullable.");
      if (databaseType == Configuration.DatabaseType.DERBY) {
        // This is a workaround for UpgradeTest.java unit test
        dbAccessor.executeQuery("ALTER TABLE " + UPGRADE_TABLE + " ALTER column " + UPGRADE_TYPE_COL + " NOT NULL");
      } else {
        dbAccessor.alterColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_TYPE_COL, String.class, 32, null, false));
      }
    }
  }

  /**
   * Populate the upgrade table with values for the columns upgrade_type and upgrade_package.
   * The upgrade_type will default to {@code org.apache.ambari.server.state.stack.upgrade.UpgradeType.ROLLING}
   * whereas the upgrade_package will be calculated.
   * @return {@code} true on success, and {@code} false otherwise.
   */
  private boolean populateUpgradeTable() {
    boolean success = true;
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        // Need to use SQL since the schema is changing and some of the columns have not yet been added..
        rs = statement.executeQuery("SELECT upgrade_id, cluster_id, from_version, to_version, direction, upgrade_package, upgrade_type FROM upgrade");
        if (rs != null) {
          try {
            while (rs.next()) {
              final long upgradeId = rs.getLong("upgrade_id");
              final long clusterId = rs.getLong("cluster_id");
              final String fromVersion = rs.getString("from_version");
              final String toVersion = rs.getString("to_version");
              final Direction direction = Direction.valueOf(rs.getString("direction"));
              // These two values are likely null.
              String upgradePackage = rs.getString("upgrade_package");
              String upgradeType = rs.getString("upgrade_type");

              LOG.info(MessageFormat.format("Populating rows for the upgrade table record with " +
                  "upgrade_id: {0,number,#}, cluster_id: {1,number,#}, from_version: {2}, to_version: {3}, direction: {4}",
                upgradeId, clusterId, fromVersion, toVersion, direction));

              // Set all upgrades that have been done so far to type "rolling"
              if (StringUtils.isEmpty(upgradeType)) {
                LOG.info("Updating the record's upgrade_type to " + UpgradeType.ROLLING);
                dbAccessor.executeQuery("UPDATE upgrade SET upgrade_type = '" + UpgradeType.ROLLING + "' WHERE upgrade_id = " + upgradeId);
              }

              if (StringUtils.isEmpty(upgradePackage)) {
                String version = null;
                StackEntity stack = null;

                if (direction == Direction.UPGRADE) {
                  version = toVersion;
                } else if (direction == Direction.DOWNGRADE) {
                  // TODO AMBARI-12698, this is going to be a problem.
                  // During a downgrade, the "to_version" is overwritten to the source version, but the "from_version"
                  // doesn't swap. E.g.,
                  //  upgrade_id | from_version |  to_version  | direction
                  // ------------+--------------+--------------+----------
                  //           1 | 2.2.6.0-2800 | 2.3.0.0-2557 | UPGRADE
                  //           2 | 2.2.6.0-2800 | 2.2.6.0-2800 | DOWNGRADE
                  version = fromVersion;
                }

                ClusterEntity cluster = clusterDAO.findById(clusterId);

                if (null != cluster) {
                  stack = cluster.getDesiredStack();
                  upgradePackage = calculateUpgradePackage(stack, version);
                } else {
                  LOG.error("Could not find a cluster with cluster_id " + clusterId);
                }

                if (!StringUtils.isEmpty(upgradePackage)) {
                  LOG.info("Updating the record's upgrade_package to " + upgradePackage);
                  dbAccessor.executeQuery("UPDATE upgrade SET upgrade_package = '" + upgradePackage + "' WHERE upgrade_id = " + upgradeId);
                } else {
                  success = false;
                  LOG.error("Unable to populate column upgrade_package for record in table upgrade with id " + upgradeId);
                }
              }
            }
          } catch (Exception e) {
            success = false;
            e.printStackTrace();
            LOG.error("Unable to populate the upgrade_type and upgrade_package columns of the upgrade table. " + e);
          }
        }
      }
    } catch (Exception e) {
      success = false;
      e.printStackTrace();
      LOG.error("Failed to retrieve records from the upgrade table to populate the upgrade_type and upgrade_package columns. Exception: " + e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (statement != null) {
          statement.close();
        }
      } catch (SQLException e) {
        ;
      }
    }
    return success;
  }

  /**
   * Find the single Repo Version for the given stack and version, and return its upgrade_package column.
   * Because the upgrade_package column is going to be removed from this entity, must use raw SQL
   * instead of the entity class.
   * @param stack Stack
   * @param version Stack version
   * @return The value of the upgrade_package column, or null if not found.
   */

  private String calculateUpgradePackage(StackEntity stack, String version) {
    String upgradePackage = null;
    // Find the corresponding repo_version, and extract its upgrade_package
    if (null != version && null != stack) {
      RepositoryVersionEntity repoVersion = repositoryVersionDAO.findByStackNameAndVersion(stack.getStackName(), version);

      Statement statement = null;
      ResultSet rs = null;
      try {
        statement = dbAccessor.getConnection().createStatement();
        if (statement != null) {
          // Need to use SQL since the schema is changing and the entity will no longer have the upgrade_package column.
          rs = statement.executeQuery("SELECT upgrade_package FROM repo_version WHERE repo_version_id = " + repoVersion.getId());
          if (rs != null && rs.next()) {
            upgradePackage = rs.getString("upgrade_package");
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to retrieve upgrade_package for repo_version record with id " + repoVersion.getId() + ". Exception: " + e.getMessage());
      } finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (statement != null) {
            statement.close();
          }
        } catch (SQLException e) {
          ;
        }
      }
    }
    return upgradePackage;
  }

  /**
   * If still on HDP 2.1, then no repo versions exist, so need to bootstrap the HDP 2.1 repo version,
   * and mark it as CURRENT in the cluster_version table for the cluster, as well as the host_version table
   * for all hosts.
   */
  @Transactional
  public void bootstrapRepoVersionForHDP21() throws AmbariException, SQLException {
    final String hardcodedInitialVersion = "2.1.0.0-0001";
    AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = amc.getAmbariMetaInfo();
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    RepositoryVersionHelper repositoryVersionHelper = injector.getInstance(RepositoryVersionHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    ClusterVersionDAO clusterVersionDAO = injector.getInstance(ClusterVersionDAO.class);
    HostVersionDAO hostVersionDAO = injector.getInstance(HostVersionDAO.class);

    Clusters clusters = amc.getClusters();
    if (clusters == null) {
      LOG.error("Unable to get Clusters entity.");
      return;
    }

    for (Cluster cluster : clusters.getClusters().values()) {
      ClusterEntity clusterEntity = clusterDAO.findByName(cluster.getClusterName());
      final StackId stackId = cluster.getCurrentStackVersion();
      LOG.info(MessageFormat.format("Analyzing cluster {0}, currently at stack {1} and version {2}",
        cluster.getClusterName(), stackId.getStackName(), stackId.getStackVersion()));

      if (stackId.getStackName().equalsIgnoreCase("HDP") && stackId.getStackVersion().equalsIgnoreCase("2.1")) {
        final StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
        StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

        LOG.info("Bootstrapping the versions since using HDP-2.1");

        // The actual value is not known, so use this.
        String displayName = stackId.getStackName() + "-" + hardcodedInitialVersion;

        // However, the Repo URLs should be correct.
        String operatingSystems = repositoryVersionHelper.serializeOperatingSystems(stackInfo.getRepositories());

        // Create the Repo Version if it doesn't already exist.
        RepositoryVersionEntity repoVersionEntity = repositoryVersionDAO.findByDisplayName(displayName);
        if (null != repoVersionEntity) {
          LOG.info(MessageFormat.format("A Repo Version already exists with Display Name: {0}", displayName));
        } else {
          final long repoVersionIdSeq = repositoryVersionDAO.findMaxId("id");
          // Safe to attempt to add the sequence if it doesn't exist already.
          addSequence("repo_version_id_seq", repoVersionIdSeq, false);

          repoVersionEntity = repositoryVersionDAO.create(
            stackEntity, hardcodedInitialVersion, displayName, operatingSystems);
          LOG.info(MessageFormat.format("Created Repo Version with ID: {0,number,#}\n, Display Name: {1}, Repo URLs: {2}\n",
            repoVersionEntity.getId(), displayName, operatingSystems));
        }

        // Create the Cluster Version if it doesn't already exist.
        ClusterVersionEntity clusterVersionEntity = clusterVersionDAO.findByClusterAndStackAndVersion(cluster.getClusterName(),
          stackId, hardcodedInitialVersion);

        if (null != clusterVersionEntity) {
          LOG.info(MessageFormat.format("A Cluster Version version for cluster: {0}, version: {1}, already exists; its state is {2}.",
            cluster.getClusterName(), clusterVersionEntity.getRepositoryVersion().getVersion(), clusterVersionEntity.getState()));

          // If there are not CURRENT cluster versions, make this one the CURRENT one.
          if (clusterVersionEntity.getState() != RepositoryVersionState.CURRENT &&
            clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).isEmpty()) {
            clusterVersionEntity.setState(RepositoryVersionState.CURRENT);
            clusterVersionDAO.merge(clusterVersionEntity);
          }
        } else {
          final long clusterVersionIdSeq = clusterVersionDAO.findMaxId("id");
          // Safe to attempt to add the sequence if it doesn't exist already.
          addSequence("cluster_version_id_seq", clusterVersionIdSeq, false);

          clusterVersionEntity = clusterVersionDAO.create(clusterEntity, repoVersionEntity, RepositoryVersionState.CURRENT,
            System.currentTimeMillis(), System.currentTimeMillis(), "admin");
          LOG.info(MessageFormat.format("Created Cluster Version with ID: {0,number,#}, cluster: {1}, version: {2}, state: {3}.",
            clusterVersionEntity.getId(), cluster.getClusterName(), clusterVersionEntity.getRepositoryVersion().getVersion(),
            clusterVersionEntity.getState()));
        }

        // Create the Host Versions if they don't already exist.
        Collection<HostEntity> hosts = clusterEntity.getHostEntities();
        boolean addedAtLeastOneHost = false;
        if (null != hosts && !hosts.isEmpty()) {
          for (HostEntity hostEntity : hosts) {
            HostVersionEntity hostVersionEntity = hostVersionDAO.findByClusterStackVersionAndHost(cluster.getClusterName(),
              stackId, hardcodedInitialVersion, hostEntity.getHostName());

            if (null != hostVersionEntity) {
              LOG.info(MessageFormat.format("A Host Version version for cluster: {0}, version: {1}, host: {2}, already exists; its state is {3}.",
                cluster.getClusterName(), hostVersionEntity.getRepositoryVersion().getVersion(),
                hostEntity.getHostName(), hostVersionEntity.getState()));

              if (hostVersionEntity.getState() != RepositoryVersionState.CURRENT &&
                hostVersionDAO.findByClusterHostAndState(cluster.getClusterName(), hostEntity.getHostName(),
                  RepositoryVersionState.CURRENT).isEmpty()) {
                hostVersionEntity.setState(RepositoryVersionState.CURRENT);
                hostVersionDAO.merge(hostVersionEntity);
              }
            } else {
              // This should only be done the first time.
              if (!addedAtLeastOneHost) {
                final long hostVersionIdSeq = hostVersionDAO.findMaxId("id");
                // Safe to attempt to add the sequence if it doesn't exist already.
                addSequence("host_version_id_seq", hostVersionIdSeq, false);
                addedAtLeastOneHost = true;
              }

              hostVersionEntity = new HostVersionEntity(hostEntity, repoVersionEntity, RepositoryVersionState.CURRENT);
              hostVersionDAO.create(hostVersionEntity);
              LOG.info(MessageFormat.format("Created Host Version with ID: {0,number,#}, cluster: {1}, version: {2}, host: {3}, state: {4}.",
                hostVersionEntity.getId(), cluster.getClusterName(), hostVersionEntity.getRepositoryVersion().getVersion(),
                hostEntity.getHostName(), hostVersionEntity.getState()));
            }
          }
        } else {
          LOG.info(MessageFormat.format("Not inserting any Host Version records since cluster {0} does not have any hosts.",
            cluster.getClusterName()));
        }
      }
    }
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
