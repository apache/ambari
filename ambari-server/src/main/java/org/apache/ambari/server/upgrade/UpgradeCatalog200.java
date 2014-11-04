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
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 2.0.0.
 */
public class UpgradeCatalog200 extends AbstractUpgradeCatalog {

  private static final String ALERT_TABLE_DEFINITION = "alert_definition";

  @Inject
  private DaoUtils daoUtils;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "1.7.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "1.2.0";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger
      (UpgradeCatalog200.class);

  /**
   * Constructor.
   *
   * @param injector
   */
  @Inject
  public UpgradeCatalog200(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    // add ignore_host column to alert_definition
    dbAccessor.addColumn(ALERT_TABLE_DEFINITION, new DBColumnInfo(
        "ignore_host", Short.class, 1, 0, false));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
  }
}
