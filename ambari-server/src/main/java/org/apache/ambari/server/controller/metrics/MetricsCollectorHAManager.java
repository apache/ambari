/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.metrics;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.events.MetricsCollectorHostDownEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/*
  Class used as a gateway to retrieving/updating metric collector hosts for all managed clusters.
 */
public class MetricsCollectorHAManager {

  @Inject
  protected AmbariEventPublisher eventPublisher;

  private Map<String, MetricsCollectorHAClusterState> clusterCollectorHAState;
  private static final Logger LOG =
    LoggerFactory.getLogger(MetricsCollectorHAManager.class);

  public MetricsCollectorHAManager() {
    clusterCollectorHAState = new HashMap<>();

    if (null == eventPublisher && null != AmbariServer.getController()) {
      eventPublisher = AmbariServer.getController().getAmbariEventPublisher();
      if (eventPublisher != null) {
        eventPublisher.register(this);
      } else {
        LOG.error("Unable to retrieve AmbariEventPublisher for Metric collector host event listening.");
      }
    }
  }

  public void addCollectorHost(String clusterName, String collectorHost) {

    LOG.info("Adding collector host : " + collectorHost + " to cluster : " + clusterName);

    if (! clusterCollectorHAState.containsKey(clusterName)) {
      clusterCollectorHAState.put(clusterName, new MetricsCollectorHAClusterState(clusterName));
    }
    MetricsCollectorHAClusterState collectorHAClusterState = clusterCollectorHAState.get(clusterName);
    collectorHAClusterState.addMetricsCollectorHost(collectorHost);
  }

  public String getCollectorHost(String clusterName) {

    if (! clusterCollectorHAState.containsKey(clusterName)) {
      clusterCollectorHAState.put(clusterName, new MetricsCollectorHAClusterState(clusterName));
    }

    MetricsCollectorHAClusterState collectorHAClusterState = clusterCollectorHAState.get(clusterName);
    return collectorHAClusterState.getCurrentCollectorHost();
  }

  /**
   * Handles {@link MetricsCollectorHostDownEvent}
   *
   * @param event
   *          the change event.
   */
  @Subscribe
  public void onMetricsCollectorHostDownEvent(MetricsCollectorHostDownEvent event) {

    LOG.debug("MetricsCollectorHostDownEvent caught, Down collector : {}", event.getCollectorHost());

    String clusterName = event.getClusterName();
    MetricsCollectorHAClusterState collectorHAClusterState = clusterCollectorHAState.get(clusterName);
    collectorHAClusterState.onCollectorHostDown(event.getCollectorHost());
  }

  public boolean isEmpty() {
    return this.clusterCollectorHAState.isEmpty();
  }

  public boolean isCollectorHostLive(String clusterName) {
    MetricsCollectorHAClusterState metricsCollectorHAClusterState = this.clusterCollectorHAState.get(clusterName);
    return metricsCollectorHAClusterState != null && metricsCollectorHAClusterState.isCollectorHostLive();
  }

  public boolean isCollectorComponentLive(String clusterName) {
    MetricsCollectorHAClusterState metricsCollectorHAClusterState = this.clusterCollectorHAState.get(clusterName);
    return metricsCollectorHAClusterState != null && metricsCollectorHAClusterState.isCollectorComponentAlive();
  }
}
