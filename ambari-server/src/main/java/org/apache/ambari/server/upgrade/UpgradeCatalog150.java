package org.apache.ambari.server.upgrade;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.state.State;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public void executeDDLUpdates() throws AmbariException, SQLException {
    LOG.debug("Upgrading schema...");

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
    columns.add(new DBColumnInfo("target_component", String.class, 255, null, true));
    columns.add(new DBColumnInfo("target_hosts", String.class, null, null, false));
    columns.add(new DBColumnInfo("target_service", String .class, 255, null, true));

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
    columns.add(new DBColumnInfo("request_id", Long.class, null, null, false));
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

    if (getDbType().equals(Configuration.POSTGRES_DB_NAME)) {
      try {
        dbAccessor.executeQuery("ALTER TABLE hostcomponentdesiredconfigmapping rename to hcdesiredconfigmapping;");
        dbAccessor.executeQuery("ALTER TABLE users ALTER column user_id DROP DEFAULT;");
        dbAccessor.executeQuery("ALTER TABLE users ALTER column ldap_user TYPE INTEGER USING CASE WHEN ldap_user=true THEN 1 ELSE 0 END;");
        dbAccessor.executeQuery("ALTER TABLE hosts DROP COLUMN disks_info;");
      } catch (SQLException e) {
        LOG.warn("Error encountered while altering schema. ", e);
        // continue updates
      }
    }

    //Move tables from ambarirca db to ambari db; drop ambarirca; Mysql
    if (getDbType().equals(Configuration.MYSQL_DB_NAME)) {
      String dbName = configuration.getServerJDBCSchemaName();
      moveRCATableInMySQL("workflow", dbName);
      moveRCATableInMySQL("job", dbName);
      moveRCATableInMySQL("task", dbName);
      moveRCATableInMySQL("taskAttempt", dbName);
      moveRCATableInMySQL("hdfsEvent", dbName);
      moveRCATableInMySQL("mapreduceEvent", dbName);
      moveRCATableInMySQL("clusterEvent", dbName);
      dbAccessor.executeQuery("DROP DATABASE IF EXISTS ambarirca;");
    }

    // ========================================================================
    // Add constraints

    dbAccessor.addFKConstraint("stage", "FK_stage_request_id", "request_id", "request", "request_id", true);
    dbAccessor.addFKConstraint("request", "FK_request_cluster_id", "cluster_id", "clusters", "cluster_id", true);
    dbAccessor.addFKConstraint("request", "FK_request_schedule_id", "request_schedule_id", "requestschedule", "schedule_id", true);
    dbAccessor.addFKConstraint("requestschedulebatchrequest", "FK_requestschedulebatchrequest_schedule_id", "schedule_id", "requestschedule", "schedule_id", true);
    dbAccessor.addFKConstraint("hostconfigmapping", "FK_hostconfigmapping_cluster_id", "cluster_id", "clusters", "cluster_id", true);
    dbAccessor.addFKConstraint("hostconfigmapping", "FK_hostconfigmapping_host_name", "host_name", "hosts", "host_name", true);
    dbAccessor.addFKConstraint("configgroup", "FK_configgroup_cluster_id", "cluster_id", "clusters", "cluster_id", true);
    dbAccessor.addFKConstraint("confgroupclusterconfigmapping", "FK_cg_cluster_cm_config_tag", new String[] {"version_tag", "config_type", "cluster_id"}, "clusterconfig", new String[] {"version_tag", "type_name", "cluster_id"}, true);
    dbAccessor.addFKConstraint("confgroupclusterconfigmapping", "FK_cg_cluster_cm_group_id", "config_group_id", "configgroup", "group_id", true);
    dbAccessor.addFKConstraint("confgrouphostmapping", "FK_cghostm_configgroup_id", "config_group_id", "configgroup", "group_id", true);
    dbAccessor.addFKConstraint("confgrouphostmapping", "FK_cghostm_host_name", "host_name", "hosts", "host_name", true);
    dbAccessor.addFKConstraint("clusterconfigmapping", "FK_clustercfgmap_cluster_id", "cluster_id", "clusters", "cluster_id", true);

    // ========================================================================
    // Finally update schema version
    updateMetaInfoVersion(getTargetVersion());
  }

  private void moveRCATableInMySQL(String tableName, String dbName) throws SQLException {
    if (!dbAccessor.tableExists(tableName)) {
      dbAccessor.executeQuery(String.format("RENAME TABLE ambarirca.%s TO %s.%s;", tableName, dbName, tableName), true);
    }
  }

  @Override
  public void executeDMLUpdates() throws AmbariException, SQLException {
    // Service Config mapping
    String tableName = "serviceconfigmapping";
    String dbType = getDbType();

    EntityManager em = getEntityManagerProvider().get();

    //unable to do via dao, as they were dropped
    //TODO should we move this to ddl and drop unused tables then?
    if (dbAccessor.tableExists(tableName)
      && dbAccessor.tableHasData(tableName)
      && dbAccessor.tableExists("clusterconfigmapping")) {

      if (dbType.equals(Configuration.POSTGRES_DB_NAME)) {
        // Service config mapping entity object will be deleted so need to
        // proceed with executing as query

        dbAccessor.executeQuery(getPostgresServiceConfigMappingQuery());

        dbAccessor.truncateTable(tableName);

      } else {
        LOG.warn("Unsupported database for service config mapping query. " +
          "database = " + dbType);
      }
    }


    // TODO: Convert all possible native queries using Criteria builder
    // Request Entries
    tableName = "request";
    if (dbAccessor.tableExists(tableName) &&
      !dbAccessor.tableHasData(tableName)) {

      String query;
      if (dbType.equals(Configuration.POSTGRES_DB_NAME)) {
        query = getPostgresRequestUpgradeQuery();
      } else if (dbType.equals(Configuration.ORACLE_DB_NAME)) {
        query = getOracleRequestUpgradeQuery();
      } else {
        query = getMysqlRequestUpgradeQuery();
      }

      dbAccessor.executeQuery(query);
    }

    // Sequences
    if (dbAccessor.tableExists("ambari_sequences")) {
      if (dbType.equals(Configuration.POSTGRES_DB_NAME)) {
        try {
          dbAccessor.executeQuery(getPostgresSequenceUpgradeQuery());
          // Deletes
          dbAccessor.dropSequence("host_role_command_task_id_seq");
          dbAccessor.dropSequence("users_user_id_seq");
          dbAccessor.dropSequence("clusters_cluster_id_seq");
        } catch (SQLException sql) {
          LOG.warn("Sequence update threw exception. ", sql);
        }
      }
    }

    //clear cache due to direct table manipulation
    ((JpaEntityManager)em.getDelegate()).getServerSession().getIdentityMapAccessor().invalidateAll();

    // Updates

    // HostComponentState
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<HostComponentStateEntity> c1 = cb.createQuery(HostComponentStateEntity.class);
    Root<HostComponentStateEntity> hsc = c1.from(HostComponentStateEntity.class);
    Expression<String> exp = hsc.get("current_state");
    List<String> statuses = new ArrayList<String>() {{
       add("STOP_FAILED");
       add("START_FAILED");
    }};
    Predicate predicate = exp.in(statuses);
    c1.select(hsc).where(predicate);

    TypedQuery<HostComponentStateEntity> q1 = em.createQuery(c1);
    List<HostComponentStateEntity> r1 = q1.getResultList();

    HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    if (r1 != null && !r1.isEmpty()) {
      for (HostComponentStateEntity entity : r1) {
        entity.setCurrentState(State.INSTALLED);
        hostComponentStateDAO.merge(entity);
      }
    }

    // HostRoleCommand
    CriteriaQuery<HostRoleCommandEntity> c2 = cb.createQuery(HostRoleCommandEntity.class);
    Root<HostRoleCommandEntity> hrc = c2.from(HostRoleCommandEntity.class);
    statuses = new ArrayList<String>() {{
      add("PENDING");
      add("QUEUED");
      add("IN_PROGRESS");
    }};
    exp = hrc.get("status");
    predicate = exp.in(statuses);
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
            clusterStateEntity.setCurrentStackVersion(clusterEntity.getDesiredStackVersion());

            clusterStateDAO.create(clusterStateEntity);

            clusterEntity.setClusterStateEntity(clusterStateEntity);

            clusterDAO.merge(clusterEntity);
          }
        }
      }
    });



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
    return "INSERT INTO ambari_sequences(sequence_name, \"value\") " +
      "SELECT 'cluster_id_seq', nextval('clusters_cluster_id_seq') " +
      "UNION ALL " +
      "SELECT 'user_id_seq', nextval('users_user_id_seq') " +
      "UNION ALL " +
      "SELECT 'host_role_command_id_seq', COALESCE((SELECT max(task_id) FROM host_role_command), 1) + 50 " +
      "UNION ALL " +
      "SELECT 'configgroup_id_seq', 1";
  }

  private String getPostgresRequestUpgradeQuery() {
    return "insert into request" +
      "(request_id, cluster_id, request_context, start_time, end_time, create_time) " +
      "(select distinct s.request_id, s.cluster_id, s.request_context, " +
      "coalesce (cmd.start_time, -1), coalesce (cmd.end_time, -1), -1 " +
      "from " +
      "(select distinct request_id, cluster_id, request_context from stage ) s " +
      "left join " +
      "(select request_id, min(start_time) as start_time, max(end_time) " +
      "as end_time from host_role_command group by request_id) cmd";
  }

  private String getOracleRequestUpgradeQuery() {
    return "INSERT INTO request" +
      "(request_id, cluster_id, request_context, start_time, end_time, create_time) " +
      "SELECT DISTINCT s.request_id, s.cluster_id, s.request_context, " +
      "nvl(cmd.start_time, -1), nvl(cmd.end_time, -1), -1" +
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
      "coalesce (cmd.start_time, -1), coalesce (cmd.end_time, -1), -1 " +
      "from " +
      "(select distinct request_id, cluster_id, request_context from stage ) s " +
      "left join " +
      "(select request_id, min(start_time) as start_time, max(end_time) " +
      "as end_time from host_role_command group by request_id) cmd " +
      "on s.request_id=cmd.request_id";
  }

  private void createQuartzTables() throws SQLException {
    String dbType = getDbType();

    // Run script to create quartz tables
    String scriptPath = configuration.getResourceDirPath() +
      File.separator + "upgrade" + File.separator + "ddl" +
      File.separator + String.format(quartzScriptFilePattern, dbType);

    try {
      dbAccessor.executeScript(scriptPath);
    } catch (IOException e) {
      LOG.error("Error reading file.", e);
    }

    // TODO: Verify if this is necessary and possible
    if (dbType.equals(Configuration.POSTGRES_DB_NAME)) {
      grantAllPostgresPrivileges();
    }
  }

  @Override
  public String getTargetVersion() {
    return "1.5.0";
  }
}
