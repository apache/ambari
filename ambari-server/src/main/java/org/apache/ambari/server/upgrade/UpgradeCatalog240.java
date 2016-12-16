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

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.RemoteAmbariClusterDAO;
import org.apache.ambari.server.orm.dao.RequestScheduleDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntityEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.state.AlertFirmness;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.view.DefaultMasker;
import org.apache.ambari.view.ClusterType;
import org.apache.ambari.view.MaskException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

/**
 * Upgrade catalog for version 2.4.0.
 */
public class UpgradeCatalog240 extends AbstractUpgradeCatalog {

  protected static final String ADMIN_PERMISSION_TABLE = "adminpermission";
  protected static final String PRINCIPAL_ID_COL = "principal_id";
  protected static final String ALERT_DEFINITION_TABLE = "alert_definition";
  protected static final String ALERT_TARGET_TABLE = "alert_target";
  protected static final String ALERT_TARGET_ENABLED_COLUMN = "is_enabled";
  protected static final String ALERT_CURRENT_TABLE = "alert_current";
  protected static final String ALERT_CURRENT_OCCURRENCES_COLUMN = "occurrences";
  protected static final String ALERT_CURRENT_FIRMNESS_COLUMN = "firmness";
  protected static final String HELP_URL_COLUMN = "help_url";
  protected static final String REPEAT_TOLERANCE_COLUMN = "repeat_tolerance";
  protected static final String REPEAT_TOLERANCE_ENABLED_COLUMN = "repeat_tolerance_enabled";
  protected static final String PERMISSION_ID_COL = "permission_name";
  protected static final String SORT_ORDER_COL = "sort_order";
  protected static final String REPO_VERSION_TABLE = "repo_version";
  protected static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  protected static final String SERVICE_COMPONENT_DS_TABLE = "servicecomponentdesiredstate";
  protected static final String HOST_COMPONENT_DS_TABLE = "hostcomponentdesiredstate";
  protected static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  protected static final String SERVICE_COMPONENT_HISTORY_TABLE = "servicecomponent_history";
  protected static final String UPGRADE_TABLE = "upgrade";
  protected static final String STACK_TABLE = "stack";
  protected static final String CLUSTER_TABLE = "clusters";
  protected static final String CLUSTER_UPGRADE_ID_COLUMN = "upgrade_id";
  protected static final String YARN_ENV_CONFIG = "yarn-env";
  protected static final String CAPACITY_SCHEDULER_CONFIG = "capacity-scheduler";
  protected static final String WEBHCAT_SITE_CONFIG = "webhcat-site";
  protected static final String TEZ_SITE_CONFIG = "tez-site";
  protected static final String MAPRED_SITE_CONFIG = "mapred-site";
  public static final String DESIRED_VERSION_COLUMN_NAME = "desired_version";
  public static final String BLUEPRINT_SETTING_TABLE = "blueprint_setting";
  public static final String BLUEPRINT_NAME_COL = "blueprint_name";
  public static final String SETTING_NAME_COL = "setting_name";
  public static final String SETTING_DATA_COL = "setting_data";
  public static final String ID = "id";
  public static final String BLUEPRINT_TABLE = "blueprint";
  public static final String VIEWINSTANCE_TABLE = "viewinstance";
  public static final String SHORT_URL_COLUMN = "short_url";
  public static final String CLUSTER_HANDLE_COLUMN = "cluster_handle";
  public static final String REQUESTSCHEDULE_TABLE = "requestschedule";
  public static final String AUTHENTICATED_USER_ID_COLUMN = "authenticated_user_id";
  protected static final String CLUSTER_VERSION_TABLE = "cluster_version";
  protected static final String HOST_VERSION_TABLE = "host_version";
  protected static final String TOPOLOGY_REQUEST_TABLE = "topology_request";
  protected static final String PROVISION_ACTION_COL = "provision_action";
  protected static final String PHOENIX_QUERY_SERVER_PRINCIPAL_KEY = "phoenix.queryserver.kerberos.principal";
  protected static final String PHOENIX_QUERY_SERVER_KEYTAB_KEY = "phoenix.queryserver.keytab.file";
  protected static final String DEFAULT_CONFIG_VERSION = "version1";
  protected static final String SLIDER_SERVICE_NAME = "SLIDER";

  private static final String OOZIE_ENV_CONFIG = "oozie-env";
  private static final String SLIDER_CLIENT_CONFIG = "slider-client";
  private static final String HIVE_ENV_CONFIG = "hive-env";
  private static final String AMS_SITE = "ams-site";
  public static final String TIMELINE_METRICS_SINK_COLLECTION_PERIOD = "timeline.metrics.sink.collection.period";
  public static final String ONE_DIR_PER_PARITION_PROPERTY = "one_dir_per_partition";
  public static final String VIEWURL_TABLE = "viewurl";
  public static final String URL_ID_COLUMN = "url_id";
  private static final String PRINCIPAL_TYPE_TABLE = "adminprincipaltype";
  private static final String PRINCIPAL_TABLE = "adminprincipal";
  protected static final String HBASE_SITE_CONFIG = "hbase-site";
  protected static final String HBASE_SPNEGO_PRINCIPAL_KEY = "hbase.security.authentication.spnego.kerberos.principal";
  protected static final String HBASE_SPNEGO_KEYTAB_KEY = "hbase.security.authentication.spnego.kerberos.keytab";
  protected static final String EXTENSION_TABLE = "extension";
  protected static final String EXTENSION_ID_COLUMN = "extension_id";
  protected static final String EXTENSION_LINK_TABLE = "extensionlink";
  protected static final String EXTENSION_LINK_ID_COLUMN = "link_id";
  protected static final String KAFKA_BROKER_CONFIG = "kafka-broker";

  private static final Map<String, Integer> ROLE_ORDER;
  private static final String AMS_HBASE_SITE = "ams-hbase-site";
  private static final String HBASE_RPC_TIMEOUT_PROPERTY = "hbase.rpc.timeout";
  private static final String AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY = "hbase.normalizer.enabled";
  public static final String PRECISION_TABLE_TTL_PROPERTY = "timeline.metrics.host.aggregator.ttl";
  public static final String CLUSTER_SECOND_TABLE_TTL_PROPERTY = "timeline.metrics.cluster.aggregator.second.ttl";

  static {
    // Manually create role order since there really isn't any mechanism for this
    ROLE_ORDER = new HashMap<String, Integer>();
    ROLE_ORDER.put("AMBARI.ADMINISTRATOR", 1);
    ROLE_ORDER.put("CLUSTER.ADMINISTRATOR", 2);
    ROLE_ORDER.put("CLUSTER.OPERATOR", 3);
    ROLE_ORDER.put("SERVICE.ADMINISTRATOR", 4);
    ROLE_ORDER.put("SERVICE.OPERATOR", 5);
    ROLE_ORDER.put("CLUSTER.USER", 6);
  }

  @Inject
  UserDAO userDAO;

  @Inject
  PermissionDAO permissionDAO;

  @Inject
  PrivilegeDAO privilegeDAO;

  @Inject
  ResourceTypeDAO resourceTypeDAO;

  @Inject
  ClusterDAO clusterDAO;

  @Inject
  PrincipalTypeDAO principalTypeDAO;

  @Inject
  PrincipalDAO principalDAO;

  @Inject
  RequestScheduleDAO requestScheduleDAO;

  @Inject
  Users users;

