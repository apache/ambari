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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity_;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 1.7.0.
 */
public class UpgradeCatalog170 extends AbstractUpgradeCatalog {
  private static final String CONTENT_FIELD_NAME = "content";
  private static final String ENV_CONFIGS_POSTFIX = "-env";

  private static final String ALERT_TABLE_DEFINITION = "alert_definition";
  private static final String ALERT_TABLE_HISTORY = "alert_history";
  private static final String ALERT_TABLE_CURRENT = "alert_current";
  private static final String ALERT_TABLE_GROUP = "alert_group";
  private static final String ALERT_TABLE_TARGET = "alert_target";
  private static final String ALERT_TABLE_GROUP_TARGET = "alert_group_target";
  private static final String ALERT_TABLE_GROUPING = "alert_grouping";
  private static final String ALERT_TABLE_NOTICE = "alert_notice";

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
    List<DBColumnInfo> columns;
    String dbType = getDbType();

    // add group and members tables
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("group_id", Integer.class, 1, null, false));
    columns.add(new DBColumnInfo("principal_id", Integer.class, 1, null, false));
    columns.add(new DBColumnInfo("group_name", String.class, 1, null, false));
    columns.add(new DBColumnInfo("ldap_group", Integer.class, 1, 0, false));
    dbAccessor.createTable("groups", columns, "group_id");

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("member_id", Integer.class, 1, null, false));
    columns.add(new DBColumnInfo("group_id", Integer.class, 1, null, false));
    columns.add(new DBColumnInfo("user_id", Integer.class, 1, null, false));
    dbAccessor.createTable("members", columns, "member_id");

    // add admin tables and initial values prior to adding referencing columns on existing tables
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("principal_type_id", Integer.class, 1, null,
        false));
    columns.add(new DBColumnInfo("principal_type_name", String.class, null,
        null, false));

    dbAccessor.createTable("adminprincipaltype", columns, "principal_type_id");

    dbAccessor.insertRow("adminprincipaltype", new String[]{"principal_type_id", "principal_type_name"}, new String[]{"1", "'USER'"}, true);
    dbAccessor.insertRow("adminprincipaltype", new String[]{"principal_type_id", "principal_type_name"}, new String[]{"2", "'GROUP'"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("principal_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("principal_type_id", Integer.class, 1, null,
        false));

    dbAccessor.createTable("adminprincipal", columns, "principal_id");

    dbAccessor.insertRow("adminprincipal", new String[]{"principal_id", "principal_type_id"}, new String[]{"1", "1"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("resource_type_id", Integer.class, 1, null,
        false));
    columns.add(new DBColumnInfo("resource_type_name", String.class, null,
        null, false));

    dbAccessor.createTable("adminresourcetype", columns, "resource_type_id");

    dbAccessor.insertRow("adminresourcetype", new String[]{"resource_type_id", "resource_type_name"}, new String[]{"1", "'AMBARI'"}, true);
    dbAccessor.insertRow("adminresourcetype", new String[]{"resource_type_id", "resource_type_name"}, new String[]{"2", "'CLUSTER'"}, true);
    dbAccessor.insertRow("adminresourcetype", new String[]{"resource_type_id", "resource_type_name"}, new String[]{"3", "'VIEW'"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("resource_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("resource_type_id", Integer.class, 1, null,
        false));

    dbAccessor.createTable("adminresource", columns, "resource_id");

    dbAccessor.insertRow("adminresource", new String[]{"resource_id", "resource_type_id"}, new String[]{"1", "1"}, true);

    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("permission_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("permission_name", String.class, null, null,
        false));
    columns.add(new DBColumnInfo("resource_type_id", Integer.class, 1, null,
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

    if (dbType.equals(Configuration.ORACLE_DB_NAME)) {
      dbAccessor.executeQuery("ALTER TABLE clusterconfig ADD config_attributes CLOB NULL");
    } else {
      DBColumnInfo clusterConfigAttributesColumn = new DBColumnInfo(
          "config_attributes", String.class, 32000, null, true);
      dbAccessor.addColumn("clusterconfig", clusterConfigAttributesColumn);
    }

    // Add columns
    dbAccessor.addColumn("viewmain", new DBColumnInfo("mask",
      String.class, 255, null, true));
    dbAccessor.addColumn("viewparameter", new DBColumnInfo("masked",
      Character.class, 1, null, true));
    dbAccessor.addColumn("users", new DBColumnInfo("active",
      Integer.class, 1, 1, false));
    dbAccessor.addColumn("users", new DBColumnInfo("principal_id",
        Long.class, 1, 1, false));
    dbAccessor.addColumn("viewmain", new DBColumnInfo("resource_type_id",
        Integer.class, 1, 1, false));
    dbAccessor.addColumn("viewinstance", new DBColumnInfo("resource_id",
        Long.class, 1, 1, false));
    dbAccessor.addColumn("viewinstance", new DBColumnInfo("xml_driven",
        Character.class, 1, null, true));
    dbAccessor.addColumn("clusters", new DBColumnInfo("resource_id",
        Long.class, 1, 1, false));

    dbAccessor.addColumn("host_role_command", new DBColumnInfo("output_log",
        String.class, 255, null, true));
    dbAccessor.addColumn("host_role_command", new DBColumnInfo("error_log",
        String.class, 255, null, true));

    addAlertingFrameworkDDL();

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

    dbAccessor.dropConstraint("confgroupclusterconfigmapping", "FK_confg");

    if (Configuration.ORACLE_DB_NAME.equals(dbType)
      || Configuration.MYSQL_DB_NAME.equals(dbType)
      || Configuration.DERBY_DB_NAME.equals(dbType)) {
      dbAccessor.executeQuery("ALTER TABLE clusterconfig DROP PRIMARY KEY", true);
    } else if (Configuration.POSTGRES_DB_NAME.equals(dbType)) {
      dbAccessor.executeQuery("ALTER TABLE clusterconfig DROP CONSTRAINT clusterconfig_pkey CASCADE", true);
    }

    dbAccessor.addColumn("clusterconfig", new DBColumnInfo("config_id", Long.class, null, null, true));

    if (Configuration.ORACLE_DB_NAME.equals(dbType)) {
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
    } else if (Configuration.MYSQL_DB_NAME.equals(dbType)) {
      if (dbAccessor.tableHasData("clusterconfig")) {
        dbAccessor.executeQuery("UPDATE viewinstance " +
          "SET config_id = (SELECT @a := @a + 1 FROM (SELECT @a := 1) s)");
      }
    } else if (Configuration.POSTGRES_DB_NAME.equals(dbType)) {
      if (dbAccessor.tableHasData("clusterconfig")) {
        //window functions like row_number were added in 8.4, workaround for earlier versions (redhat/centos 5)
        dbAccessor.executeQuery("CREATE SEQUENCE temp_seq START WITH 1");
        dbAccessor.executeQuery("UPDATE clusterconfig SET config_id = nextval('temp_seq')");
        dbAccessor.dropSequence("temp_seq");
      }
    }

    //upgrade unit test workaround
    if (Configuration.DERBY_DB_NAME.equals(dbType)) {
      dbAccessor.executeQuery("ALTER TABLE clusterconfig ALTER COLUMN config_id DEFAULT 0");
      dbAccessor.executeQuery("ALTER TABLE clusterconfig ALTER COLUMN config_id NOT NULL");
    }

    dbAccessor.executeQuery("ALTER TABLE clusterconfig ADD PRIMARY KEY (config_id)");

    //fill version column
    dbAccessor.addColumn("clusterconfig", new DBColumnInfo("version", Long.class, null));

    populateConfigVersions();

    dbAccessor.setNullable("clusterconfig", "version", false);

    dbAccessor.executeQuery("ALTER TABLE clusterconfig ADD CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag)", true);
    dbAccessor.executeQuery("ALTER TABLE clusterconfig ADD CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version)", true);


    columns.clear();
    columns.add(new DBColumnInfo("service_config_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("service_name", String.class, null, null, false));
    columns.add(new DBColumnInfo("version", Long.class, null, null, false));
    columns.add(new DBColumnInfo("create_timestamp", Long.class, null, null, false));
    columns.add(new DBColumnInfo("user_name", String.class, null, "_db", false));
    columns.add(new DBColumnInfo("note", char[].class, null, null, true));
    dbAccessor.createTable("serviceconfig", columns, "service_config_id");

    dbAccessor.executeQuery("ALTER TABLE serviceconfig ADD CONSTRAINT UQ_scv_service_version UNIQUE (cluster_id, service_name, version)", true);

    columns.clear();
    columns.add(new DBColumnInfo("service_config_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("config_id", Long.class, null, null, false));
    dbAccessor.createTable("serviceconfigmapping", columns, "service_config_id", "config_id");

    dbAccessor.addFKConstraint("confgroupclusterconfigmapping", "FK_confg",
      new String[]{"cluster_id", "config_type", "version_tag"}, "clusterconfig",
      new String[]{"cluster_id", "type_name", "version_tag"}, true);



    //service config version sequences
    String valueColumnName = "\"value\"";
    if (Configuration.ORACLE_DB_NAME.equals(dbType)
      || Configuration.MYSQL_DB_NAME.equals(dbType)) {
      valueColumnName = "value";
    }

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('alert_definition_id_seq', 0)",
            false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('alert_group_id_seq', 0)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('alert_target_id_seq', 0)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('alert_history_id_seq', 0)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('alert_notice_id_seq', 0)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('alert_current_id_seq', 0)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('group_id_seq', 1)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('member_id_seq', 1)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('resource_type_id_seq', 4)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('resource_id_seq', 2)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('principal_type_id_seq', 3)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('principal_id_seq', 2)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('permission_id_seq', 5)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('privilege_id_seq', 1)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
            + valueColumnName + ") " + "VALUES('service_config_id_seq', 1)", false);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
      + valueColumnName + ") " + "VALUES('service_config_application_id_seq', 1)", false);

    long count = 1;
    ResultSet resultSet = null;
    try {
      resultSet = dbAccessor.executeSelect("SELECT count(*) FROM clusterconfig");
      if (resultSet.next()) {
        count = resultSet.getLong(1) + 2;
      }
    } finally {
      if (resultSet != null) {
        resultSet.close();
      }
    }

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
      + valueColumnName + ") " + "VALUES('config_id_seq', " + count + ")", false);

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

    dbAccessor.executeQuery("ALTER TABLE groups ADD CONSTRAINT UNQ_groups_0 UNIQUE (group_name, ldap_group)");
    dbAccessor.executeQuery("ALTER TABLE members ADD CONSTRAINT UNQ_members_0 UNIQUE (group_id, user_id)");
    dbAccessor.executeQuery("ALTER TABLE adminpermission ADD CONSTRAINT UQ_perm_name_resource_type_id UNIQUE (permission_name, resource_type_id)");
  }

  private void populateConfigVersions() throws SQLException {
    ResultSet resultSet = dbAccessor.executeSelect("SELECT DISTINCT type_name FROM clusterconfig ");
    Set<String> configTypes = new HashSet<String>();
    if (resultSet != null) {
      try {
        while (resultSet.next()) {
          configTypes.add(resultSet.getString("type_name"));
        }
      } finally {
        resultSet.close();
      }
    }

    //use new connection to not affect state of internal one
    Connection connection = dbAccessor.getNewConnection();
    PreparedStatement orderedConfigsStatement =
      connection.prepareStatement("SELECT config_id FROM clusterconfig WHERE type_name = ? ORDER BY create_timestamp");

    Map<String, List<Long>> configVersionMap = new HashMap<String, List<Long>>();
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

    orderedConfigsStatement.close();

    connection.setAutoCommit(false); //disable autocommit
    PreparedStatement configVersionStatement =
      connection.prepareStatement("UPDATE clusterconfig SET version = ? WHERE config_id = ?");


    try {
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
      configVersionStatement.close();
      connection.close();
    }

  }


  // ----- UpgradeCatalog ----------------------------------------------------

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    String dbType = getDbType();

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
    addMissingConfigs();
    upgradePermissionModel();
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
    columns.add(new DBColumnInfo("scope", String.class, 255, null, true));
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

    dbAccessor.executeQuery(
        "ALTER TABLE "
            + ALERT_TABLE_DEFINITION
            + " ADD CONSTRAINT uni_alert_def_name UNIQUE (cluster_id,definition_name)",
        false);

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
    dbAccessor.createTable(ALERT_TABLE_CURRENT, columns, "alert_id");

    dbAccessor.addFKConstraint(ALERT_TABLE_CURRENT, "fk_alert_current_def_id",
        "definition_id", ALERT_TABLE_DEFINITION, "definition_id", false);

    dbAccessor.addFKConstraint(ALERT_TABLE_CURRENT,
        "fk_alert_current_history_id", "history_id", ALERT_TABLE_HISTORY,
        "alert_id", false);

    dbAccessor.executeQuery("ALTER TABLE " + ALERT_TABLE_CURRENT
        + " ADD CONSTRAINT uni_alert_current_hist_id UNIQUE (history_id)",
        false);

    // alert_group
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("group_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("group_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("is_default", Short.class, 1, 1, false));
    dbAccessor.createTable(ALERT_TABLE_GROUP, columns, "group_id");

    dbAccessor.executeQuery(
        "ALTER TABLE "
            + ALERT_TABLE_GROUP
            + " ADD CONSTRAINT uni_alert_group_name UNIQUE (cluster_id,group_name)",
        false);

    // alert_target
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("target_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("target_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("notification_type", String.class, 64, null, false));
    columns.add(new DBColumnInfo("properties", char[].class, 32672, null, true));
    columns.add(new DBColumnInfo("description", String.class, 1024, null, true));
    dbAccessor.createTable(ALERT_TABLE_TARGET, columns, "target_id");

    dbAccessor.executeQuery("ALTER TABLE " + ALERT_TABLE_TARGET
        + " ADD CONSTRAINT uni_alert_target_name UNIQUE (target_name)",
        false);

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
    dbAccessor.createTable(ALERT_TABLE_NOTICE, columns, "notification_id");

    dbAccessor.addFKConstraint(ALERT_TABLE_NOTICE, "fk_alert_notice_target_id",
        "target_id", ALERT_TABLE_TARGET, "target_id", false);

    dbAccessor.addFKConstraint(ALERT_TABLE_NOTICE, "fk_alert_notice_hist_id",
        "history_id", ALERT_TABLE_HISTORY, "alert_id", false);

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
    updateConfigurationProperties("hbase-env",
        Collections.singletonMap("hbase_regionserver_xmn_max", "512"), false,
        false);

    updateConfigurationProperties("hbase-env",
        Collections.singletonMap("hbase_regionserver_xmn_ratio", "0.2"), false,
        false);
    
    updateConfigurationProperties("yarn-env",
        Collections.singletonMap("min_user_id", "1000"), false,
        false);
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
        Set<String> configTypes = configHelper.findConfigTypesByPropertyName(cluster.getCurrentStackVersion(), CONTENT_FIELD_NAME);

        for(String configType:configTypes) {
          if(!configType.endsWith(ENV_CONFIGS_POSTFIX)) {
            continue;
          }

          String value = configHelper.getPropertyValueFromStackDefenitions(cluster, configType, CONTENT_FIELD_NAME);
          updateConfigurationProperties(configType, Collections.singletonMap(CONTENT_FIELD_NAME, value), true, true);
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

          Set<String> newConfigTypes = configHelper.findConfigTypesByPropertyName(cluster.getCurrentStackVersion(), propertyName);
          // if it's custom user service global.xml can be still there.
          newConfigTypes.remove(Configuration.GLOBAL_CONFIG_TAG);

          String newConfigType = null;
          if(newConfigTypes.size() > 0) {
            newConfigType = newConfigTypes.iterator().next();
          } else {
            newConfigType = getAdditionalMappingGlobalToEnv().get(propertyName);
          }

          if(newConfigType==null) {
            LOG.warn("Cannot find where to map " + propertyName + " from " + Configuration.GLOBAL_CONFIG_TAG +
                " (value="+propertyValue+")");
            unmappedGlobalProperties.put(propertyName, propertyValue);
            continue;
          }

          LOG.info("Mapping config " + propertyName + " from " + Configuration.GLOBAL_CONFIG_TAG +
              " to " + newConfigType +
              " (value="+propertyValue+")");

          if(!newProperties.containsKey(newConfigType)) {
            newProperties.put(newConfigType, new HashMap<String, String>());
          }
          newProperties.get(newConfigType).put(propertyName, propertyValue);
        }

        for (Entry<String, Map<String, String>> newProperty : newProperties.entrySet()) {
          updateConfigurationProperties(newProperty.getKey(), newProperty.getValue(), true, true);
        }

        // if have some custom properties, for own services etc., leave that as it was
        if(unmappedGlobalProperties.size() != 0) {
          LOG.info("Not deleting globals because have custom properties");
          configHelper.createConfigType(cluster, ambariManagementController, Configuration.GLOBAL_CONFIG_TAG, unmappedGlobalProperties, "ambari-upgrade");
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
    result.put("kerberos_domain","hadoop-env");
    result.put("hbase_user_keytab","hbase-env");
    result.put("nagios_principal_name","nagios-env");
    result.put("nagios_keytab_path","nagios-env");
    result.put("oozie_keytab","oozie-env");
    result.put("zookeeper_principal_name","zookeeper-env");
    result.put("zookeeper_keytab_path","zookeeper-env");
    result.put("storm_principal_name","storm-env");
    result.put("storm_keytab","storm-env");

    return result;
  }

  private void upgradePermissionModel() {
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
    for (UserEntity user: userDAO.findAll()) {
      boolean hasAdminRole = false;
      boolean hasUserRole = false;
      for (RoleEntity role: user.getRoleEntities()) {
        if (role.getRoleName().equals("admin")) {
          hasAdminRole = true;
        }
        if (role.getRoleName().equals("user")) {
          hasUserRole = true;
        }
        if (hasAdminRole) {
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
        } else if (hasUserRole) {
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
    }
  }
}
