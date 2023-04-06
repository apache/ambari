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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * The {@link UpgradeCatalog275} upgrades Ambari from 2.7.4 to 2.7.5.
 */
public class UpgradeCatalog275 extends AbstractUpgradeCatalog {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog275.class);
  static final Gson GSON = new Gson();

  @Inject
  public UpgradeCatalog275(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "2.7.4";
  }

  @Override
  public String getTargetVersion() {
    return "2.7.5";
  }

  /**
   * Perform database schema transformation. Can work only before persist service start
   *
   * @throws AmbariException
   * @throws SQLException
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    // no actions needed
  }

  /**
   * Perform data insertion before running normal upgrade of data, requires started persist service
   *
   * @throws AmbariException
   * @throws SQLException
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    LOG.debug("UpgradeCatalog275 executing Pre-DML Updates.");
    removeDfsHAInitial();
  }

  /**
   * Performs normal data upgrade
   *
   * @throws AmbariException
   * @throws SQLException
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    LOG.debug("UpgradeCatalog275 executing DML Updates.");
    addNewConfigurationsFromXml();
  }

  protected void removeDfsHAInitial() {
    BlueprintDAO blueprintDAO = injector.getInstance(BlueprintDAO.class);
    List<BlueprintEntity> blueprintEntityList = blueprintDAO.findAll();
    List<BlueprintEntity> changedBlueprints = new ArrayList<>();
    for (BlueprintEntity blueprintEntity : blueprintEntityList){
      boolean changed = false;
      Collection<BlueprintConfigEntity> blueprintConfigurations = blueprintEntity.getConfigurations();
      for (BlueprintConfigEntity blueprintConfigEntity : blueprintConfigurations) {
        if (blueprintConfigEntity.getType().equals("hadoop-env")) {
          String configData = blueprintConfigEntity.getConfigData();

          Map<String, String> typeProperties = GSON.<Map<String, String>>fromJson(
            configData, Map.class);

          typeProperties.remove("dfs_ha_initial_namenode_standby");
          typeProperties.remove("dfs_ha_initial_namenode_active");

          blueprintConfigEntity.setConfigData(GSON.toJson(typeProperties));
          changed = true;
        }
      }
      if (changed) {
        changedBlueprints.add(blueprintEntity);
      }
    }
    for (BlueprintEntity blueprintEntity : changedBlueprints) {
      blueprintDAO.merge(blueprintEntity);
    }
  }
}
