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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostSummary;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;

import com.google.inject.Inject;

/**
 * Action that represents finalizing the Upgrade by completing any database changes.
 */
public class FinalizeUpgradeAction extends AbstractServerAction {

  public static final String CLUSTER_NAME_KEY = "cluster_name";
  public static final String UPGRADE_DIRECTION_KEY = "upgrade_direction";
  public static final String VERSION_KEY = "version";
  public static final String PREVIOUS_UPGRADE_NOT_COMPLETED_MSG = "It is possible that a previous upgrade was not finalized. " +
      "For this reason, Ambari will not remove any configs. Please ensure that all database records are correct.";

  /**
   * The original "current" stack of the cluster before the upgrade started.
   * This is the same regardless of whether the current direction is
   * {@link Direction#UPGRADE} or {@link Direction#DOWNGRADE}.
   */
  public static final String ORIGINAL_STACK_KEY = "original_stack";

  /**
   * The target upgrade stack before the upgrade started. This is the same
   * regardless of whether the current direction is {@link Direction#UPGRADE} or
   * {@link Direction#DOWNGRADE}.
   */
  public static final String TARGET_STACK_KEY = "target_stack";

  /**
   * The Cluster that this ServerAction implementation is executing on
   */
  @Inject
  protected Clusters clusters;

  @Inject
  private ClusterVersionDAO clusterVersionDAO;

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private HostComponentStateDAO hostComponentStateDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    Map<String, String> commandParams = getExecutionCommand().getCommandParams();

    boolean isDowngrade = commandParams.containsKey(UPGRADE_DIRECTION_KEY) &&
        "downgrade".equals(commandParams.get(UPGRADE_DIRECTION_KEY).toLowerCase());

    String version = commandParams.get(VERSION_KEY);
    StackId originalStackId = new StackId(commandParams.get(ORIGINAL_STACK_KEY));
    StackId targetStackId = new StackId(commandParams.get(TARGET_STACK_KEY));

    String clusterName = getExecutionCommand().getClusterName();

