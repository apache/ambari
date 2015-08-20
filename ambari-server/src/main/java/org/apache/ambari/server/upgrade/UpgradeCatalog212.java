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
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;


/**
 * Upgrade catalog for version 2.1.2.
 */
public class UpgradeCatalog212 extends AbstractUpgradeCatalog {
  private static final String HIVE_SITE = "hive-site";
  private static final String HIVE_ENV = "hive-env";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog212.class);

  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog212(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.1.2";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.1.1";
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
    addMissingConfigs();
  }

  protected void addMissingConfigs() throws AmbariException {
    updateHiveConfigs();
  }

  protected void updateHiveConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          String content = null;
          Boolean isHiveSitePresent = cluster.getDesiredConfigByType(HIVE_SITE) != null;
          StackId stackId = cluster.getCurrentStackVersion();
          Boolean isStackNotLess22 = (stackId != null && stackId.getStackName().equals("HDP") &&
                  VersionUtils.compareVersions(stackId.getStackVersion(), "2.2") >= 0);

          if (cluster.getDesiredConfigByType(HIVE_ENV) != null && isStackNotLess22) {
            Map<String, String> hiveEnvProps = new HashMap<String, String>();
            content = cluster.getDesiredConfigByType(HIVE_ENV).getProperties().get("content");
            if(content != null) {
              content = updateHiveEnvContent(content);
              hiveEnvProps.put("content", content);
            }
            updateConfigurationPropertiesForCluster(cluster, HIVE_ENV, hiveEnvProps, true, true);
          }

          if (isHiveSitePresent && isStackNotLess22) {
            Set<String> hiveSiteRemoveProps = new HashSet<String>();
            hiveSiteRemoveProps.add("hive.heapsize");
            hiveSiteRemoveProps.add("hive.optimize.mapjoin.mapreduce");
            hiveSiteRemoveProps.add("hive.server2.enable.impersonation");
            hiveSiteRemoveProps.add("hive.auto.convert.sortmerge.join.noconditionaltask");

            updateConfigurationPropertiesForCluster(cluster, HIVE_SITE, new HashMap<String, String>(), hiveSiteRemoveProps, false, true);
          }
        }
      }
    }
  }

  protected String updateHiveEnvContent(String hiveEnvContent) {
    if(hiveEnvContent == null) {
      return null;
    }
    String oldHeapSizeRegex = "export HADOOP_HEAPSIZE=\"\\{\\{hive_heapsize\\}\\}\"\\s*\\n" +
            "export HADOOP_CLIENT_OPTS=\"-Xmx\\$\\{HADOOP_HEAPSIZE\\}m \\$HADOOP_CLIENT_OPTS\"";
    String newAuxJarPath = "";
    return hiveEnvContent.replaceAll(oldHeapSizeRegex, Matcher.quoteReplacement(newAuxJarPath));
  }

}
