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

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupConfigMappingDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity_;
import org.apache.ambari.server.orm.entities.KeyValueEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.view.ViewRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 1.7.0.
 */
public class UpgradeCatalog170 extends AbstractUpgradeCatalog {
  private static final String CONTENT_FIELD_NAME = "content";
  private static final String PIG_CONTENT_FIELD_NAME = "pig-content";
  private static final String ENV_CONFIGS_POSTFIX = "-env";

  private static final String PIG_PROPERTIES_CONFIG_TYPE = "pig-properties";

  private static final String ALERT_TABLE_DEFINITION = "alert_definition";
  private static final String ALERT_TABLE_HISTORY = "alert_history";
  private static final String ALERT_TABLE_CURRENT = "alert_current";
  private static final String ALERT_TABLE_GROUP = "alert_group";
  private static final String ALERT_TABLE_TARGET = "alert_target";
  private static final String ALERT_TABLE_GROUP_TARGET = "alert_group_target";
  private static final String ALERT_TABLE_GROUPING = "alert_grouping";
  private static final String ALERT_TABLE_NOTICE = "alert_notice";
  public static final String JOBS_VIEW_NAME = "JOBS";
  public static final String VIEW_NAME_REG_EXP = JOBS_VIEW_NAME + "\\{.*\\}";
  public static final String JOBS_VIEW_INSTANCE_NAME = "JOBS_1";
  public static final String SHOW_JOBS_FOR_NON_ADMIN_KEY = "showJobsForNonAdmin";
  public static final String JOBS_VIEW_INSTANCE_LABEL = "Jobs";
  public static final String YARN_TIMELINE_SERVICE_WEBAPP_ADDRESS_PROPERTY = "yarn.timeline-service.webapp.address";
  public static final String YARN_RESOURCEMANAGER_WEBAPP_ADDRESS_PROPERTY = "yarn.resourcemanager.webapp.address";
  public static final String YARN_SITE = "yarn-site";
  public static final String YARN_ATS_URL_PROPERTY = "yarn.ats.url";
  public static final String YARN_RESOURCEMANAGER_URL_PROPERTY = "yarn.resourcemanager.url";

  public static final StackId CLUSTER_STATE_STACK_HDP_2_1 = new StackId("HDP",
      "2.1");

  //SourceVersion is only for book-keeping purpos
  @Override
  public String getSourceVersion() {
    return "1.6.1";
  }

  @Override
  public String getTargetVersion() {
    return "1.7.0";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger
      (UpgradeCatalog170.class);

  // ----- Constructors ------------------------------------------------------

  @Inject
  public UpgradeCatalog170(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  @Inject
  DaoUtils daoUtils;

  // ----- AbstractUpgradeCatalog --------------------------------------------

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    DatabaseType databaseType = configuration.getDatabaseType();

    // needs to be executed first
    renameSequenceValueColumnName();


    List<DBColumnInfo> columns;

    // add group and members tables
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("group_id", Integer.class, null, null, false));
    columns.add(new DBColumnInfo("principal_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("group_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("ldap_group", Integer.class, 1, 0, false));
    dbAccessor.createTable("groups", columns, "group_id");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("member_id", Integer.class, null, null, false));
    columns.add(new DBColumnInfo("group_id", Integer.class, null, null, false));
    columns.add(new DBColumnInfo("user_id", Integer.class, null, null, false));
    dbAccessor.createTable("members", columns, "member_id");

    // add admin tables and initial values prior to adding referencing columns on existing tables
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("principal_type_id", Integer.class, null, null,
        false));
    columns.add(new DBColumnInfo("principal_type_name", String.class, null,
        null, false));

    dbAccessor.createTable("adminprincipaltype", columns, "principal_type_id");

