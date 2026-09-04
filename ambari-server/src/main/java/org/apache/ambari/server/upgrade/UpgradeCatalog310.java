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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;

import com.google.inject.Inject;
import com.google.inject.Injector;

/** Adds the monitoring persistence model for Ambari 3.1. */
public class UpgradeCatalog310 extends AbstractUpgradeCatalog {
  static final String DATASOURCE_TABLE = "datasource";
  static final String BOARD_TABLE = "board";
  static final String BOARD_PAYLOAD_TABLE = "board_payload";
  static final String CHART_SHARE_TABLE = "chart_share";
  static final String OLD_DATASOURCE_NAME_CONSTRAINT = "UQ_datasource_name";
  static final String DATASOURCE_NAME_CLUSTER_CONSTRAINT = "UQ_datasource_name_cluster";
  static final String OLD_BOARD_GROUP_NAME_CONSTRAINT = "UQ_board_group_name";
  static final String BOARD_CLUSTER_GROUP_NAME_CONSTRAINT = "UQ_board_cluster_group_name";
  static final String BUILTIN_BOARD_CLUSTER = "__ambari_builtin__";

  static final String DATASOURCE_SEQUENCE = "datasource_id_seq";
  static final String BOARD_SEQUENCE = "board_id_seq";
  static final String CHART_SHARE_SEQUENCE = "chart_share_id_seq";

  @Inject
  public UpgradeCatalog310(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "3.0.0";
  }

