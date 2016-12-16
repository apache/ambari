/**
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

package org.apache.ambari.server.serveraction.upgrades;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.events.AlertHashInvalidationEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;

/**
 * This class is used to update Ranger service alert-check configs in Ambari
 */
public class RangerWebAlertConfigAction extends AbstractServerAction {


  @Inject
  Clusters m_clusters;

  @Inject
  AlertDefinitionDAO alertDefinitionDAO;

  @Inject
  AlertDefinitionHash alertDefinitionHash;

  @Inject
  AmbariEventPublisher eventPublisher;


  private Logger logger = LoggerFactory.getLogger(RangerWebAlertConfigAction.class);


  /**
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return
   * @throws AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = m_clusters.getCluster(clusterName);

    String ranger_admin_process = "ranger_admin_process";

    try {

      AlertDefinitionEntity rangerAlertDefinitionEntity = alertDefinitionDAO.findByName(cluster.getClusterId(), ranger_admin_process);
      if (rangerAlertDefinitionEntity != null) {

        logger.info("Updating the alert definition for ranger_admin_process.");

        String rangerAlertCurrentDefinitionSource = rangerAlertDefinitionEntity.getSource();
        JsonObject rangerAlertsCurrentDefinitionJSON = new JsonParser().parse(rangerAlertCurrentDefinitionSource).getAsJsonObject();

        //updating the alert definition
        rangerAlertsCurrentDefinitionJSON.remove("uri");

        JsonObject rangerUpdatedURLConfig = new JsonObject();
        rangerUpdatedURLConfig.addProperty("http", "{{admin-properties/policymgr_external_url}}/login.jsp");
        rangerUpdatedURLConfig.addProperty("https", "{{admin-properties/policymgr_external_url}}/login.jsp");
        rangerUpdatedURLConfig.addProperty("kerberos_keytab", "{{cluster-env/smokeuser_keytab}}");
        rangerUpdatedURLConfig.addProperty("kerberos_principal", "{{cluster-env/smokeuser_principal_name}}");
        rangerUpdatedURLConfig.addProperty("https_property", "{{ranger-admin-site/ranger.service.https.attrib.ssl.enabled}}");
        rangerUpdatedURLConfig.addProperty("https_property_value", "true");
        rangerUpdatedURLConfig.addProperty("connection_timeout", 5.0);

        rangerAlertsCurrentDefinitionJSON.add("uri", rangerUpdatedURLConfig);

        rangerAlertDefinitionEntity.setHash(UUID.randomUUID().toString());
        rangerAlertDefinitionEntity.setSource(rangerAlertsCurrentDefinitionJSON.toString());

        alertDefinitionDAO.merge(rangerAlertDefinitionEntity);

        // invalidating alert and publishing the updated definition
        Set<String> invalidatedHosts = alertDefinitionHash.invalidateHosts(rangerAlertDefinitionEntity);
        AlertHashInvalidationEvent alertInvalidationEvent = new AlertHashInvalidationEvent(
            rangerAlertDefinitionEntity.getClusterId(), invalidatedHosts);

        eventPublisher.publish(alertInvalidationEvent);

        return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
            "Ranger service alert check configuration has been updated successfully.", "");

      } else {
        return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
            String.format(
                "The %s configuration type was not found; unable to update Ranger Alert properties.",
                ranger_admin_process), "");
      }
    } catch (Exception e) {
      logger.error("RangerWebAlertConfigAction.execute : There was an error in updating Ranger alerts.", e);
    }
    return null;
  }

}
