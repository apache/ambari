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

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.bootstrap.DistributeRepositoriesStructuredOutput;
import org.apache.ambari.server.events.ActionFinalReportReceivedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link org.apache.ambari.server.events.listeners.upgrade.DistributeRepositoriesActionListener} class
 * handles {@link org.apache.ambari.server.events.ActionFinalReportReceivedEvent}
 * for "Distribute repositories/install packages" action.
 * It processes command reports and and updates host stack version state acordingly.
 */
@Singleton
@EagerSingleton
public class DistributeRepositoriesActionListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(DistributeRepositoriesActionListener.class);
  public static final String INSTALL_PACKAGES = "install_packages";

  @Inject
  private Provider<HostVersionDAO> hostVersionDAO;

  @Inject
  private Provider<Clusters> clusters;

  @Inject
  private RepositoryVersionDAO repoVersionDAO;


  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public DistributeRepositoriesActionListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  @Subscribe
  public void onActionFinished(ActionFinalReportReceivedEvent event) {
    // Check if it is "Distribute repositories/install packages" action.
    if (! event.getRole().equals(INSTALL_PACKAGES)) {
      return;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    RepositoryVersionState newHostState = RepositoryVersionState.INSTALL_FAILED;
    Long clusterId = event.getClusterId();
    if (clusterId == null) {
      LOG.error("Distribute Repositories expected a cluster Id for host " + event.getHostname());
      return;
    }

    String repositoryVersion = null;

    if (event.getCommandReport() == null) {
      LOG.error(
          "Command report is null, will set all INSTALLING versions for host {} to INSTALL_FAILED.",
          event.getHostname());
    } else if (!event.getCommandReport().getStatus().equals(HostRoleStatus.COMPLETED.toString())) {
      LOG.warn(
          "Distribute repositories did not complete, will set all INSTALLING versions for host {} to INSTALL_FAILED.",
          event.getHostname());
    } else {
      // Parse structured output
      try {
        newHostState = RepositoryVersionState.INSTALLED;
        DistributeRepositoriesStructuredOutput structuredOutput = StageUtils.getGson().fromJson(
                event.getCommandReport().getStructuredOut(),
                DistributeRepositoriesStructuredOutput.class);

        repositoryVersion = structuredOutput.getInstalledRepositoryVersion();

        // Handle the case in which the version to install did not contain the build number,
        // but the structured output does contain the build number.
        if (null != structuredOutput.getActualVersion() && !structuredOutput.getActualVersion().isEmpty() &&
            null != structuredOutput.getInstalledRepositoryVersion() && !structuredOutput.getInstalledRepositoryVersion().isEmpty() &&
            null != structuredOutput.getStackId() && !structuredOutput.getStackId().isEmpty() &&
            !structuredOutput.getActualVersion().equals(structuredOutput.getInstalledRepositoryVersion())) {

          // !!! getInstalledRepositoryVersion() from the agent is the one
          // entered in the UI.  getActualVersion() is computed.

          StackId stackId = new StackId(structuredOutput.getStackId());
          RepositoryVersionEntity version = repoVersionDAO.findByStackAndVersion(
              stackId, structuredOutput.getInstalledRepositoryVersion());

          if (null != version) {
            LOG.info("Repository version {} was found, but {} is the actual value",
                structuredOutput.getInstalledRepositoryVersion(),
                structuredOutput.getActualVersion());
            // !!! the entered version is not correct
            version.setVersion(structuredOutput.getActualVersion());
            repoVersionDAO.merge(version);
            repositoryVersion = structuredOutput.getActualVersion();
          } else {
            // !!! extra check that the actual version is correct
            stackId = new StackId(structuredOutput.getStackId());
            version = repoVersionDAO.findByStackAndVersion(stackId,
                structuredOutput.getActualVersion());

            LOG.debug("Repository version {} was not found, check for {}.  Found={}",
                structuredOutput.getInstalledRepositoryVersion(),
                structuredOutput.getActualVersion(),
                Boolean.valueOf(null != version));

            if (null != version) {
              repositoryVersion = structuredOutput.getActualVersion();
            }
          }
        }
      } catch (JsonSyntaxException e) {
        LOG.error("Cannot parse structured output %s", e);
      }
    }

    List<HostVersionEntity> hostVersions = hostVersionDAO.get().findByHost(event.getHostname());
      // We have to iterate over all host versions for this host. Otherwise server-side command aborts (that do not
      // provide exact host stack version info) would be ignored
    for (HostVersionEntity hostVersion : hostVersions) {

      if (repositoryVersion != null && !hostVersion.getRepositoryVersion().getVersion().equals(repositoryVersion)) {
        continue;
      }

      // If repository version is null, it means that we were not able to determine any information (perhaps structured-out was empty),
      // so we should transition from INSTALLING to INSTALL_FAILED

      // If we know exact host stack version, there will be single execution of a code below
      if (hostVersion.getState() == RepositoryVersionState.INSTALLING) {
        hostVersion.setState(newHostState);
        hostVersionDAO.get().merge(hostVersion);
        // Update state of a cluster stack version
        try {
          Cluster cluster = clusters.get().getClusterById(clusterId);
          cluster.recalculateClusterVersionState(hostVersion.getRepositoryVersion());
        } catch (AmbariException e) {
          LOG.error("Cannot get cluster with Id " + clusterId.toString() + " to recalculate its ClusterVersion.", e);
        }
      }
    }
  }
}