  @Override
  public String getTargetVersion() {
    return "3.1.0";
  }

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    createDatasourceTable();
    createBoardTable();
    createBoardPayloadTable();
    createChartShareTable();
    addMonitoringSequences();
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
  }

  protected void createDatasourceTable() throws SQLException {
    if (dbAccessor.tableExists(DATASOURCE_TABLE)) {
      dbAccessor.dropUniqueConstraint(DATASOURCE_TABLE, OLD_DATASOURCE_NAME_CONSTRAINT, true);
      dbAccessor.dropUniqueConstraint(DATASOURCE_TABLE, DATASOURCE_NAME_CLUSTER_CONSTRAINT, true);
      dbAccessor.addUniqueConstraint(DATASOURCE_TABLE, DATASOURCE_NAME_CLUSTER_CONSTRAINT,
          "cluster_name", "name");
      return;
    }

    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(column("id", Long.class, null, null, false));
    columns.add(column("name", String.class, 191, "", false));
    columns.add(column("description", String.class, 255, "", false));
    columns.add(column("category", String.class, 255, "", false));
    columns.add(column("plugin_id", Long.class, null, 0L, false));
    columns.add(column("plugin_type", String.class, 255, "", false));
    columns.add(column("plugin_type_name", String.class, 255, "", false));
    columns.add(column("cluster_name", String.class, 255, "", false));
    columns.add(column("settings", Clob.class, null, null, false));
    columns.add(column("status", String.class, 255, "", false));
    columns.add(column("http", Clob.class, null, null, false));
    columns.add(column("auth", Clob.class, null, null, false));
    columns.add(column("is_default", Boolean.class, null, false, false));
    columns.add(column("created_at", Long.class, null, 0L, false));
    columns.add(column("created_by", String.class, 64, "", false));
    columns.add(column("updated_at", Long.class, null, 0L, false));
    columns.add(column("updated_by", String.class, 64, "", false));
    dbAccessor.createTable(DATASOURCE_TABLE, columns, "id");
    dbAccessor.addUniqueConstraint(DATASOURCE_TABLE, DATASOURCE_NAME_CLUSTER_CONSTRAINT,
        "cluster_name", "name");
    dbAccessor.createIndex("idx_datasource_cluster", DATASOURCE_TABLE, "cluster_name");
  }

  protected void createBoardTable() throws SQLException {
    if (dbAccessor.tableExists(BOARD_TABLE)) {
      if (!dbAccessor.tableHasColumn(BOARD_TABLE, "cluster_name")) {
        dbAccessor.addColumn(BOARD_TABLE,
            column("cluster_name", String.class, 255, BUILTIN_BOARD_CLUSTER, false));
      }
      dbAccessor.dropUniqueConstraint(BOARD_TABLE, OLD_BOARD_GROUP_NAME_CONSTRAINT, true);
      dbAccessor.dropUniqueConstraint(BOARD_TABLE, BOARD_CLUSTER_GROUP_NAME_CONSTRAINT, true);
      dbAccessor.addUniqueConstraint(BOARD_TABLE, BOARD_CLUSTER_GROUP_NAME_CONSTRAINT,
          "cluster_name", "group_id", "name");
      dbAccessor.createIndex("idx_board_cluster", BOARD_TABLE, "cluster_name");
      return;
    }

    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(column("id", Long.class, null, null, false));
    columns.add(column("cluster_name", String.class, 255, BUILTIN_BOARD_CLUSTER, false));
    columns.add(column("group_id", Long.class, null, 0L, false));
    columns.add(column("name", String.class, 191, null, false));
    columns.add(column("ident", String.class, 200, "", false));
    columns.add(column("tags", String.class, 255, "", false));
    columns.add(column("public", Integer.class, null, 0, false));
    columns.add(column("built_in", Integer.class, null, 0, false));
    columns.add(column("hide", Integer.class, null, 0, false));
    columns.add(column("create_at", Long.class, null, 0L, false));
    columns.add(column("create_by", String.class, 64, "", false));
    columns.add(column("update_at", Long.class, null, 0L, false));
    columns.add(column("update_by", String.class, 64, "", false));
    columns.add(column("public_cate", Long.class, null, 0L, false));
    columns.add(column("display_locations", String.class, 255, "", false));
    dbAccessor.createTable(BOARD_TABLE, columns, "id");
    dbAccessor.addUniqueConstraint(BOARD_TABLE, BOARD_CLUSTER_GROUP_NAME_CONSTRAINT,
        "cluster_name", "group_id", "name");
    dbAccessor.createIndex("idx_board_ident", BOARD_TABLE, "ident");
    dbAccessor.createIndex("idx_board_cluster", BOARD_TABLE, "cluster_name");
  }

  protected void createBoardPayloadTable() throws SQLException {
    if (dbAccessor.tableExists(BOARD_PAYLOAD_TABLE)) {
      return;
    }

    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(column("id", Long.class, null, null, false));
    columns.add(column("payload", Clob.class, null, null, false));
    dbAccessor.createTable(BOARD_PAYLOAD_TABLE, columns, "id");
    dbAccessor.addFKConstraint(BOARD_PAYLOAD_TABLE, "FK_board_payload_board", "id",
        BOARD_TABLE, "id", true, false);
  }

  protected void createChartShareTable() throws SQLException {
    if (dbAccessor.tableExists(CHART_SHARE_TABLE)) {
      return;
    }

    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(column("id", Long.class, null, null, false));
    columns.add(column("cluster", String.class, 128, "", false));
    columns.add(column("datasource_id", Long.class, null, 0L, false));
    columns.add(column("configs", Clob.class, null, null, true));
    columns.add(column("create_at", Long.class, null, 0L, false));
    columns.add(column("create_by", String.class, 64, "", false));
    dbAccessor.createTable(CHART_SHARE_TABLE, columns, "id");
    dbAccessor.createIndex("idx_chart_share_create_at", CHART_SHARE_TABLE, "create_at");
    dbAccessor.createIndex("idx_chart_share_cluster", CHART_SHARE_TABLE, "cluster");
  }

  protected void addMonitoringSequences() throws SQLException {
    addSequence(DATASOURCE_SEQUENCE, fetchMaxId(DATASOURCE_TABLE, "id") + 1, false);
    addSequence(BOARD_SEQUENCE, fetchMaxId(BOARD_TABLE, "id") + 1, false);
    addSequence(CHART_SHARE_SEQUENCE, fetchMaxId(CHART_SHARE_TABLE, "id") + 1, false);
  }

  private DBAccessor.DBColumnInfo column(String name, Class<?> type, Integer length,
      Object defaultValue, boolean nullable) {
    return new DBAccessor.DBColumnInfo(name, type, length, defaultValue, nullable);
  }
}