  @Inject
  Configuration config;

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog240.class);

  private static final String SETTING_TABLE = "setting";

  protected static final String SERVICE_COMPONENT_DESIRED_STATE_TABLE = "servicecomponentdesiredstate";
  protected static final String RECOVERY_ENABLED_COL = "recovery_enabled";

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog240(Injector injector) {
    super(injector);
    injector.injectMembers(this);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.4.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.3.0";
  }

  public static final String CLUSTER_TYPE_COLUMN = "cluster_type";
  public static final String REMOTE_AMBARI_CLUSTER_TABLE = "remoteambaricluster";
  public static final String REMOTE_AMBARI_CLUSTER_SERVICE_TABLE = "remoteambariclusterservice";

  public static final String CLUSTER_ID = "cluster_id";
  public static final String SERVICE_NAME = "service_name";
  public static final String CLUSTER_NAME = "name";


  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateAdminPermissionTable();
    updateServiceComponentDesiredStateTable();
    createExtensionTable();
    createExtensionLinkTable();
    createSettingTable();
    updateRepoVersionTableDDL();
    updateServiceComponentDesiredStateTableDDL();
    createServiceComponentHistoryTable();
    updateClusterTableDDL();
    updateAlertDefinitionTable();
    updateAlertCurrentTable();
    updateAlertTargetTable();
    createBlueprintSettingTable();
    updateHostRoleCommandTableDDL();
    createViewUrlTableDDL();
    updateViewInstanceEntityTable();
    createRemoteClusterTable();
    updateViewInstanceTable();
    updateRequestScheduleEntityTable();
    updateTopologyRequestTable();
  }

  private void createRemoteClusterTable() throws SQLException {

    List<DBColumnInfo> columns = new ArrayList<>();
    LOG.info("Creating {} table", REMOTE_AMBARI_CLUSTER_TABLE);
    columns.add(new DBColumnInfo(CLUSTER_ID, Long.class, null, null, false));
    columns.add(new DBColumnInfo(CLUSTER_NAME, String.class, 255, null, false));
    columns.add(new DBColumnInfo("url", String.class, 255, null, false));
    columns.add(new DBColumnInfo("username", String.class, 255, null, false));
    columns.add(new DBColumnInfo("password", String.class, 255, null, false));
    dbAccessor.createTable(REMOTE_AMBARI_CLUSTER_TABLE, columns, CLUSTER_ID);
    dbAccessor.addUniqueConstraint(REMOTE_AMBARI_CLUSTER_TABLE , "UQ_remote_ambari_cluster" , CLUSTER_NAME);
    addSequence("remote_cluster_id_seq", 1L, false);

    List<DBColumnInfo> remoteClusterServiceColumns = new ArrayList<>();
    LOG.info("Creating {} table", REMOTE_AMBARI_CLUSTER_SERVICE_TABLE);
    remoteClusterServiceColumns.add(new DBColumnInfo(ID, Long.class, null, null, false));
    remoteClusterServiceColumns.add(new DBColumnInfo(SERVICE_NAME, String.class, 255, null, false));
    remoteClusterServiceColumns.add(new DBColumnInfo(CLUSTER_ID, Long.class, null, null, false));
    dbAccessor.createTable(REMOTE_AMBARI_CLUSTER_SERVICE_TABLE, remoteClusterServiceColumns, ID);
    dbAccessor.addFKConstraint(REMOTE_AMBARI_CLUSTER_SERVICE_TABLE, "FK_remote_ambari_cluster_id",
      CLUSTER_ID, REMOTE_AMBARI_CLUSTER_TABLE, CLUSTER_ID, false);
    addSequence("remote_cluster_service_id_seq", 1L, false);

  }

  private void createViewUrlTableDDL() throws SQLException {
    List<DBColumnInfo> columns = new ArrayList<>();

    //  Add setting table
    LOG.info("Creating " + VIEWURL_TABLE + " table");

    columns.add(new DBColumnInfo(URL_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBColumnInfo("url_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("url_suffix", String.class, 255, null, false));
    dbAccessor.createTable(VIEWURL_TABLE, columns, URL_ID_COLUMN);
    addSequence("viewurl_id_seq", 1L, false);
  }

  private void updateViewInstanceEntityTable() throws SQLException {
    dbAccessor.addColumn(VIEWINSTANCE_TABLE,
      new DBColumnInfo(SHORT_URL_COLUMN, Long.class, null, null, true));
    dbAccessor.addFKConstraint(VIEWINSTANCE_TABLE, "FK_instance_url_id",
      SHORT_URL_COLUMN, VIEWURL_TABLE, URL_ID_COLUMN, false);
    dbAccessor.addColumn(VIEWINSTANCE_TABLE,
      new DBColumnInfo(CLUSTER_TYPE_COLUMN, String.class, 100, ClusterType.LOCAL_AMBARI.name(), false));
  }

  private void updateRequestScheduleEntityTable() throws SQLException {
    dbAccessor.addColumn(REQUESTSCHEDULE_TABLE,
      new DBColumnInfo(AUTHENTICATED_USER_ID_COLUMN, Integer.class, null, null, true));
  }

  private void updateClusterTableDDL() throws SQLException {
    dbAccessor.addColumn(CLUSTER_TABLE, new DBColumnInfo(CLUSTER_UPGRADE_ID_COLUMN, Long.class, null, null, true));

    dbAccessor.addFKConstraint(CLUSTER_TABLE, "FK_clusters_upgrade_id",
      CLUSTER_UPGRADE_ID_COLUMN, UPGRADE_TABLE, "upgrade_id", false);
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    updateAlerts();
    setRoleSortOrder();
    addSettingPermission();
    addViewOperationalLogsPermission();
    addManageUserPersistedDataPermission();
    allowClusterOperatorToManageCredentials();
    updateHDFSConfigs();
    updateKAFKAConfigs();
    updateHIVEConfigs();
    updateAMSConfigs();
    updateClusterEnv();
    updateSequenceForView();
    updateHostRoleCommandTableDML();
    updateKerberosConfigs();
    updateYarnEnv();
    updatePhoenixConfigs();
    updateSparkConfigs();
    updateHBaseConfigs();
    updateFalconConfigs();
    updateKerberosDescriptorArtifacts();
    removeHiveOozieDBConnectionConfigs();
    updateClustersAndHostsVersionStateTableDML();
    removeStandardDeviationAlerts();
    removeAtlasMetaserverAlert();
    updateClusterInheritedPermissionsConfig();
    consolidateUserRoles();
    createRolePrincipals();
    updateHDFSWidgetDefinition();
    updateTezViewProperty();
    upgradeCapSchedulerView();
    fixAuthorizationDescriptions();
    removeAuthorizations();
    addConnectionTimeoutParamForWebAndMetricAlerts();
    addSliderClientConfig();
    updateRequestScheduleEntityUserIds();
    updateRecoveryConfigurationDML();
    updatePigSmokeTestEntityClass();
    updateRangerHbasePluginProperties();
    adjustHiveJobTimestamps();
  }

  /**
   * Populates authenticated_user_id field by correct user id calculated from user name
   * @throws SQLException
   */
  protected void updateRequestScheduleEntityUserIds() throws SQLException {
    List<RequestScheduleEntity> requestScheduleEntities = requestScheduleDAO.findAll();
    for (RequestScheduleEntity requestScheduleEntity : requestScheduleEntities) {
      String createdUserName = requestScheduleEntity.getCreateUser();

      if (createdUserName != null) {
        User user = users.getUserIfUnique(createdUserName);

        if (user != null && StringUtils.equals(user.getUserName(), createdUserName)) {
          requestScheduleEntity.setAuthenticatedUserId(user.getUserId());
          requestScheduleDAO.merge(requestScheduleEntity);
        }
      }
    }
  }

  protected void updateClusterInheritedPermissionsConfig() throws SQLException {
    insertClusterInheritedPrincipal("ALL.CLUSTER.ADMINISTRATOR");
    insertClusterInheritedPrincipal("ALL.CLUSTER.OPERATOR");
    insertClusterInheritedPrincipal("ALL.CLUSTER.USER");
    insertClusterInheritedPrincipal("ALL.SERVICE.ADMINISTRATOR");
    insertClusterInheritedPrincipal("ALL.SERVICE.OPERATIOR");
  }

  private void insertClusterInheritedPrincipal(String name) {
    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();
    principalTypeEntity.setName(name);
    principalTypeEntity = principalTypeDAO.merge(principalTypeEntity);

    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalDAO.create(principalEntity);
  }
  private static final String NAME_PREFIX = "DS_";

  private String getEntityName(ViewEntityEntity entity) {
    String className = entity.getClassName();
    String[] parts = className.split("\\.");
    String simpleClassName = parts[parts.length - 1];

    if (entity.getViewInstance().alterNames()) {
      return NAME_PREFIX + simpleClassName + "_" + entity.getId();
    }
    return simpleClassName + entity.getId();
  }

  /**
   * get all entries of viewentity
   * find all the table names by parsing class_name
   * create all the sequence names by appending _id_seq
   * query each dynamic table to find the max of id
   * insert into ambari_sequence name and counter for each item
   */
  protected void updateSequenceForView() {
    LOG.info("updateSequenceForView called.");
    EntityManager entityManager = getEntityManagerProvider().get();
    TypedQuery<ViewEntityEntity> viewEntityQuery = entityManager.createQuery("SELECT vee FROM ViewEntityEntity vee", ViewEntityEntity.class);
    List<ViewEntityEntity> viewEntities = viewEntityQuery.getResultList();
    LOG.info("Received view Entities : {}, length : {}", viewEntities, viewEntities.size());

    // as the id fields are string in these entities we will have to get all ids and convert to int and find max.
    String selectIdsFormat = "select %s from %s";
    String insertQuery = "insert into ambari_sequences values ('%s',%d)";
    for (ViewEntityEntity viewEntity : viewEntities) {
      LOG.info("Working with viewEntity : {} : {} ", viewEntity, viewEntity.getViewName() + viewEntity.getViewInstance());
      String tableName = getEntityName(viewEntity);
      String seqName = tableName.toLowerCase() + "_id_seq";
      try {
        entityManager.getTransaction().begin();
        String selectIdsQueryString = String.format(selectIdsFormat, NAME_PREFIX + viewEntity.getIdProperty(), tableName).toLowerCase();
        LOG.info("executing max query string {}", selectIdsQueryString);
        Query selectIdsQuery = entityManager.createNativeQuery(selectIdsQueryString);
        List<String> ids = selectIdsQuery.getResultList();
        LOG.info("Received ids : {}", ids);
        int maxId = 0;
        if (null != ids && ids.size() != 0) {
          for (String id : ids) {
            try {
              Integer intId = Integer.parseInt(id);
              maxId = Math.max(intId, maxId);
            } catch (NumberFormatException e) {
              LOG.error("the id was non integer : id : {}. So ignoring.", id);
            }
          }
        }

        String insertQueryString = String.format(insertQuery, seqName, maxId).toLowerCase();
        LOG.info("Executing insert query : {}", insertQueryString);
        Query insertQ = entityManager.createNativeQuery(insertQueryString);
        int rowsChanged = insertQ.executeUpdate();
        entityManager.getTransaction().commit();
        LOG.info("executing insert resulted in {} row changes.", rowsChanged);
      } catch (Exception e) { // when the entity table is not yet created or other exception.
        entityManager.getTransaction().rollback();
        LOG.info("Error (can be ignored) {}", e.getMessage());
        LOG.debug("Exception occured while updating : {}",viewEntity.getViewName() + viewEntity.getViewInstance(), e);
      }
    }
  }


  /**
   * get all entries of viewentity
   * find all the table names by parsing class_name
   * update jobimpls creation timestamp * 1000
   */
  protected void adjustHiveJobTimestamps() {
    LOG.info("updateSequenceForView called.");
    EntityManager entityManager = getEntityManagerProvider().get();
    TypedQuery<ViewEntityEntity> viewEntityQuery = entityManager.createQuery("SELECT vee FROM ViewEntityEntity vee where vee.className = 'org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl'", ViewEntityEntity.class);
    List<ViewEntityEntity> viewEntities = viewEntityQuery.getResultList();
    LOG.info("Received JobImpl view Entities : {}, length : {}", viewEntities, viewEntities.size());

    String selectIdsFormat = "update %s set ds_datesubmitted = ds_datesubmitted * 1000";
    for (ViewEntityEntity viewEntity : viewEntities) {
      LOG.info("Working with JobImpl viewEntity : {} : {} ", viewEntity, viewEntity.getViewName() + ":" + viewEntity.getViewInstanceName() + ":" + viewEntity.getClassName());
      String tableName = getEntityName(viewEntity);
      try {
        entityManager.getTransaction().begin();
        String updatesQueryString = String.format(selectIdsFormat, tableName).toLowerCase();
        LOG.info("executing update query string for jobimpl {}", updatesQueryString);
        Query updateQuery = entityManager.createNativeQuery(updatesQueryString);
        int rowsChanged = updateQuery.executeUpdate();
        entityManager.getTransaction().commit();
        LOG.info("executing update on jobimpl resulted in {} row changes.", rowsChanged);
      } catch (Exception e) { // when the entity table is not yet created or other exception.
        entityManager.getTransaction().rollback();
        LOG.info("Error (can be ignored) {}", e.getMessage());
        LOG.debug("Exception occured while updating : {}",viewEntity.getViewName() + viewEntity.getViewInstance(), e);
      }
    }
  }

  private void createExtensionTable() throws SQLException {
    List<DBColumnInfo> columns = new ArrayList<>();

    // Add extension table
    LOG.info("Creating " + EXTENSION_TABLE + " table");

    columns.add(new DBColumnInfo(EXTENSION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBColumnInfo("extension_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("extension_version", String.class, 255, null, false));
    dbAccessor.createTable(EXTENSION_TABLE, columns, EXTENSION_ID_COLUMN);

    // create UNIQUE constraint, ensuring column order matches SQL files
    String[] uniqueColumns = new String[] { "extension_name", "extension_version" };
    dbAccessor.addUniqueConstraint(EXTENSION_TABLE, "UQ_extension", uniqueColumns);

    addSequence("extension_id_seq", 0L, false);
  }

  private void createExtensionLinkTable() throws SQLException {
    List<DBColumnInfo> columns = new ArrayList<>();

    // Add extension link table
    LOG.info("Creating " + EXTENSION_LINK_TABLE + " table");

    columns.add(new DBColumnInfo(EXTENSION_LINK_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBColumnInfo("stack_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo(EXTENSION_ID_COLUMN, Long.class, null, null, false));
    dbAccessor.createTable(EXTENSION_LINK_TABLE, columns, EXTENSION_LINK_ID_COLUMN);

    // create UNIQUE constraint, ensuring column order matches SQL files
    String[] uniqueColumns = new String[] { "stack_id", EXTENSION_ID_COLUMN };
    dbAccessor.addUniqueConstraint(EXTENSION_LINK_TABLE, "UQ_extension_link", uniqueColumns);

    dbAccessor.addFKConstraint(EXTENSION_LINK_TABLE, "FK_extensionlink_extension_id",
      EXTENSION_ID_COLUMN, EXTENSION_TABLE, EXTENSION_ID_COLUMN, false);

    dbAccessor.addFKConstraint(EXTENSION_LINK_TABLE, "FK_extensionlink_stack_id",
      "stack_id", STACK_TABLE, "stack_id", false);

    addSequence("link_id_seq", 0L, false);
  }

  private void createSettingTable() throws SQLException {
    List<DBColumnInfo> columns = new ArrayList<>();

    //  Add setting table
    LOG.info("Creating " + SETTING_TABLE + " table");

    columns.add(new DBColumnInfo(ID, Long.class, null, null, false));
    columns.add(new DBColumnInfo("name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("setting_type", String.class, 255, null, false));
    columns.add(new DBColumnInfo("content", String.class, 3000, null, false));
    columns.add(new DBColumnInfo("updated_by", String.class, 255, "_db", false));
    columns.add(new DBColumnInfo("update_timestamp", Long.class, null, null, false));
    dbAccessor.createTable(SETTING_TABLE, columns, ID);
    addSequence("setting_id_seq", 0L, false);
  }

  protected void addSettingPermission() throws SQLException {
    addRoleAuthorization("AMBARI.MANAGE_SETTINGS", "Manage settings", Collections.singleton("AMBARI.ADMINISTRATOR:AMBARI"));
  }

  protected void addViewOperationalLogsPermission() throws SQLException {
    Collection<String> roles = Arrays.asList(
        "AMBARI.ADMINISTRATOR:AMBARI",
        "CLUSTER.ADMINISTRATOR:CLUSTER",
        "CLUSTER.OPERATOR:CLUSTER",
        "SERVICE.ADMINISTRATOR:CLUSTER");

    addRoleAuthorization("SERVICE.VIEW_OPERATIONAL_LOGS", "View service operational logs", roles);
  }

  /**
   * Add 'MANAGE_USER_PERSISTED_DATA' permissions for CLUSTER.ADMINISTRATOR, SERVICE.OPERATOR, SERVICE.ADMINISTRATOR,
   * CLUSTER.OPERATOR, AMBARI.ADMINISTRATOR.
   *
   */
  protected void addManageUserPersistedDataPermission() throws SQLException {
    Collection<String> roles = Arrays.asList(
        "AMBARI.ADMINISTRATOR:AMBARI",
        "CLUSTER.ADMINISTRATOR:CLUSTER",
        "CLUSTER.OPERATOR:CLUSTER",
        "SERVICE.ADMINISTRATOR:CLUSTER",
        "SERVICE.OPERATOR:CLUSTER",
        "CLUSTER.USER:CLUSTER");

    addRoleAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA", "Manage cluster-level user persisted data", roles);
  }

  /**
   * Adds <code>CLUSTER.MANAGE_CREDENTIALS</code> to the set of authorizations a <code>CLUSTER.OPERATOR</code> can perform.
   *
   * @throws SQLException
   */
  protected void allowClusterOperatorToManageCredentials() throws SQLException {
    addAuthorizationToRole("CLUSTER.OPERATOR", "CLUSTER", "CLUSTER.MANAGE_CREDENTIAL");
  }

  protected void removeHiveOozieDBConnectionConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(ambariManagementController.getClusters());

    for (final Cluster cluster : clusterMap.values()) {
      Config oozieEnv = cluster.getDesiredConfigByType(OOZIE_ENV_CONFIG);
      if(oozieEnv != null) {
        Map<String, String> oozieEnvProperties = oozieEnv.getProperties();
        Set<String> removePropertiesSet = new HashSet<>();
        if (oozieEnvProperties.containsKey("oozie_derby_database")) {
          LOG.info("Removing property oozie_derby_database from " + OOZIE_ENV_CONFIG);
          removePropertiesSet.add("oozie_derby_database");
        }
        if (oozieEnvProperties.containsKey("oozie_hostname")) {
          LOG.info("Removing property oozie_hostname from " + OOZIE_ENV_CONFIG);
          removePropertiesSet.add("oozie_hostname");
        }
        if (!removePropertiesSet.isEmpty()) {
          removeConfigurationPropertiesFromCluster(cluster, OOZIE_ENV_CONFIG, removePropertiesSet);
        }
      }

      Config hiveEnv = cluster.getDesiredConfigByType(HIVE_ENV_CONFIG);
      if(hiveEnv != null) {
        Map<String, String> hiveEnvProperties = hiveEnv.getProperties();
        if (hiveEnvProperties.containsKey("hive_hostname")) {
          LOG.info("Removing property hive_hostname from " + HIVE_ENV_CONFIG);
          removeConfigurationPropertiesFromCluster(cluster, HIVE_ENV_CONFIG, Collections.singleton("hive_hostname"));
        }
      }
    }
  }

  protected void addSliderClientConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    ConfigHelper configHelper = ambariManagementController.getConfigHelper();
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);

    for (final Cluster cluster : clusterMap.values()) {
      Set<String> installedServices = cluster.getServices().keySet();
      if (installedServices.contains(SLIDER_SERVICE_NAME)) {
        Config sliderClientConfig = cluster.getDesiredConfigByType(SLIDER_CLIENT_CONFIG);
        if (sliderClientConfig == null) {
          configHelper.createConfigType(cluster, ambariManagementController, SLIDER_CLIENT_CONFIG,
                  new HashMap<String, String>(), AUTHENTICATED_USER_NAME, "");
        }
      }
    }
  }

  protected void updateAlerts() {
    // map of alert_name -> property_name -> visibility_value
    final Map<String, String> hdfsVisibilityMap = new HashMap<String, String>(){{
      put("mergeHaMetrics", "HIDDEN");
      put("appId", "HIDDEN");
      put("metricName", "HIDDEN");
    }};
    final Map<String, String> defaultKeytabVisibilityMap = new HashMap<String, String>(){{
      put("default.smoke.principal", "HIDDEN");
      put("default.smoke.keytab", "HIDDEN");
    }};

    final Map<String, String> percentParameterMap = new HashMap<String, String>(){{
      put("units", "%");
      put("type", "PERCENT");
    }};

    Map<String, Map<String, String>> visibilityMap = new HashMap<String, Map<String, String>>(){{
      put("hive_webhcat_server_status", new HashMap<String, String>(){{
        put("default.smoke.user", "HIDDEN");
      }});
      put("hive_metastore_process", defaultKeytabVisibilityMap);
      put("hive_server_process", defaultKeytabVisibilityMap);
      put("zookeeper_server_process", new HashMap<String, String>(){{
        put("socket.command", "HIDDEN");
        put("socket.command.response", "HIDDEN");
      }});
    }};

    Map<String, Map<String, String>> reportingPercentMap = new HashMap<String, Map<String, String>>(){{
      put("hawq_segment_process_percent", percentParameterMap);
      put("mapreduce_history_server_cpu", percentParameterMap);
      put("yarn_nodemanager_webui_percent", percentParameterMap);
      put("yarn_resourcemanager_cpu", percentParameterMap);
      put("datanode_process_percent", percentParameterMap);
      put("datanode_storage_percent", percentParameterMap);
      put("journalnode_process_percent", percentParameterMap);
      put("namenode_cpu", percentParameterMap);
      put("namenode_hdfs_capacity_utilization", percentParameterMap);
      put("datanode_storage", percentParameterMap);
      put("datanode_heap_usage", percentParameterMap);
      put("storm_supervisor_process_percent", percentParameterMap);
      put("hbase_regionserver_process_percent", percentParameterMap);
      put("hbase_master_cpu", percentParameterMap);
      put("zookeeper_server_process_percent", percentParameterMap);
      put("metrics_monitor_process_percent", percentParameterMap);
      put("ams_metrics_collector_hbase_master_cpu", percentParameterMap);
    }};

    Map<String, Map<String, Integer>> reportingMultiplierMap = new HashMap<String, Map<String, Integer>>(){{
      put("hawq_segment_process_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
      put("yarn_nodemanager_webui_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
      put("datanode_process_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
      put("datanode_storage_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
      put("journalnode_process_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
      put("storm_supervisor_process_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
      put("hbase_regionserver_process_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
      put("zookeeper_server_process_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
      put("metrics_monitor_process_percent", new HashMap<String, Integer>() {{
        put("warning", 100);
        put("critical", 100);
      }});
    }};

    Map<String, Map<String, Integer>> scriptAlertMultiplierMap = new HashMap<String, Map<String, Integer>>(){{
      put("ambari_agent_disk_usage", new HashMap<String, Integer>() {{
        put("percent.used.space.warning.threshold", 100);
        put("percent.free.space.critical.threshold", 100);
      }});
      put("namenode_last_checkpoint", new HashMap<String, Integer>() {{
        put("checkpoint.time.warning.threshold", 100);
        put("checkpoint.time.critical.threshold", 100);
      }});
    }};

    String newNameservicePropertyValue = "{{hdfs-site/dfs.internal.nameservices}}";
    final Set<String> alertNamesForNameserviceUpdate = new HashSet<String>() {{
      add("namenode_webui");
      add("namenode_hdfs_blocks_health");
      add("namenode_hdfs_pending_deletion_blocks");
      add("namenode_rpc_latency");
      add("namenode_directory_status");
      add("datanode_health_summary");
      add("namenode_cpu");
      add("namenode_hdfs_capacity_utilization");
    }};

    // list of alerts that need to get property updates
    Set<String> alertNamesForPropertyUpdates = new HashSet<String>() {{
      add("hawq_segment_process_percent");
      add("mapreduce_history_server_cpu");
      add("yarn_nodemanager_webui_percent");
      add("yarn_resourcemanager_cpu");
      add("datanode_process_percent");
      add("datanode_storage_percent");
      add("journalnode_process_percent");
      add("namenode_cpu");
      add("namenode_hdfs_capacity_utilization");
      add("datanode_storage");
      add("datanode_heap_usage");
      add("storm_supervisor_process_percent");
      add("hbase_regionserver_process_percent");
      add("hbase_master_cpu");
      add("zookeeper_server_process_percent");
      add("metrics_monitor_process_percent");
      add("ams_metrics_collector_hbase_master_cpu");
      add("ambari_agent_disk_usage");
      add("namenode_last_checkpoint");
      addAll(alertNamesForNameserviceUpdate);
    }};

    // list of alerts to be removed
    Set<String> alertForRemoval = new HashSet<String>() {{
      add("storm_rest_api");
      add("mapreduce_history_server_process");
    }};

    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterID = cluster.getClusterId();

      // here goes alerts that need get new properties
      final AlertDefinitionEntity namenodeLastCheckpointAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "namenode_last_checkpoint");
      final AlertDefinitionEntity namenodeHAHealthAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "namenode_ha_health");
      final AlertDefinitionEntity nodemanagerHealthAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "yarn_nodemanager_health");
      final AlertDefinitionEntity nodemanagerHealthSummaryAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "nodemanager_health_summary");
      final AlertDefinitionEntity hiveMetastoreProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_metastore_process");
      final AlertDefinitionEntity hiveServerProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_server_process");
      final AlertDefinitionEntity hiveWebhcatServerStatusAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_webhcat_server_status");
      final AlertDefinitionEntity flumeAgentStatusAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "flume_agent_status");
      final AlertDefinitionEntity zookeeperServerProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "zookeeper_server_process");

      Map<AlertDefinitionEntity, List<String>> alertDefinitionParams = new HashMap<>();
      checkedPutToMap(alertDefinitionParams, namenodeLastCheckpointAlertDefinitionEntity,
              Lists.newArrayList("connection.timeout", "checkpoint.time.warning.threshold",
                "checkpoint.time.critical.threshold", "checkpoint.txns.multiplier.warning.threshold",
                "checkpoint.txns.multiplier.critical.threshold"));
      checkedPutToMap(alertDefinitionParams, namenodeHAHealthAlertDefinitionEntity,
        Lists.newArrayList("connection.timeout"));
      checkedPutToMap(alertDefinitionParams, nodemanagerHealthAlertDefinitionEntity,
              Lists.newArrayList("connection.timeout"));
      checkedPutToMap(alertDefinitionParams, nodemanagerHealthSummaryAlertDefinitionEntity,
              Lists.newArrayList("connection.timeout"));
      checkedPutToMap(alertDefinitionParams, hiveMetastoreProcessAlertDefinitionEntity,
              Lists.newArrayList("default.smoke.user", "default.smoke.principal", "default.smoke.keytab"));
      checkedPutToMap(alertDefinitionParams, hiveServerProcessAlertDefinitionEntity,
              Lists.newArrayList("default.smoke.user", "default.smoke.principal", "default.smoke.keytab"));
      checkedPutToMap(alertDefinitionParams, hiveWebhcatServerStatusAlertDefinitionEntity,
              Lists.newArrayList("default.smoke.user", "connection.timeout"));
      checkedPutToMap(alertDefinitionParams, flumeAgentStatusAlertDefinitionEntity,
              Lists.newArrayList("run.directory"));
      checkedPutToMap(alertDefinitionParams, zookeeperServerProcessAlertDefinitionEntity,
              Lists.newArrayList("socket.command", "socket.command.response"));


      Map<Long, AlertDefinitionEntity> definitionsForPropertyUpdates = new HashMap<>();

      // adding new properties
      for (Map.Entry<AlertDefinitionEntity, List<String>> entry : alertDefinitionParams.entrySet()){
        AlertDefinitionEntity alertDefinition = entry.getKey();
        String source = alertDefinition.getSource();
        alertDefinition.setSource(addParam(source, entry.getValue()));
        definitionsForPropertyUpdates.put(alertDefinition.getDefinitionId(), alertDefinition);
      }

      // here goes alerts that need update for existing properties
      for (String name : alertNamesForPropertyUpdates) {
        AlertDefinitionEntity alertDefinition = alertDefinitionDAO.findByName(clusterID, name);
        if (alertDefinition != null && !definitionsForPropertyUpdates.containsKey(alertDefinition.getDefinitionId())) {
          definitionsForPropertyUpdates.put(alertDefinition.getDefinitionId(), alertDefinition);
        }
      }

      // updating old and new properties, best way to use map like visibilityMap.
      for (AlertDefinitionEntity alertDefinition : definitionsForPropertyUpdates.values()) {
        // here goes property updates
        if (visibilityMap.containsKey(alertDefinition.getDefinitionName())) {
          for (Map.Entry<String, String> entry : visibilityMap.get(alertDefinition.getDefinitionName()).entrySet()){
            String paramName = entry.getKey();
            String visibilityValue = entry.getValue();
            String source = alertDefinition.getSource();
            alertDefinition.setSource(addParamOption(source, paramName, "visibility", visibilityValue));
          }
        }
        // update percent script alerts param values from 0.x to 0.x * 100 values
        if (scriptAlertMultiplierMap.containsKey(alertDefinition.getDefinitionName())) {
          for (Map.Entry<String, Integer> entry : scriptAlertMultiplierMap.get(alertDefinition.getDefinitionName()).entrySet()){
            String paramName = entry.getKey();
            Integer multiplier = entry.getValue();
            String source = alertDefinition.getSource();
            Float oldValue = getParamFloatValue(source, paramName);
            if (oldValue == null) {
              alertDefinition.setSource(addParam(source, Arrays.asList(paramName)));
            } else {
              Integer newValue = Math.round(oldValue * multiplier);
              alertDefinition.setSource(setParamIntegerValue(source, paramName, newValue));
            }
          }
        }

        // update reporting alerts(aggregate and metrics) values from 0.x to 0.x * 100 values
        if (reportingMultiplierMap.containsKey(alertDefinition.getDefinitionName())) {
          for (Map.Entry<String, Integer> entry : reportingMultiplierMap.get(alertDefinition.getDefinitionName()).entrySet()){
            String reportingName = entry.getKey();
            Integer multiplier = entry.getValue();
            String source = alertDefinition.getSource();
            Float oldValue = getReportingFloatValue(source, reportingName);
            Integer newValue = Math.round(oldValue * multiplier);
            alertDefinition.setSource(setReportingIntegerValue(source, reportingName, newValue));
          }
        }

        if (reportingPercentMap.containsKey(alertDefinition.getDefinitionName())) {
          for (Map.Entry<String, String> entry : reportingPercentMap.get(alertDefinition.getDefinitionName()).entrySet()){
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            String source = alertDefinition.getSource();
            alertDefinition.setSource(addReportingOption(source, paramName, paramValue));
          }
        }

        if (alertNamesForNameserviceUpdate.contains(alertDefinition.getDefinitionName())) {
          String source = alertDefinition.getSource();
          alertDefinition.setSource(setNameservice(source, newNameservicePropertyValue));
        }
        // regeneration of hash and writing modified alerts to database, must go after all modifications finished
        alertDefinition.setHash(UUID.randomUUID().toString());
        alertDefinitionDAO.merge(alertDefinition);
      }
      //update Atlas alert
      final AlertDefinitionEntity atlasMetadataServerWebUI = alertDefinitionDAO.findByName(
              clusterID, "metadata_server_webui");
      if (atlasMetadataServerWebUI != null) {
        String source = atlasMetadataServerWebUI.getSource();
        JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();

        JsonObject uriJson = sourceJson.get("uri").getAsJsonObject();
        uriJson.remove("kerberos_keytab");
        uriJson.remove("kerberos_principal");
        uriJson.addProperty("kerberos_keytab", "{{cluster-env/smokeuser_keytab}}");
        uriJson.addProperty("kerberos_principal", "{{cluster-env/smokeuser_principal_name}}");

        atlasMetadataServerWebUI.setSource(sourceJson.toString());

        atlasMetadataServerWebUI.setHash(UUID.randomUUID().toString());
        alertDefinitionDAO.merge(atlasMetadataServerWebUI);
      }

      for (String alertName: alertForRemoval) {
        AlertDefinitionEntity alertDefinition = alertDefinitionDAO.findByName(clusterID, alertName);
        if (alertDefinition != null) {
          LOG.info("Removing alert : " + alertName);
          alertDefinitionDAO.remove(alertDefinition);
        }
      }
    }
  }

  protected String setNameservice(String source, String paramValue) {
    final String nameservicePropertyName = "nameservice";
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    JsonObject highAvailability = sourceJson.getAsJsonObject("uri").getAsJsonObject("high_availability");
    if (highAvailability.has(nameservicePropertyName)) {
      highAvailability.addProperty(nameservicePropertyName, paramValue);
    }
    return sourceJson.toString();
  }

  /*
  * Simple put method with check for key is not null
  * */
  private void checkedPutToMap(Map<AlertDefinitionEntity, List<String>> alertDefinitionParams, AlertDefinitionEntity alertDefinitionEntity,
                               List<String> params) {
    if (alertDefinitionEntity != null) {
      alertDefinitionParams.put(alertDefinitionEntity, params);
    }
  }

  /**
   * Add option to script parameter.
   * @param source json string of script source
   * @param paramName parameter name
   * @param optionName option name
   * @param optionValue option value
   * @return modified source
   */
  protected String addParamOption(String source, String paramName, String optionName, String optionValue){
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    JsonArray parametersJson = sourceJson.getAsJsonArray("parameters");
    if(parametersJson != null && !parametersJson.isJsonNull()) {
      for(JsonElement param : parametersJson) {
        if(param.isJsonObject()) {
          JsonObject paramObject = param.getAsJsonObject();
          if(paramObject.has("name") && paramObject.get("name").getAsString().equals(paramName)){
            paramObject.add(optionName, new JsonPrimitive(optionValue));
          }
        }
      }
    }
    return sourceJson.toString();
  }

  /**
   * Returns param value as float.
   * @param source source of script alert
   * @param paramName param name
   * @return param value as float
   */
  protected Float getParamFloatValue(String source, String paramName){
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    JsonArray parametersJson = sourceJson.getAsJsonArray("parameters");
    if(parametersJson != null && !parametersJson.isJsonNull()) {
      for(JsonElement param : parametersJson) {
        if(param.isJsonObject()) {
          JsonObject paramObject = param.getAsJsonObject();
          if(paramObject.has("name") && paramObject.get("name").getAsString().equals(paramName)){
            if(paramObject.has("value")) {
              return paramObject.get("value").getAsFloat();
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Set integer param value.
   * @param source source of script alert
   * @param paramName param name
   * @param value new param value
   * @return modified source
   */
  protected String setParamIntegerValue(String source, String paramName, Integer value){
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    JsonArray parametersJson = sourceJson.getAsJsonArray("parameters");
    if(parametersJson != null && !parametersJson.isJsonNull()) {
      for(JsonElement param : parametersJson) {
        if(param.isJsonObject()) {
          JsonObject paramObject = param.getAsJsonObject();
          if(paramObject.has("name") && paramObject.get("name").getAsString().equals(paramName)){
            paramObject.add("value", new JsonPrimitive(value));
          }
        }
      }
    }
    return sourceJson.toString();
  }

  /**
   * Returns reporting value as float.
   * @param source source of aggregate or metric alert
   * @param reportingName reporting name, must be "warning" or "critical"
   * @return reporting value as float
   */
  protected Float getReportingFloatValue(String source, String reportingName){
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    return sourceJson.getAsJsonObject("reporting").getAsJsonObject(reportingName).get("value").getAsFloat();
  }

  /**
   * Set integer value of reporting.
   * @param source source of aggregate or metric alert
   * @param reportingName reporting name, must be "warning" or "critical"
   * @param value new value
   * @return modified source
   */
  protected String setReportingIntegerValue(String source, String reportingName, Integer value){
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    sourceJson.getAsJsonObject("reporting").getAsJsonObject(reportingName).add("value", new JsonPrimitive(value));
    return sourceJson.toString();
  }

  /**
   * Add option to reporting
   * @param source source of aggregate or metric alert
   * @param optionName option name
   * @param value option value
   * @return modified source
   */
  protected String addReportingOption(String source, String optionName, String value){
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    sourceJson.getAsJsonObject("reporting").add(optionName, new JsonPrimitive(value));
    return sourceJson.toString();
  }

  protected String addParam(String source, List<String> params) {
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    JsonArray parametersJson = sourceJson.getAsJsonArray("parameters");

    boolean parameterExists = parametersJson != null && !parametersJson.isJsonNull();

    if (parameterExists) {
      Iterator<JsonElement> jsonElementIterator = parametersJson.iterator();
      while (jsonElementIterator.hasNext()) {
        JsonElement element = jsonElementIterator.next();
        JsonElement name = element.getAsJsonObject().get("name");
        if (name != null && !name.isJsonNull() && params.contains(name.getAsString())) {
          params.remove(name.getAsString());
        }
      }
      if (params.size() == 0) {
        return sourceJson.toString();
      }
    }

    List<JsonObject> paramsToAdd = new ArrayList<>();

    if (params.contains("connection.timeout")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("connection.timeout"));
      param.add("display_name", new JsonPrimitive("Connection Timeout"));
      param.add("value", new JsonPrimitive(5.0));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The maximum time before this alert is considered to be CRITICAL"));
      param.add("units", new JsonPrimitive("seconds"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));

      paramsToAdd.add(param);

    }
    if (params.contains("checkpoint.time.warning.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("checkpoint.time.warning.threshold"));
      param.add("display_name", new JsonPrimitive("Checkpoint Warning"));
      param.add("value", new JsonPrimitive(2.0));
      param.add("type", new JsonPrimitive("PERCENT"));
      param.add("description", new JsonPrimitive("The percentage of the last checkpoint time greater than the interval in order to trigger a warning alert."));
      param.add("units", new JsonPrimitive("%"));
      param.add("threshold", new JsonPrimitive("WARNING"));

      paramsToAdd.add(param);

    }
    if (params.contains("checkpoint.time.critical.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("checkpoint.time.critical.threshold"));
      param.add("display_name", new JsonPrimitive("Checkpoint Critical"));
      param.add("value", new JsonPrimitive(4.0));
      param.add("type", new JsonPrimitive("PERCENT"));
      param.add("description", new JsonPrimitive("The percentage of the last checkpoint time greater than the interval in order to trigger a critical alert."));
      param.add("units", new JsonPrimitive("%"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));

      paramsToAdd.add(param);

    }
    if (params.contains("checkpoint.txns.multiplier.warning.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("checkpoint.txns.multiplier.warning.threshold"));
      param.add("display_name", new JsonPrimitive("Uncommitted transactions Warning"));
      param.add("value", new JsonPrimitive(2.0));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The multiplier to use against dfs.namenode.checkpoint.period compared to the difference between last transaction id and most recent transaction id beyond which to trigger a warning alert."));
      param.add("threshold", new JsonPrimitive("WARNING"));

      paramsToAdd.add(param);
    }
    if (params.contains("checkpoint.txns.multiplier.critical.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("checkpoint.txns.multiplier.critical.threshold"));
      param.add("display_name", new JsonPrimitive("Uncommitted transactions Critical"));
      param.add("value", new JsonPrimitive(4.0));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The multiplier to use against dfs.namenode.checkpoint.period compared to the difference between last transaction id and most recent transaction id beyond which to trigger a critical alert."));
      param.add("threshold", new JsonPrimitive("CRITICAL"));

      paramsToAdd.add(param);
    }
    if (params.contains("default.smoke.user")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.user"));
      param.add("display_name", new JsonPrimitive("Default Smoke User"));
      param.add("value", new JsonPrimitive("ambari-qa"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The user that will run the Hive commands if not specified in cluster-env/smokeuser"));

      paramsToAdd.add(param);

    }
    if (params.contains("default.smoke.principal")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.principal"));
      param.add("display_name", new JsonPrimitive("Default Smoke Principal"));
      param.add("value", new JsonPrimitive("ambari-qa@EXAMPLE.COM"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The principal to use when retrieving the kerberos ticket if not specified in cluster-env/smokeuser_principal_name"));

      paramsToAdd.add(param);

    }
    if (params.contains("default.smoke.keytab")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.keytab"));
      param.add("display_name", new JsonPrimitive("Default Smoke Keytab"));
      param.add("value", new JsonPrimitive("/etc/security/keytabs/smokeuser.headless.keytab"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The keytab to use when retrieving the kerberos ticket if not specified in cluster-env/smokeuser_keytab"));

      paramsToAdd.add(param);

    }
    if (params.contains("run.directory")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("run.directory"));
      param.add("display_name", new JsonPrimitive("Run Directory"));
      param.add("value", new JsonPrimitive("/var/run/flume"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The directory where flume agent processes will place their PID files."));

      paramsToAdd.add(param);

    }
    if (params.contains("minimum.free.space")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("minimum.free.space"));
      param.add("display_name", new JsonPrimitive("Minimum Free Space"));
      param.add("value", new JsonPrimitive("5000000000"));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The overall amount of free disk space left before an alert is triggered."));
      param.add("units", new JsonPrimitive("bytes"));
      param.add("threshold", new JsonPrimitive("WARNING"));
      paramsToAdd.add(param);

    }
    if (params.contains("percent.used.space.warning.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("percent.used.space.warning.threshold"));
      param.add("display_name", new JsonPrimitive("Warning"));
      param.add("value", new JsonPrimitive("50"));
      param.add("type", new JsonPrimitive("PERCENT"));
      param.add("description", new JsonPrimitive("The percent of disk space consumed before a warning is triggered."));
      param.add("units", new JsonPrimitive("%"));
      param.add("threshold", new JsonPrimitive("WARNING"));
      paramsToAdd.add(param);

    }
    if (params.contains("percent.free.space.critical.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("percent.free.space.critical.threshold"));
      param.add("display_name", new JsonPrimitive("Critical"));
      param.add("value", new JsonPrimitive("80"));
      param.add("type", new JsonPrimitive("PERCENT"));
      param.add("description", new JsonPrimitive("The percent of disk space consumed before a critical alert is triggered."));
      param.add("units", new JsonPrimitive("%"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));
      paramsToAdd.add(param);

    }
    if (params.contains("request.by.status.warning.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("request.by.status.warning.threshold"));
      param.add("display_name", new JsonPrimitive("Warning Request Time"));
      param.add("value", new JsonPrimitive("3000"));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The time to find requests in progress before a warning alert is triggered."));
      param.add("units", new JsonPrimitive("ms"));
      param.add("threshold", new JsonPrimitive("WARNING"));
      paramsToAdd.add(param);

    }
    if (params.contains("request.by.status.critical.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("request.by.status.critical.threshold"));
      param.add("display_name", new JsonPrimitive("Critical Request Time"));
      param.add("value", new JsonPrimitive("5000"));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The time to find requests in progress before a critical alert is triggered."));
      param.add("units", new JsonPrimitive("ms"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));
      paramsToAdd.add(param);

    }
    if (params.contains("task.status.aggregation.warning.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("task.status.aggregation.warning.threshold"));
      param.add("display_name", new JsonPrimitive("Warning Process Time"));
      param.add("value", new JsonPrimitive("3000"));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The time to calculate a request's status from its tasks before a warning alert is triggered."));
      param.add("units", new JsonPrimitive("ms"));
      param.add("threshold", new JsonPrimitive("WARNING"));
      paramsToAdd.add(param);

    }
    if (params.contains("task.status.aggregation.critical.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("task.status.aggregation.critical.threshold"));
      param.add("display_name", new JsonPrimitive("Critical Process Time"));
      param.add("value", new JsonPrimitive("5000"));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The time to calculate a request's status from its tasks before a critical alert is triggered."));
      param.add("units", new JsonPrimitive("ms"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));
      paramsToAdd.add(param);

    }
    if (params.contains("rest.api.cluster.warning.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("rest.api.cluster.warning.threshold"));
      param.add("display_name", new JsonPrimitive("Warning Response Time"));
      param.add("value", new JsonPrimitive("5000"));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The time to get a cluster via the REST API before a warning alert is triggered."));
      param.add("units", new JsonPrimitive("ms"));
      param.add("threshold", new JsonPrimitive("WARNING"));
      paramsToAdd.add(param);

    }
    if (params.contains("rest.api.cluster.critical.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("rest.api.cluster.critical.threshold"));
      param.add("display_name", new JsonPrimitive("Critical Response Time"));
      param.add("value", new JsonPrimitive("7000"));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The time to get a cluster via the REST API before a critical alert is triggered."));
      param.add("units", new JsonPrimitive("ms"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));
      paramsToAdd.add(param);

    }
    if (params.contains("socket.command")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("socket.command"));
      param.add("display_name", new JsonPrimitive("Socket Command"));
      param.add("value", new JsonPrimitive("ruok"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("A socket command which queries ZooKeeper to respond with its state. The expected response is imok."));
      paramsToAdd.add(param);

    }
    if (params.contains("socket.command.response")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("socket.command.response"));
      param.add("display_name", new JsonPrimitive("Expected Response"));
      param.add("value", new JsonPrimitive("imok"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The expected response to the socket command."));
      paramsToAdd.add(param);

    }



    if (!parameterExists) {
      parametersJson = new JsonArray();
      for (JsonObject param : paramsToAdd) {
        parametersJson.add(param);
      }
      sourceJson.add("parameters", parametersJson);
    } else {
      for (JsonObject param : paramsToAdd) {
        parametersJson.add(param);
      }
      sourceJson.remove("parameters");
      sourceJson.add("parameters", parametersJson);
    }

    return sourceJson.toString();
  }

  protected void updateAdminPermissionTable() throws SQLException {
    // Add the sort_order column to the adminpermission table
    dbAccessor.addColumn(ADMIN_PERMISSION_TABLE,
        new DBColumnInfo(SORT_ORDER_COL, Short.class, null, 1, false));

    // Add the principal_id column to the adminpermission table
    //   Note: This is set to nullable here, but will be altered once the column has been set
    //         properly during the DML update phase.
    dbAccessor.addColumn(ADMIN_PERMISSION_TABLE,
        new DBColumnInfo(PRINCIPAL_ID_COL, Long.class, null, null, true));
  }

  protected void updateTopologyRequestTable() throws SQLException {
    // Add the sort_order column to the adminpermission table
    dbAccessor.addColumn(TOPOLOGY_REQUEST_TABLE,
      new DBColumnInfo(PROVISION_ACTION_COL, String.class, 255, null, true));
  }

  /**
   * Updates the {@value #ALERT_DEFINITION_TABLE} in the following ways:
   * <ul>
   * <li>Craetes the {@value #HELP_URL_COLUMN} column</li>
   * <li>Craetes the {@value #REPEAT_TOLERANCE_COLUMN} column</li>
   * <li>Craetes the {@value #REPEAT_TOLERANCE_ENABLED_COLUMN} column</li>
   * </ul>
   *
   * @throws SQLException
   */
  protected void updateAlertDefinitionTable() throws SQLException {
    dbAccessor.addColumn(ALERT_DEFINITION_TABLE,
        new DBColumnInfo(HELP_URL_COLUMN, String.class, 512, null, true));

    dbAccessor.addColumn(ALERT_DEFINITION_TABLE,
        new DBColumnInfo(REPEAT_TOLERANCE_COLUMN, Integer.class, null, 1, false));

    dbAccessor.addColumn(ALERT_DEFINITION_TABLE,
        new DBColumnInfo(REPEAT_TOLERANCE_ENABLED_COLUMN, Short.class, null, 0, false));
  }

  /**
   * Updates the {@value #ALERT_CURRENT_TABLE} in the following ways:
   * <ul>
   * <li>Creates the {@value #ALERT_CURRENT_OCCURRENCES_COLUMN} column</li>
   * <li>Creates the {@value #ALERT_CURRENT_FIRMNESS_COLUMN} column</li>
   * </ul>
   *
   * @throws SQLException
   */
  protected void updateAlertCurrentTable() throws SQLException {
    dbAccessor.addColumn(ALERT_CURRENT_TABLE,
      new DBColumnInfo(ALERT_CURRENT_OCCURRENCES_COLUMN, Long.class, null, 1, false));

    dbAccessor.addColumn(ALERT_CURRENT_TABLE, new DBColumnInfo(ALERT_CURRENT_FIRMNESS_COLUMN,
      String.class, 255, AlertFirmness.HARD.name(), false));
  }

  /**
   * Updates the {@value #ALERT_TARGET_TABLE} in the following ways:
   * <ul>
   * <li>Creates the {@value #ALERT_TARGET_ENABLED_COLUMN} column</li>
   * </ul>
   *
   * @throws SQLException
   */
  protected void updateAlertTargetTable() throws SQLException {
    dbAccessor.addColumn(ALERT_TARGET_TABLE,
      new DBColumnInfo(ALERT_TARGET_ENABLED_COLUMN, Short.class, null, 1, false));
  }

  protected void setRoleSortOrder() throws SQLException {
    String updateStatement = "UPDATE " + ADMIN_PERMISSION_TABLE + " SET " + SORT_ORDER_COL + "=%d WHERE " + PERMISSION_ID_COL + "='%s'";

    LOG.info("Setting permission labels");
    dbAccessor.executeUpdate(String.format(updateStatement,
        1, PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
        2, PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
      3, PermissionEntity.CLUSTER_OPERATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
      4, PermissionEntity.SERVICE_ADMINISTRATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
      5, PermissionEntity.SERVICE_OPERATOR_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
      6, PermissionEntity.CLUSTER_USER_PERMISSION_NAME));
    dbAccessor.executeUpdate(String.format(updateStatement,
      7, PermissionEntity.VIEW_USER_PERMISSION_NAME));
  }

  /**
   * Create and update records to create the role-based principals.
   * <p>
   * This includes creating the new "ROLE" principal type, a principal for each role, and finally
   * updating the princial_id column for the role.
   */
  void createRolePrincipals() throws SQLException {
    // Create Role Principal Type
    PrincipalTypeEntity rolePrincipalType = new PrincipalTypeEntity();
    rolePrincipalType.setName("ROLE");

    // creates the new record and returns an entity with the id set.
    rolePrincipalType = principalTypeDAO.merge(rolePrincipalType);

    // Get the roles (adminpermissions) and create a principal for each.... set the role's principal_id
    // value as we go...
    List<PermissionEntity> roleEntities = permissionDAO.findAll();

    for (PermissionEntity roleEntity : roleEntities) {
      PrincipalEntity principalEntity = new PrincipalEntity();
      principalEntity.setPrincipalType(rolePrincipalType);

      roleEntity.setPrincipal(principalDAO.merge(principalEntity));

      permissionDAO.merge(roleEntity);
    }

    // Fix the adminpermission.principal_id column to be non-nullable:
    dbAccessor.alterColumn(ADMIN_PERMISSION_TABLE,
        new DBColumnInfo(PRINCIPAL_ID_COL, Long.class, null, null, false));
  }

  /**
   * Makes the following changes to the {@value #REPO_VERSION_TABLE} table:
   * <ul>
   * <li>repo_type VARCHAR(255) DEFAULT 'STANDARD' NOT NULL</li>
   * <li>version_url VARCHAR(1024)</li>
   * <li>version_xml MEDIUMTEXT</li>
   * <li>version_xsd VARCHAR(512)</li>
   * <li>parent_id BIGINT</li>
   * </ul>
   *
   * @throws SQLException
   */
  private void updateRepoVersionTableDDL() throws SQLException {
    DBColumnInfo repoTypeColumn = new DBColumnInfo("repo_type", String.class, 255, RepositoryType.STANDARD.name(), false);
    DBColumnInfo versionUrlColumn = new DBColumnInfo("version_url", String.class, 1024, null, true);
    DBColumnInfo versionXmlColumn = new DBColumnInfo("version_xml", Clob.class, null, null, true);
    DBColumnInfo versionXsdColumn = new DBColumnInfo("version_xsd", String.class, 512, null, true);
    DBColumnInfo parentIdColumn = new DBColumnInfo("parent_id", Long.class, null, null, true);

    dbAccessor.addColumn(REPO_VERSION_TABLE, repoTypeColumn);
    dbAccessor.addColumn(REPO_VERSION_TABLE, versionUrlColumn);
    dbAccessor.addColumn(REPO_VERSION_TABLE, versionXmlColumn);
    dbAccessor.addColumn(REPO_VERSION_TABLE, versionXsdColumn);
    dbAccessor.addColumn(REPO_VERSION_TABLE, parentIdColumn);
  }

  /**
   * Makes the following changes to the {@value #SERVICE_COMPONENT_DS_TABLE} table,
   * but only if the table doesn't have it's new PK set.
   * <ul>
   * <li>id BIGINT NOT NULL</li>
   * <li>Drops FKs on {@value #HOST_COMPONENT_DS_TABLE} and {@value #HOST_COMPONENT_STATE_TABLE}</li>
   * <li>Populates ID in {@value #SERVICE_COMPONENT_DS_TABLE}</li>
   * <li>Creates {@code UNIQUE} constraint on {@value #HOST_COMPONENT_DS_TABLE}</li>
   * <li>Adds FKs on {@value #HOST_COMPONENT_DS_TABLE} and {@value #HOST_COMPONENT_STATE_TABLE}</li>
   * <li>Adds new sequence value of {@code servicecomponentdesiredstate_id_seq}</li>
   * </ul>
   *
   * @throws SQLException
   */
  void updateServiceComponentDesiredStateTableDDL() throws SQLException {
    if (dbAccessor.tableHasPrimaryKey(SERVICE_COMPONENT_DS_TABLE, ID)) {
      LOG.info("Skipping {} table Primary Key modifications since the new {} column already exists",
          SERVICE_COMPONENT_DS_TABLE, ID);

      return;
    }

    // drop FKs to SCDS in both HCDS and HCS tables
    // These are the expected constraint names
    dbAccessor.dropFKConstraint(HOST_COMPONENT_DS_TABLE, "hstcmpnntdesiredstatecmpnntnme");
    dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, "hstcomponentstatecomponentname");
    // These are the old (pre Ambari 1.5) constraint names, however still found on some installations
    dbAccessor.dropFKConstraint(HOST_COMPONENT_DS_TABLE, "FK_hostcomponentdesiredstate_component_name");
    dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, "FK_hostcomponentstate_component_name");

    // remove existing compound PK
    dbAccessor.dropPKConstraint(SERVICE_COMPONENT_DS_TABLE, "servicecomponentdesiredstate_pkey");

    // add new PK column to SCDS, making it nullable for now
    DBColumnInfo idColumn = new DBColumnInfo(ID, Long.class, null, null, true);
    dbAccessor.addColumn(SERVICE_COMPONENT_DS_TABLE, idColumn);

    // populate SCDS id column
    AtomicLong scdsIdCounter = new AtomicLong(1);
    Statement statement = null;
    ResultSet resultSet = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        String selectSQL = String.format("SELECT cluster_id, service_name, component_name FROM %s",
            SERVICE_COMPONENT_DS_TABLE);

        resultSet = statement.executeQuery(selectSQL);
        while (null != resultSet && resultSet.next()) {
          final Long clusterId = resultSet.getLong("cluster_id");
          final String serviceName = resultSet.getString("service_name");
          final String componentName = resultSet.getString("component_name");

          String updateSQL = String.format(
              "UPDATE %s SET %s = %d WHERE cluster_id = %d AND service_name = '%s' AND component_name = '%s'",
              SERVICE_COMPONENT_DS_TABLE, ID, scdsIdCounter.getAndIncrement(), clusterId,
              serviceName, componentName);

          dbAccessor.executeQuery(updateSQL);
        }
      }
    } finally {
      JdbcUtils.closeResultSet(resultSet);
      JdbcUtils.closeStatement(statement);
    }

    // make the column NON NULL now
    dbAccessor.alterColumn(SERVICE_COMPONENT_DS_TABLE,
        new DBColumnInfo(ID, Long.class, null, null, false));

    // create a new PK, matching the name of the constraint found in SQL
    dbAccessor.addPKConstraint(SERVICE_COMPONENT_DS_TABLE, "pk_sc_desiredstate", ID);

    // create UNIQUE constraint, ensuring column order matches SQL files
    String[] uniqueColumns = new String[] { "component_name", "service_name", "cluster_id" };
    dbAccessor.addUniqueConstraint(SERVICE_COMPONENT_DS_TABLE, "UQ_scdesiredstate_name",
        uniqueColumns);

    // add FKs back to SCDS in both HCDS and HCS tables
    dbAccessor.addFKConstraint(HOST_COMPONENT_DS_TABLE, "hstcmpnntdesiredstatecmpnntnme",
        uniqueColumns, SERVICE_COMPONENT_DS_TABLE, uniqueColumns, false);

    dbAccessor.addFKConstraint(HOST_COMPONENT_STATE_TABLE, "hstcomponentstatecomponentname",
        uniqueColumns, SERVICE_COMPONENT_DS_TABLE, uniqueColumns, false);

    // Add sequence for SCDS id
    addSequence("servicecomponentdesiredstate_id_seq", scdsIdCounter.get(), false);
  }

  /**
   * Makes the following changes to the {@value #SERVICE_COMPONENT_HISTORY_TABLE} table:
   * <ul>
   * <li>id BIGINT NOT NULL</li>
   * <li>component_id BIGINT NOT NULL</li>
   * <li>upgrade_id BIGINT NOT NULL</li>
   * <li>from_stack_id BIGINT NOT NULL</li>
   * <li>to_stack_id BIGINT NOT NULL</li>
   * <li>CONSTRAINT PK_sc_history PRIMARY KEY (id)</li>
   * <li>CONSTRAINT FK_sc_history_component_id FOREIGN KEY (component_id) REFERENCES servicecomponentdesiredstate (id)</li>
   * <li>CONSTRAINT FK_sc_history_upgrade_id FOREIGN KEY (upgrade_id) REFERENCES upgrade (upgrade_id)</li>
   * <li>CONSTRAINT FK_sc_history_from_stack_id FOREIGN KEY (from_stack_id) REFERENCES stack (stack_id)</li>
   * <li>CONSTRAINT FK_sc_history_to_stack_id FOREIGN KEY (to_stack_id) REFERENCES stack (stack_id)</li>
   * <li>Creates the {@code servicecomponent_history_id_seq}</li>
   * </ul>
   *
   * @throws SQLException
   */
  private void createServiceComponentHistoryTable() throws SQLException {
    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo(ID, Long.class, null, null, false));
    columns.add(new DBColumnInfo("component_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("upgrade_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("from_stack_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("to_stack_id", Long.class, null, null, false));
    dbAccessor.createTable(SERVICE_COMPONENT_HISTORY_TABLE, columns, (String[]) null);

    dbAccessor.addPKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "PK_sc_history", ID);

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "FK_sc_history_component_id",
        "component_id", SERVICE_COMPONENT_DS_TABLE, "id", false);

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "FK_sc_history_upgrade_id",
        "upgrade_id", UPGRADE_TABLE, "upgrade_id", false);

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "FK_sc_history_from_stack_id",
        "from_stack_id", STACK_TABLE, "stack_id", false);

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_HISTORY_TABLE, "FK_sc_history_to_stack_id",
      "to_stack_id", STACK_TABLE, "stack_id", false);

    addSequence("servicecomponent_history_id_seq", 0L, false);
  }

  /**
   * Alter servicecomponentdesiredstate table to add recovery_enabled column.
   * @throws SQLException
   */
  private void updateServiceComponentDesiredStateTable() throws SQLException {
    // ALTER TABLE servicecomponentdesiredstate ADD COLUMN
    // recovery_enabled SMALLINT DEFAULT 0 NOT NULL
    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
      new DBColumnInfo(RECOVERY_ENABLED_COL, Short.class, null, 0, false));

    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
      new DBColumnInfo(DESIRED_VERSION_COLUMN_NAME, String.class, 255, State.UNKNOWN.toString(), false));
  }

  /**
   * Alter host_role_command table to add original_start_time, which is needed because the start_time column now
   * allows overriding the value in ActionScheduler.java
   * @throws SQLException
   */
  private void updateHostRoleCommandTableDDL() throws SQLException {
    final String columnName = "original_start_time";
    DBColumnInfo originalStartTimeColumn = new DBColumnInfo(columnName, Long.class, null, -1L, true);
    dbAccessor.addColumn(HOST_ROLE_COMMAND_TABLE, originalStartTimeColumn);
  }

  /**
   * Alter host_role_command table to update original_start_time with values and make it non-nullable
   * @throws SQLException
   */
  protected void updateHostRoleCommandTableDML() throws SQLException {
    final String columnName = "original_start_time";
    dbAccessor.executeQuery("UPDATE " + HOST_ROLE_COMMAND_TABLE + " SET original_start_time = start_time", false);
    dbAccessor.executeQuery("UPDATE " + HOST_ROLE_COMMAND_TABLE + " SET original_start_time=-1 WHERE original_start_time IS NULL");
    dbAccessor.setColumnNullable(HOST_ROLE_COMMAND_TABLE, columnName, false);
  }

  /**
   * Puts each item in the specified list inside single quotes and
   * returns a comma separated value for use in a SQL query.
   * @param list
   * @return
   */
  private String sqlStringListFromArrayList(List<String> list) {
    List<String> sqlList = new ArrayList<>(list.size());

    for (String item : list) {
      sqlList.add(String.format("'%s'", item.trim()));
    }

    return StringUtils.join(sqlList, ',');
  }

  /**
   * Update clusterconfig table for config type 'cluster-env' with the
   * recovery attributes.
   *
   * @throws AmbariException
   */
  private void updateRecoveryClusterEnvConfig() throws AmbariException {
    Map<String, String> propertyMap = new HashMap<>();

    if (StringUtils.isNotEmpty(config.getNodeRecoveryType())) {
      propertyMap.put(RecoveryConfigHelper.RECOVERY_ENABLED_KEY, "true");
      propertyMap.put(RecoveryConfigHelper.RECOVERY_TYPE_KEY, config.getNodeRecoveryType());
    }
    else {
      propertyMap.put(RecoveryConfigHelper.RECOVERY_ENABLED_KEY, "false");
    }

    if (StringUtils.isNotEmpty(config.getNodeRecoveryLifetimeMaxCount())) {
      propertyMap.put(RecoveryConfigHelper.RECOVERY_LIFETIME_MAX_COUNT_KEY, config.getNodeRecoveryLifetimeMaxCount());
    }

    if (StringUtils.isNotEmpty(config.getNodeRecoveryMaxCount())) {
      propertyMap.put(RecoveryConfigHelper.RECOVERY_MAX_COUNT_KEY, config.getNodeRecoveryMaxCount());
    }

    if (StringUtils.isNotEmpty(config.getNodeRecoveryRetryGap())) {
      propertyMap.put(RecoveryConfigHelper.RECOVERY_RETRY_GAP_KEY, config.getNodeRecoveryRetryGap());
    }

    if (StringUtils.isNotEmpty(config.getNodeRecoveryWindowInMin())) {
      propertyMap.put(RecoveryConfigHelper.RECOVERY_WINDOW_IN_MIN_KEY, config.getNodeRecoveryWindowInMin());
    }

    AmbariManagementController ambariManagementController = injector.getInstance(
            AmbariManagementController.class);

    Clusters clusters = ambariManagementController.getClusters();

    // for each cluster, update/create the cluster-env config type in clusterconfig
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      updateConfigurationPropertiesForCluster(cluster, ConfigHelper.CLUSTER_ENV, propertyMap,
              true /* update if exists */, true /* create new config type */);
    }
  }

  /**
   * Alter servicecomponentdesiredstate table to update recovery_enabled to 1
   * for the components that have been marked for auto start in ambari.properties
   * @throws SQLException
   */
  private void updateRecoveryComponents() throws SQLException {

    /*
     * Whether specific components are enabled/disabled for recovery. Being enabled takes
     * precedence over being disabled. When specific components are enabled then only
     * those components are enabled. When specific components are disabled then all of
     * the other components are enabled.
     */
    String enabledComponents = config.getRecoveryEnabledComponents();
    String disabledComponents = config.getRecoveryDisabledComponents();
    String query;

    if (StringUtils.isEmpty(enabledComponents)) {
      if (StringUtils.isEmpty(disabledComponents)) {
        // disable all components
        query = String.format("UPDATE %s SET recovery_enabled = 0", SERVICE_COMPONENT_DESIRED_STATE_TABLE);
      }
      else {
        // enable (1 - disabledComponents)
        List<String> disabledComponentsList = Arrays.asList(disabledComponents.split(","));
        String components = sqlStringListFromArrayList(disabledComponentsList);
        query = String.format("UPDATE %s SET recovery_enabled = 1 WHERE component_name NOT IN (%s)",
                SERVICE_COMPONENT_DESIRED_STATE_TABLE, components);
      }
    }
    else {
      // enable the specified components
      List<String> enabledComponentsList = Arrays.asList(enabledComponents.split(","));
      String components = sqlStringListFromArrayList(enabledComponentsList);
      query = String.format("UPDATE %s SET recovery_enabled = 1 WHERE component_name IN (%s)",
              SERVICE_COMPONENT_DESIRED_STATE_TABLE, components);
    }

    dbAccessor.executeQuery(query);
  }


  /**
   * Update clusterconfig table and servicecomponentdesiredstate table with the
   * recovery attributes and componenents to be recovered.
   *
   * @throws SQLException
     */
  @Transactional
  protected void updateRecoveryConfigurationDML() throws SQLException, AmbariException {
    updateRecoveryClusterEnvConfig();
    updateRecoveryComponents();
  }

  /**
   * Update Clusters and Hosts Version State from UPGRADING, UPGRADE_FAILED to INSTALLED
   * and UPGRADED to CURRENT if repo_version_id from cluster_version equals repo_version_id of Clusters and Hosts Version State
   *
   * @throws SQLException
   */

  @Transactional
  protected void updateClustersAndHostsVersionStateTableDML() throws SQLException, AmbariException {

    dbAccessor.executeQuery("UPDATE " + HOST_VERSION_TABLE + " SET state = 'INSTALLED' WHERE state IN ('UPGRADING', 'UPGRADE_FAILED', 'UPGRADED')");
    dbAccessor.executeQuery("UPDATE " + CLUSTER_VERSION_TABLE + " SET state = 'INSTALLED' WHERE state IN ('UPGRADING', 'UPGRADE_FAILED', 'UPGRADED')");

    Statement statement = null;
    ResultSet resultSet = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        String selectSQL = String.format("SELECT repo_version_id, cluster_id FROM %s WHERE state = 'CURRENT'",
                CLUSTER_VERSION_TABLE);

        resultSet = statement.executeQuery(selectSQL);
        Set<Long> clusterIds = new HashSet<>();
        while (null != resultSet && resultSet.next()) {
          Long clusterId = resultSet.getLong("cluster_id");
          if (clusterIds.contains(clusterId)) {
            throw new AmbariException(String.format("Database is in a bad state. Cluster %s contains multiple CURRENT version", clusterId));
          }
          clusterIds.add(clusterId);
          Long repoVersionId = resultSet.getLong("repo_version_id");

          String updateHostVersionSQL = String.format(
                  "UPDATE %s SET state = 'CURRENT' WHERE repo_version_id = %s", HOST_VERSION_TABLE, repoVersionId);
          String updateClusterVersionSQL = String.format(
                  "UPDATE %s SET state = 'CURRENT' WHERE repo_version_id = %s", CLUSTER_VERSION_TABLE, repoVersionId);

          dbAccessor.executeQuery(updateHostVersionSQL);
          dbAccessor.executeQuery(updateClusterVersionSQL);
        }
      }

    } finally {
      JdbcUtils.closeResultSet(resultSet);
      JdbcUtils.closeStatement(statement);
    }
  }

  /**
   * In hdfs-site, set dfs.client.retry.policy.enabled=false
   * This is needed for Rolling/Express upgrade so that clients don't keep retrying, which exhausts the retries and
   * doesn't allow for a graceful failover, which is expected.
   *
   * Rely on dfs.internal.nameservices after upgrade. Copy the value from dfs.services
   * @throws AmbariException
   */
  protected void updateHDFSConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();

          if (installedServices.contains("HDFS")) {
            Config hdfsSite = cluster.getDesiredConfigByType("hdfs-site");
            if (hdfsSite != null) {
              String clientRetryPolicyEnabled = hdfsSite.getProperties().get("dfs.client.retry.policy.enabled");
              if (null != clientRetryPolicyEnabled && Boolean.parseBoolean(clientRetryPolicyEnabled)) {
                updateConfigurationProperties("hdfs-site", Collections.singletonMap("dfs.client.retry.policy.enabled", "false"), true, false);
              }
              String nameservices = hdfsSite.getProperties().get("dfs.nameservices");
              String int_nameservices = hdfsSite.getProperties().get("dfs.internal.nameservices");
              if(int_nameservices == null && nameservices != null) {
                updateConfigurationProperties("hdfs-site", Collections.singletonMap("dfs.internal.nameservices", nameservices), true, false);
              }
            }
            Config hadoopEnv = cluster.getDesiredConfigByType("hadoop-env");
            if (hadoopEnv != null) {
              String keyServerPort = hadoopEnv.getProperties().get("keyserver_port");
              if (null != keyServerPort && " ".equals(keyServerPort)) {
                updateConfigurationProperties("hadoop-env", Collections.singletonMap("keyserver_port", ""), true, false);
              }
            }
          }
        }
      }
    }
  }
 
  protected void updateKAFKAConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();

          if (installedServices.contains("KAFKA") && cluster.getSecurityType() == SecurityType.KERBEROS) {
            Config kafkaBroker = cluster.getDesiredConfigByType(KAFKA_BROKER_CONFIG);
            if (kafkaBroker != null) {
              String listenersPropertyValue = kafkaBroker.getProperties().get("listeners");
              if (StringUtils.isNotEmpty(listenersPropertyValue)) {
                String newListenersPropertyValue = listenersPropertyValue.replaceAll("\\bPLAINTEXT\\b", "PLAINTEXTSASL");
                updateConfigurationProperties(KAFKA_BROKER_CONFIG, Collections.singletonMap("listeners", newListenersPropertyValue), true, false);
              }
            }
          }
        }
      }
    }
  }

  protected void updateHIVEConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();

          if (installedServices.contains("HIVE")) {
            Config hiveSite = cluster.getDesiredConfigByType("hive-site");
            if (hiveSite != null) {
              Map<String, String> hiveSiteProperties = hiveSite.getProperties();
              String txn_manager = hiveSiteProperties.get("hive.txn.manager");
              String concurrency = hiveSiteProperties.get("hive.support.concurrency");
              String initiator_on = hiveSiteProperties.get("hive.compactor.initiator.on");
              String partition_mode = hiveSiteProperties.get("hive.exec.dynamic.partition.mode");
              boolean acid_enabled =
                  txn_manager != null && txn_manager.equals("org.apache.hadoop.hive.ql.lockmgr.DbTxnManager") &&
                  concurrency != null && concurrency.toLowerCase().equals("true") &&
                  initiator_on != null && initiator_on.toLowerCase().equals("true") &&
                  partition_mode != null && partition_mode.equals("nonstrict");

              Config hiveEnv = cluster.getDesiredConfigByType("hive-env");
              if(hiveEnv != null){
                if(acid_enabled) {
                  updateConfigurationProperties("hive-env", Collections.singletonMap("hive_txn_acid", "on"), true, false);
                }
              }
            }
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

          Config amsEnv = cluster.getDesiredConfigByType("ams-env");
          if (amsEnv != null) {
            String content = amsEnv.getProperties().get("content");
            if (content != null && !content.contains("AMS_INSTANCE_NAME")) {
              String newContent = content + "\n # AMS instance name\n" +
                      "export AMS_INSTANCE_NAME={{hostname}}\n";

              updateConfigurationProperties("ams-env", Collections.singletonMap("content", newContent), true, true);
            }
          }

          Config amsHBaseEnv = cluster.getDesiredConfigByType("ams-hbase-env");
          if (amsHBaseEnv != null) {
            String content = amsHBaseEnv.getProperties().get("content");
            Map<String, String> newProperties = new HashMap<>();

            if (content != null && !content.contains("HBASE_HOME=")) {
              String newContent = content + "\n # Explicitly Setting HBASE_HOME for AMS HBase so that there is no conflict\n" +
                "export HBASE_HOME={{ams_hbase_home_dir}}\n";
              newProperties.put("content", newContent);
            }

            updateConfigurationPropertiesForCluster(cluster, "ams-hbase-env", newProperties, true, true);
          }

          Config amsSite = cluster.getDesiredConfigByType(AMS_SITE);
          if (amsSite != null) {
            String metadataFilters = amsSite.getProperties().get("timeline.metrics.service.metadata.filters");
            if (StringUtils.isEmpty(metadataFilters) ||
                !metadataFilters.contains("ContainerResource")) {
              updateConfigurationProperties("ams-site",
                Collections.singletonMap("timeline.metrics.service.metadata.filters", "ContainerResource"), true, false);
            }

            Map<String, String> amsSiteProperties = amsSite.getProperties();
            Map<String, String> newProperties = new HashMap<>();

            if (!amsSiteProperties.containsKey(TIMELINE_METRICS_SINK_COLLECTION_PERIOD) ||
              "60".equals(amsSiteProperties.get(TIMELINE_METRICS_SINK_COLLECTION_PERIOD))) {

              newProperties.put(TIMELINE_METRICS_SINK_COLLECTION_PERIOD, "10");
              LOG.info("Setting value of " + TIMELINE_METRICS_SINK_COLLECTION_PERIOD + " : 10");
            }

            if (amsSiteProperties.containsKey(PRECISION_TABLE_TTL_PROPERTY) &&
              !amsSiteProperties.get(PRECISION_TABLE_TTL_PROPERTY).equals("86400")) {
              newProperties.put(PRECISION_TABLE_TTL_PROPERTY, String.valueOf(86400));
              LOG.info("Setting value of " + PRECISION_TABLE_TTL_PROPERTY + " : " + 86400);
            }

            if (amsSiteProperties.containsKey(CLUSTER_SECOND_TABLE_TTL_PROPERTY) &&
              !amsSiteProperties.get(CLUSTER_SECOND_TABLE_TTL_PROPERTY).equals("259200")) {
              newProperties.put(CLUSTER_SECOND_TABLE_TTL_PROPERTY, String.valueOf(259200));
              LOG.info("Setting value of " + CLUSTER_SECOND_TABLE_TTL_PROPERTY + " : " + 259200);
            }

            updateConfigurationPropertiesForCluster(cluster, AMS_SITE, newProperties, true, true);
          }

          Config amsGrafanaIni = cluster.getDesiredConfigByType("ams-grafana-ini");
          if (amsGrafanaIni != null) {
            Map<String, String> amsGrafanaIniProperties = amsGrafanaIni.getProperties();
            String content = amsGrafanaIniProperties.get("content");
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("content", updateAmsGrafanaIni(content));
            updateConfigurationPropertiesForCluster(cluster, "ams-grafana-ini", newProperties, true, true);
          }

          Config amsHbaseSite = cluster.getDesiredConfigByType(AMS_HBASE_SITE);
          if (amsHbaseSite != null) {
            Map<String, String> amsHbaseSiteProperties = amsHbaseSite.getProperties();
            Map<String, String> newProperties = new HashMap<>();
            if (amsHbaseSiteProperties.containsKey(HBASE_RPC_TIMEOUT_PROPERTY) &&
                "30000".equals(amsHbaseSiteProperties.get(HBASE_RPC_TIMEOUT_PROPERTY))) {
              newProperties.put(HBASE_RPC_TIMEOUT_PROPERTY, String.valueOf(300000));
            }

            if(amsHbaseSiteProperties.containsKey(AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY) &&
              "true".equals(amsHbaseSiteProperties.get(AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY))) {
              LOG.info("Disabling " + AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY);
              newProperties.put(AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY, String.valueOf(false));
            }


            updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_SITE, newProperties, true, true);
          }

        }
      }
    }
  }

  protected String updateAmsGrafanaIni(String content) {

    if (content == null) {
      return null;
    }
    String regSearch = "/var/lib/ambari-metrics-grafana";
    String replacement = "{{ams_grafana_data_dir}}";
    content = content.replaceAll(regSearch, replacement);

    return content;
  }

  /**
   * Create blueprint_setting table for storing the "settings" section
   * in the blueprint. Auto start information is specified in the "settings" section.
   *
   * @throws SQLException
   */
  private void createBlueprintSettingTable() throws SQLException {
    List<DBColumnInfo> columns = new ArrayList<>();

    //  Add blueprint_setting table
    LOG.info("Creating " + BLUEPRINT_SETTING_TABLE + " table");

    columns.add(new DBColumnInfo(ID, Long.class, null, null, false));
    columns.add(new DBColumnInfo(BLUEPRINT_NAME_COL, String.class, 255, null, false));
    columns.add(new DBColumnInfo(SETTING_NAME_COL, String.class, 255, null, false));
    columns.add(new DBColumnInfo(SETTING_DATA_COL, char[].class, null, null, false));
    dbAccessor.createTable(BLUEPRINT_SETTING_TABLE, columns);

    dbAccessor.addPKConstraint(BLUEPRINT_SETTING_TABLE, "PK_blueprint_setting", ID);
    dbAccessor.addUniqueConstraint(BLUEPRINT_SETTING_TABLE, "UQ_blueprint_setting_name", BLUEPRINT_NAME_COL, SETTING_NAME_COL);
    dbAccessor.addFKConstraint(BLUEPRINT_SETTING_TABLE, "FK_blueprint_setting_name",
            BLUEPRINT_NAME_COL, BLUEPRINT_TABLE, BLUEPRINT_NAME_COL, false);

    addSequence("blueprint_setting_id_seq", 0L, false);
  }

  /**
   * Updates {@code cluster-env} in the following ways:
   * <ul>
   * <li>Adds {@link ConfigHelper#CLUSTER_ENV_ALERT_REPEAT_TOLERANCE} = 1</li>
   * <li>Adds {@link UpgradeCatalog240#ONE_DIR_PER_PARITION_PROPERTY} = false</li>
   * </ul>
   *
   * @throws Exception
   */
  protected void updateClusterEnv() throws AmbariException {
    Map<String, String> propertyMap = new HashMap<>();
    propertyMap.put(ConfigHelper.CLUSTER_ENV_ALERT_REPEAT_TOLERANCE, "1");
    propertyMap.put(ONE_DIR_PER_PARITION_PROPERTY, "false");

    AmbariManagementController ambariManagementController = injector.getInstance(
        AmbariManagementController.class);

    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      updateConfigurationPropertiesForCluster(cluster, ConfigHelper.CLUSTER_ENV, propertyMap, true,
          true);
    }
  }


  /**
   * Updates {@code yarn-env} in the following ways:
   * <ul>
   * <li>Replays export YARN_HISTORYSERVER_HEAPSIZE={{apptimelineserver_heapsize}} to export
   * YARN_TIMELINESERVER_HEAPSIZE={{apptimelineserver_heapsize}}</li>
   * </ul>
   *
   * @throws Exception
   */
  protected void updateYarnEnv() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
            AmbariManagementController.class);

    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      Config yarnEnvConfig = cluster.getDesiredConfigByType(YARN_ENV_CONFIG);
      Map<String, String> yarnEnvProps = new HashMap<String, String>();
      if (yarnEnvConfig != null) {
        String content = yarnEnvConfig.getProperties().get("content");
        // comment old property
        content = content.replaceAll("export YARN_HISTORYSERVER_HEAPSIZE=\\{\\{apptimelineserver_heapsize\\}\\}",
                "# export YARN_HISTORYSERVER_HEAPSIZE=\\{\\{apptimelineserver_heapsize\\}\\}");
        // add new correct property
        content = content + "\n\n      # Specify the max Heapsize for the timeline server using a numerical value\n" +
                "      # in the scale of MB. For example, to specify an jvm option of -Xmx1000m, set\n" +
                "      # the value to 1024.\n" +
                "      # This value will be overridden by an Xmx setting specified in either YARN_OPTS\n" +
                "      # and/or YARN_TIMELINESERVER_OPTS.\n" +
                "      # If not specified, the default value will be picked from either YARN_HEAPMAX\n" +
                "      # or JAVA_HEAP_MAX with YARN_HEAPMAX as the preferred option of the two.\n" +
                "      export YARN_TIMELINESERVER_HEAPSIZE={{apptimelineserver_heapsize}}";

        yarnEnvProps.put("content", content);
        updateConfigurationPropertiesForCluster(cluster, YARN_ENV_CONFIG, yarnEnvProps, true, true);
      }

    }

  }


  /**
   * Updates the Kerberos-related configurations for the clusters managed by this Ambari
   * <p/>
   * Performs the following updates:
   * <ul>
   * <li>Rename <code>kerberos-env/kdc_host</code> to
   * <code>kerberos-env/kdc_hosts</li>
   * <li>If krb5-conf/content was not changed from the original stack default, update it to the new
   * stack default</li>
   * </ul>
   *
   * @throws AmbariException if an error occurs while updating the configurations
   */
  protected void updateKerberosConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);

    for (final Cluster cluster : clusterMap.values()) {
      Config config;

      // Find the new stack default value for krb5-conf/content
      String newDefault = null;
      AmbariMetaInfo metaInfo = ambariManagementController.getAmbariMetaInfo();
      StackId stackId = cluster.getCurrentStackVersion();
      StackInfo stackInfo = ((metaInfo == null) || (stackId == null))
          ? null
          : metaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
      if(stackInfo != null) {
        ServiceInfo kerberosService = stackInfo.getService("KERBEROS");

        if (kerberosService != null) {
          List<PropertyInfo> properties = kerberosService.getProperties();

          if (properties != null) {
            for (PropertyInfo propertyInfo : properties) {
              if ("krb5-conf.xml".equals(propertyInfo.getFilename()) &&
                  "content".equals(propertyInfo.getName())) {
                newDefault = propertyInfo.getValue();
                break;
              }
            }
          }
        }
      }

      // Update the kerberos-env properties to change kdc_host to kdc_hosts
      config = cluster.getDesiredConfigByType("kerberos-env");
      if (config != null) {
        Map<String, String> updates = new HashMap<String, String>();
        Set<String> removes = new HashSet<String>();

        // Rename kdc_host to kdc_hosts
        String value = config.getProperties().get("kdc_host");
        updates.put("kdc_hosts", value);
        removes.add("kdc_host");

        // Ensure create_ambari_principal is set to "false" since it is expected that Ambari's
        // principal, keytab file, and JAAS file has already been manually configured.
        updates.put("create_ambari_principal", "false");

        updateConfigurationPropertiesForCluster(cluster, "kerberos-env", updates, removes, true, false);
      }

      // If a new default value for krb5-conf/content is available update it
      if(newDefault != null) {
        config = cluster.getDesiredConfigByType("krb5-conf");
        if (config != null) {
          String value = config.getProperties().get("content");
          String oldDefault = "\n[libdefaults]\n  renew_lifetime = 7d\n  forwardable = true\n  default_realm = {{realm}}\n  ticket_lifetime = 24h\n  dns_lookup_realm = false\n  dns_lookup_kdc = false\n  #default_tgs_enctypes = {{encryption_types}}\n  #default_tkt_enctypes = {{encryption_types}}\n\n{% if domains %}\n[domain_realm]\n{% for domain in domains.split(\u0027,\u0027) %}\n  {{domain}} = {{realm}}\n{% endfor %}\n{% endif %}\n\n[logging]\n  default = FILE:/var/log/krb5kdc.log\n  admin_server = FILE:/var/log/kadmind.log\n  kdc = FILE:/var/log/krb5kdc.log\n\n[realms]\n  {{realm}} = {\n    admin_server = {{admin_server_host|default(kdc_host, True)}}\n    kdc = {{kdc_host}}\n  }\n\n{# Append additional realm declarations below #}";

          // if the content is the same as the old stack default, update to the new stack default;
          // else leave it alone since the user may have changed it for a reason.
          if (oldDefault.equalsIgnoreCase(value)) {
            Map<String, String> updates = Collections.singletonMap("content", newDefault);
            updateConfigurationPropertiesForCluster(cluster, "krb5-conf", updates, null, true, false);
          }
          else {
            LOG.warn("The krb5-conf/content value was not updated to use the kerberos-env/kdc_hosts " +
                "property since the stored template appears to have been changed from the default. " +
                "This value should be manually updated so that\n" +
                "\tkdc = {{kdc_host}}\n" +
                "Is changed to something like\n" +
                "\t{%- if kdc_hosts &gt; 0 -%}\n" +
                "\t{%- set kdc_host_list = kdc_hosts.split(',')  -%}\n" +
                "\t{%- if kdc_host_list -%}\n" +
                "\t{% for kdc_host in kdc_host_list %}\n" +
                "\t    kdc = {{kdc_host|trim()}}\n" +
                "\t{%- endfor -%}");
          }
        }
      }
    }
  }

  /**
   * Updates the Falcon-related configurations for the clusters managed by this Ambari
   * Removes falcon_store_uri from falcon-env.
   * Appends Atlas hook classpath to FALCON_EXTRA_CLASS_PATH from falcon-env if it doesn't contain it
   * Appends '{{atlas_application_class_addition}}' to *.application.services from falcon-startup.properties if it doesn't contain it.
   *
   * @throws AmbariException if an error occurs while updating the configurations
   */
  protected void updateFalconConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);

    for (final Cluster cluster : clusterMap.values()) {
      Config falconEnvConfig = cluster.getDesiredConfigByType("falcon-env");
      if (falconEnvConfig != null) {
        // Remove falcon_store_uri from falcon-env.
        Map<String, String> falconEnvEnvProperties = falconEnvConfig.getProperties();
        if (falconEnvEnvProperties.containsKey("falcon_store_uri")) {
          LOG.info("Removing property falcon_store_uri from falcon-env");
          removeConfigurationPropertiesFromCluster(cluster, "falcon-env", Collections.singleton("falcon_store_uri"));
        }

        // Append Atlas hook classpath to FALCON_EXTRA_CLASS_PATH from falcon-env if it doesn't contain it
        // This is necessary for stack upgrades to 2.5 after the Ambari upgrade
        final String envContentPropertyName = "content";
        String contentValue = falconEnvConfig.getProperties().get(envContentPropertyName);
        if (contentValue != null) {
          final String atlasHookCPAddition = "{{atlas_hook_cp}}";
          if (!contentValue.contains(atlasHookCPAddition)) {
            LOG.info("Appending '{}' to FALCON_EXTRA_CLASS_PATH from falcon-env since it doesn't contain it", atlasHookCPAddition);
            String newValue = contentValue + "\n\n{% if falcon_atlas_support %}\n" +
                "# Add the Atlas Falcon hook to the Falcon classpath\n" +
                "export FALCON_EXTRA_CLASS_PATH={{atlas_hook_cp}}${FALCON_EXTRA_CLASS_PATH}\n" +
                "{% endif %}";
            updateConfigurationPropertiesForCluster(cluster, "falcon-env", Collections.singletonMap(envContentPropertyName, newValue), null, true, false);
          }
        }
      }

      // Update falcon-startup.properties/*.application.services.
      // Ensure {{atlas_application_class_addition}}
      Config falconStartupConfig = cluster.getDesiredConfigByType("falcon-startup.properties");
      if (falconStartupConfig != null) {
        final String applicationServicesPropertyName = "*.application.services";
        String value = falconStartupConfig.getProperties().get(applicationServicesPropertyName);
        if (value != null) {
          final String atlasApplicationClassAddition = "{{atlas_application_class_addition}}";
          if (!(value.contains(atlasApplicationClassAddition) || value.contains("org.apache.falcon.atlas.service.AtlasService") || value.contains("org.apache.atlas.falcon.service.AtlasService"))) {
            LOG.info("Appending '{}' to *.application.services from falcon-startup.properties since it doesn't contain it", atlasApplicationClassAddition);
            String newValue = value.trim().concat(atlasApplicationClassAddition);
            updateConfigurationPropertiesForCluster(cluster, "falcon-startup.properties", Collections.singletonMap(applicationServicesPropertyName, newValue), null, true, false);
          }
        }
      }
    }
  }

  /**
   * Updates the Spark-related configurations for the clusters managed by this Ambari
   * Removes falcon_store_uri from falcon-env.
   * Updates {{hdp_full_version}} to {{full_stack_version}}
   * @throws AmbariException if an error occurs while updating the configurations
   */
  protected void updateSparkConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);


    for (final Cluster cluster : clusterMap.values()) {
      Config sparkDefaultsConfig = cluster.getDesiredConfigByType("spark-defaults");
      if (sparkDefaultsConfig != null) {
        Map<String, String> updates = new HashMap<>();
        Map<String, String> sparkDefaultsProperties = sparkDefaultsConfig.getProperties();
        if (sparkDefaultsProperties.containsKey("spark.driver.extraJavaOptions")) {
          LOG.info("Updating property spark.driver.extraJavaOptions in spark-defaults");
          String oldValue = sparkDefaultsProperties.get("spark.driver.extraJavaOptions");
          if(oldValue.contains("{{hdp_full_version}}")) {
            String newValue = oldValue.replace("{{hdp_full_version}}", "{{full_stack_version}}");
            updates.put("spark.driver.extraJavaOptions", newValue);
          }
        }
        if (sparkDefaultsProperties.containsKey("spark.yarn.am.extraJavaOptions")) {
          LOG.info("Updating property spark.yarn.am.extraJavaOptions in spark-defaults");
          String oldValue = sparkDefaultsProperties.get("spark.yarn.am.extraJavaOptions");
          if(oldValue.contains("{{hdp_full_version}}")) {
            String newValue = oldValue.replace("{{hdp_full_version}}", "{{full_stack_version}}");
            updates.put("spark.yarn.am.extraJavaOptions", newValue);
          }
        }
        if(updates.size() != 0 ) {
          updateConfigurationPropertiesForCluster(cluster, "spark-defaults", updates, null, true, false);
        }
      }

      Config sparkJavaOptsConfig = cluster.getDesiredConfigByType("spark-javaopts-properties");
      if(sparkJavaOptsConfig != null) {
        Map<String, String> updates = new HashMap<>();
        Map<String, String> sparkJavaOptsProperties = sparkJavaOptsConfig.getProperties();
        if (sparkJavaOptsProperties.containsKey("content")) {
          String oldValue = sparkJavaOptsProperties.get("content");
          if(oldValue.contains("{{hdp_full_version}}")) {
            LOG.info("Updating property content in spark-javaopts-properties");
            String newValue = oldValue.replace("{{hdp_full_version}}", "{{full_stack_version}}");
            updates.put("content", newValue);
            updateConfigurationPropertiesForCluster(cluster, "spark-javaopts-properties", updates, null, true, false);
          }
        }
      }
    }
  }

  /**
   * Removes the HDFS/AMS alert definitions for the standard deviation alerts,
   * including all history, notifications and groupings.
   * <p/>
   * These alerts shipped disabled and were not functional in prior versions of
   * Ambari. This is the cleanest and simplest way to update them all as they
   * will be read back into Ambari on server startup.
   *
   * @throws SQLException
   */
  void removeStandardDeviationAlerts() throws SQLException {
    List<String> deviationAlertNames = Lists.newArrayList(
        "namenode_service_rpc_queue_latency_hourly",
        "namenode_client_rpc_queue_latency_hourly",
        "namenode_service_rpc_processing_latency_hourly",
        "namenode_client_rpc_processing_latency_hourly",
        "increase_nn_heap_usage_daily",
        "namenode_service_rpc_processing_latency_daily",
        "namenode_client_rpc_processing_latency_daily",
        "namenode_service_rpc_queue_latency_daily",
        "namenode_client_rpc_queue_latency_daily",
        "namenode_increase_in_storage_capacity_usage_daily",
        "increase_nn_heap_usage_weekly",
        "namenode_increase_in_storage_capacity_usage_weekly");

    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterId = cluster.getClusterId();

      for (String alertName : deviationAlertNames) {
        AlertDefinitionEntity definition = alertDefinitionDAO.findByName(clusterId, alertName);
        if (null != definition) {
          alertDefinitionDAO.remove(definition);
        }
      }
    }
  }

  /**
   * Removes the Atlas meta-server alert definition, including all history, notifications and groupings.
   *
   * @throws SQLException
   */
  void removeAtlasMetaserverAlert() throws SQLException {
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    String atlas_metastore_alert_name = "metadata_server_process";

    for (final Cluster cluster : clusterMap.values()) {
      long clusterId = cluster.getClusterId();

      AlertDefinitionEntity definition = alertDefinitionDAO.findByName(clusterId, atlas_metastore_alert_name);
      if (null != definition) {
        alertDefinitionDAO.remove(definition);
      }
    }
  }

  /**
   * Ensures that each user has only one explicit role.
   * <p>
   * Before Ambari 2.4.0, users were allowed to have multiple permissions, which were like roles.
   * In Ambari 2.4.0, the concept of roles was added, where each user may have a single role
   * explicitly assigned - other roles may be assumed based on group assignments and access to views.
   * <p>
   * For each user, determine the set of explicitly set roles and prune off all but the role with
   * the greater set of permissions.
   */
  void consolidateUserRoles() {
    LOG.info("Consolidating User Roles...");

    List<UserEntity> users = userDAO.findAll();
    if(users != null) {
      for (UserEntity user : users) {
        PrincipalEntity principal = user.getPrincipal();

        if (principal != null) {
          Set<PrivilegeEntity> privileges = principal.getPrivileges();

          if (privileges != null) {
            Map<ResourceEntity, Set<PrivilegeEntity>> resourceExplicitPrivileges = new HashMap<ResourceEntity, Set<PrivilegeEntity>>();
            PrivilegeEntity ambariAdministratorPrivilege = null;

            // Find the set of explicitly assigned roles per cluster
            for (PrivilegeEntity privilege : privileges) {
              ResourceEntity resource = privilege.getResource();

              if (resource != null) {
                ResourceTypeEntity resourceType = resource.getResourceType();

                if (resourceType != null) {
                  String type = resourceType.getName();

                  // If the privilege is for the CLUSTER or AMBARI, it is an explicitly assigned role.
                  if (ResourceType.CLUSTER.name().equalsIgnoreCase(type)) {
                    // If the privilege is for a CLUSTER, create a map of cluster to roles.
                    Set<PrivilegeEntity> explicitPrivileges = resourceExplicitPrivileges.get(resource);

                    if (explicitPrivileges == null) {
                      explicitPrivileges = new HashSet<PrivilegeEntity>();
                      resourceExplicitPrivileges.put(resource, explicitPrivileges);
                    }

                    explicitPrivileges.add(privilege);
                  } else if (ResourceType.AMBARI.name().equalsIgnoreCase(type)) {
                    // If the privilege is for AMBARI, assume the user is an Ambari Administrator.
                    ambariAdministratorPrivilege = privilege;
                  }
                }
              }
            }

            if(ambariAdministratorPrivilege != null) {
              // If the user is an Ambari admin, add that privilege to each set of privileges
              for (Set<PrivilegeEntity> explicitPrivileges : resourceExplicitPrivileges.values()) {
                explicitPrivileges.add(ambariAdministratorPrivilege);
              }
            }

            // For each cluster resource, if the user has more than one role, prune off the lower
            // privileged roles.
            // If the user has roles for a cluster and is also an Ambari administrator
            // (ambariAdministratorPrivilege is not null), the Ambari administrator role takes
            // precedence over all other roles
            for (Map.Entry<ResourceEntity, Set<PrivilegeEntity>> entry : resourceExplicitPrivileges.entrySet()) {
              Set<PrivilegeEntity> explicitPrivileges = entry.getValue();

              if (explicitPrivileges.size() > 1) {
                LOG.info("{} has {} explicitly assigned roles for the cluster {}, consolidating...",
                    user.getUserName(), explicitPrivileges.size(), getClusterName(entry.getKey()));

                PrivilegeEntity toKeep = null;
                PrivilegeEntity toRemove = null;

                for (PrivilegeEntity privilegeEntity : explicitPrivileges) {
                  if (toKeep == null) {
                    toKeep = privilegeEntity;
                  } else {
                    Integer toKeepLevel = ROLE_ORDER.get(toKeep.getPermission().getPermissionName());
                    Integer currentLevel = ROLE_ORDER.get(privilegeEntity.getPermission().getPermissionName());

                    // If the PrivilegeEntity currently set to be kept is ordered higher than the
                    // PrivilegeEntity being processed, move it to the list of PrivilegeEntities to
                    // be removed and remember the one being processed as the one the keep.
                    if (toKeepLevel > currentLevel) {
                      toRemove = toKeep;
                      toKeep = privilegeEntity;
                    }
                    else {
                      toRemove = privilegeEntity;
                    }

                    LOG.info("Removing the role {} from the set assigned to {} since {} is more powerful.",
                        toRemove.getPermission().getPermissionName(), user.getUserName(),
                        toKeep.getPermission().getPermissionName());

                    privilegeDAO.remove(toRemove);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Removes unnecessary authorizations(alert management) from CLUSTER.OPERATOR and SERVICE.ADMINISTRATOR.
   */
  void removeAuthorizations(){
    List<PermissionEntity> peList = new ArrayList<>();

    for(String name : Arrays.asList("CLUSTER.OPERATOR", "SERVICE.ADMINISTRATOR")) {
      PermissionEntity pe = permissionDAO.findPermissionByNameAndType(name, resourceTypeDAO.findByName("CLUSTER"));
      if (pe != null) {
        peList.add(pe);
      }
    }

    for(PermissionEntity pe : peList) {
      Collection<RoleAuthorizationEntity> authorizations = pe.getAuthorizations();
      for (Iterator<RoleAuthorizationEntity> iterator = authorizations.iterator(); iterator.hasNext();) {
        RoleAuthorizationEntity authorization = iterator.next();
        if ("SERVICE.TOGGLE_ALERTS".equals(authorization.getAuthorizationId()) || "SERVICE.MANAGE_ALERTS".equals(authorization.getAuthorizationId())) {
          iterator.remove();
        }
        if (pe.getPermissionName().equals("SERVICE.ADMINISTRATOR")
                && ("SERVICE.MOVE".equals(authorization.getAuthorizationId()) || "SERVICE.ENABLE_HA".equals(authorization.getAuthorizationId()))) {
          iterator.remove();
        }
      }
      permissionDAO.merge(pe);
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
          // Get the service that needs to be updated
          KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService("HBASE");

          if(serviceDescriptor != null) {
            KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent("PHOENIX_QUERY_SERVER");

            if (componentDescriptor != null) {
              // Get the identity that needs to be updated
              KerberosIdentityDescriptor origIdentityDescriptor = componentDescriptor.getIdentity("hbase_queryserver_hbase");

              if (origIdentityDescriptor != null) {

                // Create the new principal descriptor
                KerberosPrincipalDescriptor origPrincipalDescriptor = origIdentityDescriptor.getPrincipalDescriptor();
                KerberosPrincipalDescriptor newPrincipalDescriptor = new KerberosPrincipalDescriptor(
                    null,
                    null,
                    (origPrincipalDescriptor == null)
                        ? "hbase-site/phoenix.queryserver.kerberos.principal"
                        : origPrincipalDescriptor.getConfiguration(),
                    null);

                // Create the new keytab descriptor
                KerberosKeytabDescriptor origKeytabDescriptor = origIdentityDescriptor.getKeytabDescriptor();
                KerberosKeytabDescriptor newKeytabDescriptor = new KerberosKeytabDescriptor(
                    null,
                    null,
                    null,
                    null,
                    null,
                    (origKeytabDescriptor == null)
                        ? "hbase-site/phoenix.queryserver.keytab.file"
                        : origKeytabDescriptor.getConfiguration(),
                    false);

                // Remove the old identity
                componentDescriptor.removeIdentity("hbase_queryserver_hbase");

                // Add the new identity
                componentDescriptor.putIdentity(new KerberosIdentityDescriptor("phoenix_spnego", "/spnego", newPrincipalDescriptor, newKeytabDescriptor, null));

                artifactEntity.setArtifactData(kerberosDescriptor.toMap());
                artifactDAO.merge(artifactEntity);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Given a {@link ResourceEntity}, attempts to find the relevant cluster's name.
   *
   * @param resourceEntity a {@link ResourceEntity}
   * @return the relevant cluster's name
   */
  private String getClusterName(ResourceEntity resourceEntity) {
    ClusterEntity cluster = null;
    ResourceTypeEntity resourceType = resourceEntity.getResourceType();

    if (ResourceType.CLUSTER.name().equalsIgnoreCase(resourceType.getName())) {
      cluster = clusterDAO.findByResourceId(resourceEntity.getId());
    }

    return (cluster == null) ? "_unknown_" : cluster.getClusterName();
  }

  protected void updateHDFSWidgetDefinition() throws AmbariException {
    LOG.info("Updating HDFS widget definition.");

    Map<String, List<String>> widgetMap = new HashMap<>();
    Map<String, String> sectionLayoutMap = new HashMap<>();

    List<String> hdfsSummaryWidgets = Collections.singletonList("NameNode Operations");
    widgetMap.put("HDFS_SUMMARY", hdfsSummaryWidgets);
    sectionLayoutMap.put("HDFS_SUMMARY", "default_hdfs_dashboard");

    updateWidgetDefinitionsForService("HDFS", widgetMap, sectionLayoutMap);
  }

  /**
   * @return True if the stack is >=HDP-2.5, false otherwise.
   */
  protected boolean isAtLeastHdp25(StackId stackId) {
    if (null == stackId) {
      return false;
    }

    try {
      return stackId.compareTo(new StackId("HDP-2.5")) >= 0;
    } catch (Exception e) {
      // Different stack names throw an exception.
      return false;
    }
  }

  /**
   * Update Phoenix Query Server Kerberos configurations. Ambari 2.4 will alter the Phoenix Query Server to
   * supporting SPNEGO authentication which requires that the "HTTP/_HOST" principal and corresponding
   * keytab file instead of the generic HBase service keytab and principal it previously had.
   */
  protected void updatePhoenixConfigs() throws AmbariException {
    final AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);
    final Clusters clusters = controller.getClusters();

    if (null != clusters) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (null != clusterMap && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          StackId stackId = cluster.getCurrentStackVersion();

          // HBase is installed and Kerberos is enabled
          if (installedServices.contains("HBASE") && SecurityType.KERBEROS == cluster.getSecurityType() && isAtLeastHdp25(stackId)) {
            Config hbaseSite = cluster.getDesiredConfigByType(HBASE_SITE_CONFIG);
            if (null != hbaseSite) {
              Map<String, String> hbaseSiteProperties = hbaseSite.getProperties();
              // Get Phoenix Query Server kerberos config properties
              String pqsKrbPrincipal = hbaseSiteProperties.get(PHOENIX_QUERY_SERVER_PRINCIPAL_KEY);
              String pqsKrbKeytab = hbaseSiteProperties.get(PHOENIX_QUERY_SERVER_KEYTAB_KEY);

              // Principal and Keytab are set
              if (null != pqsKrbPrincipal && null != pqsKrbKeytab) {
                final Map<String, String> updatedKerberosProperties = new HashMap<>();
                final KerberosDescriptor defaultDescriptor = getKerberosDescriptor(cluster);

                KerberosIdentityDescriptor spnegoDescriptor = defaultDescriptor.getIdentity("spnego");
                if (null != spnegoDescriptor) {
                  // Add the SPNEGO config for the principal
                  KerberosPrincipalDescriptor principalDescriptor = spnegoDescriptor.getPrincipalDescriptor();
                  if (null != principalDescriptor) {
                    updatedKerberosProperties.put(PHOENIX_QUERY_SERVER_PRINCIPAL_KEY, principalDescriptor.getValue());
                  }

                  // Add the SPNEGO config for the keytab
                  KerberosKeytabDescriptor keytabDescriptor = spnegoDescriptor.getKeytabDescriptor();
                  if (null != keytabDescriptor) {
                    updatedKerberosProperties.put(PHOENIX_QUERY_SERVER_KEYTAB_KEY, keytabDescriptor.getFile());
                  }

                  // Update the configuration if we changed anything
                  if (!updatedKerberosProperties.isEmpty()) {
                    updateConfigurationProperties(HBASE_SITE_CONFIG, updatedKerberosProperties, true, false);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   *  Update properties with name
   *  yarn.timeline-server.url to yarn.ats.url
   */
  private void updateTezViewProperty() throws SQLException {
    dbAccessor.executeUpdate("UPDATE viewinstanceproperty SET name = 'yarn.ats.url' where name = 'yarn.timeline-server.url'");
  }

  /**
   *  Update view instance table's cluster_handle column to have cluster_id
   *  instead of cluster_name
   */
  void updateViewInstanceTable() throws SQLException {
    try {
      if (Long.class.equals(dbAccessor.getColumnClass(VIEWINSTANCE_TABLE, CLUSTER_HANDLE_COLUMN))) {
        LOG.info(String.format("%s column is already numeric. Skipping an update of %s table.", CLUSTER_HANDLE_COLUMN, VIEWINSTANCE_TABLE));
        return;
      }
    } catch (ClassNotFoundException e) {
      LOG.warn("Cannot determine a type of " + CLUSTER_HANDLE_COLUMN + " column.");
    }

    String cluster_handle_dummy = "cluster_handle_dummy";

    dbAccessor.addColumn(VIEWINSTANCE_TABLE,
      new DBColumnInfo(cluster_handle_dummy, Long.class, null, null, true));

    Statement statement = null;
    ResultSet resultSet = null;

    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        String selectSQL = String.format("SELECT cluster_id, cluster_name FROM %s",
          CLUSTER_TABLE);

        resultSet = statement.executeQuery(selectSQL);

        //Getting 1st cluster and updating view instance table with cluster_id
        if (null != resultSet && resultSet.next()) {
          String updateClusterTypeSQL = String.format(
              "UPDATE %s SET %s = '%s' WHERE cluster_handle IS NULL",
              VIEWINSTANCE_TABLE, "cluster_type", ClusterType.NONE.name());
          dbAccessor.executeQuery(updateClusterTypeSQL);

          final Long clusterId = resultSet.getLong("cluster_id");
          String updateSQL = String.format(
            "UPDATE %s SET %s = %d WHERE cluster_handle IS NOT NULL",
            VIEWINSTANCE_TABLE, cluster_handle_dummy, clusterId);

          dbAccessor.executeQuery(updateSQL);
        }
      }
    } finally {
      JdbcUtils.closeResultSet(resultSet);
      JdbcUtils.closeStatement(statement);
    }

    dbAccessor.dropColumn(VIEWINSTANCE_TABLE, CLUSTER_HANDLE_COLUMN);
    dbAccessor.renameColumn(VIEWINSTANCE_TABLE, cluster_handle_dummy,
      new DBColumnInfo(CLUSTER_HANDLE_COLUMN, Long.class, null, null, true));
  }

  /**
   *  If Capacity Scheduler instances configured as CUSTOM
   *  then upgrade them to Remote cluster
   *
   * @throws SQLException
   */
  protected void upgradeCapSchedulerView() throws SQLException {
    String capSchedulerViewName = "CAPACITY-SCHEDULER{1.0.0}";

    RemoteAmbariClusterDAO remoteClusterDAO = injector.getInstance(RemoteAmbariClusterDAO.class);
    ViewInstanceDAO instanceDAO = injector.getInstance(ViewInstanceDAO.class);

    List<ViewInstanceEntity> instances = instanceDAO.findAll();

    for (ViewInstanceEntity instance : instances) {
      if (instance.getViewName().equals(capSchedulerViewName) && instance.getClusterHandle() == null) {
        RemoteAmbariClusterEntity clusterEntity = new RemoteAmbariClusterEntity();
        clusterEntity.setName(instance.getName() + "-cluster");
        Map<String, String> propertyMap = instance.getPropertyMap();

        String url = propertyMap.get("ambari.server.url");
        String password = propertyMap.get("ambari.server.password");
        String username = propertyMap.get("ambari.server.username");

        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
          LOG.info("One of url, username, password is empty. Skipping upgrade for View Instance {}.", instance.getName());
          continue;
        }

        clusterEntity.setUrl(url);
        clusterEntity.setUsername(username);
        try {
          clusterEntity.setPassword(new DefaultMasker().unmask(password));
        } catch (MaskException e) {
          // ignore
        }

        remoteClusterDAO.save(clusterEntity);

        instance.setClusterHandle(clusterEntity.getId());
        instance.setClusterType(ClusterType.REMOTE_AMBARI);

        instanceDAO.merge(instance);
      }
    }
  }

  void fixAuthorizationDescriptions() throws SQLException {
    RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);
    RoleAuthorizationEntity roleAuthorization = roleAuthorizationDAO.findById("SERVICE.ADD_DELETE_SERVICES");

    if (roleAuthorization != null) {
      roleAuthorization.setAuthorizationName("Add/delete services");
      roleAuthorizationDAO.merge(roleAuthorization);
    }
  }

  protected void updateRangerHbasePluginProperties() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();

          if (installedServices.contains("HBASE") && installedServices.contains("RANGER")) {
            Config rangerHbasePluginProperties = cluster.getDesiredConfigByType("ranger-hbase-plugin-properties");
            Config clusterEnv = cluster.getDesiredConfigByType("cluster-env");

            if (rangerHbasePluginProperties != null && clusterEnv != null) {
              String smokeUserName = clusterEnv.getProperties().get("smokeuser");
              String policyUser = rangerHbasePluginProperties.getProperties().get("policy_user");
              if (policyUser != null && smokeUserName != null && !policyUser.equals(smokeUserName) && policyUser.equals("ambari-qa")) {
                updateConfigurationProperties("ranger-hbase-plugin-properties", Collections.singletonMap("policy_user", smokeUserName), true, false);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Update HBase Kerberos configurations. Ambari 2.4 will alter the HBase web UIs to
   * support SPNEGO authentication. HBase needs to have new keytab and principal properties
   * to enable SPNEGO authentication (if the user so chooses to enable it).
   */
  protected void updateHBaseConfigs() throws AmbariException {
    final AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);
    final Clusters clusters = controller.getClusters();

    if (null != clusters) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (null != clusterMap && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          StackId stackId = cluster.getCurrentStackVersion();

          // HBase is installed and Kerberos is enabled
          if (installedServices.contains("HBASE") && SecurityType.KERBEROS == cluster.getSecurityType()) {
            Config hbaseSite = cluster.getDesiredConfigByType(HBASE_SITE_CONFIG);

            if (null != hbaseSite) {
              Map<String, String> hbaseSiteProperties = hbaseSite.getProperties();

              // update classes based on krb/ranger availability
              boolean enableRangerHbase = false;
              boolean xmlConfigurationsSupported = false;

              Config rangerHbasePluginProperties = cluster.getDesiredConfigByType("ranger-hbase-plugin-properties");
              if (rangerHbasePluginProperties != null && rangerHbasePluginProperties.getProperties().containsKey("ranger-hbase-plugin-enabled")) {
                enableRangerHbase = rangerHbasePluginProperties.getProperties().get("ranger-hbase-plugin-enabled").toLowerCase() == "yes";
              }
              Config rangerEnv = cluster.getDesiredConfigByType("ranger-env");
              if (rangerEnv != null && rangerEnv.getProperties().containsKey("xml_configurations_supported")) {
                xmlConfigurationsSupported = Boolean.parseBoolean(rangerEnv.getProperties().get("xml_configurations_supported"));
              }

              final Map<String, String> updatedHbaseProperties = new HashMap<>();

              if (hbaseSiteProperties.containsKey("hbase.coprocessor.master.classes") &&
                  hbaseSiteProperties.get("hbase.coprocessor.master.classes").equals("{{hbase_coprocessor_master_classes}}")) {
                if (!enableRangerHbase) {
                  updatedHbaseProperties.put("hbase.coprocessor.master.classes", "org.apache.hadoop.hbase.security.access.AccessController");
                } else if (xmlConfigurationsSupported) {
                  updatedHbaseProperties.put("hbase.coprocessor.master.classes", "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor");
                } else {
                  updatedHbaseProperties.put("hbase.coprocessor.master.classes", "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor");
                }
              }

              if (hbaseSiteProperties.containsKey("hbase.coprocessor.regionserver.classes") &&
                  hbaseSiteProperties.get("hbase.coprocessor.regionserver.classes").equals("{{hbase_coprocessor_regionserver_classes}}")) {
                if (!enableRangerHbase) {
                  updatedHbaseProperties.put("hbase.coprocessor.regionserver.classes", "org.apache.hadoop.hbase.security.access.AccessController");
                } else if (xmlConfigurationsSupported) {
                  updatedHbaseProperties.put("hbase.coprocessor.regionserver.classes", "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor");
                } else {
                  updatedHbaseProperties.put("hbase.coprocessor.regionserver.classes", "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor");
                }
              }

              if (hbaseSiteProperties.containsKey("hbase.coprocessor.region.classes") &&
                  hbaseSiteProperties.get("hbase.coprocessor.region.classes").equals("{{hbase_coprocessor_region_classes}}")) {
                if (!enableRangerHbase) {
                  updatedHbaseProperties.put("hbase.coprocessor.region.classes", "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.hadoop.hbase.security.access.AccessController");
                } else if (xmlConfigurationsSupported) {
                  updatedHbaseProperties.put("hbase.coprocessor.region.classes", "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor");
                } else {
                  updatedHbaseProperties.put("hbase.coprocessor.region.classes", "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor");
                }
              }
              updateConfigurationProperties(HBASE_SITE_CONFIG, updatedHbaseProperties, true, false);


              if (isAtLeastHdp25(stackId)) {
                // Get any existing config properties (they probably don't exist)
                String principal = hbaseSiteProperties.get(HBASE_SPNEGO_PRINCIPAL_KEY);
                String keytab = hbaseSiteProperties.get(HBASE_SPNEGO_KEYTAB_KEY);

                final Map<String, String> updatedKerberosProperties = new HashMap<>();

                // Set the principal for SPNEGO if it's not already set
                if (null == principal) {
                  final KerberosDescriptor defaultDescriptor = getKerberosDescriptor(cluster);
                  final KerberosIdentityDescriptor spnegoDescriptor = defaultDescriptor.getIdentity("spnego");
                  if (null != spnegoDescriptor) {
                    // Add the SPNEGO config for the principal
                    KerberosPrincipalDescriptor principalDescriptor = spnegoDescriptor.getPrincipalDescriptor();
                    if (null != principalDescriptor) {
                      updatedKerberosProperties.put(HBASE_SPNEGO_PRINCIPAL_KEY, principalDescriptor.getValue());
                    }
                  }
                }

                // Set the keytab for SPNEGO if it's not already set
                if (null == keytab) {
                  final KerberosDescriptor defaultDescriptor = getKerberosDescriptor(cluster);
                  final KerberosIdentityDescriptor spnegoDescriptor = defaultDescriptor.getIdentity("spnego");
                  if (null != spnegoDescriptor) {
                    // Add the SPNEGO config for the keytab
                    KerberosKeytabDescriptor keytabDescriptor = spnegoDescriptor.getKeytabDescriptor();
                    if (null != keytabDescriptor) {
                      updatedKerberosProperties.put(HBASE_SPNEGO_KEYTAB_KEY, keytabDescriptor.getFile());
                    }
                  }
                }

                // Update the configuration if we changed anything
                if (!updatedKerberosProperties.isEmpty()) {
                  updateConfigurationProperties(HBASE_SITE_CONFIG, updatedKerberosProperties, true, false);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Update somketestentity class for pig view in viewentity table
   */
  protected void updatePigSmokeTestEntityClass() throws SQLException {
    String updateSQL = "UPDATE viewentity " +
      "SET class_name = 'org.apache.ambari.view.pig.persistence.SmokeTestEntity' " +
      "WHERE class_name = 'org.apache.ambari.view.pig.persistence.DataStoreStorage$SmokeTestEntity'";
    dbAccessor.executeUpdate(updateSQL);
  }
}
