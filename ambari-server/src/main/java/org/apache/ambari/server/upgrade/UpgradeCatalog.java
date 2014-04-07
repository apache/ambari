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

import org.apache.ambari.server.AmbariException;

import java.sql.SQLException;

/**
 * Interface for upgrading Ambari DB
 */
public interface UpgradeCatalog {
  /**
   * Run the upgrade scripts for upgrading ambari server from current version
   * to the new version.
   * @throws AmbariException
   */
  public void upgradeSchema() throws AmbariException, SQLException;

  /**
   * perform data updates as necessary, requires started persist service
   * @throws AmbariException
   * @throws SQLException
   */
  public void upgradeData() throws AmbariException, SQLException;

  /**
   * Return the version that will be upgraded to
   * @return
   */
  public abstract String getTargetVersion();

  /**
   * Return latest source version that can be upgraded from.
   * Return null since no UpgradeCatalogs exist before this one.
   *
   * @return null : default
   */
  public String getSourceVersion();
}
