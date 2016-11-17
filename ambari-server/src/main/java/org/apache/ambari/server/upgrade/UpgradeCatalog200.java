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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * Upgrade catalog for version 2.0.0.
 */
public class UpgradeCatalog200 extends AbstractUpgradeCatalog {

  private static final String ALERT_DEFINITION_TABLE = "alert_definition";
  private static final String ALERT_TARGET_TABLE = "alert_target";
  private static final String ALERT_TARGET_STATES_TABLE = "alert_target_states";
  private static final String ALERT_CURRENT_TABLE = "alert_current";
  private static final String ARTIFACT_TABLE = "artifact";
  private static final String KERBEROS_PRINCIPAL_TABLE = "kerberos_principal";
  private static final String KERBEROS_PRINCIPAL_HOST_TABLE = "kerberos_principal_host";
  private static final String TEZ_USE_CLUSTER_HADOOP_LIBS_PROPERTY = "tez.use.cluster.hadoop-libs";
  private static final String FLUME_ENV_CONFIG = "flume-env";
  private static final String CONTENT_PROPERTY = "content";

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.0.0";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger
      (UpgradeCatalog200.class);

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog200(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    prepareRollingUpgradesDDL();
    executeAlertDDLUpdates();
    createArtifactTable();
    createKerberosPrincipalTables();

    // add viewparameter columns
    dbAccessor.addColumn("viewparameter", new DBColumnInfo("label",
        String.class, 255, null, true));

    dbAccessor.addColumn("viewparameter", new DBColumnInfo("placeholder",
        String.class, 255, null, true));

    dbAccessor.addColumn("viewparameter", new DBColumnInfo("default_value",
        String.class, 2000, null, true));

    // add security_type to clusters
    dbAccessor.addColumn("clusters", new DBColumnInfo(
        "security_type", String.class, 32, SecurityType.NONE.toString(), false));

    // add security_state to various tables
    dbAccessor.addColumn("hostcomponentdesiredstate", new DBColumnInfo(
        "security_state", String.class, 32, SecurityState.UNSECURED.toString(), false));
    dbAccessor.addColumn("hostcomponentstate", new DBColumnInfo(
        "security_state", String.class, 32, SecurityState.UNSECURED.toString(), false));
    dbAccessor.addColumn("servicedesiredstate", new DBColumnInfo(
        "security_state", String.class, 32, SecurityState.UNSECURED.toString(), false));

    // Alter column : make viewinstanceproperty.value & viewinstancedata.value
    // nullable
    dbAccessor.alterColumn("viewinstanceproperty", new DBColumnInfo("value",
        String.class, 2000, null, true));
    dbAccessor.alterColumn("viewinstancedata", new DBColumnInfo("value",
        String.class, 2000, null, true));
  }

