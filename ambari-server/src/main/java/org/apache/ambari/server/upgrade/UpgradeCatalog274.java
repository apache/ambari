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
import java.util.List;
import java.util.Map;

import javax.persistence.Table;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.ParameterizedSource;
import org.apache.ambari.server.state.alert.ScriptSource;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link UpgradeCatalog274} upgrades Ambari from 2.7.3 to 2.7.4.
 */
public class UpgradeCatalog274 extends AbstractUpgradeCatalog {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog274.class);
  static final String AMBARI_CONFIGURATION_TABLE = AmbariConfigurationEntity.class.getAnnotation(Table.class).name();
  static final String AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN = UpgradeCatalog270.AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN;
  static final Integer AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN_LEN = 4000;

  static final String HDFS_SERVICE_NAME = "HDFS";
  static final String NAMENODE_COMPONENT_NAME = "NAMENODE";
  static final String APPID_PROPERTY_NAME = "appId";
  static final String NAMENODE_APP_ID = NAMENODE_COMPONENT_NAME.toLowerCase();


  @Inject
  public UpgradeCatalog274(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "2.7.3";
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
    addNewConfigurationsFromXml();
    updateNameNodeAlertsAppId();
  }

  protected void updateNameNodeAlertsAppId() {
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionFactory alertDefinitionFactory = injector.getInstance(AlertDefinitionFactory.class);

    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (MapUtils.isNotEmpty(clusterMap)) {
        for (final Cluster cluster : clusterMap.values()) {
          if (cluster.getServices().containsKey(HDFS_SERVICE_NAME)) {
            List<AlertDefinitionEntity> alertDefinitions =
                alertDefinitionDAO.findByServiceComponent(cluster.getClusterId(), HDFS_SERVICE_NAME, NAMENODE_COMPONENT_NAME);
            for (AlertDefinitionEntity alertDefinitionEntity : alertDefinitions) {
              if (SourceType.SCRIPT.equals(alertDefinitionEntity.getSourceType())) {
                AlertDefinition databaseDefinition = alertDefinitionFactory.coerce(alertDefinitionEntity);
                ScriptSource scriptSource = (ScriptSource) databaseDefinition.getSource();
                for (ParameterizedSource.AlertParameter scriptParameter : scriptSource.getParameters()) {
                  if (APPID_PROPERTY_NAME.equals(scriptParameter.getName())) {
                    String value = (String) scriptParameter.getValue();
                    if (value.equalsIgnoreCase(NAMENODE_APP_ID) && !value.equals(NAMENODE_APP_ID)) {
                      scriptParameter.setValue(NAMENODE_APP_ID);
                    }
                  }
                }
                alertDefinitionEntity = alertDefinitionFactory.mergeSource(scriptSource, alertDefinitionEntity);
                alertDefinitionDAO.merge(alertDefinitionEntity);
              }
            }
          }
        }
      }
    }
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
