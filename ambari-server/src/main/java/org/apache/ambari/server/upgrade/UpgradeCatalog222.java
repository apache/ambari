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

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.WidgetLayout;
import org.apache.ambari.server.state.stack.WidgetLayoutInfo;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 2.2.2.
 */
public class UpgradeCatalog222 extends AbstractUpgradeCatalog {

  @Inject
  DaoUtils daoUtils;

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog222.class);
  private static final String AMS_SITE = "ams-site";
  private static final String AMS_HBASE_SITE = "ams-hbase-site";
  private static final String HIVE_SITE_CONFIG = "hive-site";
  private static final String ATLAS_APPLICATION_PROPERTIES_CONFIG = "application-properties";
  private static final String ATLAS_HOOK_HIVE_MINTHREADS_PROPERTY = "atlas.hook.hive.minThreads";
  private static final String ATLAS_HOOK_HIVE_MAXTHREADS_PROPERTY = "atlas.hook.hive.maxThreads";
  private static final String ATLAS_CLUSTER_NAME_PROPERTY = "atlas.cluster.name";
  private static final String ATLAS_ENABLETLS_PROPERTY = "atlas.enableTLS";
  private static final String ATLAS_SERVER_HTTP_PORT_PROPERTY = "atlas.server.http.port";
  private static final String ATLAS_SERVER_HTTPS_PORT_PROPERTY = "atlas.server.https.port";
  private static final String ATLAS_REST_ADDRESS_PROPERTY = "atlas.rest.address";

  private static final String UPGRADE_TABLE = "upgrade";
  private static final String UPGRADE_SUSPENDED_COLUMN = "suspended";

  private static final String HOST_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY =
    "timeline.metrics.host.aggregator.daily.checkpointCutOffMultiplier";
  private static final String CLUSTER_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY =
    "timeline.metrics.cluster.aggregator.daily.checkpointCutOffMultiplier";
  private static final String TIMELINE_METRICS_SERVICE_WATCHER_DISBALED_PROPERTY = "timeline.metrics.service.watcher.disabled";
  private static final String AMS_MODE_PROPERTY = "timeline.metrics.service.operation.mode";
  public static final String PRECISION_TABLE_TTL_PROPERTY = "timeline.metrics.host.aggregator.ttl";
  public static final String CLUSTER_SECOND_TABLE_TTL_PROPERTY = "timeline.metrics.cluster.aggregator.second.ttl";
  public static final String CLUSTER_MINUTE_TABLE_TTL_PROPERTY = "timeline.metrics.cluster.aggregator.minute.ttl";
  public static final String AMS_WEBAPP_ADDRESS_PROPERTY = "timeline.metrics.service.webapp.address";
  public static final String HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD_PROPERTY = "hbase.client.scanner.timeout.period";
  public static final String HBASE_RPC_TIMEOUT_PROPERTY = "hbase.rpc.timeout";
  public static final String PHOENIX_QUERY_TIMEOUT_PROPERTY = "phoenix.query.timeoutMs";
  public static final String PHOENIX_QUERY_KEEPALIVE_PROPERTY = "phoenix.query.keepAliveMs";
  public static final String TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED
    = "timeline.metrics.cluster.aggregator.interpolation.enabled";

  public static final String AMS_SERVICE_NAME = "AMBARI_METRICS";
  public static final String AMS_COLLECTOR_COMPONENT_NAME = "METRICS_COLLECTOR";

  private static final String[] HDFS_WIDGETS_TO_UPDATE = new String[] {
    "NameNode RPC", "NN Connection Load" };

  protected static final String WIDGET_TABLE = "widget";
  protected static final String WIDGET_DESCRIPTION = "description";
  protected static final String WIDGET_NAME = "widget_name";
  protected static final String WIDGET_CORRUPT_BLOCKS = "Corrupted Blocks";
  protected static final String WIDGET_CORRUPT_REPLICAS = "Blocks With Corrupted Replicas";
  protected static final String WIDGET_CORRUPT_REPLICAS_DESCRIPTION = "Number represents data blocks with at least one " +
    "corrupted replica (but not all of them). Its indicative of HDFS bad health.";
  protected static final String WIDGET_VALUES = "widget_values";
  protected static final String WIDGET_VALUES_VALUE =
    "${Hadoop:service\\" +
    "\\u003dNameNode,name\\" +
    "\\u003dFSNamesystem.CorruptBlocks}";


  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog222(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.2.2";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.2.1";
  }


  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    DBAccessor.DBColumnInfo columnInfo = new DBAccessor.DBColumnInfo("host_id", Long.class);
    dbAccessor.addColumn("topology_host_info", columnInfo);
    dbAccessor.addFKConstraint("topology_host_info", "FK_hostinfo_host_id", "host_id", "hosts", "host_id", true);
    dbAccessor.executeUpdate("update topology_host_info set host_id = (select hosts.host_id from hosts where hosts.host_name = topology_host_info.fqdn)");

    updateUpgradeTable();
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    updateAlerts();
    updateStormConfigs();
    updateAMSConfigs();
    updateHiveConfig();
    updateHostRoleCommands();
    updateHDFSWidgetDefinition();
    updateCorruptedReplicaWidget();
    updateZookeeperConfigs();
    createNewSliderConfigVersion();
  }

  protected void createNewSliderConfigVersion() {
    // Here we are creating new service config version for SLIDER, to link slider-client
    // config to SLIDER service, in serviceconfigmapping table. It could be not mapped because
    // of bug which we had a long time ago.
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(ambariManagementController.getClusters());

    for (final Cluster cluster : clusterMap.values()) {
      Service sliderService = null;
      try {
        sliderService = cluster.getService("SLIDER");
      } catch(AmbariException ambariException) {
        LOG.info("SLIDER service not found in cluster while creating new serviceconfig version for SLIDER service.");
      }
      if (sliderService != null) {
        cluster.createServiceConfigVersion("SLIDER", AUTHENTICATED_USER_NAME, "Creating new service config version for SLIDER service.", null);
      }
    }
  }

  protected void updateZookeeperConfigs() throws  AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(ambariManagementController.getClusters());

    for (final Cluster cluster : clusterMap.values()) {
      Config zooEnv = cluster.getDesiredConfigByType("zookeeper-env");
      if (zooEnv != null && zooEnv.getProperties().containsKey("zk_server_heapsize")) {
        String heapSizeValue = zooEnv.getProperties().get("zk_server_heapsize");
        if(!heapSizeValue.endsWith("m")) {
          Map<String, String> updates = new HashMap<String, String>();
          updates.put("zk_server_heapsize", heapSizeValue+"m");
          updateConfigurationPropertiesForCluster(cluster, "zookeeper-env", updates, true, false);
        }

      }
    }
  }

  protected void updateStormConfigs() throws  AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(ambariManagementController.getClusters());

    for (final Cluster cluster : clusterMap.values()) {
      if (cluster.getDesiredConfigByType("storm-site") != null && cluster.getDesiredConfigByType("storm-site").getProperties().containsKey("storm.zookeeper.superACL")
              && cluster.getDesiredConfigByType("storm-site").getProperties().get("storm.zookeeper.superACL").equals("sasl:{{storm_base_jaas_principal}}")) {
        Map<String, String> newStormProps = new HashMap<String, String>();
        newStormProps.put("storm.zookeeper.superACL", "sasl:{{storm_bare_jaas_principal}}");
        updateConfigurationPropertiesForCluster(cluster, "storm-site", newStormProps, true, false);
      }
    }
  }

  protected void updateAlerts() {
    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterID = cluster.getClusterId();

      final AlertDefinitionEntity regionserverHealthSummaryDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "regionservers_health_summary");

      final AlertDefinitionEntity atsWebAlert = alertDefinitionDAO.findByName(
              clusterID, "yarn_app_timeline_server_webui");

      if (regionserverHealthSummaryDefinitionEntity != null) {
        alertDefinitionDAO.remove(regionserverHealthSummaryDefinitionEntity);
      }

      if (atsWebAlert != null) {
        String source = atsWebAlert.getSource();
        JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();

        JsonObject uriJson = sourceJson.get("uri").getAsJsonObject();
        uriJson.remove("http");
        uriJson.remove("https");
        uriJson.addProperty("http", "{{yarn-site/yarn.timeline-service.webapp.address}}/ws/v1/timeline");
        uriJson.addProperty("https", "{{yarn-site/yarn.timeline-service.webapp.https.address}}/ws/v1/timeline");

        atsWebAlert.setSource(sourceJson.toString());
        alertDefinitionDAO.merge(atsWebAlert);
      }

      //update Atlas alert
      final AlertDefinitionEntity atlasMetadataServerWebUI = alertDefinitionDAO.findByName(
              clusterID, "metadata_server_webui");
      if (atlasMetadataServerWebUI != null) {
        String source = atlasMetadataServerWebUI.getSource();
        JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();

        JsonObject uriJson = sourceJson.get("uri").getAsJsonObject();
        uriJson.remove("http");
        uriJson.remove("https");
        uriJson.addProperty("http", "{{application-properties/atlas.server.http.port}}");
        uriJson.addProperty("https", "{{application-properties/atlas.server.https.port}}");

        atlasMetadataServerWebUI.setSource(sourceJson.toString());
        alertDefinitionDAO.merge(atlasMetadataServerWebUI);
      }

    }


  }

  protected void updateHostRoleCommands() throws SQLException {
    dbAccessor.createIndex("idx_hrc_status", "host_role_command", "status", "role");
  }

  protected void updateAMSConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {

          Config amsSite = cluster.getDesiredConfigByType(AMS_SITE);
          if (amsSite != null) {
            Map<String, String> amsSiteProperties = amsSite.getProperties();
            Map<String, String> newProperties = new HashMap<>();

            if (amsSiteProperties.containsKey(AMS_WEBAPP_ADDRESS_PROPERTY)) {
              Set<String> collectorHostNames = cluster.getHosts(AMS_SERVICE_NAME, AMS_COLLECTOR_COMPONENT_NAME);
              for (String collector: collectorHostNames) {
                String currentValue = amsSiteProperties.get(AMS_WEBAPP_ADDRESS_PROPERTY);

                if (currentValue.startsWith("0.0.0.0")) {
                  newProperties.put(AMS_WEBAPP_ADDRESS_PROPERTY, currentValue.replace("0.0.0.0", collector));
                } else if (currentValue.startsWith("localhost")) {
                  newProperties.put(AMS_WEBAPP_ADDRESS_PROPERTY, currentValue.replace("localhost", collector));
                }
              }
            }

            if (amsSiteProperties.containsKey(HOST_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY) &&
              amsSiteProperties.get(HOST_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY).equals("1")) {

              LOG.info("Setting value of " + HOST_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY + " : 2");
              newProperties.put(HOST_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY, String.valueOf(2));

            }

            if (amsSiteProperties.containsKey(CLUSTER_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY) &&
              amsSiteProperties.get(CLUSTER_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY).equals("1")) {

              LOG.info("Setting value of " + CLUSTER_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY + " : 2");
              newProperties.put(CLUSTER_AGGREGATOR_DAILY_CHECKPOINTCUTOFFMULTIPIER_PROPERTY, String.valueOf(2));

            }

            if (!amsSiteProperties.containsKey(TIMELINE_METRICS_SERVICE_WATCHER_DISBALED_PROPERTY)) {
              LOG.info("Add config  " + TIMELINE_METRICS_SERVICE_WATCHER_DISBALED_PROPERTY + " = false");
              newProperties.put(TIMELINE_METRICS_SERVICE_WATCHER_DISBALED_PROPERTY, String.valueOf(false));
            }

            boolean isDistributed = false;
            if ("distributed".equals(amsSite.getProperties().get(AMS_MODE_PROPERTY))) {
              isDistributed = true;
            }

            if (amsSiteProperties.containsKey(PRECISION_TABLE_TTL_PROPERTY)) {
              String oldTtl = amsSiteProperties.get(PRECISION_TABLE_TTL_PROPERTY);
              String newTtl = oldTtl;
              if (isDistributed) {
                if ("86400".equals(oldTtl)) {
                  newTtl = String.valueOf(3 * 86400); // 3 days
                }
              }
              newProperties.put(PRECISION_TABLE_TTL_PROPERTY, newTtl);
              LOG.info("Setting value of " + PRECISION_TABLE_TTL_PROPERTY + " : " + newTtl);
            }

            if (amsSiteProperties.containsKey(CLUSTER_SECOND_TABLE_TTL_PROPERTY)) {
              String oldTtl = amsSiteProperties.get(CLUSTER_SECOND_TABLE_TTL_PROPERTY);
              String newTtl = oldTtl;

              if ("2592000".equals(oldTtl)) {
                newTtl = String.valueOf(7 * 86400); // 7 days
              }

              newProperties.put(CLUSTER_SECOND_TABLE_TTL_PROPERTY, newTtl);
              LOG.info("Setting value of " + CLUSTER_SECOND_TABLE_TTL_PROPERTY + " : " + newTtl);
            }

            if (amsSiteProperties.containsKey(CLUSTER_MINUTE_TABLE_TTL_PROPERTY)) {
              String oldTtl = amsSiteProperties.get(CLUSTER_MINUTE_TABLE_TTL_PROPERTY);
              String newTtl = oldTtl;

              if ("7776000".equals(oldTtl)) {
                newTtl = String.valueOf(30 * 86400); // 30 days
              }

              newProperties.put(CLUSTER_MINUTE_TABLE_TTL_PROPERTY, newTtl);
              LOG.info("Setting value of " + CLUSTER_MINUTE_TABLE_TTL_PROPERTY + " : " + newTtl);
            }

            if (!amsSiteProperties.containsKey(TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED)) {
              LOG.info("Add config  " + TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED + " = true");
              newProperties.put(TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED, String.valueOf(true));
            }

            updateConfigurationPropertiesForCluster(cluster, AMS_SITE, newProperties, true, true);
          }

          Config amsHbaseSite = cluster.getDesiredConfigByType(AMS_HBASE_SITE);
          if (amsHbaseSite != null) {
            Map<String, String> amsHbaseSiteProperties = amsHbaseSite.getProperties();
            Map<String, String> newProperties = new HashMap<>();

            if (!amsHbaseSiteProperties.containsKey(HBASE_RPC_TIMEOUT_PROPERTY)) {
              newProperties.put(HBASE_RPC_TIMEOUT_PROPERTY, String.valueOf(300000));
            }

            if (!amsHbaseSiteProperties.containsKey(PHOENIX_QUERY_KEEPALIVE_PROPERTY)) {
              newProperties.put(PHOENIX_QUERY_KEEPALIVE_PROPERTY, String.valueOf(300000));
            }

            if (!amsHbaseSiteProperties.containsKey(HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD_PROPERTY) ||
              amsHbaseSiteProperties.get(HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD_PROPERTY).equals("900000")) {
              amsHbaseSiteProperties.put(HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD_PROPERTY, String.valueOf(300000));
            }

            if (!amsHbaseSiteProperties.containsKey(PHOENIX_QUERY_TIMEOUT_PROPERTY) ||
              amsHbaseSiteProperties.get(PHOENIX_QUERY_TIMEOUT_PROPERTY).equals("1200000")) {
              amsHbaseSiteProperties.put(PHOENIX_QUERY_TIMEOUT_PROPERTY, String.valueOf(300000));
            }
            updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_SITE, newProperties, true, true);
          }
        }
      }
    }
  }

  protected void updateHDFSWidgetDefinition() throws AmbariException {
    LOG.info("Updating HDFS widget definition.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    Type widgetLayoutType = new TypeToken<Map<String, List<WidgetLayout>>>(){}.getType();
    Gson gson = injector.getInstance(Gson.class);
    WidgetDAO widgetDAO = injector.getInstance(WidgetDAO.class);

    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterID = cluster.getClusterId();

      StackId stackId = cluster.getDesiredStackVersion();
      Map<String, Object> widgetDescriptor = null;
      StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
      ServiceInfo serviceInfo = stackInfo.getService("HDFS");
      if (serviceInfo == null) {
        LOG.info("Skipping updating HDFS widget definition, because HDFS service is not present in cluster " +
          "cluster_name= " + cluster.getClusterName());
        continue;
      }

      for (String widgetName : HDFS_WIDGETS_TO_UPDATE) {
        List<WidgetEntity> widgetEntities = widgetDAO.findByName(clusterID,
          widgetName, "ambari", "HDFS_SUMMARY");

        if (widgetEntities != null && widgetEntities.size() > 0) {
          WidgetEntity entityToUpdate = null;
          if (widgetEntities.size() > 1) {
            LOG.info("Found more that 1 entity with name = "+ widgetName +
              " for cluster = " + cluster.getClusterName() + ", skipping update.");
          } else {
            entityToUpdate = widgetEntities.iterator().next();
          }
          if (entityToUpdate != null) {
            LOG.info("Updating widget: " + entityToUpdate.getWidgetName());
            // Get the definition from widgets.json file
            WidgetLayoutInfo targetWidgetLayoutInfo = null;
            File widgetDescriptorFile = serviceInfo.getWidgetsDescriptorFile();
            if (widgetDescriptorFile != null && widgetDescriptorFile.exists()) {
              try {
                widgetDescriptor = gson.fromJson(new FileReader(widgetDescriptorFile), widgetLayoutType);
              } catch (Exception ex) {
                String msg = "Error loading widgets from file: " + widgetDescriptorFile;
                LOG.error(msg, ex);
                widgetDescriptor = null;
              }
            }
            if (widgetDescriptor != null) {
              LOG.debug("Loaded widget descriptor: " + widgetDescriptor);
              for (Object artifact : widgetDescriptor.values()) {
                List<WidgetLayout> widgetLayouts = (List<WidgetLayout>) artifact;
                for (WidgetLayout widgetLayout : widgetLayouts) {
                  if (widgetLayout.getLayoutName().equals("default_hdfs_dashboard")) {
                    for (WidgetLayoutInfo layoutInfo : widgetLayout.getWidgetLayoutInfoList()) {
                      if (layoutInfo.getWidgetName().equals(widgetName)) {
                        targetWidgetLayoutInfo = layoutInfo;
                      }
                    }
                  }
                }
              }
            }
            if (targetWidgetLayoutInfo != null) {
              entityToUpdate.setMetrics(gson.toJson(targetWidgetLayoutInfo.getMetricsInfo()));
              entityToUpdate.setWidgetValues(gson.toJson(targetWidgetLayoutInfo.getValues()));
              widgetDAO.merge(entityToUpdate);
            } else {
              LOG.warn("Unable to find widget layout info for " + widgetName +
                " in the stack: " + stackId);
            }
          }
        }
      }
    }
  }

  protected void updateHiveConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config hiveSiteConfig = cluster.getDesiredConfigByType(HIVE_SITE_CONFIG);
      Config atlasConfig = cluster.getDesiredConfigByType(ATLAS_APPLICATION_PROPERTIES_CONFIG);

      StackId stackId = cluster.getCurrentStackVersion();
      boolean isStackNotLess23 = (stackId != null && stackId.getStackName().equals("HDP") &&
        VersionUtils.compareVersions(stackId.getStackVersion(), "2.3") >= 0);

      List<ServiceComponentHost> atlasHost = cluster.getServiceComponentHosts("ATLAS", "ATLAS_SERVER");
      Map<String, String> updates = new HashMap<String, String>();

      if (isStackNotLess23 && atlasHost.size() != 0 && hiveSiteConfig != null) {

        updates.put(ATLAS_HOOK_HIVE_MINTHREADS_PROPERTY, "1");
        updates.put(ATLAS_HOOK_HIVE_MAXTHREADS_PROPERTY, "1");
        updates.put(ATLAS_CLUSTER_NAME_PROPERTY, "primary");

        if (atlasConfig != null && atlasConfig.getProperties().containsKey(ATLAS_ENABLETLS_PROPERTY)) {
          String atlasEnableTLSProperty = atlasConfig.getProperties().get(ATLAS_ENABLETLS_PROPERTY);
          String atlasScheme = "http";
          String atlasServerHttpPortProperty = atlasConfig.getProperties().get(ATLAS_SERVER_HTTP_PORT_PROPERTY);
          if (atlasEnableTLSProperty.toLowerCase().equals("true")) {
            atlasServerHttpPortProperty = atlasConfig.getProperties().get(ATLAS_SERVER_HTTPS_PORT_PROPERTY);
            atlasScheme = "https";
          }
          updates.put(ATLAS_REST_ADDRESS_PROPERTY, String.format("%s://%s:%s", atlasScheme, atlasHost.get(0).getHostName(), atlasServerHttpPortProperty));
        }
        updateConfigurationPropertiesForCluster(cluster, HIVE_SITE_CONFIG, updates, false, false);
      }
    }
  }

  protected void updateCorruptedReplicaWidget() throws SQLException {
    String widgetValues = String.format("[{\"name\": \"%s\", \"value\": \"%s\"}]",
      WIDGET_CORRUPT_REPLICAS, WIDGET_VALUES_VALUE);
    String updateStatement = "UPDATE %s SET %s='%s', %s='%s', %s='%s' WHERE %s='%s'";

    LOG.info("Update widget definition for HDFS corrupted blocks metric");
    dbAccessor.executeUpdate(String.format(updateStatement,
      WIDGET_TABLE,
      WIDGET_NAME, WIDGET_CORRUPT_REPLICAS,
      WIDGET_DESCRIPTION, WIDGET_CORRUPT_REPLICAS_DESCRIPTION,
      WIDGET_VALUES, widgetValues,
      WIDGET_NAME, WIDGET_CORRUPT_BLOCKS
    ));
  }

  /**
   * Updates the {@value #UPGRADE_TABLE} in the following ways:
   * <ul>
   * <li>{value {@link #UPGRADE_SUSPENDED_COLUMN} is added</li>
   * </ul>
   *
   * @throws AmbariException
   * @throws SQLException
   */
  protected void updateUpgradeTable() throws AmbariException, SQLException {
    dbAccessor.addColumn(UPGRADE_TABLE,
      new DBAccessor.DBColumnInfo(UPGRADE_SUSPENDED_COLUMN, Short.class, 1, 0, false));
  }

}
