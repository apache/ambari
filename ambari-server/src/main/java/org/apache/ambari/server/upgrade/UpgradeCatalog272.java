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

import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.LDAP_CONFIGURATION;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_TABLE;

import java.sql.SQLException;

import org.apache.ambari.server.AmbariException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link UpgradeCatalog272} upgrades Ambari from 2.7.1 to 2.7.2.
 */
public class UpgradeCatalog272 extends AbstractUpgradeCatalog {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog272.class);

  private static final String LDAP_CONFIGURATION_WRONG_COLLISION_BEHAVIOR_PROPERTY_NAME = "ambari.ldap.advance.collision_behavior";
  private static final String LDAP_CONFIGURATION_CORRECT_COLLISION_BEHAVIOR_PROPERTY_NAME = "ambari.ldap.advanced.collision_behavior";
  static final String RENAME_COLLISION_BEHAVIOR_PROPERTY_SQL = String.format("UPDATE %s SET %s = '%s' WHERE %s = '%s' AND %s = '%s'", AMBARI_CONFIGURATION_TABLE,
      AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN, LDAP_CONFIGURATION_CORRECT_COLLISION_BEHAVIOR_PROPERTY_NAME, AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN,
      LDAP_CONFIGURATION.getCategoryName(), AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN, LDAP_CONFIGURATION_WRONG_COLLISION_BEHAVIOR_PROPERTY_NAME);

  @Inject
  public UpgradeCatalog272(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "2.7.1";
  }

  @Override
  public String getTargetVersion() {
    return "2.7.2";
  }

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    // nothing to do
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    // nothing to do
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    renameLdapSynchCollisionBehaviorValue();
  }

  protected int renameLdapSynchCollisionBehaviorValue() throws SQLException {
    int numberOfRecordsRenamed = 0;
    if (dbAccessor.tableExists(AMBARI_CONFIGURATION_TABLE)) {
      LOG.debug(String.format("Executing: %s", RENAME_COLLISION_BEHAVIOR_PROPERTY_SQL));
      numberOfRecordsRenamed = dbAccessor.executeUpdate(RENAME_COLLISION_BEHAVIOR_PROPERTY_SQL);
      LOG.info(String.format("Renamed %d %s with incorrect LDAP configuration property name", numberOfRecordsRenamed, 1 >= numberOfRecordsRenamed ? "record" : "records"));
    } else {
      LOG.info(String.format("%s table does not exists; nothing to update", AMBARI_CONFIGURATION_TABLE));
    }
    return numberOfRecordsRenamed;
  }

}
