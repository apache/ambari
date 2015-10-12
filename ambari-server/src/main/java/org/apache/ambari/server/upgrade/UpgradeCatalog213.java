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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.alert.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Upgrade catalog for version 2.1.3.
 */
public class UpgradeCatalog213 extends AbstractUpgradeCatalog {

  private static final String STORM_SITE = "storm-site";
  private static final String AMS_ENV = "ams-env";
  private static final String AMS_HBASE_ENV = "ams-hbase-env";


  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog213.class);

  @Inject
  DaoUtils daoUtils;


  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog213(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.1.3";
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

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addMissingConfigs();
    updateAMSConfigs();
    updateAlertDefinitions();
  }

  /**
   * Modifies the JSON of some of the alert definitions which have changed
   * between Ambari versions.
   */
  protected void updateAlertDefinitions() {
    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      final AlertDefinitionEntity alertDefinitionEntity = alertDefinitionDAO.findByName(
          cluster.getClusterId(), "journalnode_process");

      if (alertDefinitionEntity != null) {
        String source = alertDefinitionEntity.getSource();

        alertDefinitionEntity.setSource(modifyJournalnodeProcessAlertSource(source));
        alertDefinitionEntity.setSourceType(SourceType.WEB);
        alertDefinitionEntity.setHash(UUID.randomUUID().toString());

        alertDefinitionDAO.merge(alertDefinitionEntity);
        LOG.info("journalnode_process alert definition was updated.");
      }
    }
  }

  /**
   * Modifies type of the journalnode_process alert to WEB.
   * Changes reporting text and uri according to the WEB type.
   * Removes default_port property.
   */
  String modifyJournalnodeProcessAlertSource(String source) {
    JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();

    rootJson.remove("type");
    rootJson.addProperty("type", "WEB");

    rootJson.remove("default_port");

    rootJson.remove("uri");
    JsonObject uriJson = new JsonObject();
    uriJson.addProperty("http", "{{hdfs-site/dfs.journalnode.http-address}}");
    uriJson.addProperty("https", "{{hdfs-site/dfs.journalnode.https-address}}");
    uriJson.addProperty("kerberos_keytab", "{{hdfs-site/dfs.web.authentication.kerberos.keytab}}");
    uriJson.addProperty("kerberos_principal", "{{hdfs-site/dfs.web.authentication.kerberos.principal}}");
    uriJson.addProperty("https_property", "{{hdfs-site/dfs.http.policy}}");
    uriJson.addProperty("https_property_value", "HTTPS_ONLY");
    uriJson.addProperty("connection_timeout", 5.0);
    rootJson.add("uri", uriJson);

    rootJson.getAsJsonObject("reporting").getAsJsonObject("ok").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("ok").addProperty(
            "text", "HTTP {0} response in {2:.3f}s");

    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").addProperty(
            "text", "HTTP {0} response from {1} in {2:.3f}s ({3})");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").remove("value");

    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").addProperty("text",
            "Connection failed to {1} ({3})");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").remove("value");

    return rootJson.toString();
  }

  protected void addMissingConfigs() throws AmbariException {
    updateStormConfigs();
  }

  protected void updateStormConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if ((clusterMap != null) && !clusterMap.isEmpty()) {
        // Iterate through the clusters and perform any configuration updates
        for (final Cluster cluster : clusterMap.values()) {
          Config stormSiteProps = cluster.getDesiredConfigByType(STORM_SITE);

          if (stormSiteProps != null) {
            String value = stormSiteProps.getProperties().get("nimbus.monitor.freq.secs");
            if (value != null && value.equals("10")) {
              Map<String, String> updates = new HashMap<String, String>();
              updates.put("nimbus.monitor.freq.secs", "120");
              updateConfigurationPropertiesForCluster(cluster, STORM_SITE, updates, true, false);
            }
          }
        }
      }
    }
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

            String metrics_collector_heapsize = amsEnvProperties.get("metrics_collector_heapsize");
            String content = amsEnvProperties.get("content");
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("metrics_collector_heapsize", memoryToIntMb(metrics_collector_heapsize));
            newProperties.put("content", updateAmsEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_ENV, newProperties, true, true);
          }
          Config amsHbaseEnv = cluster.getDesiredConfigByType(AMS_HBASE_ENV);
          if (amsHbaseEnv != null) {
            Map<String, String> amsHbaseEnvProperties = amsHbaseEnv.getProperties();
            String hbase_regionserver_heapsize = amsHbaseEnvProperties.get("hbase_regionserver_heapsize");
            String regionserver_xmn_size = amsHbaseEnvProperties.get("regionserver_xmn_size");
            String hbase_master_xmn_size = amsHbaseEnvProperties.get("hbase_master_xmn_size");
            String hbase_master_maxperm_size = amsHbaseEnvProperties.get("hbase_master_maxperm_size");
            String hbase_master_heapsize = amsHbaseEnvProperties.get("hbase_master_heapsize");
            String content = amsHbaseEnvProperties.get("content");

            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("hbase_regionserver_heapsize", memoryToIntMb(hbase_regionserver_heapsize));
            newProperties.put("regionserver_xmn_size", memoryToIntMb(regionserver_xmn_size));
            newProperties.put("hbase_master_xmn_size", memoryToIntMb(hbase_master_xmn_size));
            newProperties.put("hbase_master_maxperm_size", memoryToIntMb(hbase_master_maxperm_size));
            newProperties.put("hbase_master_heapsize", memoryToIntMb(hbase_master_heapsize));
            newProperties.put("content", updateAmsHbaseEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_ENV, newProperties, true, true);
          }
        }
      }
    }

  }

  protected String updateAmsEnvContent(String oldContent) {
    if (oldContent == null) {
      return null;
    }
    String regSearch = "export\\s*AMS_COLLECTOR_HEAPSIZE\\s*=\\s*\\{\\{metrics_collector_heapsize\\}\\}";
    String replacement = "export AMS_COLLECTOR_HEAPSIZE={{metrics_collector_heapsize}}m";
    return oldContent.replaceAll(regSearch, replacement);
  }

  protected String updateAmsHbaseEnvContent(String content) {
    if (content == null) {
      return null;
    }

    String regSearch = "\\{\\{hbase_heapsize\\}\\}";
    String replacement = "{{hbase_heapsize}}m";
    content = content.replaceAll(regSearch, replacement);
    regSearch = "\\{\\{hbase_master_maxperm_size\\}\\}";
    replacement = "{{hbase_master_maxperm_size}}m";
    content = content.replaceAll(regSearch, replacement);
    regSearch = "\\{\\{hbase_master_xmn_size\\}\\}";
    replacement = "{{hbase_master_xmn_size}}m";
    content = content.replaceAll(regSearch, replacement);
    regSearch = "\\{\\{regionserver_xmn_size\\}\\}";
    replacement = "{{regionserver_xmn_size}}m";
    content = content.replaceAll(regSearch, replacement);
    regSearch = "\\{\\{regionserver_heapsize\\}\\}";
    replacement = "{{regionserver_heapsize}}m";
    content = content.replaceAll(regSearch, replacement);
    return content;
  }

  private String memoryToIntMb(String memorySize) {
    if (memorySize == null) {
      return "0";
    }
    Integer value = 0;
    try {
      value = Integer.parseInt(memorySize.replaceAll("\\D+", ""));
    } catch (NumberFormatException ex) {
      LOG.error(ex.getMessage());
    }
    char unit = memorySize.toUpperCase().charAt(memorySize.length() - 1);
    // Recalculate memory size to Mb
    switch (unit) {
      case 'K':
        value /= 1024;
        break;
      case 'B':
        value /= (1024*1024);
        break;
      case 'G':
        value *= 1024;
        break;
      case 'T':
        value *= 1024*1024;
        break;
    }
    return value.toString();
  }
}
