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

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.services.AmbariServerAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The {@link AgentHeartbeatAlertRunnable} is used by the
 * {@link AmbariServerAlertService} to check agent heartbeats and fire alert
 * events when agents are not reachable.
 */
public class AgentHeartbeatAlertRunnable implements Runnable {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AgentHeartbeatAlertRunnable.class);

  /**
   * The unique name for the alert definition that governs this service.
   */
  private static final String HEARTBEAT_DEFINITION_NAME = "ambari_server_agent_heartbeat";

  /**
   * Agent initializing message.
   */
  private static final String INIT_MSG = "{0} is initializing";

  /**
   * Agent healthy message.
   */
  private static final String HEALTHY_MSG = "{0} is healthy";

  /**
   * Agent waiting for status updates message.
   */
  private static final String STATUS_UPDATE_MSG = "{0} is waiting for status updates";

  /**
   * Agent is not heartbeating message.
   */
  private static final String HEARTBEAT_LOST_MSG = "{0} is not sending heartbeats";

  /**
   * Agent is not healthy message.
   */
  private static final String UNHEALTHY_MSG = "{0} is not healthy";

  /**
   * Unknown agent state message.
   */
  private static final String UNKNOWN_MSG = "{0} has an unknown state of {1}";

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
   * Constructor. Required for type introspection by
   * {@link AmbariServerAlertService}.
   */
  public AgentHeartbeatAlertRunnable() {
  }

  @Override
  public void run() {
    try {
      Map<String, Cluster> clusterMap = m_clustersProvider.get().getClusters();
      for (Cluster cluster : clusterMap.values()) {
        AlertDefinitionEntity entity = m_dao.findByName(cluster.getClusterId(),
            HEARTBEAT_DEFINITION_NAME);

        // skip this cluster if the runnable's alert definition is missing or
        // disabled
        if (null == entity || !entity.getEnabled()) {
          continue;
        }

        long alertTimestamp = System.currentTimeMillis();

        Map<String, Host> hostMap = m_clustersProvider.get().getHostsForCluster(
            cluster.getClusterName());

        Set<Entry<String, Host>> entries = hostMap.entrySet();
        for (Entry<String, Host> entry : entries) {
          String hostName = entry.getKey();
          Host host = entry.getValue();

          String alertText;
          AlertState alertState = AlertState.OK;
          HostState hostState = host.getState();

          switch (hostState) {
            case INIT:
              alertText = MessageFormat.format(INIT_MSG, hostName);
              break;
            case HEALTHY:
              alertText = MessageFormat.format(HEALTHY_MSG, hostName);
              break;
            case WAITING_FOR_HOST_STATUS_UPDATES:
              alertText = MessageFormat.format(STATUS_UPDATE_MSG, hostName);
              break;
            case HEARTBEAT_LOST:
              alertState = AlertState.CRITICAL;
              alertText = MessageFormat.format(HEARTBEAT_LOST_MSG, hostName);
              break;
            case UNHEALTHY:
              alertState = AlertState.CRITICAL;
              alertText = MessageFormat.format(UNHEALTHY_MSG, hostName);
            default:
              alertState = AlertState.UNKNOWN;
              alertText = MessageFormat.format(UNKNOWN_MSG, hostName, hostState);
              break;
          }

          Alert alert = new Alert(entity.getDefinitionName(), null,
              entity.getServiceName(), entity.getComponentName(), hostName,
              alertState);

          alert.setLabel(entity.getLabel());
          alert.setText(alertText);
          alert.setTimestamp(alertTimestamp);
          alert.setCluster(cluster.getClusterName());

          AlertReceivedEvent event = new AlertReceivedEvent(
              cluster.getClusterId(), alert);

          m_alertEventPublisher.publish(event);
        }
      }
    } catch (Exception exception) {
      LOG.error("Unable to run the {} alert", HEARTBEAT_DEFINITION_NAME,
          exception);
    }
  }
}
