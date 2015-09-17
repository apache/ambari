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
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;

import java.sql.SQLException;

/**
 * Upgrade catalog for version 2.0.3
 */
public class UpgradeCatalog203 extends AbstractUpgradeCatalog {

  @Inject
  public UpgradeCatalog203(Injector injector) {
    super(injector);
  }

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    Configuration.DatabaseType databaseType = configuration.getDatabaseType();
    if (Configuration.DatabaseType.MYSQL == databaseType) {
        dbAccessor.alterColumn("alert_current", new DBColumnInfo("latest_text", new FieldTypeDefinition("TEXT"), null));
        dbAccessor.alterColumn("alert_history", new DBColumnInfo("alert_text", new FieldTypeDefinition("TEXT"), null));
      } else {
        dbAccessor.alterColumn("alert_current", new DBColumnInfo("latest_text", Character[].class, null));
        dbAccessor.alterColumn("alert_history", new DBColumnInfo("alert_text", Character[].class, null));
      }
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {

  }

  @Override
  public String getTargetVersion() {
    return "2.0.3";
  }
}
