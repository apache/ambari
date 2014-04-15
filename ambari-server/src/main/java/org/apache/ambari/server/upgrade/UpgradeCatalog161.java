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

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;

/**
 * Upgrade catalog for version 1.6.1.
 */
public class UpgradeCatalog161 extends AbstractUpgradeCatalog {

  // ----- Constructors ------------------------------------------------------

  @Inject
  public UpgradeCatalog161(Injector injector) {
    super(injector);
  }


  // ----- AbstractUpgradeCatalog --------------------------------------------

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
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

    // ========================================================================
    // Add constraints
    dbAccessor.addFKConstraint("requestoperationlevel", "FK_req_op_level_req_id",
            "request_id", "request", "request_id", true);
  }


  // ----- UpgradeCatalog ----------------------------------------------------

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    String dbType = getDbType();

    String valueColumnName = "\"value\"";
    if (Configuration.ORACLE_DB_NAME.equals(dbType) || Configuration.MYSQL_DB_NAME.equals(dbType)) {
      valueColumnName = "value";
    }
    //add new sequences for operation level
    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, " + valueColumnName + ") " +
            "VALUES('operation_level_id_seq', 1)", true);
  }

  @Override
  public String getTargetVersion() {
    return "1.6.1";
  }
}
