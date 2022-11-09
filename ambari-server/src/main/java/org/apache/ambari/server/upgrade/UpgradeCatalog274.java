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

import javax.persistence.Table;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link UpgradeCatalog274} upgrades Ambari from 2.7.2 to 2.7.4.
 */
public class UpgradeCatalog274 extends AbstractUpgradeCatalog {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog274.class);
  static final String AMBARI_CONFIGURATION_TABLE = AmbariConfigurationEntity.class.getAnnotation(Table.class).name();
  static final String AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN = UpgradeCatalog270.AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN;
  static final Integer AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN_LEN = 4000;


  @Inject
  public UpgradeCatalog274(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "2.7.2";
  }

  /**
   * Perform database schema transformation. Can work only before persist service start
   *
   * @throws AmbariException
   * @throws SQLException
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    upgradeConfigurationTableValueMaxSize();
  }

  @Override
  public String getTargetVersion() {
    return "2.7.4";
  }

  /**
   * Perform data insertion before running normal upgrade of data, requires started persist service
   *
   * @throws AmbariException
   * @throws SQLException
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    // no actions needed
  }

  /**
   * Performs normal data upgrade
   *
   * @throws AmbariException
   * @throws SQLException
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    // no actions needed
  }


  private void upgradeConfigurationTableValueMaxSize() throws SQLException {
    DBAccessor.DBColumnInfo propertyColumn = dbAccessor.getColumnInfo(AMBARI_CONFIGURATION_TABLE,
      AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN);

    if (propertyColumn != null && propertyColumn.getType() != null &&
      propertyColumn.getLength() < AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN_LEN) {

      LOG.info("Updating column max size to {} for {}.{}", AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN_LEN,
        AMBARI_CONFIGURATION_TABLE, AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN);

      propertyColumn.setLength(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN_LEN);
      dbAccessor.alterColumn(AMBARI_CONFIGURATION_TABLE, propertyColumn);
    }
  }
}
