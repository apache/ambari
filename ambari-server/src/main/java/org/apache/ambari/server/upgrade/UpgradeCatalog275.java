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

import static org.apache.ambari.server.utils.CustomStringUtils.deleteSubstring;
import static org.apache.ambari.server.utils.CustomStringUtils.insertAfterIfNotThere;
import static org.apache.ambari.server.utils.CustomStringUtils.replace;
import static org.apache.ambari.server.utils.CustomStringUtils.replaceIfNotThere;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.commons.collections.MapUtils;
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
    updateAmsGrafanaIniConfig();
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

  protected void updateAmsGrafanaIniConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      if (MapUtils.isNotEmpty(clusterMap)) {
        for (Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          if (installedServices.contains("AMBARI_METRICS")) {
            Config amsGrafanaIniConf = cluster.getDesiredConfigByType("ams-grafana-ini");
            if (amsGrafanaIniConf != null) {
              String contentText = amsGrafanaIniConf.getProperties().get("content");
              if (contentText != null) {
                String addAfter;
                String toInsert;
                String toFind;
                String toReplace;
                StringBuilder content = new StringBuilder(contentText);

                addAfter = "; app_mode = production";
                toInsert = "\n" +
                    "\n# instance name, defaults to HOSTNAME environment variable value or hostname if HOSTNAME var is empty" +
                    "\n; instance_name = ${HOSTNAME}";
                insertAfterIfNotThere(content, addAfter, toInsert);

                addAfter = "logs = {{ams_grafana_log_dir}}";
                String pluginsConfLine = "plugins = /var/lib/ambari-metrics-grafana/plugins";
                toInsert = "\n" +
                    "\n# Directory where grafana will automatically scan and look for plugins" +
                    "\n" + pluginsConfLine;
                insertAfterIfNotThere(content, addAfter, toInsert, pluginsConfLine);

                deleteSubstring(content, ";protocol = http\n");
                deleteSubstring(content, ";http_port = 3000\n");
                deleteSubstring(content, ";static_root_path = public\n");
                deleteSubstring(content, ";cert_file =\n");
                deleteSubstring(content, ";cert_key =\n");

                addAfter = "cert_key = {{ams_grafana_cert_key}}";
                toInsert = "\n" +
                    "\n# Unix socket path" +
                    "\n;socket =";
                insertAfterIfNotThere(content, addAfter, toInsert);

                toFind = ";password =";
                toReplace = "# If the password contains # or ; you have to wrap it with triple quotes. Ex \"\"\"#password;\"\"\"" +
                    "\n;password =" +
                    "\n" +
                    "\n# Use either URL or the previous fields to configure the database" +
                    "\n# Example: mysql://user:secret@host:port/database" +
                    "\n;url =";
                replaceIfNotThere(content, toFind, toReplace);

                addAfter = ";session_life_time = 86400";
                toInsert = "\n" +
                    "\n#################################### Data proxy ###########################" +
                    "\n[dataproxy]" +
                    "\n" +
                    "\n# This enables data proxy logging, default is false" +
                    "\n;logging = false";
                insertAfterIfNotThere(content, addAfter, toInsert);

                toFind = "# Google Analytics universal tracking code, only enabled if you specify an id here";
                toReplace = "# Set to false to disable all checks to https://grafana.net" +
                    "\n# for new versions (grafana itself and plugins), check is used" +
                    "\n# in some UI views to notify that grafana or plugin update exists" +
                    "\n# This option does not cause any auto updates, nor send any information" +
                    "\n# only a GET request to http://grafana.com to get latest versions" +
                    "\n;check_for_updates = true" +
                    "\n" +
                    "\n# Google Analytics universal tracking code, only enabled if you specify an id here";
                replaceIfNotThere(content, toFind, toReplace);

                toFind = "#################################### Users ####################################";
                toReplace = "[snapshots]" +
                    "\n# snapshot sharing options" +
                    "\n;external_enabled = true" +
                    "\n;external_snapshot_url = https://snapshots-origin.raintank.io" +
                    "\n;external_snapshot_name = Publish to snapshot.raintank.io" +
                    "\n" +
                    "\n# remove expired snapshot" +
                    "\n;snapshot_remove_expired = true" +
                    "\n" +
                    "\n# remove snapshots after 90 days" +
                    "\n;snapshot_TTL_days = 90" +
                    "\n" +
                    "\n#################################### Users ####################################";
                replaceIfNotThere(content, toFind, toReplace);

                toFind = "#################################### Anonymous Auth ##########################";
                toReplace = "# Default UI theme (\"dark\" or \"light\")" +
                    "\n;default_theme = dark" +
                    "\n" +
                    "\n# External user management, these options affect the organization users view" +
                    "\n;external_manage_link_url =" +
                    "\n;external_manage_link_name =" +
                    "\n;external_manage_info =" +
                    "\n" +
                    "\n[auth]" +
                    "\n# Set to true to disable (hide) the login form, useful if you use OAuth, defaults to false" +
                    "\n;disable_login_form = false" +
                    "\n" +
                    "\n# Set to true to disable the sign out link in the side menu. useful if you use auth.proxy, defaults to false" +
                    "\n;disable_signout_menu = false" +
                    "\n" +
                    "\n#################################### Anonymous Auth ##########################";
                replaceIfNotThere(content, toFind, toReplace);

                toFind = "#################################### Auth Proxy ##########################";
                toReplace = "#################################### Generic OAuth ##########################" +
                    "\n[auth.generic_oauth]" +
                    "\n;enabled = false" +
                    "\n;name = OAuth" +
                    "\n;allow_sign_up = true" +
                    "\n;client_id = some_id" +
                    "\n;client_secret = some_secret" +
                    "\n;scopes = user:email,read:org" +
                    "\n;auth_url = https://foo.bar/login/oauth/authorize" +
                    "\n;token_url = https://foo.bar/login/oauth/access_token" +
                    "\n;api_url = https://foo.bar/user" +
                    "\n;team_ids =" +
                    "\n;allowed_organizations =" +
                    "\n" +
                    "\n#################################### Grafana.com Auth ####################" +
                    "\n[auth.grafana_com]" +
                    "\n;enabled = false" +
                    "\n;allow_sign_up = true" +
                    "\n;client_id = some_id" +
                    "\n;client_secret = some_secret" +
                    "\n;scopes = user:email" +
                    "\n;allowed_organizations =" +
                    "\n" +
                    "\n#################################### Auth Proxy ##########################";
                replace(content, toFind, toReplace);

                toFind = "[emails]";
                toReplace = ";from_name = Grafana" +
                    "\n# EHLO identity in SMTP dialog (defaults to instance_name)" +
                    "\n;ehlo_identity = dashboard.example.com" +
                    "\n" +
                    "\n[emails]";
                replaceIfNotThere(content, toFind, toReplace);

                toFind = "# Either \"Trace\", \"Debug\", \"Info\", \"Warn\", \"Error\", \"Critical\", default is \"Trace\"";
                toReplace = "# Either \"debug\", \"info\", \"warn\", \"error\", \"critical\", default is\"info\"";
                replaceIfNotThere(content, toFind, toReplace);

                toFind = ";level = Info";
                toReplace = ";level = info";
                replaceIfNotThere(content, toFind, toReplace);

                toFind = "# Buffer length of channel, keep it as it is if you don't know what it is." +
                    "\n;buffer_len = 10000";
                toReplace = "# optional settings to set different levels for specific loggers. Ex filters = sqlstore:debug" +
                    "\n;filters =";
                replaceIfNotThere(content, toFind, toReplace);

                Map<String, String> newProperties = new HashMap<>(1);
                newProperties.put("content", content.toString());
                updateConfigurationPropertiesForCluster(cluster, "ams-grafana-ini", newProperties, true, false);
              }
            }
          }
        }
      }
    }
  }
}
