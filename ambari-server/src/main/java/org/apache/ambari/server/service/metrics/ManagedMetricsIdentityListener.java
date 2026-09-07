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
package org.apache.ambari.server.service.metrics;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.events.ClusterConfigFinishedEvent;
import org.apache.ambari.server.events.ClusterProvisionedEvent;
import org.apache.ambari.server.events.JpaInitializedEvent;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.ServiceRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Connects managed Metrics identity provisioning to cluster and service lifecycle events. */
@Singleton
@EagerSingleton
public class ManagedMetricsIdentityListener {
  private static final Logger LOG = LoggerFactory.getLogger(ManagedMetricsIdentityListener.class);
  private static final String SERVICE_NAME = "VICTORIAMETRICS";

  private final ManagedMetricsIdentityService identityService;
  private final Provider<Clusters> clusters;

  @Inject
  public ManagedMetricsIdentityListener(ManagedMetricsIdentityService identityService,
      Provider<Clusters> clusters, AmbariEventPublisher eventPublisher) {
    this.identityService = identityService;
    this.clusters = clusters;
    eventPublisher.register(this);
  }

  @Subscribe
  public void onServiceInstalled(ServiceInstalledEvent event) throws AmbariException {
    if (SERVICE_NAME.equals(event.getServiceName())) {
      identityService.provision(event.getClusterId());
    }
  }

  @Subscribe
  public void onServiceRemoved(ServiceRemovedEvent event) throws AmbariException {
    if (SERVICE_NAME.equals(event.getServiceName())) {
      identityService.provision(event.getClusterId());
    }
  }

  @Subscribe
  public void onClusterConfigChanged(ClusterConfigChangedEvent event) throws AmbariException {
    if (ManagedMetricsIdentityService.CONFIG_TYPE.equals(event.getConfigType())) {
      identityService.provisionPendingConfig(event.getClusterName(), event.getVersionTag());
    }
  }

  @Subscribe
  public void onClusterConfigFinished(ClusterConfigFinishedEvent event) throws AmbariException {
    identityService.provision(event.getClusterId());
  }

  @Subscribe
  public void onClusterProvisioned(ClusterProvisionedEvent event) throws AmbariException {
    identityService.provision(event.getClusterId());
  }

  @Subscribe
  public void onJpaInitialized(JpaInitializedEvent event) {
    for (Cluster cluster : clusters.get().getClusters().values()) {
      try {
        identityService.provision(cluster.getClusterId());
      } catch (AmbariException | RuntimeException e) {
        LOG.warn("Unable to restore the managed Metrics identity for cluster {}",
            cluster.getClusterName(), e);
      }
    }
  }
}
