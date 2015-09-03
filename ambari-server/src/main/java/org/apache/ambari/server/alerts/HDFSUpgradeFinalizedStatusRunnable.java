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
package org.apache.ambari.server.alerts;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.services.AmbariServerAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@link HDFSUpgradeFinalizedStatusRunnable} is used by the
 * {@link AmbariServerAlertService} to check hdfs finalized status
 */
public class HDFSUpgradeFinalizedStatusRunnable implements Runnable
{
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AgentHeartbeatAlertRunnable.class);
  /**
   * String constants
   */
  private final static String s_serviceName = "HDFS";
  private final static String s_serviceComponent = "NAMENODE";
  private final static String s_upgradeFinalizedMetricProperty = "UpgradeFinalized";
  private final static String s_alertText = "HDFS cluster is not in the upgrade state";
  private final static String s_alertCriticalText = "HDFS cluster is not finalized";
  private final static String s_alertUnknownText = "HDFS cluster is in the unknown state";
  /**
   * The unique name for the alert definition that governs this service.
   */
  private static final String ALERT_DEFINITION_NAME = "ambari_upgrade_finalized_state";
  /**
   * Used for looking up alert definitions.
   */
  @Inject
  private AlertDefinitionDAO m_dao;
  /**
   * Used to get alert definitions to use when generating alert instances.
   */
  @Inject
  private Provider<Clusters> m_clustersProvider;
  /**
   * Publishes {@link AlertEvent} instances.
   */
  @Inject
  private AlertEventPublisher m_alertEventPublisher;

  /**
   * Represent possibly HDFS upgrade finalization states
   */
  private enum HDFSFinalizationState {
    /**
     * HDFS is not in the upgrade state
     */
    FINALIZED,
    /**
     * HDFS currently in the upgrade state
     */
    NOT_FINALIZED,
    /**
     * Undetermined stated, probably service is turned off and no metric available
     */
    UNKNOWN
  }

  public HDFSUpgradeFinalizedStatusRunnable(){
  }

  @Override
  public void run(){
    try {
      Map<String, Cluster> clusterMap = m_clustersProvider.get().getClusters();

      for (Cluster cluster : clusterMap.values()) {
        AlertDefinitionEntity entity = m_dao.findByName(cluster.getClusterId(), ALERT_DEFINITION_NAME);

        // skip this cluster if the runnable's alert definition is missing or disabled
        if (null == entity || !entity.getEnabled()) {
          continue;
        }

        // check if the service existed
        try {
          cluster.getService(s_serviceName);
        } catch (ServiceNotFoundException e){
          continue;
        }

        Date current = new Date(System.currentTimeMillis());
        HDFSFinalizationState upgradeFinalized = getUpgradeFinalizedProperty(cluster);

        AlertState alertState;
        String alertDescription;

        if (upgradeFinalized == HDFSFinalizationState.UNKNOWN) {
          alertState = AlertState.UNKNOWN;
          alertDescription = s_alertUnknownText;
        } else if (upgradeFinalized == HDFSFinalizationState.FINALIZED){
          alertState = AlertState.OK;
          alertDescription = s_alertText;
        } else {
          alertState = AlertState.CRITICAL;
          alertDescription = s_alertCriticalText;
        }

        Alert alert = new Alert(entity.getDefinitionName(), null,
          entity.getServiceName(), entity.getComponentName(), null, alertState);

        alert.setLabel(entity.getLabel());
        alert.setText(alertDescription);
        alert.setTimestamp(current.getTime());
        alert.setCluster(cluster.getClusterName());
        AlertReceivedEvent event = new AlertReceivedEvent(cluster.getClusterId(), alert);
        m_alertEventPublisher.publish(event);
      }
    } catch (Exception e){
      LOG.error("Unable to run the {} alert", ALERT_DEFINITION_NAME, e);
    }
  }

  /**
   * Query {@link ComponentResourceProvider} for the HDFS finalization status
   * @param cluster the cluster for the query
   * @return HDFS finalization status flag
   * @throws AmbariException
   */
  private HDFSFinalizationState getUpgradeFinalizedProperty(Cluster cluster) throws AmbariException{
  try
    {
      ComponentResourceProvider crp = (ComponentResourceProvider) ClusterControllerHelper
        .getClusterController().ensureResourceProvider(Resource.Type.Component);

      Set<String> properties = new HashSet<String>();
      properties.add("ServiceComponentInfo/" + s_upgradeFinalizedMetricProperty);
      Request request = PropertyHelper.getReadRequest(properties);
      PredicateBuilder pb = new PredicateBuilder();

      Predicate predicate =  pb.begin()
        .property("ServiceComponentInfo/service_name").equals(s_serviceName)
        .and()
        .property("ServiceComponentInfo/component_name").equals(s_serviceComponent)
        .and()
        .property("ServiceComponentInfo/cluster_name").equals(cluster.getClusterName())
        .end().toPredicate();

      Set<Resource> res =  ClusterControllerHelper.getClusterController().populateResources(
                   Resource.Type.Component,
                   crp.getResources(request, predicate),
                   request,
                   predicate);

      for (Resource rr: res){
        for (Map<String, Object> t: rr.getPropertiesMap().values()){
          if (t.containsKey(s_upgradeFinalizedMetricProperty) &&
              t.get(s_upgradeFinalizedMetricProperty) instanceof Boolean) {

            if ((Boolean)t.get(s_upgradeFinalizedMetricProperty)){
              return HDFSFinalizationState.FINALIZED;
            } else {
              return HDFSFinalizationState.NOT_FINALIZED;
            }
          }
        }
      }
    } catch (SystemException|UnsupportedPropertyException|NoSuchParentResourceException|NoSuchResourceException e) {
      LOG.error("Unable to run the {} alert", ALERT_DEFINITION_NAME, e);
    }
    // no metric available
    return HDFSFinalizationState.UNKNOWN;
  }
}
