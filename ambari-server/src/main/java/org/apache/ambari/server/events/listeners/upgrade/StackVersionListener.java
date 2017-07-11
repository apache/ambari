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
package org.apache.ambari.server.events.listeners.upgrade;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.HostComponentVersionAdvertisedEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link StackVersionListener} class handles the propagation of versions
 * advertised by the {@link org.apache.ambari.server.state.ServiceComponentHost}
 * that bubble up to the
 * {@link org.apache.ambari.server.orm.entities.HostVersionEntity}.
 */
@Singleton
@EagerSingleton
public class StackVersionListener {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackVersionListener.class);
  public static final String UNKNOWN_VERSION = State.UNKNOWN.toString();

  /**
   * Used to prevent multiple threads from trying to update the same host
   * version simultaneously.
   */
  private Lock m_stackVersionLock = new ReentrantLock();

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

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
  public void onAmbariEvent(HostComponentVersionAdvertisedEvent event) {
    LOG.debug("Received event {}", event);

    Cluster cluster = event.getCluster();

    ServiceComponentHost sch = event.getServiceComponentHost();

    String newVersion = event.getVersion();
    if (StringUtils.isEmpty(newVersion)) {
      return;
    }

    // if the cluster is upgrading, there's no need to update the repo version -
    // it better be right
    if (null != event.getRepositoryVersionId() && null == cluster.getUpgradeInProgress()) {
      // !!! make sure the repo_version record actually has the same version.
      // This is NOT true when installing a cluster using a public repo where the
      // exact version is not known in advance.
      RepositoryVersionEntity rve = repositoryVersionDAO.findByPK(event.getRepositoryVersionId());
      if (null != rve) {
        String currentRepoVersion = rve.getVersion();
        if (!StringUtils.equals(currentRepoVersion, newVersion)) {
          rve.setVersion(newVersion);
          repositoryVersionDAO.merge(rve);
        }
      }
    }

    m_stackVersionLock.lock();

    // Update host component version value if needed
    try {
      ServiceComponent sc = cluster.getService(sch.getServiceName()).getServiceComponent(
          sch.getServiceComponentName());

      // not advertising a version, do nothing
      if (!sc.isVersionAdvertised()) {
        // that's odd; a version came back - log it and still do nothing
        if (!StringUtils.equalsIgnoreCase(UNKNOWN_VERSION, newVersion)) {
          LOG.debug(
              "ServiceComponent {} doesn't advertise version, however ServiceHostComponent {} on host {} advertised version as {}. Skipping version update",
              sc.getName(), sch.getServiceComponentName(), sch.getHostName(), newVersion);
        }
        return;
      }

      boolean desiredVersionIsCurrentlyUnknown = StringUtils.equalsIgnoreCase(UNKNOWN_VERSION,
          sc.getDesiredVersion());

      // proces the UNKNOWN version being received or currently desired
      if (StringUtils.equalsIgnoreCase(UNKNOWN_VERSION, newVersion)
          || desiredVersionIsCurrentlyUnknown) {
        processUnknownDesiredVersion(cluster, sc, sch, newVersion);
        return;
      }

      processComponentAdvertisedVersion(cluster, sc, sch, newVersion);
    } catch (Exception e) {
      LOG.error(
          "Unable to propagate version for ServiceHostComponent on component: {}, host: {}. Error: {}",
          sch.getServiceComponentName(), sch.getHostName(), e.getMessage());
    } finally {
      m_stackVersionLock.unlock();
    }
  }


  /**
   * Updates the version and {@link UpgradeState} for the specified
   * {@link ServiceComponentHost} if necessary. If the version or the upgrade
   * state changes, then this method will call
   * {@link ServiceComponentHost#recalculateHostVersionState()} in order to
   * ensure that the host version state is properly updated.
   * <p/>
   *
   *
   * @param cluster
   * @param sc
   * @param sch
   * @param newVersion
   * @throws AmbariException
   */
  private void processComponentAdvertisedVersion(Cluster cluster, ServiceComponent sc,
      ServiceComponentHost sch, String newVersion) throws AmbariException {
    if (StringUtils.isBlank(newVersion)) {
      return;
    }

    String previousVersion = sch.getVersion();
    String desiredVersion = sc.getDesiredVersion();
    UpgradeState upgradeState = sch.getUpgradeState();

    // was this version expected
    boolean newVersionMatchesDesired = StringUtils.equals(desiredVersion, newVersion);

    // was the prior version UNKNOWN or null
    boolean previousVersionIsUnknown = StringUtils.equalsIgnoreCase(UNKNOWN_VERSION,
        previousVersion) || StringUtils.isBlank(previousVersion);

    boolean desiredVersionIsUnknown = StringUtils.equalsIgnoreCase(UNKNOWN_VERSION, desiredVersion);

    // is there an upgrade in progress for this component
    boolean isUpgradeInProgressForThisComponent = null != cluster.getUpgradeInProgress()
        && upgradeState != UpgradeState.NONE;

    // if the current version is an actual value (ie 2.2.0.0-1234 and not
    // UNKNOWN), and the newly received version is unexpected, and we are not in
    // an upgrade - then we really should not be changing the reported version
    if (!previousVersionIsUnknown && !desiredVersionIsUnknown && !newVersionMatchesDesired
        && !isUpgradeInProgressForThisComponent) {
      LOG.warn(
          "Received a reported version of {} for {} on {}. This was not expected since the desired version is {} and the cluster is not upgrading this component. The version will not be changed.",
          newVersion, sc.getName(), sch.getHostName(), desiredVersion);

      return;
    }

    // update the SCH to the new version reported
    if (!StringUtils.equals(previousVersion, newVersion)) {
      sch.setVersion(newVersion);
    }

    if (previousVersion == null || previousVersionIsUnknown) {
      // value may be "UNKNOWN" when upgrading from older Ambari versions
      // or if host component reports it's version for the first time
      sch.setUpgradeState(UpgradeState.NONE);
      recalculateHostVersionAndClusterVersion(cluster, sch);
    } else {
      if (newVersionMatchesDesired) {
        if (isUpgradeInProgressForThisComponent) {
          sch.setStackVersion(cluster.getDesiredStackVersion());
          setUpgradeStateIfChanged(cluster, sch, UpgradeState.COMPLETE);
        } else {
          // no upgrade in progress for this component, then this should always
          // be NONE
          setUpgradeStateIfChanged(cluster, sch, UpgradeState.NONE);
        }
      } else {
        // if the versions don't match for any reason, regardless of upgrade
        // state, then VERSION_MISMATCH it
        setUpgradeStateIfChanged(cluster, sch, UpgradeState.VERSION_MISMATCH);
      }
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

    recalculateHostVersionAndClusterVersion(cluster, sch);
  }

  /**
   * Sets the upgrade state on the component if it has changed. This method will
   * not trigger an sort of {@link ClusterVersionEntity} or
   * {@link HostVersionEntity} recalculation.
   *
   * @param sch
   * @param upgradeState
   * @throws AmbariException
   */
  private void setUpgradeStateIfChanged(Cluster cluster, ServiceComponentHost sch,
      UpgradeState upgradeState) throws AmbariException {

    // don't need to recalculate anything here if the upgrade state is not changing
    if (sch.getUpgradeState() == upgradeState) {
      return;
    }

    // if the upgrade state changes, then also recalculate host versions
    sch.setUpgradeState(upgradeState);
  }

  /**
   * Recalculates the {@link HostVersionEntity} for the host specified by the
   * host component, taking into account all component states on that host. This
   * will also trigger a {@link ClusterVersionEntity} recalculatation for the
   * cluster version as well.
   *
   * @param cluster
   * @param sch
   * @throws AmbariException
   */
  private void recalculateHostVersionAndClusterVersion(Cluster cluster, ServiceComponentHost sch)
      throws AmbariException {
    // trigger a re-calculation of the cluster state based on the SCH state
    RepositoryVersionEntity repoVersion = sch.recalculateHostVersionState();
    if (null != repoVersion) {
      cluster.recalculateClusterVersionState(repoVersion);
    }
  }
}