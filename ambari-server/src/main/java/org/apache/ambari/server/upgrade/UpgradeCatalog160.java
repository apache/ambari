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
import org.apache.ambari.server.orm.DBAccessor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Upgrade catalog for version 1.6.0.
 */
public class UpgradeCatalog160 extends AbstractUpgradeCatalog {

  // ----- Constructors ------------------------------------------------------

  @Inject
  public UpgradeCatalog160(Injector injector) {
    super(injector);
  }


  // ----- AbstractUpgradeCatalog --------------------------------------------

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<DBAccessor.DBColumnInfo>();

    // BP host group configuration
    columns.clear();
    columns.add(new DBAccessor.DBColumnInfo("blueprint_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("hostgroup_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("type_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("config_data", byte[].class, null, null, false));

    dbAccessor.createTable("hostgroup_configuration", columns, "blueprint_name",
        "hostgroup_name", "type_name");

    // View entity
    columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo("view_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("view_instance_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("class_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("id_property", String.class, 255, null, true));

    dbAccessor.createTable("viewentity", columns, "id");

    //=========================================================================
    // Add columns
    //TODO type converters are not supported by DBAccessor currently, default value will be provided to query as is in most cases
    DBAccessor.DBColumnInfo restartRequiredColumn =
      new DBAccessor.DBColumnInfo("restart_required", Boolean.class, 1, 0, false);
    if (Configuration.POSTGRES_DB_NAME.equals(getDbType())) {
      //only postgres supports boolean type
      restartRequiredColumn.setDefaultValue(Boolean.FALSE);
    }
    dbAccessor.addColumn("hostcomponentdesiredstate",
      restartRequiredColumn);

    // ========================================================================
    // Add constraints
    dbAccessor.addFKConstraint("hostgroup_configuration", "FK_hg_config_blueprint_name",
        "blueprint_name", "hostgroup", "blueprint_name", true);
    dbAccessor.addFKConstraint("hostgroup_configuration", "FK_hg_config_hostgroup_name",
        "hostgroup_name", "hostgroup", "name", true);
    dbAccessor.addFKConstraint("viewentity", "FK_viewentity_view_name",
        new String[]{"view_name", "view_instance_name"}, "viewinstance", new String[]{"view_name", "name"}, true);

  }


  // ----- UpgradeCatalog ----------------------------------------------------

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    String dbType = getDbType();

    // Add new sequences for view entity
    String valueColumnName = "\"value\"";
    if (Configuration.ORACLE_DB_NAME.equals(dbType) || Configuration.MYSQL_DB_NAME.equals(dbType)) {
      valueColumnName = "value";
    }

    dbAccessor.executeQuery("INSERT INTO ambari_sequences(sequence_name, " + valueColumnName + ") " +
        "VALUES('viewentity_id_seq', 0)", true);

    // Add missing property for YARN
    updateConfigurationProperties("global", Collections.singletonMap("jobhistory_heapsize", "900"), false);
  }

  @Override
  public String getTargetVersion() {
    return "1.6.0";
  }
}
