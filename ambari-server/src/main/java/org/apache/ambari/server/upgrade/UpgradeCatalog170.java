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
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 1.7.0.
 */
public class UpgradeCatalog170 extends AbstractUpgradeCatalog {

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
  }


  // ----- AbstractUpgradeCatalog --------------------------------------------

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    // !!! TODO: alerting DDL upgrade

    List<DBAccessor.DBColumnInfo> columns;

    // add admin tables and initial values prior to adding referencing columns on existing tables
    columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("principal_type_id", Integer.class, 1, null, false));
    columns.add(new DBAccessor.DBColumnInfo("principal_type_name", String.class, null, null, false));

    dbAccessor.createTable("adminprincipaltype", columns, "principal_type_id");

    dbAccessor.executeQuery("insert into adminprincipaltype (principal_type_id, principal_type_name)\n" +
        "  select 1, 'USER'\n" +
        "  union all\n" +
        "  select 2, 'GROUP'", true);

    columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("principal_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("principal_type_id", Integer.class, 1, null, false));

    dbAccessor.createTable("adminprincipal", columns, "principal_id");

    dbAccessor.executeQuery("insert into adminprincipal (principal_id, principal_type_id)\n" +
        "  select 1, 1", true);

    columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("resource_type_id", Integer.class, 1, null, false));
    columns.add(new DBAccessor.DBColumnInfo("resource_type_name", String.class, null, null, false));

    dbAccessor.createTable("adminresourcetype", columns, "resource_type_id");

    dbAccessor.executeQuery("insert into adminresourcetype (resource_type_id, resource_type_name)\n" +
        "  select 1, 'AMBARI'\n" +
        "  union all\n" +
        "  select 2, 'CLUSTER'\n" +
        "  union all\n" +
        "  select 3, 'VIEW'", true);

    columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("resource_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("resource_type_id", Integer.class, 1, null, false));

    dbAccessor.createTable("adminresource", columns, "resource_id");

    dbAccessor.executeQuery("insert into adminresource (resource_id, resource_type_id)\n" +
        "  select 1, 1", true);

    columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("permission_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("permission_name", String.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("resource_type_id", Integer.class, 1, null, false));

    dbAccessor.createTable("adminpermission", columns, "permission_id");

    dbAccessor.executeQuery("insert into adminpermission(permission_id, permission_name, resource_type_id)\n" +
        "  select 1, 'AMBARI.ADMIN', 1\n" +
        "  union all\n" +
        "  select 2, 'CLUSTER.READ', 2\n" +
        "  union all\n" +
        "  select 3, 'CLUSTER.OPERATE', 2\n" +
        "  union all\n" +
        "  select 4, 'VIEW.USE', 3", true);

    columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("privilege_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("permission_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("resource_id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("principal_id", Long.class, null, null, false));

    dbAccessor.createTable("adminprivilege", columns, "privilege_id");

    dbAccessor.executeQuery("insert into adminprivilege (privilege_id, permission_id, resource_id, principal_id)\n" +
        "  select 1, 1, 1, 1", true);


    DBAccessor.DBColumnInfo clusterConfigAttributesColumn = new DBAccessor.DBColumnInfo(
        "config_attributes", String.class, 32000, null, true);
    dbAccessor.addColumn("clusterconfig", clusterConfigAttributesColumn);

    // Add columns
    dbAccessor.addColumn("viewmain", new DBAccessor.DBColumnInfo("mask",
      String.class, 255, null, true));
    dbAccessor.addColumn("viewparameter", new DBAccessor.DBColumnInfo("masked",
      Character.class, 1, null, true));
    dbAccessor.addColumn("users", new DBAccessor.DBColumnInfo("active",
      Integer.class, 1, 1, false));
    dbAccessor.addColumn("users", new DBAccessor.DBColumnInfo("principal_id",
        Long.class, 1, 1, false));
    dbAccessor.addColumn("viewmain", new DBAccessor.DBColumnInfo("resource_type_id",
        Integer.class, 1, 1, false));
    dbAccessor.addColumn("viewinstance", new DBAccessor.DBColumnInfo("resource_id",
        Long.class, 1, 1, false));
  }


  // ----- UpgradeCatalog ----------------------------------------------------

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    // !!! TODO: create admin principals for existing users and groups.
    // !!! TODO: create admin resources for existing clusters and view instances
    // !!! TODO: alerting DML updates (sequences)

    String dbType = getDbType();

    // add new sequences for view entity
    String valueColumnName = "\"value\"";
    if (Configuration.ORACLE_DB_NAME.equals(dbType)
        || Configuration.MYSQL_DB_NAME.equals(dbType)) {
      valueColumnName = "value";
    }

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
        + valueColumnName + ") " + "VALUES('alert_definition_id_seq', 0)", true);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
        + valueColumnName + ") " + "VALUES('alert_group_id_seq', 0)", true);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
        + valueColumnName + ") " + "VALUES('alert_target_id_seq', 0)", true);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
        + valueColumnName + ") " + "VALUES('alert_history_id_seq', 0)", true);

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, "
        + valueColumnName + ") " + "VALUES('alert_notice_id_seq', 0)", true);
  }
}
