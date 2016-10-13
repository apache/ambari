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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 2.4.2.
 */
public class UpgradeCatalog242 extends AbstractUpgradeCatalog {

  protected static final String EXTENSION_TABLE = "extension";
  protected static final String USERS_TABLE = "users";
  protected static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  protected static final String BLUEPRINT_TABLE = "blueprint";

  protected static final String BLUEPRINT_NAME_COLUMN = "blueprint_name";
  protected static final String EXTENSION_NAME_COLUMN = "extension_name";
  protected static final String EXTENSION_VERSION_COLUMN = "extension_version";
  protected static final String USER_TYPE_COLUMN = "user_type";
  protected static final String USER_NAME_COLUMN = "user_name";
  protected static final String ROLE_COLUMN = "role";
  protected static final String STATUS_COLUMN = "status";


  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog242.class);




  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog242(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.4.2";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.4.0";
  }


  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateTablesForMysql();
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
  }

  protected void updateTablesForMysql() throws SQLException {
    final Configuration.DatabaseType databaseType = configuration.getDatabaseType();
    if (databaseType == Configuration.DatabaseType.MYSQL) {
      dbAccessor.alterColumn(EXTENSION_TABLE, new DBAccessor.DBColumnInfo(EXTENSION_NAME_COLUMN, String.class, 100, null, false));
      dbAccessor.alterColumn(EXTENSION_TABLE, new DBAccessor.DBColumnInfo(EXTENSION_VERSION_COLUMN, String.class, 100, null, false));

      dbAccessor.alterColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USER_TYPE_COLUMN, String.class, 100, null, false));
      dbAccessor.alterColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USER_NAME_COLUMN, String.class, 100, null, false));

      dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE, new DBAccessor.DBColumnInfo(ROLE_COLUMN, String.class, 100, null, true));
      dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE, new DBAccessor.DBColumnInfo(STATUS_COLUMN, String.class, 100, null, true));

      dbAccessor.alterColumn(BLUEPRINT_TABLE, new DBAccessor.DBColumnInfo(BLUEPRINT_NAME_COLUMN, String.class, 100, null, false));
    }
  }

}
