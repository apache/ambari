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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

/**
 * Upgrade catalog for version 2.5.0.
 */
public class UpgradeCatalog250 extends AbstractUpgradeCatalog {

  protected static final String HOST_VERSION_TABLE = "host_version";
  private static final String AMS_ENV = "ams-env";
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog250.class);

  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog250(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.5.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.4.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    updateHostVersionTable();
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
    updateAMSConfigs();
    createRoleAuthorizations();
  }

  protected void updateHostVersionTable() throws SQLException {
    LOG.info("Updating the {} table", HOST_VERSION_TABLE);

    // Add the unique constraint to the host_version table
    dbAccessor.addUniqueConstraint(HOST_VERSION_TABLE, "UQ_host_repo", "repo_version_id", "host_id");
  }

  protected void updateAMSConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {

          Config amsEnv = cluster.getDesiredConfigByType(AMS_ENV);
          if (amsEnv != null) {
            Map<String, String> amsEnvProperties = amsEnv.getProperties();
            String content = amsEnvProperties.get("content");
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("content", updateAmsEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_ENV, newProperties, true, true);
          }

        }
      }
    }
  }


  protected String updateAmsEnvContent(String content) {
    if (content == null) {
      return null;
    }

    List<String> toReplaceList = new ArrayList<>();
    toReplaceList.add("\n# HBase normalizer enabled\n");
    toReplaceList.add("\n# HBase compaction policy enabled\n");
    toReplaceList.add("export AMS_HBASE_NORMALIZER_ENABLED={{ams_hbase_normalizer_enabled}}\n");
    toReplaceList.add("export AMS_HBASE_FIFO_COMPACTION_ENABLED={{ams_hbase_fifo_compaction_enabled}}\n");

    //Because of AMBARI-15331 : AMS HBase FIFO compaction policy and Normalizer settings are not handled correctly
    toReplaceList.add("export HBASE_NORMALIZATION_ENABLED={{ams_hbase_normalizer_enabled}}\n");
    toReplaceList.add("export HBASE_FIFO_COMPACTION_POLICY_ENABLED={{ams_hbase_fifo_compaction_policy_enabled}}\n");


    for (String toReplace : toReplaceList) {
      if (content.contains(toReplace)) {
        content = content.replace(toReplace, StringUtils.EMPTY);
      }
    }

    return content;
  }

  /**
   * Create new role authorizations: CLUSTER.RUN_CUSTOM_COMMAND and AMBARI.RUN_CUSTOM_COMMAND
   *
   * @throws SQLException
   */
  protected void createRoleAuthorizations() throws SQLException {
    LOG.info("Adding authorizations");

    addRoleAuthorization("CLUSTER.RUN_CUSTOM_COMMAND", "Perform custom cluster-level actions",
        Arrays.asList("AMBARI.ADMINISTRATOR:AMBARI", "CLUSTER.ADMINISTRATOR:CLUSTER"));

    addRoleAuthorization("AMBARI.RUN_CUSTOM_COMMAND", "Perform custom administrative actions",
        Collections.singletonList("AMBARI.ADMINISTRATOR:AMBARI"));
  }
}