  /**
   * Execute all of the alert DDL updates.
   *
   * @throws AmbariException
   * @throws SQLException
   */
  private void executeAlertDDLUpdates() throws AmbariException, SQLException {
    // add ignore_host column to alert_definition
    dbAccessor.addColumn(ALERT_DEFINITION_TABLE, new DBColumnInfo(
            "ignore_host", Short.class, 1, 0, false));

    dbAccessor.addColumn(ALERT_DEFINITION_TABLE, new DBColumnInfo(
            "description", char[].class, 32672, null, true));

    // update alert target
    dbAccessor.addColumn(ALERT_TARGET_TABLE, new DBColumnInfo("is_global",
        Short.class, 1, 0, false));

    // create alert_target_states table
    ArrayList<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("target_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("alert_state", String.class, 255, null, false));
    dbAccessor.createTable(ALERT_TARGET_STATES_TABLE, columns);
    dbAccessor.addFKConstraint(ALERT_TARGET_STATES_TABLE,
        "fk_alert_tgt_states_tgt_id", "target_id", ALERT_TARGET_TABLE,
        "target_id", false);

    // update alert current maintenance mode
    dbAccessor.alterColumn(ALERT_CURRENT_TABLE, new DBColumnInfo(
        "maintenance_state", String.class, 255, null, false));
  }

  /**
   * Add any columns, tables, and keys needed for Rolling Upgrades.
   * @throws SQLException
   */
  private void prepareRollingUpgradesDDL() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<DBAccessor.DBColumnInfo>();

    columns.add(new DBColumnInfo("repo_version_id", Long.class,    null,  null, false));
    columns.add(new DBColumnInfo("stack",           String.class,  255,   null, false));
    columns.add(new DBColumnInfo("version",         String.class,  255,   null, false));
    columns.add(new DBColumnInfo("display_name",    String.class,  128,   null, false));
    columns.add(new DBColumnInfo("upgrade_package", String.class,  255,   null, false));
    columns.add(new DBColumnInfo("repositories",    char[].class,  null,  null, false));
    dbAccessor.createTable("repo_version", columns, "repo_version_id");
    addSequence("repo_version_id_seq", 0L, false);


    dbAccessor.addUniqueConstraint("repo_version", "UQ_repo_version_display_name", "display_name");
    dbAccessor.addUniqueConstraint("repo_version", "UQ_repo_version_stack_version", "stack", "version");

    // New columns
    dbAccessor.addColumn("hostcomponentstate", new DBAccessor.DBColumnInfo("upgrade_state",
        String.class, 32, "NONE", false));

    dbAccessor.addColumn("hostcomponentstate", new DBAccessor.DBColumnInfo("version",
        String.class, 32, "UNKNOWN", false));

    dbAccessor.addColumn("host_role_command", new DBAccessor.DBColumnInfo("retry_allowed",
        Integer.class, 1, 0, false));

    dbAccessor.addColumn("stage", new DBAccessor.DBColumnInfo("skippable",
        Integer.class, 1, 0, false));

    // New tables
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("repo_version_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("state", String.class, 32, null, false));
    columns.add(new DBAccessor.DBColumnInfo("start_time", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("end_time", Long.class, null, null, true));
    columns.add(new DBAccessor.DBColumnInfo("user_name", String.class, 32, null, true));
    dbAccessor.createTable("cluster_version", columns, "id");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("repo_version_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("host_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("state", String.class, 32, null, false));
    dbAccessor.createTable("host_version", columns, "id");

    // Foreign Key Constraints
    dbAccessor.addFKConstraint("cluster_version", "FK_cluster_version_cluster_id", "cluster_id", "clusters", "cluster_id", false);
    dbAccessor.addFKConstraint("cluster_version", "FK_cluster_version_repovers_id", "repo_version_id", "repo_version", "repo_version_id", false);
    if (dbAccessor.tableHasColumn("host_version", "host_name")) {
      dbAccessor.addFKConstraint("host_version", "FK_host_version_host_name", "host_name", "hosts", "host_name", false);
    }
    dbAccessor.addFKConstraint("host_version", "FK_host_version_repovers_id", "repo_version_id", "repo_version", "repo_version_id", false);

    // New sequences
    addSequence("cluster_version_id_seq", 0L, false);
    addSequence("host_version_id_seq", 0L, false);

    // upgrade tables
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("upgrade_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("request_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("from_version", String.class, 255, "", false));
    columns.add(new DBAccessor.DBColumnInfo("to_version", String.class, 255, "", false));
    columns.add(new DBAccessor.DBColumnInfo("direction", String.class, 255, "UPGRADE", false));
    dbAccessor.createTable("upgrade", columns, "upgrade_id");
    dbAccessor.addFKConstraint("upgrade", "fk_upgrade_cluster_id", "cluster_id", "clusters", "cluster_id", false);
    dbAccessor.addFKConstraint("upgrade", "fk_upgrade_request_id", "request_id", "request", "request_id", false);
    addSequence("upgrade_id_seq", 0L, false);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("upgrade_group_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("upgrade_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("group_name", String.class, 255, "", false));
    columns.add(new DBAccessor.DBColumnInfo("group_title", String.class, 1024, "", false));
    dbAccessor.createTable("upgrade_group", columns, "upgrade_group_id");
    dbAccessor.addFKConstraint("upgrade_group", "fk_upgrade_group_upgrade_id", "upgrade_id", "upgrade", "upgrade_id", false);
    addSequence("upgrade_group_id_seq", 0L, false);


    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("upgrade_item_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("upgrade_group_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("stage_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("state", String.class, 255, UpgradeState.NONE.name(), false));
    columns.add(new DBAccessor.DBColumnInfo("hosts", char[].class, 32672, null, true));
    columns.add(new DBAccessor.DBColumnInfo("tasks", char[].class, 32672, null, true));
    columns.add(new DBAccessor.DBColumnInfo("item_text", String.class, 1024, null, true));
    dbAccessor.createTable("upgrade_item", columns, "upgrade_item_id");
    dbAccessor.addFKConstraint("upgrade_item", "fk_upg_item_upgrade_group_id", "upgrade_group_id", "upgrade_group", "upgrade_group_id", false);
    addSequence("upgrade_item_id_seq", 0L, false);
  }

  private void createArtifactTable() throws SQLException {
    ArrayList<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("artifact_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("foreign_keys", String.class, 255, null, false));
    columns.add(new DBColumnInfo("artifact_data", char[].class, null, null, false));
    dbAccessor.createTable(ARTIFACT_TABLE, columns, "artifact_name", "foreign_keys");
  }

  private void createKerberosPrincipalTables() throws SQLException {
    ArrayList<DBColumnInfo> columns;

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("principal_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("is_service", Short.class, 1, 1, false));
    columns.add(new DBColumnInfo("cached_keytab_path", String.class, 255, null, true));
    dbAccessor.createTable(KERBEROS_PRINCIPAL_TABLE, columns, "principal_name");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("principal_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("host_name", String.class, 255, null, false));
    dbAccessor.createTable(KERBEROS_PRINCIPAL_HOST_TABLE, columns, "principal_name", "host_name");
    if (dbAccessor.tableHasColumn(KERBEROS_PRINCIPAL_HOST_TABLE, "host_name")) {
      dbAccessor.addFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_hostname", "host_name", "hosts", "host_name", true, false);
    }
    dbAccessor.addFKConstraint(KERBEROS_PRINCIPAL_HOST_TABLE, "FK_krb_pr_host_principalname", "principal_name", KERBEROS_PRINCIPAL_TABLE, "principal_name", true, false);
  }

  // ----- UpgradeCatalog ----------------------------------------------------
  /**
   * {@inheritDoc}
   */
  @Override
  public void executePreDMLUpdates() {
    ;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    // remove NAGIOS to make way for the new embedded alert framework
    removeNagiosService();
    addNewConfigurationsFromXml();
    updateHiveDatabaseType();
    updateTezConfiguration();
    updateFlumeEnvConfig();
    addMissingConfigs();
    persistHDPRepo();
    updateClusterEnvConfiguration();
  }

  protected void persistHDPRepo() throws AmbariException{
    AmbariManagementController amc = injector.getInstance(
            AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = amc.getAmbariMetaInfo();
    Map<String, Cluster> clusterMap = amc.getClusters().getClusters();
    for (Cluster cluster : clusterMap.values()) {
      StackId stackId = cluster.getCurrentStackVersion();
      String stackName = stackId.getStackName();
      String stackVersion = stackId.getStackVersion();
      String stackRepoId = stackName + "-" + stackVersion;

      for (OperatingSystemInfo osi : ambariMetaInfo.getOperatingSystems(stackName, stackVersion)) {
        MetainfoDAO metaInfoDAO = injector.getInstance(MetainfoDAO.class);
        String repoMetaKey = AmbariMetaInfo.generateRepoMetaKey(stackName,stackVersion,osi.getOsType(),
                stackRepoId,AmbariMetaInfo.REPOSITORY_XML_PROPERTY_BASEURL);
        // Check if default repo is used and not persisted
        if (metaInfoDAO.findByKey(repoMetaKey) == null) {
          RepositoryInfo repositoryInfo = ambariMetaInfo.getRepository(stackName, stackVersion, osi.getOsType(), stackRepoId);
          // We save default base url which has not changed during upgrade as base url
          String baseUrl = repositoryInfo.getDefaultBaseUrl();
          ambariMetaInfo.updateRepo(stackName, stackVersion, osi.getOsType(),
              stackRepoId, baseUrl, null);
        }
      }

      // Repositories that have been autoset may be unexpected for user
      // (especially if they are taken from online json)
      // We have to output to stdout here, and not to log
      // to be sure that user sees this message
      System.out.printf("Ambari has recorded the following repository base urls for cluster %s. Please verify the " +
              "values and ensure that these are correct. If necessary, " +
              "after starting Ambari Server, you can edit them using Ambari UI, " +
              "Admin -> Stacks and Versions -> Versions Tab and editing the base urls for the current Repo. " +
              "It is critical that these repo base urls are valid for your environment as they " +
              "will be used for Add Host/Service operations.",
        cluster.getClusterName());
      System.out.println(repositoryTable(ambariMetaInfo.getStack(stackName, stackVersion).getRepositories()));
    }

  }

  /**
   * Formats a list repositories for printing to console
   * @param repositories list of repositories
   * @return multi-line string
   */
  static String repositoryTable(List<RepositoryInfo> repositories) {
    StringBuilder result = new StringBuilder();
    for (RepositoryInfo repository : repositories) {
      result.append(String.format(" %8s |", repository.getOsType()));
      result.append(String.format(" %18s |", repository.getRepoId()));
      result.append(String.format(" %48s ", repository.getBaseUrl()));
      result.append("\n");
    }
    return result.toString();
  }

  protected void updateTezConfiguration() throws AmbariException {
    updateConfigurationProperties("tez-site", Collections.singletonMap(TEZ_USE_CLUSTER_HADOOP_LIBS_PROPERTY, String.valueOf(false)), false, false);
  }

  protected void updateFlumeEnvConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config flumeEnvConfig = cluster.getDesiredConfigByType(FLUME_ENV_CONFIG);
      if (flumeEnvConfig != null) {
        String content = flumeEnvConfig.getProperties().get(CONTENT_PROPERTY);
        if (content != null && !content.contains("/usr/lib/flume/lib/ambari-metrics-flume-sink.jar")) {
          String newPartOfContent = "\n\n" +
            "# Note that the Flume conf directory is always included in the classpath.\n" +
            "# Add flume sink to classpath\n" +
            "if [ -e \"/usr/lib/flume/lib/ambari-metrics-flume-sink.jar\" ]; then\n" +
            "  export FLUME_CLASSPATH=$FLUME_CLASSPATH:/usr/lib/flume/lib/ambari-metrics-flume-sink.jar\n" +
            "fi\n";
          content += newPartOfContent;
          Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
          updateConfigurationPropertiesForCluster(cluster, FLUME_ENV_CONFIG, updates, true, false);
        }
      }
    }
  }

  protected void updateHiveDatabaseType() throws AmbariException {
    final String PROPERTY_NAME = "hive_database_type";
    final String PROPERTY_VALUE_OLD = "postgresql";
    final String PROPERTY_VALUE_NEW = "postgres";
    final String PROPERTY_CONFIG_NAME = "hive-env";

    AmbariManagementController ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      Map<String, String> prop = new HashMap<String, String>();
      String hive_database_type = null;

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          hive_database_type = null;

          if (cluster.getDesiredConfigByType(PROPERTY_CONFIG_NAME) != null) {
            hive_database_type = cluster.getDesiredConfigByType(
                    PROPERTY_CONFIG_NAME).getProperties().get(PROPERTY_NAME);
          }

          if (hive_database_type != null && !hive_database_type.isEmpty() &&
                  hive_database_type.equals(PROPERTY_VALUE_OLD)) {
            prop.put(PROPERTY_NAME, PROPERTY_VALUE_NEW);
            updateConfigurationPropertiesForCluster(cluster, PROPERTY_CONFIG_NAME, prop, true, false);
          }
        }
      }

    }
  }

  /**
   * Removes Nagios and all associated components and states.
   */
  protected void removeNagiosService() {
    executeInTransaction(new RemoveNagiosRunnable());
  }

  /**
   * The RemoveNagiosRunnable is used to remove Nagios from the cluster. This
   * runnable is exepected to run inside of a transation so that if any of the
   * removals fails, Nagios is returned to a valid service state.
   */
  protected final class RemoveNagiosRunnable implements Runnable {

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      ClusterDAO clusterDao = injector.getInstance(ClusterDAO.class);
      ClusterServiceDAO clusterServiceDao = injector.getInstance(ClusterServiceDAO.class);
      ServiceComponentDesiredStateDAO componentDesiredStateDao = injector.getInstance(ServiceComponentDesiredStateDAO.class);
      ServiceDesiredStateDAO desiredStateDao = injector.getInstance(ServiceDesiredStateDAO.class);
      HostComponentDesiredStateDAO hostComponentDesiredStateDao = injector.getInstance(HostComponentDesiredStateDAO.class);
      HostComponentStateDAO hostComponentStateDao = injector.getInstance(HostComponentStateDAO.class);

      List<ClusterEntity> clusters = clusterDao.findAll();
      if (null == clusters) {
        return;
      }

      for (ClusterEntity cluster : clusters) {
        ClusterServiceEntity nagios = clusterServiceDao.findByClusterAndServiceNames(
            cluster.getClusterName(), "NAGIOS");

        if (null == nagios) {
          continue;
        }

        Collection<ServiceComponentDesiredStateEntity> serviceComponentDesiredStates = nagios.getServiceComponentDesiredStateEntities();
        ServiceDesiredStateEntity serviceDesiredState = nagios.getServiceDesiredStateEntity();

        // remove all component states
        for (ServiceComponentDesiredStateEntity componentDesiredState : serviceComponentDesiredStates) {
          Collection<HostComponentStateEntity> hostComponentStateEntities = componentDesiredState.getHostComponentStateEntities();
          Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities = componentDesiredState.getHostComponentDesiredStateEntities();

          // remove host states
          for (HostComponentStateEntity hostComponentState : hostComponentStateEntities) {
            hostComponentStateDao.remove(hostComponentState);
          }

          // remove host desired states
          for (HostComponentDesiredStateEntity hostComponentDesiredState : hostComponentDesiredStateEntities) {
            hostComponentDesiredStateDao.remove(hostComponentDesiredState);
          }

          // remove component state
          componentDesiredStateDao.removeByName(nagios.getClusterId(),
              componentDesiredState.getServiceName(), componentDesiredState.getComponentName());
        }

        // remove service state
        desiredStateDao.remove(serviceDesiredState);

        // remove service
        cluster.getClusterServiceEntities().remove(nagios);
        ClusterServiceEntityPK primaryKey = new ClusterServiceEntityPK();
        primaryKey.setClusterId(nagios.getClusterId());
        primaryKey.setServiceName(nagios.getServiceName());
        clusterServiceDao.removeByPK(primaryKey);
      }
    }
  }
  protected void addMissingConfigs() throws AmbariException {
    updateConfigurationProperties("hive-site", Collections.singletonMap("hive.server2.transport.mode", "binary"), false, false);
  }

  /**
   * Update the cluster-env configuration (in all clusters) to add missing properties and remove
   * obsolete properties.
   *
   * @throws org.apache.ambari.server.AmbariException
   */
  protected void updateClusterEnvConfiguration() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);

    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null) {
        for (final Cluster cluster : clusterMap.values()) {
          Config configClusterEnv = cluster.getDesiredConfigByType("cluster-env");

          if (configClusterEnv != null) {
            Map<String, String> properties = configClusterEnv.getProperties();

            if (properties != null) {
              // -----------------------------------------
              // Add missing properties

              if (!properties.containsKey("smokeuser_principal_name")) {
                // Add smokeuser_principal_name, from cluster-env/smokeuser
                // Ideally a realm should be added, but for now we can assume the default realm and
                // leave it off
                String smokeUser = properties.get("smokeuser");

                if ((smokeUser == null) || smokeUser.isEmpty()) {
                  // If the smokeuser property is not set in the current configuration set, grab
                  // it from the stack defaults:
                  Set<PropertyInfo> stackProperties = configHelper.getStackProperties(cluster);

                  if (stackProperties != null) {
                    for (PropertyInfo propertyInfo : stackProperties) {
                      String filename = propertyInfo.getFilename();

                      if ((filename != null) && "cluster-env".equals(ConfigHelper.fileNameToConfigType(filename))) {
                        smokeUser = propertyInfo.getValue();
                        break;
                      }
                    }
                  }

                  // If a default value for smokeuser was not found, force it to be "ambari-qa"
                  if ((smokeUser == null) || smokeUser.isEmpty()) {
                    smokeUser = "ambari-qa";
                  }
                }

                properties.put("smokeuser_principal_name", smokeUser);
              }

              // Add missing properties (end)
              // -----------------------------------------

              // -----------------------------------------
              // Remove obsolete properties

              // Remove obsolete properties (end)
              // -----------------------------------------

              // -----------------------------------------
              // Set the updated configuration

              configHelper.createConfigType(cluster, ambariManagementController, "cluster-env", properties,
                  AUTHENTICATED_USER_NAME, "Upgrading to Ambari 2.0");

              // Set configuration (end)
              // -----------------------------------------

            }
          }
        }
      }
    }
  }
}
