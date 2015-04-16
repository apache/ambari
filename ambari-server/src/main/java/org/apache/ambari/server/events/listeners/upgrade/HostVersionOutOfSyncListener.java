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
package org.apache.ambari.server.events.listeners.upgrade;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.HostAddedEvent;
import org.apache.ambari.server.events.ServiceComponentInstalledEvent;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link org.apache.ambari.server.events.listeners.upgrade.HostVersionOutOfSyncListener} class
 * handles {@link org.apache.ambari.server.events.ServiceInstalledEvent} and
 * {@link org.apache.ambari.server.events.ServiceComponentInstalledEvent}
 * to update {@link org.apache.ambari.server.state.RepositoryVersionState}
 *
 * @see org.apache.ambari.server.state.Cluster#recalculateClusterVersionState(String)
 */
@Singleton
@EagerSingleton
public class HostVersionOutOfSyncListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(HostVersionOutOfSyncListener.class);

  @Inject
  private Provider<HostVersionDAO> hostVersionDAO;

  @Inject
  private Provider<HostDAO> hostDAO;

  @Inject
  private Provider<Clusters> clusters;

  private AmbariEventPublisher ambariEventPublisher;

  @Inject
  public HostVersionOutOfSyncListener(AmbariEventPublisher ambariEventPublisher) {
    this.ambariEventPublisher = ambariEventPublisher;
    ambariEventPublisher.register(this);
  }

  @Subscribe
  @Transactional
  public void onServiceComponentEvent(ServiceComponentInstalledEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }
    try {
      Cluster cluster = clusters.get().getClusterById(event.getClusterId());
      List<HostVersionEntity> hostVersionEntities =
          hostVersionDAO.get().findByClusterAndHost(cluster.getClusterName(), event.getHostName());
      StackId currentStackId = cluster.getCurrentStackVersion();
      for (HostVersionEntity hostVersionEntity : hostVersionEntities) {
        if (hostVersionEntity.getRepositoryVersion().getStack().equals(currentStackId.getStackId())
            && hostVersionEntity.getState().equals(RepositoryVersionState.INSTALLED)) {
          hostVersionEntity.setState(RepositoryVersionState.OUT_OF_SYNC);
          hostVersionDAO.get().merge(hostVersionEntity);
          cluster.recalculateClusterVersionState(hostVersionEntity.getRepositoryVersion().getVersion());
        }
      }
    } catch (AmbariException e) {
      LOG.error("Can not update hosts about out of sync", e);
    }
  }

  @Subscribe
  @Transactional
  public void onServiceEvent(ServiceInstalledEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }
    try {
      Cluster cluster = clusters.get().getClusterById(event.getClusterId());
      Set<String> changedRepositoryVersions = new HashSet<String>();
      StackId currentStackId = cluster.getCurrentStackVersion();
      Map<String, ServiceComponent> serviceComponents = cluster.getService(event.getServiceName()).getServiceComponents();
      // Determine hosts that become OUT_OF_SYNC when adding components for new service
      Set<String> affectedHosts = new HashSet<String>();
      for (ServiceComponent component : serviceComponents.values()) {
        for (String hostname : component.getServiceComponentHosts().keySet()) {
          affectedHosts.add(hostname);
        }
      }
      for (String hostName : affectedHosts) {
        List<HostVersionEntity> hostVersionEntities =
            hostVersionDAO.get().findByClusterAndHost(cluster.getClusterName(), hostName);
        for (HostVersionEntity hostVersionEntity : hostVersionEntities) {
          if (hostVersionEntity.getRepositoryVersion().getStack().equals(currentStackId.getStackId())
              && hostVersionEntity.getState().equals(RepositoryVersionState.INSTALLED)) {
            hostVersionEntity.setState(RepositoryVersionState.OUT_OF_SYNC);
            hostVersionDAO.get().merge(hostVersionEntity);
            changedRepositoryVersions.add(hostVersionEntity.getRepositoryVersion().getVersion());
          }
        }
      }
      for (String version : changedRepositoryVersions) {
        cluster.recalculateClusterVersionState(version);
      }
    } catch (AmbariException e) {
      LOG.error("Can not update hosts about out of sync", e);
    }
  }

  @Subscribe
  @Transactional
  public void onHostEvent(HostAddedEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }
    try {
      Cluster cluster = clusters.get().getClusterById(event.getClusterId());
      Set<String> changedRepositoryVersions = new HashSet<String>();
      Collection<ClusterVersionEntity> allClusterVersions = cluster.getAllClusterVersions();
      for (ClusterVersionEntity clusterVersion : allClusterVersions) {
        if (clusterVersion.getState() != RepositoryVersionState.CURRENT) { // Current version is taken care of automatically
          String hostName = event.getHostName();
          HostEntity hostEntity = hostDAO.get().findByName(hostName);
          HostVersionEntity missingHostVersion = new HostVersionEntity(hostEntity,
                  clusterVersion.getRepositoryVersion(), RepositoryVersionState.OUT_OF_SYNC);
          hostVersionDAO.get().create(missingHostVersion);
          changedRepositoryVersions.add(clusterVersion.getRepositoryVersion().getVersion());
        }
      }
      for (String version : changedRepositoryVersions) {
        cluster.recalculateClusterVersionState(version);
      }
    } catch (AmbariException e) {
      LOG.error("Can not update hosts about out of sync", e);
    }
  }
}