    dbAccessor.insertRow("adminprincipaltype", new String[]{"principal_type_id", "principal_type_name"}, new String[]{"1", "'USER'"}, true);
    dbAccessor.insertRow("adminprincipaltype", new String[]{"principal_type_id", "principal_type_name"}, new String[]{"2", "'GROUP'"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("principal_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("principal_type_id", Integer.class, null, null,
        false));

    dbAccessor.createTable("adminprincipal", columns, "principal_id");

    dbAccessor.insertRow("adminprincipal", new String[]{"principal_id", "principal_type_id"}, new String[]{"1", "1"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("resource_type_id", Integer.class, null, null,
        false));
    columns.add(new DBColumnInfo("resource_type_name", String.class, null,
        null, false));

    dbAccessor.createTable("adminresourcetype", columns, "resource_type_id");

    dbAccessor.insertRow("adminresourcetype", new String[]{"resource_type_id", "resource_type_name"}, new String[]{"1", "'AMBARI'"}, true);
    dbAccessor.insertRow("adminresourcetype", new String[]{"resource_type_id", "resource_type_name"}, new String[]{"2", "'CLUSTER'"}, true);
    dbAccessor.insertRow("adminresourcetype", new String[]{"resource_type_id", "resource_type_name"}, new String[]{"3", "'VIEW'"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("resource_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("resource_type_id", Integer.class, null, null,
        false));

    dbAccessor.createTable("adminresource", columns, "resource_id");

    dbAccessor.insertRow("adminresource", new String[]{"resource_id", "resource_type_id"}, new String[]{"1", "1"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("permission_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("permission_name", String.class, null, null,
        false));
    columns.add(new DBColumnInfo("resource_type_id", Integer.class, null, null,
        false));

    dbAccessor.createTable("adminpermission", columns, "permission_id");

    dbAccessor.insertRow("adminpermission", new String[]{"permission_id", "permission_name", "resource_type_id"}, new String[]{"1", "'AMBARI.ADMIN'", "1"}, true);
    dbAccessor.insertRow("adminpermission", new String[]{"permission_id", "permission_name", "resource_type_id"}, new String[]{"2", "'CLUSTER.READ'", "2"}, true);
    dbAccessor.insertRow("adminpermission", new String[]{"permission_id", "permission_name", "resource_type_id"}, new String[]{"3", "'CLUSTER.OPERATE'", "2"}, true);
    dbAccessor.insertRow("adminpermission", new String[]{"permission_id", "permission_name", "resource_type_id"}, new String[]{"4", "'VIEW.USE'", "3"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("privilege_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("permission_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("resource_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("principal_id", Long.class, null, null, false));

    dbAccessor.createTable("adminprivilege", columns, "privilege_id");

    dbAccessor.insertRow("adminprivilege", new String[]{"privilege_id", "permission_id", "resource_id", "principal_id"}, new String[]{"1", "1", "1", "1"}, true);

    String [] configAttributesTableNames = {"clusterconfig", "hostgroup_configuration", "blueprint_configuration"};

    for(String tableName : configAttributesTableNames) {
      addConfigAttributesColumn(tableName);
    }

    // Add columns
    dbAccessor.addColumn("viewmain", new DBColumnInfo("mask",
      String.class, 255, null, true));
    dbAccessor.addColumn("viewmain", new DBColumnInfo("system_view",
        Character.class, 1, null, true));
    dbAccessor.addColumn("viewmain", new DBColumnInfo("resource_type_id",
        Integer.class, null, 1, false));
    dbAccessor.addColumn("viewmain", new DBColumnInfo("description",
        String.class, 2048, null, true));
    dbAccessor.addColumn("viewparameter", new DBColumnInfo("masked",
      Character.class, 1, null, true));
    dbAccessor.addColumn("users", new DBColumnInfo("active",
      Integer.class, 1, 1, false));
    dbAccessor.addColumn("users", new DBColumnInfo("principal_id",
        Long.class, null, 1, false));
    dbAccessor.addColumn("viewinstance", new DBColumnInfo("resource_id",
        Long.class, null, 1, false));
    dbAccessor.addColumn("viewinstance", new DBColumnInfo("xml_driven",
        Character.class, 1, null, true));
    dbAccessor.addColumn("clusters", new DBColumnInfo("resource_id",
        Long.class, null, 1, false));

    dbAccessor.addColumn("host_role_command", new DBColumnInfo("output_log",
        String.class, 255, null, true));

    dbAccessor.addColumn("stage", new DBColumnInfo("command_params",
      byte[].class, null, null, true));
    dbAccessor.addColumn("stage", new DBColumnInfo("host_params",
      byte[].class, null, null, true));

    dbAccessor.addColumn("host_role_command", new DBColumnInfo("error_log",
        String.class, 255, null, true));

    addAlertingFrameworkDDL();

    // Exclusive requests changes
    dbAccessor.addColumn("request", new DBColumnInfo(
            "exclusive_execution", Integer.class, 1, 0, false));

    //service config versions changes

    //remove old artifacts (for versions <=1.4.1) which depend on tables changed
    //actually these had to be dropped in UC150, but some of tables were never used, and others were just cleared
    if (dbAccessor.tableExists("componentconfigmapping")) {
      dbAccessor.dropTable("componentconfigmapping");
    }
    if (dbAccessor.tableExists("hostcomponentconfigmapping")) {
      dbAccessor.dropTable("hostcomponentconfigmapping");
    }
    if (dbAccessor.tableExists("hcdesiredconfigmapping")) {
      dbAccessor.dropTable("hcdesiredconfigmapping");
    }
    if (dbAccessor.tableExists("serviceconfigmapping")) {
      dbAccessor.dropTable("serviceconfigmapping");
    }

    dbAccessor.dropFKConstraint("confgroupclusterconfigmapping", "FK_confg");

    if (databaseType == DatabaseType.ORACLE
        || databaseType == DatabaseType.MYSQL
        || databaseType == DatabaseType.DERBY) {
      dbAccessor.executeQuery("ALTER TABLE clusterconfig DROP PRIMARY KEY", true);
    } else if (databaseType == DatabaseType.POSTGRES) {
      dbAccessor.executeQuery("ALTER TABLE clusterconfig DROP CONSTRAINT clusterconfig_pkey CASCADE", true);
    }

    dbAccessor.addColumn("clusterconfig", new DBColumnInfo("config_id", Long.class, null, null, true));

    if (databaseType == DatabaseType.ORACLE) {
      //sequence looks to be simpler than rownum
      if (dbAccessor.tableHasData("clusterconfig")) {
        dbAccessor.executeQuery("CREATE SEQUENCE TEMP_SEQ " +
          "  START WITH 1 " +
          "  MAXVALUE 999999999999999999999999999 " +
          "  MINVALUE 1 " +
          "  NOCYCLE " +
          "  NOCACHE " +
          "  NOORDER");
        dbAccessor.executeQuery("UPDATE clusterconfig SET config_id = TEMP_SEQ.NEXTVAL");
        dbAccessor.dropSequence("TEMP_SEQ");
      }
    } else if (databaseType == DatabaseType.MYSQL) {
      if (dbAccessor.tableHasData("clusterconfig")) {
        dbAccessor.executeQuery("UPDATE clusterconfig " +
          "SET config_id = (SELECT @a := @a + 1 FROM (SELECT @a := 1) s)");
      }
    } else if (databaseType == DatabaseType.POSTGRES) {
      if (dbAccessor.tableHasData("clusterconfig")) {
        //window functions like row_number were added in 8.4, workaround for earlier versions (redhat/centos 5)
        dbAccessor.executeQuery("CREATE SEQUENCE temp_seq START WITH 1");
        dbAccessor.executeQuery("UPDATE clusterconfig SET config_id = nextval('temp_seq')");
        dbAccessor.dropSequence("temp_seq");
      }
    }

    // alter view tables description columns size
    if (databaseType == DatabaseType.ORACLE
        || databaseType == DatabaseType.MYSQL) {
      dbAccessor.executeQuery("ALTER TABLE viewinstance MODIFY description VARCHAR(2048)");
      dbAccessor.executeQuery("ALTER TABLE viewparameter MODIFY description VARCHAR(2048)");
    } else if (databaseType == DatabaseType.POSTGRES) {
      dbAccessor.executeQuery("ALTER TABLE viewinstance ALTER COLUMN description TYPE VARCHAR(2048)");
      dbAccessor.executeQuery("ALTER TABLE viewparameter ALTER COLUMN description TYPE VARCHAR(2048)");
    } else if (databaseType == DatabaseType.DERBY) {
      dbAccessor.executeQuery("ALTER TABLE viewinstance ALTER COLUMN description SET DATA TYPE VARCHAR(2048)");
      dbAccessor.executeQuery("ALTER TABLE viewparameter ALTER COLUMN description SET DATA TYPE VARCHAR(2048)");
    }

    //upgrade unit test workaround
    if (databaseType == DatabaseType.DERBY) {
      dbAccessor.executeQuery("ALTER TABLE clusterconfig ALTER COLUMN config_id DEFAULT 0");
      dbAccessor.executeQuery("ALTER TABLE clusterconfig ALTER COLUMN config_id NOT NULL");
    }

    dbAccessor.executeQuery("ALTER TABLE clusterconfig ADD PRIMARY KEY (config_id)");

    //fill version column
    dbAccessor.addColumn("clusterconfig", new DBColumnInfo("version", Long.class, null));

    populateConfigVersions();

    dbAccessor.setColumnNullable("clusterconfig", new DBColumnInfo("version", Long.class, null), false);

    dbAccessor.executeQuery("ALTER TABLE clusterconfig ADD CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag)", true);
    dbAccessor.executeQuery("ALTER TABLE clusterconfig ADD CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version)", true);

    if (databaseType != DatabaseType.ORACLE) {
      dbAccessor.alterColumn("clusterconfig", new DBColumnInfo("config_data", char[].class, null, null, false));
      dbAccessor.alterColumn("blueprint_configuration", new DBColumnInfo("config_data", char[].class, null, null, false));
      dbAccessor.alterColumn("hostgroup_configuration", new DBColumnInfo("config_data", char[].class, null, null, false));
    }

    columns.clear();
    columns.add(new DBColumnInfo("service_config_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("service_name", String.class, null, null, false));
    columns.add(new DBColumnInfo("version", Long.class, null, null, false));
    columns.add(new DBColumnInfo("create_timestamp", Long.class, null, null, false));
    columns.add(new DBColumnInfo("user_name", String.class, null, "_db", false));
    columns.add(new DBColumnInfo("note", char[].class, null, null, true));
    columns.add(new DBColumnInfo("group_id", Long.class, null, null, true));
    dbAccessor.createTable("serviceconfig", columns, "service_config_id");

    columns.clear();
    columns.add(new DBColumnInfo("service_config_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("hostname", String.class, 255, null, false));
    dbAccessor.createTable("serviceconfighosts", columns, "service_config_id", "hostname");

    dbAccessor.executeQuery("ALTER TABLE serviceconfig ADD CONSTRAINT UQ_scv_service_version UNIQUE (cluster_id, service_name, version)", true);

    columns.clear();
    columns.add(new DBColumnInfo("service_config_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("config_id", Long.class, null, null, false));
    dbAccessor.createTable("serviceconfigmapping", columns, "service_config_id", "config_id");

    dbAccessor.addFKConstraint("confgroupclusterconfigmapping", "FK_confg",
      new String[]{"cluster_id", "config_type", "version_tag"}, "clusterconfig",
      new String[]{"cluster_id", "type_name", "version_tag"}, true);

    dbAccessor.addFKConstraint("serviceconfighosts", "FK_scvhosts_scv",
      new String[]{"service_config_id"}, "serviceconfig",
      new String[]{"service_config_id"}, true);

    dbAccessor.addColumn("configgroup", new DBColumnInfo("service_name", String.class, 255));

    addSequences(Arrays.asList(
                                "alert_definition_id_seq",
                                "alert_group_id_seq",
                                "alert_target_id_seq",
                                "alert_history_id_seq",
                                "alert_notice_id_seq",
                                "alert_current_id_seq"
    ), 0L, false);
    addSequence("group_id_seq", 1L, false);
    addSequence("member_id_seq", 1L, false);
    addSequence("resource_type_id_seq", 4L, false);
    addSequence("resource_id_seq", 2L, false);
    addSequence("principal_type_id_seq", 3L, false);
    addSequence("principal_id_seq", 2L, false);
    addSequence("permission_id_seq", 5L, false);
    addSequence("privilege_id_seq", 1L, false);
    addSequence("service_config_id_seq", 1L, false);
    addSequence("service_config_application_id_seq", 1L, false);

    long count = 1;

    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        rs = statement.executeQuery("SELECT count(*) FROM clusterconfig");
        if (rs != null) {
          if (rs.next()) {
            count = rs.getLong(1) + 2;
          }
        }
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (statement != null) {
        statement.close();
      }
    }

    addSequence("config_id_seq", count, false);

    dbAccessor.addFKConstraint("users", "FK_users_principal_id", "principal_id", "adminprincipal", "principal_id", true);
    dbAccessor.addFKConstraint("clusters", "FK_clusters_resource_id", "resource_id", "adminresource", "resource_id", true);
    dbAccessor.addFKConstraint("viewinstance", "FK_viewinstance_resource_id", "resource_id", "adminresource", "resource_id", true);
    dbAccessor.addFKConstraint("adminresource", "FK_resource_resource_type_id", "resource_type_id", "adminresourcetype", "resource_type_id", true);
    dbAccessor.addFKConstraint("adminprincipal", "FK_principal_principal_type_id", "principal_type_id", "adminprincipaltype", "principal_type_id", true);
    dbAccessor.addFKConstraint("adminpermission", "FK_permission_resource_type_id", "resource_type_id", "adminresourcetype", "resource_type_id", true);
    dbAccessor.addFKConstraint("adminprivilege", "FK_privilege_permission_id", "permission_id", "adminpermission", "permission_id", true);
    dbAccessor.addFKConstraint("adminprivilege", "FK_privilege_resource_id", "resource_id", "adminresource", "resource_id", true);

    dbAccessor.addFKConstraint("groups", "FK_groups_principal_id", "principal_id", "adminprincipal", "principal_id", true);
    dbAccessor.addFKConstraint("members", "FK_members_user_id", "user_id", "users", "user_id", true);
    dbAccessor.addFKConstraint("members", "FK_members_group_id", "group_id", "groups", "group_id", true);

    dbAccessor.addUniqueConstraint("groups", "UNQ_groups_0", "group_name", "ldap_group");
    dbAccessor.addUniqueConstraint("members", "UNQ_members_0", "group_id", "user_id");
    dbAccessor.addUniqueConstraint("adminpermission", "UQ_perm_name_resource_type_id", "permission_name", "resource_type_id");
  }

  /**
   * @param tableName
   * @throws SQLException
   */
  private void addConfigAttributesColumn(String tableName) throws SQLException {
    final DatabaseType databaseType = configuration.getDatabaseType();
    if (databaseType == DatabaseType.ORACLE) {
      dbAccessor.addColumn(tableName, new DBColumnInfo("config_attributes", char[].class));
    } else {
      DBColumnInfo clusterConfigAttributesColumn = new DBColumnInfo(
          "config_attributes", Character[].class, null, null, true);
      dbAccessor.addColumn(tableName, clusterConfigAttributesColumn);
    }
  }

  /**
   * Note that you can't use dbAccessor.renameColumn(...) here as the column name is a reserved word and
   * thus requires custom approach for every database type.
   */
  private void renameSequenceValueColumnName() throws AmbariException, SQLException {
    final DatabaseType databaseType = configuration.getDatabaseType();
    if (dbAccessor.tableHasColumn("ambari_sequences", "sequence_value")) {
      return;
    }
    if (databaseType == DatabaseType.MYSQL) {
      dbAccessor.executeQuery("ALTER TABLE ambari_sequences CHANGE value sequence_value DECIMAL(38) NOT NULL");
    } else if (databaseType == DatabaseType.DERBY) {
      dbAccessor.executeQuery("RENAME COLUMN ambari_sequences.\"value\" to sequence_value");
    } else if (databaseType == DatabaseType.ORACLE) {
      dbAccessor.executeQuery("ALTER TABLE ambari_sequences RENAME COLUMN value to sequence_value");
    } else {
      // Postgres
      dbAccessor.executeQuery("ALTER TABLE ambari_sequences RENAME COLUMN \"value\" to sequence_value");
    }
  }

  private void populateConfigVersions() throws SQLException {
    ResultSet resultSet = null;
    Set<String> configTypes = new HashSet<String>();

    //use new connection to not affect state of internal one
    Connection connection = null;
    PreparedStatement orderedConfigsStatement = null;
    Map<String, List<Long>> configVersionMap = new HashMap<String, List<Long>>();
    try {
      connection = dbAccessor.getNewConnection();

      Statement statement = null;
      try {
        statement = connection.createStatement();
        if (statement != null) {
          resultSet = statement.executeQuery("SELECT DISTINCT type_name FROM clusterconfig ");
          if (resultSet != null) {
            while (resultSet.next()) {
              configTypes.add(resultSet.getString("type_name"));
            }
          }
        }
      } finally {
        if (statement != null) {
          statement.close();
        }
      }

      try {
        orderedConfigsStatement
                = connection.prepareStatement("SELECT config_id FROM clusterconfig WHERE type_name = ? ORDER BY create_timestamp");

        for (String configType : configTypes) {
          List<Long> configIds = new ArrayList<Long>();
          orderedConfigsStatement.setString(1, configType);
          resultSet = orderedConfigsStatement.executeQuery();
          if (resultSet != null) {
            try {
              while (resultSet.next()) {
                configIds.add(resultSet.getLong("config_id"));
              }
            } finally {
              resultSet.close();
            }
          }
          configVersionMap.put(configType, configIds);
        }
      } finally {
        if (orderedConfigsStatement != null) {
          orderedConfigsStatement.close();
        }
      }

      connection.setAutoCommit(false); //disable autocommit
      PreparedStatement configVersionStatement = null;
      try {
        configVersionStatement = connection.prepareStatement("UPDATE clusterconfig SET version = ? WHERE config_id = ?");

        for (Entry<String, List<Long>> entry : configVersionMap.entrySet()) {
          long version = 1L;
          for (Long configId : entry.getValue()) {
            configVersionStatement.setLong(1, version++);
            configVersionStatement.setLong(2, configId);
            configVersionStatement.addBatch();
          }
          configVersionStatement.executeBatch();
        }
        connection.commit(); //commit changes manually
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        if (configVersionStatement != null){
          configVersionStatement.close();
        }
      }
    } finally {
      if (connection != null) {
        connection.close();
      }
    }

  }


  // ----- UpgradeCatalog ----------------------------------------------------
  /**
   * {@inheritDoc}
   */
  @Override
  public void executePreDMLUpdates() {
    ;
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {

    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        try {
          moveHcatalogIntoHiveService();
          moveWebHcatIntoHiveService();
        } catch (Exception e) {
          LOG.warn("Integrating HCatalog and WebHCat services into Hive threw " +
              "exception. ", e);
        }
      }
    });

    // Update historic records with the log paths, but only enough so as to not prolong the upgrade process
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        try {
          HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
          EntityManager em = getEntityManagerProvider().get();
          CriteriaBuilder cb = em.getCriteriaBuilder();
          CriteriaQuery<HostRoleCommandEntity> cq1 = cb.createQuery(HostRoleCommandEntity.class);
          CriteriaQuery<HostRoleCommandEntity> cq2 = cb.createQuery(HostRoleCommandEntity.class);
          Root<HostRoleCommandEntity> hrc1 = cq1.from(HostRoleCommandEntity.class);
          Root<HostRoleCommandEntity> hrc2 = cq1.from(HostRoleCommandEntity.class);

          // Rather than using Java reflection, which is more susceptible to breaking, use the classname_.field canonical model
          // that is safer because it exposes the persistent attributes statically.
          Expression<Long> taskID1 = hrc1.get(HostRoleCommandEntity_.taskId);
          Expression<Long> taskID2 = hrc2.get(HostRoleCommandEntity_.taskId);
          Expression<String> outputLog = hrc1.get(HostRoleCommandEntity_.outputLog);
          Expression<String> errorLog = hrc2.get(HostRoleCommandEntity_.errorLog);

          Predicate p1 = cb.isNull(outputLog);
          Predicate p2 = cb.equal(outputLog, "");
          Predicate p1_or_2 = cb.or(p1, p2);

          Predicate p3 = cb.isNull(errorLog);
          Predicate p4 = cb.equal(errorLog, "");
          Predicate p3_or_4 = cb.or(p3, p4);

          if (daoUtils == null) {
            daoUtils = new DaoUtils();
          }

          // Update output_log
          cq1.select(hrc1).where(p1_or_2).orderBy(cb.desc(taskID1));
          TypedQuery<HostRoleCommandEntity> q1 = em.createQuery(cq1);
          q1.setMaxResults(1000);
          List<HostRoleCommandEntity> r1 = daoUtils.selectList(q1);
          for (HostRoleCommandEntity entity : r1) {
            entity.setOutputLog("/var/lib/ambari-agent/data/output-" + entity.getTaskId() + ".txt");
            hostRoleCommandDAO.merge(entity);
          }

          // Update error_log
          cq2.select(hrc2).where(p3_or_4).orderBy(cb.desc(taskID2));
          TypedQuery<HostRoleCommandEntity> q2 = em.createQuery(cq2);
          q2.setMaxResults(1000);
          List<HostRoleCommandEntity> r2 = daoUtils.selectList(q2);
          for (HostRoleCommandEntity entity : r2) {
            entity.setErrorLog("/var/lib/ambari-agent/data/errors-" + entity.getTaskId() + ".txt");
            hostRoleCommandDAO.merge(entity);
          }
        } catch (Exception e) {
          LOG.warn("Could not populate historic records with output_log and error_log in host_role_command table. ", e);
        }
      }
    });

    moveGlobalsToEnv();
    addEnvContentFields();
    renamePigProperties();
    upgradePermissionModel();
    addJobsViewPermissions();
    moveConfigGroupsGlobalToEnv();
    addMissingConfigs();
    updateClusterProvisionState();
    removeMapred2Log4jConfig();
  }

  private void removeMapred2Log4jConfig() {
    final ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);

    List<ClusterEntity> clusters = clusterDAO.findAll();
    for (ClusterEntity cluster : clusters) {
      for (ClusterConfigMappingEntity configMapping : cluster.getConfigMappingEntities()) {
        if (configMapping.getType().equals(Configuration.MAPREDUCE2_LOG4J_CONFIG_TAG)) {
          configMapping.setSelected(0);
          configMapping = clusterDAO.mergeConfigMapping(configMapping);
        }
      }
      clusterDAO.merge(cluster);
    }
  }

  public void updateClusterProvisionState() {
    // Change the provisioning_state of the cluster to INIT state
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        try {
          final ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
          EntityManager em = getEntityManagerProvider().get();
          List<ClusterEntity> clusterEntities = clusterDAO.findAll();
          for (ClusterEntity clusterEntity : clusterEntities) {
            clusterEntity.setProvisioningState(State.INSTALLED);
            em.merge(clusterEntity);
          }
        } catch (Exception e) {
          LOG.warn("Updating cluster provisioning_state to INSTALLED threw " +
              "exception. ", e);
        }
      }
    });
  }

  public void moveHcatalogIntoHiveService() throws AmbariException {
    final String serviceName = "HIVE";
    final String serviceNameToBeDeleted = "HCATALOG";
    final String componentName = "HCAT";
    moveComponentsIntoService(serviceName, serviceNameToBeDeleted, componentName);
  }

  private void moveWebHcatIntoHiveService() throws AmbariException {
    final String serviceName = "HIVE";
    final String serviceNameToBeDeleted = "WEBHCAT";
    final String componentName = "WEBHCAT_SERVER";
    moveComponentsIntoService(serviceName, serviceNameToBeDeleted, componentName);
  }

  private void moveComponentsIntoService(String serviceName, String serviceNameToBeDeleted, String componentName) throws AmbariException {
    EntityManager em = getEntityManagerProvider().get();
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ServiceDesiredStateDAO serviceDesiredStateDAO = injector.getInstance(ServiceDesiredStateDAO.class);
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);

    List<ClusterEntity> clusterEntities = clusterDAO.findAll();
    for (final ClusterEntity clusterEntity : clusterEntities) {
      ServiceComponentDesiredStateEntityPK pkHCATInHcatalog = new ServiceComponentDesiredStateEntityPK();
      pkHCATInHcatalog.setComponentName(componentName);
      pkHCATInHcatalog.setClusterId(clusterEntity.getClusterId());
      pkHCATInHcatalog.setServiceName(serviceNameToBeDeleted);
      ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntityToDelete = serviceComponentDesiredStateDAO.findByPK(pkHCATInHcatalog);

      if (serviceComponentDesiredStateEntityToDelete == null) {
        continue;
      }

      ServiceDesiredStateEntityPK serviceDesiredStateEntityPK = new ServiceDesiredStateEntityPK();
      serviceDesiredStateEntityPK.setClusterId(clusterEntity.getClusterId());
      serviceDesiredStateEntityPK.setServiceName(serviceNameToBeDeleted);
      ServiceDesiredStateEntity serviceDesiredStateEntity = serviceDesiredStateDAO.findByPK(serviceDesiredStateEntityPK);

      ClusterServiceEntityPK clusterServiceEntityToBeDeletedPK = new ClusterServiceEntityPK();
      clusterServiceEntityToBeDeletedPK.setClusterId(clusterEntity.getClusterId());
      clusterServiceEntityToBeDeletedPK.setServiceName(serviceNameToBeDeleted);
      ClusterServiceEntity clusterServiceEntityToBeDeleted = clusterServiceDAO.findByPK(clusterServiceEntityToBeDeletedPK);

      ClusterServiceEntityPK clusterServiceEntityPK = new ClusterServiceEntityPK();
      clusterServiceEntityPK.setClusterId(clusterEntity.getClusterId());
      clusterServiceEntityPK.setServiceName(serviceName);


      ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByPK(clusterServiceEntityPK);

      ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = new ServiceComponentDesiredStateEntity();
      serviceComponentDesiredStateEntity.setServiceName(serviceName);
      serviceComponentDesiredStateEntity.setComponentName(serviceComponentDesiredStateEntityToDelete.getComponentName());
      serviceComponentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
      serviceComponentDesiredStateEntity.setDesiredStack(serviceComponentDesiredStateEntityToDelete.getDesiredStack());
      serviceComponentDesiredStateEntity.setDesiredState(serviceComponentDesiredStateEntityToDelete.getDesiredState());
      serviceComponentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);

      Iterator<HostComponentDesiredStateEntity> hostComponentDesiredStateIterator = serviceComponentDesiredStateEntityToDelete.getHostComponentDesiredStateEntities().iterator();
      Iterator<HostComponentStateEntity> hostComponentStateIterator = serviceComponentDesiredStateEntityToDelete.getHostComponentStateEntities().iterator();

      while (hostComponentDesiredStateIterator.hasNext()) {
        HostComponentDesiredStateEntity hcDesiredStateEntityToBeDeleted = hostComponentDesiredStateIterator.next();
        HostComponentDesiredStateEntity hostComponentDesiredStateEntity = new HostComponentDesiredStateEntity();
        hostComponentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
        hostComponentDesiredStateEntity.setComponentName(hcDesiredStateEntityToBeDeleted.getComponentName());
        hostComponentDesiredStateEntity.setDesiredStack(hcDesiredStateEntityToBeDeleted.getDesiredStack());
        hostComponentDesiredStateEntity.setDesiredState(hcDesiredStateEntityToBeDeleted.getDesiredState());
        hostComponentDesiredStateEntity.setHostEntity(hcDesiredStateEntityToBeDeleted.getHostEntity());
        hostComponentDesiredStateEntity.setAdminState(hcDesiredStateEntityToBeDeleted.getAdminState());
        hostComponentDesiredStateEntity.setMaintenanceState(hcDesiredStateEntityToBeDeleted.getMaintenanceState());
        hostComponentDesiredStateEntity.setRestartRequired(hcDesiredStateEntityToBeDeleted.isRestartRequired());
        hostComponentDesiredStateEntity.setServiceName(serviceName);
        hostComponentDesiredStateEntity.setServiceComponentDesiredStateEntity(serviceComponentDesiredStateEntity);
        em.merge(hostComponentDesiredStateEntity);
        em.remove(hcDesiredStateEntityToBeDeleted);
      }

      while (hostComponentStateIterator.hasNext()) {
        HostComponentStateEntity hcStateToBeDeleted = hostComponentStateIterator.next();
        HostEntity hostToBeDeleted = hostDAO.findByName(hcStateToBeDeleted.getHostName());
        if (hostToBeDeleted == null) {
          continue;
        }

        HostComponentStateEntity hostComponentStateEntity = new HostComponentStateEntity();
        hostComponentStateEntity.setClusterId(clusterEntity.getClusterId());
        hostComponentStateEntity.setComponentName(hcStateToBeDeleted.getComponentName());
        hostComponentStateEntity.setCurrentStack(hcStateToBeDeleted.getCurrentStack());
        hostComponentStateEntity.setCurrentState(hcStateToBeDeleted.getCurrentState());
        hostComponentStateEntity.setHostEntity(hcStateToBeDeleted.getHostEntity());
        hostComponentStateEntity.setServiceName(serviceName);
        hostComponentStateEntity.setServiceComponentDesiredStateEntity(serviceComponentDesiredStateEntity);
        em.merge(hostComponentStateEntity);
        em.remove(hcStateToBeDeleted);
      }
      serviceComponentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
      em.merge(serviceComponentDesiredStateEntity);
      em.remove(serviceComponentDesiredStateEntityToDelete);
      em.remove(serviceDesiredStateEntity);
      em.remove(clusterServiceEntityToBeDeleted);
    }
  }


  private void moveConfigGroupsGlobalToEnv() throws AmbariException {
    final ConfigGroupConfigMappingDAO confGroupConfMappingDAO = injector.getInstance(ConfigGroupConfigMappingDAO.class);
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    final ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    List<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities = confGroupConfMappingDAO.findAll();
    List<ConfigGroupConfigMappingEntity> configGroupsWithGlobalConfigs = new ArrayList<ConfigGroupConfigMappingEntity>();
    Type type = new TypeToken<Map<String, String>>() {}.getType();

    for (ConfigGroupConfigMappingEntity entity : configGroupConfigMappingEntities) {
      if (entity.getConfigType().equals(Configuration.GLOBAL_CONFIG_TAG)) {
        configGroupsWithGlobalConfigs.add(entity);
      }
    }

    for (ConfigGroupConfigMappingEntity entity : configGroupsWithGlobalConfigs) {
      String configData = entity.getClusterConfigEntity().getData();
      StackEntity stackEntity = entity.getClusterConfigEntity().getStack();

      Map<String, String> properties = StageUtils.getGson().fromJson(configData, type);
      Cluster cluster = ambariManagementController.getClusters().getClusterById(entity.getClusterId());
      HashMap<String, HashMap<String, String>> configs = new HashMap<String, HashMap<String, String>>();

      for (Entry<String, String> property : properties.entrySet()) {
        Set<String> configTypes = configHelper.findConfigTypesByPropertyName(cluster.getCurrentStackVersion(),
                property.getKey(), cluster.getClusterName());
        // i'm not sure, but i hope that every service property is unique
        if (configTypes != null && configTypes.size() > 0) {
          String configType = configTypes.iterator().next();

          if (configs.containsKey(configType)) {
            HashMap<String, String> config = configs.get(configType);
            config.put(property.getKey(), property.getValue());
          } else {
            HashMap<String, String> config = new HashMap<String, String>();
            config.put(property.getKey(), property.getValue());
            configs.put(configType, config);
          }
        }
      }

      for (Entry<String, HashMap<String, String>> config : configs.entrySet()) {

        String tag;
        if(cluster.getConfigsByType(config.getKey()) == null) {
          tag = "version1";
        } else {
          tag = "version" + System.currentTimeMillis();
        }

        ClusterConfigEntity clusterConfigEntity = new ClusterConfigEntity();
        clusterConfigEntity.setClusterEntity(entity.getClusterConfigEntity().getClusterEntity());
        clusterConfigEntity.setClusterId(cluster.getClusterId());
        clusterConfigEntity.setType(config.getKey());
        clusterConfigEntity.setVersion(cluster.getNextConfigVersion(config.getKey()));
        clusterConfigEntity.setTag(tag);
        clusterConfigEntity.setTimestamp(new Date().getTime());
        clusterConfigEntity.setData(StageUtils.getGson().toJson(config.getValue()));
        clusterConfigEntity.setStack(stackEntity);

        clusterDAO.createConfig(clusterConfigEntity);

        ConfigGroupConfigMappingEntity configGroupConfigMappingEntity = new ConfigGroupConfigMappingEntity();
        configGroupConfigMappingEntity.setTimestamp(System.currentTimeMillis());
        configGroupConfigMappingEntity.setClusterId(entity.getClusterId());
        configGroupConfigMappingEntity.setClusterConfigEntity(clusterConfigEntity);
        configGroupConfigMappingEntity.setConfigGroupEntity(entity.getConfigGroupEntity());
        configGroupConfigMappingEntity.setConfigGroupId(entity.getConfigGroupId());
        configGroupConfigMappingEntity.setConfigType(config.getKey());
        configGroupConfigMappingEntity.setVersionTag(clusterConfigEntity.getTag());
        confGroupConfMappingDAO.create(configGroupConfigMappingEntity);
      }
    }

    for (ConfigGroupConfigMappingEntity entity : configGroupsWithGlobalConfigs) {
      confGroupConfMappingDAO.remove(entity);
    }
  }

  /**
   * Adds the alert tables and constraints.
   */
  private void addAlertingFrameworkDDL() throws AmbariException, SQLException {
    // alert_definition
    ArrayList<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("definition_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("definition_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("service_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("component_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("scope", String.class, 255, Scope.ANY.name(), false));
    columns.add(new DBColumnInfo("label", String.class, 255, null, true));
    columns.add(new DBColumnInfo("enabled", Short.class, 1, 1, false));
    columns.add(new DBColumnInfo("schedule_interval", Integer.class, null, null, false));
    columns.add(new DBColumnInfo("source_type", String.class, 255, null, false));
    columns.add(new DBColumnInfo("alert_source", char[].class, 32672, null,
        false));
    columns.add(new DBColumnInfo("hash", String.class, 64, null, false));
    dbAccessor.createTable(ALERT_TABLE_DEFINITION, columns, "definition_id");

    dbAccessor.addFKConstraint(ALERT_TABLE_DEFINITION,
        "fk_alert_def_cluster_id",
        "cluster_id", "clusters", "cluster_id", false);

    dbAccessor.addUniqueConstraint(ALERT_TABLE_DEFINITION, "uni_alert_def_name", "cluster_id", "definition_name");

    // alert_history
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("alert_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("alert_definition_id", Long.class, null, null,
        false));
    columns.add(new DBColumnInfo("service_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("component_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("host_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("alert_instance", String.class, 255, null,
        true));
    columns.add(new DBColumnInfo("alert_timestamp", Long.class, null, null,
        false));
    columns.add(new DBColumnInfo("alert_label", String.class, 1024, null, true));
    columns.add(new DBColumnInfo("alert_state", String.class, 255, null, false));
    columns.add(new DBColumnInfo("alert_text", String.class, 4000, null, true));
    dbAccessor.createTable(ALERT_TABLE_HISTORY, columns, "alert_id");

    dbAccessor.addFKConstraint(ALERT_TABLE_HISTORY, "fk_alert_history_def_id",
        "alert_definition_id", ALERT_TABLE_DEFINITION, "definition_id", false);

    dbAccessor.addFKConstraint(ALERT_TABLE_HISTORY,
        "fk_alert_history_cluster_id",
        "cluster_id", "clusters", "cluster_id", false);

    // alert_current
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("alert_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("definition_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("history_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("maintenance_state", String.class, 255, null,
        true));
    columns.add(new DBColumnInfo("original_timestamp", Long.class, 0, null,
        false));
    columns.add(new DBColumnInfo("latest_timestamp", Long.class, 0, null, false));
    columns.add(new DBColumnInfo("latest_text", String.class, 4000, null, true));
    dbAccessor.createTable(ALERT_TABLE_CURRENT, columns, "alert_id");

    dbAccessor.addFKConstraint(ALERT_TABLE_CURRENT, "fk_alert_current_def_id",
        "definition_id", ALERT_TABLE_DEFINITION, "definition_id", false);

    dbAccessor.addFKConstraint(ALERT_TABLE_CURRENT,
        "fk_alert_current_history_id", "history_id", ALERT_TABLE_HISTORY,
        "alert_id", false);

    dbAccessor.addUniqueConstraint(ALERT_TABLE_CURRENT, "uni_alert_current_hist_id", "history_id");

    // alert_group
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("group_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("group_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("is_default", Short.class, 1, 1, false));
    columns.add(new DBColumnInfo("service_name", String.class, 255, null, true));
    dbAccessor.createTable(ALERT_TABLE_GROUP, columns, "group_id");

    dbAccessor.addUniqueConstraint(ALERT_TABLE_GROUP, "uni_alert_group_name", "cluster_id", "group_name");

    // alert_target
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("target_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("target_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("notification_type", String.class, 64, null, false));
    columns.add(new DBColumnInfo("properties", char[].class, 32672, null, true));
    columns.add(new DBColumnInfo("description", String.class, 1024, null, true));
    dbAccessor.createTable(ALERT_TABLE_TARGET, columns, "target_id");

    dbAccessor.addUniqueConstraint(ALERT_TABLE_TARGET, "uni_alert_target_name", "target_name");

    // alert_group_target
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("group_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("target_id", Long.class, null, null, false));
    dbAccessor.createTable(ALERT_TABLE_GROUP_TARGET, columns, "group_id",
        "target_id");

    dbAccessor.addFKConstraint(ALERT_TABLE_GROUP_TARGET,
        "fk_alert_gt_group_id", "group_id", ALERT_TABLE_GROUP, "group_id",
        false);

    dbAccessor.addFKConstraint(ALERT_TABLE_GROUP_TARGET,
        "fk_alert_gt_target_id", "target_id", ALERT_TABLE_TARGET, "target_id",
        false);

    // alert_grouping
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("definition_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("group_id", Long.class, null, null, false));
    dbAccessor.createTable(ALERT_TABLE_GROUPING, columns, "group_id",
        "definition_id");

    dbAccessor.addFKConstraint(ALERT_TABLE_GROUPING,
        "fk_alert_grouping_def_id", "definition_id", ALERT_TABLE_DEFINITION,
        "definition_id", false);

    dbAccessor.addFKConstraint(ALERT_TABLE_GROUPING,
        "fk_alert_grouping_group_id", "group_id", ALERT_TABLE_GROUP,
        "group_id", false);

    // alert_notice
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("notification_id", Long.class, null, null,
        false));
    columns.add(new DBColumnInfo("target_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("history_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("notify_state", String.class, 255, null, false));
    columns.add(new DBColumnInfo("uuid", String.class, 64, null, false));
    dbAccessor.createTable(ALERT_TABLE_NOTICE, columns, "notification_id");

    dbAccessor.addFKConstraint(ALERT_TABLE_NOTICE, "fk_alert_notice_target_id",
        "target_id", ALERT_TABLE_TARGET, "target_id", false);

    dbAccessor.addFKConstraint(ALERT_TABLE_NOTICE, "fk_alert_notice_hist_id",
        "history_id", ALERT_TABLE_HISTORY, "alert_id", false);

    dbAccessor.addUniqueConstraint(ALERT_TABLE_NOTICE, "uni_alert_notice_uuid", "uuid");
    // Indexes
    dbAccessor.createIndex("idx_alert_history_def_id", ALERT_TABLE_HISTORY,
        "alert_definition_id");
    dbAccessor.createIndex("idx_alert_history_service", ALERT_TABLE_HISTORY,
        "service_name");
    dbAccessor.createIndex("idx_alert_history_host", ALERT_TABLE_HISTORY,
        "host_name");
    dbAccessor.createIndex("idx_alert_history_time", ALERT_TABLE_HISTORY,
        "alert_timestamp");
    dbAccessor.createIndex("idx_alert_history_state", ALERT_TABLE_HISTORY,
        "alert_state");
    dbAccessor.createIndex("idx_alert_group_name", ALERT_TABLE_GROUP,
        "group_name");
    dbAccessor.createIndex("idx_alert_notice_state", ALERT_TABLE_NOTICE,
        "notify_state");
  }

  protected void addMissingConfigs() throws AmbariException {
    addNewConfigurationsFromXml();
    updateOozieConfigs();
  }

  protected void updateOozieConfigs() throws AmbariException {
    final String PROPERTY_NAME = "log4j.appender.oozie.layout.ConversionPattern=";
    final String PROPERTY_VALUE_OLD = "%d{ISO8601} %5p %c{1}:%L - %m%n";
    final String PROPERTY_VALUE_NEW = "%d{ISO8601} %5p %c{1}:%L - SERVER[${oozie.instance.id}] %m%n";

    AmbariManagementController ambariManagementController = injector.getInstance(
        AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      Map<String, String> prop = new HashMap<String, String>();
      String content = null;

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          content = null;
          if (cluster.getDesiredConfigByType("oozie-log4j") != null) {
            content = cluster.getDesiredConfigByType(
                "oozie-log4j").getProperties().get("content");
          }

          if (content != null) {
            content = content.replace(PROPERTY_NAME + PROPERTY_VALUE_OLD,
                PROPERTY_NAME + PROPERTY_VALUE_NEW);

            prop.put("content", content);
            updateConfigurationPropertiesForCluster(cluster, "oozie-log4j",
                prop, true, false);
          }

          //oozie_heapsize is added for HDP2, we should check if it exists and not add it for HDP1
          if(cluster.getDesiredConfigByType("oozie-env") != null &&
              cluster.getDesiredConfigByType("oozie-env").getProperties().containsKey("oozie_heapsize")) {
            Map<String, String> oozieProps = new HashMap<String, String>();
            oozieProps.put("oozie_heapsize","2048m");
            oozieProps.put("oozie_permsize","256m");
            updateConfigurationPropertiesForCluster(cluster, "oozie-env",
                    oozieProps, true, false);
          }
        }
      }
    }
  }

  /**
   * Rename pig-content to content in pig-properties config
   * @throws AmbariException
   */
  protected void renamePigProperties() throws AmbariException {
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    AmbariManagementController ambariManagementController = injector.getInstance(
      AmbariManagementController.class);

    Clusters clusters = ambariManagementController.getClusters();
    if (clusters == null) {
      return;
    }

    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (final Cluster cluster : clusterMap.values()) {
        Config oldConfig = cluster.getDesiredConfigByType(PIG_PROPERTIES_CONFIG_TYPE);
        if (oldConfig != null) {
          Map<String, String> properties = oldConfig.getProperties();

          if(!properties.containsKey(CONTENT_FIELD_NAME)) {
            String value = properties.remove(PIG_CONTENT_FIELD_NAME);
            properties.put(CONTENT_FIELD_NAME, value);
            configHelper.createConfigType(cluster, ambariManagementController,
                PIG_PROPERTIES_CONFIG_TYPE, properties, "ambari-upgrade", null);
          }
        }
      }
    }
  }

  protected void addEnvContentFields() throws AmbariException {
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    AmbariManagementController ambariManagementController = injector.getInstance(
        AmbariManagementController.class);

    Clusters clusters = ambariManagementController.getClusters();
    if (clusters == null) {
      return;
    }

    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (final Cluster cluster : clusterMap.values()) {
        Set<String> configTypes = configHelper.findConfigTypesByPropertyName(cluster.getCurrentStackVersion(),
            CONTENT_FIELD_NAME, cluster.getClusterName());

        for(String configType:configTypes) {
          if(!configType.endsWith(ENV_CONFIGS_POSTFIX) && !configType.equals("pig-properties")) {
            continue;
          }

          updateConfigurationPropertiesWithValuesFromXml(configType, Collections.singleton(CONTENT_FIELD_NAME), false, true);
        }
      }
    }
  }

