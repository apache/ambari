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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.HostComponentVersionAdvertisedEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link StackVersionListener} class handles the propagation of versions
 * advertised by the {@link org.apache.ambari.server.state.ServiceComponentHost}
 * that bubble up to the
 * {@link org.apache.ambari.server.orm.entities.HostVersionEntity} and
 * eventually the
 * {@link org.apache.ambari.server.orm.entities.ClusterVersionEntity}
 */
@Singleton
@EagerSingleton
public class StackVersionListener {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackVersionListener.class);
  private static final String UNKNOWN_VERSION = State.UNKNOWN.toString();

  /**
   * Used to prevent multiple threads from trying to create host alerts
   * simultaneously.
   */
  private Lock m_stackVersionLock = new ReentrantLock();

  /**
   * Constructor.
   *
   * @param eventPublisher  the publisher
   */
  @Inject
  public StackVersionListener(VersionEventPublisher eventPublisher) {
    eventPublisher.register(this);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(HostComponentVersionAdvertisedEvent event) {
    LOG.debug("Received event {}", event);

    Cluster cluster = event.getCluster();

    ServiceComponentHost sch = event.getServiceComponentHost();
    String newVersion = event.getVersion();

    m_stackVersionLock.lock();

    // Update host component version value if needed
    try {
      ServiceComponent sc = cluster.getService(sch.getServiceName()).getServiceComponent(sch.getServiceComponentName());
      if (newVersion == null) {
        processComponentVersionNotAdvertised(sc, sch);
      } else if (UNKNOWN_VERSION.equals(sc.getDesiredVersion())) {
        processUnknownDesiredVersion(cluster, sc, sch, newVersion);
      } else if (StringUtils.isNotBlank(newVersion)) {
        String previousVersion = sch.getVersion();
        if (previousVersion == null || UNKNOWN_VERSION.equalsIgnoreCase(previousVersion)) {
          // value may be "UNKNOWN" when upgrading from older Ambari versions
          // or if host component reports it's version for the first time
          sch.setUpgradeState(UpgradeState.NONE);
          sch.setVersion(newVersion);
          bootstrapVersion(cluster, sch);
        } else if (!StringUtils.equals(previousVersion, newVersion)) { //
          processComponentVersionChange(cluster, sc, sch, newVersion);
        }
      }
    } catch (Exception e) {
      LOG.error(
          "Unable to propagate version for ServiceHostComponent on component: {}, host: {}. Error: {}",
          sch.getServiceComponentName(), sch.getHostName(), e.getMessage());
    } finally {
      m_stackVersionLock.unlock();
    }
  }

  /**
   * Bootstrap cluster/repo version when version is reported for the first time
   * @param cluster target cluster
   * @param sch target host component
   * @throws AmbariException
   */
  private void bootstrapVersion(Cluster cluster, ServiceComponentHost sch) throws AmbariException {
    RepositoryVersionEntity repoVersion = sch.recalculateHostVersionState();
    if (null != repoVersion) {
      cluster.recalculateClusterVersionState(repoVersion);
    }
  }

  /**
   * Possible situation after upgrade from older Ambari version. Just use
   * reported component version as desired version
   * @param cluster target cluster
   * @param sc target service component
   * @param sch target host component
   * @param newVersion advertised version
   */
  private void processUnknownDesiredVersion(Cluster cluster, ServiceComponent sc,
                                            ServiceComponentHost sch,
                                            String newVersion) throws AmbariException {
    sc.setDesiredVersion(newVersion);
    sch.setUpgradeState(UpgradeState.NONE);
    sch.setVersion(newVersion);
    bootstrapVersion(cluster, sch);
  }

  /**
   * Focuses on cases when host component version really changed
   * @param cluster target cluster
   * @param sc target service component
   * @param sch target host component
   * @param newVersion advertised version
   */
  private void processComponentVersionChange(Cluster cluster, ServiceComponent sc,
                                             ServiceComponentHost sch,
                                             String newVersion) {
    String desiredVersion = sc.getDesiredVersion();
    UpgradeState upgradeState = sch.getUpgradeState();
    if (upgradeState == UpgradeState.IN_PROGRESS) {
      // Component status update is received during upgrade process
      if (desiredVersion.equals(newVersion)) {
        sch.setUpgradeState(UpgradeState.COMPLETE);  // Component upgrade confirmed
        sch.setStackVersion(cluster.getDesiredStackVersion());
      } else { // Unexpected (wrong) version received
        // Even during failed upgrade, we should not receive wrong version
        // That's why mark as VERSION_MISMATCH
        sch.setUpgradeState(UpgradeState.VERSION_MISMATCH);
      }
    } else if (upgradeState == UpgradeState.VERSION_MISMATCH && desiredVersion.equals(newVersion)) {
      if (cluster.getUpgradeEntity() != null) {
        sch.setUpgradeState(UpgradeState.COMPLETE);
      } else {
        sch.setUpgradeState(UpgradeState.NONE);
      }
    } else { // No upgrade in progress, unexpected version change
      sch.setUpgradeState(UpgradeState.VERSION_MISMATCH);
    }
    sch.setVersion(newVersion);
  }

  /**
   * Focuses on cases when component does not advertise it's version
   */
  private void processComponentVersionNotAdvertised(ServiceComponent sc, ServiceComponentHost sch) {
    if (UpgradeState.ONGOING_UPGRADE_STATES.contains(sch.getUpgradeState())) {
      sch.setUpgradeState(UpgradeState.FAILED);
    } else if (!sc.isVersionAdvertised()) {
      sch.setUpgradeState(UpgradeState.NONE);
    } else {
      sch.setUpgradeState(UpgradeState.VERSION_MISMATCH);
    }
  }
}
