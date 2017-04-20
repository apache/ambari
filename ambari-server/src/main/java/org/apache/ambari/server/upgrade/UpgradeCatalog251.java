/**
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
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link UpgradeCatalog251} upgrades Ambari from 2.5.0 to 2.5.1.
 */
public class UpgradeCatalog251 extends AbstractUpgradeCatalog {

  static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  static final String HRC_IS_BACKGROUND_COLUMN = "is_background";

  /**
   * Constructor.
   *
   * @param injector
   */
  @Inject
  public UpgradeCatalog251(Injector injector) {
    super(injector);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.5.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.5.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    addBackgroundColumnToHostRoleCommand();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
  }

  /**
   * Adds the {@value #HRC_IS_BACKGROUND_COLUMN} column to the
   * {@value #HOST_ROLE_COMMAND_TABLE} table.
   *
   * @throws SQLException
   */
  private void addBackgroundColumnToHostRoleCommand() throws SQLException {
    dbAccessor.addColumn(HOST_ROLE_COMMAND_TABLE,
        new DBColumnInfo(HRC_IS_BACKGROUND_COLUMN, Short.class, null, 0, false));
  }
}
