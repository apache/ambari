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
import java.util.List;
import java.util.Locale;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Creates the additive persistence boundary for managed service dependencies. */
@Singleton
public class ServiceDependencySchemaUpgrade {
  static final String BINDING = "service_dependency_binding";
  static final String SNAPSHOT = "service_dependency_snapshot";
  static final String OPERATION = "service_dependency_operation";
  static final String HOST_RESULT = "service_dependency_host_result";
  static final String FENCE = "service_dependency_fence";

  private final DBAccessor dbAccessor;

  @Inject
  public ServiceDependencySchemaUpgrade(DBAccessor dbAccessor) {
    this.dbAccessor = dbAccessor;
  }

  public void execute() throws AmbariException, SQLException {
    ensureBinding();
    ensureSnapshot();
    ensureOperation();
    ensureHostResult();
    ensureFence();
  }

  private void ensureBinding() throws AmbariException, SQLException {
    List<DBColumnInfo> columns = List.of(
        string("binding_id", 36, false),
        number("consumer_cluster_id", false),
        string("consumer_service_name", 255, false),
        number("provider_cluster_id", false),
        string("provider_service_name", 255, false),
        string("dependency_type", 32, false),
        string("state", 32, false),
        string("provisioning_phase", 64, true),
        number("row_version", false),
        number("operation_epoch", false),
        number("desired_snapshot_version", false),
        string("snapshot_approval", 32, false),
        string("provider_preparation_hash", 71, true),
        number("applied_snapshot_version", true),
        string("provider_fingerprint", 71, false),
        string("applied_provider_fingerprint", 71, true),
        string("namespace_root", 2048, true),
        string("namespace_wal", 2048, true),
        string("namespace_znode", 1024, true),
        number("action_host_id", true),
        string("active_operation_id", 36, false),
        number("active_request_id", true),
        string("failure_code", 128, true),
        string("failure_phase", 64, true),
        string("failure_message", 1024, true),
        bool("failure_retryable", false),
        integer("created_by_user_id", false),
        integer("updated_by_user_id", false),
        number("create_timestamp", false),
        number("update_timestamp", false));
    createOrVerify(BINDING, columns, "binding_id");
    if (!dbAccessor.tableHasIndex(BINDING, true, "uq_svc_dep_consumer_type")) {
      dbAccessor.addUniqueConstraint(BINDING, "uq_svc_dep_consumer_type",
          "consumer_cluster_id", "consumer_service_name", "dependency_type");
    }
    ensureCompositeForeignKey(BINDING, "fk_svc_dep_consumer",
        new String[] {"consumer_service_name", "consumer_cluster_id"}, "clusterservices",
        new String[] {"service_name", "cluster_id"});
    ensureCompositeForeignKey(BINDING, "fk_svc_dep_provider",
        new String[] {"provider_service_name", "provider_cluster_id"}, "clusterservices",
        new String[] {"service_name", "cluster_id"});
    ensureForeignKey(BINDING, "fk_svc_dep_action_host", "action_host_id", "hosts", "host_id");
  }

  private void ensureSnapshot() throws AmbariException, SQLException {
    List<DBColumnInfo> columns = List.of(
        string("binding_id", 36, false),
        number("snapshot_version", false),
        integer("schema_version", false),
        string("consumer_fingerprint", 71, false),
        string("provider_fingerprint", 71, false),
        string("provider_display_name", 255, false),
        string("consumer_service_version", 128, false),
        string("snapshot_fingerprint", 71, false),
        string("client_features_hash", 71, false),
        string("security_policy_hash", 71, false),
        lob("snapshot_json", false),
        integer("created_by_user_id", false),
        number("create_timestamp", false));
    createOrVerify(SNAPSHOT, columns, "binding_id", "snapshot_version");
    ensureForeignKey(SNAPSHOT, "fk_svc_dep_snapshot_binding", "binding_id", BINDING, "binding_id");
  }

  private void ensureOperation() throws AmbariException, SQLException {
    List<DBColumnInfo> columns = List.of(
        string("operation_id", 36, false),
        string("binding_id", 36, false),
        string("operation_kind", 32, false),
        number("operation_epoch", false),
        number("target_snapshot_version", false),
        string("request_hash", 71, false),
        string("state", 32, false),
        number("ambari_request_id", true),
        string("failure_code", 128, true),
        string("failure_message", 1024, true),
        number("create_timestamp", false),
        number("update_timestamp", false));
    createOrVerify(OPERATION, columns, "operation_id");
    ensureForeignKey(OPERATION, "fk_svc_dep_operation_binding", "binding_id", BINDING, "binding_id");
  }

