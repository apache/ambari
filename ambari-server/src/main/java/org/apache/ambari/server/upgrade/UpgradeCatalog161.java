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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 1.6.1.
 */
public class UpgradeCatalog161 extends AbstractUpgradeCatalog {

  //SourceVersion is only for book-keeping purpos
  @Override
  public String getSourceVersion() {
    return "1.6.0";
  }


  @Override
  public String getTargetVersion() {
    return "1.6.1";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger
      (UpgradeCatalog161.class);

  // ----- Constructors ------------------------------------------------------

  @Inject
  public UpgradeCatalog161(Injector injector) {
    super(injector);
  }


  // ----- AbstractUpgradeCatalog --------------------------------------------

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    DatabaseType databaseType = configuration.getDatabaseType();

    List<DBColumnInfo> columns;

    // Operation level
    columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("operation_level_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("request_id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("level_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("cluster_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("service_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("host_component_name", String.class, 255, null, true));
    columns.add(new DBColumnInfo("host_name", String.class, 255, null, true));

    dbAccessor.createTable("requestoperationlevel", columns, "operation_level_id");

    // 1.6.0 initially shipped with restart_required as a BOOELAN so some
    // upgrades might be BOOLEAN but most are probably SMALLINT
    if (databaseType == DatabaseType.POSTGRES) {
      int columnType = dbAccessor.getColumnType("hostcomponentdesiredstate",
          "restart_required");

      if (columnType == Types.BOOLEAN || columnType == Types.BIT) {
        dbAccessor.executeQuery(
            "ALTER TABLE hostcomponentdesiredstate ALTER column restart_required TYPE SMALLINT USING CASE WHEN restart_required=true THEN 1 ELSE 0 END",
            true);
      }
    }

    if (databaseType == DatabaseType.ORACLE) {
      dbAccessor.executeQuery(
          "ALTER TABLE hostcomponentdesiredstate MODIFY (restart_required DEFAULT 0)",
          true);

    } else {
      dbAccessor.executeQuery(
          "ALTER TABLE hostcomponentdesiredstate ALTER column restart_required SET DEFAULT 0",
          true);
    }

    //=========================================================================
    // Add columns
    dbAccessor.addColumn("viewmain",
        new DBAccessor.DBColumnInfo("icon", String.class, 255, null, true));

    dbAccessor.addColumn("viewmain",
        new DBAccessor.DBColumnInfo("icon64", String.class, 255, null, true));

    dbAccessor.addColumn("viewinstancedata",
        new DBAccessor.DBColumnInfo("user_name", String.class, 255, " ", false));

    dbAccessor.dropFKConstraint("viewinstancedata", "FK_viewinstdata_view_name");
    dbAccessor.dropFKConstraint("viewinstanceproperty", "FK_viewinstprop_view_name");
    dbAccessor.dropFKConstraint("viewentity", "FK_viewentity_view_name");
    dbAccessor.dropFKConstraint("viewinstance", "FK_viewinst_view_name");

    //modify primary key of viewinstancedata
    if (databaseType == DatabaseType.ORACLE
        || databaseType == DatabaseType.MYSQL
        || databaseType == DatabaseType.DERBY) {
      dbAccessor.executeQuery("ALTER TABLE viewinstance DROP PRIMARY KEY", true);
      dbAccessor.executeQuery("ALTER TABLE viewinstancedata DROP PRIMARY KEY", true);
    } else if (databaseType == DatabaseType.POSTGRES) {
      dbAccessor.executeQuery("ALTER TABLE viewinstance DROP CONSTRAINT viewinstance_pkey CASCADE", true);
      dbAccessor.executeQuery("ALTER TABLE viewinstancedata DROP CONSTRAINT viewinstancedata_pkey CASCADE", true);
    }


    dbAccessor.addColumn("viewinstance", new DBAccessor.DBColumnInfo("view_instance_id", Long.class, null, null, true));
    dbAccessor.addColumn("viewinstancedata",
      new DBAccessor.DBColumnInfo("view_instance_id", Long.class, null, null, true));

    if (databaseType == DatabaseType.ORACLE) {
      //sequence looks to be simpler than rownum
      if (dbAccessor.tableHasData("viewinstancedata")) {
        dbAccessor.executeQuery("CREATE SEQUENCE TEMP_SEQ " +
            "  START WITH 1 " +
            "  MAXVALUE 999999999999999999999999999 " +
            "  MINVALUE 1 " +
            "  NOCYCLE " +
            "  NOCACHE " +
            "  NOORDER");
        dbAccessor.executeQuery("UPDATE viewinstance SET view_instance_id = TEMP_SEQ.NEXTVAL");
        dbAccessor.dropSequence("TEMP_SEQ");
      }
    } else if (databaseType == DatabaseType.MYSQL) {
      if (dbAccessor.tableHasData("viewinstance")) {
        dbAccessor.executeQuery("UPDATE viewinstance " +
            "SET view_instance_id = (SELECT @a := @a + 1 FROM (SELECT @a := 1) s)");
      }
    } else if (databaseType == DatabaseType.POSTGRES) {
      if (dbAccessor.tableHasData("viewinstance")) {
        //window functions like row_number were added in 8.4, workaround for earlier versions (redhat/centos 5)
        dbAccessor.executeQuery("CREATE SEQUENCE temp_seq START WITH 1");
        dbAccessor.executeQuery("UPDATE viewinstance SET view_instance_id = nextval('temp_seq')");
        dbAccessor.dropSequence("temp_seq");
      }


    }

    if (databaseType == DatabaseType.DERBY) {
      dbAccessor.executeQuery("ALTER TABLE viewinstance ALTER COLUMN view_instance_id DEFAULT 0");
      dbAccessor.executeQuery("ALTER TABLE viewinstance ALTER COLUMN view_instance_id NOT NULL");
      dbAccessor.executeQuery("ALTER TABLE viewinstancedata ALTER COLUMN view_instance_id DEFAULT 0");
      dbAccessor.executeQuery("ALTER TABLE viewinstancedata ALTER COLUMN view_instance_id NOT NULL");
      dbAccessor.executeQuery("ALTER TABLE viewinstancedata ALTER COLUMN user_name DEFAULT ' '");
      dbAccessor.executeQuery("ALTER TABLE viewinstancedata ALTER COLUMN user_name NOT NULL");
    }

    dbAccessor.executeQuery("alter table viewinstance add primary key (view_instance_id)");
    dbAccessor.executeQuery("ALTER TABLE viewinstance ADD CONSTRAINT UQ_viewinstance_name UNIQUE (view_name, name)", true);
    dbAccessor.executeQuery("ALTER TABLE viewinstance ADD CONSTRAINT UQ_viewinstance_name_id UNIQUE (view_instance_id, view_name, name)", true);

    dbAccessor.addFKConstraint("viewinstanceproperty", "FK_viewinstprop_view_name",
        new String[]{"view_name", "view_instance_name"}, "viewinstance", new String[]{"view_name", "name"}, true);
    dbAccessor.addFKConstraint("viewentity", "FK_viewentity_view_name",
        new String[]{"view_name", "view_instance_name"}, "viewinstance", new String[]{"view_name", "name"}, true);
    dbAccessor.addFKConstraint("viewinstance", "FK_viewinst_view_name", "view_name", "viewmain", "view_name", true);

    if (databaseType == DatabaseType.POSTGRES) {
      dbAccessor.executeQuery("UPDATE viewinstancedata " +
        "SET view_instance_id = vi.view_instance_id FROM viewinstance AS vi " +
        "WHERE vi.name = viewinstancedata.view_instance_name AND vi.view_name = viewinstancedata.view_name");
    } else if (databaseType == DatabaseType.ORACLE) {
      dbAccessor.executeQuery("UPDATE viewinstancedata vid SET view_instance_id = (" +
        "SELECT view_instance_id FROM viewinstance vi WHERE vi.name = vid.view_instance_name AND vi.view_name = vid.view_name)");
    } else if (databaseType == DatabaseType.MYSQL) {
      dbAccessor.executeQuery("UPDATE viewinstancedata AS vid JOIN viewinstance AS vi " +
        "ON vi.name = vid.view_instance_name AND vi.view_name = vid.view_name " +
        "SET vid.view_instance_id = vi.view_instance_id");
    }

    dbAccessor.executeQuery("alter table viewinstancedata add primary key (view_instance_id, name, user_name)");

    dbAccessor.addFKConstraint("viewinstancedata", "FK_viewinstdata_view_name", new String[]{"view_instance_id", "view_name", "view_instance_name"},
      "viewinstance", new String[]{"view_instance_id", "view_name", "name"}, true);


    long count = 1;
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        rs = statement.executeQuery("SELECT count(*) FROM viewinstance");
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

    String valueColumnName = "\"value\"";
    if (databaseType == DatabaseType.ORACLE
        || databaseType == DatabaseType.MYSQL) {
      valueColumnName = "value";
    }

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, " + valueColumnName + ") " +
        "VALUES('view_instance_id_seq', " + count + ")", true);

    dbAccessor.addColumn("viewinstance",
        new DBAccessor.DBColumnInfo("label", String.class, 255, null, true));

    dbAccessor.addColumn("viewinstance",
        new DBAccessor.DBColumnInfo("description", String.class, 255, null, true));

    dbAccessor.addColumn("viewinstance",
        new DBAccessor.DBColumnInfo("visible", Character.class, 1, null, true));

    dbAccessor.addColumn("viewinstance",
        new DBAccessor.DBColumnInfo("icon", String.class, 255, null, true));

    dbAccessor.addColumn("viewinstance",
        new DBAccessor.DBColumnInfo("icon64", String.class, 255, null, true));

    // ========================================================================
    // Add constraints
    dbAccessor.addFKConstraint("requestoperationlevel", "FK_req_op_level_req_id",
            "request_id", "request", "request_id", true);

    // Clusters
    dbAccessor.addColumn("clusters", new DBColumnInfo("provisioning_state", String.class, 255, State.INIT.name(), false));

    dbAccessor.dropFKConstraint("stage", "FK_stage_cluster_id", true);
    dbAccessor.dropFKConstraint("request", "FK_request_cluster_id", true);
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
    //add new sequences for operation level
    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, sequence_value) " +
            "VALUES('operation_level_id_seq', 1)", true);

