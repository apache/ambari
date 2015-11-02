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
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Upgrade catalog for version 2.1.2.1
 */
public class UpgradeCatalog2121 extends AbstractUpgradeCatalog {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog2121.class);

  @Inject
  DaoUtils daoUtils;

  private static final String OOZIE_SITE_CONFIG = "oozie-site";
  private static final String OOZIE_AUTHENTICATION_KERBEROS_NAME_RULES = "oozie.authentication.kerberos.name.rules";

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog2121(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.1.2.1";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.1.2";
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
    updatePHDConfigs();
    updateOozieConfigs();
  }

  /**
   * Update PHD stack configs
   * @throws AmbariException
   */
  protected void updatePHDConfigs() throws AmbariException {

    Map<String, String> replacements = new LinkedHashMap<String, String>();
    replacements.put("-Dstack.name=\\{\\{\\s*stack_name\\s*\\}\\}\\s*", "");
    replacements.put("-Dstack.name=\\$\\{stack.name\\}\\s*", "");
    replacements.put("-Dstack.version=\\{\\{\\s*stack_version_buildnum\\s*\\}\\}", "-Dhdp.version=\\$HDP_VERSION");
    replacements.put("-Dstack.version=\\$\\{stack.version\\}", "-Dhdp.version=\\$\\{hdp.version\\}");
    replacements.put("\\{\\{\\s*stack_name\\s*\\}\\}", "phd");
    replacements.put("\\$\\{stack.name\\}", "phd");
    replacements.put("\\$\\{stack.version\\}", "\\$\\{hdp.version\\}");

    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if ((clusterMap != null) && !clusterMap.isEmpty()) {
        // Iterate through the clusters and perform any configuration updates
        for (final Cluster cluster : clusterMap.values()) {
          StackId currentStackVersion = cluster.getCurrentStackVersion();
          String currentStackName = currentStackVersion != null? currentStackVersion.getStackName() : null;
          if (currentStackName != null && currentStackName.equalsIgnoreCase("PHD")) {
            // Update configs only if PHD stack is deployed
            Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
            if(desiredConfigs != null && !desiredConfigs.isEmpty()) {
              for (Map.Entry<String, DesiredConfig> dc : desiredConfigs.entrySet()) {
                String configType = dc.getKey();
                DesiredConfig desiredConfig = dc.getValue();
                String configTag = desiredConfig.getTag();
                Config config = cluster.getConfig(configType, configTag);

                Map<String, String> properties = config.getProperties();
                if(properties != null && !properties.isEmpty()) {
                  Map<String, String> updates = new HashMap<String, String>();
                  for (Map.Entry<String, String> property : properties.entrySet()) {
                    String propertyKey = property.getKey();
                    String propertyValue = property.getValue();
                    String modifiedPropertyValue = propertyValue;
                    for (String regex : replacements.keySet()) {
                      modifiedPropertyValue = modifiedPropertyValue.replaceAll(regex, replacements.get(regex));
                    }
                    if (!modifiedPropertyValue.equals(propertyValue)) {
                      updates.put(propertyKey, modifiedPropertyValue);
                    }
                  }
                  if (!updates.isEmpty()) {
                    updateConfigurationPropertiesForCluster(cluster, configType, updates, true, false);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  protected void updateOozieConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config oozieSiteProps = cluster.getDesiredConfigByType(OOZIE_SITE_CONFIG);
      if (oozieSiteProps != null) {
        // Remove oozie.authentication.kerberos.name.rules if empty
        String oozieAuthKerbRules = oozieSiteProps.getProperties().get(OOZIE_AUTHENTICATION_KERBEROS_NAME_RULES);
        if (StringUtils.isBlank(oozieAuthKerbRules)) {
          Set<String> removeProperties = new HashSet<String>();
          removeProperties.add(OOZIE_AUTHENTICATION_KERBEROS_NAME_RULES);
          updateConfigurationPropertiesForCluster(cluster, OOZIE_SITE_CONFIG, new HashMap<String, String>(), removeProperties, true, false);
        }
      }
    }

  }
}