  protected void moveGlobalsToEnv() throws AmbariException {
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);

    AmbariManagementController ambariManagementController = injector.getInstance(
        AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters == null) {
      return;
    }
    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (final Cluster cluster : clusterMap.values()) {
        Config config = cluster.getDesiredConfigByType(Configuration.GLOBAL_CONFIG_TAG);
        if (config == null) {
          LOG.info("Config " + Configuration.GLOBAL_CONFIG_TAG + " not found. Assuming upgrade already done.");
          return;
        }

        Map<String, Map<String, String>> newProperties = new HashMap<String, Map<String, String>>();
        Map<String, String> globalProperites = config.getProperties();
        Map<String, String> unmappedGlobalProperties = new HashMap<String, String>();

        for (Map.Entry<String, String> property : globalProperites.entrySet()) {
          String propertyName = property.getKey();
          String propertyValue = property.getValue();
          String newPropertyName = getNewPropertyName().get(propertyName);

          Set<String> newConfigTypes = configHelper.findConfigTypesByPropertyName(cluster.getCurrentStackVersion(),
                  propertyName, cluster.getClusterName());
          // if it's custom user service global.xml can be still there.
          newConfigTypes.remove(Configuration.GLOBAL_CONFIG_TAG);

          String newConfigType = null;
          if(newConfigTypes.size() > 0) {
            newConfigType = newConfigTypes.iterator().next();
          } else {
            newConfigType = getAdditionalMappingGlobalToEnv().get(((newPropertyName == null)? propertyName : newPropertyName));
          }

          if(newConfigType==null) {
            LOG.warn("Cannot find where to map " + propertyName + " from " + Configuration.GLOBAL_CONFIG_TAG +
                " (value="+propertyValue+")");
            unmappedGlobalProperties.put(propertyName, propertyValue);
            continue;
          }

          LOG.info("Mapping config " + propertyName + " from " + Configuration.GLOBAL_CONFIG_TAG +
              " to " + ((newPropertyName == null)? propertyName : newPropertyName) + " property in " + newConfigType +
              " (value="+propertyValue+")");

          if(!newProperties.containsKey(newConfigType)) {
            newProperties.put(newConfigType, new HashMap<String, String>());
          }
          newProperties.get(newConfigType).put(((newPropertyName == null)? propertyName : newPropertyName), propertyValue);
        }

        for (Entry<String, Map<String, String>> newProperty : newProperties.entrySet()) {
          updateConfigurationProperties(newProperty.getKey(), newProperty.getValue(), false, true);
        }

        // if have some custom properties, for own services etc., leave that as it was
        if(unmappedGlobalProperties.size() != 0) {
          LOG.info("Not deleting globals because have custom properties");
          configHelper.createConfigType(cluster, ambariManagementController,
              Configuration.GLOBAL_CONFIG_TAG, unmappedGlobalProperties,
              "ambari-upgrade", null);
        } else {
          configHelper.removeConfigsByType(cluster, Configuration.GLOBAL_CONFIG_TAG);
        }
      }
    }
  }

  public static Map<String, String> getAdditionalMappingGlobalToEnv() {
    Map<String, String> result = new HashMap<String, String>();

    result.put("smokeuser_keytab","hadoop-env");
    result.put("hdfs_user_keytab","hadoop-env");
    result.put("hdfs_principal_name","hadoop-env");
    result.put("kerberos_domain","hadoop-env");
    result.put("hbase_user_keytab","hbase-env");
    result.put("hbase_principal_name", "hbase-env");
    result.put("nagios_principal_name","nagios-env");
    result.put("nagios_keytab_path","nagios-env");
    result.put("oozie_keytab","oozie-env");
    result.put("zookeeper_principal_name","zookeeper-env");
    result.put("zookeeper_keytab_path","zookeeper-env");
    result.put("storm_principal_name","storm-env");
    result.put("storm_keytab","storm-env");
    result.put("hive_hostname","hive-env");
    result.put("oozie_hostname","oozie-env");
    result.put("dataDir","zoo.cfg");

    return result;
  }

  public Map<String, String> getNewPropertyName() {
    Map<String, String> result = new HashMap<String, String>();
    result.put("zk_data_dir","dataDir");
    return result;
  }

  private void upgradePermissionModel() throws SQLException {
    final UserDAO userDAO = injector.getInstance(UserDAO.class);
    final PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    final PrincipalTypeDAO principalTypeDAO = injector.getInstance(PrincipalTypeDAO.class);
    final ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    final ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    final ResourceDAO resourceDAO = injector.getInstance(ResourceDAO.class);
    final ViewDAO viewDAO = injector.getInstance(ViewDAO.class);
    final ViewInstanceDAO viewInstanceDAO = injector.getInstance(ViewInstanceDAO.class);
    final PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    final PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);

    final PrincipalTypeEntity userPrincipalType = principalTypeDAO.findById(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
    for (UserEntity user: userDAO.findAll()) {
      final PrincipalEntity principalEntity = new PrincipalEntity();
      principalEntity.setPrincipalType(userPrincipalType);
      principalDAO.create(principalEntity);
      user.setPrincipal(principalEntity);
      userDAO.merge(user);
    }

    final ResourceTypeEntity clusterResourceType = resourceTypeDAO.findById(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    for (ClusterEntity cluster: clusterDAO.findAll()) {
      final ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setResourceType(clusterResourceType);
      resourceDAO.create(resourceEntity);
      cluster.setResource(resourceEntity);
      clusterDAO.merge(cluster);
    }

    for (ViewEntity view: viewDAO.findAll()) {
      final ResourceTypeEntity resourceType = new ResourceTypeEntity();
      resourceType.setName(ViewEntity.getViewName(view.getCommonName(), view.getVersion()));
      resourceTypeDAO.create(resourceType);
    }

    for (ViewInstanceEntity viewInstance: viewInstanceDAO.findAll()) {
      final ResourceEntity resourceEntity = new ResourceEntity();
      viewInstance.getViewEntity();
      resourceEntity.setResourceType(resourceTypeDAO.findByName(
          ViewEntity.getViewName(
              viewInstance.getViewEntity().getCommonName(),
              viewInstance.getViewEntity().getVersion())));
      viewInstance.setResource(resourceEntity);
      resourceDAO.create(resourceEntity);
      viewInstanceDAO.merge(viewInstance);
    }

    final PermissionEntity adminPermission = permissionDAO.findAmbariAdminPermission();
    final PermissionEntity clusterOperatePermission = permissionDAO.findClusterOperatePermission();
    final PermissionEntity clusterReadPermission = permissionDAO.findClusterReadPermission();
    final ResourceEntity ambariResource = resourceDAO.findAmbariResource();

    final Map<UserEntity, List<String>> roles = new HashMap<UserEntity, List<String>>();
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        rs = statement.executeQuery("SELECT role_name, user_id FROM user_roles");
        if (rs != null) {
          while (rs.next()) {
            final String roleName = rs.getString(1);
            final int userId = rs.getInt(2);

            final UserEntity user = userDAO.findByPK(userId);
            List<String> userRoles = roles.get(user);
            if (userRoles == null) {
              userRoles = new ArrayList<String>();
              roles.put(user, userRoles);
            }
            userRoles.add(roleName);
          }
        }
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (statement != null) {
        statement.close();
      }
    }
    for (UserEntity user: userDAO.findAll()) {
      List<String> userRoles = roles.get(user);
      if (userRoles.contains("admin")) {
        final PrivilegeEntity privilege = new PrivilegeEntity();
        privilege.setPermission(adminPermission);
        privilege.setPrincipal(user.getPrincipal());
        privilege.setResource(ambariResource);
        user.getPrincipal().getPrivileges().add(privilege);
        privilegeDAO.create(privilege);
        for (ClusterEntity cluster: clusterDAO.findAll()) {
          final PrivilegeEntity clusterPrivilege = new PrivilegeEntity();
          clusterPrivilege.setPermission(clusterOperatePermission);
          clusterPrivilege.setPrincipal(user.getPrincipal());
          clusterPrivilege.setResource(cluster.getResource());
          privilegeDAO.create(clusterPrivilege);
          user.getPrincipal().getPrivileges().add(clusterPrivilege);
        }
        userDAO.merge(user);
      } else if (userRoles.contains("user")) {
        for (ClusterEntity cluster: clusterDAO.findAll()) {
          final PrivilegeEntity privilege = new PrivilegeEntity();
          privilege.setPermission(clusterReadPermission);
          privilege.setPrincipal(user.getPrincipal());
          privilege.setResource(cluster.getResource());
          privilegeDAO.create(privilege);
          user.getPrincipal().getPrivileges().add(privilege);
        }
        userDAO.merge(user);
      }
    }

    dbAccessor.dropTable("user_roles");
    dbAccessor.dropTable("roles");
  }

  protected void addJobsViewPermissions() {

    final UserDAO userDAO = injector.getInstance(UserDAO.class);
    final ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    final ResourceDAO resourceDAO = injector.getInstance(ResourceDAO.class);
    final ViewDAO viewDAO = injector.getInstance(ViewDAO.class);
    final ViewInstanceDAO viewInstanceDAO = injector.getInstance(ViewInstanceDAO.class);
    final KeyValueDAO keyValueDAO = injector.getInstance(KeyValueDAO.class);
    final PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    final PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    final ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    final ViewRegistry viewRegistry = injector.getInstance(ViewRegistry.class);

    List<ClusterEntity> clusters = clusterDAO.findAll();
    if (!clusters.isEmpty()) {
      ClusterEntity currentCluster = clusters.get(0);
      StackEntity currentStack = currentCluster.getClusterStateEntity().getCurrentStack();

      boolean isStackHdp21 = CLUSTER_STATE_STACK_HDP_2_1.getStackName().equals(
          currentStack.getStackName())
          && CLUSTER_STATE_STACK_HDP_2_1.getStackVersion().equals(
              currentStack.getStackVersion());

      if (isStackHdp21) {
        ViewRegistry.initInstance(viewRegistry);
        viewRegistry.readViewArchives(VIEW_NAME_REG_EXP);
        ViewEntity jobsView = viewDAO.findByCommonName(JOBS_VIEW_NAME);

        if (jobsView != null) {
          ViewInstanceEntity jobsInstance = jobsView.getInstanceDefinition(JOBS_VIEW_INSTANCE_NAME);
          if (jobsInstance == null) {
            jobsInstance = new ViewInstanceEntity(jobsView, JOBS_VIEW_INSTANCE_NAME, JOBS_VIEW_INSTANCE_LABEL);
            ResourceEntity resourceEntity = new ResourceEntity();
            resourceEntity.setResourceType(resourceTypeDAO.findByName(
                ViewEntity.getViewName(
                    jobsView.getCommonName(),
                    jobsView.getVersion())));
            String atsHost;
            String rmHost;
            try {
              ClusterConfigEntity currentYarnConfig = null;
              for (ClusterConfigMappingEntity configMappingEntity : currentCluster.getConfigMappingEntities()) {
                if (YARN_SITE.equals(configMappingEntity.getType()) && configMappingEntity.isSelected() > 0) {
                  currentYarnConfig = clusterDAO.findConfig(currentCluster.getClusterId(),
                      configMappingEntity.getType(), configMappingEntity.getTag());
                  break;
                }
              }
              Type type = new TypeToken<Map<String, String>>() {}.getType();
              Map<String, String> yarnSiteProps = StageUtils.getGson().fromJson(currentYarnConfig.getData(), type);
              atsHost = yarnSiteProps.get(YARN_TIMELINE_SERVICE_WEBAPP_ADDRESS_PROPERTY);
              rmHost = yarnSiteProps.get(YARN_RESOURCEMANAGER_WEBAPP_ADDRESS_PROPERTY);
            } catch (Exception ex) {
              // Required properties failed to be set, therefore jobs instance should not be created
              return;
            }
            jobsInstance.setResource(resourceEntity);
            jobsInstance.putProperty(YARN_ATS_URL_PROPERTY, "http://" + atsHost);
            jobsInstance.putProperty(YARN_RESOURCEMANAGER_URL_PROPERTY, "http://" + rmHost);
            jobsView.addInstanceDefinition(jobsInstance);
            resourceDAO.create(resourceEntity);
            viewInstanceDAO.create(jobsInstance);
            viewDAO.merge(jobsView);
          }
          // get showJobsForNonAdmin value and remove it
          boolean showJobsForNonAdmin = false;
          KeyValueEntity showJobsKeyValueEntity = keyValueDAO.findByKey(SHOW_JOBS_FOR_NON_ADMIN_KEY);
          if (showJobsKeyValueEntity != null) {
            String value = showJobsKeyValueEntity.getValue();
            showJobsForNonAdmin = Boolean.parseBoolean(value);
            keyValueDAO.remove(showJobsKeyValueEntity);
          }
          if (showJobsForNonAdmin) {
            ResourceEntity jobsResource = jobsInstance.getResource();
            long jobsResourceId = jobsResource.getId();
            PermissionEntity viewUsePermission = permissionDAO.findViewUsePermission();
            PermissionEntity adminPermission = permissionDAO.findAmbariAdminPermission();
            int viewUsePermissionId = viewUsePermission.getId();
            int adminPermissionId = adminPermission.getId();
            for (UserEntity userEntity : userDAO.findAll()) {
              // check if user has VIEW.USE privilege for JOBS view
              List<PrivilegeEntity> privilegeEntities = privilegeDAO.findAllByPrincipal(
                  Collections.singletonList(userEntity.getPrincipal()));
              boolean hasJobsUsePrivilege = false;
              for (PrivilegeEntity privilegeEntity : privilegeEntities) {
                int privilegePermissionId = privilegeEntity.getPermission().getId();
                Long privilegeResourceId = privilegeEntity.getResource().getId();
                if ((privilegePermissionId == viewUsePermissionId && privilegeResourceId == jobsResourceId)
                    || privilegePermissionId == adminPermissionId) {
                  hasJobsUsePrivilege = true;
                  break;
                }
              }
              // if not - add VIEW.use privilege
              if (!hasJobsUsePrivilege) {
                PrivilegeEntity privilegeEntity = new PrivilegeEntity();
                privilegeEntity.setResource(jobsResource);
                privilegeEntity.setPermission(viewUsePermission);
                privilegeEntity.setPrincipal(userEntity.getPrincipal());
                privilegeDAO.create(privilegeEntity);
              }
            }
          }
        }
      }
    }

  }
}
