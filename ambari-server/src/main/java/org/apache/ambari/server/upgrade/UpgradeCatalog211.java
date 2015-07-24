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
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * Upgrade catalog for version 2.1.1.
 */
public class UpgradeCatalog211 extends AbstractUpgradeCatalog {
  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog211(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.1.1";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.1.0";
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
    updateExistingConfigurations();
  }


  // ----- UpgradeCatalog211 --------------------------------------------


  /**
   * Iterates over the set of clusters to call service-specific configuration update routines.
   *
   * @throws AmbariException if an error occurs while updating the configurations
   */
  protected void updateExistingConfigurations() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if ((clusterMap != null) && !clusterMap.isEmpty()) {
        // Iterate through the clusters and perform any configuration updates
        for (final Cluster cluster : clusterMap.values()) {
          updateKerberosConfigurations(cluster);

          /* *********************************************************
           * Add additional configuration update methods here
           * ********************************************************* */
        }
      }
    }
  }

  /**
   * Updates the Kerberos configurations for the given cluster
   * <p/>
   * Performs the following updates:
   * <ul>
   * <li>Rename <code>create_attributes_template</code> to <code>ad_create_attributes_template</code></li>
   * </ul>
   *
   * @param cluster the cluster
   * @throws AmbariException if an error occurs while updating the configurations
   */
  protected void updateKerberosConfigurations(Cluster cluster) throws AmbariException {
    Config config = cluster.getDesiredConfigByType("kerberos-env");

    if (config != null) {
      // Rename create_attributes_template to ad_create_attributes_template
      String value = config.getProperties().get("create_attributes_template");
      Map<String, String> updates = Collections.singletonMap("ad_create_attributes_template", value);
      Set<String> removes = Collections.singleton("create_attributes_template");

      updateConfigurationPropertiesForCluster(cluster, "kerberos-env", updates, removes, true, false);
    }
  }
}
