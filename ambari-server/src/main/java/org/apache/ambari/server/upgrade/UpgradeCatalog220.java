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

import com.google.inject.Provider;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import javax.persistence.EntityManager;


/**
 * Upgrade catalog for version 2.2.0.
 */
public class UpgradeCatalog220 extends AbstractUpgradeCatalog {
  private static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  private static final String USERS_TABLE = "users";

  private static final String HOST_ID_COL = "host_id";
  private static final String USER_TYPE_COL = "user_type";

  private static final String ADMIN_PERMISSION_TABLE = "adminpermission";
  private static final String PERMISSION_LABEL_COL = "permission_label";

  @Inject
  DaoUtils daoUtils;


  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.1.3";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.2.0";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog220.class);

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog220(Injector injector) {
    super(injector);
    this.injector = injector;

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {

    dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));
    dbAccessor.addColumn(USERS_TABLE, new DBColumnInfo(USER_TYPE_COL, String.class, null, "LOCAL", true));

    dbAccessor.executeQuery("UPDATE users SET user_type='LDAP' WHERE ldap_user=1");

    dbAccessor.addUniqueConstraint(USERS_TABLE, "UNQ_users_0", "user_name", "user_type");


    updateAdminPermissionTable();
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    setPermissionLabels();
  }


  // ----- UpgradeCatalog ----------------------------------------------------

  private void updateAdminPermissionTable() throws SQLException {
    // Add the permission_label column to the adminpermission table
    dbAccessor.addColumn(ADMIN_PERMISSION_TABLE, new DBColumnInfo(PERMISSION_LABEL_COL, String.class, 255, null, true));
  }

  private void setPermissionLabels() throws SQLException {
    String updateStatement = "UPDATE " + ADMIN_PERMISSION_TABLE + " SET " + PERMISSION_LABEL_COL + "='%s' WHERE permission_id=%d";

    dbAccessor.executeUpdate(String.format(updateStatement, "Administrator", 1));
    dbAccessor.executeUpdate(String.format(updateStatement, "Read-Only", 2));
    dbAccessor.executeUpdate(String.format(updateStatement, "Operator", 3));
    dbAccessor.executeUpdate(String.format(updateStatement, "Use View", 4));
  }



}
