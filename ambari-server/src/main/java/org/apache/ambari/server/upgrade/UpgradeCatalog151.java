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
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.orm.DBAccessor;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 1.5.1.
 */
public class UpgradeCatalog151 extends AbstractUpgradeCatalog {

  //SourceVersion is only for book-keeping purpose
  @Override
  public String getSourceVersion() {
    return "1.5.0";
  }

  @Override
  public String getTargetVersion() {
    return "1.5.1";
  }

  // ----- Constructors ------------------------------------------------------

  @Inject
  public UpgradeCatalog151(Injector injector) {
    super(injector);
  }


  // ----- AbstractUpgradeCatalog --------------------------------------------

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<DBAccessor.DBColumnInfo>();
    DatabaseType databaseType = configuration.getDatabaseType();

    // ========================================================================
    // Create tables

    // View
    columns.clear();
    columns.add(new DBAccessor.DBColumnInfo("view_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("label", String.class, 255, null, true));
    columns.add(new DBAccessor.DBColumnInfo("version", String.class, 255, null, true));
    columns.add(new DBAccessor.DBColumnInfo("archive", String.class, 255, null, true));

    dbAccessor.createTable("viewmain", columns, "view_name");

    // View Instance Data
    columns.clear();
    columns.add(new DBAccessor.DBColumnInfo("view_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("view_instance_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("value", String.class, 255, null, true));

    dbAccessor.createTable("viewinstancedata", columns, "view_name", "view_instance_name", "name");

    // View Instance
    columns.clear();
    columns.add(new DBAccessor.DBColumnInfo("view_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("name", String.class, 255, null, false));

    dbAccessor.createTable("viewinstance", columns, "view_name", "name");

    // View Instance Property
    columns.clear();
    columns.add(new DBAccessor.DBColumnInfo("view_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("view_instance_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("value", String.class, 255, null, true));

    dbAccessor.createTable("viewinstanceproperty", columns, "view_name", "view_instance_name", "name");

    // View Parameter
    columns.clear();
    columns.add(new DBAccessor.DBColumnInfo("view_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("description", String.class, 255, null, true));
    columns.add(new DBAccessor.DBColumnInfo("required", Character.class, 1, null, true));

    dbAccessor.createTable("viewparameter", columns, "view_name", "name");

    // View Resource
    columns.clear();
    columns.add(new DBAccessor.DBColumnInfo("view_name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("name", String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo("plural_name", String.class, 255, null, true));
    columns.add(new DBAccessor.DBColumnInfo("id_property", String.class, 255, null, true));
    columns.add(new DBAccessor.DBColumnInfo("subResource_names", String.class, 255, null, true));
    columns.add(new DBAccessor.DBColumnInfo("provider", String.class, 255, null, true));
    columns.add(new DBAccessor.DBColumnInfo("service", String.class, 255, null, true));
    if (databaseType == DatabaseType.MYSQL) {
      columns.add(new DBAccessor.DBColumnInfo("`resource`", String.class, 255, null, true));
      //TODO incorrect name for MySQL

    } else{
      columns.add(new DBAccessor.DBColumnInfo("\"resource\"", String.class, 255, null, true));
        //TODO incorrect name for oracle
    }
    dbAccessor.createTable("viewresource", columns, "view_name", "name");

    // ========================================================================
    // Add constraints
    dbAccessor.addFKConstraint("viewparameter", "FK_viewparam_view_name", "view_name", "viewmain", "view_name", true);
    dbAccessor.addFKConstraint("viewresource", "FK_viewres_view_name", "view_name", "viewmain", "view_name", true);
    dbAccessor.addFKConstraint("viewinstance", "FK_viewinst_view_name", "view_name", "viewmain", "view_name", true);
    dbAccessor.addFKConstraint("viewinstanceproperty", "FK_viewinstprop_view_name",
        new String[]{"view_name", "view_instance_name"}, "viewinstance", new String[]{"view_name", "name"}, true);
    dbAccessor.addFKConstraint("viewinstancedata", "FK_viewinstdata_view_name",
        new String[]{"view_name", "view_instance_name"}, "viewinstance", new String[]{"view_name", "name"}, true);

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
  public void executeDMLUpdates() throws AmbariException, SQLException {
  }
}
