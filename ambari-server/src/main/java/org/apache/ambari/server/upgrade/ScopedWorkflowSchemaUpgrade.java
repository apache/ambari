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

/** Creates the portable LOB-backed store used only by scoped workflows. */
@Singleton
public class ScopedWorkflowSchemaUpgrade {
  static final String TABLE_NAME = "scoped_workflow_state";
  static final List<String> REQUIRED_COLUMNS = List.of(
      "scope_key", "revision", "owner_user_id", "owner_name", "workflow", "phase", "payload");
  static final String CREATED_CLUSTER_ID_COLUMN = "created_cluster_id";

  private final DBAccessor dbAccessor;

  @Inject
  public ScopedWorkflowSchemaUpgrade(DBAccessor dbAccessor) {
    this.dbAccessor = dbAccessor;
  }

  public void execute() throws AmbariException, SQLException {
    if (dbAccessor.tableExists(TABLE_NAME)) {
      for (String column : REQUIRED_COLUMNS) {
        if (!dbAccessor.tableHasColumn(TABLE_NAME, column)) {
          throw new AmbariException(String.format(
              "Existing %s table is missing required column %s; repair the partial schema and rerun ambari-server upgrade",
              TABLE_NAME, column));
        }
      }
      if (!dbAccessor.tableHasColumn(TABLE_NAME, CREATED_CLUSTER_ID_COLUMN)) {
        dbAccessor.addColumn(TABLE_NAME,
            new DBColumnInfo(CREATED_CLUSTER_ID_COLUMN, Long.class, null, null, true));
      }
      return;
    }

    FieldTypeDefinition payloadType = new FieldTypeDefinition(resolveLobType());
    payloadType.setSizeDisallowed();
    dbAccessor.createTable(TABLE_NAME, List.of(
        new DBColumnInfo("scope_key", String.class, 255, null, false),
        new DBColumnInfo("revision", Long.class, null, null, false),
        new DBColumnInfo("owner_user_id", Integer.class, null, null, true),
        new DBColumnInfo("owner_name", String.class, 255, null, true),
        new DBColumnInfo(CREATED_CLUSTER_ID_COLUMN, Long.class, null, null, true),
        new DBColumnInfo("workflow", String.class, 64, null, false),
        new DBColumnInfo("phase", String.class, 128, null, false),
        new DBColumnInfo("payload", payloadType, null, null, false)), "scope_key");
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
        throw new AmbariException("Cannot select a scoped workflow LOB type for database " + product);
    }
  }
}
