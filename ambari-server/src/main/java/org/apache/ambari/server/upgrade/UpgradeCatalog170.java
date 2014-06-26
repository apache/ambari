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
import org.apache.ambari.server.orm.DBAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 1.7.0.
 */
public class UpgradeCatalog170 extends AbstractUpgradeCatalog {

  //SourceVersion is only for book-keeping purpos
  @Override
  public String getSourceVersion() {
    return "1.6.1";
  }

  @Override
  public String getTargetVersion() {
    return "1.7.0";
  }

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger
      (UpgradeCatalog170.class);
  
  // ----- Constructors ------------------------------------------------------

  @Inject
  public UpgradeCatalog170(Injector injector) {
    super(injector);
  }


  // ----- AbstractUpgradeCatalog --------------------------------------------

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    DBAccessor.DBColumnInfo clusterConfigAttributesColumn = new DBAccessor.DBColumnInfo(
        "config_attributes", String.class, 32000, null, true);
    dbAccessor.addColumn("clusterconfig", clusterConfigAttributesColumn);
  }


  // ----- UpgradeCatalog ----------------------------------------------------

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {}
  
  protected void addMissingConfigs() throws AmbariException {}

}