    // upgrade cluster provision state
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        // it should be safe to bulk update the current cluster state since
        // this field is not currently used and since all clusters stored in
        // the database must (at this point) be installed
        final EntityManager em = getEntityManagerProvider().get();
        final TypedQuery<ClusterEntity> query = em.createQuery(
            "UPDATE ClusterEntity SET provisioningState = :provisioningState",
            ClusterEntity.class);

        query.setParameter("provisioningState", State.INSTALLED);
        final int updatedClusterProvisionedStateCount = query.executeUpdate();

        LOG.info("Updated {} cluster provisioning states to {}",
            updatedClusterProvisionedStateCount, State.INSTALLED);
      }
    });

    addMissingConfigs();
  }

  protected void addMissingConfigs() throws AmbariException {
    updateConfigurationProperties("hbase-site", Collections.singletonMap("hbase.regionserver.info.port", "60030"), false, false);
    updateConfigurationProperties("hbase-site", Collections.singletonMap("hbase.master.info.port", "60010"), false, false);
    updateConfigurationProperties("hive-site", Collections.singletonMap("hive.heapsize", "1024"), false, false);
    updateConfigurationProperties("pig-properties", Collections.singletonMap("pig-content", "\n# Licensed to the Apache " +
            "Software Foundation (ASF) under one\n# or more contributor license agreements.  See the NOTICE file\n# " +
            "distributed with this work for additional information\n# regarding copyright ownership.  The ASF " +
            "licenses this file\n# to you under the Apache License, Version 2.0 (the\n# \"License\"); you may " +
            "not use this file except in compliance\n# with the License.  You may obtain a copy of the License " +
            "at\n#\n#http://www.apache.org/licenses/LICENSE-2.0\n#\n# Unless required by applicable law or agreed to " +
            "in writing,\n# software distributed under the License is distributed on an\n# \"AS IS\" BASIS, WITHOUT " +
            "WARRANTIES OR CONDITIONS OF ANY\n# KIND, either express or implied.  See the License for the\n# " +
            "specific language governing permissions and limitations\n# under the License.\n\n# Pig default " +
            "configuration file. All values can be overwritten by pig.properties and command line arguments.\n# " +
            "see bin/pig -help\n\n# brief logging (no timestamps)\nbrief=false\n\n# debug level, INFO is default" +
            "\ndebug=INFO\n\n# verbose print all log messages to screen (default to print only INFO and above to " +
            "screen)\nverbose=false\n\n# exectype local|mapreduce, mapreduce is default\nexectype=mapreduce\n\n# " +
            "Enable insertion of information about script into hadoop job conf \npig.script.info.enabled=true\n\n# " +
            "Do not spill temp files smaller than this size (bytes)\npig.spill.size.threshold=5000000\n\n# " +
            "EXPERIMENT: Activate garbage collection when spilling a file bigger than this size (bytes)\n# " +
            "This should help reduce the number of files being spilled.\npig.spill.gc.activation.size=40000000\n\n# " +
            "the following two parameters are to help estimate the reducer number\npig.exec.reducers.bytes.per." +
            "reducer=1000000000\npig.exec.reducers.max=999\n\n# Temporary location to store the intermediate " +
            "data.\npig.temp.dir=/tmp/\n\n# Threshold for merging FRJoin fragment files\npig.files.concatenation." +
            "threshold=100\npig.optimistic.files.concatenation=false;\n\npig.disable.counter=false\n\n" +
            "hcat.bin=/usr/bin/hcat"), true, false);
  }
}
