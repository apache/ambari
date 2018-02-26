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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.HostStatusHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/*
Class used to hold the status of metric collector hosts for a cluster.
 */
public class MetricsCollectorHAClusterState {

  private String clusterName;
  private Set<String> liveCollectorHosts;
  private Set<String> deadCollectorHosts;
  private AtomicInteger collectorDownRefreshCounter;
  private static int collectorDownRefreshCounterLimit = 5;
  private String currentCollectorHost = null;

  @Inject
  AmbariManagementController managementController;

  private static final Logger LOG =
    LoggerFactory.getLogger(MetricsCollectorHAClusterState.class);

  public MetricsCollectorHAClusterState(String clusterName) {

    if (managementController == null) {
      managementController = AmbariServer.getController();
    }

    this.clusterName = clusterName;
    this.liveCollectorHosts = new CopyOnWriteArraySet<>();
    this.deadCollectorHosts = new CopyOnWriteArraySet<>();
    collectorDownRefreshCounter = new AtomicInteger(0);
  }

  public void addMetricsCollectorHost(String collectorHost) {
    Long componentId = null;
    AmbariManagementController managementController = AmbariServer.getController();
    try {
      // TODO : Multi_Metrics_Changes. componentName=Role.METRICS_COLLECTOR.name() may or may not be unique if there are multiple instances.
      componentId = managementController.getClusters().getCluster(clusterName).getComponentId(Role.METRICS_COLLECTOR.name());
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    if (HostStatusHelper.isHostComponentLive(managementController, clusterName, collectorHost, "AMBARI_METRICS",
            componentId, Role.METRICS_COLLECTOR.name(), Role.METRICS_COLLECTOR.name())) {
      liveCollectorHosts.add(collectorHost);
      deadCollectorHosts.remove(collectorHost);
    } else {
      deadCollectorHosts.add(collectorHost);
      liveCollectorHosts.remove(collectorHost);
    }

    //If there is no current collector host or the current host is down, this will be a proactive switch.
    // TODO : Multi_Metrics_Changes. componentName=Role.METRICS_COLLECTOR.name() may or may not be unique if there are multiple instances.
    if (currentCollectorHost == null || !HostStatusHelper.isHostComponentLive(managementController, clusterName,
      currentCollectorHost, "AMBARI_METRICS", componentId, Role.METRICS_COLLECTOR.name(), Role.METRICS_COLLECTOR.name())) {
      refreshCollectorHost(currentCollectorHost);
    }
  }

  private void refreshCollectorHost(String currentHost) {
    LOG.info("Refreshing collector host, current collector host : " + currentHost);

    testAndAddDeadCollectorsToLiveList(); //A good time to check if there are some dead collectors that have now become alive.

    if (currentHost != null) {
      if (liveCollectorHosts.contains(currentHost)) {
        liveCollectorHosts.remove(currentHost);
      }
      if (!deadCollectorHosts.contains(currentHost)) {
        deadCollectorHosts.add(currentHost);
      }
    }

    if (!liveCollectorHosts.isEmpty()) {
      currentCollectorHost = getRandom(liveCollectorHosts);
    }

    if (currentCollectorHost == null && !deadCollectorHosts.isEmpty()) {
      currentCollectorHost = getRandom(deadCollectorHosts);
    }

    LOG.info("After refresh, new collector host : " + currentCollectorHost);
  }

  public String getCurrentCollectorHost() {
    return currentCollectorHost;
  }

  public void onCollectorHostDown(String deadCollectorHost) {

    if (deadCollectorHost == null) {
      // Case 1: Collector is null. Ideally this can never happen
      refreshCollectorHost(null);

    } else if (deadCollectorHost.equals(currentCollectorHost) && numCollectors() > 1) {
      // Case 2: Event informing us that the current collector is dead. We have not refreshed it yet.
      if (testRefreshCounter()) {
        refreshCollectorHost(deadCollectorHost);
      }
    }
    //Case 3 : Got a dead collector event. Already changed the collector to a new one.
    //No-Op
  }

  private void testAndAddDeadCollectorsToLiveList() {
    Set<String> liveHosts = new HashSet<>();

    for (String deadHost : deadCollectorHosts) {
      if (isValidAliveCollectorHost(clusterName, deadHost)) {
        liveHosts.add(deadHost);
      }
    }

    for (String liveHost : liveHosts) {
      LOG.info("Removing collector " + liveHost +  " from dead list to live list");
      deadCollectorHosts.remove(liveHost);
      liveCollectorHosts.add(liveHost);
    }
  }

  private boolean isValidAliveCollectorHost(String clusterName, String collectorHost) {
    Long componentId = null;
    AmbariManagementController managementController = AmbariServer.getController();
    try {
      componentId = managementController.getClusters().getCluster(clusterName).getComponentId(Role.METRICS_COLLECTOR.name());
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    // TODO : Multi_Metrics_Changes. componentName=Role.METRICS_COLLECTOR.name() may or may not be unique if there are multiple instances.
    return ((collectorHost != null) &&
      HostStatusHelper.isHostLive(managementController, clusterName, collectorHost) &&
      HostStatusHelper.isHostComponentLive(managementController, clusterName, collectorHost,
              "AMBARI_METRICS", componentId, Role.METRICS_COLLECTOR.name(), Role.METRICS_COLLECTOR.name()));
  }

  /*
    A refresh counter to track number of collector down events received. If it exceeds the limit,
    then we go ahead and refresh the collector.
   */
  private boolean testRefreshCounter() {
    collectorDownRefreshCounter.incrementAndGet();
    if (collectorDownRefreshCounter.get() == collectorDownRefreshCounterLimit) {
      collectorDownRefreshCounter = new AtomicInteger(0);
      return true;
    }
    return false;
  }

  public boolean isCollectorHostLive() {
    for (String host : liveCollectorHosts) {
      if (HostStatusHelper.isHostLive(managementController, clusterName, host)) {
        return true;
      }
    }

    //If no host is alive, check if some dead collectors have become live.
    testAndAddDeadCollectorsToLiveList();

    //try one more time
    for (String host : liveCollectorHosts) {
      if (HostStatusHelper.isHostLive(managementController, clusterName, host)) {
        return true;
      }
    }
    return false;
    }

  public boolean isCollectorComponentAlive() {

    Long componentId = null;
    AmbariManagementController managementController = AmbariServer.getController();
    try {
      componentId = managementController.getClusters().getCluster(clusterName).getComponentId(Role.METRICS_COLLECTOR.name());
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    //Check in live hosts
    // TODO : Multi_Metrics_Changes. componentName=Role.METRICS_COLLECTOR.name() may or may not be unique if there are multiple instances.
    for (String host : liveCollectorHosts) {
      if (HostStatusHelper.isHostComponentLive(managementController, clusterName, host, "AMBARI_METRICS",
              componentId, Role.METRICS_COLLECTOR.name(), Role.METRICS_COLLECTOR.name())) {
        return true;
      }
    }

    //Check in dead hosts. Don't update live and dead lists. Can be done on refresh call.
    // TODO : Multi_Metrics_Changes. componentName=Role.METRICS_COLLECTOR.name() may or may not be unique if there are multiple instances.
    for (String host : deadCollectorHosts) {
      if (HostStatusHelper.isHostComponentLive(managementController, clusterName, host, "AMBARI_METRICS",
              componentId, Role.METRICS_COLLECTOR.name(), Role.METRICS_COLLECTOR.name())) {
        return true;
      }
    }

    return false;
  }

  private int numCollectors() {
    return this.liveCollectorHosts.size() + deadCollectorHosts.size();
  }

  private String getRandom(Set<String> collectorSet) {
    int randIndex = new Random().nextInt(collectorSet.size());
    int i = 0;
    for(String host : collectorSet)
    {
      if (i == randIndex) {
        return host;
      }
      i = i + 1;
    }
    return null;
  }
}
