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

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.KeyValueEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.State;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class UpgradeCatalog150 extends AbstractUpgradeCatalog {
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog150.class);
  private static final String quartzScriptFilePattern = "quartz.%s.sql";
  private Injector injector;

  @Inject
  public UpgradeCatalog150(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  @Override
  public String getTargetVersion() {
    return "1.5.0";
  }

  @Override
  public void executeDDLUpdates() throws AmbariException, SQLException {
    LOG.debug("Upgrading schema...");
    DatabaseType databaseType = configuration.getDatabaseType();
    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();

    // ========================================================================
    // Create tables

    // ClusterConfigMapping
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("type_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("version_tag", String.class, 255, null, false));
    columns.add(new DBColumnInfo("create_timestamp", Long.class, null, null, false));
    columns.add(new DBColumnInfo("selected", Integer.class, 0, null, false));
    columns.add(new DBColumnInfo("user_name", String.class, 255, "_db", false));

    dbAccessor.createTable("clusterconfigmapping", columns, "cluster_id", "type_name", "create_timestamp");

    // Request
    columns.clear();
    columns.add(new DBColumnInfo("request_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, true));
    columns.add(new DBColumnInfo("request_schedule_id", Long.class, null, null, true));
    columns.add(new DBColumnInfo("command_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("create_time", Long.class, null, null, true));
    columns.add(new DBColumnInfo("end_time", Long.class, null, null, true));
    columns.add(new DBColumnInfo("inputs", byte[].class, null, null, true));
    columns.add(new DBColumnInfo("request_context", String.class, 255, null, true));
    columns.add(new DBColumnInfo("request_type", String.class, 255, null, true));
    columns.add(new DBColumnInfo("start_time", Long.class, null, null, false));
    columns.add(new DBColumnInfo("status", String.class, 255));

    dbAccessor.createTable("request", columns, "request_id");

    // RequestSchedule
    columns.clear();
    columns.add(new DBColumnInfo("schedule_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("description", String.class, 255, null, true));
    columns.add(new DBColumnInfo("status", String.class, 255, null, true));
    columns.add(new DBColumnInfo("batch_separation_seconds", Integer.class, null, null, true));
    columns.add(new DBColumnInfo("batch_toleration_limit", Integer.class, null, null, true));
    columns.add(new DBColumnInfo("create_user", String.class, 255, null, true));
    columns.add(new DBColumnInfo("create_timestamp", Long.class, null, null, true));
    columns.add(new DBColumnInfo("update_user", String.class, 255, null, true));
    columns.add(new DBColumnInfo("update_timestamp", Long.class, null, null, true));
    columns.add(new DBColumnInfo("minutes", String.class, 10, null, true));
    columns.add(new DBColumnInfo("hours", String.class, 10, null, true));
    columns.add(new DBColumnInfo("days_of_month", String.class, 10, null, true));
    columns.add(new DBColumnInfo("month", String.class, 10, null, true));
    columns.add(new DBColumnInfo("day_of_week", String.class, 10, null, true));
    columns.add(new DBColumnInfo("yearToSchedule", String.class, 10, null, true));
    columns.add(new DBColumnInfo("startTime", String.class, 50, null, true));
    columns.add(new DBColumnInfo("endTime", String.class, 50, null, true));
    columns.add(new DBColumnInfo("last_execution_status", String.class, 255, null, true));

    dbAccessor.createTable("requestschedule", columns, "schedule_id");

    // RequestScheduleBatchRequest
    columns.clear();
    columns.add(new DBColumnInfo("schedule_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("batch_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("request_id", Long.class, null, null, true));
    columns.add(new DBColumnInfo("request_type", String.class, 255, null, true));
    columns.add(new DBColumnInfo("request_uri", String.class, 1024, null, true));
    columns.add(new DBColumnInfo("request_body", byte[].class, null, null, true));
    columns.add(new DBColumnInfo("request_status", String.class, 255, null, true));
    columns.add(new DBColumnInfo("return_code", Integer.class, null, null, true));
    columns.add(new DBColumnInfo("return_message", String.class, 2000, null, true));

    dbAccessor.createTable("requestschedulebatchrequest", columns, "schedule_id", "batch_id");

    // HostConfigMapping
    columns.clear();
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("host_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("type_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("version_tag", String.class, 255, null, true));
    columns.add(new DBColumnInfo("service_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("create_timestamp", Long.class, null, null, false));
    columns.add(new DBColumnInfo("selected", Integer.class, 0, null, false));

    dbAccessor.createTable("hostconfigmapping", columns, "cluster_id", "host_name", "type_name", "create_timestamp");

    // Sequences
    columns.clear();
    columns.add(new DBColumnInfo("sequence_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("value", Long.class, null, null, false));

    dbAccessor.createTable("ambari_sequences", columns, "sequence_name");

    // Metainfo

    columns.clear();
    columns.add(new DBColumnInfo("metainfo_key", String.class, 255, null, false));
    columns.add(new DBColumnInfo("metainfo_value", String.class, 255, null, false));

    dbAccessor.createTable("metainfo", columns, "metainfo_key");

    // ConfigGroup
    columns.clear();
    columns.add(new DBColumnInfo("group_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("group_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("tag", String.class, 1024, null, false));
    columns.add(new DBColumnInfo("description", String.class, 1024, null, true));
    columns.add(new DBColumnInfo("create_timestamp", Long.class, null, null, false));

    dbAccessor.createTable("configgroup", columns, "group_id");

    // ConfigGroupClusterConfigMapping
    columns.clear();
    columns.add(new DBColumnInfo("config_group_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("cluster_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("config_type", String.class, 255, null, false));
    columns.add(new DBColumnInfo("version_tag", String.class, 255, null, false));
    columns.add(new DBColumnInfo("user_name", String.class, 255, "_db", true));
    columns.add(new DBColumnInfo("create_timestamp", Long.class, null, null, false));

    dbAccessor.createTable("confgroupclusterconfigmapping", columns, "config_group_id", "cluster_id", "config_type");

    // ConfigGroupHostMapping
    columns.clear();
    columns.add(new DBColumnInfo("config_group_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("host_name", String.class, 255, null, false));

    dbAccessor.createTable("configgrouphostmapping", columns, "config_group_id", "host_name");

    // Blueprint
    columns.clear();
    columns.add(new DBColumnInfo("blueprint_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("stack_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("stack_version", String.class, 255, null, false));

    dbAccessor.createTable("blueprint", columns, "blueprint_name");

    // Blueprint Config
    columns.clear();
    columns.add(new DBColumnInfo("blueprint_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("type_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("config_data", byte[].class, null, null, false));

    dbAccessor.createTable("blueprint_configuration", columns, "blueprint_name", "type_name");

    // HostGroup
    columns.clear();
    columns.add(new DBColumnInfo("blueprint_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("cardinality", String.class, 255, null, false));

    dbAccessor.createTable("hostgroup", columns, "blueprint_name", "name");

    // HostGroupComponent
    columns.clear();
    columns.add(new DBColumnInfo("blueprint_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("hostgroup_name", String.class, 255, null, false));
    columns.add(new DBColumnInfo("name", String.class, 255, null, false));

    dbAccessor.createTable("hostgroup_component", columns, "blueprint_name", "hostgroup_name", "name");

    // RequestResourceFilter
    columns.clear();
    columns.add(new DBColumnInfo("filter_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("request_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("service_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("component_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("hosts", byte[].class, null, null, true));

    dbAccessor.createTable("requestresourcefilter", columns, "filter_id");

    createQuartzTables();

    // ========================================================================
    // Add columns

    dbAccessor.addColumn("hostcomponentdesiredstate", new DBColumnInfo("maintenance_state", String.class, 32, "OFF", false));
    dbAccessor.addColumn("servicedesiredstate", new DBColumnInfo("maintenance_state", String.class, 32, "OFF", false));
    dbAccessor.addColumn("hoststate", new DBColumnInfo("maintenance_state", String.class, 512, null, true));
    dbAccessor.addColumn("hostcomponentdesiredstate", new DBColumnInfo("admin_state", String.class, 32, null, true));
    dbAccessor.addColumn("hosts", new DBColumnInfo("ph_cpu_count", Integer.class, 32, null, true));
    dbAccessor.addColumn("clusterstate", new DBColumnInfo("current_stack_version", String.class, 255, null, false));
    dbAccessor.addColumn("hostconfigmapping", new DBColumnInfo("user_name", String.class, 255, "_db", false));
    dbAccessor.addColumn("stage", new DBColumnInfo("request_context", String.class, 255, null, true));
    dbAccessor.addColumn("stage", new DBColumnInfo("cluster_host_info", byte[].class, null, null, true));
    dbAccessor.addColumn("clusterconfigmapping", new DBColumnInfo("user_name", String.class, 255, "_db", false));
    dbAccessor.addColumn("host_role_command", new DBColumnInfo("end_time", Long.class, null, null, true));
    dbAccessor.addColumn("host_role_command", new DBColumnInfo("structured_out", byte[].class, null, null, true));
    dbAccessor.addColumn("host_role_command", new DBColumnInfo("command_detail", String.class, 255, null, true));
    dbAccessor.addColumn("host_role_command", new DBColumnInfo("custom_command_name", String.class, 255, null, true));

    // Alter columns

    if (databaseType == DatabaseType.POSTGRES) {
      if (dbAccessor.tableExists("hostcomponentdesiredconfigmapping")) {
        dbAccessor.executeQuery("ALTER TABLE hostcomponentdesiredconfigmapping rename to hcdesiredconfigmapping", true);
      }
      dbAccessor.executeQuery("ALTER TABLE users ALTER column user_id DROP DEFAULT", true);
      dbAccessor.executeQuery("ALTER TABLE users ALTER column ldap_user TYPE INTEGER USING CASE WHEN ldap_user=true THEN 1 ELSE 0 END", true);
    }

    if (databaseType == DatabaseType.ORACLE || databaseType == DatabaseType.POSTGRES) {
      if (dbAccessor.tableHasColumn("hosts", "disks_info")) {
        dbAccessor.executeQuery("ALTER TABLE hosts DROP COLUMN disks_info", true);
      }
    }


    //Move tables from ambarirca db to ambari db; drop ambarirca; Mysql
    if (databaseType == DatabaseType.MYSQL) {
      String dbName = configuration.getServerJDBCPostgresSchemaName();
      moveRCATableInMySQL("workflow", dbName);
      moveRCATableInMySQL("job", dbName);
      moveRCATableInMySQL("task", dbName);
      moveRCATableInMySQL("taskAttempt", dbName);
      moveRCATableInMySQL("hdfsEvent", dbName);
      moveRCATableInMySQL("mapreduceEvent", dbName);
      moveRCATableInMySQL("clusterEvent", dbName);
      dbAccessor.executeQuery("DROP DATABASE IF EXISTS ambarirca");
    }

    //Newly created tables should be filled before creating FKs
    // Request Entries
    String tableName = "request";
    if (!dbAccessor.tableExists(tableName)) {
      String msg = String.format("Table \"%s\" was not created during schema upgrade", tableName);
      LOG.error(msg);
      throw new AmbariException(msg);
    } else if (!dbAccessor.tableHasData(tableName)) {
      String query = null;
      if (databaseType == DatabaseType.POSTGRES) {
        query = getPostgresRequestUpgradeQuery();
      } else if (databaseType == DatabaseType.ORACLE) {
        query = getOracleRequestUpgradeQuery();
      } else if (databaseType == DatabaseType.MYSQL) {
        query = getMysqlRequestUpgradeQuery();
      }

      if (query != null) {
        dbAccessor.executeQuery(query);
      }
    } else {
      LOG.info("Table {} already filled", tableName);
    }

    // Drop old constraints
    // ========================================================================
    if (databaseType == DatabaseType.POSTGRES
      || databaseType == DatabaseType.MYSQL
      || databaseType == DatabaseType.DERBY) {

      //recreate old constraints to sync with oracle
      dbAccessor.dropFKConstraint("clusterconfigmapping", "FK_clusterconfigmapping_cluster_id");
      dbAccessor.dropFKConstraint("hostcomponentdesiredstate", "FK_hostcomponentdesiredstate_host_name");
      dbAccessor.dropFKConstraint("hostcomponentdesiredstate", "FK_hostcomponentdesiredstate_component_name");
      dbAccessor.dropFKConstraint("hostcomponentstate", "FK_hostcomponentstate_component_name");
      dbAccessor.dropFKConstraint("hostcomponentstate", "FK_hostcomponentstate_host_name");
      dbAccessor.dropFKConstraint("servicecomponentdesiredstate", "FK_servicecomponentdesiredstate_service_name");
      dbAccessor.dropFKConstraint("servicedesiredstate", "FK_servicedesiredstate_service_name");
      dbAccessor.dropFKConstraint("role_success_criteria", "FK_role_success_criteria_stage_id");
      dbAccessor.dropFKConstraint("ClusterHostMapping", "FK_ClusterHostMapping_host_name");
      dbAccessor.dropFKConstraint("ClusterHostMapping", "FK_ClusterHostMapping_cluster_id");

      dbAccessor.addFKConstraint("clusterconfigmapping", "clusterconfigmappingcluster_id", "cluster_id", "clusters", "cluster_id", false);
      dbAccessor.addFKConstraint("hostcomponentdesiredstate", "hstcmponentdesiredstatehstname", "host_name", "hosts", "host_name", false);
      dbAccessor.addFKConstraint("hostcomponentdesiredstate", "hstcmpnntdesiredstatecmpnntnme",
        new String[] {"component_name", "cluster_id", "service_name"}, "servicecomponentdesiredstate",
        new String[] {"component_name", "cluster_id", "service_name"}, false);
      dbAccessor.addFKConstraint("hostcomponentstate", "hstcomponentstatecomponentname",
        new String[] {"component_name", "cluster_id", "service_name"}, "servicecomponentdesiredstate",
        new String[] {"component_name", "cluster_id", "service_name"}, false);
      dbAccessor.addFKConstraint("hostcomponentstate", "hostcomponentstate_host_name", "host_name", "hosts", "host_name", false);
      dbAccessor.addFKConstraint("servicecomponentdesiredstate", "srvccmponentdesiredstatesrvcnm",
        new String[] {"service_name", "cluster_id"}, "clusterservices",
        new String[] {"service_name", "cluster_id"}, false);
      dbAccessor.addFKConstraint("servicedesiredstate", "servicedesiredstateservicename",
        new String[] {"service_name", "cluster_id"}, "clusterservices",
        new String[] {"service_name", "cluster_id"}, false);
      dbAccessor.addFKConstraint("role_success_criteria", "role_success_criteria_stage_id",
        new String[] {"stage_id", "request_id"}, "stage",
        new String[] {"stage_id", "request_id"}, false);
      dbAccessor.addFKConstraint("ClusterHostMapping", "ClusterHostMapping_cluster_id", "cluster_id", "clusters", "cluster_id", false);
      dbAccessor.addFKConstraint("ClusterHostMapping", "ClusterHostMapping_host_name", "host_name", "hosts", "host_name", false);


      //drop new constraints with to sync with oracle
      dbAccessor.dropFKConstraint("confgroupclusterconfigmapping", "FK_confgroupclusterconfigmapping_config_tag", true);
      dbAccessor.dropFKConstraint("confgroupclusterconfigmapping", "FK_confgroupclusterconfigmapping_group_id", true);
      dbAccessor.dropFKConstraint("configgrouphostmapping", "FK_configgrouphostmapping_configgroup_id", true);
      dbAccessor.dropFKConstraint("configgrouphostmapping", "FK_configgrouphostmapping_host_name", true);


    }

    // ========================================================================
    // Add constraints

    dbAccessor.addFKConstraint("stage", "FK_stage_request_id", "request_id", "request", "request_id", true);
    dbAccessor.addFKConstraint("request", "FK_request_cluster_id", "cluster_id", "clusters", "cluster_id", true);
    dbAccessor.addFKConstraint("request", "FK_request_schedule_id", "request_schedule_id", "requestschedule", "schedule_id", true);
    dbAccessor.addFKConstraint("requestschedulebatchrequest", "FK_rsbatchrequest_schedule_id", "schedule_id", "requestschedule", "schedule_id", true);
    dbAccessor.addFKConstraint("hostconfigmapping", "FK_hostconfmapping_cluster_id", "cluster_id", "clusters", "cluster_id", true);
    dbAccessor.addFKConstraint("hostconfigmapping", "FK_hostconfmapping_host_name", "host_name", "hosts", "host_name", true);
    dbAccessor.addFKConstraint("configgroup", "FK_configgroup_cluster_id", "cluster_id", "clusters", "cluster_id", true);
    dbAccessor.addFKConstraint("confgroupclusterconfigmapping", "FK_confg", new String[] {"version_tag", "config_type", "cluster_id"}, "clusterconfig", new String[] {"version_tag", "type_name", "cluster_id"}, true);
    dbAccessor.addFKConstraint("confgroupclusterconfigmapping", "FK_cgccm_gid", "config_group_id", "configgroup", "group_id", true);
    dbAccessor.addFKConstraint("configgrouphostmapping", "FK_cghm_cgid", "config_group_id", "configgroup", "group_id", true);
    dbAccessor.addFKConstraint("configgrouphostmapping", "FK_cghm_hname", "host_name", "hosts", "host_name", true);
    dbAccessor.addFKConstraint("clusterconfigmapping", "FK_clustercfgmap_cluster_id", "cluster_id", "clusters", "cluster_id", true);
    dbAccessor.addFKConstraint("requestresourcefilter", "FK_reqresfilter_req_id", "request_id", "request", "request_id", true);
    dbAccessor.addFKConstraint("hostgroup", "FK_hostgroup_blueprint_name", "blueprint_name", "blueprint", "blueprint_name", true);
    dbAccessor.addFKConstraint("hostgroup_component", "FK_hg_blueprint_name", "blueprint_name", "hostgroup", "blueprint_name", true);
    dbAccessor.addFKConstraint("hostgroup_component", "FK_hgc_blueprint_name", "hostgroup_name", "hostgroup", "name", true);
    dbAccessor.addFKConstraint("blueprint_configuration", "FK_cfg_blueprint_name", "blueprint_name", "blueprint", "blueprint_name", true);
  }

  private void moveRCATableInMySQL(String tableName, String dbName) throws SQLException {
    if (!dbAccessor.tableExists(tableName)) {
      dbAccessor.executeQuery(String.format("RENAME TABLE ambarirca.%s TO %s.%s", tableName, dbName, tableName), true);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void executePreDMLUpdates() {
    ;
  }

  @Override
  public void executeDMLUpdates() throws AmbariException, SQLException {
    // Service Config mapping
    String tableName = "serviceconfigmapping";
    DatabaseType databaseType = configuration.getDatabaseType();

    EntityManager em = getEntityManagerProvider().get();

    //unable to do via dao, as they were dropped
    //TODO should we move this to ddl and drop unused tables then?
    if (dbAccessor.tableExists(tableName)
      && dbAccessor.tableHasData(tableName)
      && dbAccessor.tableExists("clusterconfigmapping")) {

      if (databaseType == DatabaseType.POSTGRES) {
        // Service config mapping entity object will be deleted so need to
        // proceed with executing as query

        dbAccessor.executeQuery(getPostgresServiceConfigMappingQuery());

        dbAccessor.truncateTable(tableName);

      } else {
        LOG.warn("Unsupported database for service config mapping query. " +
          "database = " + databaseType);
      }
    }

    // Sequences
    if (dbAccessor.tableExists("ambari_sequences")) {
      if (databaseType == DatabaseType.POSTGRES) {
        Statement statement = null;
        ResultSet rs = null;
        try {
          statement = dbAccessor.getConnection().createStatement();
          if (statement != null) {
            rs = statement.executeQuery("select * from ambari_sequences where sequence_name in " +
              "('cluster_id_seq','user_id_seq','host_role_command_id_seq')");
            if (rs != null) {
              if (!rs.next()) {
                dbAccessor.executeQuery(getPostgresSequenceUpgradeQuery(), true);
                // Deletes
                dbAccessor.dropSequence("host_role_command_task_id_seq");
                dbAccessor.dropSequence("users_user_id_seq");
                dbAccessor.dropSequence("clusters_cluster_id_seq");
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
      }
    }

    //add new sequences for config groups
    String valueColumnName = "sequence_value";

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, " + valueColumnName + ") " +
      "VALUES('configgroup_id_seq', 1)", true);
    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, " + valueColumnName + ") " +
      "VALUES('requestschedule_id_seq', 1)", true);
    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, " + valueColumnName + ") " +
      "VALUES('resourcefilter_id_seq', 1)", true);

    //clear cache due to direct table manipulation
    ((JpaEntityManager)em.getDelegate()).getServerSession().getIdentityMapAccessor().invalidateAll();

    // Updates

    // HostComponentState - reverted to native query due to incorrect criteria api usage
    // (it forces us to use enums not strings, which were deleted)
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        EntityManager em = getEntityManagerProvider().get();
        Query nativeQuery = em.createNativeQuery("UPDATE hostcomponentstate SET current_state=?1 WHERE current_state in (?2, ?3)");
        nativeQuery.setParameter(1, "INSTALLED");
        nativeQuery.setParameter(2, "STOP_FAILED");
        nativeQuery.setParameter(3, "START_FAILED");
        nativeQuery.executeUpdate();
      }
    });

    // HostRoleCommand
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        EntityManager em = getEntityManagerProvider().get();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<HostRoleCommandEntity> c2 = cb.createQuery(HostRoleCommandEntity.class);
        Root<HostRoleCommandEntity> hrc = c2.from(HostRoleCommandEntity.class);
        List<HostRoleStatus> statuses = new ArrayList<HostRoleStatus>() {{
          add(HostRoleStatus.PENDING);
          add(HostRoleStatus.QUEUED);
          add(HostRoleStatus.IN_PROGRESS);
        }};
        Expression<String> exp = hrc.get("status");
        Predicate predicate = exp.in(statuses);
        c2.select(hrc).where(predicate);

        TypedQuery<HostRoleCommandEntity> q2 = em.createQuery(c2);
        List<HostRoleCommandEntity> r2 = q2.getResultList();

        HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
        if (r2 != null && !r2.isEmpty()) {
          for (HostRoleCommandEntity entity : r2) {
            entity.setStatus(HostRoleStatus.ABORTED);
            hostRoleCommandDAO.merge(entity);
          }
        }
      }
    });

    // Stack version changes from HDPLocal to HDP
    stackUpgradeUtil.updateStackDetails("HDP", null);

    //create cluster state entities if not present
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
        ClusterStateDAO clusterStateDAO = injector.getInstance(ClusterStateDAO.class);
        List<ClusterEntity> clusterEntities = clusterDAO.findAll();
        for (ClusterEntity clusterEntity : clusterEntities) {
          if (clusterStateDAO.findByPK(clusterEntity.getClusterId()) == null) {
            ClusterStateEntity clusterStateEntity = new ClusterStateEntity();
            clusterStateEntity.setClusterEntity(clusterEntity);
            clusterStateEntity.setCurrentStack(clusterEntity.getDesiredStack());

            clusterStateDAO.create(clusterStateEntity);

            clusterEntity.setClusterStateEntity(clusterStateEntity);

            clusterDAO.merge(clusterEntity);
          }
        }
      }
    });

    // add history server on the host where jobtracker is
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        addHistoryServer();
      }
    });

    // Add default log4j configs if they are absent
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        addMissingLog4jConfigs();
      }
    });

    // Move decommissioned datanode data to new table
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        try {
          processDecommissionedDatanodes();
        } catch (Exception e) {
          LOG.warn("Updating decommissioned datanodes to new format threw " +
            "exception. ", e);
        }
      }
    });
  }

  protected void addHistoryServer() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    HostDAO hostDao = injector.getInstance(HostDAO.class);

    List<ClusterEntity> clusterEntities = clusterDAO.findAll();
    for (final ClusterEntity clusterEntity : clusterEntities) {
      ServiceComponentDesiredStateEntityPK pkHS = new ServiceComponentDesiredStateEntityPK();
      pkHS.setComponentName("HISTORYSERVER");
      pkHS.setClusterId(clusterEntity.getClusterId());
      pkHS.setServiceName("MAPREDUCE");

      ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntityHS = serviceComponentDesiredStateDAO.findByPK(pkHS);

      // already have historyserver
      if(serviceComponentDesiredStateEntityHS != null) {
        continue;
      }

      ServiceComponentDesiredStateEntityPK pkJT = new ServiceComponentDesiredStateEntityPK();
      pkJT.setComponentName("JOBTRACKER");
      pkJT.setClusterId(clusterEntity.getClusterId());
      pkJT.setServiceName("MAPREDUCE");

      ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntityJT = serviceComponentDesiredStateDAO.findByPK(pkJT);

      // no jobtracker present probably mapreduce is not installed
      if(serviceComponentDesiredStateEntityJT == null) {
        continue;
      }


      HostComponentStateEntity jtHostComponentStateEntity = serviceComponentDesiredStateEntityJT.getHostComponentStateEntities().iterator().next();
      HostComponentDesiredStateEntity jtHostComponentDesiredStateEntity = serviceComponentDesiredStateEntityJT.getHostComponentDesiredStateEntities().iterator().next();
      String jtHostname = jtHostComponentStateEntity.getHostName();
      State jtCurrState = jtHostComponentStateEntity.getCurrentState();
      State jtHostComponentDesiredState = jtHostComponentDesiredStateEntity.getDesiredState();
      State jtServiceComponentDesiredState = serviceComponentDesiredStateEntityJT.getDesiredState();

      ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
      pk.setClusterId(clusterEntity.getClusterId());
      pk.setServiceName("MAPREDUCE");

      ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByPK(pk);

      final ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = new ServiceComponentDesiredStateEntity();
      serviceComponentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
      serviceComponentDesiredStateEntity.setComponentName("HISTORYSERVER");
      serviceComponentDesiredStateEntity.setDesiredStack(clusterEntity.getDesiredStack());
      serviceComponentDesiredStateEntity.setDesiredState(jtServiceComponentDesiredState);
      serviceComponentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
      serviceComponentDesiredStateEntity.setHostComponentDesiredStateEntities(new ArrayList<HostComponentDesiredStateEntity>());
      serviceComponentDesiredStateEntity.setHostComponentStateEntities(new ArrayList<HostComponentStateEntity>());

      serviceComponentDesiredStateDAO.create(serviceComponentDesiredStateEntity);

      final HostEntity host = hostDao.findByName(jtHostname);
      if (host == null) {
        continue;
      }

      final HostComponentStateEntity stateEntity = new HostComponentStateEntity();
      stateEntity.setHostEntity(host);
      stateEntity.setCurrentState(jtCurrState);
      stateEntity.setCurrentStack(clusterEntity.getDesiredStack());
      stateEntity.setClusterId(clusterEntity.getClusterId());

      final HostComponentDesiredStateEntity desiredStateEntity = new HostComponentDesiredStateEntity();
      desiredStateEntity.setDesiredState(jtHostComponentDesiredState);
      desiredStateEntity.setDesiredStack(clusterEntity.getDesiredStack());
      desiredStateEntity.setClusterId(clusterEntity.getClusterId());

      persistComponentEntities(stateEntity, desiredStateEntity, serviceComponentDesiredStateEntity);
    }
  }

  private void persistComponentEntities(HostComponentStateEntity stateEntity,
                                        HostComponentDesiredStateEntity desiredStateEntity,
                                        ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);

    HostEntity hostEntity = stateEntity.getHostEntity();

    desiredStateEntity.setHostEntity(hostEntity);
    desiredStateEntity.setServiceComponentDesiredStateEntity(serviceComponentDesiredStateEntity);
    serviceComponentDesiredStateEntity.getHostComponentDesiredStateEntities().add(desiredStateEntity);
    hostComponentDesiredStateDAO.create(desiredStateEntity);

    stateEntity.setHostEntity(hostEntity);
    stateEntity.setServiceComponentDesiredStateEntity(serviceComponentDesiredStateEntity);
    serviceComponentDesiredStateEntity.getHostComponentStateEntities().add(stateEntity);
    hostComponentStateDAO.create(stateEntity);

    serviceComponentDesiredStateDAO.merge(serviceComponentDesiredStateEntity);

    hostEntity.addHostComponentDesiredStateEntity(desiredStateEntity);
    hostEntity.addHostComponentStateEntity(stateEntity);

    hostDAO.merge(hostEntity);
  }

  protected void addMissingLog4jConfigs() {

    final String log4jConfigTypeContains = "log4j";
    final String defaultVersionTag = "version1";
    final String defaultUser = "admin";

    LOG.debug("Adding missing configs into Ambari DB.");
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    Gson gson = injector.getInstance(Gson.class);

    List <ClusterEntity> clusterEntities = clusterDAO.findAll();
    for (final ClusterEntity clusterEntity : clusterEntities) {
      Long clusterId = clusterEntity.getClusterId();
      StackEntity stackEntity = clusterEntity.getDesiredStack();
      String stackName = stackEntity.getStackName();
      String stackVersion = stackEntity.getStackVersion();

      List<ClusterServiceEntity> clusterServiceEntities = clusterServiceDAO.findAll();
      for (final ClusterServiceEntity clusterServiceEntity : clusterServiceEntities) {
        String serviceName = clusterServiceEntity.getServiceName();
        ServiceInfo serviceInfo = null;
        try {
          serviceInfo = ambariMetaInfo.getService(stackName, stackVersion, serviceName);
        } catch (AmbariException e) {
          LOG.error("Service " + serviceName + " not found for " + stackName + stackVersion);
          continue;
        }
        List<String> configTypes = serviceInfo.getConfigDependencies();
        if (configTypes != null) {
          for (String configType : configTypes) {
            if (configType.contains(log4jConfigTypeContains)) {
              ClusterConfigEntity configEntity = clusterDAO.findConfig(clusterId, configType, defaultVersionTag);

              if (configEntity == null) {
                String filename = configType + ".xml";
                Map<String, String> properties = new HashMap<String, String>();
                for (PropertyInfo propertyInfo : serviceInfo.getProperties()) {
                  if (filename.equals(propertyInfo.getFilename())) {
                    properties.put(propertyInfo.getName(), propertyInfo.getValue());
                  }
                }
                if (!properties.isEmpty()) {
                  String configData = gson.toJson(properties);
                  configEntity = new ClusterConfigEntity();
                  configEntity.setClusterId(clusterId);
                  configEntity.setType(configType);
                  configEntity.setTag(defaultVersionTag);
                  configEntity.setData(configData);
                  configEntity.setVersion(1L);
                  configEntity.setTimestamp(System.currentTimeMillis());
                  configEntity.setClusterEntity(clusterEntity);
                  configEntity.setStack(stackEntity);

                  LOG.debug("Creating new " + configType + " config...");
                  clusterDAO.createConfig(configEntity);

                  Collection<ClusterConfigMappingEntity> entities =
                    clusterEntity.getConfigMappingEntities();

                  ClusterConfigMappingEntity clusterConfigMappingEntity =
                    new ClusterConfigMappingEntity();
                  clusterConfigMappingEntity.setClusterEntity(clusterEntity);
                  clusterConfigMappingEntity.setClusterId(clusterId);
                  clusterConfigMappingEntity.setType(configType);
                  clusterConfigMappingEntity.setCreateTimestamp(
                    Long.valueOf(System.currentTimeMillis()));
                  clusterConfigMappingEntity.setSelected(1);
                  clusterConfigMappingEntity.setUser(defaultUser);
                  clusterConfigMappingEntity.setTag(configEntity.getTag());
                  entities.add(clusterConfigMappingEntity);
                  clusterDAO.persistConfigMapping(clusterConfigMappingEntity);
                  clusterDAO.merge(clusterEntity);
                }
              }
            }

          }

        }
      }
    }
    LOG.debug("Missing configs have been successfully added into Ambari DB.");
  }

  protected void processDecommissionedDatanodes() {
    KeyValueDAO keyValueDAO = injector.getInstance(KeyValueDAO.class);
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    Gson gson = injector.getInstance(Gson.class);
    HostComponentDesiredStateDAO desiredStateDAO = injector.getInstance
      (HostComponentDesiredStateDAO.class);

    KeyValueEntity keyValueEntity = keyValueDAO.findByKey("decommissionDataNodesTag");
    String value = null;
    if (keyValueEntity != null) {
      value = keyValueEntity.getValue();
      if (value != null && !value.isEmpty()) {
        List<ClusterEntity> clusterEntities = clusterDAO.findAll();
        for (ClusterEntity clusterEntity : clusterEntities) {
          Long clusterId = clusterEntity.getClusterId();
          ClusterConfigEntity configEntity = clusterDAO.findConfig(clusterId, "hdfs-exclude-file", value.trim());
          if (configEntity != null) {
            String configData = configEntity.getData();
            if (configData != null) {
              Map<String, String> properties = gson.<Map<String, String>>fromJson(configData, Map.class);
              if (properties != null && !properties.isEmpty()) {
                String decommissionedNodes = properties.get("datanodes");
                if (decommissionedNodes != null) {
                  String[] nodes = decommissionedNodes.split(",");
                  if (nodes.length > 0) {
                    for (String node : nodes) {
                      HostEntity hostEntity = hostDAO.findByName(node.trim());
                      HostComponentDesiredStateEntityPK entityPK =
                        new HostComponentDesiredStateEntityPK();
                      entityPK.setClusterId(clusterId);
                      entityPK.setServiceName("HDFS");
                      entityPK.setComponentName("DATANODE");
                      entityPK.setHostId(hostEntity.getHostId());
                      HostComponentDesiredStateEntity desiredStateEntity =
                        desiredStateDAO.findByPK(entityPK);
                      desiredStateEntity.setAdminState(HostComponentAdminState.DECOMMISSIONED);
                      desiredStateDAO.merge(desiredStateEntity);
                    }
                  }
                }
              }
            }
          }
        }
      }
      // Rename saved key value entity so that the move is finalized
      KeyValueEntity newEntity = new KeyValueEntity();
      newEntity.setKey("decommissionDataNodesTag-Moved");
      newEntity.setValue(value);
      keyValueDAO.create(newEntity);
      keyValueDAO.remove(keyValueEntity);
    }
  }

  private String getPostgresServiceConfigMappingQuery() {
    return "INSERT INTO clusterconfigmapping " +
      "(cluster_id, type_name, version_tag, create_timestamp, selected) " +
      "(SELECT DISTINCT cluster_id, config_type, config_tag, " +
      "cast(date_part('epoch', now()) as bigint), 1 " +
      "FROM serviceconfigmapping scm " +
      "WHERE timestamp = (SELECT max(timestamp) FROM serviceconfigmapping " +
      "WHERE cluster_id = scm.cluster_id AND config_type = scm.config_type))";
  }

  private String getPostgresSequenceUpgradeQuery() {
    return "INSERT INTO ambari_sequences(sequence_name, sequence_value) " +
      "SELECT 'cluster_id_seq', nextval('clusters_cluster_id_seq') " +
      "UNION ALL " +
      "SELECT 'user_id_seq', nextval('users_user_id_seq') " +
      "UNION ALL " +
      "SELECT 'host_role_command_id_seq', COALESCE((SELECT max(task_id) FROM host_role_command), 1) + 50 ";
  }

  private String getPostgresRequestUpgradeQuery() {
    return "insert into ambari.request(request_id, cluster_id, request_context, start_time, end_time, create_time) (\n" +
      "  select distinct s.request_id, s.cluster_id, s.request_context, coalesce (cmd.start_time, -1), coalesce (cmd.end_time, -1), -1\n" +
      "  from\n" +
      "    (select distinct request_id, cluster_id, request_context from ambari.stage ) s\n" +
      "    left join\n" +
      "    (select request_id, min(start_time) as start_time, max(end_time) as end_time from ambari.host_role_command group by request_id) cmd\n" +
      "    on s.request_id=cmd.request_id\n" +
      ")";
  }

  private String getOracleRequestUpgradeQuery() {
    return "INSERT INTO request" +
      "(request_id, cluster_id, request_context, start_time, end_time, create_time) " +
      "SELECT DISTINCT s.request_id, s.cluster_id, s.request_context, " +
      "nvl(cmd.start_time, -1), nvl(cmd.end_time, -1), -1 " +
      "FROM " +
      "(SELECT DISTINCT request_id, cluster_id, request_context FROM stage ) s " +
      "LEFT JOIN " +
      "(SELECT request_id, min(start_time) as start_time, max(end_time) " +
      "as end_time FROM host_role_command GROUP BY request_id) cmd " +
      "ON s.request_id=cmd.request_id";
  }

  private String getMysqlRequestUpgradeQuery() {
    return "insert into request" +
      "(request_id, cluster_id, request_context, start_time, end_time, create_time) " +
      "select distinct s.request_id, s.cluster_id, s.request_context, " +
      "coalesce(cmd.start_time, -1), coalesce(cmd.end_time, -1), -1 " +
      "from " +
      "(select distinct request_id, cluster_id, request_context from stage ) s " +
      "left join " +
      "(select request_id, min(start_time) as start_time, max(end_time) " +
      "as end_time from host_role_command group by request_id) cmd " +
      "on s.request_id=cmd.request_id";
  }

  private void createQuartzTables() throws SQLException {
    DatabaseType databaseType = configuration.getDatabaseType();

    // Run script to create quartz tables
    String scriptPath = configuration.getResourceDirPath() +
      File.separator + "upgrade" + File.separator + "ddl" +
      File.separator + String.format(quartzScriptFilePattern, databaseType.getName());

    try {
      dbAccessor.executeScript(scriptPath);
    } catch (IOException e) {
      LOG.error("Error reading file.", e);
    }

  }
}
