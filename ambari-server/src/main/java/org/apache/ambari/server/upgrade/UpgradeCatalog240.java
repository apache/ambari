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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Upgrade catalog for version 2.4.0.
 */
public class UpgradeCatalog240 extends AbstractUpgradeCatalog {

  @Inject
  DaoUtils daoUtils;

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog240.class);




  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog240(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.4.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.3.0";
  }


  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    updateAlerts();

  }

  protected void updateAlerts() {
    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterID = cluster.getClusterId();

      final AlertDefinitionEntity namenodeLastCheckpointAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "namenode_last_checkpoint");
      final AlertDefinitionEntity namenodeHAHealthAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "namenode_ha_health");
      final AlertDefinitionEntity nodemanagerHealthAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "yarn_nodemanager_health");
      final AlertDefinitionEntity nodemanagerHealthSummaryAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "nodemanager_health_summary");
      final AlertDefinitionEntity hiveMetastoreProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_metastore_process");
      final AlertDefinitionEntity hiveServerProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_server_process");
      final AlertDefinitionEntity hiveWebhcatServerStatusAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "hive_webhcat_server_status");
      final AlertDefinitionEntity flumeAgentStatusAlertDefinitionEntity = alertDefinitionDAO.findByName(
              clusterID, "flume_agent_status");

      Map<AlertDefinitionEntity, List<String>> alertDefinitionParams = new HashMap<>();
      checkedPutToMap(alertDefinitionParams, namenodeLastCheckpointAlertDefinitionEntity,
              new ArrayList<String>(Arrays.asList("connection.timeout", "checkpoint.time.warning.threshold", "checkpoint.time.critical.threshold")));
      checkedPutToMap(alertDefinitionParams, namenodeHAHealthAlertDefinitionEntity,
              new ArrayList<String>(Arrays.asList("connection.timeout")));
      checkedPutToMap(alertDefinitionParams, nodemanagerHealthAlertDefinitionEntity,
              new ArrayList<String>(Arrays.asList("connection.timeout")));
      checkedPutToMap(alertDefinitionParams, nodemanagerHealthSummaryAlertDefinitionEntity,
              new ArrayList<String>(Arrays.asList("connection.timeout")));
      checkedPutToMap(alertDefinitionParams, hiveMetastoreProcessAlertDefinitionEntity,
              new ArrayList<String>(Arrays.asList("default.smoke.user", "default.smoke.principal", "default.smoke.keytab")));
      checkedPutToMap(alertDefinitionParams, hiveServerProcessAlertDefinitionEntity,
              new ArrayList<String>(Arrays.asList("default.smoke.user", "default.smoke.principal", "default.smoke.keytab")));
      checkedPutToMap(alertDefinitionParams, hiveWebhcatServerStatusAlertDefinitionEntity,
              new ArrayList<String>(Arrays.asList("default.smoke.user", "connection.timeout")));
      checkedPutToMap(alertDefinitionParams, flumeAgentStatusAlertDefinitionEntity,
              new ArrayList<String>(Arrays.asList("run.directory")));

      for(Map.Entry<AlertDefinitionEntity, List<String>> entry : alertDefinitionParams.entrySet()){
        AlertDefinitionEntity alertDefinition = entry.getKey();
        String source = alertDefinition.getSource();

        alertDefinition.setSource(addParam(source, entry.getValue()));
        alertDefinition.setHash(UUID.randomUUID().toString());

        alertDefinitionDAO.merge(alertDefinition);
      }

    }
  }

  /*
  * Simple put method with check for key is not null
  * */
  private void checkedPutToMap(Map<AlertDefinitionEntity, List<String>> alertDefinitionParams, AlertDefinitionEntity alertDefinitionEntity,
                               List<String> params) {
    if (alertDefinitionEntity != null) {
      alertDefinitionParams.put(alertDefinitionEntity, params);
    }
  }

  protected String addParam(String source, List<String> params) {
    JsonObject sourceJson = new JsonParser().parse(source).getAsJsonObject();
    JsonArray parametersJson = sourceJson.getAsJsonArray("parameters");

    boolean parameterExists = parametersJson != null && !parametersJson.isJsonNull();

    if (parameterExists) {
      Iterator<JsonElement> jsonElementIterator = parametersJson.iterator();
      while(jsonElementIterator.hasNext()) {
        JsonElement element = jsonElementIterator.next();
        JsonElement name = element.getAsJsonObject().get("name");
        if (name != null && !name.isJsonNull() && params.contains(name.getAsString())) {
          params.remove(name.getAsString());
        }
      }
      if (params.size() == 0) {
        return sourceJson.toString();
      }
    }

    List<JsonObject> paramsToAdd = new ArrayList<>();

    if (params.contains("connection.timeout")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("connection.timeout"));
      param.add("display_name", new JsonPrimitive("Connection Timeout"));
      param.add("value", new JsonPrimitive(5.0));
      param.add("type", new JsonPrimitive("NUMERIC"));
      param.add("description", new JsonPrimitive("The maximum time before this alert is considered to be CRITICAL"));
      param.add("units", new JsonPrimitive("seconds"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));

      paramsToAdd.add(param);

    }
    if (params.contains("checkpoint.time.warning.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("checkpoint.time.warning.threshold"));
      param.add("display_name", new JsonPrimitive("Checkpoint Warning"));
      param.add("value", new JsonPrimitive(2.0));
      param.add("type", new JsonPrimitive("PERCENT"));
      param.add("description", new JsonPrimitive("The percentage of the last checkpoint time greater than the interval in order to trigger a warning alert."));
      param.add("units", new JsonPrimitive("%"));
      param.add("threshold", new JsonPrimitive("WARNING"));

      paramsToAdd.add(param);

    }
    if (params.contains("checkpoint.time.critical.threshold")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("checkpoint.time.critical.threshold"));
      param.add("display_name", new JsonPrimitive("Checkpoint Critical"));
      param.add("value", new JsonPrimitive(2.0));
      param.add("type", new JsonPrimitive("PERCENT"));
      param.add("description", new JsonPrimitive("The percentage of the last checkpoint time greater than the interval in order to trigger a critical alert."));
      param.add("units", new JsonPrimitive("%"));
      param.add("threshold", new JsonPrimitive("CRITICAL"));

      paramsToAdd.add(param);

    }
    if (params.contains("default.smoke.user")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.user"));
      param.add("display_name", new JsonPrimitive("Default Smoke User"));
      param.add("value", new JsonPrimitive("ambari-qa"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The user that will run the Hive commands if not specified in cluster-env/smokeuser"));

      paramsToAdd.add(param);

    }
    if (params.contains("default.smoke.principal")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.principal"));
      param.add("display_name", new JsonPrimitive("Default Smoke Principal"));
      param.add("value", new JsonPrimitive("ambari-qa@EXAMPLE.COM"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The principal to use when retrieving the kerberos ticket if not specified in cluster-env/smokeuser_principal_name"));

      paramsToAdd.add(param);

    }
    if (params.contains("default.smoke.keytab")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("default.smoke.keytab"));
      param.add("display_name", new JsonPrimitive("Default Smoke Keytab"));
      param.add("value", new JsonPrimitive("/etc/security/keytabs/smokeuser.headless.keytab"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The keytab to use when retrieving the kerberos ticket if not specified in cluster-env/smokeuser_keytab"));

      paramsToAdd.add(param);

    }
    if (params.contains("run.directory")) {
      JsonObject param = new JsonObject();
      param.add("name", new JsonPrimitive("run.directory"));
      param.add("display_name", new JsonPrimitive("Run Directory"));
      param.add("value", new JsonPrimitive("/var/run/flume"));
      param.add("type", new JsonPrimitive("STRING"));
      param.add("description", new JsonPrimitive("The directory where flume agent processes will place their PID files."));

      paramsToAdd.add(param);

    }


    if (!parameterExists) {
      parametersJson = new JsonArray();
      for (JsonObject param : paramsToAdd) {
        parametersJson.add(param);
      }
      sourceJson.add("parameters", parametersJson);
    } else {
      for (JsonObject param : paramsToAdd) {
        parametersJson.add(param);
      }
      sourceJson.remove("parameters");
      sourceJson.add("parameters", parametersJson);
    }

    return sourceJson.toString();
  }



}
