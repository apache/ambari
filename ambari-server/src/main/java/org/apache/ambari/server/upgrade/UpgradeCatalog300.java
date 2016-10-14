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
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class UpgradeCatalog300 extends AbstractUpgradeCatalog {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog300.class);

  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog300(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "3.0.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

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
  protected void executeDDLUpdates() throws AmbariException, SQLException {
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
    addNewConfigurationsFromXml();
    showHcatDeletedUserMessage();
  }

  protected void showHcatDeletedUserMessage() {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      for (final Cluster cluster : clusterMap.values()) {
        Config hiveEnvConfig = cluster.getDesiredConfigByType("hive-env");
        if (hiveEnvConfig != null) {
          Map<String, String> hiveEnvProperties = hiveEnvConfig.getProperties();
          String webhcatUser = hiveEnvProperties.get("webhcat_user");
          String hcatUser = hiveEnvProperties.get("hcat_user");
          if (!StringUtils.equals(webhcatUser, hcatUser)) {
            System.out.print("WARNING: In hive-env config, webhcat and hcat user are different. In current ambari release (3.0.0), hcat user was removed from stack, so potentially you could have some problems.");
            LOG.warn("In hive-env config, webhcat and hcat user are different. In current ambari release (3.0.0), hcat user was removed from stack, so potentially you could have some problems.");
          }
        }
      }
    }

  }

}