    if (isDowngrade) {
      return finalizeDowngrade(clusterName, originalStackId, targetStackId,
          version);
    } else {
      return finalizeUpgrade(clusterName, version);
    }
  }

  /**
   * Execution path for upgrade.
   * @param clusterName the name of the cluster the upgrade is for
   * @param version     the target version of the upgrade
   * @return the command report
   */
  private CommandReport finalizeUpgrade(String clusterName, String version)
    throws AmbariException, InterruptedException {

    StringBuilder outSB = new StringBuilder();
    StringBuilder errSB = new StringBuilder();

    try {
      outSB.append(MessageFormat.format("Begin finalizing the upgrade of cluster {0} to version {1}\n", clusterName, version));

      Cluster cluster = clusters.getCluster(clusterName);
      StackId clusterDesiredStackId = cluster.getDesiredStackVersion();

      ClusterVersionEntity upgradingClusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(
          clusterName, clusterDesiredStackId, version);

      if (upgradingClusterVersion == null) {
        throw new AmbariException(MessageFormat.format(
            "Cluster stack version {0} not found", version));
      }

      // Validate that all of the hosts with a version in the cluster have the
      // version being upgraded to, and it is in an allowed state.
      List<HostVersionEntity> hostVersions = hostVersionDAO.findByClusterStackAndVersion(
          clusterName, clusterDesiredStackId, version);

      // Will include hosts whose state is UPGRADED, and potentially INSTALLED
      Set<HostVersionEntity> hostVersionsAllowed = new HashSet<HostVersionEntity>();
      Set<String> hostsWithoutCorrectVersionState = new HashSet<String>();
      Set<String> hostsToUpdate = new HashSet<String>();

      // It is important to only iterate over the hosts with a version, as
      // opposed to all hosts, since some hosts may only have components that do
      // not advertise a version, such as AMBARI_METRICS.
      for (HostVersionEntity hostVersion : hostVersions) {
        boolean hostHasCorrectVersionState = false;
        RepositoryVersionState hostVersionState = hostVersion.getState();
        switch( hostVersionState ){
          case UPGRADED:
          case CURRENT:{
            // if the state is correct, then do nothing
            hostHasCorrectVersionState = true;
            break;
          }
          case INSTALLED:{
            // It is possible that the host version has a state of INSTALLED and it
            // never changed if the host only has components that do not advertise a
            // version.
            HostEntity host = hostVersion.getHostEntity();

            ServiceComponentHostSummary hostSummary = new ServiceComponentHostSummary(ambariMetaInfo,
                host, clusterDesiredStackId);

            // if all components have finished advertising their version, then
            // this host can be considered UPGRADED
            if (hostSummary.haveAllComponentsFinishedAdvertisingVersion()) {
              // mark this as UPGRADED
              hostHasCorrectVersionState = true;
              hostVersion.setState(RepositoryVersionState.UPGRADED);
              hostVersion = hostVersionDAO.merge(hostVersion);
            } else {
              hostsWithoutCorrectVersionState.add(hostVersion.getHostName());
            }

            break;
          }
          default: {
            // all other states are not allowed
            hostsWithoutCorrectVersionState.add(hostVersion.getHostName());
            break;
          }
        }

        // keep track of this host version in order to transition it correctly
        if (hostHasCorrectVersionState) {
          hostVersionsAllowed.add(hostVersion);
          hostsToUpdate.add(hostVersion.getHostName());
        }
      }

      // throw an exception if there are hosts which are not not fully UPGRADED
      if (hostsWithoutCorrectVersionState.size() > 0) {
        String message = String.format("The following %d host(s) have not been upgraded to version %s. " +
                "Please install and upgrade the Stack Version on those hosts and try again.\nHosts: %s\n",
            hostsWithoutCorrectVersionState.size(),
            version,
            StringUtils.join(hostsWithoutCorrectVersionState, ", "));
        outSB.append(message);
        throw new AmbariException(message);
      }

      // iterate through all host components and make sure that they are on the
      // correct version; if they are not, then this will throw an exception
      List<InfoTuple> errors = checkHostComponentVersions(cluster, version, clusterDesiredStackId);
      if (! errors.isEmpty()) {
        StrBuilder messageBuff = new StrBuilder(
            String.format(
                "The following %d host component(s) "
                    + "have not been upgraded to version %s. Please install and upgrade "
                    + "the Stack Version on those hosts and try again.\nHost components:\n",
                errors.size(), version));

        for (InfoTuple error : errors) {
          messageBuff.append(String.format("%s on host %s\n", error.componentName, error.hostName));
        }

        throw new AmbariException(messageBuff.toString());
      }


      // we're guaranteed to be ready transition to UPGRADED now; ensure that
      // the transition will be allowed if the cluster state is not UPGRADED
      upgradingClusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(clusterName,
          clusterDesiredStackId, version);

      if (RepositoryVersionState.UPGRADING == upgradingClusterVersion.getState()) {
        cluster.transitionClusterVersion(clusterDesiredStackId, version,
            RepositoryVersionState.UPGRADED);

        upgradingClusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(
            clusterName, clusterDesiredStackId, version);
      }

      // we cannot finalize since the cluster was not ready to move into the
      // UPGRADED state
      if (RepositoryVersionState.UPGRADED != upgradingClusterVersion.getState()) {
        throw new AmbariException(String.format("The cluster stack version state %s is not allowed to transition directly into %s",
            upgradingClusterVersion.getState(), RepositoryVersionState.CURRENT.toString()));
      }

      outSB.append(String.format("Will finalize the upgraded state of host components in %d host(s).\n", hostVersionsAllowed.size()));

      // Reset the upgrade state
      for (HostVersionEntity hostVersion : hostVersionsAllowed) {
        Collection<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(hostVersion.getHostName());
        for (HostComponentStateEntity hostComponentStateEntity: hostComponentStates) {
          hostComponentStateEntity.setUpgradeState(UpgradeState.NONE);
          hostComponentStateDAO.merge(hostComponentStateEntity);
        }
      }

      outSB.append(String.format("Will finalize the version for %d host(s).\n", hostVersionsAllowed.size()));

      // Impacts all hosts that have a version
      cluster.mapHostVersions(hostsToUpdate, upgradingClusterVersion, RepositoryVersionState.CURRENT);

      outSB.append(String.format("Will finalize the version for cluster %s.\n", clusterName));

      // transitioning the cluster into CURRENT will update the current/desired
      // stack values
      cluster.transitionClusterVersion(clusterDesiredStackId, version,
          RepositoryVersionState.CURRENT);

      outSB.append("Upgrade was successful!\n");
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outSB.toString(), errSB.toString());
    } catch (Exception e) {
      errSB.append(e.getMessage());
      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", outSB.toString(), errSB.toString());
    }
  }

  /**
   * Execution path for downgrade.
   *
   * @param clusterName
   *          the name of the cluster the downgrade is for
   * @paran originalStackId the stack ID of the cluster before the upgrade.
   * @paran targetStackId the stack ID that was desired for this upgrade.
   * @param version
   *          the target version of the downgrade
   * @return the command report
   */
  private CommandReport finalizeDowngrade(String clusterName,
      StackId originalStackId, StackId targetStackId, String version)
      throws AmbariException, InterruptedException {

    StringBuilder out = new StringBuilder();
    StringBuilder err = new StringBuilder();

    try {
      Cluster cluster = clusters.getCluster(clusterName);
      StackId currentClusterStackId = cluster.getCurrentStackVersion();

      // Safety check that the cluster's stack (from clusterstate's current_stack_id) is equivalent to the
      // cluster's CURRENT repo version's stack. This is to avoid deleting configs from the target stack if the customer
      // ended up modifying their database manually after a stack upgrade and forgot to call "Save DB State".
      ClusterVersionEntity currentClusterVersion = cluster.getCurrentClusterVersion();
      RepositoryVersionEntity currentRepoVersion = currentClusterVersion.getRepositoryVersion();
      StackId currentRepoStackId = currentRepoVersion.getStackId();
      if (!currentRepoStackId.equals(originalStackId)) {
        String msg = String.format("The stack of Cluster %s's CURRENT repo version is %s, yet the original stack id from " +
            "the Stack Upgrade has a different value of %s. %s",
            clusterName, currentRepoStackId.getStackId(), originalStackId.getStackId(), PREVIOUS_UPGRADE_NOT_COMPLETED_MSG);
        out.append(msg);
        err.append(msg);
        throw new AmbariException("The source target stack doesn't match the cluster's CURRENT repo version's stack.");
      }

      // This was a cross-stack upgrade, meaning that configurations were created that now need to be removed.
      if (!originalStackId.equals(targetStackId)) {
        out.append(String.format("Will remove configs since the original stack %s differs from the target stack %s " +
            "that Ambari just downgraded from.", originalStackId.getStackId(), targetStackId.getStackId()));
        cluster.removeConfigurations(targetStackId);
      }

      // !!! find and make sure the cluster_version EXCEPT current are set back
      out.append(String.format("Searching for current version for %s\n",
          clusterName));

      ClusterVersionEntity clusterVersion = clusterVersionDAO.findByClusterAndStateCurrent(clusterName);
      if (null == clusterVersion) {
        throw new AmbariException("Could not find current cluster version");
      }

      out.append(String.format("Comparing downgrade version %s to current cluster version %s\n",
          version,
          clusterVersion.getRepositoryVersion().getVersion()));

      if (!version.equals(clusterVersion.getRepositoryVersion().getVersion())) {
        throw new AmbariException(
            String.format("Downgrade version %s is not the current cluster version of %s",
                version, clusterVersion.getRepositoryVersion().getVersion()));
      } else {
        out.append(String.format("Downgrade version is the same as current.  Searching " +
          "for cluster versions that do not match %s\n", version));
      }

      Set<String> badVersions = new HashSet<String>();

      // update the cluster version
      for (ClusterVersionEntity cve : clusterVersionDAO.findByCluster(clusterName)) {
        switch (cve.getState()) {
          case UPGRADE_FAILED:
          case UPGRADED:
          case UPGRADING: {
              badVersions.add(cve.getRepositoryVersion().getVersion());
              cve.setState(RepositoryVersionState.INSTALLED);
              clusterVersionDAO.merge(cve);
              break;
            }
          default:
            break;
        }
      }

      out.append(String.format("Found %d other version(s) not matching downgrade: %s\n",
          badVersions.size(), StringUtils.join(badVersions, ", ")));

      Set<String> badHosts = new HashSet<String>();
      for (String badVersion : badVersions) {
        List<HostVersionEntity> hostVersions = hostVersionDAO.findByClusterStackAndVersion(
            clusterName, targetStackId, badVersion);

        for (HostVersionEntity hostVersion : hostVersions) {
          badHosts.add(hostVersion.getHostName());
          hostVersion.setState(RepositoryVersionState.INSTALLED);
          hostVersionDAO.merge(hostVersion);
        }
      }

      out.append(String.format("Found %d hosts not matching downgrade version: %s\n",
          badHosts.size(), version));

      for (String badHost : badHosts) {
        List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(badHost);
        for (HostComponentStateEntity hostComponentState : hostComponentStates) {
          hostComponentState.setUpgradeState(UpgradeState.NONE);
          hostComponentStateDAO.merge(hostComponentState);
        }
      }

      // ensure that when downgrading, we set the desired back to the
      // original value
      cluster.setDesiredStackVersion(currentClusterStackId);

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
          out.toString(), err.toString());

    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      err.append(sw.toString());

      return createCommandReport(-1, HostRoleStatus.FAILED, "{}",
          out.toString(), err.toString());
    }
  }


  /**
   * Confirms that all host components that are able to provide hdp version,
   * have been upgraded to the target version.
   * @param cluster         the cluster the upgrade is for
   * @param desiredVersion  the target version of the upgrade
   * @param targetStack     the target stack id for meta-info lookup
   * @return the list of {@link InfoTuple} objects of host components in error
   */
  protected List<InfoTuple> checkHostComponentVersions(Cluster cluster, String desiredVersion, StackId targetStackId)
          throws AmbariException {

    ArrayList<InfoTuple> errors = new ArrayList<InfoTuple>();

    for (Service service : cluster.getServices().values()) {
      for (ServiceComponent serviceComponent : service.getServiceComponents().values()) {
        for (ServiceComponentHost serviceComponentHost : serviceComponent.getServiceComponentHosts().values()) {
          ComponentInfo componentInfo = ambariMetaInfo.getComponent(targetStackId.getStackName(),
                  targetStackId.getStackVersion(), service.getName(), serviceComponent.getName());

          if (!componentInfo.isVersionAdvertised()) {
            StackId desired = serviceComponentHost.getDesiredStackVersion();
            StackId actual = serviceComponentHost.getStackVersion();
            if (!desired.equals(actual)) {
              serviceComponentHost.setStackVersion(desired);
            }
          } else if (componentInfo.isVersionAdvertised()
              && !serviceComponentHost.getVersion().equals(desiredVersion)) {
            errors.add(new InfoTuple(
                service.getName(), serviceComponent.getName(),
                serviceComponentHost.getHostName(), serviceComponentHost.getVersion()));
          }
        }
      }
    }

    return errors;
  }

  protected static class InfoTuple {
    protected final String serviceName;
    protected final String componentName;
    protected final String hostName;
    protected final String currentVersion;

    protected InfoTuple(String service, String component, String host, String version) {
      serviceName = service;
      componentName = component;
      hostName = host;
      currentVersion = version;
    }

  }

}