  private void ensureHostResult() throws AmbariException, SQLException {
    List<DBColumnInfo> columns = List.of(
        string("binding_id", 36, false),
        number("snapshot_version", false),
        number("host_id", false),
        string("dependency_type", 32, false),
        string("check_kind", 64, false),
        number("operation_epoch", false),
        string("operation_id", 36, false),
        string("component_name", 255, true),
        string("command_request_hash", 71, false),
        lob("command_json", false),
        number("ambari_request_id", true),
        number("ambari_stage_id", true),
        number("ambari_task_id", true),
        string("required_package_hash", 71, false),
        string("observed_package_hash", 71, true),
        string("rendered_config_hash", 71, true),
        string("identity_fingerprint", 71, true),
        lob("result_json", true),
        string("result_hash", 71, true),
        string("preparation_observation_id", 36, true),
        string("preparation_request_hash", 71, true),
        string("preparation_observation_fingerprint", 71, true),
        string("package_name", 128, true),
        string("package_version", 512, true),
        string("client_software_version", 128, true),
        string("state", 32, false),
        number("check_timestamp", false),
        string("failure_code", 128, true),
        string("failure_message", 1024, true));
    createOrVerify(HOST_RESULT, columns, "binding_id", "snapshot_version", "operation_epoch",
        "host_id", "dependency_type", "check_kind");
    if (!dbAccessor.tableHasPrimaryKey(HOST_RESULT, "operation_epoch")) {
      dbAccessor.dropPKConstraint(HOST_RESULT, "pk_" + HOST_RESULT);
      dbAccessor.addPKConstraint(HOST_RESULT, "pk_" + HOST_RESULT,
          "binding_id", "snapshot_version", "operation_epoch", "host_id",
          "dependency_type", "check_kind");
    }
    ensureCompositeForeignKey(HOST_RESULT, "fk_svc_dep_host_snapshot",
        new String[] {"binding_id", "snapshot_version"}, SNAPSHOT,
        new String[] {"binding_id", "snapshot_version"});
    ensureForeignKey(HOST_RESULT, "fk_svc_dep_host_operation", "operation_id",
        OPERATION, "operation_id");
    ensureForeignKey(HOST_RESULT, "fk_svc_dep_host", "host_id", "hosts", "host_id");
  }

  private void ensureFence() throws AmbariException, SQLException {
    List<DBColumnInfo> columns = List.of(
        string("binding_id", 36, false),
        number("final_epoch", false),
        string("immutable_spec_hash", 71, false),
        string("dependency_type", 32, false),
        number("consumer_cluster_id", false),
        string("consumer_service_name", 255, false),
        number("provider_cluster_id", false),
        string("provider_service_name", 255, false),
        string("namespace_hash", 71, false),
        string("detach_operation_id", 36, true),
        string("detach_request_hash", 71, true),
        integer("detached_by_user_id", false),
        number("detach_timestamp", false));
    createOrVerify(FENCE, columns, "binding_id");
  }

  private void createOrVerify(String table, List<DBColumnInfo> columns, String... primaryKey)
      throws AmbariException, SQLException {
    if (dbAccessor.tableExists(table)) {
      for (DBColumnInfo column : columns) {
        if (!dbAccessor.tableHasColumn(table, column.getName()) && column.isNullable()) {
          dbAccessor.addColumn(table, column);
        } else if (!dbAccessor.tableHasColumn(table, column.getName())) {
          throw new AmbariException(String.format(
              "Existing %s table is missing required column %s; repair the partial schema and rerun ambari-server upgrade",
              table, column.getName()));
        }
      }
      return;
    }
    dbAccessor.createTable(table, columns);
    dbAccessor.addPKConstraint(table, "pk_" + table, primaryKey);
  }

  private void ensureForeignKey(String table, String name, String column,
      String referenceTable, String referenceColumn) throws SQLException {
    if (!dbAccessor.tableHasForeignKey(table, name)) {
      dbAccessor.addFKConstraint(table, name, column, referenceTable, referenceColumn, false);
    }
  }

  private void ensureCompositeForeignKey(String table, String name, String[] columns,
      String referenceTable, String[] referenceColumns) throws SQLException {
    if (!dbAccessor.tableHasForeignKey(table, name)) {
      dbAccessor.addFKConstraint(table, name, columns, referenceTable, referenceColumns, false, false);
    }
  }

  private DBColumnInfo string(String name, int length, boolean nullable) {
    return new DBColumnInfo(name, String.class, length, null, nullable);
  }

  private DBColumnInfo number(String name, boolean nullable) {
    return new DBColumnInfo(name, Long.class, null, null, nullable);
  }

  private DBColumnInfo integer(String name, boolean nullable) {
    return new DBColumnInfo(name, Integer.class, null, null, nullable);
  }

  private DBColumnInfo bool(String name, boolean nullable) {
    return new DBColumnInfo(name, Short.class, null, null, nullable);
  }

  private DBColumnInfo lob(String name, boolean nullable) throws SQLException, AmbariException {
    FieldTypeDefinition type = new FieldTypeDefinition(resolveLobType());
    type.setSizeDisallowed();
    return new DBColumnInfo(name, type, null, null, nullable);
  }

  private String resolveLobType() throws SQLException, AmbariException {
    switch (dbAccessor.getDbType()) {
      case MYSQL:
        return "LONGTEXT";
      case POSTGRES:
        return "TEXT";
      case ORACLE:
      case DERBY:
      case H2:
        return "CLOB";
      default:
        String product = dbAccessor.getConnection().getMetaData().getDatabaseProductName()
            .toLowerCase(Locale.ROOT);
        if (product.contains("microsoft") || product.contains("sql server")) {
          return "VARCHAR(MAX)";
        }
        if (product.contains("sql anywhere") || product.contains("adaptive server anywhere")) {
          return "TEXT";
        }
        throw new AmbariException("Cannot select managed dependency LOB type for database " + product);
    }
  }
}
